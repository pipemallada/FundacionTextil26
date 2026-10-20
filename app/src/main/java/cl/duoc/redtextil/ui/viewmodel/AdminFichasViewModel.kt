package cl.duoc.redtextil.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.redtextil.data.local.entity.EmprendimientoEntity
import cl.duoc.redtextil.data.repository.EmprendimientoRepository
import cl.duoc.redtextil.data.repository.HistoricoRepository
import cl.duoc.redtextil.data.repository.ResultadoImportacion
import cl.duoc.redtextil.domain.model.EstadoFicha
import cl.duoc.redtextil.domain.validation.FichaValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FichaAdmin(
    val emprendimiento: EmprendimientoEntity,
    val problemas: List<String>,
    val completos: Int,
    val aplicables: Int
)

data class FiltrosAdmin(
    val estado: EstadoFicha? = null,
    val texto: String = "",
    val soloConProblemas: Boolean = false
)

data class EstadoInterfazAdmin(
    val importando: Boolean = false,
    val procesadas: Int = 0,
    val totalImportar: Int = 0,
    val resultado: ResultadoImportacion? = null,
    val fichaObservar: EmprendimientoEntity? = null,
    val textoObservacion: String = "",
    val errorObservacion: String? = null,
    val procesandoId: Long? = null,
    val mensaje: String? = null
)

data class AdminFichasUiState(
    val cargando: Boolean = true,
    val filtros: FiltrosAdmin = FiltrosAdmin(),
    val fichas: List<FichaAdmin> = emptyList(),
    val conteos: Map<EstadoFicha, Int> = emptyMap(),
    val historicosIntegrados: Int = 0,
    val interfaz: EstadoInterfazAdmin = EstadoInterfazAdmin()
) {
    val totalFichas: Int get() = conteos.values.sum()
    val progresoImportacion: Float
        get() = if (interfaz.totalImportar == 0) 0f else interfaz.procesadas.toFloat() / interfaz.totalImportar
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class AdminFichasViewModel(
    private val emprendimientos: EmprendimientoRepository,
    private val historico: HistoricoRepository
) : ViewModel() {

    private val filtros = MutableStateFlow(FiltrosAdmin())
    private val interfaz = MutableStateFlow(EstadoInterfazAdmin())

    private val fichas = filtros.debounce(200).flatMapLatest { f ->
        emprendimientos.observarParaAdmin(f.estado, f.texto).map { lista ->
            val revisadas = lista.map { e ->
                val datos = e.aFichaDatos()
                FichaAdmin(
                    emprendimiento = e,
                    problemas = FichaValidator.revisarConsistencia(datos),
                    completos = FichaValidator.contarCompletos(datos),
                    aplicables = FichaValidator.camposAplicables(datos).size
                )
            }
            if (f.soloConProblemas) revisadas.filter { it.problemas.isNotEmpty() } else revisadas
        }
    }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<AdminFichasUiState> = combine(
        filtros,
        fichas,
        emprendimientos.contarPorEstado(),
        historico.totalIntegrados(),
        interfaz
    ) { f, lista, conteos, historicos, ui ->
        AdminFichasUiState(
            cargando = false,
            filtros = f,
            fichas = lista,
            conteos = conteos,
            historicosIntegrados = historicos,
            interfaz = ui
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminFichasUiState())

    fun filtrarEstado(estado: EstadoFicha?) = filtros.update { it.copy(estado = estado) }

    fun onTexto(texto: String) = filtros.update { it.copy(texto = texto) }

    fun alternarSoloProblemas() = filtros.update { it.copy(soloConProblemas = !it.soloConProblemas) }

    fun aprobar(ficha: EmprendimientoEntity) =
        cambiarEstado(ficha, EstadoFicha.ACTIVA, "", "aprobada y publicada. Se envió el correo de aprobación")

    fun darDeBaja(ficha: EmprendimientoEntity) =
        cambiarEstado(ficha, EstadoFicha.BAJA, "", "quedó de baja y ya no aparece en el directorio")

    fun abrirObservacion(ficha: EmprendimientoEntity) = interfaz.update {
        it.copy(fichaObservar = ficha, textoObservacion = ficha.observacion, errorObservacion = null)
    }

    fun onTextoObservacion(texto: String) = interfaz.update {
        it.copy(
            textoObservacion = texto,
            errorObservacion = if (it.errorObservacion != null) FichaValidator.validarObservacion(texto) else null
        )
    }

    fun cerrarObservacion() = interfaz.update { it.copy(fichaObservar = null, errorObservacion = null) }

    fun confirmarObservacion() {
        val ui = interfaz.value
        val ficha = ui.fichaObservar ?: return
        val error = FichaValidator.validarObservacion(ui.textoObservacion)
        if (error != null) {
            interfaz.update { it.copy(errorObservacion = error) }
            return
        }
        interfaz.update { it.copy(fichaObservar = null) }
        cambiarEstado(ficha, EstadoFicha.OBSERVADA, ui.textoObservacion, "quedó observada. Se avisó por correo a la emprendedora")
    }

    private fun cambiarEstado(ficha: EmprendimientoEntity, estado: EstadoFicha, observacion: String, texto: String) {
        viewModelScope.launch {
            interfaz.update { it.copy(procesandoId = ficha.id) }
            emprendimientos.cambiarEstado(ficha.id, estado, observacion)
            interfaz.update { it.copy(procesandoId = null, mensaje = "${ficha.nombreComercial} $texto") }
        }
    }

    fun integrarDatasetHistorico() {
        if (interfaz.value.importando) return
        viewModelScope.launch {
            interfaz.update { it.copy(importando = true, resultado = null, procesadas = 0, totalImportar = 0) }
            val resultado = runCatching {
                historico.integrarDataset { procesadas, total ->
                    interfaz.update { it.copy(procesadas = procesadas, totalImportar = total) }
                }
            }
            interfaz.update { actual ->
                resultado.fold(
                    onSuccess = { r ->
                        actual.copy(
                            importando = false,
                            resultado = r,
                            mensaje = if (r.integradas > 0) {
                                "Se integraron ${r.integradas} fichas históricas sin re-registro"
                            } else {
                                "El dataset ya estaba integrado (${r.yaExistian} fichas)"
                            }
                        )
                    },
                    onFailure = { actual.copy(importando = false, mensaje = "No se pudo leer el dataset histórico") }
                )
            }
        }
    }

    fun mensajeMostrado() = interfaz.update { it.copy(mensaje = null) }
}
