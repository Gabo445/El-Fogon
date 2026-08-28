package com.example.elfogonlogin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HomeActivity : AppCompatActivity() {

    private lateinit var ivPromoBanner: ImageView
    private lateinit var ivCatHamburguesas: ImageView
    private lateinit var ivCatPizzas: ImageView
    private lateinit var ivCatTacos: ImageView
    private lateinit var ivCatBebidas: ImageView
    private lateinit var ivCatPostres: ImageView
    private lateinit var ivProductoPizza: ImageView
    private lateinit var ivProductoHamburguesa: ImageView

    // Vistas del menú desplegable
    private lateinit var ivMenu: ImageView
    private lateinit var ivCerrarMenu: ImageView
    private lateinit var overlayMenu: View
    private lateinit var navDrawer: View

    // Barra de búsqueda
    private lateinit var etBuscar: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        ajustarPorTeclado()
        inicializarVistas()
        configurarMenuDesplegable()
        configurarBotonAtras()
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
        ivPromoBanner = findViewById(R.id.ivPromoBanner)
        ivCatHamburguesas = findViewById(R.id.ivCatHamburguesas)
        ivCatPizzas = findViewById(R.id.ivCatPizzas)
        ivCatTacos = findViewById(R.id.ivCatTacos)
        ivCatBebidas = findViewById(R.id.ivCatBebidas)
        ivCatPostres = findViewById(R.id.ivCatPostres)
        ivProductoPizza = findViewById(R.id.ivProductoPizza)
        ivProductoHamburguesa = findViewById(R.id.ivProductoHamburguesa)

        // Cuando tengas las fotos, aquí las asignas así, por ejemplo:
        // ivPromoBanner.setImageResource(R.drawable.hamburguesa_promo)
        // ivCatHamburguesas.setImageResource(R.drawable.cat_hamburguesas)

        ivMenu = findViewById(R.id.ivMenu)
        ivCerrarMenu = findViewById(R.id.ivCerrarMenu)
        overlayMenu = findViewById(R.id.overlayMenu)
        navDrawer = findViewById(R.id.navDrawer)

        etBuscar = findViewById(R.id.etBuscar)
    }

    // Conecta las 3 rayitas, la X, el fondo oscuro y cada opción del menú
    private fun configurarMenuDesplegable() {
        ivMenu.setOnClickListener { abrirMenu() }
        ivCerrarMenu.setOnClickListener { cerrarMenu() }
        overlayMenu.setOnClickListener { cerrarMenu() }

        // Opciones que todavía no tienen pantalla propia
        val opcionesProximamente = listOf(
            R.id.rowPerfil,
            R.id.rowPedidos,
            R.id.rowFavoritos,
            R.id.rowDirecciones,
            R.id.rowMetodosPago,
            R.id.rowAyuda,
            R.id.rowCercaDeMi,
            R.id.rowConfiguracion
        )
        for (idOpcion in opcionesProximamente) {
            findViewById<View>(idOpcion).setOnClickListener {
                Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show()
                cerrarMenu()
            }
        }

        findViewById<View>(R.id.rowCerrarSesion).setOnClickListener {
            cerrarSesion()
        }
    }

    // Si el usuario presiona "atrás" con el menú abierto, solo lo cierra (no sale de la app)
    private fun configurarBotonAtras() {
        onBackPressedDispatcher.addCallback(this) {
            if (navDrawer.visibility == View.VISIBLE) {
                cerrarMenu()
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun anchoDrawerPx(): Float = 280 * resources.displayMetrics.density

    private fun abrirMenu() {
        overlayMenu.visibility = View.VISIBLE
        navDrawer.visibility = View.VISIBLE
        navDrawer.translationX = -anchoDrawerPx()
        overlayMenu.alpha = 0f
        navDrawer.animate().translationX(0f).setDuration(220).start()
        overlayMenu.animate().alpha(1f).setDuration(220).start()
    }

    private fun cerrarMenu() {
        navDrawer.animate().translationX(-anchoDrawerPx()).setDuration(200)
            .withEndAction { navDrawer.visibility = View.GONE }
            .start()
        overlayMenu.animate().alpha(0f).setDuration(200)
            .withEndAction { overlayMenu.visibility = View.GONE }
            .start()
    }

    // Cierra la sesión y regresa al login, sin poder volver atrás con el botón "back"
    private fun cerrarSesion() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}