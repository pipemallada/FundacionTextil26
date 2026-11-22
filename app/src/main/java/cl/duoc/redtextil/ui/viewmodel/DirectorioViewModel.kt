package cl.duoc.redtextil.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.redtextil.data.local.entity.EmprendimientoEntity
import cl.duoc.redtextil.data.repository.CatalogoRepository
import cl.duoc.redtextil.data.repository.EmprendimientoRepository
import cl.duoc.redtextil.data.repository.MetricasRepository
import cl.duoc.redtextil.domain.model.Rubro
import cl.duoc.redtextil.util.ChileGeoData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FiltrosDirectorio(
    val texto: String = "",
    val region: String = "",
    val comuna: String = "",
    val rubro: Rubro? = null
) {
    val tieneCriterios: Boolean
        get() = texto.isNotBlank() || region.isNotEmpty() || comuna.isNotEmpty() || rubro != null
}

data class DirectorioUiState(
    val cargando: Boolean = true,
    val filtros: FiltrosDirectorio = FiltrosDirectorio(),
    val comunas: List<String> = emptyList(),
    val resultados: List<EmprendimientoEntity> = emptyList(),
    val portadas: Map<Long, String> = emptyMap()
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class DirectorioViewModel(
    private val emprendimientos: EmprendimientoRepository,
    catalogo: CatalogoRepository,
    private val metricas: MetricasRepository
) : ViewModel() {

    private val filtros = MutableStateFlow(FiltrosDirectorio())

    private val resultados = filtros.debounce(250).flatMapLatest { f ->
        emprendimientos.buscarDirectorio(f.texto, f.region, f.comuna, f.rubro)
    }

    val uiState: StateFlow<DirectorioUiState> = combine(filtros, resultados, catalogo.portadas()) { f, lista, portadas ->
        DirectorioUiState(
            cargando = false,
            filtros = f,
            comunas = ChileGeoData.comunasDe(f.region),
            resultados = lista,
            portadas = portadas
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DirectorioUiState())

    init {
        // Cada busqueda queda registrada para las metricas de la administradora (se espera a que termine de escribir)
        viewModelScope.launch {
            filtros.debounce(1_200)
                .distinctUntilChanged()
                .drop(1)
                .filter { it.tieneCriterios }
                .collect { f -> metricas.registrarBusqueda(f.texto, f.region, f.rubro) }
        }
    }

    fun onTexto(texto: String) = filtros.update { it.copy(texto = texto) }

    // Limpiamos la comuna al cambiar de region para que el filtro no quede inconsistente
    fun onRegion(region: String) = filtros.update {
        it.copy(region = if (region == TODAS_LAS_REGIONES) "" else region, comuna = "")
    }

    fun onComuna(comuna: String) = filtros.update {
        it.copy(comuna = if (comuna == TODAS_LAS_COMUNAS) "" else comuna)
    }

    fun onRubro(rubro: Rubro?) = filtros.update { it.copy(rubro = if (it.rubro == rubro) null else rubro) }

    fun limpiarFiltros() {
        filtros.value = FiltrosDirectorio()
    }

    companion object {
        const val TODAS_LAS_REGIONES = "Todas las regiones"
        const val TODAS_LAS_COMUNAS = "Todas las comunas"
    }
}
