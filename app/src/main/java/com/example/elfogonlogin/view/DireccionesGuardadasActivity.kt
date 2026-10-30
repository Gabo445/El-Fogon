package com.example.elfogonlogin.view

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.elfogonlogin.R
import com.example.elfogonlogin.controller.DireccionEntrega
import com.example.elfogonlogin.controller.DireccionRepository
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

// "Direcciones guardadas": muestra la dirección que tienes guardada (zona,
// dirección y tiempo estimado) y dónde queda en el mapa. Solo se ve; para
// cambiarla hay un botón que lleva a la pantalla de editar. Por ahora la app
// guarda UNA dirección por cuenta. Se vuelve a leer cada vez que regresas,
// así que si la editas, aquí aparece ya actualizada.
class DireccionesGuardadasActivity : AppCompatActivity() {

    private lateinit var ivBack: View
    private lateinit var tvCargandoDirecciones: View
    private lateinit var scrollDireccion: View
    private lateinit var layoutSinDireccion: View

    private lateinit var tvZonaGuardada: TextView
    private lateinit var tvDireccionEspecificaGuardada: TextView
    private lateinit var tvTiempoEstimadoGuardado: TextView

    private lateinit var webViewUbicacion: WebView
    private lateinit var tvSinMapa: View
    private lateinit var btnVerEnGoogleMaps: View
    private lateinit var btnEditarDireccion: View
    private lateinit var btnAgregarDireccion: View

    // Para no volver a cargar el mismo mapa cada vez que regresas a la pantalla.
    private var ultimoMapaCargado: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_direcciones_guardadas)

        ajustarBarrasDelSistema()
        inicializarVistas()
        configurarMapa()
        configurarAcciones()
    }

    // Se vuelve a leer cada vez que regresas (por ejemplo, después de editar la
    // dirección), así lo que ves siempre está al día.
    override fun onResume() {
        super.onResume()
        cargarDireccion()
    }

    override fun onDestroy() {
        if (::webViewUbicacion.isInitialized) {
            webViewUbicacion.destroy()
        }
        super.onDestroy()
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
        tvCargandoDirecciones = findViewById(R.id.tvCargandoDirecciones)
        scrollDireccion = findViewById(R.id.scrollDireccion)
        layoutSinDireccion = findViewById(R.id.layoutSinDireccion)

        tvZonaGuardada = findViewById(R.id.tvZonaGuardada)
        tvDireccionEspecificaGuardada = findViewById(R.id.tvDireccionEspecificaGuardada)
        tvTiempoEstimadoGuardado = findViewById(R.id.tvTiempoEstimadoGuardado)

        webViewUbicacion = findViewById(R.id.webViewUbicacion)
        tvSinMapa = findViewById(R.id.tvSinMapa)
        btnVerEnGoogleMaps = findViewById(R.id.btnVerEnGoogleMaps)
        btnEditarDireccion = findViewById(R.id.btnEditarDireccion)
        btnAgregarDireccion = findViewById(R.id.btnAgregarDireccion)
    }

    // El mapa de OpenStreetMap necesita JavaScript para poder moverlo y acercarlo.
    // Es seguro porque aquí solo se abre la página del mapa y nada más.
    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun configurarMapa() {
        webViewUbicacion.settings.javaScriptEnabled = true
        webViewUbicacion.webViewClient = WebViewClient()

        // Mientras tocas el mapa, la pantalla no se desplaza: así lo puedes
        // mover con el dedo sin que se mueva toda la página.
        webViewUbicacion.setOnTouchListener { vista, _ ->
            vista.parent.requestDisallowInterceptTouchEvent(true)
            false
        }
    }

    private fun configurarAcciones() {
        ivBack.setOnClickListener { finish() }
        btnEditarDireccion.setOnClickListener { abrirEditarDireccion() }
        btnAgregarDireccion.setOnClickListener { abrirEditarDireccion() }
    }

    // Lleva a la pantalla donde se elige la zona, se escribe la dirección y se
    // usa el GPS (la misma que se abre desde el Carrito).
    private fun abrirEditarDireccion() {
        startActivity(Intent(this, DireccionEntregaActivity::class.java))
    }

    // ---------------------------------------------------------------------
    // Mostrar la dirección
    // ---------------------------------------------------------------------

    private fun cargarDireccion() {
        DireccionRepository.obtenerDireccion { direccion ->
            if (isFinishing || isDestroyed) return@obtenerDireccion

            tvCargandoDirecciones.visibility = View.GONE
            if (direccion == null) {
                scrollDireccion.visibility = View.GONE
                layoutSinDireccion.visibility = View.VISIBLE
            } else {
                layoutSinDireccion.visibility = View.GONE
                scrollDireccion.visibility = View.VISIBLE
                mostrarDireccion(direccion)
            }
        }
    }

    private fun mostrarDireccion(direccion: DireccionEntrega) {
        tvZonaGuardada.text = direccion.zona
        tvDireccionEspecificaGuardada.text =
            if (direccion.direccionEspecifica.isNotBlank()) direccion.direccionEspecifica else "Sin dirección específica"
        tvTiempoEstimadoGuardado.text = "Entrega estimada: " +
                (DireccionRepository.tiemposEstimadosPorZona[direccion.zona] ?: "30 - 40 min")

        val latitud = direccion.latitud
        val longitud = direccion.longitud
        if (latitud != null && longitud != null) {
            // Hay coordenadas (se guardó con el GPS): se dibuja el mapa.
            webViewUbicacion.visibility = View.VISIBLE
            tvSinMapa.visibility = View.GONE
            cargarMapa(latitud, longitud)

            btnVerEnGoogleMaps.visibility = View.VISIBLE
            btnVerEnGoogleMaps.setOnClickListener {
                val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitud,$longitud")
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        } else {
            // Se guardó solo escribiendo la dirección, sin GPS: no hay punto que dibujar.
            webViewUbicacion.visibility = View.GONE
            tvSinMapa.visibility = View.VISIBLE
            btnVerEnGoogleMaps.visibility = View.GONE
        }
    }

    // Abre el mapa de OpenStreetMap centrado en tu punto, con un marcador, y
    // con la zona de alrededor visible (unos 500 metros a cada lado). El
    // Locale.US es importante: en español los decimales saldrían con coma y la
    // dirección web quedaría mal.
    private fun cargarMapa(latitud: Double, longitud: Double) {
        val oeste = longitud - 0.005
        val este = longitud + 0.005
        val sur = latitud - 0.003
        val norte = latitud + 0.003

        val direccionWeb = String.format(
            Locale.US,
            "https://www.openstreetmap.org/export/embed.html?bbox=%.6f%%2C%.6f%%2C%.6f%%2C%.6f&layer=mapnik&marker=%.6f%%2C%.6f",
            oeste, sur, este, norte, latitud, longitud
        )

        if (direccionWeb != ultimoMapaCargado) {
            ultimoMapaCargado = direccionWeb
            webViewUbicacion.loadUrl(direccionWeb)
        }
    }
}