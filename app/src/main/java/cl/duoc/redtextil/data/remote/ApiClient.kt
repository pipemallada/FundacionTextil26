package cl.duoc.redtextil.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // 10.0.2.2 es el localhost del computador visto desde el emulador de Android Studio
    const val BASE_URL = "http://10.0.2.2:8080/api/"

    private val registro = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    // Tiempos cortos para no quedar esperando cuando la senal es mala, total los datos quedan en Room
    private val clienteHttp = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(registro)
        .build()

    val servicio: RedTextilApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(clienteHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RedTextilApiService::class.java)
    }
}
