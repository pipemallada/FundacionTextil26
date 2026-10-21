package cl.duoc.redtextil.data.repository

import cl.duoc.redtextil.data.local.database.AppDatabase
import cl.duoc.redtextil.data.local.entity.ConsultaConNegocio
import cl.duoc.redtextil.data.local.entity.ConsultaEntity
import cl.duoc.redtextil.data.local.entity.EmprendimientoEntity
import cl.duoc.redtextil.data.remote.RedTextilApiService
import cl.duoc.redtextil.data.remote.aDto
import cl.duoc.redtextil.domain.model.ConsultaForm
import cl.duoc.redtextil.domain.model.EstadoConsulta
import cl.duoc.redtextil.domain.model.EstadoSync
import cl.duoc.redtextil.domain.model.TipoInteraccion
import cl.duoc.redtextil.domain.validation.ReglasComunes
import cl.duoc.redtextil.util.NetworkMonitor
import cl.duoc.redtextil.util.NotificationHelper
import cl.duoc.redtextil.util.TextoUtil
import kotlinx.coroutines.flow.Flow

class ConsultaRepository(
    private val db: AppDatabase,
    private val api: () -> RedTextilApiService,
    private val red: NetworkMonitor,
    private val notificaciones: NotificationHelper,
    private val metricas: MetricasRepository
) {

    private val dao = db.consultaDao()

    fun observarBandeja(emprendimientoId: Long): Flow<List<ConsultaEntity>> = dao.observarDeEmprendimiento(emprendimientoId)

    fun contarPendientesDe(emprendimientoId: Long): Flow<Int> = dao.contarPendientesDe(emprendimientoId)

    fun observarEnviadasDesdeApp(): Flow<List<ConsultaConNegocio>> = dao.observarEnviadasDesdeApp()

    fun contarPendientesSync(): Flow<Int> = dao.contarPendientes()

    suspend fun enviar(emprendimiento: EmprendimientoEntity, form: ConsultaForm): Long {
        val ahora = System.currentTimeMillis()
        val consulta = ConsultaEntity(
            emprendimientoId = emprendimiento.id,
            nombreCliente = TextoUtil.limpiarEspacios(form.nombre),
            correoCliente = form.correo.trim().lowercase(),
            telefonoCliente = ReglasComunes.normalizarTelefono(form.telefono).orEmpty(),
            servicio = form.servicio,
            mensaje = form.mensaje.trim(),
            fechaEnvio = ahora,
            fechaActualizacion = ahora
        )
        val id = dao.insertar(consulta)
        metricas.registrarContacto(TipoInteraccion.CONSULTA, emprendimiento, form.servicio)
        // Aviso inmediato a la emprendedora: en la demo ambos perfiles conviven en el mismo telefono
        notificaciones.notificarNuevaConsulta(emprendimiento.nombreComercial, consulta.nombreCliente, consulta.servicio)
        if (red.enLinea.value) runCatching { sincronizarUna(consulta.copy(id = id)) }
        return id
    }

    suspend fun responder(id: Long, respuesta: String) {
        val consulta = dao.obtenerPorId(id) ?: return
        val ahora = System.currentTimeMillis()
        val respondida = consulta.copy(
            respuesta = respuesta.trim(),
            estado = EstadoConsulta.RESPONDIDA,
            fechaRespuesta = ahora,
            estadoSync = EstadoSync.PENDIENTE_SINCRONIZAR,
            fechaActualizacion = ahora
        )
        dao.actualizar(respondida)
        if (red.enLinea.value) runCatching { sincronizarUna(respondida) }
    }

    suspend fun cambiarEstado(id: Long, estado: EstadoConsulta) {
        val consulta = dao.obtenerPorId(id) ?: return
        val actualizada = consulta.copy(
            estado = estado,
            estadoSync = EstadoSync.PENDIENTE_SINCRONIZAR,
            fechaActualizacion = System.currentTimeMillis()
        )
        dao.actualizar(actualizada)
        if (red.enLinea.value) runCatching { sincronizarUna(actualizada) }
    }

    suspend fun sincronizarPendientes(): Int {
        var enviados = 0
        for (pendiente in dao.pendientesDeSincronizar()) {
            sincronizarUna(pendiente)
            enviados++
        }
        return enviados
    }

    private suspend fun sincronizarUna(consulta: ConsultaEntity) {
        val fichaRemota = db.emprendimientoDao().obtenerPorId(consulta.emprendimientoId)?.remoteId
        val idRemoto = consulta.remoteId
        val respuesta = if (idRemoto == null) {
            api().crearConsulta(consulta.aDto(fichaRemota))
        } else {
            api().actualizarConsulta(idRemoto, consulta.aDto(fichaRemota))
        }
        dao.marcarSincronizada(consulta.id, respuesta.id ?: idRemoto, consulta.fechaActualizacion)
    }
}
