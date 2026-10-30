package com.example.elfogonlogin.view

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.elfogonlogin.R
import com.example.elfogonlogin.controller.CarritoRepository
import com.example.elfogonlogin.controller.DetalleProducto
import com.example.elfogonlogin.controller.DetalleProductoRepository
import com.example.elfogonlogin.controller.FavoritosRepository
import com.example.elfogonlogin.controller.ItemCarrito
import com.example.elfogonlogin.controller.OpcionPersonalizacion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale

class DetalleProductoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PRODUCTO_ID = "extra_producto_id"
    }

    private lateinit var ivBack: View
    private lateinit var ivCart: FrameLayout
    private lateinit var tvCartBadge: TextView

    private lateinit var ivImagenProducto: ImageView
    private lateinit var ivFavorito: FrameLayout
    private lateinit var ivIconoFavorito: ImageView
    private lateinit var tvNombreProducto: TextView
    private lateinit var tvRating: TextView
    private lateinit var tvNumOpiniones: TextView
    private lateinit var tvDescripcion: TextView
    private lateinit var tvPrecioBase: TextView
    private lateinit var tvPersonalizaTitulo: TextView
    private lateinit var contenedorTamanos: LinearLayout
    private lateinit var contenedorExtras: LinearLayout
    private lateinit var tvTotalCarrito: TextView
    private lateinit var btnAgregarAlCarrito: View

    private lateinit var navInicio: View
    private lateinit var navMenu: View
    private lateinit var navCarrito: View
    private lateinit var navPedidos: View
    private lateinit var navPerfil: View

    private lateinit var detalle: DetalleProducto

    // Si este producto está en tus favoritos. Se actualiza solo con lo que hay
    // guardado en tu cuenta (ver onResume).
    private var esFavorito = false

    private var listenerCarrito: ListenerRegistration? = null
    private var listenerFavoritos: ListenerRegistration? = null
    private lateinit var tamanoSeleccionado: OpcionPersonalizacion
    private val radioButtonsTamanos = mutableListOf<Pair<OpcionPersonalizacion, RadioButton>>()
    private val checkboxesExtras = mutableListOf<Pair<OpcionPersonalizacion, CheckBox>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val productoId = intent.getStringExtra(EXTRA_PRODUCTO_ID)
        val detalleEncontrado = productoId?.let { DetalleProductoRepository.obtener(it) }
        if (detalleEncontrado == null) {
            finish()
            return
        }
        detalle = detalleEncontrado
        tamanoSeleccionado = detalle.tamanos.first()

        setContentView(R.layout.activity_detalle_producto)

        ajustarPorTeclado()
        inicializarVistas()
        mostrarInfoProducto()
        construirTamanos()
        construirExtras()
        actualizarTotal()
        configurarFavorito()
        configurarBotonAgregar()
        configurarBottomNav()
        configurarBotonAtras()
    }

    override fun onResume() {
        super.onResume()
        listenerCarrito = CarritoRepository.escucharCambios { actualizarBadgeCarrito() }

        // Escucha tus favoritos: si este producto está marcado, el corazón sale
        // rojo y relleno; si no, vacío. También cambia solo si lo quitas desde
        // la pantalla de Favoritos.
        if (::detalle.isInitialized) {
            listenerFavoritos = FavoritosRepository.escucharFavoritos { ids ->
                esFavorito = ids.contains(detalle.id)
                actualizarCorazon()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        listenerCarrito?.remove()
        listenerFavoritos?.remove()
    }

    private fun ajustarPorTeclado() {
        val vistaRaiz = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(vistaRaiz) { view, insets ->
            val teclado = insets.getInsets(WindowInsetsCompat.Type.ime())
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(barras.left, barras.top, barras.right, maxOf(teclado.bottom, barras.bottom))
            insets
        }
    }

    private fun inicializarVistas() {
        ivBack = findViewById(R.id.ivBack)
        ivCart = findViewById(R.id.ivCart)
        tvCartBadge = findViewById(R.id.tvCartBadge)

        ivImagenProducto = findViewById(R.id.ivImagenProducto)
        ivFavorito = findViewById(R.id.ivFavorito)
        ivIconoFavorito = findViewById(R.id.ivIconoFavorito)
        tvNombreProducto = findViewById(R.id.tvNombreProducto)
        tvRating = findViewById(R.id.tvRating)
        tvNumOpiniones = findViewById(R.id.tvNumOpiniones)
        tvDescripcion = findViewById(R.id.tvDescripcion)
        tvPrecioBase = findViewById(R.id.tvPrecioBase)
        tvPersonalizaTitulo = findViewById(R.id.tvPersonalizaTitulo)
        contenedorTamanos = findViewById(R.id.contenedorTamanos)
        contenedorExtras = findViewById(R.id.contenedorExtras)
        tvTotalCarrito = findViewById(R.id.tvTotalCarrito)
        btnAgregarAlCarrito = findViewById(R.id.btnAgregarAlCarrito)

        navInicio = findViewById(R.id.navInicio)
        navMenu = findViewById(R.id.navMenu)
        navCarrito = findViewById(R.id.navCarrito)
        navPedidos = findViewById(R.id.navPedidos)
        navPerfil = findViewById(R.id.navPerfil)
    }

    private fun mostrarInfoProducto() {
        ivImagenProducto.setImageResource(detalle.imagenResId)
        tvNombreProducto.text = detalle.nombre
        tvRating.text = detalle.rating.toString()
        tvNumOpiniones.text = "(${detalle.numOpiniones} opiniones)"
        tvDescripcion.text = detalle.descripcion
        tvPrecioBase.text = formatoPrecio(detalle.tamanos.first().precio)
        tvPersonalizaTitulo.text = "Personaliza tu ${detalle.nombre.lowercase()}"
    }

    private fun construirTamanos() {
        contenedorTamanos.removeAllViews()
        radioButtonsTamanos.clear()
        for ((index, opcion) in detalle.tamanos.withIndex()) {
            val fila = LayoutInflater.from(this).inflate(R.layout.menu_item_tamano, contenedorTamanos, false)
            val radio = fila.findViewById<RadioButton>(R.id.rbTamano)
            val tvNombreTamano = fila.findViewById<TextView>(R.id.tvNombreTamano)
            val tvPrecioTamano = fila.findViewById<TextView>(R.id.tvPrecioTamano)

            tvNombreTamano.text = opcion.nombre
            tvPrecioTamano.text = formatoPrecio(opcion.precio)
            radio.isChecked = index == 0

            radio.setOnClickListener { seleccionarTamano(opcion) }
            fila.setOnClickListener { seleccionarTamano(opcion) }

            radioButtonsTamanos.add(opcion to radio)
            contenedorTamanos.addView(fila)
        }
    }

    private fun seleccionarTamano(opcion: OpcionPersonalizacion) {
        tamanoSeleccionado = opcion
        for ((opcionRadio, radio) in radioButtonsTamanos) {
            radio.isChecked = opcionRadio == opcion
        }
        actualizarTotal()
    }

    private fun construirExtras() {
        contenedorExtras.removeAllViews()
        checkboxesExtras.clear()
        for (opcion in detalle.extras) {
            val fila = LayoutInflater.from(this).inflate(R.layout.menu_item_extra, contenedorExtras, false)
            val checkbox = fila.findViewById<CheckBox>(R.id.cbExtra)
            val tvNombreExtra = fila.findViewById<TextView>(R.id.tvNombreExtra)
            val tvPrecioExtra = fila.findViewById<TextView>(R.id.tvPrecioExtra)

            tvNombreExtra.text = opcion.nombre
            tvPrecioExtra.text = "+" + formatoPrecio(opcion.precio)
            checkbox.isChecked = detalle.extrasMarcadosPorDefecto.contains(opcion.nombre)

            checkbox.setOnCheckedChangeListener { _, _ -> actualizarTotal() }
            fila.setOnClickListener { checkbox.isChecked = !checkbox.isChecked }

            checkboxesExtras.add(opcion to checkbox)
            contenedorExtras.addView(fila)
        }
    }

    private fun actualizarTotal() {
        tvTotalCarrito.text = formatoPrecio(calcularTotal())
    }

    private fun calcularTotal(): Double {
        var total = tamanoSeleccionado.precio
        for ((opcion, checkbox) in checkboxesExtras) {
            if (checkbox.isChecked) {
                total += opcion.precio
            }
        }
        return total
    }

    // ---------------------------------------------------------------------
    // El corazón de favoritos
    // ---------------------------------------------------------------------

    // El corazón de arriba a la derecha de la foto. Al tocarlo, el producto se
    // guarda en (o se quita de) tus favoritos, dentro de tu cuenta, así que
    // sigue marcado aunque cierres la app.
    private fun configurarFavorito() {
        // El ícono se ajusta por código para que siempre se vea bien: un corazón
        // de 22dp centrado dentro del botón blanco.
        val lado = (22 * resources.displayMetrics.density).toInt()
        ivIconoFavorito.layoutParams = FrameLayout.LayoutParams(lado, lado, Gravity.CENTER)
        ivIconoFavorito.setPadding(0, 0, 0, 0)
        ivIconoFavorito.scaleType = ImageView.ScaleType.FIT_CENTER
        actualizarCorazon()

        ivFavorito.setOnClickListener {
            val nuevoEstado = !esFavorito

            // Se refleja de una vez en pantalla...
            esFavorito = nuevoEstado
            actualizarCorazon()
            Toast.makeText(
                this,
                if (nuevoEstado) "Agregado a favoritos" else "Quitado de favoritos",
                Toast.LENGTH_SHORT
            ).show()

            // ...y se guarda en tu cuenta. Si falla, vuelve a como estaba.
            FavoritosRepository.marcar(detalle.id, nuevoEstado) { ok, error ->
                if (!ok && !isFinishing && !isDestroyed) {
                    esFavorito = !nuevoEstado
                    actualizarCorazon()
                    Toast.makeText(
                        this,
                        "No se pudo guardar tu favorito: ${error ?: "error desconocido"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Corazón rojo y relleno = es favorito. Corazón gris y vacío = no lo es.
    private fun actualizarCorazon() {
        if (esFavorito) {
            ivIconoFavorito.setImageResource(R.drawable.ic_corazon_lleno)
            ivIconoFavorito.imageTintList = ColorStateList.valueOf(getColor(R.color.fogonRed))
        } else {
            ivIconoFavorito.setImageResource(R.drawable.ic_perfil_corazon)
            ivIconoFavorito.imageTintList = ColorStateList.valueOf(getColor(R.color.fogonGrayText))
        }
    }

    // Agrega al carrito con el tamaño y los extras que la persona eligió
    // en esta pantalla — se guarda en Firestore, dentro de la cuenta.
    private fun configurarBotonAgregar() {
        btnAgregarAlCarrito.setOnClickListener {
            val extrasElegidos = checkboxesExtras
                .filter { (_, checkbox) -> checkbox.isChecked }
                .map { (opcion, _) -> opcion }

            CarritoRepository.agregarItem(
                ItemCarrito(
                    nombreProducto = detalle.nombre,
                    detalleId = detalle.id,
                    nombreTamano = tamanoSeleccionado.nombre,
                    precioTamano = tamanoSeleccionado.precio,
                    extrasSeleccionados = extrasElegidos
                )
            )
            actualizarBadgeCarrito()

            val nombreConTamano = "${detalle.nombre} (${tamanoSeleccionado.nombre})"
            Toast.makeText(
                this,
                "Agregado al carrito: $nombreConTamano - ${formatoPrecio(calcularTotal())}",
                Toast.LENGTH_SHORT
            ).show()
        }
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

    private fun irAInicio() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    private fun irAMenu() {
        val intent = Intent(this, MenuActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    private fun irACarrito() {
        val intent = Intent(this, CarritoActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    private fun irAPedidos() {
        val intent = Intent(this, PedidosActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    private fun irAPerfil() {
        val intent = Intent(this, PerfilActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    private fun configurarBottomNav() {
        navInicio.setOnClickListener { irAInicio() }
        navMenu.setOnClickListener { irAMenu() }
        navCarrito.setOnClickListener { irACarrito() }

        navPedidos.setOnClickListener { irAPedidos() }

        navPerfil.setOnClickListener { irAPerfil() }

        ivCart.setOnClickListener { irACarrito() }
    }

    private fun configurarBotonAtras() {
        ivBack.setOnClickListener { finish() }
    }
}