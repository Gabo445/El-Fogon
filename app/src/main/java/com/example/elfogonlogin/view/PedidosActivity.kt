package com.example.elfogonlogin.view

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.elfogonlogin.R
import com.example.elfogonlogin.controller.CarritoRepository
import com.example.elfogonlogin.controller.ItemPedido
import com.example.elfogonlogin.controller.Pedido
import com.example.elfogonlogin.controller.PedidoRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Pestaña "Pedidos": el historial del usuario, con el pedido más reciente
// arriba. Cada pedido se despliega con "Ver detalles" para ver su seguimiento
// y sus productos. Todo se actualiza solo: si el campo "estado" de un pedido
// cambia en Firestore, aquí cambia al instante, sin recargar.
class PedidosActivity : AppCompatActivity() {

    // Un paso del seguimiento. "codigo" es el valor que se guarda en el campo
    // "estado" del pedido en Firestore.
    private class PasoSeguimiento(
        val codigo: String,
        val titulo: String,
        val textoActual: String,
        val textoChip: String
    )

    // En orden: el primero es el primer paso y el último es el final.
    // "textoChip" es lo que dice el globito de arriba a la derecha de cada pedido:
    // "Por confirmar" mientras El Fogón no haya dado el OK, "Pendiente" mientras
    // el pedido siga en curso, y "Recibido" cuando ya se completó la entrega
    // (es decir, cuando el cliente ya lo recibió).
    private val pasos = listOf(
        PasoSeguimiento(PedidoRepository.ESTADO_PENDIENTE, "Pedido enviado", "Esperando que El Fogón confirme tu pedido", "Por confirmar"),
        PasoSeguimiento(PedidoRepository.ESTADO_RECIBIDO, "Pedido recibido", "El Fogón ya recibió tu pedido", "Pendiente"),
        PasoSeguimiento(PedidoRepository.ESTADO_PREPARANDO, "En preparación", "Estamos preparando tu pedido", "Pendiente"),
        PasoSeguimiento(PedidoRepository.ESTADO_CAMINO, "En camino", "Tu pedido está en camino", "Pendiente"),
        PasoSeguimiento(PedidoRepository.ESTADO_ENTREGADO, "Entregado", "Tu pedido fue entregado", "Recibido")
    )

    // Colores del globito de estado: (fondo, letras), uno por paso.
    // Amarillo = pendiente (los 4 primeros pasos), verde = recibido (entregado).
    private val coloresChip = listOf(
        "#FFF4D6" to "#B26A00",
        "#FFF4D6" to "#B26A00",
        "#FFF4D6" to "#B26A00",
        "#FFF4D6" to "#B26A00",
        "#E3F4E6" to "#2E7D32"
    )

    // Ícono de cada paso cuando es el paso actual (círculo naranja).
    private val iconosPasoActual = listOf(
        R.drawable.ic_receipt,
        R.drawable.ic_pago_check,
        R.drawable.ic_pedido_cocina,
        R.drawable.ic_pago_moto,
        R.drawable.ic_pago_check
    )

    private val colorLineaHecha = Color.parseColor("#2E9E44")
    private val colorLineaPendiente = Color.parseColor("#D9D9D9")

    // Siempre en hora de El Salvador, sin importar la zona del celular.
    private val zonaElSalvador: TimeZone = TimeZone.getTimeZone("America/El_Salvador")

    private lateinit var ivBack: View
    private lateinit var ivCart: FrameLayout
    private lateinit var tvCartBadge: TextView
    private lateinit var tvCargandoPedidos: TextView
    private lateinit var scrollPedidos: View
    private lateinit var contenedorPedidos: ViewGroup
    private lateinit var layoutSinPedidos: View
    private lateinit var btnIrAlMenuPedidos: View

    private lateinit var navInicio: View
    private lateinit var navMenu: View
    private lateinit var navCarrito: View
    private lateinit var navPedidos: View
    private lateinit var navPerfil: View

    // Los pedidos que tienen "Ver detalles" abierto. Se recuerdan aquí para
    // que, cuando llegue un cambio (por ejemplo cambia el estado), la lista se
    // vuelva a armar sin cerrarte lo que tenías abierto.
    private val pedidosAbiertos = mutableSetOf<String>()

