package com.example.elfogonlogin.controller

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

// Firebase Authentication devuelve sus errores en inglés (ej. "The email
// address is already in use by another account."). Este helper los traduce
// a mensajes en español para mostrárselos al usuario en los Toast.
object FirebaseErrorHelper {

    fun mensajeAmigable(exception: Exception): String {
        return when (exception) {
            is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese correo"
            is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil, usa al menos 6 caracteres"
            is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos"
            is FirebaseAuthInvalidUserException -> "No existe una cuenta con ese correo"
            is FirebaseNetworkException -> "Revisa tu conexión a internet e intenta de nuevo"
            else -> exception.localizedMessage ?: "Ocurrió un error, intenta de nuevo"
        }
    }
}