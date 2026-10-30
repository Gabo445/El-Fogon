package com.example.elfogonlogin.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.elfogonlogin.R
import com.example.elfogonlogin.controller.FotoPerfil
import com.example.elfogonlogin.controller.PerfilRepository
import com.example.elfogonlogin.controller.PerfilUsuario
import com.google.firebase.auth.FirebaseAuth

// Pestaña "Perfil". Los datos de arriba (foto, nombre, correo, teléfono) y el
// método de pago preferido salen de la cuenta y de Firestore, así que cambian
// solos cuando los editas. Se vuelven a leer cada vez que regresas a esta
// pantalla.
class PerfilActivity : AppCompatActivity() {

    companion object {
        // Si llega en true, al abrir el Perfil se muestra de una vez la ventana
        // "¿Cómo vas a pagar?". La usa la opción "Métodos de Pago" del menú
        // lateral del Inicio.
        const val EXTRA_ABRIR_METODOS_PAGO = "abrirMetodosPago"
    }

    // Una fila de la lista. "mostrarValor" es para la fila que enseña un dato a
    // la derecha (Métodos de pago enseña cuál tienes elegido).
    private class OpcionPerfil(
        val icono: Int,
        val titulo: String,
        val mostrarValor: Boolean = false,
        val accion: () -> Unit
    )

    private lateinit var frameFotoPerfil: View
    private lateinit var ivFotoPerfil: ImageView
    private lateinit var tvNombrePerfil: TextView
    private lateinit var tvCorreoPerfil: TextView
    private lateinit var filaTelefonoPerfil: View
    private lateinit var tvTelefonoPerfil: TextView
    private lateinit var contenedorOpciones: LinearLayout
    private lateinit var btnCerrarSesion: View

    private lateinit var navInicio: View
    private lateinit var navMenu: View
    private lateinit var navCarrito: View
    private lateinit var navPedidos: View
    private lateinit var navPerfil: View

    // El texto de la derecha de la fila "Métodos de pago".
    private var tvMetodoPagoActual: TextView? = null

    // Lo último que se leyó de Firestore (para los avisos de elegir/guardar).
    private var perfilActual: PerfilUsuario? = null

