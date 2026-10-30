package com.example.elfogonlogin.controller

import android.util.Log
import com.example.elfogonlogin.view.MetodoPago
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

// Los datos del usuario que se ven en la pestaña Perfil.
data class PerfilUsuario(
    val nombre: String,
    val correo: String,
    // Solo los 8 dígitos (ej. "71234567"), o "" si todavía no lo ha puesto.
    val telefono: String,
    // El método con el que le gusta pagar; null = todavía no ha elegido uno.
    val metodoPagoPreferido: MetodoPago?,
    val notifPedidos: Boolean,
    val notifPromociones: Boolean,
    // La foto de perfil, ya reducida y convertida a texto (ver FotoPerfil).
    // null = todavía no ha puesto una.
    val fotoBase64: String? = null
)

// Todo esto vive en el mismo documento de siempre: usuarios/{uid}. Se guarda
// siempre con "merge" para no borrar lo que ya había ahí (por ejemplo tu
// dirección de entrega). El correo NO se guarda aquí: se toma de la cuenta.
// Ese documento solo lo puede leer y escribir su dueño (así lo dicen las
// reglas de Firestore), por eso ningún otro cliente puede ver tu foto.
object PerfilRepository {

    private const val ETIQUETA = "PerfilRepository"

    private fun documentoUsuario() =
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            FirebaseFirestore.getInstance().collection("usuarios").document(uid)
        }

    // "7123 4567"
    fun formatoTelefono(digitos: String): String = digitos.chunked(4).joinToString(" ")

    // Celulares de El Salvador: 8 dígitos, y los móviles empiezan con 6 o 7.
    fun telefonoValido(digitos: String): Boolean =
        digitos.length == 8 && (digitos[0] == '6' || digitos[0] == '7')

    // Siempre responde algo: si Firestore falla (por ejemplo sin internet),
    // devuelve al menos lo que sabe la cuenta (nombre y correo).
    fun obtenerPerfil(onResultado: (PerfilUsuario) -> Unit) {
        val usuario = FirebaseAuth.getInstance().currentUser
        val base = PerfilUsuario(
            nombre = usuario?.displayName.orEmpty(),
            correo = usuario?.email.orEmpty(),
            telefono = "",
            metodoPagoPreferido = null,
            notifPedidos = true,
            notifPromociones = true,
            fotoBase64 = null
        )

        val doc = documentoUsuario()
        if (doc == null) {
            onResultado(base)
            return
        }

        doc.get()
            .addOnSuccessListener { snapshot ->
                val nombreGuardado = snapshot.getString("nombre")
                val metodo = snapshot.getString("metodoPagoPreferido")?.let { guardado ->
                    MetodoPago.values().firstOrNull { it.name == guardado }
                }
                onResultado(
                    base.copy(
                        nombre = if (!nombreGuardado.isNullOrBlank()) nombreGuardado else base.nombre,
                        telefono = snapshot.getString("telefono").orEmpty(),
                        metodoPagoPreferido = metodo,
                        notifPedidos = snapshot.getBoolean("notifPedidos") ?: true,
                        notifPromociones = snapshot.getBoolean("notifPromociones") ?: true,
                        fotoBase64 = snapshot.getString("fotoBase64")
                    )
                )
            }
            .addOnFailureListener { error ->
                Log.w(ETIQUETA, "No se pudo leer el perfil", error)
                onResultado(base)
            }
    }

    // Guarda nombre y teléfono. El nombre también se cambia en la cuenta, para
    // que el saludo del Inicio ("Hola, Juan") use el nombre nuevo.
    // onResultado: (true, null) si salió bien, (false, motivo) si falló.
    fun guardarDatos(nombre: String, telefono: String, onResultado: (Boolean, String?) -> Unit) {
        val usuario = FirebaseAuth.getInstance().currentUser
        val doc = documentoUsuario()
        if (usuario == null || doc == null) {
            onResultado(false, "No hay una sesión iniciada")
            return
        }

        doc.set(mapOf("nombre" to nombre, "telefono" to telefono), SetOptions.merge())
            .addOnSuccessListener {
                val cambio = UserProfileChangeRequest.Builder().setDisplayName(nombre).build()
                usuario.updateProfile(cambio).addOnCompleteListener { tarea ->
                    if (!tarea.isSuccessful) {
                        Log.w(ETIQUETA, "No se pudo actualizar el nombre de la cuenta", tarea.exception)
                    }
                    onResultado(true, null)
                }
            }
            .addOnFailureListener { error ->
                Log.e(ETIQUETA, "No se pudieron guardar los datos", error)
                onResultado(false, error.message ?: "Error desconocido")
            }
    }

    // El método que se mostrará ya elegido en la pantalla de Pago.
    fun guardarMetodoPagoPreferido(metodo: MetodoPago, onResultado: (Boolean, String?) -> Unit) {
        guardarCampos(mapOf("metodoPagoPreferido" to metodo.name), onResultado)
    }

    // Solo guarda la preferencia. Los avisos al celular todavía no existen;
    // cuando se activen, leerán esto para saber a quién avisarle qué.
    fun guardarNotificaciones(pedidos: Boolean, promociones: Boolean, onResultado: (Boolean, String?) -> Unit) {
        guardarCampos(mapOf("notifPedidos" to pedidos, "notifPromociones" to promociones), onResultado)
    }

    // La foto ya viene reducida y convertida a texto por FotoPerfil.
    fun guardarFoto(fotoBase64: String, onResultado: (Boolean, String?) -> Unit) {
        guardarCampos(mapOf("fotoBase64" to fotoBase64), onResultado)
    }

    // Borra el campo de la foto del documento (vuelve a verse la silueta).
    fun quitarFoto(onResultado: (Boolean, String?) -> Unit) {
        guardarCampos(mapOf("fotoBase64" to FieldValue.delete()), onResultado)
    }

    private fun guardarCampos(campos: Map<String, Any>, onResultado: (Boolean, String?) -> Unit) {
        val doc = documentoUsuario()
        if (doc == null) {
            onResultado(false, "No hay una sesión iniciada")
            return
        }
        doc.set(campos, SetOptions.merge())
            .addOnSuccessListener { onResultado(true, null) }
            .addOnFailureListener { error ->
                Log.e(ETIQUETA, "No se pudo guardar", error)
                onResultado(false, error.message ?: "Error desconocido")
            }
    }
}