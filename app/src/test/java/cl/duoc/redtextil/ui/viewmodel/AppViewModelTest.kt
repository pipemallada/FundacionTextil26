package cl.duoc.redtextil.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import cl.duoc.redtextil.ReglaCorrutinas
import cl.duoc.redtextil.data.local.PreferenciasLocales
import cl.duoc.redtextil.data.repository.ConsultaRepository
import cl.duoc.redtextil.data.repository.ResultadoSincronizacion
import cl.duoc.redtextil.data.repository.SesionRepository
import cl.duoc.redtextil.data.repository.SincronizacionRepository
import cl.duoc.redtextil.suscribir
import cl.duoc.redtextil.ui.navigation.ModoApp
import cl.duoc.redtextil.util.NetworkMonitor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    @get:Rule
    val corrutinas = ReglaCorrutinas()

    private val enLinea = MutableStateFlow(true)
    private val forzarOffline = MutableStateFlow(false)
    private val pendientes = MutableStateFlow(0)
    private val sesionId = MutableStateFlow<Long?>(1)
    private val preferencias = mockk<PreferenciasLocales>(relaxed = true).also {
        every { it.forzarOffline } returns forzarOffline
    }
    private val red = mockk<NetworkMonitor>().also { every { it.enLinea } returns enLinea }
    private val sincronizacion = mockk<SincronizacionRepository>().also {
        every { it.pendientesTotales } returns pendientes
        coEvery { it.sincronizarTodo() } returns ResultadoSincronizacion(0, true, "Todo está al día")
    }
    private val sesion = mockk<SesionRepository>().also { every { it.sesionId } returns sesionId }
    private val consultas = mockk<ConsultaRepository>().also { every { it.contarPendientesDe(1) } returns flowOf(3) }

    private fun crear() = AppViewModel(SavedStateHandle(), preferencias, red, sincronizacion, sesion, consultas)

    @Test
    fun combinaConexionPendientesYConsultasPorResponder() = runTest {
        pendientes.value = 4
        coEvery { sincronizacion.sincronizarTodo() } returns ResultadoSincronizacion(0, false, "sin servidor")
        val vm = crear()
        suscribir(vm.uiState)

        val estado = vm.uiState.value
        assertTrue(estado.enLinea)
        assertEquals(4, estado.pendientes)
        assertEquals(3, estado.consultasPendientes)
    }

    @Test
    fun alAbrirConSenalEnviaLoPendienteYAvisaSiHuboErrores() = runTest {
        pendientes.value = 2
        coEvery { sincronizacion.sincronizarTodo() } returns ResultadoSincronizacion(0, false, "No se pudo conectar")
        val vm = crear()
        val mensajes = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.mensajes.collect { mensajes += it } }

        enLinea.value = false
        enLinea.value = true

        coVerify(atLeast = 2) { sincronizacion.sincronizarTodo() }
        assertTrue("No se pudo conectar" in mensajes)
    }

    @Test
    fun laSincronizacionAutomaticaSinCambiosNoMuestraMensaje() = runTest {
        val vm = crear()
        val mensajes = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.mensajes.collect { mensajes += it } }

        enLinea.value = false
        enLinea.value = true
        assertTrue(mensajes.isEmpty())

        vm.sincronizar()
        assertEquals(listOf("Todo está al día"), mensajes)
    }

    @Test
    fun cambiarModoYSimularSinSenal() = runTest {
        val vm = crear()
        val mensajes = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.mensajes.collect { mensajes += it } }

        vm.cambiarModo(ModoApp.ADMIN)
        assertEquals(ModoApp.ADMIN, vm.modo.value)

        vm.cambiarForzarOffline(true)
        vm.cambiarForzarOffline(false)
        verify { preferencias.cambiarForzarOffline(true) }
        assertEquals(2, mensajes.size)
        assertTrue(mensajes.first().startsWith("Simulando zona sin señal"))
    }
}
