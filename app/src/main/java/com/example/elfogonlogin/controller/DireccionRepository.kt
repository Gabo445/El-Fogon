package com.example.elfogonlogin.controller

import android.location.Location
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

data class DireccionEntrega(
    val zona: String = "",
    val direccionEspecifica: String = "",
    val latitud: Double? = null,
    val longitud: Double? = null
)

object DireccionRepository {

    // Universidad Don Bosco, Soyapango — donde está ubicado "El Fogón".
    const val LATITUD_RESTAURANTE = 13.7159
    const val LONGITUD_RESTAURANTE = -89.1537

    // Hasta dónde llega el reparto, en kilómetros EN LÍNEA RECTA desde El
    // Fogón. Con 15 km quedan adentro las 9 zonas de la lista (la más lejana,
    // San Martín, está a unos 11 km del centro) y queda afuera, por ejemplo,
    // Santiago Nonualco (unos 31 km). Si algún día quieres entregar más lejos
    // o más cerca, este es el único número que hay que cambiar.
    const val RADIO_ENTREGA_KM = 15.0

    // Distancia en kilómetros entre El Fogón y un punto del GPS. Funciona sin
    // internet: es solo una cuenta con las dos coordenadas.
    fun distanciaAlRestauranteKm(latitud: Double, longitud: Double): Double {
        val resultado = FloatArray(1)
        Location.distanceBetween(
            LATITUD_RESTAURANTE, LONGITUD_RESTAURANTE,
            latitud, longitud,
            resultado
        )
        return resultado[0] / 1000.0
    }

    fun estaDentroDelAreaDeEntrega(latitud: Double, longitud: Double): Boolean =
        distanciaAlRestauranteKm(latitud, longitud) <= RADIO_ENTREGA_KM

    // Las únicas zonas donde "El Fogón" (ubicado en Don Bosco, Soyapango)
    // puede entregar pedidos. Como el usuario elige de esta lista (no
    // escribe libre), es imposible guardar una zona que no esté aquí —
    // la restricción la hace el propio código, no una validación de texto.
    val zonasValidas = listOf(
        "Soyapango",
        "Ilopango",
        "San Marcos",
        "Santo Tomás",
        "San Salvador Oriente",
        "Cuscatancingo",
        "Ciudad Delgado",
        "San Martín",
        "Tonacatepeque"
    )

    // Tiempo estimado por zona, calculado a mano según qué tan lejos está
    // cada una de "El Fogón". No es en tiempo real (eso necesitaría una
    // API de rutas con costo), pero sí cambia según la zona guardada.
    val tiemposEstimadosPorZona = mapOf(
        "Soyapango" to "15 - 25 min",
        "Ilopango" to "20 - 30 min",
        "San Marcos" to "25 - 35 min",
        "Santo Tomás" to "20 - 30 min",
        "San Salvador Oriente" to "25 - 35 min",
        "Cuscatancingo" to "25 - 35 min",
        "Ciudad Delgado" to "20 - 30 min",
        "San Martín" to "20 - 30 min",
        "Tonacatepeque" to "30 - 40 min"
    )

    private fun documentoUsuario() =
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            FirebaseFirestore.getInstance().collection("usuarios").document(uid)
        }

    // Una dirección solo cuenta como dirección si tiene las coordenadas del
    // GPS Y esas coordenadas están dentro del área de entrega:
    //  - Sin coordenadas: se escribió a mano antes de que la dirección se
    //    llenara solo con el botón "Usar mi ubicación"; no se puede confiar.
    //  - Fuera del área: se guardó antes de existir el límite de reparto, o el
    //    límite se hizo más pequeño después.
    // Para el resto de la app (Carrito, Pago, Direcciones guardadas) cualquiera
    // de los dos casos es lo mismo que "no hay dirección": se vuelve a pedir
    // usar la ubicación.
    fun obtenerDireccion(onResultado: (DireccionEntrega?) -> Unit) {
        val doc = documentoUsuario()
        if (doc == null) {
            onResultado(null)
            return
        }
        doc.get()
            .addOnSuccessListener { snapshot ->
                val zona = snapshot.getString("zonaEntrega")
                val lat = snapshot.getDouble("latitud")
                val lng = snapshot.getDouble("longitud")
                if (zona != null && lat != null && lng != null &&
                    estaDentroDelAreaDeEntrega(lat, lng)
                ) {
                    val direccion = snapshot.getString("direccionEspecifica") ?: ""
                    onResultado(DireccionEntrega(zona, direccion, lat, lng))
                } else {
                    onResultado(null)
                }
            }
            .addOnFailureListener { onResultado(null) }
    }

    // zona DEBE venir de zonasValidas (la pantalla solo deja elegir de ahí),
    // así que siempre queda garantizado que es una zona donde sí se entrega.
    // latitud y longitud ya NO son opcionales: la dirección siempre viene del
    // GPS, así que no se puede guardar una dirección sin su punto en el mapa.
    fun guardarDireccion(
        zona: String,
        direccionEspecifica: String,
        latitud: Double,
        longitud: Double,
        onListo: () -> Unit
    ) {
        val doc = documentoUsuario() ?: return
        val datos = mapOf<String, Any>(
            "zonaEntrega" to zona,
            "direccionEspecifica" to direccionEspecifica,
            "latitud" to latitud,
            "longitud" to longitud
        )
        doc.set(datos, SetOptions.merge())
            .addOnSuccessListener { onListo() }
    }

    fun borrarDireccion(onListo: () -> Unit) {
        val doc = documentoUsuario() ?: return
        doc.update(
            mapOf(
                "zonaEntrega" to FieldValue.delete(),
                "direccionEspecifica" to FieldValue.delete(),
                "latitud" to FieldValue.delete(),
                "longitud" to FieldValue.delete()
            )
        ).addOnSuccessListener { onListo() }
    }
}