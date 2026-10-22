package cl.duoc.redtextil.data.repository

import cl.duoc.redtextil.util.NetworkMonitor
import cl.duoc.redtextil.util.NotificationHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ResultadoSincronizacion(
    val enviados: Int,
    val exito: Boolean,
    val mensaje: String
)

class SincronizacionRepository(
    private val emprendimientos: EmprendimientoRepository,
    private val catalogo: CatalogoRepository,
    private val consultas: ConsultaRepository,
    private val metricas: MetricasRepository,
    private val correos: CorreoRepository,
    private val red: NetworkMonitor,
    private val notificaciones: NotificationHelper
) {

    private val candado = Mutex()

    val pendientesTotales: Flow<Int> = combine(
        emprendimientos.contarPendientes(),
        catalogo.contarPendientes(),
        consultas.contarPendientesSync(),
        metricas.contarPendientes(),
        correos.contarEnCola()
    ) { fichas, publicaciones, mensajes, interacciones, enCola ->
        fichas + publicaciones + mensajes + interacciones + enCola
    }

    suspend fun sincronizarTodo(): ResultadoSincronizacion = candado.withLock {
        if (!red.enLinea.value) {
            return@withLock ResultadoSincronizacion(
                0,
                false,
                "Sin conexión: tus cambios siguen guardados en el teléfono"
            )
        }
        var enviados = 0
        val error = runCatching {
            enviados += emprendimientos.sincronizarPendientes()
            enviados += catalogo.sincronizarPendientes()
            enviados += consultas.sincronizarPendientes()
            enviados += metricas.sincronizarPendientes()
            enviados += correos.reenviarPendientes()
        }.exceptionOrNull()

        if (error == null) {
            if (enviados > 0) notificaciones.notificarSincronizacion(enviados)
            ResultadoSincronizacion(
                enviados,
                true,
                if (enviados > 0) "Sincronización completa: $enviados cambios enviados" else "Todo está al día"
            )
        } else {
            ResultadoSincronizacion(
                enviados,
                false,
                "No se pudo conectar con el servidor. Tus datos siguen guardados en el teléfono."
            )
        }
    }
}
