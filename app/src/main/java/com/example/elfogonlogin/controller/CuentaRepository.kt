package com.example.elfogonlogin.controller

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

// Cómo terminó un intento de eliminar la cuenta.
enum class ResultadoCuenta { EXITO, CONTRASENA_INCORRECTA, ERROR }

// Las acciones de seguridad de la cuenta: cambiar la contraseña y eliminarla.
object CuentaRepository {

    private const val ETIQUETA = "CuentaRepository"

    // Manda al correo de la cuenta un enlace para elegir una contraseña nueva.
    // La contraseña actual sigue funcionando hasta que se use el enlace.
    // onResultado: (true, null) si salió bien, (false, motivo) si falló.
    fun enviarCorreoCambioContrasena(onResultado: (Boolean, String?) -> Unit) {
        val correo = FirebaseAuth.getInstance().currentUser?.email
        if (correo.isNullOrBlank()) {
            onResultado(false, "Tu cuenta no tiene un correo registrado")
            return
        }

        FirebaseAuth.getInstance().sendPasswordResetEmail(correo)
            .addOnSuccessListener { onResultado(true, null) }
            .addOnFailureListener { error ->
                Log.e(ETIQUETA, "No se pudo enviar el correo de contraseña", error)
                onResultado(false, mensajeDeError(error))
            }
    }

    // Elimina la cuenta de forma definitiva. El orden es a propósito:
    //   1) se confirma la contraseña (si está mal, no se borra NADA),
    //   2) se borran los datos de Firestore (perfil, foto, dirección, favoritos
    //      y carrito) en un solo lote: o se borran todos o ninguno,
    //   3) por último se elimina la cuenta de inicio de sesión.
    // Los pedidos ya hechos NO se borran: quedan como registro del restaurante.
    fun eliminarCuenta(contrasena: String, onResultado: (ResultadoCuenta, String?) -> Unit) {
        val usuario = FirebaseAuth.getInstance().currentUser
        val correo = usuario?.email
        if (usuario == null || correo.isNullOrBlank()) {
            onResultado(ResultadoCuenta.ERROR, "No hay una sesión iniciada")
            return
        }

        val credencial = EmailAuthProvider.getCredential(correo, contrasena)
        usuario.reauthenticate(credencial)
            .addOnSuccessListener { borrarDatosYCuenta(usuario, onResultado) }
            .addOnFailureListener { error ->
                if (error is FirebaseAuthInvalidCredentialsException || error is FirebaseAuthInvalidUserException) {
                    onResultado(ResultadoCuenta.CONTRASENA_INCORRECTA, null)
                } else {
                    Log.e(ETIQUETA, "No se pudo confirmar la contraseña", error)
                    onResultado(ResultadoCuenta.ERROR, mensajeDeError(error))
                }
            }
    }

    private fun borrarDatosYCuenta(usuario: FirebaseUser, onResultado: (ResultadoCuenta, String?) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val docUsuario = db.collection("usuarios").document(usuario.uid)

        // El carrito vive dentro del usuario como subcolección, y al borrar el
        // documento del usuario sus subcolecciones NO se borran solas: se
        // borran una por una en el mismo lote.
        docUsuario.collection("carrito").get()
            .addOnSuccessListener { carrito ->
                val lote = db.batch()
                for (documento in carrito.documents) {
                    lote.delete(documento.reference)
                }
                lote.delete(docUsuario)

                lote.commit()
                    .addOnSuccessListener {
                        usuario.delete()
                            .addOnSuccessListener { onResultado(ResultadoCuenta.EXITO, null) }
                            .addOnFailureListener { error ->
                                Log.e(ETIQUETA, "Se borraron los datos pero no la cuenta", error)
                                onResultado(
                                    ResultadoCuenta.ERROR,
                                    "Se borraron tus datos, pero no se pudo cerrar la cuenta. " +
                                            "Inténtalo de nuevo. (${mensajeDeError(error)})"
                                )
                            }
                    }
                    .addOnFailureListener { error ->
                        Log.e(ETIQUETA, "No se pudieron borrar los datos", error)
                        onResultado(ResultadoCuenta.ERROR, mensajeDeError(error))
                    }
            }
            .addOnFailureListener { error ->
                Log.e(ETIQUETA, "No se pudo leer el carrito para borrarlo", error)
                onResultado(ResultadoCuenta.ERROR, mensajeDeError(error))
            }
    }

    private fun mensajeDeError(error: Exception): String = when (error) {
        is FirebaseNetworkException -> "Sin conexión a internet. Revísala e inténtalo de nuevo."
        is FirebaseTooManyRequestsException -> "Demasiados intentos. Espera unos minutos e inténtalo de nuevo."
        else -> error.message ?: "Error desconocido"
    }
}