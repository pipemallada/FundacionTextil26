package cl.duoc.redtextil

import cl.duoc.redtextil.data.local.entity.ConsultaEntity
import cl.duoc.redtextil.data.local.entity.CorreoEntity
import cl.duoc.redtextil.data.local.entity.EmprendimientoEntity
import cl.duoc.redtextil.data.local.entity.PublicacionEntity
import cl.duoc.redtextil.domain.model.CategoriaTrabajo
import cl.duoc.redtextil.domain.model.EstadoFicha
import cl.duoc.redtextil.domain.model.EstadoSync
import cl.duoc.redtextil.domain.model.FichaDatos
import cl.duoc.redtextil.domain.model.Rubro
import cl.duoc.redtextil.domain.model.TipoCorreo
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

// Datos ficticios compartidos por las pruebas (mismos formatos que valida la app)
object DatosPrueba {

    val fichaCompleta = FichaDatos(
        nombreEmprendedora = "Camila Rojas Fuentes",
        rut = "12.345.678-5",
        fechaNacimiento = "21/03/1985",
        nacionalidad = "Chilena",
        correo = "camila.rojas@example.com",
        telefono = "+56900000101",
        jefaHogar = "Sí",
        nombreComercial = "Renace Textil",
        descripcion = "Transformamos ropa en desuso en bolsos y chaquetas únicas.",
        anioInicio = "2019",
        lugarTrabajo = "Taller en mi casa",
        cantidadPersonas = "2",
        region = "Valparaíso",
        comuna = "Viña del Mar",
        realizaEnvios = true,
        rubros = setOf(Rubro.ACCESORIOS),
        tecnicas = "Patchwork",
        whatsapp = "+56900000101",
        horarioAtencion = "Lunes a viernes 10 a 18",
        aceptaPublicacion = true
    )

    fun emprendimiento(
        id: Long = 1,
        estado: EstadoFicha = EstadoFicha.ACTIVA,
        estadoSync: EstadoSync = EstadoSync.SINCRONIZADO,
        remoteId: Long? = 100
    ) = EmprendimientoEntity(
        id = id,
        nombreEmprendedora = "",
        nombreComercial = "",
        region = "",
        comuna = ""
    ).conDatos(fichaCompleta).copy(
        estado = estado,
        estadoSync = estadoSync,
        remoteId = remoteId,
        instagram = "@renace.textil",
        sitioWeb = "renacetextil.cl",
        anioInicio = 2019
    )

    fun publicacion(
        id: Long = 10,
        emprendimientoId: Long = 1,
        remoteId: Long? = null,
        estadoSync: EstadoSync = EstadoSync.PENDIENTE_SINCRONIZAR,
        fotoRuta: String? = null
    ) = PublicacionEntity(
        id = id,
        emprendimientoId = emprendimientoId,
        titulo = "Bolso de mezclilla",
        categoria = CategoriaTrabajo.ACCESORIO,
        tecnica = "Patchwork",
        precioReferencial = 25_000,
        descripcion = "Bolso hecho con jeans reciclados.",
        fotoRuta = fotoRuta,
        estadoSync = estadoSync,
        remoteId = remoteId,
        fechaActualizacion = 1_000
    )

    fun consulta(
        id: Long = 20,
        emprendimientoId: Long = 1,
        remoteId: Long? = null,
        estadoSync: EstadoSync = EstadoSync.PENDIENTE_SINCRONIZAR
    ) = ConsultaEntity(
        id = id,
        emprendimientoId = emprendimientoId,
        nombreCliente = "Pedro Soto",
        correoCliente = "pedro@example.com",
        telefonoCliente = "+56911112222",
        servicio = "Accesorios y ecodiseño",
        mensaje = "Hola, ¿hacen bolsos con jeans de mi familia?",
        estadoSync = estadoSync,
        remoteId = remoteId,
        fechaEnvio = 500,
        fechaActualizacion = 1_000
    )

    fun correo(id: Long = 30, destinatario: String = "camila.rojas@example.com") = CorreoEntity(
        id = id,
        emprendimientoId = 1,
        destinatario = destinatario,
        nombreDestinatario = "Camila Rojas Fuentes",
        asunto = "Bienvenida",
        cuerpo = "Hola Camila",
        tipo = TipoCorreo.BIENVENIDA
    )

    fun error404() = HttpException(Response.error<Any>(404, "".toResponseBody()))

    fun error500() = HttpException(Response.error<Any>(500, "".toResponseBody()))
}
