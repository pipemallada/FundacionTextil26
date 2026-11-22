package cl.duoc.redtextil.data.remote

import cl.duoc.redtextil.data.local.entity.ConsultaEntity
import cl.duoc.redtextil.data.local.entity.CorreoEntity
import cl.duoc.redtextil.data.local.entity.EmprendimientoEntity
import cl.duoc.redtextil.data.local.entity.InteraccionEntity
import cl.duoc.redtextil.data.local.entity.PublicacionEntity
import cl.duoc.redtextil.data.remote.dto.ConsultaDto
import cl.duoc.redtextil.data.remote.dto.CorreoDto
import cl.duoc.redtextil.data.remote.dto.FichaDto
import cl.duoc.redtextil.data.remote.dto.FichaHistoricaDto
import cl.duoc.redtextil.data.remote.dto.InteraccionDto
import cl.duoc.redtextil.data.remote.dto.PublicacionDto
import cl.duoc.redtextil.domain.model.CategoriaTrabajo
import cl.duoc.redtextil.domain.model.EstadoConsulta
import cl.duoc.redtextil.domain.model.EstadoFicha
import cl.duoc.redtextil.domain.model.EstadoSync
import cl.duoc.redtextil.domain.model.OrigenFicha
import cl.duoc.redtextil.util.ChileGeoData

fun EmprendimientoEntity.aDto() = FichaDto(
    id = remoteId,
    idLocal = id,
    nombreEmprendedora = nombreEmprendedora,
    rut = rut,
    correo = correo,
    telefono = telefono,
    nombreComercial = nombreComercial,
    descripcion = descripcion,
    region = region,
    comuna = comuna,
    rubros = listaRubros().map { it.name },
    whatsapp = whatsapp,
    instagram = instagram,
    sitioWeb = sitioWeb,
    horarioAtencion = horarioAtencion,
    estado = estado.name,
    origen = origen.name,
    actualizadoEn = fechaActualizacion
)

fun PublicacionEntity.aDto(fichaRemotaId: Long?) = PublicacionDto(
    id = remoteId,
    idLocal = id,
    fichaId = fichaRemotaId,
    titulo = titulo,
    categoria = categoria.name,
    tecnica = tecnica,
    precioReferencial = precioReferencial,
    descripcion = descripcion
)

fun ConsultaEntity.aDto(fichaRemotaId: Long?) = ConsultaDto(
    id = remoteId,
    idLocal = id,
    fichaId = fichaRemotaId,
    nombreCliente = nombreCliente,
    correoCliente = correoCliente,
    telefonoCliente = telefonoCliente,
    servicio = servicio,
    mensaje = mensaje,
    estado = estado.name,
    respuesta = respuesta,
    fechaEnvio = fechaEnvio
)

fun InteraccionEntity.aDto() = InteraccionDto(
    tipo = tipo.name,
    fichaId = emprendimientoId,
    region = region,
    rubro = rubro,
    detalle = detalle,
    fecha = fecha
)

fun CorreoEntity.aDto() = CorreoDto(
    destinatario = destinatario,
    asunto = asunto,
    cuerpo = cuerpo,
    tipo = tipo.name
)

fun FichaHistoricaDto.aFilaCsv(): Map<String, String> = mapOf(
    "id_historico" to idHistorico.orEmpty(),
    "marca_temporal" to marcaTemporal.orEmpty(),
    "nombre_emprendedora" to nombreEmprendedora.orEmpty(),
    "nombre_comercial" to nombreComercial.orEmpty(),
    "correo" to correo.orEmpty(),
    "telefono" to telefono.orEmpty(),
    "region" to region.orEmpty(),
    "rubros" to rubros.orEmpty(),
    "instagram" to instagram.orEmpty(),
    "descripcion" to descripcion.orEmpty()
) + (comunas?.mapKeys { "comuna_${it.key}" } ?: regionComunaColumna())

private fun FichaHistoricaDto.regionComunaColumna(): Map<String, String> {
    val reg = ChileGeoData.buscarRegion(region.orEmpty()) ?: return emptyMap()
    return mapOf("comuna_${reg.codigo}" to comuna.orEmpty())
}

