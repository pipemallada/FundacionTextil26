package cl.duoc.redtextil.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ReglasComunesTest {

    @Test
    fun rutConDigitoCorrectoEsValido() {
        assertNull(ReglasComunes.validarRut("12.345.678-5"))
        assertNull(ReglasComunes.validarRut("123456785"))
        assertNull(ReglasComunes.validarRut("11.111.111-1"))
    }

    @Test
    fun rutConDigitoIncorrectoDaError() {
        assertNotNull(ReglasComunes.validarRut("12.345.678-9"))
        assertNotNull(ReglasComunes.validarRut("abc"))
        assertNotNull(ReglasComunes.validarRut(""))
    }

    @Test
    fun rutConKSeCalculaBien() {
        val cuerpo = "10000013"
        assertEquals('K', ReglasComunes.calcularDigitoVerificador(cuerpo))
        assertNull(ReglasComunes.validarRut("10.000.013-k"))
    }

    @Test
    fun rutSeFormateaConPuntosYGuion() {
        assertEquals("12.345.678-5", ReglasComunes.formatearRut("123456785"))
        assertEquals("7.654.321-6", ReglasComunes.formatearRut("7654321-6"))
    }

    @Test
    fun telefonoSeNormalizaAFormatoChileno() {
        assertEquals("+56912345678", ReglasComunes.normalizarTelefono("+56 9 1234 5678"))
        assertEquals("+56912345678", ReglasComunes.normalizarTelefono("9 1234 5678"))
        assertEquals("+56912345678", ReglasComunes.normalizarTelefono("56912345678"))
        assertNull(ReglasComunes.normalizarTelefono("2 2345 6789"))
        assertNull(ReglasComunes.normalizarTelefono("+56 9 1234"))
        assertNull(ReglasComunes.normalizarTelefono("9 1234 567a"))
    }

    @Test
    fun instagramAceptaUsuarioOEnlace() {
        assertEquals("@renace.textil", ReglasComunes.normalizarInstagram("@Renace.Textil"))
        assertEquals("@renace.textil", ReglasComunes.normalizarInstagram("https://www.instagram.com/renace.textil/?hl=es"))
        assertEquals("@renace_textil", ReglasComunes.normalizarInstagram("instagram.com/renace_textil"))
    }

    @Test
    fun instagramRotoSeRechaza() {
        assertNull(ReglasComunes.normalizarInstagram("www.instagran.com/atelier"))
        assertNull(ReglasComunes.normalizarInstagram("@atelier copiapo"))
        assertNull(ReglasComunes.normalizarInstagram("https://instagram.com/p/ABC123"))
        assertNull(ReglasComunes.normalizarInstagram("@usuario..doble"))
    }

    @Test
    fun correoValido() {
        assertNull(ReglasComunes.validarCorreo("camila.rojas@example.com"))
        assertNotNull(ReglasComunes.validarCorreo("camila.rojas@example"))
        assertNotNull(ReglasComunes.validarCorreo("camila rojas@example.com"))
    }

    @Test
    fun sitioWebSinProtocoloSeCompleta() {
        assertEquals("https://www.mitienda.cl", ReglasComunes.normalizarUrl("www.mitienda.cl"))
        assertNull(ReglasComunes.normalizarUrl("mi tienda"))
    }

    @Test
    fun nombreCompletoNecesitaNombreYApellido() {
        assertNull(ReglasComunes.validarNombreCompleto("María José Pérez"))
        assertNotNull(ReglasComunes.validarNombreCompleto("María"))
        assertNotNull(ReglasComunes.validarNombreCompleto("María 123"))
    }
}
