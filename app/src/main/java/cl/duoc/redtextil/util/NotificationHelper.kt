package cl.duoc.redtextil.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import cl.duoc.redtextil.MainActivity
import cl.duoc.redtextil.R

class NotificationHelper(private val context: Context) {

    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun crearCanales() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val consultas = NotificationChannel(
            CANAL_CONSULTAS,
            "Consultas de clientes",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Avisa cuando una clienta o cliente te escribe desde el directorio" }
        val sistema = NotificationChannel(
            CANAL_SISTEMA,
            "Sincronización y estado de la ficha",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Avisos de envío al servidor, altas y aprobaciones" }
        manager.createNotificationChannels(listOf(consultas, sistema))
    }

    fun tienePermiso(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun notificarNuevaConsulta(nombreComercial: String, nombreCliente: String, servicio: String) {
        mostrar(
            id = (System.currentTimeMillis() % 100000).toInt(),
            canal = CANAL_CONSULTAS,
            titulo = "Nueva consulta para $nombreComercial",
            texto = "$nombreCliente te escribió por \"$servicio\". Toca para responder.",
            destino = DESTINO_BANDEJA
        )
    }

    fun notificarSincronizacion(cantidad: Int) {
        mostrar(
            id = ID_SINCRONIZACION,
            canal = CANAL_SISTEMA,
            titulo = "Datos sincronizados",
            texto = "Se enviaron $cantidad cambios guardados en el teléfono al servidor de la Fundación.",
            destino = null
        )
    }

    fun notificarRegistro(nombreComercial: String) {
        mostrar(
            id = ID_ALTA,
            canal = CANAL_SISTEMA,
            titulo = "Registro recibido",
            texto = "$nombreComercial quedó inscrito. La administradora revisará tu ficha y te avisaremos por correo.",
            destino = DESTINO_PERFIL
        )
    }

    fun notificarCambioEstado(nombreComercial: String, estado: String) {
        mostrar(
            id = ID_ALTA + 1,
            canal = CANAL_SISTEMA,
            titulo = "Tu ficha cambió de estado",
            texto = "$nombreComercial ahora está: $estado.",
            destino = DESTINO_PERFIL
        )
    }

    private fun mostrar(id: Int, canal: String, titulo: String, texto: String, destino: String?) {
        // Sin permiso (Android 13+) no se muestra nada, pero la app sigue funcionando normal
        if (!tienePermiso()) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (destino != null) putExtra(EXTRA_DESTINO, destino)
        }
        val pendiente = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificacion = NotificationCompat.Builder(context, canal)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setColor(0xFFA94E2D.toInt())
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(
                if (canal == CANAL_CONSULTAS) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setAutoCancel(true)
            .setContentIntent(pendiente)
            .build()
        manager.notify(id, notificacion)
    }

    companion object {
        const val CANAL_CONSULTAS = "consultas_clientes"
        const val CANAL_SISTEMA = "sistema_sync"
        const val EXTRA_DESTINO = "destino_notificacion"
        const val DESTINO_BANDEJA = "bandeja"
        const val DESTINO_PERFIL = "perfil"
        private const val ID_SINCRONIZACION = 9001
        private const val ID_ALTA = 9100
    }
}
