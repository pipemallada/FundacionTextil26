package cl.duoc.redtextil.domain.validation

import cl.duoc.redtextil.domain.model.CampoFicha
import cl.duoc.redtextil.domain.model.FichaDatos
import cl.duoc.redtextil.domain.model.OpcionesFicha
import cl.duoc.redtextil.domain.model.Rubro
import cl.duoc.redtextil.domain.model.SeccionFicha
import cl.duoc.redtextil.util.ChileGeoData

object FichaValidator {

    const val MIN_RUBROS = 1
    const val MAX_RUBROS = 2
    const val TOTAL_CAMPOS = 32

    private val regexDireccion = Regex(".*\\d.*")

    fun muestraDonaciones(rubros: Set<Rubro>): Boolean =
        Rubro.SUPRARRECICLAJE in rubros || Rubro.SEGUNDA_MANO in rubros

    fun muestraTalleres(rubros: Set<Rubro>): Boolean = Rubro.TALLERES in rubros

    fun muestraArriendo(rubros: Set<Rubro>): Boolean = Rubro.ARRIENDO in rubros

    // Saltos condicionales del formulario de la fundacion: estas preguntas solo aparecen segun lo que se responde antes
    fun campoVisible(campo: CampoFicha, datos: FichaDatos): Boolean = when (campo) {
        CampoFicha.RUT_EMPRESA, CampoFicha.TIPO_FORMALIZACION -> datos.formalizado
        CampoFicha.DIRECCION_TALLER -> datos.atencionPresencial
        CampoFicha.RECIBE_DONACIONES -> muestraDonaciones(datos.rubros)
        CampoFicha.TIPOS_DONACION -> muestraDonaciones(datos.rubros) && datos.recibeDonaciones
        CampoFicha.CUPOS_TALLER, CampoFicha.MODALIDAD_TALLER -> muestraTalleres(datos.rubros)
        CampoFicha.TALLAS_ARRIENDO -> muestraArriendo(datos.rubros)
        else -> true
    }

    // Tope maximo de 2 rubros segun regla de la fundacion
    fun puedeAgregarRubro(actuales: Set<Rubro>, rubro: Rubro): Boolean =
        rubro in actuales || actuales.size < MAX_RUBROS

    fun validarRubros(rubros: Set<Rubro>): String? = when {
        rubros.size < MIN_RUBROS -> "Elige al menos 1 rubro"
        rubros.size > MAX_RUBROS -> "Puedes elegir máximo 2 rubros"
        else -> null
    }

    fun validarComuna(region: String, comuna: String): String? = when {
        comuna.isBlank() -> "Elige tu comuna"
        !ChileGeoData.comunaPerteneceARegion(comuna, region) -> "La comuna no pertenece a la región elegida"
        else -> null
    }

    fun validarFechaNacimiento(valor: String, hoy: FechaSimple): String? {
        if (valor.isBlank()) return "Escribe tu fecha de nacimiento"
        val fecha = FechaSimple.desdeTexto(valor) ?: return "Usa el formato día/mes/año (ej: 21/03/1985)"
        val edad = fecha.edadAl(hoy)
        return when {
            edad < 18 -> "Debes ser mayor de 18 años para inscribirte"
            edad > 100 -> "Revisa el año de nacimiento"
            else -> null
        }
    }