// ---- Datos que llegan desde el microservicio hacia Room ----

fun EmprendimientoEntity.conDatosDelServidor(dto: FichaDto) = copy(
    nombreEmprendedora = dto.nombreEmprendedora ?: nombreEmprendedora,
    rut = dto.rut ?: rut,
    correo = dto.correo ?: correo,
    telefono = dto.telefono ?: telefono,
    nombreComercial = dto.nombreComercial ?: nombreComercial,
    descripcion = dto.descripcion ?: descripcion,
    region = dto.region ?: region,
    comuna = dto.comuna ?: comuna,
    rubroPrincipal = dto.rubros?.getOrNull(0) ?: rubroPrincipal,
    rubroSecundario = dto.rubros?.let { it.getOrNull(1).orEmpty() } ?: rubroSecundario,
    whatsapp = dto.whatsapp ?: whatsapp,
    instagram = dto.instagram ?: instagram,
    sitioWeb = dto.sitioWeb ?: sitioWeb,
    horarioAtencion = dto.horarioAtencion ?: horarioAtencion,
    estado = EstadoFicha.entries.firstOrNull { it.name == dto.estado } ?: estado,
    origen = OrigenFicha.entries.firstOrNull { it.name == dto.origen } ?: origen,
    remoteId = dto.id,
    estadoSync = EstadoSync.SINCRONIZADO,
    fechaActualizacion = dto.actualizadoEn ?: fechaActualizacion
)

fun FichaDto.aEntidad(): EmprendimientoEntity = EmprendimientoEntity(
    nombreEmprendedora = "",
    nombreComercial = "",
    region = "",
    comuna = "",
    aceptaPublicacion = true,
    aceptaPrivacidad = true
).conDatosDelServidor(this)

fun PublicacionEntity.conDatosDelServidor(dto: PublicacionDto) = copy(
    titulo = dto.titulo ?: titulo,
    categoria = CategoriaTrabajo.entries.firstOrNull { it.name == dto.categoria } ?: categoria,
    tecnica = dto.tecnica ?: tecnica,
    precioReferencial = dto.precioReferencial ?: precioReferencial,
    descripcion = dto.descripcion ?: descripcion,
    // Si la foto no esta en el telefono se muestra la que guardo el microservicio
    fotoRuta = fotoRuta ?: dto.urlFoto?.let { ApiClient.BASE_URL + it },
    fotoSincronizada = true,
    remoteId = dto.id,
    estadoSync = EstadoSync.SINCRONIZADO
)

fun PublicacionDto.aEntidad(emprendimientoId: Long): PublicacionEntity = PublicacionEntity(
    emprendimientoId = emprendimientoId,
    titulo = "",
    categoria = CategoriaTrabajo.entries.first(),
    tecnica = "",
    precioReferencial = 0,
    descripcion = "",
    fotoRuta = null
).conDatosDelServidor(this)

fun ConsultaEntity.conDatosDelServidor(dto: ConsultaDto) = copy(
    nombreCliente = dto.nombreCliente ?: nombreCliente,
    correoCliente = dto.correoCliente ?: correoCliente,
    telefonoCliente = dto.telefonoCliente ?: telefonoCliente,
    servicio = dto.servicio ?: servicio,
    mensaje = dto.mensaje ?: mensaje,
    estado = EstadoConsulta.entries.firstOrNull { it.name == dto.estado } ?: estado,
    respuesta = dto.respuesta ?: respuesta,
    fechaEnvio = dto.fechaEnvio ?: fechaEnvio,
    remoteId = dto.id,
    estadoSync = EstadoSync.SINCRONIZADO
)

fun ConsultaDto.aEntidad(emprendimientoId: Long): ConsultaEntity = ConsultaEntity(
    emprendimientoId = emprendimientoId,
    nombreCliente = "",
    correoCliente = "",
    telefonoCliente = "",
    servicio = "",
    mensaje = "",
    enviadaDesdeApp = false
).conDatosDelServidor(this)
