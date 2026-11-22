package cl.duoc.redtextil.data.repository

import cl.duoc.redtextil.data.remote.MindicadorApi
import cl.duoc.redtextil.data.remote.dto.IndicadoresDto
import java.util.Locale

data class IndicadoresEconomicos(
    val dolar: Double,
    val euro: Double?,
    val uf: Double?,
    val fecha: String
) {
    // Precio referencial del catalogo en dolares (los precios en 0 son "a convenir")
    fun enDolares(pesos: Int): String? =
        if (pesos <= 0) null else "≈ US$ " + String.format(CHILE, "%,.2f", pesos / dolar)

    companion object {
        private val CHILE: Locale = Locale.forLanguageTag("es-CL")

        fun formatearPesos(valor: Double): String = "$" + String.format(CHILE, "%,.2f", valor)
    }
}

class IndicadoresRepository(private val api: () -> MindicadorApi) {

    // Solo en memoria: estos datos no se guardan en Room ni se envian al microservicio propio
    private var enMemoria: IndicadoresEconomicos? = null

    suspend fun obtener(forzar: Boolean = false): Result<IndicadoresEconomicos> = runCatching {
        enMemoria?.takeUnless { forzar } ?: api().indicadoresDelDia().aIndicadores().also { enMemoria = it }
    }
}

internal fun IndicadoresDto.aIndicadores(): IndicadoresEconomicos {
    val valorDolar = dolar?.valor
    require(valorDolar != null && valorDolar > 0) { "mindicador.cl no entregó el valor del dólar" }
    return IndicadoresEconomicos(
        dolar = valorDolar,
        euro = euro?.valor,
        uf = uf?.valor,
        fecha = formatearFecha(dolar.fecha ?: fecha)
    )
}

// "2026-11-20T03:00:00.000Z" -> "20/11/2026"
internal fun formatearFecha(iso: String?): String =
    iso?.take(10)?.split("-")?.takeIf { it.size == 3 }?.reversed()?.joinToString("/").orEmpty()