    fun validarCampo(campo: CampoFicha, datos: FichaDatos, hoy: FechaSimple = FechaSimple.hoy()): String? {
        // Las preguntas condicionales que no se muestran no se validan
        if (!campoVisible(campo, datos)) return null
        return when (campo) {
            CampoFicha.NOMBRE_EMPRENDEDORA -> ReglasComunes.validarNombreCompleto(datos.nombreEmprendedora)
            CampoFicha.RUT -> ReglasComunes.validarRut(datos.rut)
            CampoFicha.FECHA_NACIMIENTO -> validarFechaNacimiento(datos.fechaNacimiento, hoy)
            CampoFicha.NACIONALIDAD ->
                ReglasComunes.validarOpcion(datos.nacionalidad, OpcionesFicha.nacionalidades, "Elige tu nacionalidad")
            CampoFicha.CORREO -> ReglasComunes.validarCorreo(datos.correo)
            CampoFicha.TELEFONO -> ReglasComunes.validarTelefono(datos.telefono)
            CampoFicha.JEFA_HOGAR ->
                ReglasComunes.validarOpcion(datos.jefaHogar, OpcionesFicha.jefaHogar, "Elige una opción")
            CampoFicha.NOMBRE_COMERCIAL -> ReglasComunes.validarNombreComercial(datos.nombreComercial)
            CampoFicha.DESCRIPCION -> ReglasComunes.validarLargo(datos.descripcion, 20, 400, "una descripción")
            CampoFicha.ANIO_INICIO -> ReglasComunes.validarEntero(datos.anioInicio, 1950, hoy.anio, "el año de inicio")
            CampoFicha.LUGAR_TRABAJO ->
                ReglasComunes.validarOpcion(datos.lugarTrabajo, OpcionesFicha.lugaresTrabajo, "Elige dónde trabajas")
            CampoFicha.CANTIDAD_PERSONAS ->
                ReglasComunes.validarEntero(datos.cantidadPersonas, 1, 50, "cuántas personas trabajan")
            CampoFicha.FORMALIZADO -> null
            CampoFicha.RUT_EMPRESA -> ReglasComunes.validarRut(datos.rutEmpresa, "el RUT de la actividad")
            CampoFicha.TIPO_FORMALIZACION -> ReglasComunes.validarOpcion(
                datos.tipoFormalizacion,
                OpcionesFicha.tiposFormalizacion,
                "Elige el tipo de formalización"
            )
            CampoFicha.REGION -> if (ChileGeoData.existeRegion(datos.region)) null else "Elige tu región"
            CampoFicha.COMUNA -> validarComuna(datos.region, datos.comuna)
            CampoFicha.ATENCION_PRESENCIAL -> null
            CampoFicha.DIRECCION_TALLER -> {
                val error = ReglasComunes.validarLargo(datos.direccionTaller, 5, 100, "la dirección")
                when {
                    error != null -> error
                    !regexDireccion.matches(datos.direccionTaller) -> "Incluye la calle y el número"
                    else -> null
                }
            }
            CampoFicha.REALIZA_ENVIOS ->
                if (!datos.atencionPresencial && !datos.realizaEnvios) {
                    "Marca si atiendes presencial o haces envíos, así tus clientes saben cómo recibir tu trabajo"
                } else {
                    null
                }
            CampoFicha.RUBROS -> validarRubros(datos.rubros)
            CampoFicha.TECNICAS -> ReglasComunes.validarLargo(datos.tecnicas, 3, 120, "las técnicas que usas")
            CampoFicha.RECIBE_DONACIONES -> null
            CampoFicha.TIPOS_DONACION ->
                ReglasComunes.validarLargo(datos.tiposDonacion, 5, 150, "qué donaciones recibes")
            CampoFicha.CUPOS_TALLER -> ReglasComunes.validarEntero(datos.cuposTaller, 1, 40, "los cupos")
            CampoFicha.MODALIDAD_TALLER ->
                ReglasComunes.validarOpcion(datos.modalidadTaller, OpcionesFicha.modalidadesTaller, "Elige la modalidad")
            CampoFicha.TALLAS_ARRIENDO -> ReglasComunes.validarLargo(datos.tallasArriendo, 1, 60, "las tallas")
            CampoFicha.WHATSAPP -> ReglasComunes.validarTelefono(datos.whatsapp, "tu WhatsApp")
            CampoFicha.INSTAGRAM -> ReglasComunes.validarInstagram(datos.instagram)
            CampoFicha.SITIO_WEB -> ReglasComunes.validarUrl(datos.sitioWeb)
            CampoFicha.HORARIO_ATENCION ->
                ReglasComunes.validarLargo(datos.horarioAtencion, 5, 80, "tu horario de atención")
            CampoFicha.ACEPTA_PUBLICACION ->
                if (datos.aceptaPublicacion) null else "Debes autorizar la publicación para aparecer en el directorio"
        }
    }

    fun validarTodo(datos: FichaDatos, hoy: FechaSimple = FechaSimple.hoy()): Map<CampoFicha, String> =
        CampoFicha.entries.mapNotNull { campo -> validarCampo(campo, datos, hoy)?.let { campo to it } }.toMap()

    fun camposAplicables(datos: FichaDatos): List<CampoFicha> =
        CampoFicha.entries.filter { campoVisible(it, datos) }

    fun camposDeSeccion(seccion: SeccionFicha, datos: FichaDatos): List<CampoFicha> =
        camposAplicables(datos).filter { it.seccion == seccion }

    fun contarCompletos(datos: FichaDatos, hoy: FechaSimple = FechaSimple.hoy()): Int {
        val errores = validarTodo(datos, hoy)
        return camposAplicables(datos).count { it !in errores }
    }

    // Al desmarcar un rubro o un si/no borramos las respuestas que dependian de esa pregunta
    fun limpiarCondicionales(datos: FichaDatos): FichaDatos {
        val donaciones = muestraDonaciones(datos.rubros) && datos.recibeDonaciones
        return datos.copy(
            rutEmpresa = if (datos.formalizado) datos.rutEmpresa else "",
            tipoFormalizacion = if (datos.formalizado) datos.tipoFormalizacion else "",
            direccionTaller = if (datos.atencionPresencial) datos.direccionTaller else "",
            recibeDonaciones = donaciones,
            tiposDonacion = if (donaciones) datos.tiposDonacion else "",
            cuposTaller = if (muestraTalleres(datos.rubros)) datos.cuposTaller else "",
            modalidadTaller = if (muestraTalleres(datos.rubros)) datos.modalidadTaller else "",
            tallasArriendo = if (muestraArriendo(datos.rubros)) datos.tallasArriendo else ""
        )
    }

    // Revision rapida que usa la administradora para detectar fichas invisibles o con enlaces rotos
    fun revisarConsistencia(datos: FichaDatos): List<String> {
        val problemas = mutableListOf<String>()
        if (datos.nombreComercial.isBlank()) problemas.add("Sin nombre comercial")
        if (!ChileGeoData.existeRegion(datos.region)) {
            problemas.add("Región no reconocida")
        } else if (datos.comuna.isBlank()) {
            problemas.add("Sin comuna: no aparece al filtrar por comuna")
        } else if (!ChileGeoData.comunaPerteneceARegion(datos.comuna, datos.region)) {
            problemas.add("La comuna ${datos.comuna} no pertenece a ${datos.region}")
        }
        validarRubros(datos.rubros)?.let { problemas.add(it) }
        val whatsappOk = ReglasComunes.normalizarTelefono(datos.whatsapp) != null
        val instagramOk = ReglasComunes.normalizarInstagram(datos.instagram) != null
        if (datos.whatsapp.isNotBlank() && !whatsappOk) problemas.add("WhatsApp con formato inválido")
        if (datos.instagram.isNotBlank() && !instagramOk) problemas.add("Enlace de Instagram roto")
        if (!whatsappOk && !instagramOk) problemas.add("Sin un canal de contacto válido")
        return problemas
    }

    fun validarObservacion(texto: String): String? =
        ReglasComunes.validarLargo(texto, 10, 200, "la observación para la emprendedora")
}
