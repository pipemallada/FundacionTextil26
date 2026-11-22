package cl.duoc.redtextil.ui.viewmodel

import cl.duoc.redtextil.DatosPrueba
import cl.duoc.redtextil.ReglaCorrutinas
import cl.duoc.redtextil.data.repository.CorreoRepository
import cl.duoc.redtextil.domain.model.EstadoCorreo
import cl.duoc.redtextil.suscribir
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class CorreosViewModelTest {

    @get:Rule
    val corrutinas = ReglaCorrutinas()

    private val correo = DatosPrueba.correo()
    private val correos = mockk<CorreoRepository>().also {
        every { it.observar(any()) } returns flowOf(listOf(correo))
        every { it.contarPorEstado() } returns flowOf(mapOf(EstadoCorreo.ENVIADO_OK to 5, EstadoCorreo.FALLIDO to 2))
    }
    private val vm by lazy { CorreosViewModel(correos) }

    @Test
    fun cuentaLosCorreosYFiltraPorEstado() = runTest {
        suscribir(vm.uiState)
        assertEquals(7, vm.uiState.value.total)
        assertEquals(2, vm.uiState.value.noEnviados)

        vm.cambiarFiltro(EstadoCorreo.FALLIDO)

        assertEquals(EstadoCorreo.FALLIDO, vm.uiState.value.filtro)
        verify { correos.observar(EstadoCorreo.FALLIDO) }
    }

    @Test
    fun reenviarExplicaElResultado() = runTest {
        suscribir(vm.uiState)

        coEvery { correos.reenviar(30) } returns correo.copy(estado = EstadoCorreo.ENVIADO_OK)
        vm.reenviar(correo)
        assertEquals("Correo reenviado a camila.rojas@example.com", vm.uiState.value.mensaje)

        coEvery { correos.reenviar(30) } returns correo.copy(estado = EstadoCorreo.EN_COLA_OFFLINE)
        vm.reenviar(correo)
        assertEquals("Sin conexión: el correo sigue en cola y se enviará al volver la señal", vm.uiState.value.mensaje)

        coEvery { correos.reenviar(30) } returns correo.copy(estado = EstadoCorreo.FALLIDO, detalleError = "Buzón lleno")
        vm.reenviar(correo)
        assertEquals("No se pudo enviar: Buzón lleno", vm.uiState.value.mensaje)

        coEvery { correos.reenviar(30) } returns null
        vm.reenviar(correo)
        assertEquals("No se pudo enviar: error desconocido", vm.uiState.value.mensaje)
        assertNull(vm.uiState.value.reenviandoId)
    }

    @Test
    fun reenviarTodosLosPendientes() = runTest {
        suscribir(vm.uiState)

        coEvery { correos.reenviarPendientes() } returns 2
        vm.reenviarTodos()
        assertEquals("Se enviaron 2 correos pendientes", vm.uiState.value.mensaje)
        assertFalse(vm.uiState.value.reenviandoTodos)

        coEvery { correos.reenviarPendientes() } returns 0
        vm.reenviarTodos()
        assertEquals("No se pudo enviar ningún correo. Revisa la conexión.", vm.uiState.value.mensaje)
        vm.mensajeMostrado()
        assertNull(vm.uiState.value.mensaje)
    }
}
