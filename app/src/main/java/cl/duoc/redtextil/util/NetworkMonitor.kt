package cl.duoc.redtextil.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import cl.duoc.redtextil.data.local.PreferenciasLocales
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

class NetworkMonitor(
    context: Context,
    scope: CoroutineScope,
    preferencias: PreferenciasLocales
) {

    private val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val redDelTelefono = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(hayInternetAhora())
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            }
        }
        trySend(hayInternetAhora())
        connectivity.registerDefaultNetworkCallback(callback)
        awaitClose { connectivity.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    // Se puede forzar el modo sin senal desde ajustes para probar la app como si estuviera en zona rural
    val enLinea: StateFlow<Boolean> = combine(redDelTelefono, preferencias.forzarOffline) { red, forzado ->
        red && !forzado
    }.stateIn(scope, SharingStarted.Eagerly, hayInternetAhora() && !preferencias.forzarOffline.value)

    fun hayInternetAhora(): Boolean {
        val red = connectivity.activeNetwork ?: return false
        val capacidades = connectivity.getNetworkCapabilities(red) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
