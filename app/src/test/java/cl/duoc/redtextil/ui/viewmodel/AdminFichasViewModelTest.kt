package cl.duoc.redtextil.ui.viewmodel

import cl.duoc.redtextil.DatosPrueba
import cl.duoc.redtextil.ReglaCorrutinas
import cl.duoc.redtextil.data.repository.EmprendimientoRepository
import cl.duoc.redtextil.data.repository.HistoricoRepository
import cl.duoc.redtextil.data.repository.ResultadoImportacion
import cl.duoc.redtextil.domain.model.EstadoFicha
import cl.duoc.redtextil.suscribir
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AdminFichasViewModelTest {

    @get:Rule
    val corrutinas = ReglaCorrutinas()

    private val completa = DatosPrueba.emprendimiento(id = 1)
    private val sinComuna = DatosPrueba.emprendimiento(id = 2, estado = EstadoFicha.PENDIENTE).copy(comuna = "")
    private val emprendimientos = mockk<EmprendimientoRepository>(relaxed = true).also {
        every { it.observarParaAdmin(any(), any()) } returns flowOf(listOf(completa, sinComuna))
        every { it.contarPorEstado() } returns flowOf(mapOf(EstadoFicha.ACTIVA to 1, EstadoFicha.PENDIENTE to 1))
        coEvery { it.actualizarDesdeServidor() } returns Result.success(0)
    }
    private val historico = mockk<HistoricoRepository>().also { every { it.totalIntegrados() } returns flowOf(982) }
    private val vm by lazy { AdminFichasViewModel(emprendimientos, historico, corrutinas.dispatcher) }

    // La lista de fichas pasa por un debounce de 200 ms: se avanza el reloj virtual antes de mirar el estado
    private fun TestScope.abrirPantalla() {
        suscribir(vm.uiState)
        advanceTimeBy(300)
    }

    private val resultado = ResultadoImportacion(
        total = 982, integradas = 982, yaExistian = 0, activas = 900, observadas = 82, correcciones = 300, fuente = "API de la Fundación"
    )

    @Test
    fun revisaLaConsistenciaDeCadaFicha() = runTest {
        abrirPantalla()

        val estado = vm.uiState.value
        assertEquals(2, estado.totalFichas)
        assertEquals(982, estado.historicosIntegrados)
        assertTrue(estado.fichas.first { it.emprendimiento.id == 1L }.problemas.isEmpty())
        assertTrue(estado.fichas.first { it.emprendimiento.id == 2L }.problemas.isNotEmpty())

        vm.alternarSoloProblemas()
        advanceTimeBy(300)
        assertEquals(listOf(2L), vm.uiState.value.fichas.map { it.emprendimiento.id })
    }

    @Test
    fun losFiltrosLleganAlRepositorio() = runTest {
        abrirPantalla()

        vm.filtrarEstado(EstadoFicha.PENDIENTE)
        vm.onTexto("renace")
        advanceTimeBy(300)

        verify { emprendimientos.observarParaAdmin(EstadoFicha.PENDIENTE, "renace") }
    }

    @Test
    fun aprobarYDarDeBajaAvisanALaAdministradora() = runTest {
        abrirPantalla()

        vm.aprobar(sinComuna)
        coVerify { emprendimientos.cambiarEstado(2, EstadoFicha.ACTIVA, "") }
        vm.darDeBaja(completa)
        coVerify { emprendimientos.cambiarEstado(1, EstadoFicha.BAJA, "") }
        assertEquals("Renace Textil quedó de baja y ya no aparece en el directorio", vm.uiState.value.interfaz.mensaje)
        assertNull(vm.uiState.value.interfaz.procesandoId)
    }

    @Test
    fun observarExigeUnaNotaValida() = runTest {
        abrirPantalla()

        vm.abrirObservacion(sinComuna)
        assertEquals(sinComuna, vm.uiState.value.interfaz.fichaObservar)
        vm.onTextoObservacion("x")
        vm.confirmarObservacion()
        assertTrue(vm.uiState.value.interfaz.errorObservacion != null)
        vm.onTextoObservacion("Falta indicar la comuna del taller")
        assertNull(vm.uiState.value.interfaz.errorObservacion)

        vm.confirmarObservacion()
        coVerify { emprendimientos.cambiarEstado(2, EstadoFicha.OBSERVADA, "Falta indicar la comuna del taller") }
        assertNull(vm.uiState.value.interfaz.fichaObservar)

        vm.abrirObservacion(completa)
        vm.cerrarObservacion()
        assertNull(vm.uiState.value.interfaz.fichaObservar)
        vm.confirmarObservacion()
        vm.mensajeMostrado()
        assertNull(vm.uiState.value.interfaz.mensaje)
    }

    @Test
    fun integraElDatasetHistoricoMostrandoElAvance() = runTest {
        coEvery { historico.integrarDataset(any()) } coAnswers {
            firstArg<(Int, Int) -> Unit>().invoke(491, 982)
            resultado
        }
        abrirPantalla()

        vm.integrarDatasetHistorico()

        val interfaz = vm.uiState.value.interfaz
        assertFalse(interfaz.importando)
        assertEquals(resultado, interfaz.resultado)
        assertEquals(0.5f, vm.uiState.value.progresoImportacion, 0.01f)
        assertEquals("Se integraron 982 fichas históricas sin re-registro", interfaz.mensaje)
    }

    @Test
    fun integrarDosVecesNoDuplica() = runTest {
        coEvery { historico.integrarDataset(any()) } returns resultado.copy(integradas = 0, yaExistian = 982)
        abrirPantalla()

        vm.integrarDatasetHistorico()

        assertEquals("El dataset ya estaba integrado (982 fichas)", vm.uiState.value.interfaz.mensaje)
    }

    @Test
    fun siNoSePuedeLeerElDatasetAvisa() = runTest {
        coEvery { historico.integrarDataset(any()) } throws IOException("archivo dañado")
        abrirPantalla()

        vm.integrarDatasetHistorico()

        assertEquals("No se pudo leer el dataset histórico", vm.uiState.value.interfaz.mensaje)
        assertEquals(0f, vm.uiState.value.progresoImportacion, 0f)
    }

    @Test
    fun recibeLasFichasDelServidor() = runTest {
        coEvery { emprendimientos.actualizarDesdeServidor() } returns Result.success(3)
        abrirPantalla()
        assertEquals("Se recibieron 3 fichas nuevas o actualizadas del servidor", vm.uiState.value.interfaz.mensaje)

        coEvery { emprendimientos.actualizarDesdeServidor() } returns Result.success(0)
        vm.actualizarDesdeServidor()
        assertEquals("Las fichas ya están al día con el servidor", vm.uiState.value.interfaz.mensaje)

        coEvery { emprendimientos.actualizarDesdeServidor() } returns Result.failure(IOException())
        vm.actualizarDesdeServidor()
        assertEquals("No se pudo conectar con el servidor. Se muestran las fichas guardadas.", vm.uiState.value.interfaz.mensaje)
        assertFalse(vm.uiState.value.interfaz.actualizando)
    }
}