    private var listenerPedidos: ListenerRegistration? = null
    private var listenerCarrito: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_pedidos)

        ajustarBarrasDelSistema()
        inicializarVistas()
        configurarAcciones()
        configurarBottomNav()
    }

    override fun onResume() {
        super.onResume()

        listenerCarrito = CarritoRepository.escucharCambios { actualizarBadgeCarrito() }
        actualizarBadgeCarrito()

        listenerPedidos = PedidoRepository.escucharMisPedidos(
            onCambio = { pedidos -> mostrarPedidos(pedidos) },
            onError = { mensaje -> mostrarErrorDeCarga(mensaje) }
        )
    }

    override fun onPause() {
        super.onPause()
        listenerCarrito?.remove()
        listenerPedidos?.remove()
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
        ivBack = findViewById(R.id.ivBack)
        ivCart = findViewById(R.id.ivCart)
        tvCartBadge = findViewById(R.id.tvCartBadge)
        tvCargandoPedidos = findViewById(R.id.tvCargandoPedidos)
        scrollPedidos = findViewById(R.id.scrollPedidos)
        contenedorPedidos = findViewById(R.id.contenedorPedidos)
        layoutSinPedidos = findViewById(R.id.layoutSinPedidos)
        btnIrAlMenuPedidos = findViewById(R.id.btnIrAlMenuPedidos)

        navInicio = findViewById(R.id.navInicio)
        navMenu = findViewById(R.id.navMenu)
        navCarrito = findViewById(R.id.navCarrito)
        navPedidos = findViewById(R.id.navPedidos)
        navPerfil = findViewById(R.id.navPerfil)
    }

    private fun configurarAcciones() {
        ivBack.setOnClickListener { finish() }
        ivCart.setOnClickListener { irA(CarritoActivity::class.java) }
        btnIrAlMenuPedidos.setOnClickListener { irA(MenuActivity::class.java) }
    }

    // ---------------------------------------------------------------------
    // La lista
    // ---------------------------------------------------------------------

    private fun mostrarPedidos(pedidos: List<Pedido>) {
        tvCargandoPedidos.visibility = View.GONE
        contenedorPedidos.removeAllViews()

        if (pedidos.isEmpty()) {
            scrollPedidos.visibility = View.GONE
            layoutSinPedidos.visibility = View.VISIBLE
            return
        }

        layoutSinPedidos.visibility = View.GONE
        scrollPedidos.visibility = View.VISIBLE

        // Ya vienen ordenados: el más reciente primero.
        for (pedido in pedidos) {
            contenedorPedidos.addView(crearTarjetaPedido(pedido))
        }
    }

    // Si algo falla al cargar (por ejemplo, las reglas de Firestore todavía no
    // están publicadas), se muestra el motivo en vez de dejar la pantalla vacía.
    private fun mostrarErrorDeCarga(mensaje: String) {
        scrollPedidos.visibility = View.GONE
        layoutSinPedidos.visibility = View.GONE
        tvCargandoPedidos.visibility = View.VISIBLE
        tvCargandoPedidos.text = "No pudimos cargar tus pedidos.\n\nDetalle técnico: $mensaje"
    }

    private fun crearTarjetaPedido(pedido: Pedido): View {
        val tarjeta = LayoutInflater.from(this).inflate(R.layout.item_pedido, contenedorPedidos, false)
        val indiceActual = indiceDelEstado(pedido.estado)

        tarjeta.findViewById<TextView>(R.id.tvNumeroPedido).text =
            "Pedido ${PedidoRepository.formatoNumero(pedido.numero)}"
        tarjeta.findViewById<TextView>(R.id.tvFechaPedido).text =
            pedido.fechaCreacion?.let { formatearFechaHora(it) } ?: "Guardando…"

        val cantidadProductos = pedido.items.sumOf { it.cantidad }
        tarjeta.findViewById<TextView>(R.id.tvResumenProductos).text =
            if (cantidadProductos == 1) "1 producto" else "$cantidadProductos productos"
        tarjeta.findViewById<TextView>(R.id.tvTotalPedido).text = formatoPrecio(pedido.total)

        pintarGlobitoDeEstado(tarjeta.findViewById(R.id.tvEstadoChip), indiceActual)
        llenarDetalles(tarjeta, pedido, indiceActual)

        // "Ver detalles" abre y cierra la zona desplegable de esta tarjeta.
        val layoutDetalles = tarjeta.findViewById<View>(R.id.layoutDetalles)
        val btnVerDetalles = tarjeta.findViewById<TextView>(R.id.btnVerDetalles)

        aplicarEstadoDeDetalles(layoutDetalles, btnVerDetalles, pedidosAbiertos.contains(pedido.id))

        btnVerDetalles.setOnClickListener {
            val ahoraAbierto = !pedidosAbiertos.contains(pedido.id)
            if (ahoraAbierto) pedidosAbiertos.add(pedido.id) else pedidosAbiertos.remove(pedido.id)

            TransitionManager.beginDelayedTransition(contenedorPedidos)
            aplicarEstadoDeDetalles(layoutDetalles, btnVerDetalles, ahoraAbierto)
        }

        return tarjeta
    }

    private fun aplicarEstadoDeDetalles(layoutDetalles: View, boton: TextView, abierto: Boolean) {
        layoutDetalles.visibility = if (abierto) View.VISIBLE else View.GONE
        boton.text = if (abierto) "Ocultar detalles ▴" else "Ver detalles ▾"
    }

    // En qué paso va el pedido (0 al 4). Si el estado tiene un valor raro
    // (por ejemplo, un error al escribirlo a mano en Firebase), cuenta como el
    // primero.
    private fun indiceDelEstado(estado: String): Int {
        val indice = pasos.indexOfFirst { it.codigo == estado }
        return if (indice >= 0) indice else 0
    }

    private fun pintarGlobitoDeEstado(globito: TextView, indice: Int) {
        val (fondo, letras) = coloresChip[indice]
        globito.text = pasos[indice].textoChip
        globito.backgroundTintList = ColorStateList.valueOf(Color.parseColor(fondo))
        globito.setTextColor(Color.parseColor(letras))
    }

    // ---------------------------------------------------------------------
    // La zona desplegable
    // ---------------------------------------------------------------------

    private fun llenarDetalles(tarjeta: View, pedido: Pedido, indiceActual: Int) {
        // Seguimiento: los 5 pasos
        val contenedorPasos = tarjeta.findViewById<ViewGroup>(R.id.contenedorPasos)
        for (indice in pasos.indices) {
            contenedorPasos.addView(crearFilaDePaso(contenedorPasos, indice, indiceActual, pedido))
        }

        // Mientras el pedido no esté entregado: un espacio para el mapa en vivo
        // (que se hará con el panel de control) y el botón para ver dónde se entrega.
        val enCurso = indiceActual < pasos.lastIndex
        tarjeta.findViewById<View>(R.id.layoutMapaPlaceholder).visibility =
            if (enCurso) View.VISIBLE else View.GONE

        val btnVerUbicacion = tarjeta.findViewById<View>(R.id.btnVerUbicacion)
        val latitud = pedido.latitud
        val longitud = pedido.longitud
        if (enCurso && latitud != null && longitud != null) {
            btnVerUbicacion.visibility = View.VISIBLE
            btnVerUbicacion.setOnClickListener {
                val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitud,$longitud")
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        } else {
            btnVerUbicacion.visibility = View.GONE
        }

        // Productos
        val contenedorProductos = tarjeta.findViewById<ViewGroup>(R.id.contenedorProductosPedido)
        for (item in pedido.items) {
            contenedorProductos.addView(crearFilaDeProducto(contenedorProductos, item))
        }

        // Totales
        tarjeta.findViewById<TextView>(R.id.tvSubtotalPedido).text = formatoPrecio(pedido.subtotal)
        if (pedido.descuento > 0) {
            tarjeta.findViewById<View>(R.id.rowDescuentoPedido).visibility = View.VISIBLE
            tarjeta.findViewById<TextView>(R.id.tvEtiquetaDescuentoPedido).text =
                if (pedido.codigoPromo != null) "Descuento (${pedido.codigoPromo})" else "Descuento"
            tarjeta.findViewById<TextView>(R.id.tvDescuentoPedido).text = "-" + formatoPrecio(pedido.descuento)
        }
        tarjeta.findViewById<TextView>(R.id.tvEntregaPedido).text = formatoPrecio(pedido.entrega)
        tarjeta.findViewById<TextView>(R.id.tvImpuestosPedido).text = formatoPrecio(pedido.impuestos)
        tarjeta.findViewById<TextView>(R.id.tvTotalDetallePedido).text = formatoPrecio(pedido.total)

        // Pago y entrega
        var textoPago = pedido.metodoPago
        if (pedido.detallePago.isNotBlank()) textoPago += "\n" + pedido.detallePago
        if (pedido.pagoDePrueba) textoPago += "\nPago de prueba: no se cobró nada"
        tarjeta.findViewById<TextView>(R.id.tvMetodoPagoPedido).text = textoPago

        tarjeta.findViewById<TextView>(R.id.tvDireccionPedido).text =
            "${pedido.zona} — ${pedido.direccionEspecifica}"
    }

    // Un paso del seguimiento. Los que ya pasaron salen verdes con palomita, el
    // actual en naranja con su ícono, y los que faltan con el círculo vacío.
    private fun crearFilaDePaso(padre: ViewGroup, indice: Int, indiceActual: Int, pedido: Pedido): View {
        val fila = LayoutInflater.from(this).inflate(R.layout.item_paso_pedido, padre, false)
        val paso = pasos[indice]

        val flIndicador = fila.findViewById<FrameLayout>(R.id.flIndicador)
        val ivIndicador = fila.findViewById<ImageView>(R.id.ivIndicador)
        val viewLinea = fila.findViewById<View>(R.id.viewLinea)
        val tvTitulo = fila.findViewById<TextView>(R.id.tvTituloPaso)
        val tvHora = fila.findViewById<TextView>(R.id.tvHoraPaso)
        val tvSubtitulo = fila.findViewById<TextView>(R.id.tvSubtituloPaso)

        tvTitulo.text = paso.titulo

        val esUltimo = indice == pasos.lastIndex
        // El último paso (Entregado), cuando se alcanza, ya no es "en curso": es la meta cumplida.
        val completado = indice < indiceActual || (esUltimo && indice == indiceActual)
        val enCurso = indice == indiceActual && !esUltimo

        when {
            completado -> {
                flIndicador.setBackgroundResource(R.drawable.bg_pedido_paso_hecho)
                ivIndicador.visibility = View.VISIBLE
                ivIndicador.setImageResource(R.drawable.ic_pago_check)
                tvSubtitulo.visibility = View.GONE
            }
            enCurso -> {
                flIndicador.setBackgroundResource(R.drawable.bg_pedido_paso_actual)
                ivIndicador.visibility = View.VISIBLE
                ivIndicador.setImageResource(iconosPasoActual[indice])
                tvSubtitulo.visibility = View.VISIBLE
                tvSubtitulo.text = paso.textoActual
            }
            else -> {
                flIndicador.setBackgroundResource(R.drawable.bg_pedido_paso_pendiente)
                ivIndicador.visibility = View.GONE
                tvSubtitulo.visibility = View.VISIBLE
                tvSubtitulo.text = "Pendiente"
            }
        }

        // La hora de cada paso, solo si ya se alcanzó y se tiene guardada.
        val hora = horaDelPaso(pedido, indice)
        if ((completado || enCurso) && hora != null) {
            tvHora.visibility = View.VISIBLE
            tvHora.text = formatearHora(hora)
        } else {
            tvHora.visibility = View.GONE
        }

        // La línea que baja al siguiente paso: verde si ese paso ya se alcanzó.
        if (esUltimo) {
            viewLinea.visibility = View.INVISIBLE
        } else {
            viewLinea.visibility = View.VISIBLE
            viewLinea.setBackgroundColor(if (indice < indiceActual) colorLineaHecha else colorLineaPendiente)
        }

        return fila
    }

    private fun horaDelPaso(pedido: Pedido, indice: Int): Date? = when (indice) {
        0 -> pedido.fechaCreacion
        1 -> pedido.horaRecibido
        2 -> pedido.horaPreparando
        3 -> pedido.horaCamino
        else -> pedido.horaEntregado
    }

    private fun crearFilaDeProducto(padre: ViewGroup, item: ItemPedido): View {
        val fila = LayoutInflater.from(this).inflate(R.layout.item_pedido_producto, padre, false)

        var nombre = "${item.cantidad} × ${item.nombreProducto}"
        if (item.nombreTamano.isNotBlank()) nombre += " · ${item.nombreTamano}"
        fila.findViewById<TextView>(R.id.tvProductoPedido).text = nombre
        fila.findViewById<TextView>(R.id.tvPrecioProductoPedido).text = formatoPrecio(item.subtotal)

        val tvExtras = fila.findViewById<TextView>(R.id.tvExtrasProductoPedido)
        if (item.extras.isEmpty()) {
            tvExtras.visibility = View.GONE
        } else {
            tvExtras.visibility = View.VISIBLE
            tvExtras.text = "Extras: " + item.extras.joinToString(", ")
        }

        return fila
    }

    // ---------------------------------------------------------------------
    // Fecha, hora y precio
    // ---------------------------------------------------------------------

    // "4 de octubre de 2026 · 2:39 p.m."
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

    private fun actualizarBadgeCarrito() {
        val total = CarritoRepository.obtenerTotalItems()
        if (total > 0) {
            tvCartBadge.visibility = View.VISIBLE
            tvCartBadge.text = total.toString()
        } else {
            tvCartBadge.visibility = View.GONE
        }
    }

    // ---------------------------------------------------------------------
    // Navegación
    // ---------------------------------------------------------------------

    private fun irA(destino: Class<*>) {
        val intent = Intent(this, destino)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    private fun configurarBottomNav() {
        navInicio.setOnClickListener { irA(HomeActivity::class.java) }
        navMenu.setOnClickListener { irA(MenuActivity::class.java) }
        navCarrito.setOnClickListener { irA(CarritoActivity::class.java) }
        navPedidos.setOnClickListener { /* ya estamos en Pedidos */ }
        navPerfil.setOnClickListener { irA(PerfilActivity::class.java) }
    }
}