package cl.duoc.redtextil.ui.viewmodel

import cl.duoc.redtextil.DatosPrueba
import cl.duoc.redtextil.ReglaCorrutinas
import cl.duoc.redtextil.data.local.entity.EmprendimientoEntity
import cl.duoc.redtextil.data.repository.CatalogoRepository
import cl.duoc.redtextil.data.repository.ConsultaRepository
import cl.duoc.redtextil.data.repository.EmprendimientoRepository
import cl.duoc.redtextil.data.repository.SesionRepository
import cl.duoc.redtextil.domain.model.EstadoFicha
import cl.duoc.redtextil.suscribir
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class PerfilViewModelTest {

    @get:Rule
    val corrutinas = ReglaCorrutinas()

    private val actual = MutableStateFlow<EmprendimientoEntity?>(null)
    private val sesion = mockk<SesionRepository>(relaxed = true).also { every { it.emprendimientoActual } returns actual }
    private val emprendimientos = mockk<EmprendimientoRepository>(relaxed = true).also {
        every { it.observarCuentasLocales() } returns flowOf(listOf(DatosPrueba.emprendimiento()))
    }
    private val catalogo = mockk<CatalogoRepository>().also { every { it.contarDe(1) } returns flowOf(6) }
    private val consultas = mockk<ConsultaRepository>().also { every { it.contarPendientesDe(1) } returns flowOf(2) }
    // Se crea despues de que la regla reemplaza Dispatchers.Main (el uiState usa viewModelScope al construirse)
    private val vm by lazy { PerfilViewModel(sesion, emprendimientos, catalogo, consultas) }

    @Test
    fun sinSesionMuestraLasCuentasDisponibles() = runTest {
        suscribir(vm.uiState)

        val estado = vm.uiState.value
        assertNull(estado.sesion)
        assertEquals(1, estado.cuentas.size)
        assertEquals(0, estado.camposCompletos)
    }

    @Test
    fun conSesionResumeLaFicha() = runTest {
        suscribir(vm.uiState)
        actual.value = DatosPrueba.emprendimiento()

        val estado = vm.uiState.value
        assertEquals(6, estado.totalPublicaciones)
        assertEquals(2, estado.consultasPendientes)
        assertEquals(estado.camposAplicables, estado.camposCompletos)
    }

    @Test
    fun iniciarYCerrarSesionAvisanALaUsuaria() = runTest {
        suscribir(vm.uiState)

        vm.iniciarSesion(DatosPrueba.emprendimiento(id = 3))
        assertEquals("Hola Camila, ingresaste a Renace Textil", vm.uiState.value.mensaje)
        verify { sesion.iniciarSesion(3) }

        vm.cerrarSesion()
        assertEquals("Cerraste sesión", vm.uiState.value.mensaje)
        vm.mensajeMostrado()
        assertNull(vm.uiState.value.mensaje)
    }

    @Test
    fun pausarYReactivarCambianElEstadoDeLaFicha() = runTest {
        suscribir(vm.uiState)
        vm.pausarFicha()
        coVerify(exactly = 0) { emprendimientos.cambiarEstado(any(), any(), any()) }

        actual.value = DatosPrueba.emprendimiento()
        vm.pausarFicha()
        coVerify { emprendimientos.cambiarEstado(1, EstadoFicha.BAJA, any()) }

        vm.reactivarFicha()
        coVerify { emprendimientos.cambiarEstado(1, EstadoFicha.PENDIENTE, any()) }
        assertEquals("Tu ficha volvió a revisión de la administradora", vm.uiState.value.mensaje)
    }
}
