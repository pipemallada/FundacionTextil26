package cl.duoc.redtextil.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.redtextil.data.repository.EmprendimientoRepository
import cl.duoc.redtextil.data.repository.SesionRepository
import cl.duoc.redtextil.domain.model.CampoFicha
import cl.duoc.redtextil.domain.model.EstadoFicha
import cl.duoc.redtextil.domain.model.EstadoSync
import cl.duoc.redtextil.domain.model.FichaDatos
import cl.duoc.redtextil.domain.model.Rubro
import cl.duoc.redtextil.domain.model.SeccionFicha
import cl.duoc.redtextil.domain.validation.FichaValidator
import cl.duoc.redtextil.util.ChileGeoData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FichaUiState(
    val cargando: Boolean = true,
    val sinSesion: Boolean = false,
    val emprendimientoId: Long? = null,
    val datos: FichaDatos = FichaDatos(),
    val errores: Map<CampoFicha, String> = emptyMap(),
    val validos: Set<CampoFicha> = emptySet(),
    val visibles: Set<CampoFicha> = emptySet(),
    val comunas: List<String> = emptyList(),
    val rubrosBloqueados: Set<Rubro> = emptySet(),
    val seccionesAbiertas: Set<SeccionFicha> = setOf(SeccionFicha.PERSONALES),
    val totalPorSeccion: Map<SeccionFicha, Int> = emptyMap(),
    val completosPorSeccion: Map<SeccionFicha, Int> = emptyMap(),
    val erroresPorSeccion: Map<SeccionFicha, Int> = emptyMap(),
    val camposCompletos: Int = 0,
    val camposAplicables: Int = FichaValidator.TOTAL_CAMPOS,
    val estadoSync: EstadoSync = EstadoSync.SINCRONIZADO,
    val estadoFicha: EstadoFicha = EstadoFicha.PENDIENTE,
    val observacion: String = "",
    val guardando: Boolean = false,
    val mensaje: String? = null
)

