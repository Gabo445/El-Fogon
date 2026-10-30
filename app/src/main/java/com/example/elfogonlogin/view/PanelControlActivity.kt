package com.example.elfogonlogin.view

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.elfogonlogin.R
import com.example.elfogonlogin.controller.AdminRepository
import com.example.elfogonlogin.controller.FotoPerfil
import com.example.elfogonlogin.controller.Pedido
import com.example.elfogonlogin.controller.PedidoRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Panel de control del administrador. Muestra TODOS los pedidos en tiempo real
// y deja avanzarlos paso a paso: pendiente -> recibido -> preparando ->
// camino -> entregado. Cada avance guarda la hora del paso y el cliente lo ve
// al instante en su pestaña Pedidos.
//
// Aviso importante: el sonido y la vibración de "pedido nuevo" funcionan solo
// MIENTRAS esta pantalla está abierta. Con la app cerrada no puede llegar
// ninguna alerta, porque para eso hacen falta notificaciones push, y esas
// piden un servidor de pago (plan Blaze de Firebase).
class PanelControlActivity : AppCompatActivity() {

    private enum class Pestana { NUEVOS, EN_CURSO, ENTREGADOS }

    // Los estados que cuentan como "en curso" (ya confirmados, aún sin entregar).
    private val estadosEnCurso = listOf(
        PedidoRepository.ESTADO_RECIBIDO,
        PedidoRepository.ESTADO_PREPARANDO,
        PedidoRepository.ESTADO_CAMINO
    )

    // Siempre en hora de El Salvador, sin importar la zona del celular.
    private val zonaElSalvador: TimeZone = TimeZone.getTimeZone("America/El_Salvador")

    private lateinit var tvCerrarSesion: View
    private lateinit var tabNuevos: TextView
    private lateinit var tabEnCurso: TextView
    private lateinit var tabEntregados: TextView
    private lateinit var tvCargandoPanel: TextView
    private lateinit var tvVacioPanel: TextView
    private lateinit var scrollPanel: View
    private lateinit var contenedorPanel: ViewGroup

    private var pestanaActual = Pestana.NUEVOS
    private var pedidos: List<Pedido> = emptyList()

    private var listenerPedidos: ListenerRegistration? = null
    private var adminVerificado = false
    private var pantallaVisible = false

    // Para avisar solo de pedidos que son nuevos de verdad: los que ya se
    // habían visto (aunque hayas salido un momento a WhatsApp o al mapa y
    // regresado) no vuelven a sonar.
    private val idsConocidos = mutableSetOf<String>()
    private var yaCargoAntes = false
    private var ringtone: Ringtone? = null

    // Fotos de los clientes (se piden una sola vez por cliente).
    private val fotos = mutableMapOf<String, Bitmap>()
    private val fotosPedidas = mutableSetOf<String>()

