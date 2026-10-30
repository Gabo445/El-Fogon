package com.example.elfogonlogin.controller

import android.util.Log
import com.example.elfogonlogin.view.MetodoPago
import com.example.elfogonlogin.model.ResumenPedido
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.Date
import java.util.Locale
import kotlin.collections.get

// Un producto dentro de un pedido ya hecho. Es una COPIA de lo que había en
// el carrito en ese momento: si mañana cambias un precio, el pedido viejo
// sigue mostrando el precio con el que se hizo.
data class ItemPedido(
    val nombreProducto: String,
    val nombreTamano: String,
    val cantidad: Int,
    val subtotal: Double,
    val extras: List<String>
)

data class Pedido(
    val id: String,
    val numero: Long,
    val estado: String,
    val fechaCreacion: Date?,
    // La hora de cada paso. La de "pendiente" es fechaCreacion (se guarda sola
    // al confirmar el pago); las demás las pone el panel de control cada vez
    // que el administrador avanza el pedido.
    val horaRecibido: Date?,
    val horaPreparando: Date?,
    val horaCamino: Date?,
    val horaEntregado: Date?,
    val items: List<ItemPedido>,
    val subtotal: Double,
    val descuento: Double,
    val entrega: Double,
    val impuestos: Double,
    val total: Double,
    val codigoPromo: String?,
    val metodoPago: String,
    val detallePago: String,
    val pagoDePrueba: Boolean,
    val zona: String,
    val direccionEspecifica: String,
    val latitud: Double?,
    val longitud: Double?,
    // Quién pidió y a qué número llamarle, tal como estaban al confirmar el
    // pedido (aunque después el cliente cambie su perfil).
    val nombreCliente: String = "",
    val telefonoCliente: String = "",
    // El uid de quien hizo el pedido. El panel de control lo usa para buscar
    // la foto del cliente en usuarios/{uid}.
    val uid: String = ""
)

// Los pedidos viven en la colección general "pedidos" (no dentro de cada
// usuario) para que el panel de control pueda ver los de todos.
object PedidoRepository {

    private const val ETIQUETA = "PedidoRepository"

    // Los 5 valores que puede tener el campo "estado" de un pedido, en orden:
    //  pendiente  -> el cliente ya pagó y el pedido llegó, pero TÚ todavía no
    //                lo has confirmado.
    //  recibido   -> tú confirmaste que lo recibiste (el "OK").
    //  preparando -> ya se está cocinando.
    //  camino     -> ya salió el repartidor.
    //  entregado  -> el cliente ya lo tiene.
    const val ESTADO_PENDIENTE = "pendiente"
    const val ESTADO_RECIBIDO = "recibido"
    const val ESTADO_PREPARANDO = "preparando"
    const val ESTADO_CAMINO = "camino"
    const val ESTADO_ENTREGADO = "entregado"

    // El estado que sigue después de cada uno (null = ya terminó, no hay más).
    // Es la única regla de avance: nunca se salta un paso ni se regresa.
    fun estadoSiguiente(estado: String): String? = when (estado) {
        ESTADO_PENDIENTE -> ESTADO_RECIBIDO
        ESTADO_RECIBIDO -> ESTADO_PREPARANDO
        ESTADO_PREPARANDO -> ESTADO_CAMINO
        ESTADO_CAMINO -> ESTADO_ENTREGADO
        else -> null
    }

    // #001, #002 ... #999, #1000 ...
    fun formatoNumero(numero: Long): String = String.format(Locale.US, "#%03d", numero)

    // Guarda el pedido con el siguiente número de la cuenta (contadores/pedidos,
    // campo "ultimo"). Se hace en una TRANSACCIÓN: leer el contador, subirlo en
    // uno y guardar el pedido ocurren juntos o no ocurre nada; así dos pedidos
    // al mismo tiempo nunca reciben el mismo número.
    // onResultado: (numeroDePedido, null) si salió bien, (null, motivo) si falló.
    fun crearPedido(
        items: List<ItemCarrito>,
        resumen: ResumenPedido,
        codigo: CodigoPromo?,
        metodo: MetodoPago,
        detallePago: String,
        direccion: DireccionEntrega,
        nombreCliente: String,
        telefonoCliente: String,
        onResultado: (Long?, String?) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            onResultado(null, "No hay una sesión iniciada")
            return
        }

