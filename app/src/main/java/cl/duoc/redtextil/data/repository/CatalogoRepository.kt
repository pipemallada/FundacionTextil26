package cl.duoc.redtextil.data.repository

import cl.duoc.redtextil.data.local.database.AppDatabase
import cl.duoc.redtextil.data.local.entity.PublicacionEntity
import cl.duoc.redtextil.data.remote.RedTextilApiService
import cl.duoc.redtextil.data.remote.aDto
import cl.duoc.redtextil.data.remote.aEntidad
import cl.duoc.redtextil.data.remote.actualizarOCrear
import cl.duoc.redtextil.data.remote.conDatosDelServidor
import cl.duoc.redtextil.data.remote.eliminarEnServidor
import cl.duoc.redtextil.domain.model.EstadoSync
import cl.duoc.redtextil.domain.model.PublicacionForm
import cl.duoc.redtextil.domain.validation.CatalogoValidator
import cl.duoc.redtextil.util.ImageCompressor
import cl.duoc.redtextil.util.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class CatalogoRepository(
    private val db: AppDatabase,
    private val api: () -> RedTextilApiService,
    private val red: NetworkMonitor,
    private val compresor: ImageCompressor
) {

    private val dao = db.publicacionDao()

    fun observarDe(emprendimientoId: Long): Flow<List<PublicacionEntity>> = dao.observarDeEmprendimiento(emprendimientoId)

    fun contarDe(emprendimientoId: Long): Flow<Int> = dao.contarDeEmprendimiento(emprendimientoId)

    fun portadas(): Flow<Map<Long, String>> = dao.observarPortadas().map { lista ->
        lista.mapNotNull { portada -> portada.fotoRuta?.let { portada.emprendimientoId to it } }.toMap()
    }

    fun contarPendientes(): Flow<Int> = dao.contarPendientes()

    suspend fun obtener(id: Long): PublicacionEntity? = dao.obtenerPorId(id)

    suspend fun guardar(
        emprendimientoId: Long,
        idExistente: Long?,
        form: PublicacionForm,
        kbOriginal: Long,
        kbOptimizado: Long
    ): Long {
        val categoria = requireNotNull(form.categoria) { "La categoría es obligatoria" }
        val precio = CatalogoValidator.normalizarPrecio(form.precio) ?: 0
        val ahora = System.currentTimeMillis()
        val existente = idExistente?.let { dao.obtenerPorId(it) }

        val id = if (existente == null) {
            dao.insertar(
                PublicacionEntity(
                    emprendimientoId = emprendimientoId,
                    titulo = form.titulo.trim(),
                    categoria = categoria,
                    tecnica = form.tecnica.trim(),
                    precioReferencial = precio,
                    descripcion = form.descripcion.trim(),
                    fotoRuta = form.fotoRuta,
                    kbOriginal = kbOriginal,
                    kbOptimizado = kbOptimizado,
                    fechaCreacion = ahora,
                    fechaActualizacion = ahora
                )
            )
        } else {
            val cambioFoto = existente.fotoRuta != form.fotoRuta
            // Si cambio la foto borramos la anterior para no llenar la memoria del telefono
            if (cambioFoto) compresor.eliminarArchivo(existente.fotoRuta)
            dao.actualizar(
                existente.copy(
                    titulo = form.titulo.trim(),
                    categoria = categoria,
                    tecnica = form.tecnica.trim(),
                    precioReferencial = precio,
                    descripcion = form.descripcion.trim(),
                    fotoRuta = form.fotoRuta,
                    kbOriginal = if (cambioFoto) kbOriginal else existente.kbOriginal,
                    kbOptimizado = if (cambioFoto) kbOptimizado else existente.kbOptimizado,
                    fotoSincronizada = existente.fotoSincronizada && !cambioFoto,
                    estadoSync = EstadoSync.PENDIENTE_SINCRONIZAR,
                    fechaActualizacion = ahora
                )
            )
            existente.id
        }
        if (red.enLinea.value) {
            dao.obtenerPorId(id)?.let { guardada -> runCatching { sincronizarUna(guardada) } }
        }
        return id
    }

    suspend fun eliminar(id: Long) {
        val publicacion = dao.obtenerPorId(id) ?: return
        if (publicacion.remoteId == null) {
            compresor.eliminarArchivo(publicacion.fotoRuta)
            dao.eliminarDefinitivo(publicacion)
            return
        }
        // Ya estaba en el servidor: la marcamos y se borra alla en la proxima sincronizacion
        val marcada = publicacion.copy(
            eliminada = true,
            estadoSync = EstadoSync.PENDIENTE_SINCRONIZAR,
            fechaActualizacion = System.currentTimeMillis()
        )
        dao.actualizar(marcada)
        if (red.enLinea.value) runCatching { sincronizarUna(marcada) }
    }

    suspend fun sincronizarPendientes(): Int = dao.pendientesDeSincronizar().count { sincronizarUna(it) }

    // Trae el catalogo guardado en el microservicio: agrega los trabajos nuevos y actualiza los que cambiaron
    suspend fun actualizarDesdeServidor(emprendimientoId: Long): Result<Int> = runCatching {
        check(red.enLinea.value) { "Sin conexión" }
        val fichaRemota = db.emprendimientoDao().obtenerPorId(emprendimientoId)?.remoteId ?: return@runCatching 0
        var cambios = 0
        for (dto in api().listarPublicaciones(fichaRemota)) {
            val idRemoto = dto.id ?: continue
            val local = dao.obtenerPorRemoteId(idRemoto)
            if (local == null) {
                dao.insertar(dto.aEntidad(emprendimientoId))
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

    private suspend fun sincronizarUna(publicacion: PublicacionEntity): Boolean {
        val idRemoto = publicacion.remoteId
        if (publicacion.eliminada) {
            if (idRemoto != null) eliminarEnServidor { api().eliminarPublicacion(idRemoto) }
            compresor.eliminarArchivo(publicacion.fotoRuta)
            dao.eliminarDefinitivo(publicacion)
            return true
        }
        val ficha = db.emprendimientoDao().obtenerPorId(publicacion.emprendimientoId)
        // La ficha se envia primero: sin su id del servidor, el trabajo espera a la proxima sincronizacion
        if (ficha != null && ficha.remoteId == null && ficha.estadoSync == EstadoSync.PENDIENTE_SINCRONIZAR) return false
        val dto = publicacion.aDto(ficha?.remoteId)
        val respuesta = actualizarOCrear(
            idRemoto,
            actualizar = { api().actualizarPublicacion(it, dto) },
            crear = { api().crearPublicacion(dto) }
        )
        val nuevoIdRemoto = respuesta.id ?: idRemoto
        val ruta = publicacion.fotoRuta
        // La foto ya va comprimida (max 1080 px y JPEG al 75%), asi se sube rapido aunque la senal sea mala
        if (nuevoIdRemoto != null && ruta != null && !publicacion.fotoSincronizada) {
            val archivo = File(ruta)
            if (archivo.exists()) {
                val parte = MultipartBody.Part.createFormData(
                    "foto",
                    archivo.name,
                    archivo.asRequestBody("image/jpeg".toMediaType())
                )
                api().subirFoto(nuevoIdRemoto, parte)
            }
        }
        dao.marcarSincronizada(publicacion.id, nuevoIdRemoto, publicacion.fechaActualizacion)
        return true
    }
}
