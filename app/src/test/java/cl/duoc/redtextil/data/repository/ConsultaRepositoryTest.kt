package cl.duoc.redtextil.data.repository

import cl.duoc.redtextil.DatosPrueba
import cl.duoc.redtextil.data.local.dao.ConsultaDao
import cl.duoc.redtextil.data.local.dao.EmprendimientoDao
import cl.duoc.redtextil.data.local.database.AppDatabase
import cl.duoc.redtextil.data.local.entity.ConsultaConNegocio
import cl.duoc.redtextil.data.remote.RedTextilApiService
import cl.duoc.redtextil.data.remote.dto.ConsultaDto
import cl.duoc.redtextil.domain.model.ConsultaForm
import cl.duoc.redtextil.domain.model.EstadoConsulta
import cl.duoc.redtextil.domain.model.EstadoSync
import cl.duoc.redtextil.domain.model.TipoInteraccion
import cl.duoc.redtextil.util.NetworkMonitor
import cl.duoc.redtextil.util.NotificationHelper
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

class ConsultaRepositoryTest {

    private val dao = mockk<ConsultaDao>(relaxed = true)
    private val fichas = mockk<EmprendimientoDao>(relaxed = true)
    private val db = mockk<AppDatabase>().also {
        every { it.consultaDao() } returns dao
        every { it.emprendimientoDao() } returns fichas
    }
    private val api = mockk<RedTextilApiService>(relaxed = true)
    private val enLinea = MutableStateFlow(true)
    private val red = mockk<NetworkMonitor>().also { every { it.enLinea } returns enLinea }
    private val notificaciones = mockk<NotificationHelper>(relaxed = true)
    private val metricas = mockk<MetricasRepository>(relaxed = true)
    private val repo = ConsultaRepository(db, { api }, red, notificaciones, metricas)

    init {
        coEvery { fichas.obtenerPorId(1) } returns DatosPrueba.emprendimiento(remoteId = 100)
    }

    @Test
    fun enviarGuardaRegistraLaMetricaNotificaYSincroniza() = runTest {
        coEvery { dao.insertar(any()) } returns 21
        coEvery { api.crearConsulta(any()) } returns ConsultaDto(id = 90)
        val form = ConsultaForm(
            nombre = "  Pedro   Soto ",
            correo = " PEDRO@Example.com ",
            telefono = "9 1111 2222",
            servicio = "Reparación",
            mensaje = "  ¿Arreglan cierres de chaquetas?  "
        )

        assertEquals(21L, repo.enviar(DatosPrueba.emprendimiento(), form))

        coVerify {
            dao.insertar(match {
                it.nombreCliente == "Pedro Soto" && it.correoCliente == "pedro@example.com" &&
                    it.telefonoCliente == "+56911112222" && it.mensaje == "¿Arreglan cierres de chaquetas?"
            })
        }
        coVerify { metricas.registrarContacto(TipoInteraccion.CONSULTA, any(), "Reparación") }
        verify { notificaciones.notificarNuevaConsulta("Renace Textil", "Pedro Soto", "Reparación") }
        coVerify { api.crearConsulta(match { it.fichaId == 100L }) }
        coVerify { dao.marcarSincronizada(21, 90, any()) }
    }

    @Test
    fun responderMarcaComoRespondidaYEnvia() = runTest {
        coEvery { dao.obtenerPorId(20) } returns DatosPrueba.consulta(remoteId = 90)
        coEvery { api.actualizarConsulta(90, any()) } returns ConsultaDto(id = 90)

        repo.responder(20, "  Sí, desde $3.000  ")

        coVerify {
            dao.actualizar(match {
                it.estado == EstadoConsulta.RESPONDIDA && it.respuesta == "Sí, desde $3.000" && it.fechaRespuesta != null
            })
        }
        coVerify { api.actualizarConsulta(90, match { it.estado == "RESPONDIDA" }) }
    }

    @Test
    fun responderOCambiarEstadoDeUnaConsultaInexistenteNoHaceNada() = runTest {
        coEvery { dao.obtenerPorId(99) } returns null

        repo.responder(99, "respuesta")
        repo.cambiarEstado(99, EstadoConsulta.PENDIENTE)
        repo.eliminar(99)

        coVerify(exactly = 0) { dao.actualizar(any()) }
        coVerify(exactly = 0) { dao.eliminarDefinitivo(any()) }
    }

    @Test
    fun cambiarEstadoSinSenalQuedaPendienteDeEnvio() = runTest {
        enLinea.value = false
        coEvery { dao.obtenerPorId(20) } returns DatosPrueba.consulta(remoteId = 90, estadoSync = EstadoSync.SINCRONIZADO)

        repo.cambiarEstado(20, EstadoConsulta.RESPONDIDA)

        coVerify { dao.actualizar(match { it.estado == EstadoConsulta.RESPONDIDA && it.estadoSync == EstadoSync.PENDIENTE_SINCRONIZAR }) }
        coVerify(exactly = 0) { api.actualizarConsulta(any(), any()) }
    }

    @Test
    fun eliminarUnaConsultaQueNoLlegoAlServidorLaBorraAlTiro() = runTest {
        val local = DatosPrueba.consulta()
        coEvery { dao.obtenerPorId(20) } returns local

        repo.eliminar(20)

        coVerify { dao.eliminarDefinitivo(local) }
        coVerify(exactly = 0) { api.eliminarConsulta(any()) }
    }

