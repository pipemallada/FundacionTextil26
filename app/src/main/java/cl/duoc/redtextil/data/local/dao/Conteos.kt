package cl.duoc.redtextil.data.local.dao

data class ConteoPorClave(
    val clave: String,
    val total: Int
)

data class PortadaCatalogo(
    val emprendimientoId: Long,
    val fotoRuta: String?
)