        val db = FirebaseFirestore.getInstance()
        val refContador = db.collection("contadores").document("pedidos")
        val refPedido = db.collection("pedidos").document()

        // Todo se prepara AQUÍ, antes de la transacción, para que lo que se
        // guarde sea exactamente lo que había al tocar "Confirmar pago".
        val itemsParaGuardar = items.map { item ->
            hashMapOf<String, Any>(
                "nombreProducto" to item.nombreProducto,
                "nombreTamano" to item.nombreTamano,
                "precioTamano" to item.precioTamano,
                "extras" to item.extrasSeleccionados.map { extra ->
                    hashMapOf<String, Any>("nombre" to extra.nombre, "precio" to extra.precio)
                },
                "cantidad" to item.cantidad,
                "subtotal" to item.subtotalLinea()
            )
        }

        val direccionParaGuardar = hashMapOf<String, Any>(
            "zona" to direccion.zona,
            "direccionEspecifica" to direccion.direccionEspecifica
        )
        direccion.latitud?.let { direccionParaGuardar["latitud"] = it }
        direccion.longitud?.let { direccionParaGuardar["longitud"] = it }

        val datosBase = hashMapOf<String, Any>(
            "uid" to uid,
            "nombreCliente" to nombreCliente,
            "telefonoCliente" to telefonoCliente,
            // Todo pedido nuevo nace "pendiente": espera tu OK en el panel.
            "estado" to ESTADO_PENDIENTE,
            // La hora la pone el SERVIDOR de Firebase (no el reloj del celular).
            "fechaCreacion" to FieldValue.serverTimestamp(),
            "horas" to hashMapOf<String, Any>("pendiente" to FieldValue.serverTimestamp()),
            "items" to itemsParaGuardar,
            "subtotal" to resumen.subtotal,
            "descuento" to resumen.descuento,
            "entrega" to resumen.entrega,
            "impuestos" to resumen.impuestos,
            "total" to resumen.total,
            "metodoPago" to metodo.nombre,
            "detallePago" to detallePago,
            // Mientras no exista el cobro real, todos los pagos son de prueba.
            "pagoDePrueba" to true,
            "direccion" to direccionParaGuardar
        )
        codigo?.let { datosBase["codigoPromo"] = it.codigo }

