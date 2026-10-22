package cl.duoc.redtextil.data.local.database

import android.content.Context
import androidx.room.withTransaction
import cl.duoc.redtextil.data.local.entity.ConsultaEntity
import cl.duoc.redtextil.data.local.entity.CorreoEntity
import cl.duoc.redtextil.data.local.entity.EmprendimientoEntity
import cl.duoc.redtextil.data.local.entity.InteraccionEntity
import cl.duoc.redtextil.data.local.entity.PublicacionEntity
import cl.duoc.redtextil.domain.model.CategoriaTrabajo
import cl.duoc.redtextil.domain.model.EstadoConsulta
import cl.duoc.redtextil.domain.model.EstadoCorreo
import cl.duoc.redtextil.domain.model.EstadoFicha
import cl.duoc.redtextil.domain.model.EstadoSync
import cl.duoc.redtextil.domain.model.OrigenFicha
import cl.duoc.redtextil.domain.model.PlantillasCorreo
import cl.duoc.redtextil.domain.model.Rubro
import cl.duoc.redtextil.domain.model.TipoCorreo
import cl.duoc.redtextil.domain.model.TipoInteraccion
import cl.duoc.redtextil.domain.validation.ReglasComunes
import cl.duoc.redtextil.util.ChileGeoData
import cl.duoc.redtextil.util.TextoUtil
import kotlin.random.Random

// Todos los datos son ficticios (Ley 19.628 y Ley 21.719): telefonos +5690000XXXX y correos @example.com
object DatosSemilla {

    private const val DIA = 24L * 60 * 60 * 1000

    private data class TrabajoSemilla(
        val titulo: String,
        val categoria: CategoriaTrabajo,
        val tecnica: String,
        val precio: Int,
        val descripcion: String
    )

    suspend fun poblarSiEstaVacia(db: AppDatabase, context: Context) {
        if (db.emprendimientoDao().contar() > 0) return
        val ahora = System.currentTimeMillis()
        val fichas = fichasDemo(ahora)

        // Las fotos demo se dibujan antes de abrir la transaccion para no tener la base bloqueada mientras tanto
        var indiceImagen = 0
        val fotos = fichas.indices.map { i ->
            trabajosDemo(i).map { trabajo ->
                runCatching { ImagenesSemilla.generar(context, trabajo.categoria, indiceImagen++) }.getOrNull()
            }
        }

        db.withTransaction {
            val ids = fichas.map { db.emprendimientoDao().insertar(it) }
            val guardadas = fichas.mapIndexed { i, ficha -> ficha.copy(id = ids[i]) }

            val publicaciones = mutableListOf<PublicacionEntity>()
            guardadas.forEachIndexed { i, ficha ->
                trabajosDemo(i).forEachIndexed { j, trabajo ->
                    val ruta = fotos[i].getOrNull(j)
                    publicaciones.add(
                        PublicacionEntity(
                            emprendimientoId = ficha.id,
                            titulo = trabajo.titulo,
                            categoria = trabajo.categoria,
                            tecnica = trabajo.tecnica,
                            precioReferencial = trabajo.precio,
                            descripcion = trabajo.descripcion,
                            fotoRuta = ruta,
                            kbOriginal = 2400L + j * 310,
                            kbOptimizado = 96L + j * 12,
                            fotoSincronizada = true,
                            estadoSync = EstadoSync.SINCRONIZADO,
                            remoteId = 5000L + publicaciones.size,
                            fechaCreacion = ahora - (i + j + 1) * DIA,
                            fechaActualizacion = ahora - (i + j + 1) * DIA
                        )
                    )
                }
            }
            db.publicacionDao().insertarLote(publicaciones)
            db.consultaDao().insertarLote(consultasDemo(guardadas, ahora))
            db.interaccionDao().insertarLote(interaccionesDemo(guardadas, ahora))
            db.correoDao().insertarLote(correosDemo(guardadas, ahora))
        }
    }

    private fun rutDemo(cuerpo: Int): String {
        val texto = cuerpo.toString()
        return ReglasComunes.formatearRut(texto + ReglasComunes.calcularDigitoVerificador(texto))
    }

    private fun fonoDemo(numero: Int): String = "+5690000" + numero.toString().padStart(4, '0')

    private fun correoDemo(nombre: String): String {
        val partes = TextoUtil.normalizar(nombre).split(" ")
        return "${partes.first()}.${partes.getOrElse(1) { "demo" }}@example.com".replace("'", "")
    }

