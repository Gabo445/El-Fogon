package com.example.elfogonlogin.view

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.elfogonlogin.R
import com.example.elfogonlogin.controller.CuentaRepository
import com.example.elfogonlogin.controller.ResultadoCuenta
import com.google.firebase.auth.FirebaseAuth

// "Privacidad y seguridad". Tiene tres partes:
//  - Seguridad de tu cuenta: cambiar la contraseña y ver/ajustar el permiso de ubicación.
//  - Información: qué datos se guardan, quién los ve y cómo se protegen (preguntas
//    que se abren y se cierran, igual que en Ayuda).
//  - Tu control: ver y editar tus datos y eliminar tu cuenta.
// Los textos están escritos según lo que la app hace HOY; si cambia algo (por
// ejemplo se agregan avisos al celular o cobro real), hay que actualizarlos.
class PrivacidadActivity : AppCompatActivity() {

    // Una fila con acción. "destructiva" la pinta con borde rojo (Eliminar cuenta).
    private class Fila(
        val icono: Int,
        val titulo: String,
        val subtitulo: String,
        val destructiva: Boolean = false,
        val accion: () -> Unit
    )

    private class Tema(val pregunta: String, val respuesta: String)

    private lateinit var ivBack: View
    private lateinit var contenedorSeguridad: ViewGroup
    private lateinit var contenedorDatos: ViewGroup
    private lateinit var contenedorProteccion: ViewGroup
    private lateinit var contenedorControl: ViewGroup
    private lateinit var tvIrAAyuda: View

    // El "Permitida / No permitida" de la fila de Ubicación.
    private var tvEstadoUbicacion: TextView? = null

