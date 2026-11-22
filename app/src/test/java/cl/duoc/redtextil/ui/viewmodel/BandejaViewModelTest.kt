package cl.duoc.redtextil.ui.viewmodel

import cl.duoc.redtextil.DatosPrueba
import cl.duoc.redtextil.ReglaCorrutinas
import cl.duoc.redtextil.data.local.entity.EmprendimientoEntity
import cl.duoc.redtextil.data.repository.ConsultaRepository
import cl.duoc.redtextil.data.repository.SesionRepository
import cl.duoc.redtextil.domain.model.EstadoConsulta
import cl.duoc.redtextil.suscribir
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class BandejaViewModelTest {

    @get:Rule
    val corrutinas = ReglaCorrutinas()

    private val pendiente = DatosPrueba.consulta(id = 20)
    private val respondida = DatosPrueba.consulta(id = 21).copy(estado = EstadoConsulta.RESPONDIDA, respuesta = "Hola Pedro, sí podemos.")
    private val sesionId = MutableStateFlow<Long?>(1)
    private val actual = MutableStateFlow<EmprendimientoEntity?>(DatosPrueba.emprendimiento())
    private val sesion = mockk<SesionRepository>().also {
        every { it.sesionId } returns sesionId
        every { it.emprendimientoActual } returns actual
    }
    private val consultas = mockk<ConsultaRepository>(relaxed = true).also {
        every { it.observarBandeja(1) } returns flowOf(listOf(pendiente, respondida))
        coEvery { it.actualizarDesdeServidor(1) } returns Result.success(0)
    }
    private val vm by lazy { BandejaViewModel(sesion, consultas) }

    @Test
    fun sinSesionPideIniciarSesion() = runTest {
        sesionId.value = null
        actual.value = null
        suscribir(vm.uiState)

        assertTrue(vm.uiState.value.sinSesion)
    }

    @Test
    fun filtraPorEstadoYCuenta() = runTest {
        suscribir(vm.uiState)
        assertEquals(2, vm.uiState.value.total)
        assertEquals(1, vm.uiState.value.pendientes)
        assertEquals(1, vm.uiState.value.respondidas)

        vm.cambiarFiltro(FiltroConsultas.PENDIENTES)
        assertEquals(listOf(pendiente), vm.uiState.value.consultas)
        vm.cambiarFiltro(FiltroConsultas.RESPONDIDAS)
        assertEquals(listOf(respondida), vm.uiState.value.consultas)
        vm.cambiarFiltro(FiltroConsultas.TODAS)
        assertEquals(2, vm.uiState.value.consultas.size)
    }

    @Test
    fun unaRespuestaMuyCortaNoSeGuarda() = runTest {
        suscribir(vm.uiState)

        vm.onBorrador(20, "Sí")
        vm.guardarRespuesta(pendiente)
        assertTrue(20L in vm.uiState.value.errores)

        vm.onBorrador(20, "Sí, hacemos bolsos con jeans.")
        assertFalse(20L in vm.uiState.value.errores)
        coVerify(exactly = 0) { consultas.responder(any(), any()) }
    }

    @Test
    fun guardarRespuestaValidaLaEnvia() = runTest {
        suscribir(vm.uiState)
        vm.onBorrador(20, "Sí, hacemos bolsos con jeans.")

        vm.guardarRespuesta(pendiente)

        coVerify { consultas.responder(20, "Sí, hacemos bolsos con jeans.") }
        assertEquals("Respuesta guardada para Pedro Soto", vm.uiState.value.mensaje)
        assertFalse(20L in vm.uiState.value.borradores)
        assertNull(vm.uiState.value.guardandoId)
    }

    @Test
    fun alternarEstadoMarcaRespondidaOPendiente() = runTest {
        suscribir(vm.uiState)

        vm.alternarEstado(pendiente)
        coVerify { consultas.cambiarEstado(20, EstadoConsulta.RESPONDIDA) }
        vm.alternarEstado(respondida)
        coVerify { consultas.cambiarEstado(21, EstadoConsulta.PENDIENTE) }
        assertEquals("Consulta marcada como pendiente", vm.uiState.value.mensaje)
    }

    @Test
    fun armaElEnlaceDeWhatsappDelCliente() = runTest {
        suscribir(vm.uiState)

        assertTrue(vm.urlWhatsapp(pendiente)!!.startsWith("https://wa.me/56911112222"))
        vm.avisar("No pudimos abrir WhatsApp")
        assertEquals("No pudimos abrir WhatsApp", vm.uiState.value.mensaje)
        vm.mensajeMostrado()
        assertNull(vm.uiState.value.mensaje)
    }

    @Test
    fun recibeLasConsultasDelServidor() = runTest {
        coEvery { consultas.actualizarDesdeServidor(1) } returns Result.success(2)
        suscribir(vm.uiState)
        assertEquals("Se recibieron 2 cambios del servidor", vm.uiState.value.mensaje)

        coEvery { consultas.actualizarDesdeServidor(1) } returns Result.success(0)
        vm.actualizarDesdeServidor()
        assertEquals("Tu bandeja ya está al día con el servidor", vm.uiState.value.mensaje)

        coEvery { consultas.actualizarDesdeServidor(1) } returns Result.failure(IOException())
        vm.actualizarDesdeServidor()
        assertEquals("No se pudo conectar con el servidor. Se muestran las consultas guardadas.", vm.uiState.value.mensaje)
        assertFalse(vm.uiState.value.actualizando)
    }

    @Test
    fun eliminarBorraLaConsultaYSuBorrador() = runTest {
        suscribir(vm.uiState)
        vm.onBorrador(20, "borrador")

        vm.eliminar(pendiente)

        coVerify { consultas.eliminar(20) }
        assertFalse(20L in vm.uiState.value.borradores)
        assertEquals("Se eliminó la consulta de Pedro Soto", vm.uiState.value.mensaje)
    }
}
