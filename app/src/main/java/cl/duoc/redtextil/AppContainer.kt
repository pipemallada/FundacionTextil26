package cl.duoc.redtextil

import android.content.Context
import cl.duoc.redtextil.data.local.PreferenciasLocales
import cl.duoc.redtextil.data.local.database.AppDatabase
import cl.duoc.redtextil.data.remote.ApiClient
import cl.duoc.redtextil.data.remote.RedTextilApiService
import cl.duoc.redtextil.data.remote.ServidorDemoApi
import cl.duoc.redtextil.data.repository.CatalogoRepository
import cl.duoc.redtextil.data.repository.ConsultaRepository
import cl.duoc.redtextil.data.repository.CorreoRepository
import cl.duoc.redtextil.data.repository.EmprendimientoRepository
import cl.duoc.redtextil.data.repository.HistoricoRepository
import cl.duoc.redtextil.data.repository.MetricasRepository
import cl.duoc.redtextil.data.repository.SesionRepository
import cl.duoc.redtextil.data.repository.SincronizacionRepository
import cl.duoc.redtextil.util.ImageCompressor
import cl.duoc.redtextil.util.NetworkMonitor
import cl.duoc.redtextil.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

// Inyeccion de dependencias manual: un solo lugar donde se crean la base de datos, la API y los repositorios
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase = AppDatabase.obtener(appContext)
    val preferencias = PreferenciasLocales(appContext)
    val networkMonitor = NetworkMonitor(appContext, scope, preferencias)
    val notificaciones = NotificationHelper(appContext)
    val imageCompressor = ImageCompressor(appContext)

    private val servidorDemo = ServidorDemoApi()
    private val api: () -> RedTextilApiService = {
        if (preferencias.servidorDemo.value) servidorDemo else ApiClient.servicio
    }

    val correoRepository = CorreoRepository(database.correoDao(), api, networkMonitor)
    val metricasRepository = MetricasRepository(database.interaccionDao(), api)
    val sesionRepository = SesionRepository(preferencias, database.emprendimientoDao())
    val emprendimientoRepository = EmprendimientoRepository(database, api, networkMonitor, correoRepository, notificaciones)
    val catalogoRepository = CatalogoRepository(database, api, networkMonitor, imageCompressor)
    val consultaRepository = ConsultaRepository(database, api, networkMonitor, notificaciones, metricasRepository)
    val historicoRepository = HistoricoRepository(appContext, database, api, networkMonitor)
    val sincronizacionRepository = SincronizacionRepository(
        emprendimientoRepository,
        catalogoRepository,
        consultaRepository,
        metricasRepository,
        correoRepository,
        networkMonitor,
        notificaciones
    )
}
