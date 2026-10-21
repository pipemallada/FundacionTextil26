package cl.duoc.redtextil

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.core.util.Consumer
import cl.duoc.redtextil.ui.navigation.AppPrincipal
import cl.duoc.redtextil.ui.theme.RedTextilTheme
import cl.duoc.redtextil.util.NotificationHelper

class MainActivity : ComponentActivity() {

    private val destinoNotificacion = mutableStateOf<String?>(null)

    private val escuchaIntents = Consumer<Intent> { intent ->
        destinoNotificacion.value = intent.getStringExtra(NotificationHelper.EXTRA_DESTINO)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Iconos claros sobre la barra terracota y oscuros sobre la barra inferior crema
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(
                Color.argb(0xE6, 0xFF, 0xFF, 0xFF),
                Color.argb(0x80, 0x1B, 0x1B, 0x1B)
            )
        )
        if (savedInstanceState == null) {
            destinoNotificacion.value = intent?.getStringExtra(NotificationHelper.EXTRA_DESTINO)
        }
        addOnNewIntentListener(escuchaIntents)

        setContent {
            RedTextilTheme {
                AppPrincipal(
                    destinoNotificacion = destinoNotificacion.value,
                    onDestinoAtendido = { destinoNotificacion.value = null }
                )
            }
        }
    }

    override fun onDestroy() {
        removeOnNewIntentListener(escuchaIntents)
        super.onDestroy()
    }
}