    private fun base(
        i: Int,
        nombre: String,
        comercial: String,
        region: String,
        comuna: String,
        rubros: List<Rubro>,
        ahora: Long
    ) = EmprendimientoEntity(
        nombreEmprendedora = nombre,
        rut = rutDemo(31_100_000 + i * 7919),
        fechaNacimiento = "%02d/%02d/%d".format(1 + (i * 7) % 28, 1 + (i * 5) % 12, 1965 + (i * 3) % 35),
        nacionalidad = if (i % 9 == 4) "Extranjera residente" else "Chilena",
        correo = correoDemo(nombre),
        telefono = fonoDemo(100 + i),
        jefaHogar = listOf("Sí", "No", "Prefiero no decir")[i % 3],
        nombreComercial = comercial,
        region = region,
        comuna = comuna,
        rubroPrincipal = rubros.getOrNull(0)?.name.orEmpty(),
        rubroSecundario = rubros.getOrNull(1)?.name.orEmpty(),
        whatsapp = fonoDemo(100 + i),
        aceptaPublicacion = true,
        aceptaPrivacidad = true,
        estado = EstadoFicha.ACTIVA,
        origen = OrigenFicha.SEMILLA,
        estadoSync = EstadoSync.SINCRONIZADO,
        remoteId = 1000L + i,
        fechaRegistro = ahora - (60 - i) * DIA,
        fechaActualizacion = ahora - (30 - i) * DIA
    )

