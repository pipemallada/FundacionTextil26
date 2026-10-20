package cl.duoc.redtextil.domain.validation

import java.util.Calendar

// Usamos una fecha propia porque java.time recien existe desde Android 8 y la app parte en Android 7
data class FechaSimple(val dia: Int, val mes: Int, val anio: Int) {

    fun esValida(): Boolean = mes in 1..12 && dia in 1..diasDelMes(mes, anio) && anio in 1900..2100

    fun edadAl(hoy: FechaSimple): Int {
        var edad = hoy.anio - anio
        if (hoy.mes < mes || (hoy.mes == mes && hoy.dia < dia)) edad--
        return edad
    }

    companion object {
        private val regexFecha = Regex("^(\\d{1,2})/(\\d{1,2})/(\\d{4})$")

        fun hoy(): FechaSimple {
            val calendario = Calendar.getInstance()
            return FechaSimple(
                calendario.get(Calendar.DAY_OF_MONTH),
                calendario.get(Calendar.MONTH) + 1,
                calendario.get(Calendar.YEAR)
            )
        }

        fun desdeTexto(texto: String): FechaSimple? {
            val coincidencia = regexFecha.find(texto.trim()) ?: return null
            val (dia, mes, anio) = coincidencia.destructured
            val fecha = FechaSimple(dia.toInt(), mes.toInt(), anio.toInt())
            return if (fecha.esValida()) fecha else null
        }

        fun esBisiesto(anio: Int): Boolean = (anio % 4 == 0 && anio % 100 != 0) || anio % 400 == 0

        fun diasDelMes(mes: Int, anio: Int): Int = when (mes) {
            2 -> if (esBisiesto(anio)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }
}
