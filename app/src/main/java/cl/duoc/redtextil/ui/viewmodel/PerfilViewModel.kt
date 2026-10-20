package cl.duoc.redtextil.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.redtextil.data.local.entity.EmprendimientoEntity
import cl.duoc.redtextil.data.repository.CatalogoRepository
import cl.duoc.redtextil.data.repository.ConsultaRepository
import cl.duoc.redtextil.data.repository.EmprendimientoRepository
import cl.duoc.redtextil.data.repository.SesionRepository
import cl.duoc.redtextil.domain.model.EstadoFicha
import cl.duoc.redtextil.domain.validation.FichaValidator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PerfilUiState(
    val cargando: Boolean = true,
    val sesion: EmprendimientoEntity? = null,
    val cuentas: List<EmprendimientoEntity> = emptyList(),
    val totalPublicaciones: Int = 0,
    val consultasPendientes: Int = 0,
    val camposCompletos: Int = 0,
    val camposAplicables: Int = FichaValidator.TOTAL_CAMPOS,
    val problemas: List<String> = emptyList(),
    val procesando: Boolean = false,
    val mensaje: String? = null
)

private data class ResumenSesion(
    val emprendimiento: EmprendimientoEntity?,
    val publicaciones: Int,
    val pendientes: Int
)

class PerfilViewModel(
    private val sesion: SesionRepository,
    private val emprendimientos: EmprendimientoRepository,
    catalogo: CatalogoRepository,
    consultas: ConsultaRepository
) : ViewModel() {

    private val procesando = MutableStateFlow(false)
    private val mensaje = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val resumen = sesion.emprendimientoActual.flatMapLatest { actual ->
        if (actual == null) {
            flowOf(ResumenSesion(null, 0, 0))
        } else {
            combine(catalogo.contarDe(actual.id), consultas.contarPendientesDe(actual.id)) { publicaciones, pendientes ->
                ResumenSesion(actual, publicaciones, pendientes)
            }
        }
    }

    val uiState: StateFlow<PerfilUiState> = combine(
        resumen,
        emprendimientos.observarCuentasLocales(),
        procesando,
        mensaje
    ) { r, cuentas, ocupado, texto ->
        val datos = r.emprendimiento?.aFichaDatos()
        PerfilUiState(
            cargando = false,
            sesion = r.emprendimiento,
            cuentas = cuentas,
            totalPublicaciones = r.publicaciones,
            consultasPendientes = r.pendientes,
            camposCompletos = datos?.let { FichaValidator.contarCompletos(it) } ?: 0,
            camposAplicables = datos?.let { FichaValidator.camposAplicables(it).size } ?: FichaValidator.TOTAL_CAMPOS,
            problemas = datos?.let { FichaValidator.revisarConsistencia(it) } ?: emptyList(),
            procesando = ocupado,
            mensaje = texto
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PerfilUiState())

    fun iniciarSesion(cuenta: EmprendimientoEntity) {
        sesion.iniciarSesion(cuenta.id)
        mensaje.value = "Hola ${cuenta.nombreEmprendedora.substringBefore(" ")}, ingresaste a ${cuenta.nombreComercial}"
    }

    fun cerrarSesion() {
        sesion.cerrarSesion()
        mensaje.value = "Cerraste sesión"
    }

    fun pausarFicha() = cambiarEstado(EstadoFicha.BAJA, "Tu ficha quedó pausada y ya no aparece en el directorio")

    fun reactivarFicha() = cambiarEstado(EstadoFicha.PENDIENTE, "Tu ficha volvió a revisión de la administradora")

    private fun cambiarEstado(estado: EstadoFicha, texto: String) {
        val actual = uiState.value.sesion ?: return
        viewModelScope.launch {
            procesando.value = true
            emprendimientos.cambiarEstado(actual.id, estado)
            procesando.value = false
            mensaje.value = texto
        }
    }

    fun mensajeMostrado() {
        mensaje.value = null
    }
}
