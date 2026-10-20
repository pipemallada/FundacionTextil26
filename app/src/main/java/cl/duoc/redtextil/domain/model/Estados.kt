package cl.duoc.redtextil.domain.model

enum class EstadoFicha(val etiqueta: String, val explicacion: String) {
    ACTIVA("Activa", "Tu ficha aparece en el directorio y los clientes pueden contactarte."),
    PENDIENTE("Pendiente", "La administradora está revisando tu ficha. Te avisaremos por correo."),
    OBSERVADA("Observada", "Hay datos por corregir. Revisa la observación y actualiza tu ficha."),
    BAJA("De baja", "Tu ficha está pausada y no aparece en el directorio.")
}

enum class EstadoSync {
    SINCRONIZADO,
    PENDIENTE_SINCRONIZAR
}

enum class OrigenFicha(val etiqueta: String) {
    APP("App móvil"),
    SEMILLA("Cuenta demo"),
    HISTORICO("Dataset histórico")
}

enum class EstadoConsulta(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    RESPONDIDA("Respondida")
}

enum class TipoInteraccion(val etiqueta: String) {
    BUSQUEDA("Búsquedas"),
    CONSULTA("Consultas enviadas"),
    CLIC_WHATSAPP("Clics en WhatsApp"),
    CLIC_INSTAGRAM("Clics en Instagram");

    val esContactoEfectivo: Boolean
        get() = this != BUSQUEDA
}

enum class EstadoCorreo(val etiqueta: String) {
    ENVIADO_OK("Enviado"),
    EN_COLA_OFFLINE("En cola (sin señal)"),
    FALLIDO("Falló el envío")
}

enum class TipoCorreo(val etiqueta: String) {
    BIENVENIDA("Bienvenida"),
    CONFIRMACION_INSCRIPCION("Confirmación de inscripción"),
    APROBACION("Ficha aprobada"),
    OBSERVACION("Ficha observada")
}

enum class CategoriaTrabajo(val etiqueta: String) {
    PRENDA_INTERVENIDA("Prenda intervenida"),
    ARREGLO("Arreglo o ajuste"),
    SEGUNDA_MANO("Prenda de segunda mano"),
    ARRIENDO("Prenda para arriendo"),
    ACCESORIO("Accesorio"),
    TALLER("Taller o curso"),
    TEJIDO("Tejido o confección");

    companion object {
        fun desdeEtiqueta(etiqueta: String): CategoriaTrabajo? = entries.firstOrNull { it.etiqueta == etiqueta }
    }
}
