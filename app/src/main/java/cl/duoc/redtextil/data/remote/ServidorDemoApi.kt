package cl.duoc.redtextil.data.remote

import cl.duoc.redtextil.data.remote.dto.ConsultaDto
import cl.duoc.redtextil.data.remote.dto.CorreoDto
import cl.duoc.redtextil.data.remote.dto.FichaDto
import cl.duoc.redtextil.data.remote.dto.FichaHistoricaDto
import cl.duoc.redtextil.data.remote.dto.InteraccionDto
import cl.duoc.redtextil.data.remote.dto.PublicacionDto
import cl.duoc.redtextil.data.remote.dto.RespuestaApi
import kotlinx.coroutines.delay
import okhttp3.MultipartBody
import java.util.concurrent.atomic.AtomicLong

// Simula las respuestas del backend Spring Boot para poder probar la sincronizacion sin servidor levantado
class ServidorDemoApi : RedTextilApiService {

    private val secuencia = AtomicLong(20_000)

    private suspend fun latencia() = delay(350)

    override suspend fun listarFichas(region: String?): List<FichaDto> {
        latencia()
        return emptyList()
    }

    override suspend fun crearFicha(ficha: FichaDto): FichaDto {
        latencia()
        return ficha.copy(id = secuencia.incrementAndGet())
    }

    override suspend fun actualizarFicha(id: Long, ficha: FichaDto): FichaDto {
        latencia()
        return ficha.copy(id = id)
    }

    override suspend fun crearPublicacion(publicacion: PublicacionDto): PublicacionDto {
        latencia()
        return publicacion.copy(id = secuencia.incrementAndGet())
    }

    override suspend fun actualizarPublicacion(id: Long, publicacion: PublicacionDto): PublicacionDto {
        latencia()
        return publicacion.copy(id = id)
    }

    override suspend fun eliminarPublicacion(id: Long): RespuestaApi {
        latencia()
        return RespuestaApi(ok = true, mensaje = "Publicación $id eliminada")
    }

    override suspend fun subirFoto(id: Long, foto: MultipartBody.Part): RespuestaApi {
        latencia()
        return RespuestaApi(ok = true, mensaje = "Foto recibida")
    }

    override suspend fun crearConsulta(consulta: ConsultaDto): ConsultaDto {
        latencia()
        return consulta.copy(id = secuencia.incrementAndGet())
    }

    override suspend fun actualizarConsulta(id: Long, consulta: ConsultaDto): ConsultaDto {
        latencia()
        return consulta.copy(id = id)
    }

    override suspend fun enviarInteracciones(interacciones: List<InteraccionDto>): RespuestaApi {
        latencia()
        return RespuestaApi(ok = true, mensaje = "${interacciones.size} registros recibidos")
    }

    override suspend fun enviarCorreo(correo: CorreoDto): RespuestaApi {
        latencia()
        return RespuestaApi(ok = true, mensaje = "Correo enviado a ${correo.destinatario}")
    }

    // El servidor demo no trae el historico, asi la app usa el respaldo depurado que viene en assets
    override suspend fun descargarHistorico(): List<FichaHistoricaDto> {
        latencia()
        return emptyList()
    }
}
