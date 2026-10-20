package cl.duoc.redtextil.domain.validation

import cl.duoc.redtextil.domain.model.Rubro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DepuradorHistoricoTest {

    private val filaBase = mapOf(
        "id_historico" to "HIST-0001",
        "marca_temporal" to "15/03/2024 10:22:31",
        "nombre_emprendedora" to "  ana  pérez soto ",
        "nombre_comercial" to "hilos del mar",
        "correo" to "Ana.Perez@Example.com",
        "telefono" to "9 0000 1234",
        "region" to "Región de Valparaíso",
        "comuna_VA" to "vina del mar",
        "rubros" to "Reparación y Ajustes; Tejido y Confección Local",
        "instagram" to "https://www.instagram.com/hilosdelmar.demo/?hl=es",
        "descripcion" to "Arreglos de ropa"
    )

    @Test
    fun filaSeDepuraYQuedaActiva() {
        val r = DepuradorHistorico.depurar(filaBase)
        assertTrue(r.quedaActiva)
        assertEquals("Ana Pérez Soto", r.datos.nombreEmprendedora)
        assertEquals("Hilos del Mar", r.datos.nombreComercial)
        assertEquals("Valparaíso", r.datos.region)
        assertEquals("Viña del Mar", r.datos.comuna)
        assertEquals("+56900001234", r.datos.whatsapp)
        assertEquals("@hilosdelmar.demo", r.datos.instagram)
        assertEquals(setOf(Rubro.REPARACION, Rubro.TEJIDO), r.datos.rubros)
        assertTrue(r.correcciones > 0)
    }

    @Test
    fun comunasDuplicadasSeResuelvenSegunRegion() {
        val fila = filaBase + mapOf("comuna_RM" to "Maipú")
        val r = DepuradorHistorico.depurar(fila)
        assertEquals("Viña del Mar", r.datos.comuna)
        assertTrue(r.quedaActiva)
    }

    @Test
    fun cambioDeRegionSinComunaQuedaObservada() {
        val fila = filaBase - "comuna_VA" + mapOf("comuna_RM" to "Maipú")
        val r = DepuradorHistorico.depurar(fila)
        assertFalse(r.quedaActiva)
        assertEquals("", r.datos.comuna)
    }

    @Test
    fun masDeDosRubrosSeRecortan() {
        val fila = filaBase + mapOf("rubros" to "Upcycling; Arriendo de Prendas; Talleres y Educación")
        val r = DepuradorHistorico.depurar(fila)
        assertEquals(2, r.datos.rubros.size)
    }

    @Test
    fun instagramRotoSeLimpiaSinBloquearSiHayWhatsapp() {
        val fila = filaBase + mapOf("instagram" to "www.instagran.com/hilos del mar")
        val r = DepuradorHistorico.depurar(fila)
        assertEquals("", r.datos.instagram)
        assertTrue(r.quedaActiva)
        assertTrue(r.avisos.any { it.contains("Instagram") })
    }
}
