package cl.duoc.redtextil.ui.viewmodel

import cl.duoc.redtextil.ReglaCorrutinas
import cl.duoc.redtextil.data.local.entity.InteraccionEntity
import cl.duoc.redtextil.data.repository.MetricasRepository
import cl.duoc.redtextil.domain.model.Rubro
import cl.duoc.redtextil.domain.model.TipoInteraccion
import cl.duoc.redtextil.suscribir
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MetricasViewModelTest {

    @get:Rule
    val corrutinas = ReglaCorrutinas()

    private val metricas = mockk<MetricasRepository>().also {
        every { it.conteoPorTipo() } returns flowOf(
            mapOf(TipoInteraccion.BUSQUEDA to 10, TipoInteraccion.CONSULTA to 3, TipoInteraccion.CLIC_WHATSAPP to 4)
        )
        every { it.contactosPorRegion() } returns flowOf(mapOf("Valparaíso" to 5, "Biobío" to 2))
        every { it.busquedasPorRegion() } returns flowOf(mapOf("Valparaíso" to 8))
        every { it.contactosPorRubro() } returns flowOf(mapOf("TEJIDO" to 6))
        every { it.busquedasPorRubro() } returns flowOf(mapOf("ARRIENDO" to 1))
        every { it.recientes() } returns flowOf(listOf(InteraccionEntity(tipo = TipoInteraccion.CONSULTA)))
        every { it.contarPendientes() } returns flowOf(2)
    }
    private val vm by lazy { MetricasViewModel(metricas) }

    @Test
    fun muestraLas16RegionesYLos7RubrosAunqueTenganCero() = runTest {
        suscribir(vm.uiState)

        val estado = vm.uiState.value
        assertEquals(16, estado.porRegion.size)
        assertEquals(Rubro.entries.size, estado.porRubro.size)
        assertEquals(FilaMetrica("Valparaíso", 5, 8), estado.porRegion.first { it.etiqueta == "Valparaíso" })
        assertEquals(0, estado.porRegion.first { it.etiqueta == "Atacama" }.contactos)
        assertEquals(6, estado.porRubro.first { it.etiqueta == Rubro.TEJIDO.etiqueta }.contactos)
        assertEquals(7, estado.totalEfectivas)
        assertEquals(5, estado.maximoRegion)
        assertEquals(6, estado.maximoRubro)
        assertEquals(1, estado.recientes.size)
        assertEquals(2, estado.pendientesEnvio)
    }

    @Test
    fun cambiaEntreTerritorioYRubro() = runTest {
        suscribir(vm.uiState)

        vm.cambiarVista(VistaMetricas.RUBRO)

        assertEquals(VistaMetricas.RUBRO, vm.uiState.value.vista)
    }
}
