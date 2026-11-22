package cl.duoc.redtextil.util

data class RegionChile(
    val codigo: String,
    val numero: String,
    val nombre: String,
    val comunas: List<String>
)

object ChileGeoData {

    val regiones: List<RegionChile> = listOf(
        RegionChile(
            "AP", "XV", "Arica y Parinacota",
            listOf("Arica", "Camarones", "General Lagos", "Putre")
        ),
        RegionChile(
            "TA", "I", "Tarapacá",
            listOf("Alto Hospicio", "Camiña", "Colchane", "Huara", "Iquique", "Pica", "Pozo Almonte")
        ),
        RegionChile(
            "AN", "II", "Antofagasta",
            listOf(
                "Antofagasta", "Calama", "María Elena", "Mejillones", "Ollagüe",
                "San Pedro de Atacama", "Sierra Gorda", "Taltal", "Tocopilla"
            )
        ),
        RegionChile(
            "AT", "III", "Atacama",
            listOf(
                "Alto del Carmen", "Caldera", "Chañaral", "Copiapó", "Diego de Almagro",
                "Freirina", "Huasco", "Tierra Amarilla", "Vallenar"
            )
        ),
        RegionChile(
            "CO", "IV", "Coquimbo",
            listOf(
                "Andacollo", "Canela", "Combarbalá", "Coquimbo", "Illapel", "La Higuera",
                "La Serena", "Los Vilos", "Monte Patria", "Ovalle", "Paihuano", "Punitaqui",
                "Río Hurtado", "Salamanca", "Vicuña"
            )
        ),
        RegionChile(
            "VA", "V", "Valparaíso",
            listOf(
                "Algarrobo", "Cabildo", "Calle Larga", "Cartagena", "Casablanca", "Catemu",
                "Concón", "El Quisco", "El Tabo", "Hijuelas", "Isla de Pascua", "Juan Fernández",
                "La Calera", "La Cruz", "La Ligua", "Limache", "Llaillay", "Los Andes", "Nogales",
                "Olmué", "Panquehue", "Papudo", "Petorca", "Puchuncaví", "Putaendo", "Quillota",
                "Quilpué", "Quintero", "Rinconada", "San Antonio", "San Esteban", "San Felipe",
                "Santa María", "Santo Domingo", "Valparaíso", "Villa Alemana", "Viña del Mar",
                "Zapallar"
            )
        ),
        RegionChile(
            "RM", "RM", "Metropolitana",
            listOf(
                "Alhué", "Buin", "Calera de Tango", "Cerrillos", "Cerro Navia", "Colina",
                "Conchalí", "Curacaví", "El Bosque", "El Monte", "Estación Central", "Huechuraba",
                "Independencia", "Isla de Maipo", "La Cisterna", "La Florida", "La Granja",
                "La Pintana", "La Reina", "Lampa", "Las Condes", "Lo Barnechea", "Lo Espejo",
                "Lo Prado", "Macul", "Maipú", "María Pinto", "Melipilla", "Ñuñoa", "Padre Hurtado",
                "Paine", "Pedro Aguirre Cerda", "Peñaflor", "Peñalolén", "Pirque", "Providencia",
                "Pudahuel", "Puente Alto", "Quilicura", "Quinta Normal", "Recoleta", "Renca",
                "San Bernardo", "San Joaquín", "San José de Maipo", "San Miguel", "San Pedro",
                "San Ramón", "Santiago", "Talagante", "Tiltil", "Vitacura"
            )
        ),
        RegionChile(
            "LI", "VI", "O'Higgins",
            listOf(
                "Chépica", "Chimbarongo", "Codegua", "Coinco", "Coltauco", "Doñihue", "Graneros",
                "La Estrella", "Las Cabras", "Litueche", "Lolol", "Machalí", "Malloa", "Marchigüe",
                "Mostazal", "Nancagua", "Navidad", "Olivar", "Palmilla", "Paredones", "Peralillo",
                "Peumo", "Pichidegua", "Pichilemu", "Placilla", "Pumanque", "Quinta de Tilcoco",
                "Rancagua", "Rengo", "Requínoa", "San Fernando", "San Vicente", "Santa Cruz"
            )
        ),
        RegionChile(
            "ML", "VII", "Maule",
            listOf(
                "Cauquenes", "Chanco", "Colbún", "Constitución", "Curepto", "Curicó", "Empedrado",
                "Hualañé", "Licantén", "Linares", "Longaví", "Maule", "Molina", "Parral", "Pelarco",
                "Pelluhue", "Pencahue", "Rauco", "Retiro", "Río Claro", "Romeral",
                "Sagrada Familia", "San Clemente", "San Javier", "San Rafael", "Talca", "Teno",
                "Vichuquén", "Villa Alegre", "Yerbas Buenas"
            )
        ),
        RegionChile(
            "NB", "XVI", "Ñuble",
            listOf(
                "Bulnes", "Chillán", "Chillán Viejo", "Cobquecura", "Coelemu", "Coihueco",
                "El Carmen", "Ninhue", "Ñiquén", "Pemuco", "Pinto", "Portezuelo", "Quillón",
                "Quirihue", "Ránquil", "San Carlos", "San Fabián", "San Ignacio", "San Nicolás",
                "Treguaco", "Yungay"
            )
        ),
        RegionChile(
            "BI", "VIII", "Biobío",
            listOf(
                "Alto Biobío", "Antuco", "Arauco", "Cabrero", "Cañete", "Chiguayante", "Concepción",
                "Contulmo", "Coronel", "Curanilahue", "Florida", "Hualpén", "Hualqui", "Laja",
                "Lebu", "Los Álamos", "Los Ángeles", "Lota", "Mulchén", "Nacimiento", "Negrete",
                "Penco", "Quilaco", "Quilleco", "San Pedro de la Paz", "San Rosendo",
                "Santa Bárbara", "Santa Juana", "Talcahuano", "Tirúa", "Tomé", "Tucapel", "Yumbel"
            )
        ),
        RegionChile(
            "AR", "IX", "La Araucanía",
            listOf(
                "Angol", "Carahue", "Cholchol", "Collipulli", "Cunco", "Curacautín", "Curarrehue",
                "Ercilla", "Freire", "Galvarino", "Gorbea", "Lautaro", "Loncoche", "Lonquimay",
                "Los Sauces", "Lumaco", "Melipeuco", "Nueva Imperial", "Padre Las Casas",
                "Perquenco", "Pitrufquén", "Pucón", "Purén", "Renaico", "Saavedra", "Temuco",
                "Teodoro Schmidt", "Toltén", "Traiguén", "Victoria", "Vilcún", "Villarrica"
            )
        ),
        RegionChile(
            "LR", "XIV", "Los Ríos",
            listOf(
                "Corral", "Futrono", "La Unión", "Lago Ranco", "Lanco", "Los Lagos", "Máfil",
                "Mariquina", "Paillaco", "Panguipulli", "Río Bueno", "Valdivia"
            )
        ),
        RegionChile(
            "LL", "X", "Los Lagos",
            listOf(
                "Ancud", "Calbuco", "Castro", "Chaitén", "Chonchi", "Cochamó", "Curaco de Vélez",
                "Dalcahue", "Fresia", "Frutillar", "Futaleufú", "Hualaihué", "Llanquihue",
                "Los Muermos", "Maullín", "Osorno", "Palena", "Puerto Montt", "Puerto Octay",
                "Puerto Varas", "Puqueldón", "Purranque", "Puyehue", "Queilén", "Quellón",
                "Quemchi", "Quinchao", "Río Negro", "San Juan de la Costa", "San Pablo"
            )
        ),
        RegionChile(
            "AI", "XI", "Aysén",
            listOf(
                "Aysén", "Chile Chico", "Cisnes", "Cochrane", "Coyhaique", "Guaitecas",
                "Lago Verde", "O'Higgins", "Río Ibáñez", "Tortel"
            )
        ),
        RegionChile(
            "MA", "XII", "Magallanes",
            listOf(
                "Antártica", "Cabo de Hornos", "Laguna Blanca", "Natales", "Porvenir", "Primavera",
                "Punta Arenas", "Río Verde", "San Gregorio", "Timaukel", "Torres del Paine"
            )
        )
    )

