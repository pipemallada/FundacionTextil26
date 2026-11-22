package cl.duoc.redtextil.ui.viewmodel

import cl.duoc.redtextil.ReglaCorrutinas
import cl.duoc.redtextil.data.repository.EmprendimientoRepository
import cl.duoc.redtextil.data.repository.ResultadoRegistro
import cl.duoc.redtextil.data.repository.SesionRepository
import cl.duoc.redtextil.domain.model.CampoRegistro
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RegistroViewModelTest {

    @get:Rule
    val corrutinas = ReglaCorrutinas()

    private val emprendimientos = mockk<EmprendimientoRepository>()
    private val sesion = mockk<SesionRepository>(relaxed = true)
    private val vm = RegistroViewModel(emprendimientos, sesion)

    private fun completarFormulario() {
        vm.onNombre("Camila Rojas Fuentes")
        vm.onNombreComercial("Renace Textil")
        vm.onRut("12.345.678-5")
        vm.onCorreo("camila@example.com")
        vm.onTelefono("+56900000101")
        vm.onRegion("Valparaíso")
        vm.onComuna("Viña del Mar")
        vm.onPrivacidad(true)
    }

    @Test
    fun soloMuestraErroresDeLosCamposTocados() {
        vm.onRut("12.345.678-0")

        val estado = vm.uiState.value
        assertTrue(CampoRegistro.RUT in estado.errores)
        assertFalse(CampoRegistro.CORREO in estado.errores)
    }

    @Test
    fun cambiarLaRegionLimpiaLaComunaYCargaSusComunas() {
        vm.onRegion("Valparaíso")
        vm.onComuna("Viña del Mar")
        vm.onRegion("Biobío")

        val estado = vm.uiState.value
        assertEquals("", estado.form.comuna)
        assertTrue("Concepción" in estado.comunas)
    }

    @Test
    fun registrarConErroresLosMuestraTodos() {
        vm.registrar()

        val estado = vm.uiState.value
        assertEquals(CampoRegistro.entries.size, estado.errores.size)
        assertEquals("Revisa los 8 datos marcados en rojo", estado.mensaje)
        coVerify(exactly = 0) { emprendimientos.registrar(any()) }
    }

    @Test
    fun registroExitosoIniciaSesion() = runTest {
        coEvery { emprendimientos.registrar(any()) } returns ResultadoRegistro.Exito(12)
        completarFormulario()

        assertEquals(CampoRegistro.entries.toSet(), vm.uiState.value.validos)
        vm.registrar()

        assertTrue(vm.uiState.value.registrado)
        verify { sesion.iniciarSesion(12) }
    }

    @Test
    fun registroRechazadoMuestraElMotivo() = runTest {
        coEvery { emprendimientos.registrar(any()) } returns ResultadoRegistro.Error("Ya existe una cuenta con este RUT.")
        completarFormulario()

        vm.registrar()

        assertEquals("Ya existe una cuenta con este RUT.", vm.uiState.value.mensaje)
        assertFalse(vm.uiState.value.guardando)
        vm.mensajeMostrado()
        assertNull(vm.uiState.value.mensaje)
    }
}