        db.runTransaction { transaccion ->
            val snapshotContador = transaccion.get(refContador)
            val numero = (snapshotContador.getLong("ultimo") ?: 0L) + 1L

            if (snapshotContador.exists()) {
                transaccion.update(refContador, "ultimo", numero)
            } else {
                transaccion.set(refContador, hashMapOf<String, Any>("ultimo" to numero))
            }

            val datos = HashMap<String, Any>(datosBase)
            datos["numero"] = numero
            transaccion.set(refPedido, datos)

            numero
        }.addOnSuccessListener { numero ->
            onResultado(numero, null)
        }.addOnFailureListener { error ->
            Log.e(ETIQUETA, "No se pudo guardar el pedido", error)
            onResultado(null, error.message ?: "Error desconocido")
        }
    }

    // Escucha en tiempo real SOLO los pedidos del usuario con la sesión
    // iniciada. Se ordenan aquí (el más reciente primero, por número) en vez
    // de pedírselo a Firestore, porque así no hace falta crear un índice.
    // Hay que guardar el ListenerRegistration y quitarlo en onPause.
    fun escucharMisPedidos(
        onCambio: (List<Pedido>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null

        return FirebaseFirestore.getInstance()
            .collection("pedidos")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(ETIQUETA, "No se pudieron cargar los pedidos", error)
                    onError(error.message ?: "Error desconocido")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val pedidos = snapshot.documents
                    .mapNotNull { convertirPedido(it) }
                    .sortedByDescending { it.numero }
                onCambio(pedidos)
            }
    }

    // ---------------------------------------------------------------------
    // Solo para el panel de control (administrador)
    // ---------------------------------------------------------------------

    // Escucha en tiempo real los últimos 100 pedidos de TODOS los clientes,
    // el más reciente primero. Solo funciona con la cuenta de administrador:
    // las reglas de Firestore rechazan a cualquier otra. Ordenar por un solo
    // campo no necesita crear índices. Hay que quitar el listener en onPause.
    fun escucharTodosLosPedidos(
        onCambio: (List<Pedido>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return FirebaseFirestore.getInstance()
            .collection("pedidos")
            .orderBy("numero", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(ETIQUETA, "No se pudieron cargar los pedidos del panel", error)
                    onError(error.message ?: "Error desconocido")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                onCambio(snapshot.documents.mapNotNull { convertirPedido(it) })
            }
    }

    // Pasa un pedido al estado que sigue y guarda la hora de ese paso (con la
    // hora del servidor). Las reglas de Firestore solo dejan hacerlo al
    // administrador y solo hacia el estado siguiente.
    // onResultado: null si salió bien, o el motivo si falló.
    fun cambiarEstado(idPedido: String, estadoNuevo: String, onResultado: (String?) -> Unit) {
        val cambios = hashMapOf<String, Any>(
            "estado" to estadoNuevo,
            "horas.$estadoNuevo" to FieldValue.serverTimestamp()
        )
        FirebaseFirestore.getInstance()
            .collection("pedidos")
            .document(idPedido)
            .update(cambios)
            .addOnSuccessListener { onResultado(null) }
            .addOnFailureListener { error ->
                Log.e(ETIQUETA, "No se pudo cambiar el estado del pedido", error)
                onResultado(error.message ?: "Error desconocido")
            }
    }

    private fun convertirPedido(doc: DocumentSnapshot): Pedido? {
        val numero = doc.getLong("numero") ?: return null

        // ESTIMATE: si la hora del servidor todavía no llega (justo después de
        // confirmar), usa la hora aproximada del celular en vez de dejarla vacía.
        val estimar = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE

        val itemsRaw = doc.get("items") as? List<*> ?: emptyList<Any>()
        val items = itemsRaw.mapNotNull { itemRaw ->
            val mapa = itemRaw as? Map<*, *> ?: return@mapNotNull null
            val extrasRaw = mapa["extras"] as? List<*> ?: emptyList<Any>()
            ItemPedido(
                nombreProducto = mapa["nombreProducto"] as? String ?: "",
                nombreTamano = mapa["nombreTamano"] as? String ?: "",
                cantidad = (mapa["cantidad"] as? Number)?.toInt() ?: 1,
                subtotal = (mapa["subtotal"] as? Number)?.toDouble() ?: 0.0,
                extras = extrasRaw.mapNotNull { extraRaw ->
                    (extraRaw as? Map<*, *>)?.get("nombre") as? String
                }
            )
        }

        return Pedido(
            id = doc.id,
            numero = numero,
            estado = doc.getString("estado") ?: ESTADO_PENDIENTE,
            fechaCreacion = doc.getTimestamp("fechaCreacion", estimar)?.toDate(),
            horaRecibido = doc.getTimestamp("horas.recibido", estimar)?.toDate(),
            horaPreparando = doc.getTimestamp("horas.preparando", estimar)?.toDate(),
            horaCamino = doc.getTimestamp("horas.camino", estimar)?.toDate(),
            horaEntregado = doc.getTimestamp("horas.entregado", estimar)?.toDate(),
            items = items,
            subtotal = doc.getDouble("subtotal") ?: 0.0,
            descuento = doc.getDouble("descuento") ?: 0.0,
            entrega = doc.getDouble("entrega") ?: 0.0,
            impuestos = doc.getDouble("impuestos") ?: 0.0,
            total = doc.getDouble("total") ?: 0.0,
            codigoPromo = doc.getString("codigoPromo"),
            metodoPago = doc.getString("metodoPago") ?: "",
            detallePago = doc.getString("detallePago") ?: "",
            pagoDePrueba = doc.getBoolean("pagoDePrueba") ?: false,
            zona = doc.getString("direccion.zona") ?: "",
            direccionEspecifica = doc.getString("direccion.direccionEspecifica") ?: "",
            latitud = doc.getDouble("direccion.latitud"),
            longitud = doc.getDouble("direccion.longitud"),
            nombreCliente = doc.getString("nombreCliente") ?: "",
            telefonoCliente = doc.getString("telefonoCliente") ?: "",
            uid = doc.getString("uid") ?: ""
        )
    }
}