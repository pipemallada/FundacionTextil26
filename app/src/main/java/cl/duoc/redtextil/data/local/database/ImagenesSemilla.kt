package cl.duoc.redtextil.data.local.database

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import cl.duoc.redtextil.domain.model.CategoriaTrabajo
import java.io.File
import java.io.FileOutputStream

// Genera fotos de muestra para el catalogo demo, asi no se incluyen fotos de personas reales en el proyecto
object ImagenesSemilla {

    private val paletas = listOf(
        intArrayOf(0xFFA94E2D.toInt(), 0xFFF6D9CC.toInt(), 0xFF2F5D3A.toInt()),
        intArrayOf(0xFF5E7D63.toInt(), 0xFFDCE8D9.toInt(), 0xFFA94E2D.toInt()),
        intArrayOf(0xFF2F5D3A.toInt(), 0xFFC9E4CC.toInt(), 0xFFE8B04B.toInt()),
        intArrayOf(0xFF8C5A3C.toInt(), 0xFFFBF6EE.toInt(), 0xFF5E7D63.toInt()),
        intArrayOf(0xFF3E4F7A.toInt(), 0xFFE6E9F2.toInt(), 0xFFA94E2D.toInt())
    )

    fun generar(context: Context, categoria: CategoriaTrabajo, indice: Int): String {
        val lado = 720
        val bitmap = Bitmap.createBitmap(lado, lado, Bitmap.Config.RGB_565)
        val lienzo = Canvas(bitmap)
        val colores = paletas[indice % paletas.size]
        val pincel = Paint(Paint.ANTI_ALIAS_FLAG)

        lienzo.drawColor(colores[1])
        pincel.color = colores[0]
        val franja = lado / 12f
        for (i in 0 until 14) {
            if (i % 2 == 0) lienzo.drawRect(0f, i * franja, lado.toFloat(), i * franja + franja / 2, pincel)
        }

        pincel.color = colores[2]
        when (categoria) {
            CategoriaTrabajo.ACCESORIO, CategoriaTrabajo.TEJIDO -> {
                for (fila in 0 until 6) for (col in 0 until 6) {
                    lienzo.drawCircle(60f + col * 120f, 60f + fila * 120f, 34f, pincel)
                }
            }
            CategoriaTrabajo.TALLER -> {
                lienzo.drawRoundRect(RectF(120f, 160f, 600f, 560f), 60f, 60f, pincel)
            }
            else -> {
                val camisa = android.graphics.Path().apply {
                    moveTo(250f, 140f)
                    lineTo(470f, 140f)
                    lineTo(610f, 260f)
                    lineTo(540f, 330f)
                    lineTo(490f, 290f)
                    lineTo(490f, 600f)
                    lineTo(230f, 600f)
                    lineTo(230f, 290f)
                    lineTo(180f, 330f)
                    lineTo(110f, 260f)
                    close()
                }
                lienzo.drawPath(camisa, pincel)
            }
        }

        val carpeta = File(context.filesDir, "catalogo").apply { mkdirs() }
        val archivo = File(carpeta, "demo_${categoria.name.lowercase()}_$indice.jpg")
        FileOutputStream(archivo).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 75, it) }
        bitmap.recycle()
        return archivo.absolutePath
    }
}