    val nombresRegiones: List<String> = regiones.map { it.nombre }

    val totalComunas: Int = regiones.sumOf { it.comunas.size }

    private val indiceComunas: Map<String, Pair<String, RegionChile>> =
        regiones.flatMap { region -> region.comunas.map { TextoUtil.normalizar(it) to (it to region) } }.toMap()

    fun comunasDe(region: String): List<String> =
        regiones.firstOrNull { it.nombre == region }?.comunas ?: emptyList()

    fun existeRegion(region: String): Boolean = regiones.any { it.nombre == region }

    fun comunaPerteneceARegion(comuna: String, region: String): Boolean =
        comunasDe(region).contains(comuna)

    fun regionPorCodigo(codigo: String): RegionChile? = regiones.firstOrNull { it.codigo == codigo }

    // Acepta "Valparaíso", "region de valparaiso", "V" o "VA", tal como venia escrito en la planilla antigua
    fun buscarRegion(texto: String): RegionChile? {
        val limpio = TextoUtil.normalizar(texto)
            .removePrefix("region de ").removePrefix("region del ").removePrefix("region ")
            .trim()
        if (limpio.isEmpty()) return null
        return regiones.firstOrNull { TextoUtil.normalizar(it.nombre) == limpio }
            ?: regiones.firstOrNull { it.codigo.equals(limpio, ignoreCase = true) || it.numero.equals(limpio, ignoreCase = true) }
            ?: regiones.firstOrNull { TextoUtil.normalizar(it.nombre).contains(limpio) || limpio.contains(TextoUtil.normalizar(it.nombre)) }
    }

    fun buscarComuna(texto: String): Pair<String, RegionChile>? = indiceComunas[TextoUtil.normalizar(texto)]

    fun indiceGeografico(region: String): Int = regiones.indexOfFirst { it.nombre == region }.let { if (it < 0) 99 else it }
}
