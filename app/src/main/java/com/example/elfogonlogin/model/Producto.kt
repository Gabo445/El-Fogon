package com.example.elfogonlogin.model

data class Producto(
    val id: Int,
    val categoria: String,
    val nombre: String,
    val descripcion: String,
    val precio: Double,
    val imagenResId: Int,
    val detalleId: String // conecta con DetalleProductoRepository para la pantalla de detalle
)