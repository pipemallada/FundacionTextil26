package cl.duoc.redtextil.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.redtextil.data.local.entity.EmprendimientoEntity
import cl.duoc.redtextil.data.repository.ConsultaRepository
import cl.duoc.redtextil.data.repository.EmprendimientoRepository
import cl.duoc.redtextil.domain.model.CampoConsulta
import cl.duoc.redtextil.domain.model.ConsultaForm
import cl.duoc.redtextil.domain.validation.ConsultaValidator
import cl.duoc.redtextil.ui.navigation.Rutas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConsultaFormUiState(
    val cargando: Boolean = true,
    val emprendimiento: EmprendimientoEntity? = null,
    val servicios: List<String> = emptyList(),
    val form: ConsultaForm = ConsultaForm(),
    val errores: Map<CampoConsulta, String> = emptyMap(),
    val validos: Set<CampoConsulta> = emptySet(),
    val enviando: Boolean = false,
    val enviada: Boolean = false,
    val mensaje: String? = null
)

class ConsultaFormViewModel(
    savedStateHandle: SavedStateHandle,
    private val emprendimientos: EmprendimientoRepository,
    private val consultas: ConsultaRepository
) : ViewModel() {

    private val id: Long = checkNotNull(savedStateHandle.get<Long>(Rutas.ARG_ID))

    private val _uiState = MutableStateFlow(ConsultaFormUiState())
    val uiState: StateFlow<ConsultaFormUiState> = _uiState.asStateFlow()

    private var tocados: Set<CampoConsulta> = emptySet()
    private var intentoEnviar = false

    init {
        viewModelScope.launch {
            val encontrado = emprendimientos.obtener(id)
            val servicios = encontrado?.listaRubros()?.map { it.etiqueta }.orEmpty() + SERVICIO_GENERAL
            _uiState.update { it.copy(cargando = false, emprendimiento = encontrado, servicios = servicios) }
        }
    }

    fun onNombre(valor: String) = actualizar(CampoConsulta.NOMBRE) { it.copy(nombre = valor) }
    fun onCorreo(valor: String) = actualizar(CampoConsulta.CORREO) { it.copy(correo = valor) }
    fun onTelefono(valor: String) = actualizar(CampoConsulta.TELEFONO) { it.copy(telefono = valor) }
    fun onServicio(valor: String) = actualizar(CampoConsulta.SERVICIO) { it.copy(servicio = valor) }
    fun onMensaje(valor: String) = actualizar(CampoConsulta.MENSAJE) { it.copy(mensaje = valor) }

    fun enviar() {
        val estado = _uiState.value
        val emprendimiento = estado.emprendimiento ?: return
        val errores = ConsultaValidator.validarTodo(estado.form, estado.servicios)
        if (errores.isNotEmpty()) {
            intentoEnviar = true
            _uiState.update { recalcular(it).copy(mensaje = "Revisa los ${errores.size} datos marcados en rojo") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(enviando = true) }
            consultas.enviar(emprendimiento, estado.form)
            _uiState.update { it.copy(enviando = false, enviada = true) }
        }
    }

    fun nuevaConsulta() {
        tocados = emptySet()
        intentoEnviar = false
        _uiState.update {
            it.copy(
                form = it.form.copy(servicio = "", mensaje = ""),
                errores = emptyMap(),
                validos = emptySet(),
                enviada = false
            )
        }
    }

    fun mensajeMostrado() {
        _uiState.update { it.copy(mensaje = null) }
    }

    private fun actualizar(campo: CampoConsulta, cambio: (ConsultaForm) -> ConsultaForm) {
        tocados = tocados + campo
        _uiState.update { recalcular(it.copy(form = cambio(it.form))) }
    }

    private fun recalcular(estado: ConsultaFormUiState): ConsultaFormUiState {
        val todos = ConsultaValidator.validarTodo(estado.form, estado.servicios)
        return estado.copy(
            errores = todos.filterKeys { intentoEnviar || it in tocados },
            validos = CampoConsulta.entries.filter { it in tocados && it !in todos }.toSet()
        )
    }

    companion object {
        const val SERVICIO_GENERAL = "Otro / consulta general"
    }
}
