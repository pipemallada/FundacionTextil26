package cl.duoc.redtextil.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.redtextil.data.repository.CatalogoRepository
import cl.duoc.redtextil.data.repository.SesionRepository
import cl.duoc.redtextil.domain.model.CampoPublicacion
import cl.duoc.redtextil.domain.model.CategoriaTrabajo
import cl.duoc.redtextil.domain.model.PublicacionForm
import cl.duoc.redtextil.domain.validation.CatalogoValidator
import cl.duoc.redtextil.ui.navigation.Rutas
import cl.duoc.redtextil.util.ImageCompressor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PublicacionFormUiState(
    val cargando: Boolean = true,
    val esEdicion: Boolean = false,
    val form: PublicacionForm = PublicacionForm(),
    val errores: Map<CampoPublicacion, String> = emptyMap(),
    val validos: Set<CampoPublicacion> = emptySet(),
    val procesandoFoto: Boolean = false,
    val kbOriginal: Long = 0,
    val kbOptimizado: Long = 0,
    val dimensiones: String = "",
    val fotoNueva: Boolean = false,
    val guardando: Boolean = false,
    val guardado: Boolean = false,
    val mensaje: String? = null
) {
    val porcentajeAhorro: Int
        get() = if (kbOriginal <= 0) 0 else (100 - (kbOptimizado * 100 / kbOriginal)).toInt().coerceIn(0, 100)
}

class PublicacionFormViewModel(
    savedStateHandle: SavedStateHandle,
    private val sesion: SesionRepository,
    private val catalogo: CatalogoRepository,
    private val compresor: ImageCompressor
) : ViewModel() {

    private val idPublicacion: Long? = savedStateHandle.get<Long>(Rutas.ARG_ID)?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(PublicacionFormUiState(esEdicion = idPublicacion != null))
    val uiState: StateFlow<PublicacionFormUiState> = _uiState.asStateFlow()

    private var tocados: Set<CampoPublicacion> = emptySet()
    private var intentoGuardar = false
    private val fotosSinGuardar = mutableListOf<String>()

    init {
        viewModelScope.launch {
            val existente = idPublicacion?.let { catalogo.obtener(it) }
            _uiState.update { estado ->
                if (existente == null) {
                    estado.copy(cargando = false)
                } else {
                    tocados = CampoPublicacion.entries.toSet()
                    recalcular(
                        estado.copy(
                            cargando = false,
                            form = PublicacionForm(
                                titulo = existente.titulo,
                                categoria = existente.categoria,
                                tecnica = existente.tecnica,
                                precio = existente.precioReferencial.toString(),
                                descripcion = existente.descripcion,
                                fotoRuta = existente.fotoRuta
                            ),
                            kbOriginal = existente.kbOriginal,
                            kbOptimizado = existente.kbOptimizado
                        )
                    )
                }
            }
        }
    }

    fun crearUriCamara(): Uri = compresor.crearUriCamara()

    fun procesarFoto(uri: Uri, desdeCamara: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(procesandoFoto = true) }
            val resultado = runCatching { compresor.comprimir(uri) }
            if (desdeCamara) compresor.eliminarCaptura(uri)
            resultado.onSuccess { r ->
                fotosSinGuardar.add(r.ruta)
                tocados = tocados + CampoPublicacion.FOTO
                _uiState.update {
                    recalcular(
                        it.copy(
                            procesandoFoto = false,
                            form = it.form.copy(fotoRuta = r.ruta),
                            kbOriginal = r.kbOriginal,
                            kbOptimizado = r.kbOptimizado,
                            dimensiones = "${r.ancho} x ${r.alto} px",
                            fotoNueva = true
                        )
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(procesandoFoto = false, mensaje = "No pudimos procesar la foto. Prueba con otra imagen.")
                }
            }
        }
    }

    fun permisoCamaraDenegado() {
        _uiState.update {
            it.copy(mensaje = "Sin permiso de cámara no podemos tomar la foto. Puedes elegirla desde la galería.")
        }
    }

    fun onTitulo(valor: String) = actualizar(CampoPublicacion.TITULO) { it.copy(titulo = valor) }
    fun onCategoria(etiqueta: String) = actualizar(CampoPublicacion.CATEGORIA) {
        it.copy(categoria = CategoriaTrabajo.desdeEtiqueta(etiqueta))
    }
    fun onTecnica(valor: String) = actualizar(CampoPublicacion.TECNICA) { it.copy(tecnica = valor) }
    fun onPrecio(valor: String) = actualizar(CampoPublicacion.PRECIO) { it.copy(precio = valor) }
    fun onDescripcion(valor: String) = actualizar(CampoPublicacion.DESCRIPCION) { it.copy(descripcion = valor) }

    fun guardar() {
        val estado = _uiState.value
        val errores = CatalogoValidator.validarTodo(estado.form)
        if (errores.isNotEmpty()) {
            intentoGuardar = true
            _uiState.update { recalcular(it).copy(mensaje = "Revisa los ${errores.size} datos marcados en rojo") }
            return
        }
        val emprendimientoId = sesion.sesionId.value
        if (emprendimientoId == null) {
            _uiState.update { it.copy(mensaje = "Primero inicia sesión en Mi perfil") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true) }
            catalogo.guardar(emprendimientoId, idPublicacion, estado.form, estado.kbOriginal, estado.kbOptimizado)
            fotosSinGuardar.remove(estado.form.fotoRuta)
            fotosSinGuardar.forEach { compresor.eliminarArchivo(it) }
            fotosSinGuardar.clear()
            _uiState.update { it.copy(guardando = false, guardado = true) }
        }
    }

    fun mensajeMostrado() {
        _uiState.update { it.copy(mensaje = null) }
    }

    private fun actualizar(campo: CampoPublicacion, cambio: (PublicacionForm) -> PublicacionForm) {
        tocados = tocados + campo
        _uiState.update { recalcular(it.copy(form = cambio(it.form))) }
    }

    private fun recalcular(estado: PublicacionFormUiState): PublicacionFormUiState {
        val todos = CatalogoValidator.validarTodo(estado.form)
        return estado.copy(
            errores = todos.filterKeys { intentoGuardar || it in tocados },
            validos = CampoPublicacion.entries.filter { it in tocados && it !in todos }.toSet()
        )
    }

    // Si la usuaria sale sin guardar, borramos las fotos comprimidas que quedaron sueltas
    override fun onCleared() {
        fotosSinGuardar.forEach { compresor.eliminarArchivo(it) }
        super.onCleared()
    }
}
