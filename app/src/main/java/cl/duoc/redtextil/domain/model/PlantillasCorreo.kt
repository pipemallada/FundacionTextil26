package cl.duoc.redtextil.domain.model

data class PlantillaCorreo(val asunto: String, val cuerpo: String)

object PlantillasCorreo {

    private const val FIRMA = "\n\nUn abrazo,\nEquipo Fundación Textil Circular\nRegión de Valparaíso, Chile"

    private fun primerNombre(nombre: String): String = nombre.trim().substringBefore(" ").ifBlank { nombre }

    fun bienvenida(nombre: String, nombreComercial: String) = PlantillaCorreo(
        asunto = "¡Bienvenida a la Red Textil Circular, ${primerNombre(nombre)}!",
        cuerpo = "Hola ${primerNombre(nombre)}:\n\n" +
            "Gracias por sumar \"$nombreComercial\" a la Red Textil Circular. " +
            "Desde la app puedes completar tu ficha, subir fotos de tus trabajos y responder las consultas de tus clientes, " +
            "incluso sin señal: todo se guarda en tu teléfono y se envía cuando vuelva la conexión." + FIRMA
    )

    fun confirmacionInscripcion(nombre: String, nombreComercial: String, comuna: String, region: String) = PlantillaCorreo(
        asunto = "Confirmamos tu inscripción: $nombreComercial",
        cuerpo = "Hola ${primerNombre(nombre)}:\n\n" +
            "Recibimos la inscripción de \"$nombreComercial\" en $comuna, región de $region. " +
            "La administradora del directorio revisará tus datos y te avisaremos cuando tu ficha esté publicada." + FIRMA
    )

    fun aprobacion(nombre: String, nombreComercial: String) = PlantillaCorreo(
        asunto = "¡Tu ficha ya está publicada en el directorio!",
        cuerpo = "Hola ${primerNombre(nombre)}:\n\n" +
            "\"$nombreComercial\" ya aparece en el Directorio Textil Circular. " +
            "Los clientes pueden ver tu catálogo y escribirte por WhatsApp, Instagram o con el formulario de consultas." + FIRMA
    )

    fun observacion(nombre: String, nombreComercial: String, observacion: String) = PlantillaCorreo(
        asunto = "Tu ficha $nombreComercial tiene una observación",
        cuerpo = "Hola ${primerNombre(nombre)}:\n\n" +
            "Revisamos tu ficha y necesitamos que corrijas lo siguiente:\n\n\"$observacion\"\n\n" +
            "Entra a la app, sección \"Mi ficha\", corrige el dato y guarda. Tu ficha volverá a revisión." + FIRMA
    )
}
