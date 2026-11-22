package cl.duoc.redtextil.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class ResultadoCompresion(
    val ruta: String,
    val kbOriginal: Long,
    val kbOptimizado: Long,
    val ancho: Int,
    val alto: Int
) {
    val porcentajeAhorro: Int
        get() = if (kbOriginal <= 0) 0 else (100 - (kbOptimizado * 100 / kbOriginal)).toInt().coerceIn(0, 100)
}

class ImageCompressor(private val context: Context) {

    private val autoridad = "${context.packageName}.fileprovider"

    fun crearUriCamara(): Uri {
        val carpeta = File(context.cacheDir, "camara").apply { mkdirs() }
        val archivo = File(carpeta, "captura_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, autoridad, archivo)
    }

    suspend fun comprimir(uri: Uri): ResultadoCompresion = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val bytesOriginales = tamanoEnBytes(uri)

        // Primero leemos solo el tamano de la foto para no cargarla completa en memoria (celulares de gama baja)
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
        if (limites.outWidth <= 0 || limites.outHeight <= 0) {
            throw IllegalArgumentException("No pudimos leer la imagen seleccionada")
        }

        val opciones = BitmapFactory.Options().apply {
            inSampleSize = calcularMuestreo(limites.outWidth, limites.outHeight, LADO_MAXIMO)
        }
        val leida = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
            ?: throw IllegalArgumentException("No pudimos abrir la imagen seleccionada")

        val enderezada = corregirOrientacion(uri, leida)
        val escalada = escalar(enderezada, LADO_MAXIMO)

        val carpeta = File(context.filesDir, "catalogo").apply { mkdirs() }
        val destino = File(carpeta, "IMG_${System.currentTimeMillis()}.jpg")
        // Comprimimos la foto antes de guardar para no gastar datos moviles en zonas rurales
        FileOutputStream(destino).use { salida ->
            escalada.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida)
        }

        val resultado = ResultadoCompresion(
            ruta = destino.absolutePath,
            kbOriginal = (bytesOriginales / 1024).coerceAtLeast(1),
            kbOptimizado = (destino.length() / 1024).coerceAtLeast(1),
            ancho = escalada.width,
            alto = escalada.height
        )
        if (escalada !== leida) escalada.recycle()
        if (enderezada !== leida && enderezada !== escalada) enderezada.recycle()
        leida.recycle()
        resultado
    }

    fun eliminarArchivo(ruta: String?) {
        if (ruta.isNullOrBlank()) return
        runCatching { File(ruta).delete() }
    }

    // La foto grande de la camara ya no sirve despues de comprimirla, asi que la borramos
    fun eliminarCaptura(uri: Uri) {
        if (uri.authority != autoridad) return
        runCatching { context.contentResolver.delete(uri, null, null) }
    }

    fun limpiarCapturasTemporales() {
        runCatching { File(context.cacheDir, "camara").listFiles()?.forEach { it.delete() } }
    }

    private fun tamanoEnBytes(uri: Uri): Long {
        val porConsulta = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
            }
        }.getOrNull()
        if (porConsulta != null && porConsulta > 0) return porConsulta

        var total = 0L
        context.contentResolver.openInputStream(uri)?.use { entrada ->
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val leidos = entrada.read(buffer)
                if (leidos < 0) break
                total += leidos
            }
        }
        return total
    }

    private fun calcularMuestreo(ancho: Int, alto: Int, ladoMaximo: Int): Int {
        var muestreo = 1
        while (ancho / (muestreo * 2) >= ladoMaximo && alto / (muestreo * 2) >= ladoMaximo) {
            muestreo *= 2
        }
        return muestreo
    }

    private fun escalar(bitmap: Bitmap, ladoMaximo: Int): Bitmap {
        val ladoMayor = maxOf(bitmap.width, bitmap.height)
        if (ladoMayor <= ladoMaximo) return bitmap
        val factor = ladoMaximo.toFloat() / ladoMayor
        val nuevoAncho = (bitmap.width * factor).toInt().coerceAtLeast(1)
        val nuevoAlto = (bitmap.height * factor).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, nuevoAncho, nuevoAlto, true)
    }

    // Muchas camaras guardan la foto "acostada" y solo marcan la rotacion en el EXIF
    private fun corregirOrientacion(uri: Uri, bitmap: Bitmap): Bitmap {
        val orientacion = runCatching {
            context.contentResolver.openInputStream(uri)?.use { entrada ->
                ExifInterface(entrada).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        val grados = when (orientacion) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (grados == 0f) return bitmap
        val matriz = Matrix().apply { postRotate(grados) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
    }

    companion object {
        const val LADO_MAXIMO = 1080
        const val CALIDAD_JPEG = 75
    }
}
