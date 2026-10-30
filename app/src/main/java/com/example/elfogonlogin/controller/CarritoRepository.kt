package com.example.elfogonlogin.controller

import com.example.elfogonlogin.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlin.collections.get

// Una fila real del carrito: qué producto, qué tamaño eligieron, qué extras
// (si eligieron alguno) y cuántos. Vive en Firestore, dentro de la cuenta
// de cada usuario — por eso NO guardamos la foto directamente (un número de
// recurso de Android no significa nada en la nube), guardamos el detalleId
// y buscamos la foto con DetalleProductoRepository al mostrarlo.
data class ItemCarrito(
    val idDocumento: String = "",
    val nombreProducto: String = "",
    val detalleId: String = "",
    val nombreTamano: String = "",
    val precioTamano: Double = 0.0,
    val extrasSeleccionados: List<OpcionPersonalizacion> = emptyList(),
    val cantidad: Int = 1
) {
    fun precioUnitario(): Double = precioTamano + extrasSeleccionados.sumOf { it.precio }
    fun subtotalLinea(): Double = precioUnitario() * cantidad
    fun imagenResId(): Int =
        DetalleProductoRepository.obtener(detalleId)?.imagenResId ?: R.drawable.bg_placeholder_rounded
}

object CarritoRepository {

    // Copia local que se actualiza sola cada vez que Firestore avisa un
    // cambio (porque tú lo cambiaste, o porque cambió desde otro celular
    // con la misma cuenta). obtenerTotalItems()/calcularSubtotal() leen de
    // aquí, por eso cada pantalla necesita tener un listener activo.
    private var cacheItems: List<ItemCarrito> = emptyList()

    // El carrito vive en: usuarios/{uid}/carrito/{idDocumento}
    private fun coleccionCarrito() =
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .collection("carrito")
        }

    // Empieza a escuchar el carrito en tiempo real. onCambio se llama de
    // inmediato con lo que haya, y de nuevo cada vez que algo cambie.
    // Hay que guardar el ListenerRegistration que devuelve y cancelarlo
    // (.remove()) cuando la pantalla ya no esté visible (onPause).
    fun escucharCambios(onCambio: (List<ItemCarrito>) -> Unit): ListenerRegistration? {
        val coleccion = coleccionCarrito() ?: return null
        return coleccion.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener

            val items = snapshot.documents.mapNotNull { doc ->
                val nombreProducto = doc.getString("nombreProducto") ?: return@mapNotNull null
                val detalleId = doc.getString("detalleId") ?: ""
                val nombreTamano = doc.getString("nombreTamano") ?: ""
                val precioTamano = doc.getDouble("precioTamano") ?: 0.0
                val cantidad = (doc.getLong("cantidad") ?: 1L).toInt()

                val extrasRaw = doc.get("extras") as? List<*> ?: emptyList<Any>()
                val extras = extrasRaw.mapNotNull { extraRaw ->
                    val mapa = extraRaw as? Map<*, *> ?: return@mapNotNull null
                    val nombreExtra = mapa["nombre"] as? String ?: return@mapNotNull null
                    val precioExtra = (mapa["precio"] as? Number)?.toDouble() ?: 0.0
                    OpcionPersonalizacion(nombreExtra, precioExtra)
                }

                ItemCarrito(
                    idDocumento = doc.id,
                    nombreProducto = nombreProducto,
                    detalleId = detalleId,
                    nombreTamano = nombreTamano,
                    precioTamano = precioTamano,
                    extrasSeleccionados = extras,
                    cantidad = cantidad
                )
            }
            cacheItems = items
            onCambio(items)
        }
    }

    // Si ya hay en el carrito un producto con el mismo nombre, tamaño y
    // extras, solo le sube la cantidad en vez de crear una fila repetida.
    fun agregarItem(nuevoItem: ItemCarrito) {
        val coleccion = coleccionCarrito() ?: return

        val existente = cacheItems.find {
            it.nombreProducto == nuevoItem.nombreProducto &&
                    it.nombreTamano == nuevoItem.nombreTamano &&
                    it.extrasSeleccionados.map { extra -> extra.nombre }.toSet() ==
                    nuevoItem.extrasSeleccionados.map { extra -> extra.nombre }.toSet()
        }

        if (existente != null) {
            coleccion.document(existente.idDocumento)
                .update("cantidad", existente.cantidad + nuevoItem.cantidad)
            return
        }

        val datos = hashMapOf(
            "nombreProducto" to nuevoItem.nombreProducto,
            "detalleId" to nuevoItem.detalleId,
            "nombreTamano" to nuevoItem.nombreTamano,
            "precioTamano" to nuevoItem.precioTamano,
            "extras" to nuevoItem.extrasSeleccionados.map {
                hashMapOf("nombre" to it.nombre, "precio" to it.precio)
            },
            "cantidad" to nuevoItem.cantidad
        )
        coleccion.add(datos)
    }

    fun actualizarCantidad(idDocumento: String, nuevaCantidad: Int) {
        val coleccion = coleccionCarrito() ?: return
        if (nuevaCantidad <= 0) {
            quitarItem(idDocumento)
            return
        }
        coleccion.document(idDocumento).update("cantidad", nuevaCantidad)
    }

    fun quitarItem(idDocumento: String) {
        val coleccion = coleccionCarrito() ?: return
        coleccion.document(idDocumento).delete()
    }

    // Para cuando lleguemos a "confirmar pedido" más adelante.
    fun vaciar() {
        val coleccion = coleccionCarrito() ?: return
        coleccion.get().addOnSuccessListener { snapshot ->
            for (doc in snapshot.documents) {
                doc.reference.delete()
            }
        }
    }

    fun obtenerItems(): List<ItemCarrito> = cacheItems

    fun obtenerTotalItems(): Int = cacheItems.sumOf { it.cantidad }

    fun calcularSubtotal(): Double = cacheItems.sumOf { it.subtotalLinea() }
}