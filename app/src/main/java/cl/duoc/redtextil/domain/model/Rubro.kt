package cl.duoc.redtextil.domain.model

import cl.duoc.redtextil.util.TextoUtil

enum class Rubro(
    val etiqueta: String,
    val descripcion: String,
    private val palabrasClave: List<String>
) {
    SUPRARRECICLAJE(
        "Suprarreciclaje (Upcycling)",
        "Transforma ropa en desuso en prendas y objetos nuevos",
        listOf("suprarreciclaje", "upcycling")
    ),
    REPARACION(
        "Reparación y Ajustes",
        "Arreglos, bastas, cierres y ajustes de talla",
        listOf("reparacion", "ajuste", "arreglo")
    ),
    SEGUNDA_MANO(
        "Segunda Mano y Reutilización",
        "Venta e intercambio de ropa usada en buen estado",
        listOf("segunda mano", "reutilizacion", "ropa usada")
    ),
    ARRIENDO(
        "Arriendo de Prendas",
        "Arriendo de vestidos, trajes y ropa para eventos",
        listOf("arriendo", "alquiler")
    ),
    TALLERES(
        "Talleres y Educación",
        "Clases de costura, tejido y reciclaje textil",
        listOf("taller", "educacion", "clases")
    ),
    ACCESORIOS(
        "Accesorios y Ecodiseño",
        "Bolsos, aros y accesorios hechos con material reutilizado",
        listOf("accesorio", "ecodiseno")
    ),
    TEJIDO(
        "Tejido y Confección Local",
        "Tejido a palillo, crochet, telar y confección a medida",
        listOf("tejido", "confeccion", "crochet", "telar")
    );

    companion object {
        fun desdeCodigo(codigo: String?): Rubro? = entries.firstOrNull { it.name == codigo }

        // En la planilla antigua los rubros venian escritos a mano, por eso buscamos por palabras clave
        fun desdeTexto(texto: String): Rubro? {
            val limpio = TextoUtil.normalizar(texto)
            if (limpio.isBlank()) return null
            return entries.firstOrNull { it.name.equals(limpio, ignoreCase = true) }
                ?: entries.firstOrNull { rubro -> rubro.palabrasClave.any { limpio.contains(it) } }
        }
    }
}
