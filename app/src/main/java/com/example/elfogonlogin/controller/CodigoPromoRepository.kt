package com.example.elfogonlogin.controller

import com.google.firebase.firestore.FirebaseFirestore

data class CodigoPromo(
    val codigo: String,
    val tipoDescuento: String, // "porcentaje" o "fijo"
    val valor: Double
)

object CodigoPromoRepository {

    // Busca el código directo por su nombre en Firestore (colección
    // "codigosPromocionales", un documento por código). Si el documento
    // existe, el código es válido. Si tú lo borras desde Firestore
    // Console, automáticamente deja de funcionar — no hace falta fecha
    // de vencimiento ni nada más.
    fun validarCodigo(codigoEscrito: String, onResultado: (CodigoPromo?) -> Unit) {
        val codigo = codigoEscrito.trim().uppercase()
        if (codigo.isEmpty()) {
            onResultado(null)
            return
        }
        FirebaseFirestore.getInstance()
            .collection("codigosPromocionales")
            .document(codigo)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val tipo = snapshot.getString("tipoDescuento") ?: "fijo"
                    val valor = snapshot.getDouble("valor") ?: 0.0
                    onResultado(CodigoPromo(codigo, tipo, valor))
                } else {
                    onResultado(null)
                }
            }
            .addOnFailureListener { onResultado(null) }
    }
}