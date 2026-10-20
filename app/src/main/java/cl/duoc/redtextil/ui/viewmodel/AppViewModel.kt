package cl.duoc.redtextil.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.redtextil.data.local.PreferenciasLocales
import cl.duoc.redtextil.data.repository.ConsultaRepository
import cl.duoc.redtextil.data.repository.SesionRepository
import cl.duoc.redtextil.data.repository.SincronizacionRepository
import cl.duoc.redtextil.ui.navigation.ModoApp
import cl.duoc.redtextil.util.NetworkMonitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppUiState(
    val enLinea: Boolean = true,
    val pendientes: Int = 0,
    val sincronizando: Boolean = false,
    val forzarOffline: Boolean = false,
    val servidorDemo: Boolean = true,
    val consultasPendientes: Int = 0
)

class AppViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val preferencias: PreferenciasLocales,
    private val red: NetworkMonitor,
    private val sincronizacion: SincronizacionRepository,
    sesion: SesionRepository,
    consultas: ConsultaRepository
) : ViewModel() {

    val modo: StateFlow<ModoApp> = savedStateHandle.getStateFlow(CLAVE_MODO, ModoApp.EMPRENDEDORA)

    private val sincronizando = MutableStateFlow(false)

    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val mensajes: SharedFlow<String> = _mensajes.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val consultasPendientes = sesion.sesionId.flatMapLatest { id ->
        if (id == null) flowOf(0) else consultas.contarPendientesDe(id)
    }

    val uiState: StateFlow<AppUiState> = combine(
        red.enLinea,
        sincronizacion.pendientesTotales,
        sincronizando,
        preferencias.forzarOffline,
        preferencias.servidorDemo
    ) { enLinea, pendientes, sync, forzado, demo ->
        AppUiState(enLinea, pendientes, sync, forzado, demo)
    }.combine(consultasPendientes) { estado, porResponder ->
        estado.copy(consultasPendientes = porResponder)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    init {
        // Si la app se cerro sin senal y ahora hay conexion, se envia lo pendiente al abrirla
        viewModelScope.launch {
            if (red.enLinea.value && sincronizacion.pendientesTotales.first() > 0) sincronizar(automatica = true)
        }
        // Sincronizacion diferida: apenas vuelve la senal se envian solos los cambios guardados offline
        viewModelScope.launch {
            red.enLinea.drop(1).filter { it }.collect { sincronizar(automatica = true) }
        }
    }

    fun cambiarModo(nuevo: ModoApp) {
        savedStateHandle[CLAVE_MODO] = nuevo
    }

    fun sincronizar(automatica: Boolean = false) {
        if (sincronizando.value) return
        viewModelScope.launch {
            sincronizando.value = true
            val resultado = sincronizacion.sincronizarTodo()
            sincronizando.value = false
            if (!automatica || resultado.enviados > 0 || !resultado.exito) {
                _mensajes.emit(resultado.mensaje)
            }
        }
    }

    fun cambiarForzarOffline(valor: Boolean) {
        preferencias.cambiarForzarOffline(valor)
        viewModelScope.launch {
            _mensajes.emit(
                if (valor) "Simulando zona sin señal: todo se guardará en el teléfono"
                else "Señal restablecida: enviando cambios pendientes"
            )
        }
    }

    fun cambiarServidorDemo(valor: Boolean) {
        preferencias.cambiarServidorDemo(valor)
        viewModelScope.launch {
            _mensajes.emit(
                if (valor) "Usando servidor de demostración"
                else "Usando API Spring Boot en http://10.0.2.2:8080/api/"
            )
        }
    }

    companion object {
        private const val CLAVE_MODO = "modo_app"
    }
}
