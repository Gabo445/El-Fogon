package com.example.elfogonlogin.controller

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Quién es administrador. NO se decide en la app: se decide en Firestore.
// Eres administrador si existe el documento admins/{tu uid}, que solo tú puedes
// crear a mano desde la consola de Firebase (las reglas no dejan que la app lo
// cree). Aunque alguien lograra abrir la pantalla del panel, las reglas de
// Firestore le negarían leer y cambiar los pedidos de los demás.
object AdminRepository {

    private const val ETIQUETA = "AdminRepository"

    // onResultado(true) si la cuenta con la sesión iniciada es administrador.
    // Si no hay sesión, no hay internet o falla algo, responde false.
    fun esAdmin(onResultado: (Boolean) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            onResultado(false)
            return
        }

        FirebaseFirestore.getInstance()
            .collection("admins")
            .document(uid)
            .get()
            .addOnSuccessListener { documento -> onResultado(documento.exists()) }
            .addOnFailureListener { error ->
                Log.w(ETIQUETA, "No se pudo comprobar si es administrador", error)
                onResultado(false)
            }
    }
}