package cl.duoc.redtextil.data.remote

import cl.duoc.redtextil.data.remote.dto.IndicadoresDto
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

// API publica y gratuita con el dolar, euro y UF del dia en Chile (no requiere clave)
interface MindicadorApi {

    @GET("api")
    suspend fun indicadoresDelDia(): IndicadoresDto
}

// Cliente Retrofit separado: no comparte URL, cliente HTTP ni datos con el microservicio de la Fundacion
object MindicadorClient {

    const val BASE_URL = "https://mindicador.cl/"

    val servicio: MindicadorApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MindicadorApi::class.java)
    }
}
