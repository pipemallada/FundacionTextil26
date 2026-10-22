package cl.duoc.redtextil.data.repository

import android.content.Context
import cl.duoc.redtextil.data.local.database.AppDatabase
import cl.duoc.redtextil.data.local.entity.EmprendimientoEntity
import cl.duoc.redtextil.data.remote.RedTextilApiService
import cl.duoc.redtextil.data.remote.aFilaCsv
import cl.duoc.redtextil.domain.model.EstadoFicha
import cl.duoc.redtextil.domain.model.EstadoSync
import cl.duoc.redtextil.domain.model.OrigenFicha
import cl.duoc.redtextil.domain.validation.DepuradorHistorico
import cl.duoc.redtextil.domain.validation.FilaHistoricaDepurada
import cl.duoc.redtextil.util.CsvLector
import cl.duoc.redtextil.util.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

data class ResultadoImportacion(
    val total: Int,
    val integradas: Int,
    val yaExistian: Int,
    val activas: Int,
    val observadas: Int,
    val correcciones: Int,
    val fuente: String
)

class HistoricoRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val api: () -> RedTextilApiService,
    private val red: NetworkMonitor
) {

    fun totalIntegrados(): Flow<Int> = db.emprendimientoDao().observarTotalHistoricos()

    // Integra las fichas de la planilla antigua sin pedirle a las emprendedoras que se registren de nuevo
    suspend fun integrarDataset(onProgreso: (procesadas: Int, total: Int) -> Unit): ResultadoImportacion =
        withContext(Dispatchers.IO) {
            val desdeServidor = if (red.enLinea.value) {
                runCatching { api().descargarHistorico() }.getOrNull().orEmpty()
            } else {
                emptyList()
            }
            val filas: List<Map<String, String>>
            val fuente: String
            if (desdeServidor.isNotEmpty()) {
                filas = desdeServidor.map { it.aFilaCsv() }
                fuente = "API de la Fundación"
            } else {
                filas = context.assets.open(ARCHIVO_DATASET).use { CsvLector.leer(it) }
                fuente = "Respaldo local depurado ($ARCHIVO_DATASET)"
            }

            val total = filas.size
            var integradas = 0
            var yaExistian = 0
            var activas = 0
            var observadas = 0
            var correcciones = 0
            var procesadas = 0
            onProgreso(0, total)

            // Se inserta por lotes para no bloquear la base de datos en celulares de gama baja
            filas.chunked(TAMANO_LOTE).forEach { lote ->
                val depuradas = lote.map { DepuradorHistorico.depurar(it) }
                val ids = db.emprendimientoDao().insertarLote(depuradas.map { it.aEntidad() })
                ids.forEachIndexed { i, id ->
                    if (id == -1L) {
                        yaExistian++
                    } else {
                        integradas++
                        correcciones += depuradas[i].correcciones
                        if (depuradas[i].quedaActiva) activas++ else observadas++
                    }
                }
                procesadas += lote.size
                onProgreso(procesadas, total)
            }

            ResultadoImportacion(total, integradas, yaExistian, activas, observadas, correcciones, fuente)
        }

    private fun FilaHistoricaDepurada.aEntidad(): EmprendimientoEntity {
        val fecha = leerFecha(marcaTemporal)
        val notas = (problemasCriticos + avisos).joinToString(". ")
        return EmprendimientoEntity(
            nombreEmprendedora = "",
            nombreComercial = "",
            region = "",
            comuna = ""
        ).conDatos(datos).copy(
            nombreEmprendedora = datos.nombreEmprendedora.ifBlank { "Sin nombre registrado" },
            nombreComercial = datos.nombreComercial.ifBlank { "Emprendimiento sin nombre" },
            estado = if (quedaActiva) EstadoFicha.ACTIVA else EstadoFicha.OBSERVADA,
            observacion = notas,
            origen = OrigenFicha.HISTORICO,
            idHistorico = idHistorico,
            aceptaPrivacidad = true,
            estadoSync = EstadoSync.SINCRONIZADO,
            fechaRegistro = fecha,
            fechaActualizacion = fecha
        )
    }

    private fun leerFecha(texto: String): Long =
        runCatching { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US).parse(texto)?.time }.getOrNull()
            ?: System.currentTimeMillis()

    companion object {
        const val ARCHIVO_DATASET = "dataset_historico_982.csv"
        const val TOTAL_ESPERADO = 982
        private const val TAMANO_LOTE = 50
    }
}
