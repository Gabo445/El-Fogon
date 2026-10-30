package com.example.elfogonlogin.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.elfogonlogin.R
import com.example.elfogonlogin.controller.DireccionRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class DireccionEntregaActivity : AppCompatActivity() {

    private lateinit var ivBack: View
    private lateinit var webViewMapa: WebView
    private lateinit var contenedorZonas: LinearLayout
    private lateinit var tvUsarUbicacionActual: TextView
    private lateinit var tvDireccionEspecifica: TextView
    private lateinit var contenedorMiUbicacion: View
    private lateinit var webViewMiUbicacion: WebView
    private lateinit var tvBorrarDireccion: View
    private lateinit var btnGuardarDireccion: View

    private var zonaSeleccionada: String? = null
    private val radioButtonsZonas = mutableListOf<Pair<String, RadioButton>>()

    // La dirección, su latitud y su longitud siempre se llenan JUNTAS y solo
    // con el resultado del GPS (o con lo que ya estaba guardado, que también
    // vino del GPS). No hay ningún campo donde la persona pueda escribir.
    private var direccionActual: String? = null
    private var latitudActual: Double? = null
    private var longitudActual: Double? = null

    // Mientras el GPS (o la traducción a texto) está trabajando, se evita
    // pedir otra vez y se evita guardar a medias.
    private var buscandoUbicacion = false

    private val solicitarPermisoUbicacion = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            obtenerUbicacionActual()
        } else {
            val sePuedePreguntarOtraVez = ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            )
            if (sePuedePreguntarOtraVez) {
                Toast.makeText(
                    this,
                    "Necesitamos tu ubicación precisa para registrar tu dirección",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                // Ya no se puede volver a preguntar desde la app: la única salida
                // es que la persona lo active en los ajustes del teléfono.
                mostrarAviso(
                    titulo = "Permiso de ubicación",
                    mensaje = "Tu dirección se toma de tu ubicación, así que necesitamos el permiso de Ubicación precisa. Actívalo en los ajustes de la app.",
                    textoBoton = "Abrir ajustes"
                ) { abrirAjustesDeLaApp() }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_direccion_entrega)

        ajustarBarrasDelSistema()
        inicializarVistas()
        cargarMapaRestaurante()
        construirListaZonas()
        cargarDireccionExistente()
        configurarUsarUbicacionActual()
        configurarBotonBorrar()
        configurarBotonGuardar()
        configurarBotonAtras()
    }

    private fun pantallaActiva(): Boolean = !isFinishing && !isDestroyed

    private fun ajustarBarrasDelSistema() {
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
        webViewMapa = findViewById(R.id.webViewMapa)
        contenedorZonas = findViewById(R.id.contenedorZonas)
        tvUsarUbicacionActual = findViewById(R.id.tvUsarUbicacionActual)
        tvDireccionEspecifica = findViewById(R.id.tvDireccionEspecifica)
        contenedorMiUbicacion = findViewById(R.id.contenedorMiUbicacion)
        webViewMiUbicacion = findViewById(R.id.webViewMiUbicacion)
        tvBorrarDireccion = findViewById(R.id.tvBorrarDireccion)
        btnGuardarDireccion = findViewById(R.id.btnGuardarDireccion)
        webViewMiUbicacion.settings.javaScriptEnabled = true
    }

    // Mismo truco gratis de OpenStreetMap que el mapa del restaurante, pero
    // ahora con el pin en TU ubicación. Se llama cada vez que hay
    // coordenadas nuevas (GPS recién detectado, o una dirección ya
    // guardada con coordenadas de antes).
    private fun mostrarMiUbicacionEnMapa(lat: Double, lng: Double) {
        val delta = 0.006
        val url = "https://www.openstreetmap.org/export/embed.html" +
                "?bbox=${lng - delta},${lat - delta},${lng + delta},${lat + delta}" +
                "&layer=mapnik" +
                "&marker=$lat,$lng"
        webViewMiUbicacion.loadUrl(url)
        contenedorMiUbicacion.visibility = View.VISIBLE
    }

    // Mapa de OpenStreetMap con un pin fijo en "El Fogón" (Universidad Don
    // Bosco, Soyapango). Es 100% gratis, no necesita API key ni tarjeta de
    // crédito — a diferencia de Google Maps. Solo informativo, no es
    // interactivo para elegir ubicación (eso lo hace el GPS más abajo).
    private fun cargarMapaRestaurante() {
        webViewMapa.settings.javaScriptEnabled = true
        val latitudRestaurante = DireccionRepository.LATITUD_RESTAURANTE
        val longitudRestaurante = DireccionRepository.LONGITUD_RESTAURANTE
        val delta = 0.006
        val izquierda = longitudRestaurante - delta
        val derecha = longitudRestaurante + delta
        val abajo = latitudRestaurante - delta
        val arriba = latitudRestaurante + delta
        val url = "https://www.openstreetmap.org/export/embed.html" +
                "?bbox=$izquierda,$abajo,$derecha,$arriba" +
                "&layer=mapnik" +
                "&marker=$latitudRestaurante,$longitudRestaurante"
        webViewMapa.loadUrl(url)
    }

    private fun construirListaZonas() {
        contenedorZonas.removeAllViews()
        radioButtonsZonas.clear()
        for (zona in DireccionRepository.zonasValidas) {
            val fila = LayoutInflater.from(this).inflate(R.layout.item_zona, contenedorZonas, false)
            val radio = fila.findViewById<RadioButton>(R.id.rbZona)
            val tvNombreZona = fila.findViewById<TextView>(R.id.tvNombreZona)

            tvNombreZona.text = zona
            radio.setOnClickListener { seleccionarZona(zona) }
            fila.setOnClickListener { seleccionarZona(zona) }

            radioButtonsZonas.add(zona to radio)
            contenedorZonas.addView(fila)
        }
    }

    private fun seleccionarZona(zona: String) {
        zonaSeleccionada = zona
        for ((nombreZona, radio) in radioButtonsZonas) {
            radio.isChecked = nombreZona == zona
        }
    }

    // Si ya había una dirección guardada, la precarga (con sus coordenadas)
    // y muestra el botón de borrar. DireccionRepository solo devuelve
    // direcciones que traen coordenadas del GPS; una escrita a mano antes de
    // este cambio llega aquí como "no hay dirección" y hay que volver a usar
    // la ubicación.
    private fun cargarDireccionExistente() {
        DireccionRepository.obtenerDireccion { direccion ->
            if (!pantallaActiva()) return@obtenerDireccion
            if (direccion == null) return@obtenerDireccion

            // Si la persona ya detectó su ubicación mientras esto cargaba, lo
            // nuevo gana y no se pisa con lo viejo.
            if (zonaSeleccionada == null) {
                seleccionarZona(direccion.zona)
            }
            val lat = direccion.latitud
            val lng = direccion.longitud
            if (direccionActual == null && !buscandoUbicacion && lat != null && lng != null) {
                direccionActual = direccion.direccionEspecifica
                latitudActual = lat
                longitudActual = lng
                tvDireccionEspecifica.text = direccion.direccionEspecifica
                mostrarMiUbicacionEnMapa(lat, lng)
                actualizarTextoBotonUbicacion()
            }
            tvBorrarDireccion.visibility = View.VISIBLE
        }
    }

    // ---------------------------------------------------------------------
    // Ubicación (GPS): la ÚNICA forma de llenar la dirección
    // ---------------------------------------------------------------------

    private fun configurarUsarUbicacionActual() {
        tvUsarUbicacionActual.setOnClickListener { pedirUbicacion() }
        // Tocar el cuadro de la dirección hace lo mismo que el botón: no se
        // puede escribir en él, pero sí sirve como atajo para detectarla.
        tvDireccionEspecifica.setOnClickListener { pedirUbicacion() }
    }

    private fun pedirUbicacion() {
        if (buscandoUbicacion) return

        val tienePermiso = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (tienePermiso) {
            obtenerUbicacionActual()
        } else {
            solicitarPermisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun gpsEncendido(): Boolean {
        val administrador = getSystemService(LOCATION_SERVICE) as? LocationManager
            ?: return true
        return LocationManagerCompat.isLocationEnabled(administrador)
    }

    // Apps de "GPS falso" le dicen al teléfono que la ubicación es simulada.
    // Android lo marca en la propia ubicación, y aquí se rechaza.
    @Suppress("DEPRECATION")
    private fun esUbicacionSimulada(ubicacion: Location): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ubicacion.isMock
        } else {
            ubicacion.isFromMockProvider
        }
    }

    private fun obtenerUbicacionActual() {
        if (buscandoUbicacion) return

        val tienePermiso = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!tienePermiso) return

        if (!gpsEncendido()) {
            mostrarAviso(
                titulo = "Activa tu ubicación",
                mensaje = "El GPS de tu teléfono está apagado. Enciéndelo para detectar tu dirección.",
                textoBoton = "Abrir ajustes"
            ) { startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
            return
        }

        marcarBuscando(true)

        // Alta precisión: ahora la ubicación es lo único que define a dónde
        // llega el repartidor, así que importa que sea exacta.
        val clienteUbicacion = LocationServices.getFusedLocationProviderClient(this)
        clienteUbicacion.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { ubicacion ->
                if (!pantallaActiva()) return@addOnSuccessListener

                if (ubicacion == null) {
                    marcarBuscando(false)
                    Toast.makeText(
                        this,
                        "No se pudo detectar tu ubicación. Revisa que el GPS esté activo e intenta de nuevo",
                        Toast.LENGTH_LONG
                    ).show()
                } else if (esUbicacionSimulada(ubicacion)) {
                    marcarBuscando(false)
                    mostrarAviso(
                        titulo = "Ubicación no válida",
                        mensaje = "Tu teléfono está usando una ubicación simulada. Desactívala para poder registrar tu dirección."
                    )
                } else if (!DireccionRepository.estaDentroDelAreaDeEntrega(ubicacion.latitude, ubicacion.longitude)) {
                    // Se revisa ANTES de traducir el punto a una dirección: no hace
                    // falta internet y la respuesta es inmediata. No se guarda nada
                    // de esta ubicación; lo que ya hubiera en pantalla se queda igual.
                    marcarBuscando(false)
                    mostrarFueraDeZona(
                        DireccionRepository.distanciaAlRestauranteKm(ubicacion.latitude, ubicacion.longitude)
                    )
                } else {
                    convertirCoordenadasATexto(ubicacion.latitude, ubicacion.longitude)
                }
            }
            .addOnFailureListener {
                if (!pantallaActiva()) return@addOnFailureListener
                marcarBuscando(false)
                Toast.makeText(this, "No se pudo detectar tu ubicación. Intenta de nuevo", Toast.LENGTH_LONG).show()
            }
    }

    // Convierte el punto del GPS en una dirección escrita (calle, colonia,
    // municipio). Solo cuando eso termina se guardan juntos el texto y las
    // coordenadas nuevas, así nunca queda un texto de una ubicación con las
    // coordenadas de otra.
    private fun convertirCoordenadasATexto(lat: Double, lng: Double) {
        lifecycleScope.launch {
            val textoDireccion = withContext(Dispatchers.IO) {
                buscarTextoDeDireccion(lat, lng)
            }

            if (textoDireccion != null) {
                aplicarUbicacion(textoDireccion, lat, lng)
                val mensaje = if (zonaSeleccionada == null) {
                    "Ubicación detectada. Ahora elige tu zona"
                } else {
                    "Ubicación detectada"
                }
                Toast.makeText(this@DireccionEntregaActivity, mensaje, Toast.LENGTH_SHORT).show()
            } else {
                // Sin internet o sin datos de calles para ese punto: la ubicación
                // sigue siendo real, así que se muestra con sus coordenadas. El
                // repartidor la abre en el mapa con el botón de Google Maps.
                val coordenadas = String.format(Locale.US, "%.5f, %.5f", lat, lng)
                aplicarUbicacion("Ubicación GPS ($coordenadas)", lat, lng)
                Toast.makeText(
                    this@DireccionEntregaActivity,
                    "No pudimos obtener el nombre de tu calle, pero tu ubicación quedó marcada en el mapa",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun buscarTextoDeDireccion(lat: Double, lng: Double): String? {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            @Suppress("DEPRECATION")
            val direcciones = geocoder.getFromLocation(lat, lng, 1)
            val direccion = direcciones?.firstOrNull() ?: return null
            armarTexto(direccion)
        } catch (e: Exception) {
            null
        }
    }

    private fun armarTexto(direccion: Address): String? {
        val linea = direccion.getAddressLine(0)
        if (!linea.isNullOrBlank()) return linea.trim()

        val partes = listOfNotNull(
            direccion.thoroughfare,
            direccion.subLocality,
            direccion.locality,
            direccion.adminArea
        ).filter { it.isNotBlank() }
        return if (partes.isEmpty()) null else partes.joinToString(", ")
    }

    private fun aplicarUbicacion(texto: String, lat: Double, lng: Double) {
        if (!pantallaActiva()) return
        direccionActual = texto
        latitudActual = lat
        longitudActual = lng
        tvDireccionEspecifica.text = texto
        mostrarMiUbicacionEnMapa(lat, lng)
        marcarBuscando(false)
    }

    private fun marcarBuscando(buscando: Boolean) {
        buscandoUbicacion = buscando
        tvUsarUbicacionActual.isEnabled = !buscando
        tvUsarUbicacionActual.alpha = if (buscando) 0.6f else 1f
        if (buscando) {
            tvUsarUbicacionActual.text = "Buscando tu ubicación…"
        } else {
            actualizarTextoBotonUbicacion()
        }
    }

    private fun actualizarTextoBotonUbicacion() {
        tvUsarUbicacionActual.text = if (direccionActual == null) {
            "📍 Usar mi ubicación actual"
        } else {
            "📍 Volver a detectar mi ubicación"
        }
    }

    // ---------------------------------------------------------------------
    // Avisos
    // ---------------------------------------------------------------------

    private fun mostrarAviso(
        titulo: String,
        mensaje: String,
        textoBoton: String? = null,
        accion: (() -> Unit)? = null
    ) {
        if (!pantallaActiva()) return
        val constructor = AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
        if (textoBoton != null && accion != null) {
            constructor.setPositiveButton(textoBoton) { _, _ -> accion() }
            constructor.setNegativeButton("Cancelar", null)
        } else {
            constructor.setPositiveButton("Entendido", null)
        }
        constructor.show()
    }

    private fun mostrarFueraDeZona(distanciaKm: Double) {
        val distancia = Math.round(distanciaKm)
        val alcance = DireccionRepository.RADIO_ENTREGA_KM.toInt()
        mostrarAviso(
            titulo = "Fuera de nuestra zona de entrega",
            mensaje = "No podemos llegar hasta ahí. Tu ubicación está a unos $distancia km de El Fogón " +
                    "y solo entregamos hasta $alcance km a la redonda, en las zonas de la lista. " +
                    "Por ahora no podemos llevar pedidos a este lugar."
        )
    }

    private fun abrirAjustesDeLaApp() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        startActivity(intent)
    }

    // ---------------------------------------------------------------------
    // Borrar / Guardar / Atrás
    // ---------------------------------------------------------------------

    private fun configurarBotonBorrar() {
        tvBorrarDireccion.setOnClickListener {
            DireccionRepository.borrarDireccion {
                Toast.makeText(this, "Ubicación borrada", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun configurarBotonGuardar() {
        btnGuardarDireccion.setOnClickListener {
            if (buscandoUbicacion) {
                Toast.makeText(this, "Espera un momento, estamos detectando tu ubicación", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val zona = zonaSeleccionada
            val direccion = direccionActual
            val lat = latitudActual
            val lng = longitudActual

            // Sin ubicación del GPS no hay dirección: es la única forma de tenerla.
            if (direccion == null || direccion.isBlank() || lat == null || lng == null) {
                Toast.makeText(this, "Toca «Usar mi ubicación actual» para detectar tu dirección", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (zona == null) {
                Toast.makeText(this, "Elige tu zona de la lista", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            DireccionRepository.guardarDireccion(zona, direccion, lat, lng) {
                Toast.makeText(this, "Ubicación guardada", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun configurarBotonAtras() {
        ivBack.setOnClickListener { finish() }
    }
}