    private fun fichasDemo(ahora: Long): List<EmprendimientoEntity> = listOf(
        base(1, "Camila Rojas Fuentes", "Renace Textil", "Valparaíso", "Valparaíso",
            listOf(Rubro.SUPRARRECICLAJE, Rubro.ACCESORIOS), ahora).copy(
            descripcion = "Transformamos poleras, jeans y cortinas en desuso en bolsos, chaquetas y accesorios únicos. Trabajamos con ropa donada por vecinas del cerro.",
            tecnicas = "Patchwork, bordado a mano y teñido natural",
            instagram = "@renace.textil.demo",
            anioInicio = 2019, lugarTrabajo = "Taller en mi casa", cantidadPersonas = 2,
            formalizado = true, rutEmpresa = rutDemo(31_107_919), tipoFormalizacion = "Persona natural con inicio de actividades",
            atencionPresencial = true, direccionTaller = "Pasaje Las Gaviotas 245, Cerro Alegre", realizaEnvios = true,
            recibeDonaciones = true, tiposDonacion = "Jeans, poleras de algodón y retazos de tela",
            horarioAtencion = "Lunes a viernes de 10:00 a 18:00"
        ),
        base(2, "Valentina Muñoz Soto", "Aguja y Mar", "Valparaíso", "Viña del Mar",
            listOf(Rubro.REPARACION), ahora).copy(
            descripcion = "Arreglos de ropa en el día: bastas, cierres, ajustes de talla y reparación de chaquetas. Le damos una segunda vida a tu ropa favorita.",
            tecnicas = "Costura recta, overlock y zurcido invisible",
            instagram = "@agujaymar.demo",
            anioInicio = 2016, lugarTrabajo = "Local comercial", cantidadPersonas = 3,
            formalizado = true, rutEmpresa = rutDemo(76_100_200), tipoFormalizacion = "Empresa Individual (EIRL)",
            atencionPresencial = true, direccionTaller = "Avenida Libertad 1180, local 4", realizaEnvios = false,
            horarioAtencion = "Lunes a sábado de 09:30 a 19:00"
        ),
        base(3, "Francisca Díaz Herrera", "Ropero Circular", "Valparaíso", "Quilpué",
            listOf(Rubro.SEGUNDA_MANO), ahora).copy(
            descripcion = "Ropa de segunda mano seleccionada, lavada y reparada. Tallas para toda la familia a precios justos.",
            tecnicas = "Selección, lavado y reparación de prendas",
            instagram = "@roperocircular.demo",
            anioInicio = 2021, lugarTrabajo = "Feria o persa", cantidadPersonas = 1,
            atencionPresencial = true, direccionTaller = "Feria Los Carrera 300, puesto 112", realizaEnvios = true,
            recibeDonaciones = true, tiposDonacion = "Ropa de adulto y niño en buen estado",
            horarioAtencion = "Sábado y domingo de 09:00 a 15:00"
        ),
        base(4, "Javiera González Pérez", "Vístete de Nuevo", "Metropolitana", "Providencia",
            listOf(Rubro.ARRIENDO, Rubro.SEGUNDA_MANO), ahora).copy(
            descripcion = "Arriendo de vestidos de fiesta, trajes y ropa para eventos. Todas las prendas se revisan y ajustan antes de cada arriendo.",
            tecnicas = "Ajuste de tallas y planchado profesional",
            instagram = "@vistetedenuevo.demo",
            anioInicio = 2018, lugarTrabajo = "Local comercial", cantidadPersonas = 4,
            formalizado = true, rutEmpresa = rutDemo(77_300_450), tipoFormalizacion = "Sociedad por Acciones (SpA)",
            atencionPresencial = true, direccionTaller = "Avenida Providencia 2133, oficina 12", realizaEnvios = true,
            tallasArriendo = "XS, S, M, L, XL y tallas grandes",
            horarioAtencion = "Martes a sábado de 11:00 a 20:00"
        ),
        base(5, "Rosa Contreras Vidal", "Hilando Saberes", "Biobío", "Concepción",
            listOf(Rubro.TALLERES, Rubro.TEJIDO), ahora).copy(
            descripcion = "Talleres de tejido a palillo, crochet y reparación de ropa para vecinas y estudiantes. Enseñamos a reutilizar lana de prendas antiguas.",
            tecnicas = "Crochet, palillo y telar decorativo",
            instagram = "@hilandosaberes.demo",
            anioInicio = 2015, lugarTrabajo = "Taller en mi casa", cantidadPersonas = 2,
            atencionPresencial = true, direccionTaller = "Calle Freire 845, Concepción", realizaEnvios = false,
            cuposTaller = 12, modalidadTaller = "Mixta",
            horarioAtencion = "Miércoles y viernes de 15:00 a 19:00"
        ),
        base(6, "Margarita Álvarez Barría", "Lana del Fin del Mundo", "Magallanes", "Punta Arenas",
            listOf(Rubro.TEJIDO), ahora).copy(
            descripcion = "Gorros, bufandas y chalecos tejidos con lana de oveja magallánica reutilizada y teñida con plantas de la zona.",
            tecnicas = "Hilado artesanal, teñido natural y palillo",
            instagram = "@lanadelfindelmundo.demo",
            anioInicio = 2012, lugarTrabajo = "Solo venta online", cantidadPersonas = 1,
            formalizado = true, rutEmpresa = rutDemo(31_147_514), tipoFormalizacion = "Persona natural con inicio de actividades",
            atencionPresencial = false, realizaEnvios = true,
            horarioAtencion = "Todos los días por WhatsApp de 10:00 a 21:00"
        ),
        base(7, "Daniela Castillo Araya", "Telar Elqui", "Coquimbo", "La Serena",
            listOf(Rubro.TEJIDO, Rubro.ACCESORIOS), ahora).copy(
            descripcion = "Tejidos en telar con fibras recuperadas: caminos de mesa, bolsos y aros hechos con retazos de tela.",
            tecnicas = "Telar maría, macramé y trenzado",
            instagram = "@telarelqui.demo",
            anioInicio = 2020, lugarTrabajo = "Feria o persa", cantidadPersonas = 2,
            atencionPresencial = true, direccionTaller = "Recova de La Serena, local 33", realizaEnvios = true,
            horarioAtencion = "Lunes a sábado de 10:00 a 18:30"
        ),
        base(8, "Ana Huenchullán Painemal", "Kimün Textil", "La Araucanía", "Temuco",
            listOf(Rubro.TEJIDO, Rubro.TALLERES), ahora).copy(
            descripcion = "Textilería mapuche en telar witral y talleres para aprender a tejer con lana reciclada de la zona.",
            tecnicas = "Telar witral y teñido con plantas nativas",
            instagram = "@kimuntextil.demo",
            anioInicio = 2010, lugarTrabajo = "Taller en mi casa", cantidadPersonas = 5,
            formalizado = true, rutEmpresa = rutDemo(65_200_310), tipoFormalizacion = "Cooperativa",
            atencionPresencial = true, direccionTaller = "Pasaje Los Copihues 1420, Temuco", realizaEnvios = true,
            cuposTaller = 8, modalidadTaller = "Presencial",
            horarioAtencion = "Martes a sábado de 10:00 a 17:00"
        ),
        base(9, "Paula Oyarzún Mansilla", "Remiendo Sureño", "Los Lagos", "Puerto Montt",
            listOf(Rubro.REPARACION, Rubro.SUPRARRECICLAJE), ahora).copy(
            descripcion = "Reparamos parkas, impermeables y ropa de trabajo. Lo que no se puede reparar lo transformamos en bolsos y estuches.",
            tecnicas = "Costura industrial, sellado de costuras y parches",
            instagram = "@remiendosureno.demo",
            anioInicio = 2017, lugarTrabajo = "Local comercial", cantidadPersonas = 2,
            formalizado = true, rutEmpresa = rutDemo(31_171_271), tipoFormalizacion = "Persona natural con inicio de actividades",
            atencionPresencial = true, direccionTaller = "Calle Urmeneta 560, Puerto Montt", realizaEnvios = true,
            recibeDonaciones = true, tiposDonacion = "Parkas y chaquetas en desuso",
            horarioAtencion = "Lunes a viernes de 09:00 a 18:00"
        ),
        base(10, "Catalina Flores Mamani", "Desierto Upcycling", "Antofagasta", "Antofagasta",
            listOf(Rubro.SUPRARRECICLAJE), ahora).copy(
            descripcion = "Diseño de ropa con prendas rescatadas del desierto de Atacama. Cada pieza es única y cuenta una historia.",
            tecnicas = "Patchwork, estampado y deconstrucción de prendas",
            instagram = "@desiertoupcycling.demo",
            anioInicio = 2022, lugarTrabajo = "Solo venta online", cantidadPersonas = 3,
            formalizado = true, rutEmpresa = rutDemo(77_810_020), tipoFormalizacion = "Sociedad por Acciones (SpA)",
            atencionPresencial = false, realizaEnvios = true,
            recibeDonaciones = false,
            sitioWeb = "https://www.example.com/desierto-upcycling",
            horarioAtencion = "Lunes a viernes de 10:00 a 19:00"
        ),
        base(11, "Fernanda Reyes Tapia", "Moda Segunda Vuelta", "Arica y Parinacota", "Arica",
            listOf(Rubro.SEGUNDA_MANO), ahora).copy(
            descripcion = "Tienda de ropa usada seleccionada pieza por pieza. También hacemos cambios y trueque de ropa.",
            tecnicas = "Selección y restauración de prendas",
            instagram = "@modasegundavuelta.demo",
            anioInicio = 2023, lugarTrabajo = "Feria o persa", cantidadPersonas = 1,
            atencionPresencial = true, direccionTaller = "Feria Máximo Lira, puesto 58", realizaEnvios = false,
            recibeDonaciones = true, tiposDonacion = "Ropa de verano y calzado",
            horarioAtencion = "Jueves a domingo de 10:00 a 16:00"
        ),
        base(12, "Marcela Pino Sepúlveda", "Costura Viva", "Maule", "Talca",
            listOf(Rubro.REPARACION, Rubro.TALLERES), ahora).copy(
            descripcion = "Arreglos de ropa y talleres de costura básica para que aprendas a reparar tu propia ropa.",
            tecnicas = "Máquina recta, overlock y bordado",
            instagram = "@costuraviva.demo",
            anioInicio = 2019, lugarTrabajo = "Taller en mi casa", cantidadPersonas = 1,
            atencionPresencial = true, direccionTaller = "Calle 4 Norte 1320, Talca", realizaEnvios = false,
            cuposTaller = 10, modalidadTaller = "Presencial",
            horarioAtencion = "Lunes a viernes de 14:00 a 20:00"
        ),
        base(13, "Constanza Vera Schmidt", "Retazos de Valdivia", "Los Ríos", "Valdivia",
            listOf(Rubro.ACCESORIOS), ahora).copy(
            descripcion = "Accesorios de ecodiseño: cintillos, bolsos y estuches hechos con retazos de tela que descartan los talleres.",
            tecnicas = "Patchwork y costura a mano",
            instagram = "@retazosdevaldivia.demo",
            anioInicio = 2021, lugarTrabajo = "Solo venta online", cantidadPersonas = 1,
            formalizado = true, rutEmpresa = rutDemo(31_202_947), tipoFormalizacion = "Persona natural con inicio de actividades",
            atencionPresencial = false, realizaEnvios = true,
            horarioAtencion = "Lunes a sábado de 10:00 a 20:00"
        ),
        base(14, "Carolina Bravo Núñez", "Trapitos al Sol", "O'Higgins", "Rancagua",
            listOf(Rubro.ARRIENDO, Rubro.SEGUNDA_MANO), ahora).copy(
            descripcion = "Arriendo de disfraces y ropa de época para colegios y eventos, además de venta de ropa infantil usada.",
            tecnicas = "Confección de disfraces y ajustes",
            instagram = "@trapitosalsol.demo",
            anioInicio = 2018, lugarTrabajo = "Taller en mi casa", cantidadPersonas = 2,
            atencionPresencial = true, direccionTaller = "Pasaje Los Aromos 77, Rancagua", realizaEnvios = true,
            tallasArriendo = "Infantil 2 a 16 y adulto S a XL",
            recibeDonaciones = true, tiposDonacion = "Disfraces y ropa infantil",
            horarioAtencion = "Lunes a sábado de 10:00 a 19:00"
        ),
        base(15, "Gabriela Navarro Cárcamo", "Patagonia Reteje", "Aysén", "Coyhaique",
            listOf(Rubro.TEJIDO), ahora).copy(
            descripcion = "Tejemos chalecos y frazadas con lana recuperada de prendas antiguas. Pedidos a medida para todo Chile.",
            tecnicas = "Palillo, crochet y telar",
            anioInicio = 2014, lugarTrabajo = "Taller en mi casa", cantidadPersonas = 1,
            atencionPresencial = false, realizaEnvios = true,
            horarioAtencion = "Lunes a viernes de 09:00 a 13:00"
        ),
        base(16, "Patricia Leiva Cortés", "Mujeres que Reparan", "Metropolitana", "Puente Alto",
            listOf(Rubro.REPARACION, Rubro.SEGUNDA_MANO), ahora).copy(
            descripcion = "Taller comunitario de reparación de ropa y venta de prendas recuperadas en Puente Alto.",
            tecnicas = "Costura recta, overlock y zurcido",
            instagram = "@mujeresquereparan.demo",
            anioInicio = 2020, lugarTrabajo = "Local comercial", cantidadPersonas = 6,
            formalizado = true, rutEmpresa = rutDemo(65_330_870), tipoFormalizacion = "Cooperativa",
            atencionPresencial = true, direccionTaller = "Avenida Concha y Toro 3050, Puente Alto", realizaEnvios = true,
            recibeDonaciones = true, tiposDonacion = "Ropa en desuso para reparar",
            horarioAtencion = "Lunes a sábado de 10:00 a 19:00"
        ),
        base(17, "Isidora Sanhueza Lagos", "Ñuble Circular", "Ñuble", "Chillán",
            listOf(Rubro.SUPRARRECICLAJE, Rubro.REPARACION), ahora).copy(
            descripcion = "Recién empezando: transformo ropa de mis vecinas en prendas nuevas y hago arreglos simples.",
            tecnicas = "Costura recta y bordado",
            instagram = "@nublecircular.demo",
            anioInicio = 2025, lugarTrabajo = "Taller en mi casa", cantidadPersonas = 1,
            atencionPresencial = true, direccionTaller = "Calle Arauco 455, Chillán", realizaEnvios = false,
            horarioAtencion = "Sábados de 10:00 a 14:00",
            estado = EstadoFicha.PENDIENTE
        ),
        base(18, "Lorena Godoy Rivera", "Atelier Copiapó", "Atacama", "Copiapó",
            listOf(Rubro.REPARACION), ahora).copy(
            descripcion = "Arreglos y confección a medida de uniformes y ropa de trabajo para la minería y el comercio.",
            tecnicas = "Confección a medida",
            instagram = "www.instagran.com/atelier copiapo",
            anioInicio = 2013, lugarTrabajo = "Local comercial", cantidadPersonas = 2,
            atencionPresencial = true, direccionTaller = "Calle Atacama 789, Copiapó", realizaEnvios = false,
            horarioAtencion = "Lunes a viernes de 09:00 a 18:00",
            estado = EstadoFicha.OBSERVADA,
            observacion = "El enlace de Instagram no funciona. Corrige tu usuario para que los clientes puedan ver tu trabajo."
        ),
        base(19, "Elena Mamani Choque", "Tejidos Pica", "Tarapacá", "Pica",
            listOf(Rubro.TEJIDO), ahora).copy(
            descripcion = "Tejidos de alpaca y lana reciclada hechos a mano en el oasis de Pica.",
            tecnicas = "Telar de pedal y palillo",
            instagram = "@tejidospica.demo",
            anioInicio = 2011, lugarTrabajo = "Feria o persa", cantidadPersonas = 3,
            atencionPresencial = true, direccionTaller = "Calle Balmaceda 120, Pica", realizaEnvios = true,
            horarioAtencion = "Viernes a domingo de 10:00 a 18:00",
            estado = EstadoFicha.PENDIENTE
        ),
        base(20, "Sofía Morales Espinoza", "Puntada Verde", "Valparaíso", "Concón",
            listOf(Rubro.TALLERES), ahora).copy(
            descripcion = "Talleres de costura sustentable para niñas y niños. Pausado temporalmente por vacaciones.",
            tecnicas = "Costura básica y reciclaje creativo",
            instagram = "@puntadaverde.demo",
            anioInicio = 2019, lugarTrabajo = "Taller en mi casa", cantidadPersonas = 1,
            atencionPresencial = true, direccionTaller = "Avenida Borgoño 22100, Concón", realizaEnvios = false,
            cuposTaller = 15, modalidadTaller = "Presencial",
            horarioAtencion = "Sábados de 10:00 a 12:00",
            estado = EstadoFicha.BAJA
        )
    )

