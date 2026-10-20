package cl.duoc.redtextil.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.redtextil.data.local.entity.ConsultaConNegocio
import cl.duoc.redtextil.data.repository.ConsultaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MisConsultasUiState(
    val cargando: Boolean = true,
    val consultas: List<ConsultaConNegocio> = emptyList()
)

class MisConsultasViewModel(consultas: ConsultaRepository) : ViewModel() {

    val uiState: StateFlow<MisConsultasUiState> = consultas.observarEnviadasDesdeApp()
        .map { MisConsultasUiState(cargando = false, consultas = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MisConsultasUiState())
}
