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
) + regionComunaColumna()

private fun FichaHistoricaDto.regionComunaColumna(): Map<String, String> {
    val reg = ChileGeoData.buscarRegion(region.orEmpty()) ?: return emptyMap()
    return mapOf("comuna_${reg.codigo}" to comuna.orEmpty())
}