    // Cada minuto se vuelve a dibujar la lista para que "hace 5 min" no se quede viejo.
    private val manejador = Handler(Looper.getMainLooper())
    private val refrescoDeTiempos = object : Runnable {
        override fun run() {
            if (pedidos.isNotEmpty()) mostrarLista()
            manejador.postDelayed(this, 60_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_panel_control)

        // Con el panel abierto la pantalla no se apaga sola: si se apagara,
        // no te enterarías de los pedidos nuevos.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        ajustarBarrasDelSistema()
        inicializarVistas()
        configurarAcciones()
        mostrarLista()

        // Primero se comprueba que esta cuenta sea administradora. Si no lo es,
        // la pantalla se cierra y ni siquiera se intenta leer los pedidos.
        AdminRepository.esAdmin { esAdministrador ->
            if (isFinishing || isDestroyed) return@esAdmin
            if (esAdministrador) {
                adminVerificado = true
                iniciarEscuchaSiCorresponde()
            } else {
                Toast.makeText(
                    this,
                    "Esta pantalla es solo para el administrador de El Fogón",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        pantallaVisible = true
        iniciarEscuchaSiCorresponde()
        manejador.postDelayed(refrescoDeTiempos, 60_000L)
    }

    override fun onPause() {
        super.onPause()
        pantallaVisible = false
        listenerPedidos?.remove()
        listenerPedidos = null
        manejador.removeCallbacks(refrescoDeTiempos)
        ringtone?.stop()
    }

    private fun ajustarBarrasDelSistema() {
        val vistaRaiz = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(vistaRaiz) { view, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
    }

    private fun inicializarVistas() {
        tvCerrarSesion = findViewById(R.id.tvCerrarSesion)
        tabNuevos = findViewById(R.id.tabNuevos)
        tabEnCurso = findViewById(R.id.tabEnCurso)
        tabEntregados = findViewById(R.id.tabEntregados)
        tvCargandoPanel = findViewById(R.id.tvCargandoPanel)
        tvVacioPanel = findViewById(R.id.tvVacioPanel)
        scrollPanel = findViewById(R.id.scrollPanel)
        contenedorPanel = findViewById(R.id.contenedorPanel)
    }

    private fun configurarAcciones() {
        tvCerrarSesion.setOnClickListener { preguntarCerrarSesion() }
        tabNuevos.setOnClickListener { cambiarPestana(Pestana.NUEVOS) }
        tabEnCurso.setOnClickListener { cambiarPestana(Pestana.EN_CURSO) }
        tabEntregados.setOnClickListener { cambiarPestana(Pestana.ENTREGADOS) }
    }

    private fun cambiarPestana(nueva: Pestana) {
        pestanaActual = nueva
        mostrarLista()
    }

    // Se pregunta antes para que un toque sin querer no te saque del panel.
    private fun preguntarCerrarSesion() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Quieres salir del panel de control? Ya no te sonarán las alertas de pedidos nuevos.")
            .setPositiveButton("Sí, salir") { _, _ -> cerrarSesion() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Cierra la sesión y regresa a la pantalla de inicio de sesión, sin dejar
    // pantallas del panel atrás.
    private fun cerrarSesion() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // ---------------------------------------------------------------------
    // Escuchar los pedidos
    // ---------------------------------------------------------------------

    // Se empieza a escuchar solo cuando la pantalla está visible Y ya se
    // confirmó que es administrador. Se llama desde los dos lados porque
    // cualquiera de los dos puede ocurrir primero.
    private fun iniciarEscuchaSiCorresponde() {
        if (!adminVerificado || !pantallaVisible || listenerPedidos != null) return

        listenerPedidos = PedidoRepository.escucharTodosLosPedidos(
            onCambio = { lista -> alRecibirPedidos(lista) },
            onError = { mensaje -> mostrarErrorDeCarga(mensaje) }
        )
    }

    private fun alRecibirPedidos(lista: List<Pedido>) {
        val pendientes = lista.filter { it.estado == PedidoRepository.ESTADO_PENDIENTE }
        val llegaronNuevos = pendientes.filter { it.id !in idsConocidos }

        if (!yaCargoAntes) {
            if (pendientes.isNotEmpty()) {
                val texto = if (pendientes.size == 1) "Tienes 1 pedido por confirmar"
                else "Tienes ${pendientes.size} pedidos por confirmar"
                Toast.makeText(this, texto, Toast.LENGTH_LONG).show()
            }
        } else if (llegaronNuevos.isNotEmpty()) {
            alertarPedidoNuevo(llegaronNuevos)
        }

        idsConocidos.clear()
        idsConocidos.addAll(lista.map { it.id })
        yaCargoAntes = true

        pedidos = lista
        tvCargandoPanel.visibility = View.GONE
        mostrarLista()
    }

    // Si algo falla (por ejemplo, las reglas todavía no están publicadas o
    // falta el documento admins/{tu uid}), se muestra el motivo en vez de
    // dejar la pantalla vacía.
    private fun mostrarErrorDeCarga(mensaje: String) {
        scrollPanel.visibility = View.GONE
        tvVacioPanel.visibility = View.GONE
        tvCargandoPanel.visibility = View.VISIBLE
        tvCargandoPanel.text = "No pudimos cargar los pedidos.\n\n" +
                "Revisa que ya publicaste las reglas nuevas de Firestore y que existe " +
                "el documento admins/(tu UID).\n\nDetalle técnico: $mensaje"
    }

    // Sonido de notificación + vibración + aviso en pantalla.
    @Suppress("DEPRECATION")
    private fun alertarPedidoNuevo(nuevos: List<Pedido>) {
        val texto = if (nuevos.size == 1) {
            "🔔 Pedido nuevo ${PedidoRepository.formatoNumero(nuevos[0].numero)}"
        } else {
            "🔔 ${nuevos.size} pedidos nuevos"
        }
        Toast.makeText(this, texto, Toast.LENGTH_LONG).show()

        try {
            ringtone?.stop()
            val sonido = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(applicationContext, sonido)
            ringtone?.play()
        } catch (e: Exception) {
            Log.w(ETIQUETA, "No se pudo reproducir el sonido", e)
        }

        try {
            val vibrador = getSystemService(VIBRATOR_SERVICE) as? Vibrator
            if (vibrador != null && vibrador.hasVibrator()) {
                val patron = longArrayOf(0, 400, 200, 400)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrador.vibrate(VibrationEffect.createWaveform(patron, -1))
                } else {
                    vibrador.vibrate(patron, -1)
                }
            }
        } catch (e: Exception) {
            Log.w(ETIQUETA, "No se pudo vibrar", e)
        }
    }

    // ---------------------------------------------------------------------
    // La lista
    // ---------------------------------------------------------------------

    private fun mostrarLista() {
        val nuevos = pedidos.filter { it.estado == PedidoRepository.ESTADO_PENDIENTE }
        val enCurso = pedidos.filter { it.estado in estadosEnCurso }
        val entregados = pedidos.filter { it.estado == PedidoRepository.ESTADO_ENTREGADO }

        pintarPestana(tabNuevos, "Nuevos (${nuevos.size})", pestanaActual == Pestana.NUEVOS)
        pintarPestana(tabEnCurso, "En curso (${enCurso.size})", pestanaActual == Pestana.EN_CURSO)
        pintarPestana(tabEntregados, "Entregados", pestanaActual == Pestana.ENTREGADOS)

        // Los nuevos y los que están en curso: el más viejo arriba (el que
        // lleva más tiempo esperando). Los entregados: el más reciente arriba,
        // y solo los últimos 30 para que la pantalla no se ponga pesada.
        val visibles = when (pestanaActual) {
            Pestana.NUEVOS -> nuevos.sortedBy { it.numero }
            Pestana.EN_CURSO -> enCurso.sortedBy { it.numero }
            Pestana.ENTREGADOS -> entregados.sortedByDescending { it.numero }.take(30)
        }

        contenedorPanel.removeAllViews()

        if (visibles.isEmpty()) {
            scrollPanel.visibility = View.GONE
            // Mientras todavía se está cargando, el "Cargando…" ya ocupa el lugar.
            if (tvCargandoPanel.visibility == View.VISIBLE) {
                tvVacioPanel.visibility = View.GONE
            } else {
                tvVacioPanel.visibility = View.VISIBLE
                tvVacioPanel.text = when (pestanaActual) {
                    Pestana.NUEVOS -> "No hay pedidos nuevos por confirmar 🎉"
                    Pestana.EN_CURSO -> "No hay pedidos en curso"
                    Pestana.ENTREGADOS -> "Todavía no hay pedidos entregados"
                }
            }
            return
        }

        tvVacioPanel.visibility = View.GONE
        scrollPanel.visibility = View.VISIBLE
        for (pedido in visibles) {
            contenedorPanel.addView(crearTarjeta(pedido))
        }
    }

    // La pestaña activa va en rojo sólido; las demás, con borde rojo.
    private fun pintarPestana(tab: TextView, texto: String, activa: Boolean) {
        tab.text = texto
        if (activa) {
            tab.setBackgroundResource(R.drawable.bg_button_red)
            tab.setTextColor(Color.WHITE)
        } else {
            tab.setBackgroundResource(R.drawable.bg_perfil_cerrar_sesion)
            tab.setTextColor(getColor(R.color.fogonRed))
        }
    }

    private fun crearTarjeta(pedido: Pedido): View {
        val tarjeta = LayoutInflater.from(this).inflate(R.layout.item_panel_pedido, contenedorPanel, false)

        tarjeta.findViewById<TextView>(R.id.tvNumeroPanel).text =
            "Pedido ${PedidoRepository.formatoNumero(pedido.numero)}"

        pintarGlobito(tarjeta.findViewById(R.id.tvEstadoPanel), pedido.estado)

        val fecha = pedido.fechaCreacion
        tarjeta.findViewById<TextView>(R.id.tvFechaPanel).text =
            if (fecha != null) "${formatearFechaHora(fecha)} · ${textoHace(fecha)}" else "Guardando…"

        // Cliente
        tarjeta.findViewById<TextView>(R.id.tvNombreCliente).text =
            pedido.nombreCliente.ifBlank { "Cliente sin nombre" }
        tarjeta.findViewById<TextView>(R.id.tvTelefonoCliente).text =
            pedido.telefonoCliente.ifBlank { "Sin teléfono" }
        ponerFoto(tarjeta.findViewById(R.id.ivFotoCliente), pedido.uid)

        val layoutContacto = tarjeta.findViewById<View>(R.id.layoutContacto)
        if (soloDigitos(pedido.telefonoCliente).isEmpty()) {
            layoutContacto.visibility = View.GONE
        } else {
            layoutContacto.visibility = View.VISIBLE
            tarjeta.findViewById<View>(R.id.btnLlamar).setOnClickListener { llamar(pedido) }
            tarjeta.findViewById<View>(R.id.btnWhatsApp).setOnClickListener { escribirPorWhatsApp(pedido) }
        }

        // Productos, total y pago
        tarjeta.findViewById<TextView>(R.id.tvProductosPanel).text = textoDeProductos(pedido)
        tarjeta.findViewById<TextView>(R.id.tvTotalPanel).text = formatoPrecio(pedido.total)

        var textoPago = pedido.metodoPago
        if (pedido.detallePago.isNotBlank()) textoPago += "\n" + pedido.detallePago
        if (pedido.pagoDePrueba) textoPago += "\nPago de prueba: no se cobró nada"
        tarjeta.findViewById<TextView>(R.id.tvPagoPanel).text = textoPago

        // Dirección y mapa
        tarjeta.findViewById<TextView>(R.id.tvDireccionPanel).text =
            "${pedido.zona} — ${pedido.direccionEspecifica}"

        val btnComoLlegar = tarjeta.findViewById<View>(R.id.btnComoLlegar)
        val latitud = pedido.latitud
        val longitud = pedido.longitud
        if (latitud != null && longitud != null) {
            btnComoLlegar.visibility = View.VISIBLE
            btnComoLlegar.setOnClickListener {
                abrirEnlace("https://www.google.com/maps/dir/?api=1&destination=$latitud,$longitud")
            }
        } else {
            btnComoLlegar.visibility = View.GONE
        }

        // El botón grande (o, si ya se entregó, la hora de entrega)
        val btnAccion = tarjeta.findViewById<TextView>(R.id.btnAccionPanel)
        val tvFinalizado = tarjeta.findViewById<TextView>(R.id.tvFinalizadoPanel)
        val siguiente = PedidoRepository.estadoSiguiente(pedido.estado)

        if (siguiente == null) {
            btnAccion.visibility = View.GONE
            tvFinalizado.visibility = View.VISIBLE
            val horaEntrega = pedido.horaEntregado
            tvFinalizado.text =
                if (horaEntrega != null) "✅ Entregado a las ${formatearHora(horaEntrega)}"
                else "✅ Entregado"
        } else {
            tvFinalizado.visibility = View.GONE
            btnAccion.visibility = View.VISIBLE
            btnAccion.text = textoDelBoton(pedido.estado)
            btnAccion.setOnClickListener { confirmarCambio(pedido, siguiente) }
        }

        return tarjeta
    }

    // ---------------------------------------------------------------------
    // Cambiar el estado
    // ---------------------------------------------------------------------

    // El texto del botón depende de en qué estado está AHORA el pedido.
    private fun textoDelBoton(estadoActual: String): String = when (estadoActual) {
        PedidoRepository.ESTADO_PENDIENTE -> "✅ Confirmar pedido"
        PedidoRepository.ESTADO_RECIBIDO -> "🍳 Empezar a preparar"
        PedidoRepository.ESTADO_PREPARANDO -> "🛵 Ya va en camino"
        else -> "📦 Marcar como entregado"
    }

    // Antes de avanzar se pregunta, porque un pedido no puede regresar a un
    // paso anterior: así un toque sin querer no cambia nada.
    private fun confirmarCambio(pedido: Pedido, estadoNuevo: String) {
        val pregunta = when (estadoNuevo) {
            PedidoRepository.ESTADO_RECIBIDO ->
                "¿Confirmas que recibiste este pedido? El cliente verá que ya lo recibiste."
            PedidoRepository.ESTADO_PREPARANDO ->
                "¿Empiezas a preparar este pedido? El cliente verá que está en preparación."
            PedidoRepository.ESTADO_CAMINO ->
                "¿Ya salió el repartidor con este pedido? El cliente verá que va en camino."
            else ->
                "¿Ya se entregó este pedido? El cliente verá que fue entregado."
        }

        AlertDialog.Builder(this)
            .setTitle("Pedido ${PedidoRepository.formatoNumero(pedido.numero)}")
            .setMessage("$pregunta\n\nEste paso no se puede deshacer.")
            .setPositiveButton("Sí, continuar") { _, _ -> aplicarCambio(pedido, estadoNuevo) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun aplicarCambio(pedido: Pedido, estadoNuevo: String) {
        PedidoRepository.cambiarEstado(pedido.id, estadoNuevo) { error ->
            // Si todo sale bien no hay que hacer nada: la lista se actualiza
            // sola porque ya estamos escuchando los cambios.
            if (error != null && !isFinishing && !isDestroyed) {
                AlertDialog.Builder(this)
                    .setTitle("No se pudo cambiar el estado")
                    .setMessage(
                        "El pedido ${PedidoRepository.formatoNumero(pedido.numero)} sigue igual. " +
                                "Revisa tu internet e inténtalo de nuevo.\n\nDetalle técnico: $error"
                    )
                    .setPositiveButton("Entendido", null)
                    .show()
            }
        }
    }

    // ---------------------------------------------------------------------
    // Cliente: foto, llamada y WhatsApp
    // ---------------------------------------------------------------------

    // Pone la foto del cliente si ya se descargó; si no, pone la silueta y la
    // pide una sola vez. Cuando llega, la lista se dibuja de nuevo y ya sale.
    private fun ponerFoto(imageView: ImageView, uid: String) {
        val foto = fotos[uid]
        if (foto != null) {
            imageView.setImageBitmap(foto)
            return
        }

        imageView.setImageResource(R.drawable.ic_perfil_avatar)
        if (uid.isBlank() || fotosPedidas.contains(uid)) return
        fotosPedidas.add(uid)

        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(uid)
            .get()
            .addOnSuccessListener { documento ->
                val bitmap = FotoPerfil.decodificar(documento.getString("fotoBase64"))
                if (bitmap != null && !isFinishing && !isDestroyed) {
                    fotos[uid] = bitmap
                    mostrarLista()
                }
            }
            .addOnFailureListener { error ->
                Log.w(ETIQUETA, "No se pudo cargar la foto del cliente", error)
            }
    }

    private fun llamar(pedido: Pedido) {
        val telefono = soloDigitos(pedido.telefonoCliente)
        abrirEnlace("tel:$telefono", Intent.ACTION_DIAL)
    }

    private fun escribirPorWhatsApp(pedido: Pedido) {
        val numero = numeroParaWhatsApp(pedido.telefonoCliente)
        val saludo = if (pedido.nombreCliente.isBlank()) "Hola" else "Hola ${pedido.nombreCliente}"
        val mensaje = "$saludo, te escribe El Fogón " +
                "sobre tu pedido ${PedidoRepository.formatoNumero(pedido.numero)}."
        abrirEnlace("https://wa.me/$numero?text=${Uri.encode(mensaje)}")
    }

    private fun abrirEnlace(enlace: String, accion: String = Intent.ACTION_VIEW) {
        try {
            startActivity(Intent(accion, Uri.parse(enlace)))
        } catch (e: Exception) {
            Log.w(ETIQUETA, "No se pudo abrir $enlace", e)
            Toast.makeText(this, "No se pudo abrir esa aplicación", Toast.LENGTH_SHORT).show()
        }
    }

    private fun soloDigitos(texto: String): String = texto.filter { it.isDigit() }

    // WhatsApp necesita el número con código de país, sin signos ni espacios.
    // Los números de El Salvador tienen 8 dígitos, así que se les antepone 503.
    private fun numeroParaWhatsApp(telefono: String): String {
        val digitos = soloDigitos(telefono)
        return if (digitos.length == 8) "503$digitos" else digitos
    }

    // ---------------------------------------------------------------------
    // Textos, colores, fecha y precio
    // ---------------------------------------------------------------------

    private fun textoDeProductos(pedido: Pedido): String {
        if (pedido.items.isEmpty()) return "Sin productos"
        return pedido.items.joinToString("\n") { item ->
            var linea = "${item.cantidad} × ${item.nombreProducto}"
            if (item.nombreTamano.isNotBlank()) linea += " · ${item.nombreTamano}"
            if (item.extras.isNotEmpty()) linea += "\n      Extras: " + item.extras.joinToString(", ")
            linea
        }
    }

    // Texto y colores del globito de estado: (texto, fondo, letras).
    private fun pintarGlobito(globito: TextView, estado: String) {
        val (texto, fondo, letras) = when (estado) {
            PedidoRepository.ESTADO_PENDIENTE -> Triple("NUEVO · Por confirmar", "#FFE0E0", "#C62828")
            PedidoRepository.ESTADO_RECIBIDO -> Triple("Recibido", "#FFF4D6", "#B26A00")
            PedidoRepository.ESTADO_PREPARANDO -> Triple("En preparación", "#FFE9CC", "#E65100")
            PedidoRepository.ESTADO_CAMINO -> Triple("En camino", "#E3F0FF", "#1565C0")
            else -> Triple("Entregado", "#E3F4E6", "#2E7D32")
        }
        globito.text = texto
        globito.backgroundTintList = ColorStateList.valueOf(Color.parseColor(fondo))
        globito.setTextColor(Color.parseColor(letras))
    }

    // "hace un momento", "hace 5 min", "hace 1 h 20 min"
    private fun textoHace(fecha: Date): String {
        val minutos = ((System.currentTimeMillis() - fecha.time) / 60_000L).toInt()
        return when {
            minutos < 1 -> "hace un momento"
            minutos < 60 -> "hace $minutos min"
            else -> "hace ${minutos / 60} h ${minutos % 60} min"
        }
    }

    // "11 de octubre de 2026 · 2:39 p.m."
    private fun formatearFechaHora(fecha: Date): String {
        val formatoFecha = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "SV"))
        formatoFecha.timeZone = zonaElSalvador
        return "${formatoFecha.format(fecha)} · ${formatearHora(fecha)}"
    }

    // "2:39 p.m." (se arma a mano para que siempre salga igual en cualquier celular)
    private fun formatearHora(fecha: Date): String {
        val calendario = Calendar.getInstance(zonaElSalvador)
        calendario.time = fecha

        val hora = calendario.get(Calendar.HOUR)
        val hora12 = if (hora == 0) 12 else hora
        val minutos = calendario.get(Calendar.MINUTE)
        val sufijo = if (calendario.get(Calendar.AM_PM) == Calendar.AM) "a.m." else "p.m."

        return String.format(Locale.US, "%d:%02d %s", hora12, minutos, sufijo)
    }

    private fun formatoPrecio(precio: Double): String = String.format(Locale.US, "$%.2f", precio)

    private companion object {
        const val ETIQUETA = "PanelControl"
    }
}