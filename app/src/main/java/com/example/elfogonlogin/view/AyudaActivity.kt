package com.example.elfogonlogin.view

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.elfogonlogin.R
import com.example.elfogonlogin.controller.DireccionRepository
import com.example.elfogonlogin.model.ResumenPedido
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale
import kotlin.math.roundToInt

// "Ayuda y soporte": dos formas de contacto (WhatsApp y correo) y las
// preguntas frecuentes, agrupadas por tema. Cada pregunta se abre y se cierra
// al tocarla. El costo de envío, el IVA y las zonas con su tiempo se toman de la
// propia app, así que si los cambias en el código, esta pantalla se actualiza sola.
class AyudaActivity : AppCompatActivity() {

    companion object {
        // Datos de contacto del restaurante. Si algún día cambian, se cambian solo aquí.
        private const val CORREO_SOPORTE = "aportilloalfaro@gmail.com"
        private const val WHATSAPP_PARA_ENLACE = "50368404442" // sin "+" ni espacios: así lo pide WhatsApp
        private const val WHATSAPP_VISIBLE = "+503 6840 4442"
    }

    private class Contacto(
        val icono: Int,
        val titulo: String,
        val detalle: String,
        val accion: () -> Unit
    )

    private class Pregunta(val pregunta: String, val respuesta: String)

    private class Seccion(val titulo: String, val preguntas: List<Pregunta>)

