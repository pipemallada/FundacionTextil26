package cl.duoc.redtextil.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.redtextil.data.repository.EmprendimientoRepository
import cl.duoc.redtextil.data.repository.ResultadoRegistro
import cl.duoc.redtextil.data.repository.SesionRepository
import cl.duoc.redtextil.domain.model.CampoRegistro
import cl.duoc.redtextil.domain.model.RegistroForm
import cl.duoc.redtextil.domain.validation.RegistroValidator
import cl.duoc.redtextil.util.ChileGeoData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegistroUiState(
    val form: RegistroForm = RegistroForm(),
    val errores: Map<CampoRegistro, String> = emptyMap(),
    val validos: Set<CampoRegistro> = emptySet(),
    val comunas: List<String> = emptyList(),
    val guardando: Boolean = false,
    val registrado: Boolean = false,
    val mensaje: String? = null
)

class RegistroViewModel(
    private val emprendimientos: EmprendimientoRepository,
    private val sesion: SesionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistroUiState())
    val uiState: StateFlow<RegistroUiState> = _uiState.asStateFlow()

    private var tocados: Set<CampoRegistro> = emptySet()
    private var intentoEnviar = false

    fun onNombre(valor: String) = actualizar(CampoRegistro.NOMBRE_EMPRENDEDORA) { it.copy(nombreEmprendedora = valor) }
    fun onNombreComercial(valor: String) = actualizar(CampoRegistro.NOMBRE_COMERCIAL) { it.copy(nombreComercial = valor) }
    fun onRut(valor: String) = actualizar(CampoRegistro.RUT) { it.copy(rut = valor) }
    fun onCorreo(valor: String) = actualizar(CampoRegistro.CORREO) { it.copy(correo = valor) }
    fun onTelefono(valor: String) = actualizar(CampoRegistro.TELEFONO) { it.copy(telefono = valor) }
    fun onComuna(valor: String) = actualizar(CampoRegistro.COMUNA) { it.copy(comuna = valor) }
    fun onPrivacidad(valor: Boolean) = actualizar(CampoRegistro.ACEPTA_PRIVACIDAD) { it.copy(aceptaPrivacidad = valor) }

    // Limpiamos la comuna al cambiar de region para evitar fichas invisibles
    fun onRegion(valor: String) = actualizar(CampoRegistro.REGION) { it.copy(region = valor, comuna = "") }

    private fun actualizar(campo: CampoRegistro, cambio: (RegistroForm) -> RegistroForm) {
        tocados = tocados + campo
        _uiState.update { recalcular(it.copy(form = cambio(it.form))) }
    }

    private fun recalcular(estado: RegistroUiState): RegistroUiState {
        val errores = RegistroValidator.validarTodo(estado.form)
        val visibles = errores.filterKeys { intentoEnviar || it in tocados }
        val validos = CampoRegistro.entries.filter { it in tocados && it !in errores }.toSet()
        return estado.copy(
            errores = visibles,
            validos = validos,
            comunas = ChileGeoData.comunasDe(estado.form.region)
        )
    }

    fun registrar() {
        val form = _uiState.value.form
        val errores = RegistroValidator.validarTodo(form)
        if (errores.isNotEmpty()) {
            intentoEnviar = true
            _uiState.update { recalcular(it).copy(mensaje = "Revisa los ${errores.size} datos marcados en rojo") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true) }
            when (val resultado = emprendimientos.registrar(form)) {
                is ResultadoRegistro.Exito -> {
                    sesion.iniciarSesion(resultado.id)
                    _uiState.update { it.copy(guardando = false, registrado = true) }
                }
                is ResultadoRegistro.Error -> _uiState.update { it.copy(guardando = false, mensaje = resultado.mensaje) }
            }
        }
    }

    fun mensajeMostrado() {
        _uiState.update { it.copy(mensaje = null) }
    }
}
