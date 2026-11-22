package cl.duoc.redtextil.data.repository

import cl.duoc.redtextil.DatosPrueba
import cl.duoc.redtextil.data.local.dao.ConteoPorClave
import cl.duoc.redtextil.data.local.dao.CorreoDao
import cl.duoc.redtextil.data.remote.RedTextilApiService
import cl.duoc.redtextil.data.remote.dto.RespuestaApi
import cl.duoc.redtextil.domain.model.EstadoCorreo
import cl.duoc.redtextil.domain.model.PlantillaCorreo
import cl.duoc.redtextil.domain.model.TipoCorreo
import cl.duoc.redtextil.util.NetworkMonitor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class CorreoRepositoryTest {

    private val dao = mockk<CorreoDao>(relaxed = true)
    private val api = mockk<RedTextilApiService>()
    private val enLinea = MutableStateFlow(true)
    private val red = mockk<NetworkMonitor>().also { every { it.enLinea } returns enLinea }
    private val repo = CorreoRepository(dao, { api }, red)

    @Test
    fun programarGuardaYEnviaElCorreo() = runTest {
        coEvery { dao.insertar(any()) } returns 9
        coEvery { api.enviarCorreo(any()) } returns RespuestaApi(ok = true)

        val correo = repo.programar(DatosPrueba.emprendimiento(), TipoCorreo.BIENVENIDA, PlantillaCorreo("Hola", "Cuerpo"))

        assertEquals(9L, correo.id)
        assertEquals(EstadoCorreo.ENVIADO_OK, correo.estado)
        assertEquals(1, correo.intentos)
        coVerify { api.enviarCorreo(match { it.destinatario == "camila.rojas@example.com" && it.tipo == "BIENVENIDA" }) }
        coVerify { dao.actualizar(match { it.estado == EstadoCorreo.ENVIADO_OK }) }
    }

    @Test
    fun sinDestinatarioFallaSinLlamarAlServidor() = runTest {
        val correo = repo.enviar(DatosPrueba.correo(destinatario = ""))

        assertEquals(EstadoCorreo.FALLIDO, correo.estado)
        assertEquals("La ficha no tiene correo registrado", correo.detalleError)
        coVerify(exactly = 0) { api.enviarCorreo(any()) }
    }

    @Test
    fun sinSenalQuedaEnCola() = runTest {
        enLinea.value = false

        val correo = repo.enviar(DatosPrueba.correo())

        assertEquals(EstadoCorreo.EN_COLA_OFFLINE, correo.estado)
        coVerify(exactly = 0) { api.enviarCorreo(any()) }
    }

    @Test
    fun elServidorRechazaElEnvio() = runTest {
        coEvery { api.enviarCorreo(any()) } returns RespuestaApi(ok = false, mensaje = "Buzón lleno")

        val correo = repo.enviar(DatosPrueba.correo())

        assertEquals(EstadoCorreo.FALLIDO, correo.estado)
        assertEquals("Buzón lleno", correo.detalleError)
    }

    @Test
    fun errorDeRedMarcaElCorreoComoFallido() = runTest {
        coEvery { api.enviarCorreo(any()) } throws IOException("sin red")

        val correo = repo.enviar(DatosPrueba.correo())

        assertEquals(EstadoCorreo.FALLIDO, correo.estado)
        assertEquals("No se pudo conectar con el servidor de correo", correo.detalleError)
    }

    @Test
    fun reenviarBuscaElCorreoPorId() = runTest {
        coEvery { dao.obtenerPorId(1) } returns null
        coEvery { dao.obtenerPorId(30) } returns DatosPrueba.correo()
        coEvery { api.enviarCorreo(any()) } returns RespuestaApi(ok = true)

        assertNull(repo.reenviar(1))
        assertEquals(EstadoCorreo.ENVIADO_OK, repo.reenviar(30)?.estado)
    }

    @Test
    fun reenviarPendientesCuentaSoloLosEnviados() = runTest {
        coEvery { dao.noEnviados() } returns listOf(DatosPrueba.correo(id = 1), DatosPrueba.correo(id = 2, destinatario = ""))
        coEvery { api.enviarCorreo(any()) } returns RespuestaApi(ok = true)

        assertEquals(1, repo.reenviarPendientes())
    }

    @Test
    fun reenviarPendientesSeDetieneSinSenal() = runTest {
        enLinea.value = false
        coEvery { dao.noEnviados() } returns listOf(DatosPrueba.correo())

        assertEquals(0, repo.reenviarPendientes())
        coVerify(exactly = 0) { api.enviarCorreo(any()) }
    }

    @Test
    fun observaCorreosYConteosPorEstado() = runTest {
        every { dao.observar("FALLIDO") } returns flowOf(listOf(DatosPrueba.correo()))
        every { dao.contarPorEstado() } returns flowOf(listOf(ConteoPorClave("ENVIADO_OK", 3), ConteoPorClave("OTRO", 1)))
        every { dao.contarEnCola() } returns flowOf(2)

        assertEquals(1, repo.observar(EstadoCorreo.FALLIDO).first().size)
        assertEquals(mapOf(EstadoCorreo.ENVIADO_OK to 3), repo.contarPorEstado().first())
        assertEquals(2, repo.contarEnCola().first())
    }
}