    private fun trabajosDemo(indice: Int): List<TrabajoSemilla> = when (indice) {
        0 -> listOf(
            TrabajoSemilla("Bolso de jeans reciclado", CategoriaTrabajo.ACCESORIO, "Patchwork", 18000,
                "Bolso grande hecho con 3 jeans en desuso, forro de algodón y bolsillo interior."),
            TrabajoSemilla("Chaqueta intervenida con bordado", CategoriaTrabajo.PRENDA_INTERVENIDA, "Bordado a mano", 35000,
                "Chaqueta de mezclilla recuperada con bordado floral hecho a mano."),
            TrabajoSemilla("Polera teñida con palta", CategoriaTrabajo.PRENDA_INTERVENIDA, "Teñido natural", 12000,
                "Polera de algodón teñida con cáscara y cuesco de palta. Color rosado suave.")
        )
        1 -> listOf(
            TrabajoSemilla("Ajuste de basta y cintura", CategoriaTrabajo.ARREGLO, "Costura recta", 6000,
                "Ajuste de pantalones de tela o jeans. Entrega en 24 horas."),
            TrabajoSemilla("Cambio de cierre de parka", CategoriaTrabajo.ARREGLO, "Overlock", 9000,
                "Cambio de cierre completo en parkas y chaquetas, con cierre de buena calidad.")
        )
        2 -> listOf(
            TrabajoSemilla("Pack ropa de niño talla 6", CategoriaTrabajo.SEGUNDA_MANO, "Selección y lavado", 12000,
                "Pack de 5 prendas de niño en excelente estado, lavadas y revisadas."),
            TrabajoSemilla("Abrigo de lana vintage", CategoriaTrabajo.SEGUNDA_MANO, "Restauración", 22000,
                "Abrigo de lana talla M, restaurado y con botones nuevos.")
        )
        3 -> listOf(
            TrabajoSemilla("Vestido de fiesta largo", CategoriaTrabajo.ARRIENDO, "Ajuste de talla", 25000,
                "Arriendo por 3 días. Incluye ajuste de basta y planchado."),
            TrabajoSemilla("Traje de dos piezas", CategoriaTrabajo.ARRIENDO, "Planchado profesional", 30000,
                "Traje en tallas M y L, ideal para matrimonios y graduaciones.")
        )
        4 -> listOf(
            TrabajoSemilla("Taller de crochet para principiantes", CategoriaTrabajo.TALLER, "Crochet", 15000,
                "4 sesiones de 2 horas. Incluye materiales reciclados."),
            TrabajoSemilla("Bufanda tejida a palillo", CategoriaTrabajo.TEJIDO, "Palillo", 14000,
                "Bufanda de lana reutilizada en colores tierra.")
        )
        5 -> listOf(
            TrabajoSemilla("Gorro de lana magallánica", CategoriaTrabajo.TEJIDO, "Palillo", 16000,
                "Gorro doble capa, ideal para el frío del sur."),
            TrabajoSemilla("Chaleco teñido con calafate", CategoriaTrabajo.TEJIDO, "Teñido natural", 42000,
                "Chaleco tejido a mano con lana teñida con calafate.")
        )
        6 -> listOf(
            TrabajoSemilla("Camino de mesa en telar", CategoriaTrabajo.TEJIDO, "Telar maría", 20000,
                "Camino de mesa de 1,2 metros tejido con hilos recuperados."),
            TrabajoSemilla("Aros de retazos trenzados", CategoriaTrabajo.ACCESORIO, "Trenzado", 5000,
                "Aros livianos hechos con retazos de tela trenzados.")
        )
        7 -> listOf(
            TrabajoSemilla("Taller de telar witral", CategoriaTrabajo.TALLER, "Telar witral", 20000,
                "Taller de 3 sesiones para aprender técnicas de telar mapuche."),
            TrabajoSemilla("Faja tejida en witral", CategoriaTrabajo.TEJIDO, "Telar witral", 28000,
                "Faja tradicional tejida con lana reciclada y teñida con plantas.")
        )
        8 -> listOf(
            TrabajoSemilla("Reparación de parka impermeable", CategoriaTrabajo.ARREGLO, "Sellado de costuras", 12000,
                "Reparación de roturas y sellado de costuras en parkas."),
            TrabajoSemilla("Estuche de parka reciclada", CategoriaTrabajo.PRENDA_INTERVENIDA, "Costura industrial", 8000,
                "Estuche impermeable hecho con parkas que ya no se podían reparar.")
        )
        9 -> listOf(
            TrabajoSemilla("Chaqueta patchwork del desierto", CategoriaTrabajo.PRENDA_INTERVENIDA, "Patchwork", 45000,
                "Chaqueta única hecha con prendas rescatadas del desierto de Atacama."),
            TrabajoSemilla("Polerón deconstruido", CategoriaTrabajo.PRENDA_INTERVENIDA, "Deconstrucción", 28000,
                "Polerón intervenido con paneles de otras prendas y estampado a mano.")
        )
        10 -> listOf(
            TrabajoSemilla("Camisas de verano", CategoriaTrabajo.SEGUNDA_MANO, "Selección", 4000,
                "Camisas de lino y algodón en buen estado, varias tallas.")
        )
        11 -> listOf(
            TrabajoSemilla("Taller de costura básica", CategoriaTrabajo.TALLER, "Máquina recta", 12000,
                "Aprende a coser bastas, botones y cierres en 3 clases.")
        )
        12 -> listOf(
            TrabajoSemilla("Cintillo de retazos", CategoriaTrabajo.ACCESORIO, "Patchwork", 4500,
                "Cintillo acolchado hecho con retazos de algodón."),
            TrabajoSemilla("Bolso tote de tela recuperada", CategoriaTrabajo.ACCESORIO, "Costura a mano", 11000,
                "Bolso tote resistente hecho con retazos de tapicería.")
        )
        13 -> listOf(
            TrabajoSemilla("Disfraz de época infantil", CategoriaTrabajo.ARRIENDO, "Confección", 8000,
                "Arriendo de disfraces para actos escolares, tallas 4 a 14.")
        )
        15 -> listOf(
            TrabajoSemilla("Reparación de jeans", CategoriaTrabajo.ARREGLO, "Zurcido", 5000,
                "Reparación de roturas en jeans con zurcido y parches de tela recuperada.")
        )
        else -> emptyList()
    }

