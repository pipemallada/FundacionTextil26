package cl.duoc.redtextil.data.repository

import cl.duoc.redtextil.data.local.database.AppDatabase
import cl.duoc.redtextil.data.local.entity.EmprendimientoEntity
import cl.duoc.redtextil.data.remote.RedTextilApiService
import cl.duoc.redtextil.data.remote.aDto
import cl.duoc.redtextil.domain.model.EstadoFicha
import cl.duoc.redtextil.domain.model.EstadoSync
import cl.duoc.redtextil.domain.model.FichaDatos
import cl.duoc.redtextil.domain.model.OrigenFicha
import cl.duoc.redtextil.domain.model.PlantillasCorreo
import cl.duoc.redtextil.domain.model.RegistroForm
import cl.duoc.redtextil.domain.model.Rubro
import cl.duoc.redtextil.domain.model.TipoCorreo
import cl.duoc.redtextil.domain.validation.FichaValidator
import cl.duoc.redtextil.domain.validation.ReglasComunes
import cl.duoc.redtextil.util.NetworkMonitor
import cl.duoc.redtextil.util.NotificationHelper
import cl.duoc.redtextil.util.TextoUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed class ResultadoRegistro {
    data class Exito(val id: Long) : ResultadoRegistro()
    data class Error(val mensaje: String) : ResultadoRegistro()
}

