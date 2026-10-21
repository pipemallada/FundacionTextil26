package cl.duoc.redtextil.data.remote.dto

data class FichaDto(
    val id: Long? = null,
    val idLocal: Long? = null,
    val nombreEmprendedora: String? = null,
    val rut: String? = null,
    val correo: String? = null,
    val telefono: String? = null,
    val nombreComercial: String? = null,
    val descripcion: String? = null,
    val region: String? = null,
    val comuna: String? = null,
    val rubros: List<String>? = null,
    val whatsapp: String? = null,
    val instagram: String? = null,
    val sitioWeb: String? = null,
    val horarioAtencion: String? = null,
    val estado: String? = null,
    val origen: String? = null,
    val actualizadoEn: Long? = null
)

data class PublicacionDto(
    val id: Long? = null,
    val idLocal: Long? = null,
    val fichaId: Long? = null,
    val titulo: String? = null,
    val categoria: String? = null,
    val tecnica: String? = null,
    val precioReferencial: Int? = null,
    val descripcion: String? = null,
    val urlFoto: String? = null
)

data class ConsultaDto(
    val id: Long? = null,
    val idLocal: Long? = null,
    val fichaId: Long? = null,
    val nombreCliente: String? = null,
    val correoCliente: String? = null,
    val telefonoCliente: String? = null,
    val servicio: String? = null,
    val mensaje: String? = null,
    val estado: String? = null,
    val respuesta: String? = null,
    val fechaEnvio: Long? = null
)

data class InteraccionDto(
    val tipo: String,
    val fichaId: Long?,
    val region: String,
    val rubro: String,
    val detalle: String,
    val fecha: Long
)

data class CorreoDto(
    val destinatario: String,
    val asunto: String,
    val cuerpo: String,
    val tipo: String
)

data class FichaHistoricaDto(
    val idHistorico: String? = null,
    val marcaTemporal: String? = null,
    val nombreEmprendedora: String? = null,
    val nombreComercial: String? = null,
    val correo: String? = null,
    val telefono: String? = null,
    val region: String? = null,
    val comuna: String? = null,
    val rubros: String? = null,
    val instagram: String? = null,
    val descripcion: String? = null
)

data class RespuestaApi(
    val ok: Boolean = true,
    val mensaje: String? = null
)
