package cl.duoc.redtextil.ui.viewmodel

import cl.duoc.redtextil.DatosPrueba
import cl.duoc.redtextil.ReglaCorrutinas
import cl.duoc.redtextil.data.local.entity.EmprendimientoEntity
import cl.duoc.redtextil.data.repository.CatalogoRepository
import cl.duoc.redtextil.data.repository.SesionRepository
import cl.duoc.redtextil.domain.model.EstadoSync
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

class CatalogoViewModelTest {

    @get:Rule
    val corrutinas = ReglaCorrutinas()

    private val sesionId = MutableStateFlow<Long?>(1)
    private val actual = MutableStateFlow<EmprendimientoEntity?>(DatosPrueba.emprendimiento())
    private val sesion = mockk<SesionRepository>().also {
        every { it.sesionId } returns sesionId
        every { it.emprendimientoActual } returns actual
    }
    private val catalogo = mockk<CatalogoRepository>(relaxed = true).also {
        every { it.observarDe(1) } returns flowOf(
            listOf(
                DatosPrueba.publicacion(id = 10),
                DatosPrueba.publicacion(id = 11, estadoSync = EstadoSync.SINCRONIZADO)
            )
        )
        coEvery { it.actualizarDesdeServidor(1) } returns Result.success(0)
    }

    @Test
    fun sinSesionPideIniciarSesion() = runTest {
        sesionId.value = null
        actual.value = null
        val vm = CatalogoViewModel(sesion, catalogo)
        suscribir(vm.uiState)

        assertTrue(vm.uiState.value.sinSesion)
        coVerify(exactly = 0) { catalogo.actualizarDesdeServidor(any()) }
    }

    @Test
    fun alAbrirLaCuentaRecibeElCatalogoSinMostrarMensajeSiNoHayCambios() = runTest {
        val vm = CatalogoViewModel(sesion, catalogo)
        suscribir(vm.uiState)

        val estado = vm.uiState.value
        assertEquals(2, estado.publicaciones.size)
        assertEquals(1, estado.pendientesEnvio)
        assertEquals("Renace Textil", estado.nombreComercial)
        assertNull(estado.mensaje)
        assertFalse(estado.actualizando)
        coVerify { catalogo.actualizarDesdeServidor(1) }
    }

    @Test
    fun avisaCuandoLleganCambiosDelServidor() = runTest {
        coEvery { catalogo.actualizarDesdeServidor(1) } returns Result.success(3)
        val vm = CatalogoViewModel(sesion, catalogo)
        suscribir(vm.uiState)

        assertEquals("Se recibieron 3 cambios del servidor", vm.uiState.value.mensaje)
    }

    @Test
    fun actualizarAMandoInformaElResultado() = runTest {
        val vm = CatalogoViewModel(sesion, catalogo)
        suscribir(vm.uiState)

        vm.actualizarDesdeServidor()
        assertEquals("Tu catálogo ya está al día con el servidor", vm.uiState.value.mensaje)

        coEvery { catalogo.actualizarDesdeServidor(1) } returns Result.failure(IOException())
        vm.actualizarDesdeServidor()
        assertEquals("No se pudo conectar con el servidor. Se muestra lo guardado en el teléfono.", vm.uiState.value.mensaje)

        vm.mensajeMostrado()
        assertNull(vm.uiState.value.mensaje)
    }

    @Test
    fun eliminarQuitaElTrabajo() = runTest {
        val vm = CatalogoViewModel(sesion, catalogo)
        suscribir(vm.uiState)

        vm.eliminar(DatosPrueba.publicacion(id = 10))

        coVerify { catalogo.eliminar(10) }
        assertEquals("Se eliminó \"Bolso de mezclilla\" de tu catálogo", vm.uiState.value.mensaje)
    }
}
