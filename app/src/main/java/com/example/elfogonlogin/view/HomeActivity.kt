package com.example.elfogonlogin.view

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.elfogonlogin.R
import com.example.elfogonlogin.controller.CarritoRepository
import com.example.elfogonlogin.controller.DetalleProductoRepository
import com.example.elfogonlogin.controller.ItemCarrito
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class HomeActivity : AppCompatActivity() {

    private lateinit var scrollHome: ScrollView
    private lateinit var viewPagerPromo: ViewPager2
    private val handlerCarrusel = Handler(Looper.getMainLooper())
    private var runnableCarrusel: Runnable? = null
    private var listenerCarrito: ListenerRegistration? = null

    private lateinit var ivMenu: View
    private lateinit var ivCart: FrameLayout
    private lateinit var tvCartBadge: TextView
    private lateinit var etBuscar: EditText

    private lateinit var dot1: View
    private lateinit var dot2: View
    private lateinit var dot3: View

    private lateinit var containerCatHamburguesas: View
    private lateinit var containerCatPizzas: View
    private lateinit var containerCatTacos: View
    private lateinit var containerCatBebidas: View
    private lateinit var containerCatPostres: View
    private lateinit var containerCatBurritos: View
    private lateinit var containerCatTortas: View

    private lateinit var ivCatHamburguesas: ImageView
    private lateinit var ivCatPizzas: ImageView
    private lateinit var ivCatTacos: ImageView
    private lateinit var ivCatBebidas: ImageView
    private lateinit var ivCatPostres: ImageView
    private lateinit var ivCatBurritos: ImageView
    private lateinit var ivCatTortas: ImageView

    private lateinit var tvVerTodasCategorias: View
    private lateinit var destacadosHeader: View
    private lateinit var tvVerTodasDestacados: View

    private lateinit var ivProductoPizza: ImageView
    private lateinit var ivProductoHamburguesa: ImageView
    private lateinit var ivProductoTacosRes: ImageView
    private lateinit var ivProductoBurritos5Capas: ImageView
    private lateinit var ivProductoTortasBirria: ImageView
    private lateinit var ivProductoTresLeches: ImageView

    private lateinit var btnAgregarPizza: View
    private lateinit var btnAgregarHamburguesa: View
    private lateinit var btnAgregarTacosRes: View
    private lateinit var btnAgregarBurritos5Capas: View
    private lateinit var btnAgregarTortasBirria: View
    private lateinit var btnAgregarTresLeches: View

    private lateinit var navInicio: View
    private lateinit var navMenu: View
    private lateinit var navCarrito: View
    private lateinit var navPedidos: View
    private lateinit var navPerfil: View

    // Menú lateral (el que abre el ícono de hamburguesa ☰)
    private lateinit var overlayMenu: View
    private lateinit var navDrawer: View
    private lateinit var ivCerrarMenu: View
    private lateinit var tvBienvenida: TextView
    private lateinit var rowPerfil: View
    private lateinit var rowPedidos: View
    private lateinit var rowFavoritos: View
    private lateinit var rowDirecciones: View
    private lateinit var rowMetodosPago: View
    private lateinit var rowAyuda: View
    private lateinit var rowConfiguracion: View
    private lateinit var rowCerrarSesion: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_home)

        ajustarPorTeclado()
        inicializarVistas()
        configurarCarrusel()
        configurarCategorias()
        configurarDestacados()
        configurarBottomNav()
        configurarMenuLateral()
    }

    override fun onResume() {
        super.onResume()
        listenerCarrito = CarritoRepository.escucharCambios { actualizarBadgeCarrito() }
        actualizarSaludo()
    }

    override fun onPause() {
        super.onPause()
        listenerCarrito?.remove()
    }

    override fun onDestroy() {
        super.onDestroy()
        runnableCarrusel?.let { handlerCarrusel.removeCallbacks(it) }
    }

    override fun onBackPressed() {
        if (navDrawer.visibility == View.VISIBLE) {
            cerrarMenuLateral()
        } else {
            super.onBackPressed()
        }
    }

    private fun ajustarPorTeclado() {
        val vistaRaiz = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(vistaRaiz) { view, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
    }

    private fun inicializarVistas() {
        scrollHome = findViewById(R.id.scrollHome)
        viewPagerPromo = findViewById(R.id.viewPagerPromos)

        ivMenu = findViewById(R.id.ivMenu)
        ivCart = findViewById(R.id.ivCart)
        tvCartBadge = findViewById(R.id.tvCartBadge)
        etBuscar = findViewById(R.id.etBuscar)

        dot1 = findViewById(R.id.dot1)
        dot2 = findViewById(R.id.dot2)
        dot3 = findViewById(R.id.dot3)

        containerCatHamburguesas = findViewById(R.id.containerCatHamburguesas)
        containerCatPizzas = findViewById(R.id.containerCatPizzas)
        containerCatTacos = findViewById(R.id.containerCatTacos)
        containerCatBebidas = findViewById(R.id.containerCatBebidas)
        containerCatPostres = findViewById(R.id.containerCatPostres)
        containerCatBurritos = findViewById(R.id.containerCatBurritos)
        containerCatTortas = findViewById(R.id.containerCatTortas)

        ivCatHamburguesas = findViewById(R.id.ivCatHamburguesas)
        ivCatPizzas = findViewById(R.id.ivCatPizzas)
        ivCatTacos = findViewById(R.id.ivCatTacos)
        ivCatBebidas = findViewById(R.id.ivCatBebidas)
        ivCatPostres = findViewById(R.id.ivCatPostres)
        ivCatBurritos = findViewById(R.id.ivCatBurritos)
        ivCatTortas = findViewById(R.id.ivCatTortas)

        tvVerTodasCategorias = findViewById(R.id.tvVerTodasCategorias)
        destacadosHeader = findViewById(R.id.destacadosHeader)
        tvVerTodasDestacados = findViewById(R.id.tvVerTodasDestacados)

        ivProductoPizza = findViewById(R.id.ivProductoPizza)
        ivProductoHamburguesa = findViewById(R.id.ivProductoHamburguesa)
        ivProductoTacosRes = findViewById(R.id.ivProductoTacosRes)
        ivProductoBurritos5Capas = findViewById(R.id.ivProductoBurritos5Capas)
        ivProductoTortasBirria = findViewById(R.id.ivProductoTortasBirria)
        ivProductoTresLeches = findViewById(R.id.ivProductoTresLeches)

        btnAgregarPizza = findViewById(R.id.btnAgregarPizza)
        btnAgregarHamburguesa = findViewById(R.id.btnAgregarHamburguesa)
        btnAgregarTacosRes = findViewById(R.id.btnAgregarTacosRes)
        btnAgregarBurritos5Capas = findViewById(R.id.btnAgregarBurritos5Capas)
        btnAgregarTortasBirria = findViewById(R.id.btnAgregarTortasBirria)
        btnAgregarTresLeches = findViewById(R.id.btnAgregarTresLeches)

        navInicio = findViewById(R.id.navInicio)
        navMenu = findViewById(R.id.navMenu)
        navCarrito = findViewById(R.id.navCarrito)
        navPedidos = findViewById(R.id.navPedidos)
        navPerfil = findViewById(R.id.navPerfil)

        overlayMenu = findViewById(R.id.overlayMenu)
        navDrawer = findViewById(R.id.navDrawer)
        ivCerrarMenu = findViewById(R.id.ivCerrarMenu)
        tvBienvenida = findViewById(R.id.tvBienvenida)
        rowPerfil = findViewById(R.id.rowPerfil)
        rowPedidos = findViewById(R.id.rowPedidos)
        rowFavoritos = findViewById(R.id.rowFavoritos)
        rowDirecciones = findViewById(R.id.rowDirecciones)
        rowMetodosPago = findViewById(R.id.rowMetodosPago)
        rowAyuda = findViewById(R.id.rowAyuda)
        rowConfiguracion = findViewById(R.id.rowConfiguracion)
        rowCerrarSesion = findViewById(R.id.rowCerrarSesion)

        // Fotos de categorías (reutiliza las mismas que ya están en el Menú)
        ivCatHamburguesas.setImageResource(R.drawable.cat_hamburguesas)
        ivCatPizzas.setImageResource(R.drawable.cat_pizzas)
        ivCatTacos.setImageResource(R.drawable.cat_tacos)
        ivCatBebidas.setImageResource(R.drawable.cat_bebidas)
        ivCatPostres.setImageResource(R.drawable.cat_postres)
        ivCatBurritos.setImageResource(R.drawable.cat_burritos)
        ivCatTortas.setImageResource(R.drawable.cat_tortas)

        // Fotos de los 6 Destacados (esto se me había quedado pendiente)
        ivProductoPizza.setImageResource(R.drawable.producto_pizza)
        ivProductoHamburguesa.setImageResource(R.drawable.producto_hamburguesa)
        ivProductoTacosRes.setImageResource(R.drawable.producto_tacos_res)
        ivProductoBurritos5Capas.setImageResource(R.drawable.producto_burritos_5capas)
        ivProductoTortasBirria.setImageResource(R.drawable.producto_tortas_birria)
        ivProductoTresLeches.setImageResource(R.drawable.producto_tres_leches)
    }

    // Saluda con el nombre de la cuenta. Se vuelve a poner cada vez que regresas
    // al Inicio, así si cambias tu nombre en el Perfil, aquí cambia también.
    private fun actualizarSaludo() {
        val nombreUsuario = FirebaseAuth.getInstance().currentUser?.displayName
        if (!nombreUsuario.isNullOrBlank()) {
            tvBienvenida.text = "Hola, $nombreUsuario"
        }
    }

    private fun configurarCarrusel() {
        val banners = listOf(
            Promocion(
                "Hamburguesas Artesanales",
                "Jugosas, grandes y hechas al momento",
                R.drawable.promo_hamburguesa
            ),
            Promocion(
                "Noche de Tacos",
                "Auténtico sabor mexicano y salvadoreño",
                R.drawable.promo_tacos
            ),
            Promocion(
                "Refréscate con Nosotros",
                "Bebidas naturales y heladas",
                R.drawable.promo_bebida
            )
        )
        viewPagerPromo.adapter = PromoBannerAdapter(banners)

        viewPagerPromo.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                actualizarPuntosCarrusel(position)
            }
        })

        runnableCarrusel = object : Runnable {
            override fun run() {
                val siguiente = (viewPagerPromo.currentItem + 1) % banners.size
                viewPagerPromo.currentItem = siguiente
                handlerCarrusel.postDelayed(this, 4000)
            }
        }
        handlerCarrusel.postDelayed(runnableCarrusel!!, 4000)
    }

    private fun actualizarPuntosCarrusel(posicion: Int) {
        val puntos = listOf(dot1, dot2, dot3)
        for ((index, punto) in puntos.withIndex()) {
            punto.setBackgroundResource(
                if (index == posicion) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive
            )
        }
    }

    private fun configurarCategorias() {
        containerCatHamburguesas.setOnClickListener { irAPantallaMenu("Hamburguesas") }
        containerCatPizzas.setOnClickListener { irAPantallaMenu("Pizzas") }
        containerCatTacos.setOnClickListener { irAPantallaMenu("Tacos") }
        containerCatBebidas.setOnClickListener { irAPantallaMenu("Bebidas") }
        containerCatPostres.setOnClickListener { irAPantallaMenu("Postres") }
        containerCatBurritos.setOnClickListener { irAPantallaMenu("Burritos") }
        containerCatTortas.setOnClickListener { irAPantallaMenu("Tortas") }
        tvVerTodasCategorias.setOnClickListener { irAPantallaMenu(null) }
    }

    private fun irAPantallaMenu(categoria: String?) {
        val intent = Intent(this, MenuActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        if (categoria != null) {
            intent.putExtra(MenuActivity.EXTRA_CATEGORIA, categoria)
        }
        startActivity(intent)
    }

    private fun irACarrito() {
        val intent = Intent(this, CarritoActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    private fun configurarDestacados() {
        tvVerTodasDestacados.setOnClickListener {
            scrollHome.smoothScrollTo(0, destacadosHeader.top)
        }

        ivProductoPizza.setOnClickListener { irADetalleProducto("pizza") }
        ivProductoHamburguesa.setOnClickListener { irADetalleProducto("hamburguesa") }
        ivProductoTacosRes.setOnClickListener { irADetalleProducto("tacos") }
        ivProductoBurritos5Capas.setOnClickListener { irADetalleProducto("burritos") }
        ivProductoTortasBirria.setOnClickListener { irADetalleProducto("tortas") }
        ivProductoTresLeches.setOnClickListener { irADetalleProducto("tres_leches") }

        btnAgregarPizza.setOnClickListener { agregarAlCarrito("pizza") }
        btnAgregarHamburguesa.setOnClickListener { agregarAlCarrito("hamburguesa") }
        btnAgregarTacosRes.setOnClickListener { agregarAlCarrito("tacos") }
        btnAgregarBurritos5Capas.setOnClickListener { agregarAlCarrito("burritos") }
        btnAgregarTortasBirria.setOnClickListener { agregarAlCarrito("tortas") }
        btnAgregarTresLeches.setOnClickListener { agregarAlCarrito("tres_leches") }
    }

    private fun irADetalleProducto(detalleId: String) {
        val intent = Intent(this, DetalleProductoActivity::class.java)
        intent.putExtra(DetalleProductoActivity.EXTRA_PRODUCTO_ID, detalleId)
        startActivity(intent)
    }

    // Agrega la versión base del Destacado (su primer tamaño, sin extras).
    // Para personalizar hay que tocar la foto y entrar al detalle.
    private fun agregarAlCarrito(detalleId: String) {
        val detalle = DetalleProductoRepository.obtener(detalleId) ?: return
        val tamanoBase = detalle.tamanos.first()
        CarritoRepository.agregarItem(
            ItemCarrito(
                nombreProducto = detalle.nombre,
                detalleId = detalleId,
                nombreTamano = tamanoBase.nombre,
                precioTamano = tamanoBase.precio,
                extrasSeleccionados = emptyList()
            )
        )
        Toast.makeText(this, "Agregado al carrito: ${detalle.nombre}", Toast.LENGTH_SHORT).show()
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

    private fun irAPedidos() {
        val intent = Intent(this, PedidosActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    // Abre el Perfil. Con abrirMetodosDePago = true, el Perfil muestra de una vez
    // la ventana "¿Cómo vas a pagar?" (así funciona "Métodos de Pago" del menú lateral).
    private fun irAPerfil(abrirMetodosDePago: Boolean = false) {
        val intent = Intent(this, PerfilActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        if (abrirMetodosDePago) {
            intent.putExtra(PerfilActivity.EXTRA_ABRIR_METODOS_PAGO, true)
        }
        startActivity(intent)
    }

    private fun configurarBottomNav() {
        navInicio.setOnClickListener { /* ya estamos en Inicio */ }
        navMenu.setOnClickListener { irAPantallaMenu(null) }
        navCarrito.setOnClickListener { irACarrito() }

        navPedidos.setOnClickListener { irAPedidos() }

        navPerfil.setOnClickListener { irAPerfil() }

        ivCart.setOnClickListener { irACarrito() }
    }

    // El ícono de hamburguesa (arriba a la izquierda) abre este panel lateral
    private fun configurarMenuLateral() {
        ivMenu.setOnClickListener { abrirMenuLateral() }
        ivCerrarMenu.setOnClickListener { cerrarMenuLateral() }
        overlayMenu.setOnClickListener { cerrarMenuLateral() }

        rowCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // "Mis Pedidos": cierra el panel y abre la pestaña Pedidos.
        rowPedidos.setOnClickListener {
            cerrarMenuLateral()
            irAPedidos()
        }

        // "Perfil": cierra el panel y abre la pestaña Perfil.
        rowPerfil.setOnClickListener {
            cerrarMenuLateral()
            irAPerfil()
        }

        // "Métodos de Pago": cierra el panel, abre el Perfil y de una vez la ventana
        // para elegir cómo vas a pagar (eso es lo que verás ya marcado al pagar).
        rowMetodosPago.setOnClickListener {
            cerrarMenuLateral()
            irAPerfil(abrirMetodosDePago = true)
        }

        // "Direcciones guardadas": cierra el panel y abre la vista que muestra la
        // dirección que tienes guardada y dónde queda en el mapa.
        rowDirecciones.setOnClickListener {
            cerrarMenuLateral()
            startActivity(Intent(this, DireccionesGuardadasActivity::class.java))
        }

        // "Favoritos": cierra el panel y abre la pantalla con los productos que
        // marcaste con el corazón.
        rowFavoritos.setOnClickListener {
            cerrarMenuLateral()
            startActivity(Intent(this, FavoritosActivity::class.java))
        }

        // "Ayuda y soporte": cierra el panel y abre la pantalla de ayuda con las
        // preguntas frecuentes y los datos de contacto.
        rowAyuda.setOnClickListener {
            cerrarMenuLateral()
            startActivity(Intent(this, AyudaActivity::class.java))
        }

        // "Configuración" ya no se usa: lo importante está en el Perfil, así que
        // esta fila se esconde del menú.
        rowConfiguracion.visibility = View.GONE
    }

    private fun abrirMenuLateral() {
        overlayMenu.visibility = View.VISIBLE
        navDrawer.visibility = View.VISIBLE
    }

    private fun cerrarMenuLateral() {
        overlayMenu.visibility = View.GONE
        navDrawer.visibility = View.GONE
    }
}