class FichaViewModel(
    sesion: SesionRepository,
    private val emprendimientos: EmprendimientoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FichaUiState())
    val uiState: StateFlow<FichaUiState> = _uiState.asStateFlow()

    private var tocados: Set<CampoFicha> = emptySet()
    private var intentoGuardar = false

    init {
        viewModelScope.launch {
            sesion.emprendimientoActual.collect { actual ->
                if (actual == null) {
                    _uiState.value = FichaUiState(cargando = false, sinSesion = true)
                    return@collect
                }
                if (_uiState.value.emprendimientoId != actual.id) {
                    tocados = emptySet()
                    intentoGuardar = false
                    _uiState.value = recalcular(
                        FichaUiState(
                            cargando = false,
                            emprendimientoId = actual.id,
                            datos = actual.aFichaDatos(),
                            estadoSync = actual.estadoSync,
                            estadoFicha = actual.estado,
                            observacion = actual.observacion
                        )
                    )
                } else {
                    _uiState.update {
                        it.copy(estadoSync = actual.estadoSync, estadoFicha = actual.estado, observacion = actual.observacion)
                    }
                }
            }
        }
    }

    fun onTexto(campo: CampoFicha, valor: String) = actualizar(campo) { d ->
        when (campo) {
            CampoFicha.NOMBRE_EMPRENDEDORA -> d.copy(nombreEmprendedora = valor)
            CampoFicha.RUT -> d.copy(rut = valor)
            CampoFicha.FECHA_NACIMIENTO -> d.copy(fechaNacimiento = valor)
            CampoFicha.NACIONALIDAD -> d.copy(nacionalidad = valor)
            CampoFicha.CORREO -> d.copy(correo = valor)
            CampoFicha.TELEFONO -> d.copy(telefono = valor)
            CampoFicha.JEFA_HOGAR -> d.copy(jefaHogar = valor)
            CampoFicha.NOMBRE_COMERCIAL -> d.copy(nombreComercial = valor)
            CampoFicha.DESCRIPCION -> d.copy(descripcion = valor)
            CampoFicha.ANIO_INICIO -> d.copy(anioInicio = valor)
            CampoFicha.LUGAR_TRABAJO -> d.copy(lugarTrabajo = valor)
            CampoFicha.CANTIDAD_PERSONAS -> d.copy(cantidadPersonas = valor)
            CampoFicha.RUT_EMPRESA -> d.copy(rutEmpresa = valor)
            CampoFicha.TIPO_FORMALIZACION -> d.copy(tipoFormalizacion = valor)
            CampoFicha.COMUNA -> d.copy(comuna = valor)
            CampoFicha.DIRECCION_TALLER -> d.copy(direccionTaller = valor)
            CampoFicha.TECNICAS -> d.copy(tecnicas = valor)
            CampoFicha.TIPOS_DONACION -> d.copy(tiposDonacion = valor)
            CampoFicha.CUPOS_TALLER -> d.copy(cuposTaller = valor)
            CampoFicha.MODALIDAD_TALLER -> d.copy(modalidadTaller = valor)
            CampoFicha.TALLAS_ARRIENDO -> d.copy(tallasArriendo = valor)
            CampoFicha.WHATSAPP -> d.copy(whatsapp = valor)
            CampoFicha.INSTAGRAM -> d.copy(instagram = valor)
            CampoFicha.SITIO_WEB -> d.copy(sitioWeb = valor)
            CampoFicha.HORARIO_ATENCION -> d.copy(horarioAtencion = valor)
            else -> d
        }
    }

    fun onSwitch(campo: CampoFicha, valor: Boolean) = actualizar(campo) { d ->
        val nuevo = when (campo) {
            CampoFicha.FORMALIZADO -> d.copy(formalizado = valor)
            CampoFicha.ATENCION_PRESENCIAL -> d.copy(atencionPresencial = valor)
            CampoFicha.REALIZA_ENVIOS -> d.copy(realizaEnvios = valor)
            CampoFicha.RECIBE_DONACIONES -> d.copy(recibeDonaciones = valor)
            CampoFicha.ACEPTA_PUBLICACION -> d.copy(aceptaPublicacion = valor)
            else -> d
        }
        // Al apagar un "si" se borran las respuestas que dependian de el (salto condicional)
        FichaValidator.limpiarCondicionales(nuevo)
    }

    // Limpiamos la comuna al cambiar de region para evitar fichas invisibles en el directorio
    fun onRegion(region: String) = actualizar(CampoFicha.REGION) { d ->
        d.copy(region = region, comuna = "")
    }

    fun onRubro(rubro: Rubro) {
        val actuales = _uiState.value.datos.rubros
        if (!FichaValidator.puedeAgregarRubro(actuales, rubro)) {
            _uiState.update { it.copy(mensaje = "Puedes elegir máximo ${FichaValidator.MAX_RUBROS} rubros. Quita uno para elegir otro.") }
            return
        }
        actualizar(CampoFicha.RUBROS) { d ->
            val nuevos = if (rubro in d.rubros) d.rubros - rubro else d.rubros + rubro
            FichaValidator.limpiarCondicionales(d.copy(rubros = nuevos))
        }
    }

    fun alternarSeccion(seccion: SeccionFicha) {
        _uiState.update {
            val abiertas = if (seccion in it.seccionesAbiertas) it.seccionesAbiertas - seccion else it.seccionesAbiertas + seccion
            it.copy(seccionesAbiertas = abiertas)
        }
    }

    fun guardar() {
        val actual = _uiState.value
        val id = actual.emprendimientoId ?: return
        val errores = FichaValidator.validarTodo(actual.datos)
        if (errores.isNotEmpty()) {
            intentoGuardar = true
            val conError = errores.keys.map { it.seccion }.toSet()
            _uiState.update {
                recalcular(it).copy(
                    seccionesAbiertas = it.seccionesAbiertas + conError,
                    mensaje = "Faltan ${errores.size} datos por corregir. Están marcados en rojo."
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true) }
            val guardada = emprendimientos.guardarFicha(id, actual.datos)
            intentoGuardar = false
            tocados = emptySet()
            _uiState.update { estado ->
                recalcular(
                    estado.copy(
                        guardando = false,
                        datos = guardada?.aFichaDatos() ?: estado.datos,
                        estadoSync = guardada?.estadoSync ?: estado.estadoSync,
                        estadoFicha = guardada?.estado ?: estado.estadoFicha,
                        mensaje = if (guardada?.estadoSync == EstadoSync.SINCRONIZADO) {
                            "Ficha guardada y enviada al servidor"
                        } else {
                            "Ficha guardada en el teléfono. Se enviará cuando haya señal."
                        }
                    )
                )
            }
        }
    }

    fun mensajeMostrado() {
        _uiState.update { it.copy(mensaje = null) }
    }

    private fun actualizar(campo: CampoFicha, cambio: (FichaDatos) -> FichaDatos) {
        tocados = tocados + campo
        _uiState.update { recalcular(it.copy(datos = cambio(it.datos))) }
    }

    private fun recalcular(estado: FichaUiState): FichaUiState {
        val datos = estado.datos
        val todos = FichaValidator.validarTodo(datos)
        val visibles = FichaValidator.camposAplicables(datos).toSet()
        val errores = todos.filterKeys { intentoGuardar || it in tocados }
        val validos = visibles.filter { it !in todos && tieneValor(it, datos) }.toSet()
        val bloqueados = Rubro.entries.filter { !FichaValidator.puedeAgregarRubro(datos.rubros, it) }.toSet()
        return estado.copy(
            errores = errores,
            validos = validos,
            visibles = visibles,
            comunas = ChileGeoData.comunasDe(datos.region),
            rubrosBloqueados = bloqueados,
            totalPorSeccion = SeccionFicha.entries.associateWith { s -> visibles.count { it.seccion == s } },
            completosPorSeccion = SeccionFicha.entries.associateWith { s -> visibles.count { it.seccion == s && it !in todos } },
            erroresPorSeccion = SeccionFicha.entries.associateWith { s -> errores.keys.count { it.seccion == s } },
            camposCompletos = visibles.count { it !in todos },
            camposAplicables = visibles.size
        )
    }

    private fun tieneValor(campo: CampoFicha, d: FichaDatos): Boolean = when (campo) {
        CampoFicha.NOMBRE_EMPRENDEDORA -> d.nombreEmprendedora.isNotBlank()
        CampoFicha.RUT -> d.rut.isNotBlank()
        CampoFicha.FECHA_NACIMIENTO -> d.fechaNacimiento.isNotBlank()
        CampoFicha.NACIONALIDAD -> d.nacionalidad.isNotBlank()
        CampoFicha.CORREO -> d.correo.isNotBlank()
        CampoFicha.TELEFONO -> d.telefono.isNotBlank()
        CampoFicha.JEFA_HOGAR -> d.jefaHogar.isNotBlank()
        CampoFicha.NOMBRE_COMERCIAL -> d.nombreComercial.isNotBlank()
        CampoFicha.DESCRIPCION -> d.descripcion.isNotBlank()
        CampoFicha.ANIO_INICIO -> d.anioInicio.isNotBlank()
        CampoFicha.LUGAR_TRABAJO -> d.lugarTrabajo.isNotBlank()
        CampoFicha.CANTIDAD_PERSONAS -> d.cantidadPersonas.isNotBlank()
        CampoFicha.RUT_EMPRESA -> d.rutEmpresa.isNotBlank()
        CampoFicha.TIPO_FORMALIZACION -> d.tipoFormalizacion.isNotBlank()
        CampoFicha.REGION -> d.region.isNotBlank()
        CampoFicha.COMUNA -> d.comuna.isNotBlank()
        CampoFicha.DIRECCION_TALLER -> d.direccionTaller.isNotBlank()
        CampoFicha.RUBROS -> d.rubros.isNotEmpty()
        CampoFicha.TECNICAS -> d.tecnicas.isNotBlank()
        CampoFicha.TIPOS_DONACION -> d.tiposDonacion.isNotBlank()
        CampoFicha.CUPOS_TALLER -> d.cuposTaller.isNotBlank()
        CampoFicha.MODALIDAD_TALLER -> d.modalidadTaller.isNotBlank()
        CampoFicha.TALLAS_ARRIENDO -> d.tallasArriendo.isNotBlank()
        CampoFicha.WHATSAPP -> d.whatsapp.isNotBlank()
        CampoFicha.INSTAGRAM -> d.instagram.isNotBlank()
        CampoFicha.SITIO_WEB -> d.sitioWeb.isNotBlank()
        CampoFicha.HORARIO_ATENCION -> d.horarioAtencion.isNotBlank()
        CampoFicha.ACEPTA_PUBLICACION -> d.aceptaPublicacion
        CampoFicha.FORMALIZADO, CampoFicha.ATENCION_PRESENCIAL,
        CampoFicha.REALIZA_ENVIOS, CampoFicha.RECIBE_DONACIONES -> true
    }
}
