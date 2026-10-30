package com.example.elfogonlogin.controller

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream

// Prepara la foto de perfil para guardarla DENTRO de Firestore (sin Firebase
// Storage, que ya pide tarjeta de crédito): la recorta cuadrada, la reduce a
// 256x256 y la convierte en texto (Base64). Así pesa unos 20 a 40 KB y cabe
// sin problema en el documento del usuario, que acepta hasta 1 MB.
object FotoPerfil {

    private const val ETIQUETA = "FotoPerfil"
    private const val LADO = 256
    private const val CALIDAD_JPEG = 80
    private const val MAX_BYTES = 300_000

    // Devuelve la foto ya reducida y convertida a texto, o null si no se pudo
    // leer. Tarda un poquito con las fotos de cámara (son enormes), así que
    // hay que llamarla en segundo plano, no en el hilo de la pantalla.
    fun reducirYCodificar(contexto: Context, uri: Uri): String? {
        return try {
            // 1) Cuánto mide la foto, sin cargarla completa en memoria.
            val medidas = BitmapFactory.Options()
            medidas.inJustDecodeBounds = true
            contexto.contentResolver.openInputStream(uri)?.use { flujo ->
                BitmapFactory.decodeStream(flujo, null, medidas)
            }
            if (medidas.outWidth <= 0 || medidas.outHeight <= 0) return null

            // 2) Se carga ya reducida (potencias de 2) sin bajar de 256 por lado.
            var reduccion = 1
            while (medidas.outWidth / (reduccion * 2) >= LADO && medidas.outHeight / (reduccion * 2) >= LADO) {
                reduccion *= 2
            }
            val opciones = BitmapFactory.Options()
            opciones.inSampleSize = reduccion
            val original = contexto.contentResolver.openInputStream(uri)?.use { flujo ->
                BitmapFactory.decodeStream(flujo, null, opciones)
            } ?: return null

            // 3) Las fotos de cámara guardan "hacia dónde está arriba" aparte;
            // aquí se enderezan para que no salgan de lado.
            val enderezada = enderezar(original, leerOrientacion(contexto, uri))

            // 4) Recorte cuadrado desde el centro y ajuste a 256x256.
            val lado = minOf(enderezada.width, enderezada.height)
            val cuadrada = Bitmap.createBitmap(
                enderezada,
                (enderezada.width - lado) / 2,
                (enderezada.height - lado) / 2,
                lado,
                lado
            )
            val reducida = Bitmap.createScaledBitmap(cuadrada, LADO, LADO, true)

            // 5) A JPEG y de ahí a texto.
            val salida = ByteArrayOutputStream()
            reducida.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida)
            val bytes = salida.toByteArray()
            if (bytes.size > MAX_BYTES) return null

            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(ETIQUETA, "No se pudo procesar la foto", e)
            null
        } catch (e: OutOfMemoryError) {
            Log.e(ETIQUETA, "La foto es demasiado grande", e)
            null
        }
    }

    // Convierte el texto guardado de vuelta en imagen. null si no hay foto o
    // si el texto está dañado.
    fun decodificar(fotoBase64: String?): Bitmap? {
        if (fotoBase64.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(fotoBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: IllegalArgumentException) {
            Log.w(ETIQUETA, "La foto guardada está dañada", e)
            null
        }
    }

    private fun leerOrientacion(contexto: Context, uri: Uri): Int {
        return try {
            contexto.contentResolver.openInputStream(uri)?.use { flujo ->
                ExifInterface(flujo).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun enderezar(bitmap: Bitmap, orientacion: Int): Bitmap {
        val matriz = Matrix()
        when (orientacion) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matriz.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matriz.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matriz.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matriz.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matriz.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matriz.postRotate(90f)
                matriz.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matriz.postRotate(-90f)
                matriz.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
    }
}