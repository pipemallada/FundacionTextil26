package cl.duoc.redtextil.domain.validation

import cl.duoc.redtextil.domain.model.CampoFicha
import cl.duoc.redtextil.domain.model.FichaDatos
import cl.duoc.redtextil.domain.model.Rubro
import cl.duoc.redtextil.util.ChileGeoData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FichaValidatorTest {

    private val hoy = FechaSimple(19, 10, 2026)

    private val fichaCompleta = FichaDatos(
        nombreEmprendedora = "Camila Rojas Fuentes",
        rut = "12.345.678-5",
        fechaNacimiento = "21/03/1985",
        nacionalidad = "Chilena",
        correo = "camila.rojas@example.com",
        telefono = "+56900000101",
        jefaHogar = "Sí",
        nombreComercial = "Renace Textil",
        descripcion = "Transformamos ropa en desuso en bolsos y chaquetas únicas.",
        anioInicio = "2019",
        lugarTrabajo = "Taller en mi casa",
        cantidadPersonas = "2",
        region = "Valparaíso",
        comuna = "Viña del Mar",
        atencionPresencial = false,
        realizaEnvios = true,
        rubros = setOf(Rubro.ACCESORIOS),
        tecnicas = "Patchwork",
        whatsapp = "+56900000101",
        horarioAtencion = "Lunes a viernes 10 a 18",
        aceptaPublicacion = true
    )

    @Test
    fun hayExactamente32Campos() {
        assertEquals(FichaValidator.TOTAL_CAMPOS, CampoFicha.entries.size)
    }

    @Test
    fun fichaCompletaNoTieneErrores() {
        assertTrue(FichaValidator.validarTodo(fichaCompleta, hoy).isEmpty())
    }

    @Test
    fun maximoDosRubros() {
        val dos = setOf(Rubro.ACCESORIOS, Rubro.TEJIDO)
        assertTrue(FichaValidator.puedeAgregarRubro(setOf(Rubro.ACCESORIOS), Rubro.TEJIDO))
        assertFalse(FichaValidator.puedeAgregarRubro(dos, Rubro.TALLERES))
        assertTrue(FichaValidator.puedeAgregarRubro(dos, Rubro.TEJIDO))
        assertNotNull(FichaValidator.validarRubros(emptySet()))
        assertNotNull(FichaValidator.validarRubros(dos + Rubro.TALLERES))
    }

    @Test
    fun comunaDebePertenecerALaRegion() {
        val mal = fichaCompleta.copy(region = "Biobío", comuna = "Viña del Mar")
        assertNotNull(FichaValidator.validarCampo(CampoFicha.COMUNA, mal, hoy))
        val bien = fichaCompleta.copy(region = "Biobío", comuna = "Concepción")
        assertNull(FichaValidator.validarCampo(CampoFicha.COMUNA, bien, hoy))
    }

    @Test
    fun preguntasCondicionalesSoloSiCorresponde() {
        assertFalse(FichaValidator.campoVisible(CampoFicha.CUPOS_TALLER, fichaCompleta))
        val conTaller = fichaCompleta.copy(rubros = setOf(Rubro.TALLERES))
        assertTrue(FichaValidator.campoVisible(CampoFicha.CUPOS_TALLER, conTaller))
        assertNotNull(FichaValidator.validarCampo(CampoFicha.CUPOS_TALLER, conTaller, hoy))
        assertNull(FichaValidator.validarCampo(CampoFicha.CUPOS_TALLER, conTaller.copy(cuposTaller = "12"), hoy))
    }

    @Test
    fun alQuitarRubroSeLimpianSusRespuestas() {
        val conArriendo = fichaCompleta.copy(rubros = setOf(Rubro.ARRIENDO), tallasArriendo = "S, M, L")
        val sinArriendo = FichaValidator.limpiarCondicionales(conArriendo.copy(rubros = setOf(Rubro.TEJIDO)))
        assertEquals("", sinArriendo.tallasArriendo)
    }

    @Test
    fun debeSerMayorDeEdad() {
        assertNotNull(FichaValidator.validarFechaNacimiento("01/01/2015", hoy))
        assertNotNull(FichaValidator.validarFechaNacimiento("31/02/1990", hoy))
        assertNull(FichaValidator.validarFechaNacimiento("19/10/2008", hoy))
    }

    @Test
    fun sinPresencialNiEnviosDaError() {
        val sinEntrega = fichaCompleta.copy(atencionPresencial = false, realizaEnvios = false)
        assertNotNull(FichaValidator.validarCampo(CampoFicha.REALIZA_ENVIOS, sinEntrega, hoy))
    }

    @Test
    fun consistenciaDetectaInstagramRoto() {
        val problemas = FichaValidator.revisarConsistencia(fichaCompleta.copy(instagram = "www.instagran.com/x y"))
        assertTrue(problemas.any { it.contains("Instagram") })
    }

    @Test
    fun hay16RegionesY346Comunas() {
        assertEquals(16, ChileGeoData.regiones.size)
        assertEquals(346, ChileGeoData.totalComunas)
    }
}
