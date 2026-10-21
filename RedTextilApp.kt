package cl.duoc.redtextil

import android.app.Application
import cl.duoc.redtextil.data.local.database.DatosSemilla
import kotlinx.coroutines.launch

class RedTextilApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notificaciones.crearCanales()
        container.imageCompressor.limpiarCapturasTemporales()
        container.scope.launch {
            DatosSemilla.poblarSiEstaVacia(container.database, this@RedTextilApp)
        }
    }
}
