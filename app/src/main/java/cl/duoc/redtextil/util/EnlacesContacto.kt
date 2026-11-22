package cl.duoc.redtextil.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import cl.duoc.redtextil.domain.validation.ReglasComunes

object EnlacesContacto {

    fun urlWhatsapp(telefono: String, mensaje: String = ""): String? {
        val normalizado = ReglasComunes.normalizarTelefono(telefono) ?: return null
        val base = "https://wa.me/${normalizado.removePrefix("+")}"
        return if (mensaje.isBlank()) base else "$base?text=${Uri.encode(mensaje)}"
    }

    fun urlInstagram(usuario: String): String? {
        val normalizado = ReglasComunes.normalizarInstagram(usuario) ?: return null
        return "https://www.instagram.com/${normalizado.removePrefix("@")}/"
    }

    fun urlSitio(sitio: String): String? = ReglasComunes.normalizarUrl(sitio)

    // Solo abrimos enlaces que ya pasaron la validacion, asi no llegan enlaces rotos al cliente
    fun abrir(context: Context, url: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }
}
