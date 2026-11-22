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
import kotlinx.coroutines.flow.filterNotNull
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
    val actualizando: Boolean = false,
    val mensaje: String? = null
)

class CatalogoViewModel(
    private val sesion: SesionRepository,
    private val catalogo: CatalogoRepository
) : ViewModel() {

    private val mensaje = MutableStateFlow<String?>(null)
    private val actualizando = MutableStateFlow(false)

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

    val uiState: StateFlow<CatalogoUiState> = combine(datos, mensaje, actualizando) { estado, texto, recibiendo ->
        estado.copy(mensaje = texto, actualizando = recibiendo)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogoUiState())

    init {
        // Cada vez que se abre una cuenta se reciben los cambios del microservicio sin molestar
        viewModelScope.launch {
            sesion.sesionId.filterNotNull().collect { actualizar(it, silencioso = true) }
        }
    }

    fun actualizarDesdeServidor() {
        sesion.sesionId.value?.let { actualizar(it, silencioso = false) }
    }

    private fun actualizar(emprendimientoId: Long, silencioso: Boolean) {
        if (actualizando.value) return
        viewModelScope.launch {
            actualizando.value = true
            val resultado = catalogo.actualizarDesdeServidor(emprendimientoId)
            actualizando.value = false
            val cambios = resultado.getOrDefault(0)
            if (!silencioso || cambios > 0) {
                mensaje.value = when {
                    resultado.isFailure -> "No se pudo conectar con el servidor. Se muestra lo guardado en el teléfono."
                    cambios > 0 -> "Se recibieron $cambios cambios del servidor"
                    else -> "Tu catálogo ya está al día con el servidor"
                }
            }
        }
    }

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