    private fun consultasDemo(fichas: List<EmprendimientoEntity>, ahora: Long): List<ConsultaEntity> {
        val renace = fichas[0]
        val aguja = fichas[1]
        val hilando = fichas[4]
        return listOf(
            ConsultaEntity(
                emprendimientoId = renace.id, nombreCliente = "Marta Silva", correoCliente = "marta.silva@example.com",
                telefonoCliente = fonoDemo(5101), servicio = Rubro.ACCESORIOS.etiqueta,
                mensaje = "Hola, ¿pueden hacer un bolso con jeans que tengo en casa? Serían 4 jeans de mi familia.",
                enviadaDesdeApp = false, estadoSync = EstadoSync.SINCRONIZADO, remoteId = 8001,
                fechaEnvio = ahora - 2 * DIA, fechaActualizacion = ahora - 2 * DIA
            ),
            ConsultaEntity(
                emprendimientoId = renace.id, nombreCliente = "Jorge Paredes", correoCliente = "jorge.paredes@example.com",
                telefonoCliente = fonoDemo(5102), servicio = Rubro.SUPRARRECICLAJE.etiqueta,
                mensaje = "Quiero transformar una chaqueta antigua de mi papá en algo que pueda usar. ¿Cuánto cobran?",
                estado = EstadoConsulta.RESPONDIDA,
                respuesta = "Hola Jorge, ¡claro que sí! Podemos convertirla en un chaleco o un bolso. Pasa por el taller y lo vemos.",
                enviadaDesdeApp = false, estadoSync = EstadoSync.SINCRONIZADO, remoteId = 8002,
                fechaEnvio = ahora - 5 * DIA, fechaRespuesta = ahora - 4 * DIA, fechaActualizacion = ahora - 4 * DIA
            ),
            ConsultaEntity(
                emprendimientoId = aguja.id, nombreCliente = "Lucía Fernández", correoCliente = "lucia.fernandez@example.com",
                telefonoCliente = fonoDemo(5103), servicio = Rubro.REPARACION.etiqueta,
                mensaje = "Necesito ajustar 3 pantalones para el trabajo antes del lunes. ¿Tienen hora disponible?",
                enviadaDesdeApp = false, estadoSync = EstadoSync.SINCRONIZADO, remoteId = 8003,
                fechaEnvio = ahora - DIA, fechaActualizacion = ahora - DIA
            ),
            ConsultaEntity(
                emprendimientoId = hilando.id, nombreCliente = "Carmen Gutiérrez", correoCliente = "carmen.gutierrez@example.com",
                telefonoCliente = fonoDemo(5104), servicio = Rubro.TALLERES.etiqueta,
                mensaje = "¿Quedan cupos para el taller de crochet de marzo? Somos dos vecinas interesadas.",
                enviadaDesdeApp = false, estadoSync = EstadoSync.SINCRONIZADO, remoteId = 8004,
                fechaEnvio = ahora - 3 * DIA, fechaActualizacion = ahora - 3 * DIA
            )
        )
    }

