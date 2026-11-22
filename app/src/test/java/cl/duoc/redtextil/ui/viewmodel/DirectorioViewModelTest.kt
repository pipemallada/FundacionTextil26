package cl.duoc.redtextil.ui.viewmodel

import cl.duoc.redtextil.DatosPrueba
import cl.duoc.redtextil.ReglaCorrutinas
import cl.duoc.redtextil.data.repository.CatalogoRepository
import cl.duoc.redtextil.data.repository.EmprendimientoRepository
import cl.duoc.redtextil.data.repository.MetricasRepository
import cl.duoc.redtextil.domain.model.Rubro
import cl.duoc.redtextil.suscribir
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DirectorioViewModelTest {

    @get:Rule
    val corrutinas = ReglaCorrutinas()

    private val emprendimientos = mockk<EmprendimientoRepository>().also {
        every { it.buscarDirectorio(any(), any(), any(), any()) } returns flowOf(listOf(DatosPrueba.emprendimiento()))
    }
    private val catalogo = mockk<CatalogoRepository>().also { every { it.portadas() } returns flowOf(mapOf(1L to "/a.jpg")) }
    private val metricas = mockk<MetricasRepository>(relaxed = true)
    private val vm by lazy { DirectorioViewModel(emprendimientos, catalogo, metricas) }

    @Test
    fun muestraLosEmprendimientosActivosConSuPortada() = runTest {
        suscribir(vm.uiState)
        advanceTimeBy(300)

        val estado = vm.uiState.value
        assertFalse(estado.cargando)
        assertEquals(1, estado.resultados.size)
        assertEquals("/a.jpg", estado.portadas[1L])
        assertFalse(estado.filtros.tieneCriterios)
    }

    @Test
    fun combinaLosFiltrosYRegistraLaBusqueda() = runTest {
        suscribir(vm.uiState)
        // La primera busqueda (sin filtros) no se registra; luego se espera a que termine de escribir
        advanceTimeBy(1_300)

        vm.onTexto("bolsos")
        vm.onRegion("Valparaíso")
        vm.onComuna("Viña del Mar")
        vm.onRubro(Rubro.ACCESORIOS)
        advanceTimeBy(1_500)

        val estado = vm.uiState.value
        assertTrue("Viña del Mar" in estado.comunas)
        verify { emprendimientos.buscarDirectorio("bolsos", "Valparaíso", "Viña del Mar", Rubro.ACCESORIOS) }
        coVerify(exactly = 1) { metricas.registrarBusqueda("bolsos", "Valparaíso", Rubro.ACCESORIOS) }
    }

    @Test
    fun lasOpcionesTodasYVolverATocarElRubroQuitanElFiltro() = runTest {
        suscribir(vm.uiState)
        vm.onRegion("Biobío")
        vm.onComuna("Concepción")
        vm.onRubro(Rubro.TEJIDO)

        vm.onComuna(DirectorioViewModel.TODAS_LAS_COMUNAS)
        vm.onRubro(Rubro.TEJIDO)
        assertEquals("", vm.uiState.value.filtros.comuna)
        assertNull(vm.uiState.value.filtros.rubro)

        vm.onRegion(DirectorioViewModel.TODAS_LAS_REGIONES)
        assertEquals("", vm.uiState.value.filtros.region)

        vm.onTexto("lana")
        vm.limpiarFiltros()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.filtros.tieneCriterios)
    }
}