    private lateinit var ivBack: View
    private lateinit var contenedorContacto: ViewGroup
    private lateinit var contenedorPreguntas: ViewGroup
    private lateinit var tvVersionApp: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_ayuda)

        ajustarBarrasDelSistema()
        inicializarVistas()
        construirContactos()
        construirPreguntas()
        tvVersionApp.text = "El Fogón · Versión ${versionDeLaApp()}\nGracias por pedir con nosotros."
        ivBack.setOnClickListener { finish() }
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
        contenedorContacto = findViewById(R.id.contenedorContacto)
        contenedorPreguntas = findViewById(R.id.contenedorPreguntas)
        tvVersionApp = findViewById(R.id.tvVersionApp)
    }

    // ---------------------------------------------------------------------
    // Contacto
    // ---------------------------------------------------------------------

    private fun construirContactos() {
        val contactos = listOf(
            Contacto(R.drawable.ic_perfil_telefono, "WhatsApp", WHATSAPP_VISIBLE) { abrirWhatsApp() },
            Contacto(R.drawable.ic_perfil_correo, "Correo electrónico", CORREO_SOPORTE) { escribirCorreo() }
        )

        for (contacto in contactos) {
            val fila = LayoutInflater.from(this).inflate(R.layout.item_ayuda_contacto, contenedorContacto, false)
            fila.findViewById<ImageView>(R.id.ivIconoContacto).setImageResource(contacto.icono)
            fila.findViewById<TextView>(R.id.tvTituloContacto).text = contacto.titulo
            fila.findViewById<TextView>(R.id.tvDetalleContacto).text = contacto.detalle
            fila.setOnClickListener { contacto.accion() }
            contenedorContacto.addView(fila)
        }
    }

    // Abre WhatsApp con el chat del restaurante y un mensaje ya escrito (con tu
    // nombre, si lo tienes guardado). Si no tienes WhatsApp, se abre en el navegador.
    private fun abrirWhatsApp() {
        val nombre = FirebaseAuth.getInstance().currentUser?.displayName
        val saludo = if (!nombre.isNullOrBlank()) "Hola, soy $nombre. " else "Hola, "
        val mensaje = saludo + "necesito ayuda con la app de El Fogón."

        val enlace = Uri.parse("https://wa.me/$WHATSAPP_PARA_ENLACE?text=${Uri.encode(mensaje)}")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, enlace))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No se pudo abrir WhatsApp en este celular", Toast.LENGTH_LONG).show()
        }
    }

    // Abre tu app de correo con el mensaje para el restaurante ya empezado.
    private fun escribirCorreo() {
        val correoCuenta = FirebaseAuth.getInstance().currentUser?.email.orEmpty()

        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:")
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(CORREO_SOPORTE))
        intent.putExtra(Intent.EXTRA_SUBJECT, "Ayuda con la app El Fogón")
        intent.putExtra(
            Intent.EXTRA_TEXT,
            "Hola, necesito ayuda con lo siguiente:\n\n\n\nCorreo de mi cuenta: $correoCuenta"
        )

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                this,
                "No hay una app de correo en este celular. Escríbenos a $CORREO_SOPORTE",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ---------------------------------------------------------------------
    // Preguntas frecuentes
    // ---------------------------------------------------------------------

    private fun construirPreguntas() {
        for (seccion in crearSecciones()) {
            val titulo = LayoutInflater.from(this).inflate(R.layout.item_ayuda_titulo, contenedorPreguntas, false)
            titulo.findViewById<TextView>(R.id.tvTituloSeccion).text = seccion.titulo
            contenedorPreguntas.addView(titulo)

            for (pregunta in seccion.preguntas) {
                contenedorPreguntas.addView(crearFilaPregunta(pregunta))
            }
        }
    }

    // Una pregunta: al tocarla se despliega la respuesta (y la flecha se voltea).
    private fun crearFilaPregunta(pregunta: Pregunta): View {
        val fila = LayoutInflater.from(this).inflate(R.layout.item_ayuda_pregunta, contenedorPreguntas, false)
        val tvRespuesta = fila.findViewById<TextView>(R.id.tvRespuesta)
        val ivFlecha = fila.findViewById<ImageView>(R.id.ivFlechaPregunta)

        fila.findViewById<TextView>(R.id.tvPregunta).text = pregunta.pregunta
        tvRespuesta.text = pregunta.respuesta

        fila.setOnClickListener {
            val abrir = tvRespuesta.visibility != View.VISIBLE
            TransitionManager.beginDelayedTransition(contenedorPreguntas)
            tvRespuesta.visibility = if (abrir) View.VISIBLE else View.GONE
            ivFlecha.animate().rotation(if (abrir) -90f else 90f).setDuration(150).start()
        }
        return fila
    }

    // Los textos de ayuda. Están escritos según lo que la app hace HOY. Si
    // cambia algo (por ejemplo cuando haya cobro real con tarjeta), hay que
    // actualizar la respuesta correspondiente.
    private fun crearSecciones(): List<Seccion> {
        // Estos tres datos se toman de la app para que nunca queden desactualizados.
        val costoEnvio = String.format(Locale.US, "$%.2f", ResumenPedido.COSTO_ENTREGA)
        val iva = (ResumenPedido.TASA_IMPUESTO * 100).roundToInt()
        val zonas = DireccionRepository.zonasValidas.joinToString("\n") { zona ->
            "• $zona: ${DireccionRepository.tiemposEstimadosPorZona[zona] ?: "30 - 40 min"}"
        }

        return listOf(
            Seccion(
                "Mis pedidos",
                listOf(
                    Pregunta(
                        "¿Cómo hago un pedido?",
                        "1. Elige tu producto en Inicio o en Menú (toca su foto para elegir tamaño y extras).\n" +
                                "2. Toca “Agregar al carrito”.\n" +
                                "3. En el Carrito revisa tu pedido y toca “Proceder al pago”.\n" +
                                "4. Elige cómo pagar y toca “Confirmar pago”.\n\n" +
                                "Tu pedido aparecerá en la pestaña Pedidos con su número."
                    ),
                    Pregunta(
                        "¿Cómo sigo mi pedido?",
                        "Entra a la pestaña Pedidos y toca “Ver detalles”. Verás los pasos: Pedido recibido, " +
                                "En preparación, En camino y Entregado, con la hora de cada uno. El estado se actualiza " +
                                "solo, sin que tengas que recargar.\n\n" +
                                "El seguimiento del repartidor en el mapa estará disponible próximamente."
                    ),
                    Pregunta(
                        "¿Puedo cancelar o cambiar mi pedido?",
                        "Por ahora no se puede desde la app. Escríbenos cuanto antes por WhatsApp o por correo " +
                                "con tu número de pedido y veremos qué se puede hacer."
                    ),
                    Pregunta(
                        "Mi pedido llegó incompleto o con algún problema",
                        "Lamentamos que haya pasado. Escríbenos por WhatsApp o por correo con tu número de pedido " +
                                "(por ejemplo #012) y, si puedes, una foto. Lo revisaremos para ayudarte."
                    )
                )
            ),
            Seccion(
                "Entrega",
                listOf(
                    Pregunta(
                        "¿A qué zonas entregan y cuánto tarda?",
                        "Entregamos en estas zonas, con su tiempo estimado:\n\n$zonas\n\n" +
                                "Si tu zona no aparece, todavía no llegamos hasta allá. El tiempo de tu zona también " +
                                "lo ves en el Carrito."
                    ),
                    Pregunta(
                        "¿Cómo guardo o cambio mi dirección?",
                        "En el Carrito toca la fila de dirección, elige tu zona de la lista y escribe tu dirección. " +
                                "Si quieres, toca “Usar mi ubicación” para marcar tu punto exacto en el mapa. " +
                                "También puedes verla y editarla desde el menú ☰ → Direcciones.\n\n" +
                                "Necesitas una dirección guardada para poder pagar."
                    )
                )
            ),
            Seccion(
                "Pagos y promociones",
                listOf(
                    Pregunta(
                        "¿Qué formas de pago aceptan?",
                        "Efectivo (pagas al recibir tu pedido), tarjeta de crédito o débito, Tigo Money y " +
                                "Chivo Wallet. En Perfil → Métodos de pago puedes dejar elegido el que prefieres y te " +
                                "aparecerá ya marcado al pagar.\n\n" +
                                "Importante: en esta versión de la app el pago con tarjeta y billeteras es de prueba. " +
                                "Tu pedido se registra, pero no se hace ningún cobro."
                    ),
                    Pregunta(
                        "¿Es seguro pagar con tarjeta?",
                        "No guardamos los datos de tu tarjeta: ni el número completo, ni el vencimiento, ni el " +
                                "código de seguridad. En tu pedido solo queda el final de la tarjeta (los últimos 4 " +
                                "dígitos) para que la reconozcas."
                    ),
                    Pregunta(
                        "¿Cómo uso un código promocional?",
                        "En el Carrito escribe tu código en “Código promocional” y toca “Aplicar”. El descuento se " +
                                "resta antes de los impuestos y lo ves en el resumen. Solo se usa un código por pedido " +
                                "y da igual si lo escribes en mayúsculas o minúsculas.\n\n" +
                                "Si sales del Carrito, vuelve a aplicarlo antes de pagar."
                    ),
                    Pregunta(
                        "¿Qué incluye el total?",
                        "El subtotal de tus productos, el costo de entrega ($costoEnvio), el descuento si usaste " +
                                "un código y los impuestos (IVA $iva%). Ves todo el desglose antes de confirmar el pago."
                    )
                )
            ),
            Seccion(
                "Mi cuenta",
                listOf(
                    Pregunta(
                        "¿Cómo cambio mis datos o mi foto?",
                        "Ve a Perfil → Mis datos para cambiar tu nombre y tu teléfono. Para cambiar tu foto, " +
                                "tócala en el Perfil. El correo es el de tu cuenta y no se puede cambiar.\n\n" +
                                "Te recomendamos guardar tu teléfono: lo usamos para contactarte por tu pedido."
                    ),
                    Pregunta(
                        "¿Cómo funcionan los favoritos?",
                        "Toca el corazón en un producto para guardarlo. Los encuentras en Perfil → Favoritos y " +
                                "desde ahí puedes agregarlos al carrito. Se guardan en tu cuenta, así que siguen ahí " +
                                "aunque cierres la app."
                    ),
                    Pregunta(
                        "¿Quién ve mi información?",
                        "Tus datos son privados: ningún otro cliente puede ver tu perfil, tus pedidos ni tus " +
                                "favoritos. Tu foto, nombre y teléfono solo los ve el restaurante cuando haces un pedido."
                    )
                )
            ),
            Seccion(
                "Problemas con la app",
                listOf(
                    Pregunta(
                        "No puedo iniciar sesión o la app no carga",
                        "Revisa que tengas internet y que tu correo y contraseña estén bien escritos: la app " +
                                "necesita conexión para mostrar tus pedidos y guardar cambios. Si el problema sigue, " +
                                "escríbenos y te ayudamos."
                    ),
                    Pregunta(
                        "¿Recibiré notificaciones?",
                        "En Perfil → Notificaciones eliges qué avisos quieres. Los avisos al celular se activarán " +
                                "próximamente; mientras tanto, puedes ver el estado de tu pedido en la pestaña Pedidos."
                    )
                )
            )
        )
    }

    @Suppress("DEPRECATION")
    private fun versionDeLaApp(): String {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }
}