package cl.duoc.redtextil.data.remote

import retrofit2.HttpException

fun Throwable.esNoEncontrado(): Boolean = this is HttpException && code() == 404

// Si el microservicio ya no tiene el registro (por ejemplo, se reinicio su base de datos) se vuelve a crear
suspend fun <T> actualizarOCrear(idRemoto: Long?, actualizar: suspend (Long) -> T, crear: suspend () -> T): T {
    if (idRemoto == null) return crear()
    return try {
        actualizar(idRemoto)
    } catch (e: HttpException) {
        if (e.esNoEncontrado()) crear() else throw e
    }
}

// Borrar algo que el servidor ya no tiene se considera un borrado exitoso
suspend fun eliminarEnServidor(eliminar: suspend () -> Unit) {
    try {
        eliminar()
    } catch (e: HttpException) {
        if (!e.esNoEncontrado()) throw e
    }
}
