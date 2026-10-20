package cl.duoc.redtextil.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FechaUtil {

    private val localeChile: Locale = Locale.forLanguageTag("es-CL")

    fun fechaHora(millis: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", localeChile).format(Date(millis))

    fun fecha(millis: Long): String =
        SimpleDateFormat("dd/MM/yyyy", localeChile).format(Date(millis))

    fun haceCuanto(millis: Long, ahora: Long = System.currentTimeMillis()): String {
        val minutos = (ahora - millis) / 60_000
        return when {
            minutos < 1 -> "recién"
            minutos < 60 -> "hace $minutos min"
            minutos < 24 * 60 -> "hace ${minutos / 60} h"
            minutos < 7 * 24 * 60 -> "hace ${minutos / (24 * 60)} días"
            else -> fecha(millis)
        }
    }
}
