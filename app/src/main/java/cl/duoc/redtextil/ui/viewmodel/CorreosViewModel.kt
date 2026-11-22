package cl.duoc.redtextil.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.redtextil.data.local.entity.CorreoEntity
import cl.duoc.redtextil.data.repository.CorreoRepository
import cl.duoc.redtextil.domain.model.EstadoCorreo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CorreosUiState(
    val cargando: Boolean = true,
    val filtro: EstadoCorreo? = null,
    val correos: List<CorreoEntity> = emptyList(),
    val conteos: Map<EstadoCorreo, Int> = emptyMap(),
    val reenviandoId: Long? = null,
    val reenviandoTodos: Boolean = false,
    val mensaje: String? = null
) {
    val total: Int get() = conteos.values.sum()
    val noEnviados: Int get() = total - (conteos[EstadoCorreo.ENVIADO_OK] ?: 0)
}

private data class AccionesCorreo(
    val reenviandoId: Long? = null,
    val reenviandoTodos: Boolean = false,
    val mensaje: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class CorreosViewModel(private val correos: CorreoRepository) : ViewModel() {

    private val filtro = MutableStateFlow<EstadoCorreo?>(null)
    private val acciones = MutableStateFlow(AccionesCorreo())

    val uiState: StateFlow<CorreosUiState> = combine(
        filtro,
        filtro.flatMapLatest { correos.observar(it) },
        correos.contarPorEstado(),
        acciones
    ) { f, lista, conteos, a ->
        CorreosUiState(
            cargando = false,
            filtro = f,
            correos = lista,
            conteos = conteos,
            reenviandoId = a.reenviandoId,
            reenviandoTodos = a.reenviandoTodos,
            mensaje = a.mensaje
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CorreosUiState())

    fun cambiarFiltro(estado: EstadoCorreo?) {
        filtro.value = estado
    }

    fun reenviar(correo: CorreoEntity) {
        viewModelScope.launch {
            acciones.update { it.copy(reenviandoId = correo.id) }
            val resultado = correos.reenviar(correo.id)
            val texto = when (resultado?.estado) {
                EstadoCorreo.ENVIADO_OK -> "Correo reenviado a ${correo.destinatario}"
                EstadoCorreo.EN_COLA_OFFLINE -> "Sin conexión: el correo sigue en cola y se enviará al volver la señal"
                else -> "No se pudo enviar: ${resultado?.detalleError ?: "error desconocido"}"
            }
            acciones.update { it.copy(reenviandoId = null, mensaje = texto) }
        }
    }

    fun reenviarTodos() {
        viewModelScope.launch {
            acciones.update { it.copy(reenviandoTodos = true) }
            val enviados = correos.reenviarPendientes()
            acciones.update {
                it.copy(
                    reenviandoTodos = false,
                    mensaje = if (enviados > 0) "Se enviaron $enviados correos pendientes" else "No se pudo enviar ningún correo. Revisa la conexión."
                )
            }
        }
    }

    fun mensajeMostrado() = acciones.update { it.copy(mensaje = null) }
}