    private fun interaccionesDemo(fichas: List<EmprendimientoEntity>, ahora: Long): List<InteraccionEntity> {
        val azar = Random(2026)
        val activas = fichas.filter { it.estado == EstadoFicha.ACTIVA }
        val terminos = listOf("bolsos", "arreglo de jeans", "taller de costura", "lana", "vestido de fiesta", "ropa usada", "crochet", "parka")
        val tipos = listOf(
            TipoInteraccion.BUSQUEDA, TipoInteraccion.BUSQUEDA, TipoInteraccion.BUSQUEDA, TipoInteraccion.BUSQUEDA,
            TipoInteraccion.CLIC_WHATSAPP, TipoInteraccion.CLIC_WHATSAPP, TipoInteraccion.CLIC_WHATSAPP,
            TipoInteraccion.CLIC_INSTAGRAM, TipoInteraccion.CLIC_INSTAGRAM, TipoInteraccion.CONSULTA
        )
        return (1..180).map { n ->
            val tipo = tipos[azar.nextInt(tipos.size)]
            val fecha = ahora - azar.nextLong(30 * DIA)
            if (tipo == TipoInteraccion.BUSQUEDA) {
                val region = if (azar.nextInt(3) == 0) "" else ChileGeoData.nombresRegiones[azar.nextInt(ChileGeoData.nombresRegiones.size)]
                val rubro = if (azar.nextBoolean()) "" else Rubro.entries[azar.nextInt(Rubro.entries.size)].name
                InteraccionEntity(
                    tipo = tipo, region = region, rubro = rubro,
                    detalle = terminos[n % terminos.size], fecha = fecha, sincronizada = true
                )
            } else {
                val ficha = activas[azar.nextInt(activas.size)]
                InteraccionEntity(
                    tipo = tipo, emprendimientoId = ficha.id, nombreComercial = ficha.nombreComercial,
                    region = ficha.region, rubro = ficha.rubroPrincipal, fecha = fecha, sincronizada = true
                )
            }
        }
    }

