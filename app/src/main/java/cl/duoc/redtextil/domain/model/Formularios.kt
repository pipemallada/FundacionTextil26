package cl.duoc.redtextil.domain.model

data class RegistroForm(
    val nombreEmprendedora: String = "",
    val nombreComercial: String = "",
    val rut: String = "",
    val correo: String = "",
    val telefono: String = "",
    val region: String = "",
    val comuna: String = "",
    val aceptaPrivacidad: Boolean = false
)

enum class CampoRegistro {
    NOMBRE_EMPRENDEDORA,
    NOMBRE_COMERCIAL,
    RUT,
    CORREO,
    TELEFONO,
    REGION,
    COMUNA,
    ACEPTA_PRIVACIDAD
}

data class PublicacionForm(
    val titulo: String = "",
    val categoria: CategoriaTrabajo? = null,
    val tecnica: String = "",
    val precio: String = "",
    val descripcion: String = "",
    val fotoRuta: String? = null
)

enum class CampoPublicacion {
    FOTO,
    TITULO,
    CATEGORIA,
    TECNICA,
    PRECIO,
    DESCRIPCION
}

data class ConsultaForm(
    val nombre: String = "",
    val correo: String = "",
    val telefono: String = "",
    val servicio: String = "",
    val mensaje: String = ""
)

enum class CampoConsulta {
    NOMBRE,
    CORREO,
    TELEFONO,
    SERVICIO,
    MENSAJE
}

enum class EstadoCampo {
    NEUTRO,
    VALIDO,
    ERROR
}
