package cl.duoc.redtextil.data.remote

import cl.duoc.redtextil.data.remote.dto.ConsultaDto
import cl.duoc.redtextil.data.remote.dto.CorreoDto
import cl.duoc.redtextil.data.remote.dto.FichaDto
import cl.duoc.redtextil.data.remote.dto.FichaHistoricaDto
import cl.duoc.redtextil.data.remote.dto.InteraccionDto
import cl.duoc.redtextil.data.remote.dto.PublicacionDto
import cl.duoc.redtextil.data.remote.dto.RespuestaApi
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

// Endpoints REST del backend Spring Boot de la Fundacion (ver README, seccion API)
interface RedTextilApiService {

    @GET("fichas")
    suspend fun listarFichas(@Query("region") region: String? = null): List<FichaDto>

    @POST("fichas")
    suspend fun crearFicha(@Body ficha: FichaDto): FichaDto

    @PUT("fichas/{id}")
    suspend fun actualizarFicha(@Path("id") id: Long, @Body ficha: FichaDto): FichaDto

    @POST("catalogo")
    suspend fun crearPublicacion(@Body publicacion: PublicacionDto): PublicacionDto

    @PUT("catalogo/{id}")
    suspend fun actualizarPublicacion(@Path("id") id: Long, @Body publicacion: PublicacionDto): PublicacionDto

    @DELETE("catalogo/{id}")
    suspend fun eliminarPublicacion(@Path("id") id: Long): RespuestaApi

    @Multipart
    @POST("catalogo/{id}/foto")
    suspend fun subirFoto(@Path("id") id: Long, @Part foto: MultipartBody.Part): RespuestaApi

    @POST("consultas")
    suspend fun crearConsulta(@Body consulta: ConsultaDto): ConsultaDto

    @PUT("consultas/{id}")
    suspend fun actualizarConsulta(@Path("id") id: Long, @Body consulta: ConsultaDto): ConsultaDto

    @POST("metricas/interacciones")
    suspend fun enviarInteracciones(@Body interacciones: List<InteraccionDto>): RespuestaApi

    @POST("correos/enviar")
    suspend fun enviarCorreo(@Body correo: CorreoDto): RespuestaApi

    @GET("historico/emprendimientos")
    suspend fun descargarHistorico(): List<FichaHistoricaDto>
}
