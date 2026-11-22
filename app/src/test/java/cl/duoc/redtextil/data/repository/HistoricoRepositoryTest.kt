package cl.duoc.redtextil.data.repository

import android.content.Context
import android.content.res.AssetManager
import cl.duoc.redtextil.data.local.dao.EmprendimientoDao
import cl.duoc.redtextil.data.local.database.AppDatabase
import cl.duoc.redtextil.data.remote.RedTextilApiService
import cl.duoc.redtextil.data.remote.dto.FichaHistoricaDto
import cl.duoc.redtextil.domain.model.OrigenFicha
import cl.duoc.redtextil.util.NetworkMonitor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoricoRepositoryTest {

    private val csv = """
        id_historico,marca_temporal,nombre_emprendedora,nombre_comercial,correo,telefono,region,comuna_VA,comuna_RM,rubros,instagram,descripcion
        HIST-0001,15/03/2024 10:22:31,ana pérez soto,hilos del mar,ana@example.com,9 0000 1234,Región de Valparaíso,vina del mar,,Reparación y Ajustes,@hilosdelmar,"Arreglos de ropa, con cariño."
        HIST-0002,fecha rara,Rosa Díaz,,,,Metropolitana,,,Tejido y Confección Local,,Tejidos
    """.trimIndent()

    private val assets = mockk<AssetManager>()
    private val context = mockk<Context>().also { every { it.assets } returns assets }
    private val dao = mockk<EmprendimientoDao>(relaxed = true)
    private val db = mockk<AppDatabase>().also { every { it.emprendimientoDao() } returns dao }
    private val api = mockk<RedTextilApiService>()
    private val enLinea = MutableStateFlow(false)
    private val red = mockk<NetworkMonitor>().also { every { it.enLinea } returns enLinea }
    private val repo = HistoricoRepository(context, db, { api }, red)

    init {
        every { assets.open(HistoricoRepository.ARCHIVO_DATASET) } answers { csv.byteInputStream() }
    }

    @Test
    fun sinSenalIntegraElRespaldoLocalDepurado() = runTest {
        coEvery { dao.insertarLote(any()) } returns listOf(5L, -1L)
        val progreso = mutableListOf<Pair<Int, Int>>()

        val resultado = repo.integrarDataset { procesadas, total -> progreso += procesadas to total }

        assertEquals(2, resultado.total)
        assertEquals(1, resultado.integradas)
        assertEquals(1, resultado.yaExistian)
        assertEquals(1, resultado.activas + resultado.observadas)
        assertTrue(resultado.fuente.startsWith("Respaldo local"))
        assertEquals(listOf(0 to 2, 2 to 2), progreso)
        coVerify {
            dao.insertarLote(match { lote ->
                lote.size == 2 && lote.all { it.origen == OrigenFicha.HISTORICO } &&
                    lote[0].nombreComercial == "Hilos del Mar" && lote[1].nombreComercial == "Emprendimiento sin nombre"
            })
        }
    }

    @Test
    fun conSenalUsaElDatasetDelMicroservicio() = runTest {
        enLinea.value = true
        coEvery { api.descargarHistorico() } returns listOf(
            FichaHistoricaDto(
                idHistorico = "HIST-0017",
                marcaTemporal = "18/11/2023 11:48:33",
                nombreEmprendedora = "Fernanda Painemal",
                nombreComercial = "Atelier Saavedra",
                correo = "fernanda@example.com",
                telefono = "+56900100119",
                region = "Región de La Araucanía",
                comunas = mapOf("AN" to "Calama", "AR" to "Saavedra"),
                rubros = "Tejido y Confección Local"
            )
        )
        coEvery { dao.insertarLote(any()) } returns listOf(8L)

        val resultado = repo.integrarDataset { _, _ -> }

        assertEquals("API de la Fundación", resultado.fuente)
        assertEquals(1, resultado.integradas)
        coVerify { dao.insertarLote(match { it.single().comuna == "Saavedra" && it.single().region == "La Araucanía" }) }
        verify(exactly = 0) { assets.open(any()) }
    }

    @Test
    fun siElMicroservicioFallaUsaElRespaldoLocal() = runTest {
        enLinea.value = true
        coEvery { api.descargarHistorico() } throws java.io.IOException("sin servidor")
        coEvery { dao.insertarLote(any()) } returns listOf(5L, 6L)

        val resultado = repo.integrarDataset { _, _ -> }

        assertEquals(2, resultado.integradas)
        assertTrue(resultado.fuente.startsWith("Respaldo local"))
    }

    @Test
    fun cuentaLasFichasHistoricasIntegradas() = runTest {
        every { dao.observarTotalHistoricos() } returns flowOf(982)

        assertEquals(982, repo.totalIntegrados().first())
    }
}
