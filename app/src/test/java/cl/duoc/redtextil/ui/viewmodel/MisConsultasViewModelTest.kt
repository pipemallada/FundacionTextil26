package cl.duoc.redtextil.ui.viewmodel

import cl.duoc.redtextil.DatosPrueba
import cl.duoc.redtextil.ReglaCorrutinas
import cl.duoc.redtextil.data.local.entity.ConsultaConNegocio
import cl.duoc.redtextil.data.repository.ConsultaRepository
import cl.duoc.redtextil.suscribir
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class MisConsultasViewModelTest {

    @get:Rule
    val corrutinas = ReglaCorrutinas()

    @Test
    fun muestraLasConsultasEnviadasDesdeLaApp() = runTest {
        val consultas = mockk<ConsultaRepository>().also {
            every { it.observarEnviadasDesdeApp() } returns flowOf(listOf(ConsultaConNegocio(DatosPrueba.consulta(), "Renace Textil")))
        }
        val vm = MisConsultasViewModel(consultas)
        suscribir(vm.uiState)

        assertFalse(vm.uiState.value.cargando)
        assertEquals("Renace Textil", vm.uiState.value.consultas.single().nombreComercial)
    }
}
