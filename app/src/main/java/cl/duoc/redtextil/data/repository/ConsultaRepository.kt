package cl.duoc.redtextil.data.repository

import cl.duoc.redtextil.data.local.database.AppDatabase
import cl.duoc.redtextil.data.local.entity.ConsultaConNegocio
import cl.duoc.redtextil.data.local.entity.ConsultaEntity
import cl.duoc.redtextil.data.local.entity.EmprendimientoEntity
import cl.duoc.redtextil.data.remote.RedTextilApiService
import cl.duoc.redtextil.data.remote.aDto
import cl.duoc.redtextil.data.remote.aEntidad
import cl.duoc.redtextil.data.remote.actualizarOCrear
import cl.duoc.redtextil.data.remote.conDatosDelServidor
import cl.duoc.redtextil.data.remote.eliminarEnServidor
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

    suspend fun eliminar(id: Long) {
        val consulta = dao.obtenerPorId(id) ?: return
        if (consulta.remoteId == null) {
            dao.eliminarDefinitivo(consulta)
            return
        }
        // Ya estaba en el servidor: se marca y se borra alla apenas haya senal
        val marcada = consulta.copy(
            eliminada = true,
            estadoSync = EstadoSync.PENDIENTE_SINCRONIZAR,
            fechaActualizacion = System.currentTimeMillis()
        )
        dao.actualizar(marcada)
        if (red.enLinea.value) runCatching { sincronizarUna(marcada) }
    }

    suspend fun sincronizarPendientes(): Int = dao.pendientesDeSincronizar().count { sincronizarUna(it) }

    // Recibe las consultas que llegaron al microservicio (por ejemplo, desde otro telefono)
    suspend fun actualizarDesdeServidor(emprendimientoId: Long): Result<Int> = runCatching {
        check(red.enLinea.value) { "Sin conexión" }
        val ficha = db.emprendimientoDao().obtenerPorId(emprendimientoId)
        val fichaRemota = ficha?.remoteId ?: return@runCatching 0
        var cambios = 0
        for (dto in api().listarConsultas(fichaRemota)) {
            val idRemoto = dto.id ?: continue
            val local = dao.obtenerPorRemoteId(idRemoto)
            if (local == null) {
                val nueva = dto.aEntidad(emprendimientoId)
                dao.insertar(nueva)
                if (nueva.estado == EstadoConsulta.PENDIENTE) {
                    notificaciones.notificarNuevaConsulta(ficha.nombreComercial, nueva.nombreCliente, nueva.servicio)
                }
                cambios++
            } else if (local.estadoSync == EstadoSync.SINCRONIZADO) {
                val actualizada = local.conDatosDelServidor(dto)
                if (actualizada != local) {
                    dao.actualizar(actualizada)
                    cambios++
                }
            }
        }
        cambios
    }

    private suspend fun sincronizarUna(consulta: ConsultaEntity): Boolean {
        val idRemoto = consulta.remoteId
        if (consulta.eliminada) {
            if (idRemoto != null) eliminarEnServidor { api().eliminarConsulta(idRemoto) }
            dao.eliminarDefinitivo(consulta)
            return true
        }
        val ficha = db.emprendimientoDao().obtenerPorId(consulta.emprendimientoId)
        // La ficha se envia primero: sin su id del servidor, la consulta espera a la proxima sincronizacion
        if (ficha != null && ficha.remoteId == null && ficha.estadoSync == EstadoSync.PENDIENTE_SINCRONIZAR) return false
        val dto = consulta.aDto(ficha?.remoteId)
        val respuesta = actualizarOCrear(
            idRemoto,
            actualizar = { api().actualizarConsulta(it, dto) },
            crear = { api().crearConsulta(dto) }
        )
        dao.marcarSincronizada(consulta.id, respuesta.id ?: idRemoto, consulta.fechaActualizacion)
        return true
    }
}
