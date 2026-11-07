package com.example.darhoney.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object PhotoEncoder {
    private const val MAX_DIMENSION = 1024
    private const val JPEG_QUALITY = 75
    private const val MAX_BYTES = 700_000

    suspend fun encodeAsDataUrl(contentResolver: ContentResolver, uri: Uri): String =
        withContext(Dispatchers.IO) {
            val inputStream = contentResolver.openInputStream(uri)
                ?: throw Exception("No se pudo leer la foto. Vuelve a seleccionarla.")

            inputStream.use { stream ->
                val original = BitmapFactory.decodeStream(stream)
                    ?: throw Exception("Formato de imagen no válido.")

                val scaled = scaleDown(original)
                if (scaled !== original) original.recycle()

                val output = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
                scaled.recycle()

                val bytes = output.toByteArray()
                if (bytes.size > MAX_BYTES) {
                    throw Exception("La foto es muy grande. Elige otra imagen o toma una más pequeña.")
                }

                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                "data:image/jpeg;base64,$base64"
            }
        }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= MAX_DIMENSION && height <= MAX_DIMENSION) return bitmap

        val ratio = minOf(MAX_DIMENSION.toFloat() / width, MAX_DIMENSION.toFloat() / height)
        val newWidth = (width * ratio).toInt().coerceAtLeast(1)
        val newHeight = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
