package cl.duoc.redtextil.data.repository

import cl.duoc.redtextil.data.remote.MindicadorApi
import cl.duoc.redtextil.data.remote.dto.IndicadorDto
import cl.duoc.redtextil.data.remote.dto.IndicadoresDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IndicadoresRepositoryTest {

    private val api = mockk<MindicadorApi>()
    private val repo = IndicadoresRepository { api }

    private val respuesta = IndicadoresDto(
        fecha = "2026-11-22T07:00:00.000Z",
        dolar = IndicadorDto(codigo = "dolar", valor = 1000.0, fecha = "2026-11-20T03:00:00.000Z"),
        euro = IndicadorDto(codigo = "euro", valor = 1100.0),
        uf = IndicadorDto(codigo = "uf", valor = 41_384.81)
    )

    @Test
    fun convierteLaRespuestaDeMindicador() = runTest {
        coEvery { api.indicadoresDelDia() } returns respuesta

        val indicadores = repo.obtener().getOrThrow()

        assertEquals(1000.0, indicadores.dolar, 0.0)
        assertEquals(1100.0, indicadores.euro!!, 0.0)
        assertEquals(41_384.81, indicadores.uf!!, 0.0)
        assertEquals("20/11/2026", indicadores.fecha)
    }

    @Test
    fun guardaLosIndicadoresEnMemoriaHastaQueSeFuerce() = runTest {
        coEvery { api.indicadoresDelDia() } returns respuesta

        repo.obtener()
        repo.obtener()
        coVerify(exactly = 1) { api.indicadoresDelDia() }

        repo.obtener(forzar = true)
        coVerify(exactly = 2) { api.indicadoresDelDia() }
    }

    @Test
    fun sinValorDelDolarDevuelveError() = runTest {
        coEvery { api.indicadoresDelDia() } returns respuesta.copy(dolar = null)

        assertTrue(repo.obtener().isFailure)
    }

    @Test
    fun errorDeRedDevuelveFailure() = runTest {
        coEvery { api.indicadoresDelDia() } throws java.io.IOException("sin internet")

        assertTrue(repo.obtener().isFailure)
    }

    @Test
    fun convierteLosPreciosDelCatalogoADolares() {
        val indicadores = IndicadoresEconomicos(dolar = 1000.0, euro = null, uf = null, fecha = "20/11/2026")

        assertEquals("≈ US$ 25,00", indicadores.enDolares(25_000))
        assertEquals("≈ US$ 1.234,57", indicadores.enDolares(1_234_567))
        assertNull(indicadores.enDolares(0))
        assertEquals("$1.008,79", IndicadoresEconomicos.formatearPesos(1008.79))
    }

    @Test
    fun formateaLaFechaIso() {
        assertEquals("01/02/2026", formatearFecha("2026-02-01T03:00:00.000Z"))
        assertEquals("", formatearFecha(null))
        assertEquals("", formatearFecha("sin fecha"))
    }
}
