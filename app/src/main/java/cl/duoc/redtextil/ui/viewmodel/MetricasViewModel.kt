package cl.duoc.redtextil.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.redtextil.data.local.entity.InteraccionEntity
import cl.duoc.redtextil.data.repository.MetricasRepository
import cl.duoc.redtextil.domain.model.Rubro
import cl.duoc.redtextil.domain.model.TipoInteraccion
import cl.duoc.redtextil.util.ChileGeoData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class VistaMetricas(val etiqueta: String) {
    TERRITORIO("Por territorio"),
    RUBRO("Por rubro")
}

data class FilaMetrica(
    val etiqueta: String,
    val contactos: Int,
    val busquedas: Int
)

data class MetricasUiState(
    val cargando: Boolean = true,
    val porTipo: Map<TipoInteraccion, Int> = emptyMap(),
    val vista: VistaMetricas = VistaMetricas.TERRITORIO,
    val porRegion: List<FilaMetrica> = emptyList(),
    val porRubro: List<FilaMetrica> = emptyList(),
    val recientes: List<InteraccionEntity> = emptyList(),
    val pendientesEnvio: Int = 0
) {
    val totalEfectivas: Int
        get() = porTipo.filterKeys { it.esContactoEfectivo }.values.sum()
    val maximoRegion: Int
        get() = porRegion.maxOfOrNull { it.contactos } ?: 0
    val maximoRubro: Int
        get() = porRubro.maxOfOrNull { it.contactos } ?: 0
}

private data class Distribucion(
    val porTipo: Map<TipoInteraccion, Int>,
    val porRegion: List<FilaMetrica>,
    val porRubro: List<FilaMetrica>
)

class MetricasViewModel(metricas: MetricasRepository) : ViewModel() {

    private val vista = MutableStateFlow(VistaMetricas.TERRITORIO)

    private val distribucion = combine(
        metricas.conteoPorTipo(),
        metricas.contactosPorRegion(),
        metricas.busquedasPorRegion(),
        metricas.contactosPorRubro(),
        metricas.busquedasPorRubro()
    ) { tipos, contactosRegion, busquedasRegion, contactosRubro, busquedasRubro ->
        Distribucion(
            porTipo = tipos,
            // Se muestran siempre las 16 regiones, de norte a sur, aunque alguna tenga cero
            porRegion = ChileGeoData.regiones.map { region ->
                FilaMetrica(region.nombre, contactosRegion[region.nombre] ?: 0, busquedasRegion[region.nombre] ?: 0)
            },
            porRubro = Rubro.entries.map { rubro ->
                FilaMetrica(rubro.etiqueta, contactosRubro[rubro.name] ?: 0, busquedasRubro[rubro.name] ?: 0)
            }
        )
    }

    val uiState: StateFlow<MetricasUiState> = combine(
        distribucion,
        vista,
        metricas.recientes(),
        metricas.contarPendientes()
    ) { d, vistaActual, recientes, pendientes ->
        MetricasUiState(
            cargando = false,
            porTipo = d.porTipo,
            vista = vistaActual,
            porRegion = d.porRegion,
            porRubro = d.porRubro,
            recientes = recientes,
            pendientesEnvio = pendientes
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MetricasUiState())

    fun cambiarVista(nueva: VistaMetricas) {
        vista.value = nueva
    }
}
