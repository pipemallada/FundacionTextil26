package cl.duoc.redtextil.data.repository

import cl.duoc.redtextil.DatosPrueba
import cl.duoc.redtextil.data.local.dao.ConteoPorClave
import cl.duoc.redtextil.data.local.dao.InteraccionDao
import cl.duoc.redtextil.data.local.entity.InteraccionEntity
import cl.duoc.redtextil.data.remote.RedTextilApiService
import cl.duoc.redtextil.data.remote.dto.RespuestaApi
import cl.duoc.redtextil.domain.model.Rubro
import cl.duoc.redtextil.domain.model.TipoInteraccion
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MetricasRepositoryTest {

    private val dao = mockk<InteraccionDao>(relaxed = true)
    private val api = mockk<RedTextilApiService>()
    private val repo = MetricasRepository(dao) { api }

    @Test
    fun registraBusquedasRecortandoElTexto() = runTest {
        repo.registrarBusqueda("  " + "a".repeat(80) + "  ", "Valparaíso", Rubro.TEJIDO)

        coVerify {
            dao.insertar(match {
                it.tipo == TipoInteraccion.BUSQUEDA && it.detalle.length == 60 && it.rubro == "TEJIDO" && it.region == "Valparaíso"
            })
        }
    }

    @Test
    fun registraContactosConLosDatosDeLaFicha() = runTest {
        repo.registrarContacto(TipoInteraccion.CLIC_WHATSAPP, DatosPrueba.emprendimiento(id = 3), "detalle")

        coVerify {
            dao.insertar(match {
                it.tipo == TipoInteraccion.CLIC_WHATSAPP && it.emprendimientoId == 3L && it.nombreComercial == "Renace Textil"
            })
        }
    }

    @Test
    fun convierteLosConteosEnMapas() = runTest {
        every { dao.contarPorTipo() } returns flowOf(listOf(ConteoPorClave("BUSQUEDA", 5), ConteoPorClave("DESCONOCIDO", 1)))
        every { dao.contactosPorRegion() } returns flowOf(listOf(ConteoPorClave("Valparaíso", 2)))
        every { dao.busquedasPorRegion() } returns flowOf(listOf(ConteoPorClave("Biobío", 4)))
        every { dao.contactosPorRubro() } returns flowOf(listOf(ConteoPorClave("TEJIDO", 1)))
        every { dao.busquedasPorRubro() } returns flowOf(listOf(ConteoPorClave("ARRIENDO", 6)))
        every { dao.observarRecientes(12) } returns flowOf(listOf(InteraccionEntity(tipo = TipoInteraccion.CONSULTA)))
        every { dao.contarNoSincronizadas() } returns flowOf(3)

        assertEquals(mapOf(TipoInteraccion.BUSQUEDA to 5), repo.conteoPorTipo().first())
        assertEquals(mapOf("Valparaíso" to 2), repo.contactosPorRegion().first())
        assertEquals(mapOf("Biobío" to 4), repo.busquedasPorRegion().first())
        assertEquals(mapOf("TEJIDO" to 1), repo.contactosPorRubro().first())
        assertEquals(mapOf("ARRIENDO" to 6), repo.busquedasPorRubro().first())
        assertEquals(1, repo.recientes().first().size)
        assertEquals(3, repo.contarPendientes().first())
    }

    @Test
    fun sinPendientesNoLlamaAlServidor() = runTest {
        coEvery { dao.noSincronizadas() } returns emptyList()

        assertEquals(0, repo.sincronizarPendientes())
        coVerify(exactly = 0) { api.enviarInteracciones(any()) }
    }

    @Test
    fun enviaLasInteraccionesEnLoteYLasMarca() = runTest {
        val lote = listOf(
            InteraccionEntity(id = 1, tipo = TipoInteraccion.BUSQUEDA),
            InteraccionEntity(id = 2, tipo = TipoInteraccion.CONSULTA)
        )
        coEvery { dao.noSincronizadas() } returns lote
        coEvery { api.enviarInteracciones(any()) } returns RespuestaApi(ok = true)

        assertEquals(2, repo.sincronizarPendientes())
        coVerify { api.enviarInteracciones(match { it.map { i -> i.tipo } == listOf("BUSQUEDA", "CONSULTA") }) }
        coVerify { dao.marcarSincronizadas(listOf(1, 2)) }
    }
}
