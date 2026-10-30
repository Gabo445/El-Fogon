package com.example.elfogonlogin.model

import com.example.elfogonlogin.controller.CodigoPromo

// Los números del pedido (subtotal, descuento, entrega, impuestos, total)
// se calculan SOLO aquí, para que el Carrito y la pantalla de Pago nunca
// muestren totales distintos. Si algún día cambia el costo de entrega o el
// IVA, se cambia en este único lugar.
data class ResumenPedido(
    val subtotal: Double,
    val descuento: Double,
    val entrega: Double,
    val impuestos: Double,
    val total: Double
) {
    companion object {
        const val COSTO_ENTREGA = 2.00
        const val TASA_IMPUESTO = 0.13 // 13% IVA

        // El descuento se resta al subtotal ANTES de calcular los impuestos.
        fun calcular(subtotal: Double, codigo: CodigoPromo?): ResumenPedido {
            val descuento = calcularDescuento(subtotal, codigo)
            val subtotalConDescuento = subtotal - descuento
            val entrega = if (subtotal > 0) COSTO_ENTREGA else 0.0
            val impuestos = subtotalConDescuento * TASA_IMPUESTO
            val total = subtotalConDescuento + entrega + impuestos
            return ResumenPedido(subtotal, descuento, entrega, impuestos, total)
        }

        private fun calcularDescuento(subtotal: Double, codigo: CodigoPromo?): Double {
            if (codigo == null) return 0.0
            val descuento = if (codigo.tipoDescuento == "porcentaje") {
                subtotal * (codigo.valor / 100.0)
            } else {
                codigo.valor
            }
            return descuento.coerceAtMost(subtotal)
        }
    }
}