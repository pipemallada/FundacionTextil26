package cl.duoc.redtextil.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.redtextil.data.local.entity.EmprendimientoEntity
import cl.duoc.redtextil.data.local.entity.PublicacionEntity
import cl.duoc.redtextil.data.repository.CatalogoRepository
import cl.duoc.redtextil.data.repository.EmprendimientoRepository
import cl.duoc.redtextil.data.repository.MetricasRepository
import cl.duoc.redtextil.domain.model.TipoInteraccion
import cl.duoc.redtextil.domain.validation.FechaSimple
import cl.duoc.redtextil.ui.navigation.Rutas
import cl.duoc.redtextil.util.EnlacesContacto
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetalleUiState(
    val cargando: Boolean = true,
    val emprendimiento: EmprendimientoEntity? = null,
    val publicaciones: List<PublicacionEntity> = emptyList(),
    val urlWhatsapp: String? = null,
    val urlInstagram: String? = null,
    val urlSitio: String? = null,
    val aniosTrabajando: Int? = null
)

sealed interface EventoDetalle {
    data class AbrirEnlace(val url: String) : EventoDetalle
    data class Mensaje(val texto: String) : EventoDetalle
}

class DetalleViewModel(
    savedStateHandle: SavedStateHandle,
    emprendimientos: EmprendimientoRepository,
    catalogo: CatalogoRepository,
    private val metricas: MetricasRepository
) : ViewModel() {

    private val id: Long = checkNotNull(savedStateHandle.get<Long>(Rutas.ARG_ID))

    private val _eventos = Channel<EventoDetalle>(Channel.BUFFERED)
    val eventos: Flow<EventoDetalle> = _eventos.receiveAsFlow()

    val uiState: StateFlow<DetalleUiState> = combine(
        emprendimientos.observarPorId(id),
        catalogo.observarDe(id)
    ) { emprendimiento, publicaciones ->
        DetalleUiState(
            cargando = false,
            emprendimiento = emprendimiento,
            publicaciones = publicaciones,
            urlWhatsapp = emprendimiento?.let {
                EnlacesContacto.urlWhatsapp(
                    it.whatsapp,
                    "Hola ${it.nombreEmprendedora.substringBefore(" ")}, vi ${it.nombreComercial} en el Directorio Textil Circular y quiero hacer una consulta."
                )
            },
            urlInstagram = emprendimiento?.let { EnlacesContacto.urlInstagram(it.instagram) },
            urlSitio = emprendimiento?.let { EnlacesContacto.urlSitio(it.sitioWeb) },
            aniosTrabajando = emprendimiento?.anioInicio?.let { FechaSimple.hoy().anio - it }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleUiState())

    fun abrirWhatsapp() = abrirContacto(TipoInteraccion.CLIC_WHATSAPP, uiState.value.urlWhatsapp)

    fun abrirInstagram() = abrirContacto(TipoInteraccion.CLIC_INSTAGRAM, uiState.value.urlInstagram)

    fun abrirSitio() {
        val url = uiState.value.urlSitio ?: return
        viewModelScope.launch { _eventos.send(EventoDetalle.AbrirEnlace(url)) }
    }

    private fun abrirContacto(tipo: TipoInteraccion, url: String?) {
        val emprendimiento = uiState.value.emprendimiento ?: return
        viewModelScope.launch {
            if (url == null) {
                _eventos.send(EventoDetalle.Mensaje("Este enlace no está disponible. Usa el formulario de consulta."))
                return@launch
            }
            // Primero se registra el clic para la trazabilidad y recien despues se abre la app externa
            metricas.registrarContacto(tipo, emprendimiento)
            _eventos.send(EventoDetalle.AbrirEnlace(url))
        }
    }

    fun enlaceNoAbrio() {
        viewModelScope.launch {
            _eventos.send(EventoDetalle.Mensaje("No encontramos una app para abrir el enlace en este teléfono."))
        }
    }
}
