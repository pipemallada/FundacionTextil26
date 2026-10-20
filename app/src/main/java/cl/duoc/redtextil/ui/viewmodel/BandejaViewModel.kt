package cl.duoc.redtextil.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.redtextil.data.local.entity.ConsultaEntity
import cl.duoc.redtextil.data.local.entity.EmprendimientoEntity
import cl.duoc.redtextil.data.repository.ConsultaRepository
import cl.duoc.redtextil.data.repository.SesionRepository
import cl.duoc.redtextil.domain.model.EstadoConsulta
import cl.duoc.redtextil.domain.validation.ConsultaValidator
import cl.duoc.redtextil.util.EnlacesContacto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FiltroConsultas(val etiqueta: String) {
    TODAS("Todas"),
    PENDIENTES("Pendientes"),
    RESPONDIDAS("Respondidas")
}

data class BandejaUiState(
    val cargando: Boolean = true,
    val sinSesion: Boolean = false,
    val nombreComercial: String = "",
    val consultas: List<ConsultaEntity> = emptyList(),
    val filtro: FiltroConsultas = FiltroConsultas.TODAS,
    val total: Int = 0,
    val pendientes: Int = 0,
    val respondidas: Int = 0,
    val borradores: Map<Long, String> = emptyMap(),
    val errores: Map<Long, String> = emptyMap(),
    val guardandoId: Long? = null,
    val mensaje: String? = null
)

private data class EstadoEdicion(
    val borradores: Map<Long, String> = emptyMap(),
    val errores: Map<Long, String> = emptyMap(),
    val guardandoId: Long? = null,
    val mensaje: String? = null
)

class BandejaViewModel(
    sesion: SesionRepository,
    private val consultas: ConsultaRepository
) : ViewModel() {

    private val filtro = MutableStateFlow(FiltroConsultas.TODAS)
    private val edicion = MutableStateFlow(EstadoEdicion())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val bandeja = sesion.emprendimientoActual.flatMapLatest { actual ->
        if (actual == null) {
            flowOf<Pair<EmprendimientoEntity?, List<ConsultaEntity>>>(null to emptyList())
        } else {
            consultas.observarBandeja(actual.id).map { actual to it }
        }
    }

    val uiState: StateFlow<BandejaUiState> = combine(bandeja, filtro, edicion) { (emprendimiento, lista), filtroActual, ed ->
        val filtradas = when (filtroActual) {
            FiltroConsultas.TODAS -> lista
            FiltroConsultas.PENDIENTES -> lista.filter { it.estado == EstadoConsulta.PENDIENTE }
            FiltroConsultas.RESPONDIDAS -> lista.filter { it.estado == EstadoConsulta.RESPONDIDA }
        }
        BandejaUiState(
            cargando = false,
            sinSesion = emprendimiento == null,
            nombreComercial = emprendimiento?.nombreComercial.orEmpty(),
            consultas = filtradas,
            filtro = filtroActual,
            total = lista.size,
            pendientes = lista.count { it.estado == EstadoConsulta.PENDIENTE },
            respondidas = lista.count { it.estado == EstadoConsulta.RESPONDIDA },
            borradores = ed.borradores,
            errores = ed.errores,
            guardandoId = ed.guardandoId,
            mensaje = ed.mensaje
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BandejaUiState())

    fun cambiarFiltro(nuevo: FiltroConsultas) {
        filtro.value = nuevo
    }

    fun onBorrador(id: Long, texto: String) {
        edicion.update { ed ->
            val errores = if (id in ed.errores) {
                val error = ConsultaValidator.validarRespuesta(texto)
                if (error == null) ed.errores - id else ed.errores + (id to error)
            } else {
                ed.errores
            }
            ed.copy(borradores = ed.borradores + (id to texto), errores = errores)
        }
    }

    fun guardarRespuesta(consulta: ConsultaEntity) {
        val texto = edicion.value.borradores[consulta.id] ?: consulta.respuesta
        val error = ConsultaValidator.validarRespuesta(texto)
        if (error != null) {
            edicion.update { it.copy(errores = it.errores + (consulta.id to error)) }
            return
        }
        viewModelScope.launch {
            edicion.update { it.copy(guardandoId = consulta.id) }
            consultas.responder(consulta.id, texto)
            edicion.update {
                it.copy(
                    borradores = it.borradores - consulta.id,
                    errores = it.errores - consulta.id,
                    guardandoId = null,
                    mensaje = "Respuesta guardada para ${consulta.nombreCliente}"
                )
            }
        }
    }

    fun alternarEstado(consulta: ConsultaEntity) {
        val nuevo = if (consulta.estado == EstadoConsulta.PENDIENTE) EstadoConsulta.RESPONDIDA else EstadoConsulta.PENDIENTE
        viewModelScope.launch {
            consultas.cambiarEstado(consulta.id, nuevo)
            edicion.update { it.copy(mensaje = "Consulta marcada como ${nuevo.etiqueta.lowercase()}") }
        }
    }

    fun urlWhatsapp(consulta: ConsultaEntity): String? {
        val negocio = uiState.value.nombreComercial
        return EnlacesContacto.urlWhatsapp(
            consulta.telefonoCliente,
            "Hola ${consulta.nombreCliente.substringBefore(" ")}, te escribo de $negocio por tu consulta sobre ${consulta.servicio}."
        )
    }

    fun avisar(texto: String) {
        edicion.update { it.copy(mensaje = texto) }
    }

    fun mensajeMostrado() {
        edicion.update { it.copy(mensaje = null) }
    }
}
