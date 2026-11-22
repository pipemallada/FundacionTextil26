package cl.duoc.redtextil.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.redtextil.data.repository.IndicadoresEconomicos
import cl.duoc.redtextil.data.repository.IndicadoresRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface IndicadoresUiState {
    data object Cargando : IndicadoresUiState
    data class Listo(val indicadores: IndicadoresEconomicos) : IndicadoresUiState
    data class Error(val mensaje: String) : IndicadoresUiState
}

class IndicadoresViewModel(
    private val repositorio: IndicadoresRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<IndicadoresUiState>(IndicadoresUiState.Cargando)
    val uiState: StateFlow<IndicadoresUiState> = _uiState.asStateFlow()

    init {
        cargar(forzar = false)
    }

    fun reintentar() = cargar(forzar = true)

    private fun cargar(forzar: Boolean) {
        _uiState.value = IndicadoresUiState.Cargando
        viewModelScope.launch {
            _uiState.value = repositorio.obtener(forzar).fold(
                onSuccess = { IndicadoresUiState.Listo(it) },
                onFailure = { IndicadoresUiState.Error("No se pudieron cargar los indicadores. Revisa tu conexión.") }
            )
        }
    }
}
