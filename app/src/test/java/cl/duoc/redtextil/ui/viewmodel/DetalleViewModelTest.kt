package cl.duoc.redtextil.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import cl.duoc.redtextil.DatosPrueba
import cl.duoc.redtextil.ReglaCorrutinas
import cl.duoc.redtextil.data.local.entity.EmprendimientoEntity
import cl.duoc.redtextil.data.repository.CatalogoRepository
import cl.duoc.redtextil.data.repository.EmprendimientoRepository
import cl.duoc.redtextil.data.repository.MetricasRepository
import cl.duoc.redtextil.domain.model.TipoInteraccion
import cl.duoc.redtextil.suscribir
import cl.duoc.redtextil.ui.navigation.Rutas
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DetalleViewModelTest {

    @get:Rule
    val corrutinas = ReglaCorrutinas()

    private val ficha = MutableStateFlow<EmprendimientoEntity?>(DatosPrueba.emprendimiento(id = 1))
    private val emprendimientos = mockk<EmprendimientoRepository>().also { every { it.observarPorId(1) } returns ficha }
    private val catalogo = mockk<CatalogoRepository>().also {
        every { it.observarDe(1) } returns flowOf(listOf(DatosPrueba.publicacion()))
    }
    private val metricas = mockk<MetricasRepository>(relaxed = true)
    private val vm by lazy { DetalleViewModel(SavedStateHandle(mapOf(Rutas.ARG_ID to 1L)), emprendimientos, catalogo, metricas) }

    @Test
    fun armaLosEnlacesDeContactoDeLaFicha() = runTest {
        suscribir(vm.uiState)

        val estado = vm.uiState.value
        assertEquals(1, estado.publicaciones.size)
        assertTrue(estado.urlWhatsapp!!.startsWith("https://wa.me/56900000101"))
        assertEquals("https://www.instagram.com/renace.textil/", estado.urlInstagram)
        assertEquals("https://renacetextil.cl", estado.urlSitio)
        assertTrue(estado.aniosTrabajando!! >= 7)
    }

    @Test
    fun registraElClicAntesDeAbrirWhatsappEInstagram() = runTest {
        suscribir(vm.uiState)

        vm.abrirWhatsapp()
        assertTrue(vm.eventos.first() is EventoDetalle.AbrirEnlace)
        coVerify { metricas.registrarContacto(TipoInteraccion.CLIC_WHATSAPP, any()) }

        vm.abrirInstagram()
        assertEquals(EventoDetalle.AbrirEnlace("https://www.instagram.com/renace.textil/"), vm.eventos.first())
        coVerify { metricas.registrarContacto(TipoInteraccion.CLIC_INSTAGRAM, any()) }

        vm.abrirSitio()
        assertEquals(EventoDetalle.AbrirEnlace("https://renacetextil.cl"), vm.eventos.first())
    }

    @Test
    fun sinEnlaceSugiereElFormulario() = runTest {
        ficha.value = DatosPrueba.emprendimiento(id = 1).copy(instagram = "", sitioWeb = "")
        suscribir(vm.uiState)

        vm.abrirInstagram()
        assertEquals(EventoDetalle.Mensaje("Este enlace no está disponible. Usa el formulario de consulta."), vm.eventos.first())
        vm.abrirSitio()
        vm.enlaceNoAbrio()
        assertEquals(EventoDetalle.Mensaje("No encontramos una app para abrir el enlace en este teléfono."), vm.eventos.first())
        coVerify(exactly = 0) { metricas.registrarContacto(any(), any(), any()) }
    }

    @Test
    fun fichaNoDisponible() = runTest {
        ficha.value = null
        suscribir(vm.uiState)

        assertNull(vm.uiState.value.emprendimiento)
        vm.abrirWhatsapp()
        coVerify(exactly = 0) { metricas.registrarContacto(any(), any(), any()) }
    }
}