    private fun correosDemo(fichas: List<EmprendimientoEntity>, ahora: Long): List<CorreoEntity> {
        val correos = mutableListOf<CorreoEntity>()
        fichas.forEachIndexed { i, ficha ->
            val bienvenida = PlantillasCorreo.bienvenida(ficha.nombreEmprendedora, ficha.nombreComercial)
            correos.add(
                CorreoEntity(
                    emprendimientoId = ficha.id, destinatario = ficha.correo, nombreDestinatario = ficha.nombreEmprendedora,
                    asunto = bienvenida.asunto, cuerpo = bienvenida.cuerpo, tipo = TipoCorreo.BIENVENIDA,
                    estado = EstadoCorreo.ENVIADO_OK, intentos = 1,
                    fechaCreacion = ficha.fechaRegistro, fechaEnvio = ficha.fechaRegistro
                )
            )
            if (ficha.estado == EstadoFicha.PENDIENTE) {
                val confirmacion = PlantillasCorreo.confirmacionInscripcion(
                    ficha.nombreEmprendedora, ficha.nombreComercial, ficha.comuna, ficha.region
                )
                correos.add(
                    CorreoEntity(
                        emprendimientoId = ficha.id, destinatario = ficha.correo, nombreDestinatario = ficha.nombreEmprendedora,
                        asunto = confirmacion.asunto, cuerpo = confirmacion.cuerpo, tipo = TipoCorreo.CONFIRMACION_INSCRIPCION,
                        estado = EstadoCorreo.FALLIDO,
                        intentos = 2,
                        detalleError = if (i % 2 == 0) "Sin respuesta del servidor de correo" else "Buzón del destinatario lleno",
                        fechaCreacion = ahora - DIA
                    )
                )
            }
            if (ficha.estado == EstadoFicha.OBSERVADA) {
                val obs = PlantillasCorreo.observacion(ficha.nombreEmprendedora, ficha.nombreComercial, ficha.observacion)
                correos.add(
                    CorreoEntity(
                        emprendimientoId = ficha.id, destinatario = ficha.correo, nombreDestinatario = ficha.nombreEmprendedora,
                        asunto = obs.asunto, cuerpo = obs.cuerpo, tipo = TipoCorreo.OBSERVACION,
                        estado = EstadoCorreo.ENVIADO_OK, intentos = 1,
                        fechaCreacion = ahora - 2 * DIA, fechaEnvio = ahora - 2 * DIA
                    )
                )
            }
        }
        return correos
    }
}
