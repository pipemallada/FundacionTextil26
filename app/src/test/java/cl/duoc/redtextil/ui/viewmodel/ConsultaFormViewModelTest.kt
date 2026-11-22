package cl.duoc.redtextil.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import cl.duoc.redtextil.DatosPrueba
import cl.duoc.redtextil.ReglaCorrutinas
import cl.duoc.redtextil.data.repository.ConsultaRepository
import cl.duoc.redtextil.data.repository.EmprendimientoRepository
import cl.duoc.redtextil.domain.model.CampoConsulta
import cl.duoc.redtextil.ui.navigation.Rutas
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ConsultaFormViewModelTest {

    @get:Rule
    val corrutinas = ReglaCorrutinas()

    private val emprendimientos = mockk<EmprendimientoRepository>()
    private val consultas = mockk<ConsultaRepository>(relaxed = true)

    private fun crear(): ConsultaFormViewModel =
        ConsultaFormViewModel(SavedStateHandle(mapOf(Rutas.ARG_ID to 1L)), emprendimientos, consultas)

    private fun ConsultaFormViewModel.completar() {
        onNombre("Pedro Soto")
        onCorreo("pedro@example.com")
        onTelefono("+56911112222")
        onServicio(ConsultaFormViewModel.SERVICIO_GENERAL)
        onMensaje("Hola, ¿hacen bolsos con jeans de mi familia?")
    }

    @Test
    fun cargaLosServiciosDelEmprendimiento() = runTest {
        coEvery { emprendimientos.obtener(1) } returns DatosPrueba.emprendimiento()

        val estado = crear().uiState.value

        assertFalse(estado.cargando)
        assertEquals(2, estado.servicios.size)
        assertEquals(ConsultaFormViewModel.SERVICIO_GENERAL, estado.servicios.last())
    }

    @Test
    fun enviarConErroresLosMarcaTodos() = runTest {
        coEvery { emprendimientos.obtener(1) } returns DatosPrueba.emprendimiento()
        val vm = crear()
        vm.onCorreo("pedro@")

        vm.enviar()

        assertEquals(CampoConsulta.entries.toSet(), vm.uiState.value.errores.keys)
        assertEquals("Revisa los 5 datos marcados en rojo", vm.uiState.value.mensaje)
        vm.mensajeMostrado()
        assertNull(vm.uiState.value.mensaje)
    }

    @Test
    fun enviarYEscribirOtraConsulta() = runTest {
        coEvery { emprendimientos.obtener(1) } returns DatosPrueba.emprendimiento()
        val vm = crear()
        vm.completar()
        assertEquals(CampoConsulta.entries.toSet(), vm.uiState.value.validos)

        vm.enviar()
        assertTrue(vm.uiState.value.enviada)
        coVerify { consultas.enviar(any(), match { it.nombre == "Pedro Soto" }) }

        vm.nuevaConsulta()
        val estado = vm.uiState.value
        assertFalse(estado.enviada)
        assertEquals("Pedro Soto", estado.form.nombre)
        assertEquals("", estado.form.mensaje)
        assertTrue(estado.errores.isEmpty())
    }

    @Test
    fun siLaFichaNoExisteNoSeEnviaNada() = runTest {
        coEvery { emprendimientos.obtener(1) } returns null
        val vm = crear()
        vm.completar()

        vm.enviar()

        coVerify(exactly = 0) { consultas.enviar(any(), any()) }
    }
}
