package com.example.elfogonlogin.controller

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

// Los productos favoritos de cada persona. Se guardan como una lista de ids
// (por ejemplo ["hamburguesa", "pizza"]) en el campo "favoritos" de su propio
// documento usuarios/{uid}. Ese documento ya está protegido por tus reglas de
// Firestore (solo su dueño lo lee y lo escribe), así que NO hace falta cambiar
// nada en Firebase. Como queda guardado en la cuenta, los favoritos siguen ahí
// aunque cierres la app, la reinstales o entres desde otro celular.
object FavoritosRepository {

    private const val ETIQUETA = "FavoritosRepository"

    private fun documentoUsuario() =
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            FirebaseFirestore.getInstance().collection("usuarios").document(uid)
        }

    // Escucha en tiempo real la lista de favoritos: cada vez que se marca o se
    // quita uno (desde cualquier pantalla), aquí llega la lista nueva. La lista
    // viene con el MÁS RECIENTE PRIMERO. Hay que guardar el ListenerRegistration
    // y quitarlo en onPause.
    fun escucharFavoritos(onCambio: (List<String>) -> Unit): ListenerRegistration? {
        val doc = documentoUsuario() ?: return null

        return doc.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(ETIQUETA, "No se pudieron leer los favoritos", error)
                return@addSnapshotListener
            }
            if (snapshot == null) return@addSnapshotListener

            val guardados = snapshot.get("favoritos") as? List<*> ?: emptyList<Any>()
            // Se guardan en el orden en que se marcaron; se voltea para que el
            // último que marcaste salga de primero.
            onCambio(guardados.mapNotNull { it as? String }.reversed())
        }
    }

    // Marca (true) o quita (false) un producto de favoritos. Se usan
    // arrayUnion / arrayRemove para que agregar o quitar uno nunca borre los demás.
    // onResultado: (true, null) si salió bien, (false, motivo) si falló.
    fun marcar(idProducto: String, esFavorito: Boolean, onResultado: (Boolean, String?) -> Unit) {
        val doc = documentoUsuario()
        if (doc == null) {
            onResultado(false, "No hay una sesión iniciada")
            return
        }

        val cambio = if (esFavorito) FieldValue.arrayUnion(idProducto) else FieldValue.arrayRemove(idProducto)

        doc.set(mapOf("favoritos" to cambio), SetOptions.merge())
            .addOnSuccessListener { onResultado(true, null) }
            .addOnFailureListener { error ->
                Log.e(ETIQUETA, "No se pudo guardar el favorito", error)
                onResultado(false, error.message ?: "Error desconocido")
            }
    }
}