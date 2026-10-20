package cl.duoc.redtextil.domain.validation

import cl.duoc.redtextil.domain.model.CampoPublicacion
import cl.duoc.redtextil.domain.model.PublicacionForm

object CatalogoValidator {

    const val PRECIO_MAXIMO = 5_000_000

    fun normalizarPrecio(texto: String): Int? {
        val limpio = texto.replace("$", "").replace(".", "").replace(" ", "").trim()
        if (limpio.isEmpty() || limpio.any { !it.isDigit() }) return null
        return limpio.toIntOrNull()
    }

    fun validarCampo(campo: CampoPublicacion, form: PublicacionForm): String? = when (campo) {
        CampoPublicacion.FOTO -> if (form.fotoRuta.isNullOrBlank()) "Agrega una foto de tu trabajo" else null
        CampoPublicacion.TITULO -> ReglasComunes.validarLargo(form.titulo, 3, 60, "un título")
        CampoPublicacion.CATEGORIA -> if (form.categoria == null) "Elige una categoría" else null
        CampoPublicacion.TECNICA -> ReglasComunes.validarLargo(form.tecnica, 3, 40, "la técnica textil")
        CampoPublicacion.PRECIO -> validarPrecio(form.precio)
        CampoPublicacion.DESCRIPCION -> ReglasComunes.validarLargo(form.descripcion, 10, 300, "una descripción")
    }

    fun validarPrecio(texto: String): String? {
        if (texto.isBlank()) return "Escribe un precio de referencia (0 si es a convenir)"
        val precio = normalizarPrecio(texto) ?: return "Usa solo números, sin letras"
        return if (precio > PRECIO_MAXIMO) "El precio máximo es $5.000.000" else null
    }

    fun validarTodo(form: PublicacionForm): Map<CampoPublicacion, String> =
        CampoPublicacion.entries.mapNotNull { campo -> validarCampo(campo, form)?.let { campo to it } }.toMap()
}