    // Evita que un doble toque intente eliminar la cuenta dos veces.
    private var eliminandoCuenta = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_privacidad)

        ajustarBarrasDelSistema()
        inicializarVistas()
        construirSeguridad()
        construirDatos()
        construirProteccion()
        construirControl()

        ivBack.setOnClickListener { finish() }
        tvIrAAyuda.setOnClickListener { startActivity(Intent(this, AyudaActivity::class.java)) }
    }

    // Si vas a los ajustes del teléfono y cambias el permiso, al regresar aquí
    // el estado se actualiza solo.
    override fun onResume() {
        super.onResume()
        actualizarEstadoUbicacion()
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
        contenedorSeguridad = findViewById(R.id.contenedorSeguridad)
        contenedorDatos = findViewById(R.id.contenedorDatos)
        contenedorProteccion = findViewById(R.id.contenedorProteccion)
        contenedorControl = findViewById(R.id.contenedorControl)
        tvIrAAyuda = findViewById(R.id.tvIrAAyuda)
    }

    // ---------------------------------------------------------------------
    // Seguridad de tu cuenta
    // ---------------------------------------------------------------------

    private fun construirSeguridad() {
        agregarFila(
            contenedorSeguridad,
            Fila(R.drawable.ic_pago_candado, "Cambiar contraseña", "Te enviamos un enlace a tu correo") {
                confirmarCambioDeContrasena()
            }
        )

        val filaUbicacion = agregarFila(
            contenedorSeguridad,
            Fila(R.drawable.ic_pago_pin, "Ubicación", "Solo se usa cuando tocas “Usar mi ubicación”") {
                abrirAjustesDeLaApp()
            }
        )
        tvEstadoUbicacion = filaUbicacion.findViewById(R.id.tvEstadoFila)
        actualizarEstadoUbicacion()
    }

    private fun tienePermisoDeUbicacion(): Boolean {
        val precisa = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val aproximada = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return precisa == PackageManager.PERMISSION_GRANTED || aproximada == PackageManager.PERMISSION_GRANTED
    }

    // Verde "Permitida" o amarillo "No permitida", según lo que tenga el teléfono.
    private fun actualizarEstadoUbicacion() {
        val estado = tvEstadoUbicacion ?: return
        val permitida = tienePermisoDeUbicacion()

        estado.visibility = View.VISIBLE
        estado.text = if (permitida) "Permitida" else "No permitida"
        estado.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (permitida) "#E3F4E6" else "#FFF4D6"))
        estado.setTextColor(Color.parseColor(if (permitida) "#2E7D32" else "#B26A00"))
    }

    // Abre la pantalla de ajustes de El Fogón en tu teléfono, donde se puede
    // permitir o quitar el permiso de ubicación.
    private fun abrirAjustesDeLaApp() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No se pudieron abrir los ajustes del teléfono", Toast.LENGTH_LONG).show()
        }
    }

    private fun confirmarCambioDeContrasena() {
        val correo = FirebaseAuth.getInstance().currentUser?.email.orEmpty()

        AlertDialog.Builder(this)
            .setTitle("Cambiar contraseña")
            .setMessage(
                "Te enviaremos un enlace a $correo para que elijas una contraseña nueva. " +
                        "Tu contraseña actual sigue funcionando hasta que la cambies."
            )
            .setPositiveButton("Enviar enlace") { _, _ -> enviarEnlaceDeContrasena(correo) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun enviarEnlaceDeContrasena(correo: String) {
        CuentaRepository.enviarCorreoCambioContrasena { ok, error ->
            if (!pantallaActiva()) return@enviarCorreoCambioContrasena
            if (ok) {
                Toast.makeText(
                    this,
                    "Listo: enviamos el enlace a $correo. Revisa también tu carpeta de spam.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(this, "No se pudo enviar el enlace: ${error ?: "error desconocido"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ---------------------------------------------------------------------
    // Información: qué guardamos y cómo la protegemos
    // ---------------------------------------------------------------------

    private fun construirDatos() {
        val temas = listOf(
            Tema(
                "Datos de tu cuenta",
                "Tu nombre y tu correo y, si los agregas, tu teléfono y tu foto de perfil. Los usamos para " +
                        "identificarte, mostrar tu perfil y contactarte por tus pedidos."
            ),
            Tema(
                "Tu dirección de entrega",
                "La zona y la dirección que escribes. Si tocas “Usar mi ubicación”, también guardamos ese punto " +
                        "del mapa para que el repartidor llegue mejor.\n\n" +
                        "La ubicación se toma solo en ese momento: la app no te sigue ni la usa en segundo plano."
            ),
            Tema(
                "Tus pedidos y tu carrito",
                "Los productos que agregas y cada pedido con su fecha, total, método de pago y la dirección y el " +
                        "contacto con los que se hizo. Del pago con tarjeta solo se guardan los últimos 4 dígitos."
            ),
            Tema(
                "Tus preferencias",
                "Tus favoritos, tu método de pago preferido y tus ajustes de notificaciones, para que no tengas " +
                        "que elegirlos cada vez."
            ),
            Tema(
                "Lo que nunca guardamos",
                "El número completo de tu tarjeta, su vencimiento y su código de seguridad. Tampoco guardamos tu " +
                        "contraseña: la maneja el sistema de inicio de sesión de Google (Firebase) y nosotros no " +
                        "podemos verla."
            )
        )
        for (tema in temas) contenedorDatos.addView(crearFilaTema(contenedorDatos, tema))
    }

    private fun construirProteccion() {
        val temas = listOf(
            Tema(
                "¿Quién puede ver mis datos?",
                "Solo tú ves tu perfil, tus favoritos, tu dirección y tus pedidos: otros clientes no tienen " +
                        "acceso. El restaurante ve tu nombre, teléfono, foto y dirección únicamente para atender tus pedidos."
            ),
            Tema(
                "¿Dónde se guardan?",
                "En los servidores de Google (Firebase). Cada cuenta solo puede leer y escribir su propia " +
                        "información, y la conexión viaja cifrada."
            ),
            Tema(
                "¿Qué permisos pide la app?",
                "Internet, para funcionar, y ubicación, solo si tocas “Usar mi ubicación”. No pedimos acceso a tu " +
                        "cámara, a tus contactos ni a tus fotos: para tu foto de perfil tú eliges la imagen con el " +
                        "selector del teléfono."
            ),
            Tema(
                "¿Venden o comparten mis datos?",
                "No vendemos tu información. Solo se usa para atender tus pedidos y recordar tus preferencias."
            )
        )
        for (tema in temas) contenedorProteccion.addView(crearFilaTema(contenedorProteccion, tema))
    }

    // Igual que las preguntas de Ayuda: se despliega al tocarla y la flecha se voltea.
    private fun crearFilaTema(contenedor: ViewGroup, tema: Tema): View {
        val fila = LayoutInflater.from(this).inflate(R.layout.item_ayuda_pregunta, contenedor, false)
        val tvRespuesta = fila.findViewById<TextView>(R.id.tvRespuesta)
        val ivFlecha = fila.findViewById<ImageView>(R.id.ivFlechaPregunta)

        fila.findViewById<TextView>(R.id.tvPregunta).text = tema.pregunta
        tvRespuesta.text = tema.respuesta

        fila.setOnClickListener {
            val abrir = tvRespuesta.visibility != View.VISIBLE
            TransitionManager.beginDelayedTransition(contenedor)
            tvRespuesta.visibility = if (abrir) View.VISIBLE else View.GONE
            ivFlecha.animate().rotation(if (abrir) -90f else 90f).setDuration(150).start()
        }
        return fila
    }

    // ---------------------------------------------------------------------
    // Tu control
    // ---------------------------------------------------------------------

    private fun construirControl() {
        contenedorControl.addView(
            crearFilaTema(
                contenedorControl,
                Tema(
                    "¿Cuánto tiempo guardan mis datos?",
                    "Mientras tengas tu cuenta. Si la eliminas, borramos tu perfil, tu foto, tu dirección, tus " +
                            "favoritos y tu carrito. Los pedidos que ya hiciste se conservan como registro del restaurante."
                )
            )
        )

        agregarFila(
            contenedorControl,
            Fila(R.drawable.ic_perfil_persona, "Ver y editar mis datos", "Nombre, teléfono y foto") {
                startActivity(Intent(this, MisDatosActivity::class.java))
            }
        )

        agregarFila(
            contenedorControl,
            Fila(
                R.drawable.ic_perfil_basura,
                "Eliminar mi cuenta",
                "Borra tu perfil, foto, dirección, favoritos y carrito",
                destructiva = true
            ) { confirmarEliminarCuenta() }
        )
    }

    // Primero se explica qué pasa; después se pide la contraseña. Eliminar la
    // cuenta no se puede deshacer, por eso son dos pasos.
    private fun confirmarEliminarCuenta() {
        AlertDialog.Builder(this)
            .setTitle("¿Eliminar tu cuenta?")
            .setMessage(
                "Se borrarán tu perfil, tu foto, tu dirección, tus favoritos y tu carrito.\n\n" +
                        "Los pedidos que ya hiciste se conservan como registro del restaurante.\n\n" +
                        "Esta acción no se puede deshacer."
            )
            .setPositiveButton("Continuar") { _, _ -> pedirContrasena(null) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Por seguridad hay que escribir la contraseña para poder eliminar la cuenta.
    // "aviso" sirve para volver a pedirla diciendo qué pasó (por ejemplo, que
    // estaba mal escrita).
    private fun pedirContrasena(aviso: String?) {
        val campo = EditText(this)
        campo.hint = "Tu contraseña"
        campo.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        val densidad = resources.displayMetrics.density
        val contenedor = FrameLayout(this)
        contenedor.setPadding((24 * densidad).toInt(), (8 * densidad).toInt(), (24 * densidad).toInt(), 0)
        contenedor.addView(campo)

        AlertDialog.Builder(this)
            .setTitle("Confirma tu contraseña")
            .setMessage(aviso ?: "Por seguridad, escribe tu contraseña para eliminar tu cuenta definitivamente.")
            .setView(contenedor)
            .setPositiveButton("Eliminar mi cuenta") { _, _ -> eliminarCuenta(campo.text.toString()) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarCuenta(contrasena: String) {
        if (contrasena.isEmpty()) {
            pedirContrasena("Escribe tu contraseña para continuar.")
            return
        }
        if (eliminandoCuenta) return

        eliminandoCuenta = true
        Toast.makeText(this, "Eliminando tu cuenta…", Toast.LENGTH_SHORT).show()

        CuentaRepository.eliminarCuenta(contrasena) { resultado, error ->
            eliminandoCuenta = false
            if (!pantallaActiva()) return@eliminarCuenta

            when (resultado) {
                ResultadoCuenta.EXITO -> {
                    Toast.makeText(this, "Tu cuenta fue eliminada. ¡Gracias por haber pedido con nosotros!", Toast.LENGTH_LONG).show()
                    volverAlInicioDeSesion()
                }
                ResultadoCuenta.CONTRASENA_INCORRECTA ->
                    pedirContrasena("La contraseña no es correcta. Inténtalo de nuevo.")
                ResultadoCuenta.ERROR ->
                    Toast.makeText(this, "No se pudo eliminar la cuenta: ${error ?: "error desconocido"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Borra todas las pantallas abiertas y regresa al inicio de sesión.
    private fun volverAlInicioDeSesion() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    // ---------------------------------------------------------------------
    // Filas con acción
    // ---------------------------------------------------------------------

    private fun agregarFila(contenedor: ViewGroup, fila: Fila): View {
        val vista = LayoutInflater.from(this).inflate(R.layout.item_privacidad_fila, contenedor, false)

        vista.findViewById<ImageView>(R.id.ivIconoFila).setImageResource(fila.icono)
        val titulo = vista.findViewById<TextView>(R.id.tvTituloFila)
        titulo.text = fila.titulo
        vista.findViewById<TextView>(R.id.tvSubtituloFila).text = fila.subtitulo

        if (fila.destructiva) {
            vista.setBackgroundResource(R.drawable.bg_perfil_cerrar_sesion)
            titulo.setTextColor(getColor(R.color.fogonRed))
        }

        vista.setOnClickListener { fila.accion() }
        contenedor.addView(vista)
        return vista
    }

    private fun pantallaActiva(): Boolean = !isFinishing && !isDestroyed
}