class EmprendimientoRepository(
    db: AppDatabase,
    private val api: () -> RedTextilApiService,
    private val red: NetworkMonitor,
    private val correos: CorreoRepository,
    private val notificaciones: NotificationHelper
) {

    private val dao = db.emprendimientoDao()

    fun observarPorId(id: Long): Flow<EmprendimientoEntity?> = dao.observarPorId(id)

    fun observarCuentasLocales(): Flow<List<EmprendimientoEntity>> = dao.observarCuentasLocales()

    fun buscarDirectorio(texto: String, region: String, comuna: String, rubro: Rubro?): Flow<List<EmprendimientoEntity>> =
        dao.buscarActivas(texto.trim(), region, comuna, rubro?.name.orEmpty())

    fun observarParaAdmin(estado: EstadoFicha?, texto: String): Flow<List<EmprendimientoEntity>> =
        dao.observarParaAdmin(estado?.name.orEmpty(), texto.trim())

    fun contarPorEstado(): Flow<Map<EstadoFicha, Int>> = dao.contarPorEstado().map { conteos ->
        conteos.mapNotNull { conteo ->
            EstadoFicha.entries.firstOrNull { it.name == conteo.clave }?.let { it to conteo.total }
        }.toMap()
    }

    fun contarPendientes(): Flow<Int> = dao.contarPendientes()

    suspend fun obtener(id: Long): EmprendimientoEntity? = dao.obtenerPorId(id)

    suspend fun registrar(form: RegistroForm): ResultadoRegistro {
        val rut = ReglasComunes.formatearRut(form.rut)
        if (dao.buscarPorRut(rut) != null) {
            return ResultadoRegistro.Error("Ya existe una cuenta con este RUT. Búscala en la lista de cuentas.")
        }
        val correo = form.correo.trim().lowercase()
        if (dao.buscarPorCorreo(correo) != null) {
            return ResultadoRegistro.Error("Ya existe una cuenta con este correo electrónico.")
        }
        val telefono = ReglasComunes.normalizarTelefono(form.telefono).orEmpty()
        val ahora = System.currentTimeMillis()
        val nueva = EmprendimientoEntity(
            nombreEmprendedora = TextoUtil.limpiarEspacios(form.nombreEmprendedora),
            rut = rut,
            correo = correo,
            telefono = telefono,
            whatsapp = telefono,
            nombreComercial = TextoUtil.limpiarEspacios(form.nombreComercial),
            region = form.region,
            comuna = form.comuna,
            aceptaPrivacidad = form.aceptaPrivacidad,
            estado = EstadoFicha.PENDIENTE,
            origen = OrigenFicha.APP,
            estadoSync = EstadoSync.PENDIENTE_SINCRONIZAR,
            fechaRegistro = ahora,
            fechaActualizacion = ahora
        )
        val id = dao.insertar(nueva)
        val guardada = nueva.copy(id = id)

        correos.programar(
            guardada,
            TipoCorreo.BIENVENIDA,
            PlantillasCorreo.bienvenida(guardada.nombreEmprendedora, guardada.nombreComercial)
        )
        correos.programar(
            guardada,
            TipoCorreo.CONFIRMACION_INSCRIPCION,
            PlantillasCorreo.confirmacionInscripcion(
                guardada.nombreEmprendedora,
                guardada.nombreComercial,
                guardada.comuna,
                guardada.region
            )
        )
        notificaciones.notificarRegistro(guardada.nombreComercial)
        if (red.enLinea.value) runCatching { sincronizarUna(guardada) }
        return ResultadoRegistro.Exito(id)
    }

    suspend fun guardarFicha(id: Long, datos: FichaDatos): EmprendimientoEntity? {
        val actual = dao.obtenerPorId(id) ?: return null
        val limpios = FichaValidator.limpiarCondicionales(datos).let { d ->
            d.copy(
                rut = ReglasComunes.formatearRut(d.rut),
                telefono = ReglasComunes.normalizarTelefono(d.telefono) ?: d.telefono,
                whatsapp = ReglasComunes.normalizarTelefono(d.whatsapp) ?: d.whatsapp,
                rutEmpresa = if (d.formalizado) ReglasComunes.formatearRut(d.rutEmpresa) else "",
                instagram = ReglasComunes.normalizarInstagram(d.instagram).orEmpty(),
                sitioWeb = ReglasComunes.normalizarUrl(d.sitioWeb).orEmpty()
            )
        }
        // Si la ficha estaba observada y la emprendedora la corrige, vuelve a revision de la administradora
        val nuevoEstado = if (actual.estado == EstadoFicha.OBSERVADA) EstadoFicha.PENDIENTE else actual.estado
        val actualizada = actual.conDatos(limpios).copy(
            estado = nuevoEstado,
            observacion = if (nuevoEstado != actual.estado) "" else actual.observacion,
            estadoSync = EstadoSync.PENDIENTE_SINCRONIZAR,
            fechaActualizacion = System.currentTimeMillis()
        )
        dao.actualizar(actualizada)
        if (red.enLinea.value) runCatching { sincronizarUna(actualizada) }
        return dao.obtenerPorId(id)
    }

    suspend fun cambiarEstado(id: Long, nuevo: EstadoFicha, observacion: String = ""): EmprendimientoEntity? {
        val actual = dao.obtenerPorId(id) ?: return null
        val actualizada = actual.copy(
            estado = nuevo,
            observacion = if (nuevo == EstadoFicha.OBSERVADA) observacion.trim() else "",
            estadoSync = EstadoSync.PENDIENTE_SINCRONIZAR,
            fechaActualizacion = System.currentTimeMillis()
        )
        dao.actualizar(actualizada)

        when (nuevo) {
            EstadoFicha.ACTIVA -> correos.programar(
                actualizada,
                TipoCorreo.APROBACION,
                PlantillasCorreo.aprobacion(actualizada.nombreEmprendedora, actualizada.nombreComercial)
            )
            EstadoFicha.OBSERVADA -> correos.programar(
                actualizada,
                TipoCorreo.OBSERVACION,
                PlantillasCorreo.observacion(actualizada.nombreEmprendedora, actualizada.nombreComercial, actualizada.observacion)
            )
            else -> Unit
        }
        if (actualizada.origen != OrigenFicha.HISTORICO) {
            notificaciones.notificarCambioEstado(actualizada.nombreComercial, nuevo.etiqueta)
        }
        if (red.enLinea.value) runCatching { sincronizarUna(actualizada) }
        return actualizada
    }

    suspend fun sincronizarPendientes(): Int {
        var enviados = 0
        for (pendiente in dao.pendientesDeSincronizar()) {
            sincronizarUna(pendiente)
            enviados++
        }
        return enviados
    }

    private suspend fun sincronizarUna(emprendimiento: EmprendimientoEntity) {
        val idRemoto = emprendimiento.remoteId
        val respuesta = if (idRemoto == null) {
            api().crearFicha(emprendimiento.aDto())
        } else {
            api().actualizarFicha(idRemoto, emprendimiento.aDto())
        }
        dao.marcarSincronizado(emprendimiento.id, respuesta.id ?: idRemoto, emprendimiento.fechaActualizacion)
    }
}
