package cl.duoc.redtextil.domain.model

data class FichaDatos(
    val nombreEmprendedora: String = "",
    val rut: String = "",
    val fechaNacimiento: String = "",
    val nacionalidad: String = "",
    val correo: String = "",
    val telefono: String = "",
    val jefaHogar: String = "",
    val nombreComercial: String = "",
    val descripcion: String = "",
    val anioInicio: String = "",
    val lugarTrabajo: String = "",
    val cantidadPersonas: String = "",
    val formalizado: Boolean = false,
    val rutEmpresa: String = "",
    val tipoFormalizacion: String = "",
    val region: String = "",
    val comuna: String = "",
    val atencionPresencial: Boolean = false,
    val direccionTaller: String = "",
    val realizaEnvios: Boolean = false,
    val rubros: Set<Rubro> = emptySet(),
    val tecnicas: String = "",
    val recibeDonaciones: Boolean = false,
    val tiposDonacion: String = "",
    val cuposTaller: String = "",
    val modalidadTaller: String = "",
    val tallasArriendo: String = "",
    val whatsapp: String = "",
    val instagram: String = "",
    val sitioWeb: String = "",
    val horarioAtencion: String = "",
    val aceptaPublicacion: Boolean = false
)

enum class SeccionFicha(val titulo: String, val descripcion: String) {
    PERSONALES("Datos personales", "Quién eres y cómo te contactamos"),
    TALLER("Tu taller y formalización", "Cuéntanos de tu emprendimiento"),
    UBICACION("Ubicación", "Dónde trabajas y cómo entregas tus trabajos"),
    SERVICIOS("Rubros y servicios", "Elige 1 o 2 rubros de la economía circular"),
    CONTACTO("Contacto y redes", "Cómo te encuentran tus clientes")
}

enum class CampoFicha(val etiqueta: String, val seccion: SeccionFicha) {
    NOMBRE_EMPRENDEDORA("Nombre completo", SeccionFicha.PERSONALES),
    RUT("RUT", SeccionFicha.PERSONALES),
    FECHA_NACIMIENTO("Fecha de nacimiento", SeccionFicha.PERSONALES),
    NACIONALIDAD("Nacionalidad", SeccionFicha.PERSONALES),
    CORREO("Correo electrónico", SeccionFicha.PERSONALES),
    TELEFONO("Teléfono", SeccionFicha.PERSONALES),
    JEFA_HOGAR("¿Eres jefa de hogar?", SeccionFicha.PERSONALES),

    NOMBRE_COMERCIAL("Nombre comercial", SeccionFicha.TALLER),
    DESCRIPCION("Descripción del emprendimiento", SeccionFicha.TALLER),
    ANIO_INICIO("Año en que empezaste", SeccionFicha.TALLER),
    LUGAR_TRABAJO("¿Dónde trabajas?", SeccionFicha.TALLER),
    CANTIDAD_PERSONAS("Personas que trabajan contigo", SeccionFicha.TALLER),
    FORMALIZADO("Tengo inicio de actividades", SeccionFicha.TALLER),
    RUT_EMPRESA("RUT de la empresa o actividad", SeccionFicha.TALLER),
    TIPO_FORMALIZACION("Tipo de formalización", SeccionFicha.TALLER),

    REGION("Región", SeccionFicha.UBICACION),
    COMUNA("Comuna", SeccionFicha.UBICACION),
    ATENCION_PRESENCIAL("Atiendo presencialmente", SeccionFicha.UBICACION),
    DIRECCION_TALLER("Dirección del taller", SeccionFicha.UBICACION),
    REALIZA_ENVIOS("Hago envíos o despachos", SeccionFicha.UBICACION),

    RUBROS("Rubros de servicio", SeccionFicha.SERVICIOS),
    TECNICAS("Técnicas que usas", SeccionFicha.SERVICIOS),
    RECIBE_DONACIONES("Recibo donaciones de ropa", SeccionFicha.SERVICIOS),
    TIPOS_DONACION("¿Qué donaciones recibes?", SeccionFicha.SERVICIOS),
    CUPOS_TALLER("Cupos por taller", SeccionFicha.SERVICIOS),
    MODALIDAD_TALLER("Modalidad del taller", SeccionFicha.SERVICIOS),
    TALLAS_ARRIENDO("Tallas disponibles para arriendo", SeccionFicha.SERVICIOS),

    WHATSAPP("WhatsApp", SeccionFicha.CONTACTO),
    INSTAGRAM("Instagram", SeccionFicha.CONTACTO),
    SITIO_WEB("Sitio web o Facebook", SeccionFicha.CONTACTO),
    HORARIO_ATENCION("Horario de atención", SeccionFicha.CONTACTO),
    ACEPTA_PUBLICACION("Autorizo publicar mi ficha", SeccionFicha.CONTACTO)
}

object OpcionesFicha {
    val nacionalidades = listOf("Chilena", "Extranjera residente")
    val jefaHogar = listOf("Sí", "No", "Prefiero no decir")
    val lugaresTrabajo = listOf("Taller en mi casa", "Local comercial", "Feria o persa", "Solo venta online")
    val tiposFormalizacion = listOf(
        "Persona natural con inicio de actividades",
        "Empresa Individual (EIRL)",
        "Sociedad por Acciones (SpA)",
        "Cooperativa",
        "Otra"
    )
    val modalidadesTaller = listOf("Presencial", "Online", "Mixta")
}
