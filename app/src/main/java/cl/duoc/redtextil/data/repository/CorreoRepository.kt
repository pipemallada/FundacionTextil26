package cl.duoc.redtextil.data.repository

import cl.duoc.redtextil.data.local.dao.CorreoDao
import cl.duoc.redtextil.data.local.entity.CorreoEntity
import cl.duoc.redtextil.data.local.entity.EmprendimientoEntity
import cl.duoc.redtextil.data.remote.RedTextilApiService
import cl.duoc.redtextil.data.remote.aDto
import cl.duoc.redtextil.domain.model.EstadoCorreo
import cl.duoc.redtextil.domain.model.PlantillaCorreo
import cl.duoc.redtextil.domain.model.TipoCorreo
import cl.duoc.redtextil.util.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CorreoRepository(
    private val dao: CorreoDao,
    private val api: () -> RedTextilApiService,
    private val red: NetworkMonitor
) {

    fun observar(estado: EstadoCorreo?): Flow<List<CorreoEntity>> = dao.observar(estado?.name.orEmpty())

    fun contarPorEstado(): Flow<Map<EstadoCorreo, Int>> = dao.contarPorEstado().map { conteos ->
        conteos.mapNotNull { conteo ->
            EstadoCorreo.entries.firstOrNull { it.name == conteo.clave }?.let { it to conteo.total }
        }.toMap()
    }

    fun contarEnCola(): Flow<Int> = dao.contarEnCola()

    // Cada alta o aprobacion genera su correo automatico, sin trabajo manual de la administradora
    suspend fun programar(emprendimiento: EmprendimientoEntity, tipo: TipoCorreo, plantilla: PlantillaCorreo): CorreoEntity {
        val nuevo = CorreoEntity(
            emprendimientoId = emprendimiento.id,
            destinatario = emprendimiento.correo,
            nombreDestinatario = emprendimiento.nombreEmprendedora,
            asunto = plantilla.asunto,
            cuerpo = plantilla.cuerpo,
            tipo = tipo,
            estado = EstadoCorreo.EN_COLA_OFFLINE
        )
        val id = dao.insertar(nuevo)
        return enviar(nuevo.copy(id = id))
    }

    suspend fun enviar(correo: CorreoEntity): CorreoEntity {
        if (correo.destinatario.isBlank()) {
            val sinCorreo = correo.copy(
                estado = EstadoCorreo.FALLIDO,
                intentos = correo.intentos + 1,
                detalleError = "La ficha no tiene correo registrado"
            )
            dao.actualizar(sinCorreo)
            return sinCorreo
        }
        // Sin senal el correo queda en cola y se reintenta en la proxima sincronizacion
        if (!red.enLinea.value) {
            val enCola = correo.copy(estado = EstadoCorreo.EN_COLA_OFFLINE)
            dao.actualizar(enCola)
            return enCola
        }
        val resultado = runCatching { api().enviarCorreo(correo.aDto()) }
        val actualizado = resultado.fold(
            onSuccess = { respuesta ->
                if (respuesta.ok) {
                    correo.copy(
                        estado = EstadoCorreo.ENVIADO_OK,
                        intentos = correo.intentos + 1,
                        detalleError = "",
                        fechaEnvio = System.currentTimeMillis()
                    )
                } else {
                    correo.copy(
                        estado = EstadoCorreo.FALLIDO,
                        intentos = correo.intentos + 1,
                        detalleError = respuesta.mensaje ?: "El servidor rechazó el envío"
                    )
                }
            },
            onFailure = {
                correo.copy(
                    estado = EstadoCorreo.FALLIDO,
                    intentos = correo.intentos + 1,
                    detalleError = "No se pudo conectar con el servidor de correo"
                )
            }
        )
        dao.actualizar(actualizado)
        return actualizado
    }

    suspend fun reenviar(id: Long): CorreoEntity? = dao.obtenerPorId(id)?.let { enviar(it) }

    suspend fun reenviarPendientes(): Int {
        var enviados = 0
        for (correo in dao.noEnviados()) {
            if (!red.enLinea.value) break
            if (enviar(correo).estado == EstadoCorreo.ENVIADO_OK) enviados++
        }
        return enviados
    }
}
