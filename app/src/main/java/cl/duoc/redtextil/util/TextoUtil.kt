package cl.duoc.redtextil.util

import java.text.Normalizer

object TextoUtil {

    private val marcasDiacriticas = Regex("\\p{Mn}+")
    private val espaciosMultiples = Regex("\\s+")

    // Sirve para comparar textos escritos a mano: "Viña del Mar", "vina del mar" y "VIÑA  DEL MAR" quedan iguales
    fun normalizar(texto: String): String {
        val sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replace(marcasDiacriticas, "")
        return sinTildes.lowercase().replace(espaciosMultiples, " ").trim()
    }

    fun limpiarEspacios(texto: String): String = texto.replace(espaciosMultiples, " ").trim()

    fun capitalizarPalabras(texto: String): String {
        val conectores = setOf("de", "del", "la", "las", "los", "y", "e")
        return limpiarEspacios(texto).lowercase().split(" ").mapIndexed { i, palabra ->
            if (i > 0 && palabra in conectores) palabra
            else palabra.replaceFirstChar { it.uppercase() }
        }.joinToString(" ")
    }

    fun iniciales(nombre: String): String =
        limpiarEspacios(nombre).split(" ").filter { it.isNotEmpty() }.take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "?" }

    fun formatearPesos(valor: Int): String {
        if (valor <= 0) return "A convenir"
        val conPuntos = valor.toString().reversed().chunked(3).joinToString(".").reversed()
        return "$$conPuntos"
    }
}