    // Abre la galería para elegir una foto. Cuando eliges una, aquí llega su
    // dirección (si cierras la galería sin elegir, llega null y no pasa nada).
    private val selectorDeFoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) procesarYGuardarFoto(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_perfil)

        ajustarBarrasDelSistema()
        inicializarVistas()
        construirOpciones()
        configurarAcciones()
        configurarBottomNav()
    }

    // Si el Perfil ya estaba abierto debajo, Android lo reutiliza en vez de abrir
    // otro, y el aviso de "abre métodos de pago" llega por aquí.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    // Se vuelve a leer cada vez que regresas (por ejemplo, después de editar
    // tus datos en "Mis datos"), así lo que ves siempre está al día.
    override fun onResume() {
        super.onResume()
        cargarPerfil()
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
        frameFotoPerfil = findViewById(R.id.frameFotoPerfil)
        ivFotoPerfil = findViewById(R.id.ivFotoPerfil)
        tvNombrePerfil = findViewById(R.id.tvNombrePerfil)
        tvCorreoPerfil = findViewById(R.id.tvCorreoPerfil)
        filaTelefonoPerfil = findViewById(R.id.filaTelefonoPerfil)
        tvTelefonoPerfil = findViewById(R.id.tvTelefonoPerfil)
        contenedorOpciones = findViewById(R.id.contenedorOpciones)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)

        navInicio = findViewById(R.id.navInicio)
        navMenu = findViewById(R.id.navMenu)
        navCarrito = findViewById(R.id.navCarrito)
        navPedidos = findViewById(R.id.navPedidos)
        navPerfil = findViewById(R.id.navPerfil)
    }

    // Las 7 opciones del diseño. Cada una dice qué hace al tocarla.
    private fun construirOpciones() {
        val opciones = listOf(
            OpcionPerfil(R.drawable.ic_perfil_persona, "Mis datos") { abrirMisDatos() },
            OpcionPerfil(R.drawable.ic_pago_tarjeta, "Métodos de pago", mostrarValor = true) { mostrarElegirMetodoPago() },
            OpcionPerfil(R.drawable.ic_perfil_bolsa, "Mis pedidos") { irA(PedidosActivity::class.java) },
            OpcionPerfil(R.drawable.ic_perfil_corazon, "Favoritos") { abrirFavoritos() },
            OpcionPerfil(R.drawable.ic_perfil_campana, "Notificaciones") { mostrarNotificaciones() },
            OpcionPerfil(R.drawable.ic_perfil_ayuda, "Ayuda") { abrirAyuda() },
            OpcionPerfil(R.drawable.ic_perfil_escudo, "Privacidad y seguridad") { abrirPrivacidad() }
        )

        for (opcion in opciones) {
            val fila = LayoutInflater.from(this).inflate(R.layout.item_perfil_opcion, contenedorOpciones, false)
            fila.findViewById<ImageView>(R.id.ivIconoOpcion).setImageResource(opcion.icono)
            fila.findViewById<TextView>(R.id.tvTituloOpcion).text = opcion.titulo
            fila.setOnClickListener { opcion.accion() }

            if (opcion.mostrarValor) {
                tvMetodoPagoActual = fila.findViewById(R.id.tvValorOpcion)
            }
            contenedorOpciones.addView(fila)
        }
    }

    private fun configurarAcciones() {
        // Tocar la foto abre la galería para cambiarla.
        frameFotoPerfil.setOnClickListener { tocarFoto() }

        // Tocar el nombre o el teléfono lleva a editarlos.
        tvNombrePerfil.setOnClickListener { abrirMisDatos() }
        filaTelefonoPerfil.setOnClickListener { abrirMisDatos() }

        btnCerrarSesion.setOnClickListener { cerrarSesion() }
    }

    // ---------------------------------------------------------------------
    // Los datos de arriba
    // ---------------------------------------------------------------------

    private fun cargarPerfil() {
        // Si llegaste desde "Métodos de Pago" del menú lateral, se anota y se borra
        // el aviso ahora mismo, para que la ventana se abra una sola vez.
        val abrirMetodosPago = intent.getBooleanExtra(EXTRA_ABRIR_METODOS_PAGO, false)
        if (abrirMetodosPago) intent.removeExtra(EXTRA_ABRIR_METODOS_PAGO)

        PerfilRepository.obtenerPerfil { perfil ->
            if (isFinishing || isDestroyed) return@obtenerPerfil
            perfilActual = perfil
            mostrarPerfil(perfil)

            // Se abre cuando ya llegaron tus datos, para que salga marcado el
            // método que tenías elegido.
            if (abrirMetodosPago) mostrarElegirMetodoPago()
        }
    }

    private fun mostrarPerfil(perfil: PerfilUsuario) {
        mostrarFoto(perfil.fotoBase64)

        tvNombrePerfil.text = if (perfil.nombre.isNotBlank()) perfil.nombre else "Sin nombre"
        tvCorreoPerfil.text = perfil.correo

        if (perfil.telefono.isNotBlank()) {
            tvTelefonoPerfil.text = "+503 " + PerfilRepository.formatoTelefono(perfil.telefono)
            tvTelefonoPerfil.setTextColor(getColor(R.color.fogonBlack))
        } else {
            // Todavía no lo ha puesto: se resalta para que lo agregue.
            tvTelefonoPerfil.text = "Agrega tu teléfono"
            tvTelefonoPerfil.setTextColor(getColor(R.color.fogonRed))
        }

        actualizarValorMetodoPago()
    }

    private fun abrirMisDatos() {
        startActivity(Intent(this, MisDatosActivity::class.java))
    }

    private fun abrirFavoritos() {
        startActivity(Intent(this, FavoritosActivity::class.java))
    }

    private fun abrirAyuda() {
        startActivity(Intent(this, AyudaActivity::class.java))
    }

    private fun abrirPrivacidad() {
        startActivity(Intent(this, PrivacidadActivity::class.java))
    }

    // ---------------------------------------------------------------------
    // Foto de perfil
    // ---------------------------------------------------------------------

    // Pone la foto guardada en el círculo; si no hay, la silueta de siempre.
    private fun mostrarFoto(fotoBase64: String?) {
        val foto = FotoPerfil.decodificar(fotoBase64)
        if (foto != null) {
            ivFotoPerfil.setImageBitmap(foto)
        } else {
            ivFotoPerfil.setImageResource(R.drawable.ic_perfil_avatar)
        }
    }

    // Sin foto: abre la galería directo. Con foto: pregunta si la cambias o la quitas.
    private fun tocarFoto() {
        val fotoActual = perfilActual?.fotoBase64
        if (fotoActual.isNullOrBlank()) {
            selectorDeFoto.launch("image/*")
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Foto de perfil")
            .setItems(arrayOf("Elegir otra foto", "Quitar foto")) { _, opcion ->
                if (opcion == 0) selectorDeFoto.launch("image/*") else quitarFotoActual()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Reducir la foto toma un momento (las de cámara son enormes), por eso se
    // hace en segundo plano para que la pantalla no se trabe.
    private fun procesarYGuardarFoto(uri: Uri) {
        Toast.makeText(this, "Guardando tu foto…", Toast.LENGTH_SHORT).show()

        Thread {
            val fotoBase64 = FotoPerfil.reducirYCodificar(this, uri)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (fotoBase64 == null) {
                    Toast.makeText(this, "No se pudo leer esa foto. Prueba con otra.", Toast.LENGTH_LONG).show()
                } else {
                    guardarFotoElegida(fotoBase64)
                }
            }
        }.start()
    }

    private fun guardarFotoElegida(fotoBase64: String) {
        PerfilRepository.guardarFoto(fotoBase64) { ok, error ->
            if (isFinishing || isDestroyed) return@guardarFoto
            if (ok) {
                perfilActual = perfilActual?.copy(fotoBase64 = fotoBase64)
                mostrarFoto(fotoBase64)
                Toast.makeText(this, "Foto actualizada", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No se pudo guardar la foto: ${error ?: "error desconocido"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun quitarFotoActual() {
        PerfilRepository.quitarFoto { ok, error ->
            if (isFinishing || isDestroyed) return@quitarFoto
            if (ok) {
                perfilActual = perfilActual?.copy(fotoBase64 = null)
                mostrarFoto(null)
                Toast.makeText(this, "Foto quitada", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No se pudo quitar la foto: ${error ?: "error desconocido"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ---------------------------------------------------------------------
    // Métodos de pago: elegir cómo vas a pagar
    // ---------------------------------------------------------------------

    private fun nombreCorto(metodo: MetodoPago): String = when (metodo) {
        MetodoPago.TARJETA -> "Tarjeta"
        MetodoPago.EFECTIVO -> "Efectivo"
        MetodoPago.TIGO_MONEY -> "Tigo Money"
        MetodoPago.CHIVO_WALLET -> "Chivo Wallet"
    }

    private fun actualizarValorMetodoPago() {
        val textView = tvMetodoPagoActual ?: return
        val metodo = perfilActual?.metodoPagoPreferido
        if (metodo == null) {
            textView.visibility = View.GONE
        } else {
            textView.visibility = View.VISIBLE
            textView.text = nombreCorto(metodo)
        }
    }

    // Lo que elijas aquí es lo que te aparece ya marcado cuando llegues a la
    // pantalla de Pago. Siempre puedes cambiarlo ahí mismo.
    private fun mostrarElegirMetodoPago() {
        val metodos = MetodoPago.values()
        val nombres = metodos.map { it.nombre }.toTypedArray()
        val inicial = perfilActual?.metodoPagoPreferido?.let { metodos.indexOf(it) } ?: -1
        var elegido = inicial

        AlertDialog.Builder(this)
            .setTitle("¿Cómo vas a pagar?")
            .setSingleChoiceItems(nombres, inicial) { _, posicion -> elegido = posicion }
            .setPositiveButton("Guardar") { _, _ ->
                if (elegido >= 0) guardarMetodoPago(metodos[elegido])
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarMetodoPago(metodo: MetodoPago) {
        PerfilRepository.guardarMetodoPagoPreferido(metodo) { ok, error ->
            if (isFinishing || isDestroyed) return@guardarMetodoPagoPreferido
            if (ok) {
                perfilActual = perfilActual?.copy(metodoPagoPreferido = metodo)
                actualizarValorMetodoPago()
                Toast.makeText(this, "Listo: al pagar te aparecerá ${metodo.nombre}", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "No se pudo guardar: ${error ?: "error desconocido"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ---------------------------------------------------------------------
    // Notificaciones
    // ---------------------------------------------------------------------

    private fun mostrarNotificaciones() {
        val opciones = arrayOf("Estado de mis pedidos", "Promociones y descuentos")
        val marcadas = booleanArrayOf(
            perfilActual?.notifPedidos ?: true,
            perfilActual?.notifPromociones ?: true
        )

        AlertDialog.Builder(this)
            .setTitle("Notificaciones")
            .setMultiChoiceItems(opciones, marcadas) { _, posicion, activada -> marcadas[posicion] = activada }
            .setPositiveButton("Guardar") { _, _ -> guardarNotificaciones(marcadas[0], marcadas[1]) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarNotificaciones(pedidos: Boolean, promociones: Boolean) {
        PerfilRepository.guardarNotificaciones(pedidos, promociones) { ok, error ->
            if (isFinishing || isDestroyed) return@guardarNotificaciones
            if (ok) {
                perfilActual = perfilActual?.copy(notifPedidos = pedidos, notifPromociones = promociones)
                Toast.makeText(
                    this,
                    "Preferencias guardadas. Los avisos al celular se activarán más adelante.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(this, "No se pudo guardar: ${error ?: "error desconocido"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ---------------------------------------------------------------------
    // Cerrar sesión y navegación
    // ---------------------------------------------------------------------

    // Cierra la sesión y borra todas las pantallas abiertas, para que al darle
    // "atrás" no se pueda volver a la app sin haber iniciado sesión.
    private fun cerrarSesion() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun irA(destino: Class<*>) {
        val intent = Intent(this, destino)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    private fun configurarBottomNav() {
        navInicio.setOnClickListener { irA(HomeActivity::class.java) }
        navMenu.setOnClickListener { irA(MenuActivity::class.java) }
        navCarrito.setOnClickListener { irA(CarritoActivity::class.java) }
        navPedidos.setOnClickListener { irA(PedidosActivity::class.java) }
        navPerfil.setOnClickListener { /* ya estamos en Perfil */ }
    }
}