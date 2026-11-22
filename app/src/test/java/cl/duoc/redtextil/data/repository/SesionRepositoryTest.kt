package cl.duoc.redtextil.data.repository

import cl.duoc.redtextil.DatosPrueba
import cl.duoc.redtextil.data.local.PreferenciasLocales
import cl.duoc.redtextil.data.local.dao.EmprendimientoDao
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SesionRepositoryTest {

    private val sesionId = MutableStateFlow<Long?>(null)
    private val preferencias = mockk<PreferenciasLocales>(relaxed = true).also {
        every { it.sesionId } returns sesionId
    }
    private val dao = mockk<EmprendimientoDao>()

    @Test
    fun sinSesionNoHayEmprendimientoActual() = runTest {
        assertNull(SesionRepository(preferencias, dao).emprendimientoActual.first())
    }

    @Test
    fun conSesionObservaLaFichaDeEsaCuenta() = runTest {
        sesionId.value = 4
        every { dao.observarPorId(4) } returns flowOf(DatosPrueba.emprendimiento(id = 4))

        val repo = SesionRepository(preferencias, dao)

        assertEquals(4L, repo.sesionId.value)
        assertEquals(4L, repo.emprendimientoActual.first()?.id)
    }

    @Test
    fun iniciarYCerrarSesionSeGuardanEnPreferencias() {
        val repo = SesionRepository(preferencias, dao)

        repo.iniciarSesion(7)
        repo.cerrarSesion()

        verifyOrder {
            preferencias.guardarSesion(7)
            preferencias.guardarSesion(null)
        }
    }
}
