package com.example.elfogonlogin.view

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import com.example.elfogonlogin.R
import com.example.elfogonlogin.controller.CarritoRepository
import com.example.elfogonlogin.controller.DetalleProductoRepository
import com.example.elfogonlogin.controller.ItemCarrito
import com.example.elfogonlogin.controller.ProductosRepository
import com.example.elfogonlogin.model.Producto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale
import kotlin.collections.iterator

class MenuActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CATEGORIA = "extra_categoria"
    }

    private data class Pestana(val contenedor: View, val icono: ImageView, val texto: TextView, val subrayado: View)

    private lateinit var ivBack: View
    private lateinit var ivCart: FrameLayout
    private lateinit var tvCartBadge: TextView
    private lateinit var etBuscar: EditText
    private lateinit var tvCategoriaSeleccionada: TextView
    private lateinit var tvOrdenar: TextView
    private lateinit var tvMensajeCategoria: TextView
    private lateinit var layoutSinProductos: View
    private lateinit var contenedorProductos: LinearLayout

    private lateinit var navInicio: View
    private lateinit var navMenu: View
    private lateinit var navCarrito: View
    private lateinit var navPedidos: View
    private lateinit var navPerfil: View

    private lateinit var pestanas: Map<String, Pestana>
    private var categoriaActual: String = "Hamburguesas"
    private var listenerCarrito: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_menu)

        ajustarPorTeclado()
        inicializarVistas()
        configurarPestanas()
        configurarBusqueda()
        configurarBottomNav()
        configurarBotonAtras()

        categoriaActual = intent.getStringExtra(EXTRA_CATEGORIA) ?: "Hamburguesas"
        seleccionarCategoria(categoriaActual)
    }

    override fun onResume() {
        super.onResume()
        listenerCarrito = CarritoRepository.escucharCambios { actualizarBadgeCarrito() }
    }

    override fun onPause() {
        super.onPause()
        listenerCarrito?.remove()
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
        etBuscar = findViewById(R.id.etBuscar)
        tvCategoriaSeleccionada = findViewById(R.id.tvCategoriaSeleccionada)
        tvOrdenar = findViewById(R.id.tvOrdenar)
        tvMensajeCategoria = findViewById(R.id.tvMensajeCategoria)
        layoutSinProductos = findViewById(R.id.layoutSinProductos)
        contenedorProductos = findViewById(R.id.contenedorProductos)

        navInicio = findViewById(R.id.navInicio)
        navMenu = findViewById(R.id.navMenu)
        navCarrito = findViewById(R.id.navCarrito)
        navPedidos = findViewById(R.id.navPedidos)
        navPerfil = findViewById(R.id.navPerfil)

        pestanas = mapOf(
            "Hamburguesas" to Pestana(
                findViewById(R.id.tabHamburguesas), findViewById(R.id.ivTabHamburguesas), findViewById(
                    R.id.tvTabHamburguesas), findViewById(R.id.underlineHamburguesas)
            ),
            "Pizzas" to Pestana(
                findViewById(R.id.tabPizzas), findViewById(R.id.ivTabPizzas), findViewById(R.id.tvTabPizzas), findViewById(
                    R.id.underlinePizzas)
            ),
            "Tacos" to Pestana(
                findViewById(R.id.tabTacos), findViewById(R.id.ivTabTacos), findViewById(R.id.tvTabTacos), findViewById(
                    R.id.underlineTacos)
            ),
            "Bebidas" to Pestana(
                findViewById(R.id.tabBebidas), findViewById(R.id.ivTabBebidas), findViewById(R.id.tvTabBebidas), findViewById(
                    R.id.underlineBebidas)
            ),
            "Postres" to Pestana(
                findViewById(R.id.tabPostres), findViewById(R.id.ivTabPostres), findViewById(R.id.tvTabPostres), findViewById(
                    R.id.underlinePostres)
            ),
            "Burritos" to Pestana(
                findViewById(R.id.tabBurritos), findViewById(R.id.ivTabBurritos), findViewById(R.id.tvTabBurritos), findViewById(
                    R.id.underlineBurritos)
            ),
            "Tortas" to Pestana(
                findViewById(R.id.tabTortas), findViewById(R.id.ivTabTortas), findViewById(R.id.tvTabTortas), findViewById(
                    R.id.underlineTortas)
            )
        )

        // Reutiliza las mismas fotos de categoría que ya pusiste en el Home
        pestanas["Hamburguesas"]?.icono?.setImageResource(R.drawable.cat_hamburguesas)
        pestanas["Pizzas"]?.icono?.setImageResource(R.drawable.cat_pizzas)
        pestanas["Tacos"]?.icono?.setImageResource(R.drawable.cat_tacos)
        pestanas["Bebidas"]?.icono?.setImageResource(R.drawable.cat_bebidas)
        pestanas["Postres"]?.icono?.setImageResource(R.drawable.cat_postres)
        pestanas["Burritos"]?.icono?.setImageResource(R.drawable.cat_burritos)
        pestanas["Tortas"]?.icono?.setImageResource(R.drawable.cat_tortas)
    }

    private fun configurarPestanas() {
        for ((nombreCategoria, pestana) in pestanas) {
            pestana.contenedor.setOnClickListener { seleccionarCategoria(nombreCategoria) }
        }
        tvOrdenar.setOnClickListener {
            Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun seleccionarCategoria(nombreCategoria: String) {
        categoriaActual = nombreCategoria
        tvCategoriaSeleccionada.text = nombreCategoria

        for ((nombre, pestana) in pestanas) {
            val estaActiva = nombre == nombreCategoria
            pestana.texto.setTextColor(getColor(if (estaActiva) R.color.fogonRed else R.color.fogonGrayText))
            pestana.texto.setTypeface(null, if (estaActiva) Typeface.BOLD else Typeface.NORMAL)
            pestana.subrayado.visibility = if (estaActiva) View.VISIBLE else View.INVISIBLE
        }

        etBuscar.text.clear() // al cambiar de categoría, empezamos sin filtro de búsqueda
        actualizarListaProductos()
    }

    // El buscador filtra en vivo dentro de la categoría seleccionada
    private fun configurarBusqueda() {
        etBuscar.doOnTextChanged { _, _, _, _ -> actualizarListaProductos() }
    }

    // Busca los productos de la categoría actual (con o sin texto de
    // búsqueda) y arma la lista a mano. Si no hay ninguno (categorías que
    // todavía no tienen platillos cargados), muestra el aviso de siempre.
    private fun actualizarListaProductos() {
        val consulta = etBuscar.text.toString()
        val productos = ProductosRepository.buscar(categoriaActual, consulta)

        contenedorProductos.removeAllViews()

        if (productos.isEmpty()) {
            layoutSinProductos.visibility = View.VISIBLE
            tvMensajeCategoria.text = if (consulta.isBlank()) {
                "Próximamente vas a ver aquí los platillos de $categoriaActual"
            } else {
                "No encontramos \"$consulta\" en $categoriaActual"
            }
            return
        }

        layoutSinProductos.visibility = View.GONE
        for (producto in productos) {
            contenedorProductos.addView(crearFilaProducto(producto))
        }
    }

    private fun crearFilaProducto(producto: Producto): View {
        val fila = LayoutInflater.from(this).inflate(R.layout.menu_item_producto, contenedorProductos, false)
        val tvNombre = fila.findViewById<TextView>(R.id.tvNombreProducto)
        val tvPrecio = fila.findViewById<TextView>(R.id.tvPrecioProducto)
        val tvDescripcion = fila.findViewById<TextView>(R.id.tvDescripcionProducto)
        val btnAgregar = fila.findViewById<TextView>(R.id.btnAgregarProducto)
        val ivProducto = fila.findViewById<ImageView>(R.id.ivProducto)

        tvNombre.text = producto.nombre
        tvPrecio.text = String.format(Locale.US, "$%.2f", producto.precio)
        tvDescripcion.text = producto.descripcion
        ivProducto.setImageResource(producto.imagenResId)

        btnAgregar.setOnClickListener {
            // Agrega con el tamaño base (el primero) y sin extras. Para
            // personalizar hay que entrar al detalle tocando la foto.
            val tamanoBase = DetalleProductoRepository.obtener(producto.detalleId)?.tamanos?.firstOrNull()
            CarritoRepository.agregarItem(
                ItemCarrito(
                    nombreProducto = producto.nombre,
                    detalleId = producto.detalleId,
                    nombreTamano = tamanoBase?.nombre ?: "",
                    precioTamano = tamanoBase?.precio ?: producto.precio,
                    extrasSeleccionados = emptyList()
                )
            )
            actualizarBadgeCarrito()
            Toast.makeText(this, "Agregado al carrito: ${producto.nombre}", Toast.LENGTH_SHORT).show()
        }

        // Tocar la foto (o cualquier parte de la fila que no sea el botón)
        // lleva al detalle con personalización, igual que en Destacados.
        val irAlDetalle = {
            val intent = Intent(this, DetalleProductoActivity::class.java)
            intent.putExtra(DetalleProductoActivity.EXTRA_PRODUCTO_ID, producto.detalleId)
            startActivity(intent)
        }
        ivProducto.setOnClickListener { irAlDetalle() }
        fila.setOnClickListener { irAlDetalle() }

        return fila
    }

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
        navMenu.setOnClickListener { /* ya estamos en Menú */ }
        navCarrito.setOnClickListener { irACarrito() }

        navPedidos.setOnClickListener { irAPedidos() }


        ivCart.setOnClickListener { irACarrito() }
    }

    private fun configurarBotonAtras() {
        ivBack.setOnClickListener { finish() }
    }
}