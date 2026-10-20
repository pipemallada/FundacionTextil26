package cl.duoc.redtextil.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.redtextil.data.local.entity.PublicacionEntity
import cl.duoc.redtextil.data.repository.CatalogoRepository
import cl.duoc.redtextil.data.repository.SesionRepository
import cl.duoc.redtextil.domain.model.EstadoSync
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CatalogoUiState(
    val cargando: Boolean = true,
    val sinSesion: Boolean = false,
    val nombreComercial: String = "",
    val publicaciones: List<PublicacionEntity> = emptyList(),
    val pendientesEnvio: Int = 0,
    val mensaje: String? = null
)

class CatalogoViewModel(
    sesion: SesionRepository,
    private val catalogo: CatalogoRepository
) : ViewModel() {

    private val mensaje = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val datos = sesion.emprendimientoActual.flatMapLatest { actual ->
        if (actual == null) {
            flowOf(CatalogoUiState(cargando = false, sinSesion = true))
        } else {
            catalogo.observarDe(actual.id).map { lista ->
                CatalogoUiState(
                    cargando = false,
                    nombreComercial = actual.nombreComercial,
                    publicaciones = lista,
                    pendientesEnvio = lista.count { it.estadoSync == EstadoSync.PENDIENTE_SINCRONIZAR }
                )
            }
        }
    }

    val uiState: StateFlow<CatalogoUiState> = combine(datos, mensaje) { estado, texto ->
        estado.copy(mensaje = texto)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogoUiState())

    fun eliminar(publicacion: PublicacionEntity) {
        viewModelScope.launch {
            catalogo.eliminar(publicacion.id)
            mensaje.value = "Se eliminó \"${publicacion.titulo}\" de tu catálogo"
        }
    }

    fun mensajeMostrado() {
        mensaje.value = null
    }
}
