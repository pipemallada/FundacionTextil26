package cl.duoc.redtextil.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferenciasLocales(context: Context) {

    private val prefs = context.getSharedPreferences("red_textil_prefs", Context.MODE_PRIVATE)

    private val _sesionId = MutableStateFlow(prefs.getLong(CLAVE_SESION, SIN_SESION).takeIf { it != SIN_SESION })
    val sesionId: StateFlow<Long?> = _sesionId.asStateFlow()

    private val _forzarOffline = MutableStateFlow(prefs.getBoolean(CLAVE_OFFLINE, false))
    val forzarOffline: StateFlow<Boolean> = _forzarOffline.asStateFlow()

    // Mientras no exista el backend Spring Boot levantado, el servidor de demostracion responde igual que la API
    private val _servidorDemo = MutableStateFlow(prefs.getBoolean(CLAVE_SERVIDOR_DEMO, true))
    val servidorDemo: StateFlow<Boolean> = _servidorDemo.asStateFlow()

    fun guardarSesion(id: Long?) {
        prefs.edit().putLong(CLAVE_SESION, id ?: SIN_SESION).apply()
        _sesionId.value = id
    }

    fun cambiarForzarOffline(valor: Boolean) {
        prefs.edit().putBoolean(CLAVE_OFFLINE, valor).apply()
        _forzarOffline.value = valor
    }

    fun cambiarServidorDemo(valor: Boolean) {
        prefs.edit().putBoolean(CLAVE_SERVIDOR_DEMO, valor).apply()
        _servidorDemo.value = valor
    }

    companion object {
        private const val CLAVE_SESION = "sesion_emprendimiento_id"
        private const val CLAVE_OFFLINE = "forzar_offline"
        private const val CLAVE_SERVIDOR_DEMO = "servidor_demo"
        private const val SIN_SESION = -1L
    }
}
