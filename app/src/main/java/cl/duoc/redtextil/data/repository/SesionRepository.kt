package cl.duoc.redtextil.data.repository

import cl.duoc.redtextil.data.local.PreferenciasLocales
import cl.duoc.redtextil.data.local.dao.EmprendimientoDao
import cl.duoc.redtextil.data.local.entity.EmprendimientoEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class SesionRepository(
    private val preferencias: PreferenciasLocales,
    private val dao: EmprendimientoDao
) {

    val sesionId: StateFlow<Long?> = preferencias.sesionId

    @OptIn(ExperimentalCoroutinesApi::class)
    val emprendimientoActual: Flow<EmprendimientoEntity?> = preferencias.sesionId.flatMapLatest { id ->
        if (id == null) flowOf(null) else dao.observarPorId(id)
    }

    fun iniciarSesion(id: Long) = preferencias.guardarSesion(id)

    fun cerrarSesion() = preferencias.guardarSesion(null)
}
