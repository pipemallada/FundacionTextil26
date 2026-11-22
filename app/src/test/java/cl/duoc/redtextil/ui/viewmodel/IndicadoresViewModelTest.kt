package cl.duoc.redtextil.ui.viewmodel

import cl.duoc.redtextil.ReglaCorrutinas
import cl.duoc.redtextil.data.repository.IndicadoresEconomicos
import cl.duoc.redtextil.data.repository.IndicadoresRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class IndicadoresViewModelTest {

    @get:Rule
    val corrutinas = ReglaCorrutinas()

    private val repositorio = mockk<IndicadoresRepository>()
    private val indicadores = IndicadoresEconomicos(dolar = 1008.79, euro = 1107.59, uf = 41_384.81, fecha = "20/11/2026")

    @Test
    fun muestraLosIndicadoresDelDia() = runTest {
        coEvery { repositorio.obtener(false) } returns Result.success(indicadores)

        val vm = IndicadoresViewModel(repositorio)

        assertEquals(IndicadoresUiState.Listo(indicadores), vm.uiState.value)
    }

    @Test
    fun sinConexionMuestraErrorYPermiteReintentar() = runTest {
        coEvery { repositorio.obtener(false) } returns Result.failure(IOException())
        coEvery { repositorio.obtener(true) } returns Result.success(indicadores)

        val vm = IndicadoresViewModel(repositorio)
        assertTrue(vm.uiState.value is IndicadoresUiState.Error)

        vm.reintentar()
        assertEquals(IndicadoresUiState.Listo(indicadores), vm.uiState.value)
        coVerify { repositorio.obtener(true) }
    }
}