    @Test
    fun eliminarUnaConsultaSincronizadaLaBorraEnElServidor() = runTest {
        coEvery { dao.obtenerPorId(20) } returns DatosPrueba.consulta(remoteId = 90, estadoSync = EstadoSync.SINCRONIZADO)

        repo.eliminar(20)

        coVerify { dao.actualizar(match { it.eliminada }) }
        coVerify { api.eliminarConsulta(90) }
        coVerify { dao.eliminarDefinitivo(match { it.id == 20L }) }
    }

    @Test
    fun eliminarSinSenalSoloMarcaLaConsulta() = runTest {
        enLinea.value = false
        coEvery { dao.obtenerPorId(20) } returns DatosPrueba.consulta(remoteId = 90)

        repo.eliminar(20)

        coVerify { dao.actualizar(match { it.eliminada }) }
        coVerify(exactly = 0) { dao.eliminarDefinitivo(any()) }
    }

    @Test
    fun lasConsultasEsperanQueSuFichaLlegueAlServidor() = runTest {
        coEvery { fichas.obtenerPorId(1) } returns DatosPrueba.emprendimiento(remoteId = null, estadoSync = EstadoSync.PENDIENTE_SINCRONIZAR)
        coEvery { dao.pendientesDeSincronizar() } returns listOf(DatosPrueba.consulta())

        assertEquals(0, repo.sincronizarPendientes())
        coVerify(exactly = 0) { api.crearConsulta(any()) }
    }

    @Test
    fun siElServidorPerdioLaConsultaSeVuelveACrear() = runTest {
        coEvery { dao.pendientesDeSincronizar() } returns listOf(DatosPrueba.consulta(remoteId = 90))
        coEvery { api.actualizarConsulta(90, any()) } throws DatosPrueba.error404()
        coEvery { api.crearConsulta(any()) } returns ConsultaDto(id = 91)

        assertEquals(1, repo.sincronizarPendientes())
        coVerify { dao.marcarSincronizada(20, 91, any()) }
    }

    @Test(expected = retrofit2.HttpException::class)
    fun unErrorDelServidorDistintoDe404SePropaga() = runTest {
        coEvery { dao.pendientesDeSincronizar() } returns listOf(DatosPrueba.consulta(remoteId = 90))
        coEvery { api.actualizarConsulta(90, any()) } throws DatosPrueba.error500()

        repo.sincronizarPendientes()
    }

    @Test
    fun actualizarDesdeServidorRecibeConsultasNuevasYRespuestas() = runTest {
        val enviada = DatosPrueba.consulta(id = 22, remoteId = 92, estadoSync = EstadoSync.SINCRONIZADO)
        coEvery { api.listarConsultas(100) } returns listOf(
            ConsultaDto(id = 91, nombreCliente = "Lucía", servicio = "Arreglo", mensaje = "Necesito ajustar pantalones", estado = "PENDIENTE"),
            ConsultaDto(id = 92, estado = "RESPONDIDA", respuesta = "Listo"),
            ConsultaDto(id = 93, estado = "RESPONDIDA"),
            ConsultaDto(id = 94, estado = "RESPONDIDA", nombreCliente = "Ya respondida")
        )
        coEvery { dao.obtenerPorRemoteId(91) } returns null
        coEvery { dao.obtenerPorRemoteId(92) } returns enviada
        coEvery { dao.obtenerPorRemoteId(93) } returns DatosPrueba.consulta(id = 23, remoteId = 93)
        coEvery { dao.obtenerPorRemoteId(94) } returns null

        assertEquals(3, repo.actualizarDesdeServidor(1).getOrThrow())
        coVerify { dao.insertar(match { it.remoteId == 91L && !it.enviadaDesdeApp && it.estadoSync == EstadoSync.SINCRONIZADO }) }
        verify(exactly = 1) { notificaciones.notificarNuevaConsulta("Renace Textil", "Lucía", "Arreglo") }
        coVerify(exactly = 1) { dao.actualizar(match { it.id == 22L && it.estado == EstadoConsulta.RESPONDIDA && it.respuesta == "Listo" }) }
    }

    @Test
    fun actualizarDesdeServidorSinFichaPublicadaOSinSenal() = runTest {
        coEvery { fichas.obtenerPorId(1) } returns DatosPrueba.emprendimiento(remoteId = null)
        assertEquals(0, repo.actualizarDesdeServidor(1).getOrThrow())

        enLinea.value = false
        assertTrue(repo.actualizarDesdeServidor(1).isFailure)
        coVerify(exactly = 0) { api.listarConsultas(any()) }
    }

    @Test
    fun observaLaBandejaYLasConsultasEnviadas() = runTest {
        every { dao.observarDeEmprendimiento(1) } returns flowOf(listOf(DatosPrueba.consulta()))
        every { dao.contarPendientesDe(1) } returns flowOf(1)
        every { dao.observarEnviadasDesdeApp() } returns flowOf(listOf(ConsultaConNegocio(DatosPrueba.consulta(), "Renace Textil")))
        every { dao.contarPendientes() } returns flowOf(2)

        assertEquals(1, repo.observarBandeja(1).first().size)
        assertEquals(1, repo.contarPendientesDe(1).first())
        assertEquals("Renace Textil", repo.observarEnviadasDesdeApp().first().single().nombreComercial)
        assertEquals(2, repo.contarPendientesSync().first())
    }
}
