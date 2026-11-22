package cl.duoc.redtextil.domain.validation

import cl.duoc.redtextil.domain.model.FichaDatos
import cl.duoc.redtextil.domain.model.Rubro
import cl.duoc.redtextil.util.ChileGeoData
import cl.duoc.redtextil.util.TextoUtil

data class FilaHistoricaDepurada(
    val idHistorico: String,
    val marcaTemporal: String,
    val datos: FichaDatos,
    val problemasCriticos: List<String>,
    val avisos: List<String>,
    val correcciones: Int
) {
    val quedaActiva: Boolean get() = problemasCriticos.isEmpty()
}

// Limpia las filas exportadas desde la planilla de Google Sheets antes de integrarlas a Room
object DepuradorHistorico {

    fun depurar(fila: Map<String, String>): FilaHistoricaDepurada {
        val criticos = mutableListOf<String>()
        val avisos = mutableListOf<String>()
        var correcciones = 0

        val nombreOriginal = fila["nombre_emprendedora"].orEmpty()
        val nombre = TextoUtil.capitalizarPalabras(nombreOriginal)
        if (nombre != TextoUtil.limpiarEspacios(nombreOriginal)) correcciones++

        val comercialOriginal = TextoUtil.limpiarEspacios(fila["nombre_comercial"].orEmpty())
        val comercial = if (comercialOriginal == comercialOriginal.lowercase() || comercialOriginal == comercialOriginal.uppercase()) {
            TextoUtil.capitalizarPalabras(comercialOriginal)
        } else {
            comercialOriginal
        }
        if (comercial != comercialOriginal) correcciones++
        if (comercial.isBlank()) criticos.add("Sin nombre comercial")

        val correoOriginal = fila["correo"].orEmpty().trim()
        val correo = correoOriginal.lowercase()
        val correoFinal = if (correo.isNotEmpty() && ReglasComunes.validarCorreo(correo) == null) {
            correo
        } else {
            if (correoOriginal.isNotEmpty()) avisos.add("Correo inválido, se dejó en blanco")
            ""
        }

        val telefonoOriginal = fila["telefono"].orEmpty().trim()
        val telefono = ReglasComunes.normalizarTelefono(telefonoOriginal)
        if (telefono != null && telefono != telefonoOriginal) correcciones++
        if (telefonoOriginal.isNotEmpty() && telefono == null) avisos.add("Teléfono con formato inválido")

        val region = ChileGeoData.buscarRegion(fila["region"].orEmpty())
        if (region == null) criticos.add("Región no reconocida")
        if (region != null && region.nombre != fila["region"].orEmpty().trim()) correcciones++

        // La planilla generaba una columna de comuna por cada region; nos quedamos con la que calza con la region elegida
        var comuna = ""
        if (region != null) {
            val columnasConValor = ChileGeoData.regiones
                .map { it to fila["comuna_${it.codigo}"].orEmpty().trim() }
                .filter { it.second.isNotEmpty() }
            val propia = columnasConValor.firstOrNull { it.first.codigo == region.codigo }?.second
            val encontrada = propia?.let { ChileGeoData.buscarComuna(it) }
            when {
                encontrada != null && encontrada.second.codigo == region.codigo -> {
                    comuna = encontrada.first
                    if (encontrada.first != propia) correcciones++
                    if (columnasConValor.size > 1) {
                        correcciones++
                        avisos.add("Tenía ${columnasConValor.size} columnas de comuna, se dejó solo la de ${region.nombre}")
                    }
                }
                propia != null -> criticos.add("Comuna \"$propia\" no reconocida en ${region.nombre}")
                columnasConValor.isNotEmpty() ->
                    criticos.add("Comuna registrada en otra región (cambió de región sin actualizar la comuna)")
                else -> criticos.add("Sin comuna: la ficha quedaba invisible en las búsquedas")
            }
        }

        val textosRubro = fila["rubros"].orEmpty().split(";", "|").map { it.trim() }.filter { it.isNotEmpty() }
        val rubrosLeidos = textosRubro.mapNotNull { Rubro.desdeTexto(it) }.distinct()
        val rubros = rubrosLeidos.take(FichaValidator.MAX_RUBROS).toSet()
        if (rubrosLeidos.size > FichaValidator.MAX_RUBROS) {
            correcciones++
            avisos.add("Venía con ${rubrosLeidos.size} rubros, se dejaron los 2 primeros")
        }
        if (rubros.isEmpty()) criticos.add("Sin rubro de servicio")

        val instagramOriginal = fila["instagram"].orEmpty().trim()
        val instagram = ReglasComunes.normalizarInstagram(instagramOriginal)
        if (instagramOriginal.isNotEmpty()) {
            if (instagram == null) avisos.add("Enlace de Instagram roto: \"$instagramOriginal\"")
            else if (instagram != instagramOriginal) correcciones++
        }

        if (telefono == null && instagram == null) criticos.add("Sin un canal de contacto válido")

        val datos = FichaDatos(
            nombreEmprendedora = nombre,
            correo = correoFinal,
            telefono = telefono.orEmpty(),
            nombreComercial = comercial,
            descripcion = TextoUtil.limpiarEspacios(fila["descripcion"].orEmpty()),
            region = region?.nombre.orEmpty(),
            comuna = comuna,
            rubros = rubros,
            whatsapp = telefono.orEmpty(),
            instagram = instagram.orEmpty(),
            aceptaPublicacion = true
        )

        return FilaHistoricaDepurada(
            idHistorico = fila["id_historico"].orEmpty().trim(),
            marcaTemporal = fila["marca_temporal"].orEmpty().trim(),
            datos = datos,
            problemasCriticos = criticos,
            avisos = avisos,
            correcciones = correcciones
        )
    }
}
