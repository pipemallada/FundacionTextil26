package cl.duoc.redtextil.data.repository

import cl.duoc.redtextil.data.local.dao.ConteoPorClave
import cl.duoc.redtextil.data.local.dao.InteraccionDao
import cl.duoc.redtextil.data.local.entity.EmprendimientoEntity
import cl.duoc.redtextil.data.local.entity.InteraccionEntity
import cl.duoc.redtextil.data.remote.RedTextilApiService
import cl.duoc.redtextil.data.remote.aDto
import cl.duoc.redtextil.domain.model.Rubro
import cl.duoc.redtextil.domain.model.TipoInteraccion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MetricasRepository(
    private val dao: InteraccionDao,
    private val api: () -> RedTextilApiService
) {

    suspend fun registrarBusqueda(texto: String, region: String, rubro: Rubro?) {
        dao.insertar(
            InteraccionEntity(
                tipo = TipoInteraccion.BUSQUEDA,
                region = region,
                rubro = rubro?.name.orEmpty(),
                detalle = texto.trim().take(60)
            )
        )
    }

    suspend fun registrarContacto(tipo: TipoInteraccion, emprendimiento: EmprendimientoEntity, detalle: String = "") {
        dao.insertar(
            InteraccionEntity(
                tipo = tipo,
                emprendimientoId = emprendimiento.id,
                nombreComercial = emprendimiento.nombreComercial,
                region = emprendimiento.region,
                rubro = emprendimiento.rubroPrincipal,
                detalle = detalle
            )
        )
    }

    fun conteoPorTipo(): Flow<Map<TipoInteraccion, Int>> = dao.contarPorTipo().map { conteos ->
        conteos.mapNotNull { conteo ->
            TipoInteraccion.entries.firstOrNull { it.name == conteo.clave }?.let { it to conteo.total }
        }.toMap()
    }

    fun contactosPorRegion(): Flow<Map<String, Int>> = dao.contactosPorRegion().map { it.aMapa() }

    fun busquedasPorRegion(): Flow<Map<String, Int>> = dao.busquedasPorRegion().map { it.aMapa() }

    fun contactosPorRubro(): Flow<Map<String, Int>> = dao.contactosPorRubro().map { it.aMapa() }

    fun busquedasPorRubro(): Flow<Map<String, Int>> = dao.busquedasPorRubro().map { it.aMapa() }

    fun recientes(limite: Int = 12): Flow<List<InteraccionEntity>> = dao.observarRecientes(limite)

    fun contarPendientes(): Flow<Int> = dao.contarNoSincronizadas()

    suspend fun sincronizarPendientes(): Int {
        val lote = dao.noSincronizadas()
        if (lote.isEmpty()) return 0
        api().enviarInteracciones(lote.map { it.aDto() })
        dao.marcarSincronizadas(lote.map { it.id })
        return lote.size
    }

    private fun List<ConteoPorClave>.aMapa(): Map<String, Int> = associate { it.clave to it.total }
}
