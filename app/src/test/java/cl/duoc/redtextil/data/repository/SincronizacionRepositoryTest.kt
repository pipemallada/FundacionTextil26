package cl.duoc.redtextil.data.repository

import cl.duoc.redtextil.util.NetworkMonitor
import cl.duoc.redtextil.util.NotificationHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SincronizacionRepositoryTest {

    private val emprendimientos = mockk<EmprendimientoRepository>(relaxed = true)
    private val catalogo = mockk<CatalogoRepository>(relaxed = true)
    private val consultas = mockk<ConsultaRepository>(relaxed = true)
    private val metricas = mockk<MetricasRepository>(relaxed = true)
    private val correos = mockk<CorreoRepository>(relaxed = true)
    private val enLinea = MutableStateFlow(true)
    private val red = mockk<NetworkMonitor>().also { every { it.enLinea } returns enLinea }
    private val notificaciones = mockk<NotificationHelper>(relaxed = true)

    private fun crear() = SincronizacionRepository(emprendimientos, catalogo, consultas, metricas, correos, red, notificaciones)

    @Test
    fun sumaTodoLoQueFaltaPorEnviar() = runTest {
        every { emprendimientos.contarPendientes() } returns flowOf(1)
        every { catalogo.contarPendientes() } returns flowOf(2)
        every { consultas.contarPendientesSync() } returns flowOf(3)
        every { metricas.contarPendientes() } returns flowOf(4)
        every { correos.contarEnCola() } returns flowOf(5)

        assertEquals(15, crear().pendientesTotales.first())
    }

    @Test
    fun sinSenalNoIntentaEnviar() = runTest {
        enLinea.value = false

        val resultado = crear().sincronizarTodo()

        assertFalse(resultado.exito)
        coVerify(exactly = 0) { emprendimientos.sincronizarPendientes() }
    }

    @Test
    fun enviaEnOrdenYRecibeLasFichasNuevas() = runTest {
        coEvery { emprendimientos.sincronizarPendientes() } returns 1
        coEvery { catalogo.sincronizarPendientes() } returns 2
        coEvery { consultas.sincronizarPendientes() } returns 0
        coEvery { metricas.sincronizarPendientes() } returns 3
        coEvery { correos.reenviarPendientes() } returns 0
        coEvery { emprendimientos.actualizarDesdeServidor() } returns Result.success(4)

        val resultado = crear().sincronizarTodo()

        assertTrue(resultado.exito)
        assertEquals(6, resultado.enviados)
        assertEquals("Sincronización completa: 6 cambios enviados y 4 fichas recibidas", resultado.mensaje)
        verify { notificaciones.notificarSincronizacion(6) }
    }

    @Test
    fun sinNadaPendienteTodoEstaAlDia() = runTest {
        coEvery { emprendimientos.sincronizarPendientes() } returns 0
        coEvery { catalogo.sincronizarPendientes() } returns 0
        coEvery { consultas.sincronizarPendientes() } returns 0
        coEvery { metricas.sincronizarPendientes() } returns 0
        coEvery { correos.reenviarPendientes() } returns 0
        coEvery { emprendimientos.actualizarDesdeServidor() } returns Result.failure(IOException())

        val resultado = crear().sincronizarTodo()

        assertEquals("Todo está al día", resultado.mensaje)
        verify(exactly = 0) { notificaciones.notificarSincronizacion(any()) }
    }

    @Test
    fun siElServidorFallaLosDatosQuedanEnElTelefono() = runTest {
        coEvery { emprendimientos.sincronizarPendientes() } throws IOException("sin servidor")

        val resultado = crear().sincronizarTodo()

        assertFalse(resultado.exito)
        assertEquals("No se pudo conectar con el servidor. Tus datos siguen guardados en el teléfono.", resultado.mensaje)
        coVerify(exactly = 0) { catalogo.sincronizarPendientes() }
    }
}
