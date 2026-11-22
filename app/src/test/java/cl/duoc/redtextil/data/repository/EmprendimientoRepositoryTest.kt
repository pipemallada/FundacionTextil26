package cl.duoc.redtextil.data.repository

import cl.duoc.redtextil.DatosPrueba
import cl.duoc.redtextil.data.local.dao.ConteoPorClave
import cl.duoc.redtextil.data.local.dao.EmprendimientoDao
import cl.duoc.redtextil.data.local.database.AppDatabase
import cl.duoc.redtextil.data.remote.RedTextilApiService
import cl.duoc.redtextil.data.remote.aDto
import cl.duoc.redtextil.data.remote.dto.FichaDto
import cl.duoc.redtextil.domain.model.EstadoFicha
import cl.duoc.redtextil.domain.model.EstadoSync
import cl.duoc.redtextil.domain.model.OrigenFicha
import cl.duoc.redtextil.domain.model.RegistroForm
import cl.duoc.redtextil.domain.model.Rubro
import cl.duoc.redtextil.domain.model.TipoCorreo
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmprendimientoRepositoryTest {

    private val dao = mockk<EmprendimientoDao>(relaxed = true)
    private val db = mockk<AppDatabase>().also { every { it.emprendimientoDao() } returns dao }
    private val api = mockk<RedTextilApiService>()
    private val enLinea = MutableStateFlow(true)
    private val red = mockk<NetworkMonitor>().also { every { it.enLinea } returns enLinea }
    private val correos = mockk<CorreoRepository>(relaxed = true)
    private val notificaciones = mockk<NotificationHelper>(relaxed = true)
    private val repo = EmprendimientoRepository(db, { api }, red, correos, notificaciones)

    private val formulario = RegistroForm(
        nombreEmprendedora = "  Camila   Rojas Fuentes ",
        nombreComercial = "Renace Textil",
        rut = "123456785",
        correo = " Camila@Example.com ",
        telefono = "9 0000 0101",
        region = "Valparaíso",
        comuna = "Viña del Mar",
        aceptaPrivacidad = true
    )

    @Test
    fun registrarRechazaUnRutRepetido() = runTest {
        coEvery { dao.buscarPorRut("12.345.678-5") } returns DatosPrueba.emprendimiento()

        val resultado = repo.registrar(formulario)

        assertTrue(resultado is ResultadoRegistro.Error)
        coVerify(exactly = 0) { dao.insertar(any()) }
    }

    @Test
    fun registrarRechazaUnCorreoRepetido() = runTest {
        coEvery { dao.buscarPorRut(any()) } returns null
        coEvery { dao.buscarPorCorreo("camila@example.com") } returns DatosPrueba.emprendimiento()

        val resultado = repo.registrar(formulario) as ResultadoRegistro.Error

        assertEquals("Ya existe una cuenta con este correo electrónico.", resultado.mensaje)
    }

    @Test
    fun registrarGuardaEnviaCorreosYSincronizaConElServidor() = runTest {
        coEvery { dao.buscarPorRut(any()) } returns null
        coEvery { dao.buscarPorCorreo(any()) } returns null
        coEvery { dao.insertar(any()) } returns 5
        coEvery { api.crearFicha(any()) } returns FichaDto(id = 77)

        val resultado = repo.registrar(formulario)

        assertEquals(ResultadoRegistro.Exito(5), resultado)
        coVerify {
            dao.insertar(match {
                it.nombreEmprendedora == "Camila Rojas Fuentes" && it.correo == "camila@example.com" &&
                    it.telefono == "+56900000101" && it.estado == EstadoFicha.PENDIENTE
            })
        }
        coVerify { correos.programar(any(), TipoCorreo.BIENVENIDA, any()) }
        coVerify { correos.programar(any(), TipoCorreo.CONFIRMACION_INSCRIPCION, any()) }
        verify { notificaciones.notificarRegistro("Renace Textil") }
        coVerify { dao.marcarSincronizado(5, 77, any()) }
    }

    @Test
    fun registrarSinSenalNoLlamaAlServidor() = runTest {
        enLinea.value = false
        coEvery { dao.buscarPorRut(any()) } returns null
        coEvery { dao.buscarPorCorreo(any()) } returns null
        coEvery { dao.insertar(any()) } returns 5

        repo.registrar(formulario)

        coVerify(exactly = 0) { api.crearFicha(any()) }
    }

    @Test
    fun guardarFichaObservadaVuelveARevision() = runTest {
        val observada = DatosPrueba.emprendimiento(estado = EstadoFicha.OBSERVADA).copy(observacion = "Falta comuna")
        coEvery { dao.obtenerPorId(1) } returns observada
        coEvery { api.actualizarFicha(100, any()) } returns FichaDto(id = 100)

        repo.guardarFicha(1, DatosPrueba.fichaCompleta.copy(instagram = "renace.textil", sitioWeb = "renace.cl"))

        coVerify {
            dao.actualizar(match {
                it.estado == EstadoFicha.PENDIENTE && it.observacion.isEmpty() &&
                    it.instagram == "@renace.textil" && it.estadoSync == EstadoSync.PENDIENTE_SINCRONIZAR
            })
        }
        coVerify { dao.marcarSincronizado(1, 100, any()) }
    }

    @Test
    fun guardarFichaInexistenteDevuelveNull() = runTest {
        coEvery { dao.obtenerPorId(9) } returns null

        assertNull(repo.guardarFicha(9, DatosPrueba.fichaCompleta))
    }

    @Test
    fun siElServidorYaNoTieneLaFichaLaVuelveACrear() = runTest {
        coEvery { dao.obtenerPorId(1) } returns DatosPrueba.emprendimiento()
        coEvery { api.actualizarFicha(100, any()) } throws DatosPrueba.error404()
        coEvery { api.crearFicha(any()) } returns FichaDto(id = 200)

        repo.guardarFicha(1, DatosPrueba.fichaCompleta)

        coVerify { dao.marcarSincronizado(1, 200, any()) }
    }

    @Test
    fun aprobarProgramaElCorreoDeAprobacionYNotifica() = runTest {
        coEvery { dao.obtenerPorId(1) } returns DatosPrueba.emprendimiento(estado = EstadoFicha.PENDIENTE)
        coEvery { api.actualizarFicha(any(), any()) } returns FichaDto(id = 100)

        val actualizada = repo.cambiarEstado(1, EstadoFicha.ACTIVA)

        assertEquals(EstadoFicha.ACTIVA, actualizada?.estado)
        coVerify { correos.programar(any(), TipoCorreo.APROBACION, any()) }
        verify { notificaciones.notificarCambioEstado("Renace Textil", "Activa") }
    }

    @Test
    fun observarGuardaLaNotaYAvisaPorCorreo() = runTest {
        coEvery { dao.obtenerPorId(1) } returns DatosPrueba.emprendimiento(estado = EstadoFicha.PENDIENTE)
        coEvery { api.actualizarFicha(any(), any()) } returns FichaDto(id = 100)

        val actualizada = repo.cambiarEstado(1, EstadoFicha.OBSERVADA, "  Revisa tu comuna  ")

        assertEquals("Revisa tu comuna", actualizada?.observacion)
        coVerify { correos.programar(any(), TipoCorreo.OBSERVACION, any()) }
    }

    @Test
    fun darDeBajaUnaFichaHistoricaNoNotificaNiEnviaCorreo() = runTest {
        enLinea.value = false
        coEvery { dao.obtenerPorId(1) } returns DatosPrueba.emprendimiento().copy(origen = OrigenFicha.HISTORICO)

        repo.cambiarEstado(1, EstadoFicha.BAJA)

        coVerify(exactly = 0) { correos.programar(any(), any(), any()) }
        verify(exactly = 0) { notificaciones.notificarCambioEstado(any(), any()) }
    }

    @Test
    fun cambiarEstadoDeUnaFichaInexistenteDevuelveNull() = runTest {
        coEvery { dao.obtenerPorId(2) } returns null

        assertNull(repo.cambiarEstado(2, EstadoFicha.BAJA))
    }

    @Test
    fun sincronizarPendientesEnviaCadaFicha() = runTest {
        coEvery { dao.pendientesDeSincronizar() } returns listOf(
            DatosPrueba.emprendimiento(id = 1, remoteId = null),
            DatosPrueba.emprendimiento(id = 2, remoteId = 300)
        )
        coEvery { api.crearFicha(any()) } returns FichaDto(id = 301)
        coEvery { api.actualizarFicha(300, any()) } returns FichaDto(id = 300)

        assertEquals(2, repo.sincronizarPendientes())
        coVerify { dao.marcarSincronizado(1, 301, any()) }
        coVerify { dao.marcarSincronizado(2, 300, any()) }
    }

    @Test
    fun actualizarDesdeServidorAgregaYActualizaFichas() = runTest {
        val sincronizada = DatosPrueba.emprendimiento(id = 3, remoteId = 3)
        val conCambiosLocales = DatosPrueba.emprendimiento(id = 4, remoteId = 4, estadoSync = EstadoSync.PENDIENTE_SINCRONIZAR)
        coEvery { api.listarFichas(any()) } returns listOf(
            FichaDto(id = 1, nombreEmprendedora = "Rosa", nombreComercial = "Hilos", rubros = listOf("TEJIDO"), estado = "PENDIENTE", origen = "APP"),
            FichaDto(id = 2, nombreComercial = "Histórica", origen = "HISTORICO"),
            sincronizada.aDto().copy(nombreComercial = "Renace Textil Valparaíso"),
            conCambiosLocales.aDto().copy(nombreComercial = "No debe pisar"),
            FichaDto(id = null)
        )
        coEvery { dao.obtenerPorRemoteId(any()) } returns null
        coEvery { dao.obtenerPorRemoteId(3) } returns sincronizada
        coEvery { dao.obtenerPorRemoteId(4) } returns conCambiosLocales

        val cambios = repo.actualizarDesdeServidor().getOrThrow()

        assertEquals(2, cambios)
        coVerify(exactly = 1) {
            dao.insertar(match {
                it.remoteId == 1L && it.nombreComercial == "Hilos" && it.rubroPrincipal == "TEJIDO" &&
                    it.estadoSync == EstadoSync.SINCRONIZADO
            })
        }
        coVerify(exactly = 1) { dao.actualizar(match { it.id == 3L && it.nombreComercial == "Renace Textil Valparaíso" }) }
    }

    @Test
    fun actualizarDesdeServidorSinCambiosNoEscribe() = runTest {
        val local = DatosPrueba.emprendimiento(id = 3, remoteId = 3)
        coEvery { api.listarFichas(any()) } returns listOf(local.aDto())
        coEvery { dao.obtenerPorRemoteId(3) } returns local

        assertEquals(0, repo.actualizarDesdeServidor().getOrThrow())
        coVerify(exactly = 0) { dao.actualizar(any()) }
    }

    @Test
    fun actualizarDesdeServidorSinSenalFalla() = runTest {
        enLinea.value = false

        assertTrue(repo.actualizarDesdeServidor().isFailure)
        coVerify(exactly = 0) { api.listarFichas(any()) }
    }

    @Test
    fun consultasDelDirectorioYDeLaAdministradora() = runTest {
        every { dao.observarPorId(1) } returns flowOf(DatosPrueba.emprendimiento())
        every { dao.observarCuentasLocales() } returns flowOf(listOf(DatosPrueba.emprendimiento()))
        every { dao.buscarActivas("bolsos", "Valparaíso", "", "ACCESORIOS") } returns flowOf(listOf(DatosPrueba.emprendimiento()))
        every { dao.observarParaAdmin("PENDIENTE", "ana") } returns flowOf(emptyList())
        every { dao.contarPorEstado() } returns flowOf(listOf(ConteoPorClave("ACTIVA", 4), ConteoPorClave("RARO", 1)))
        every { dao.contarPendientes() } returns flowOf(2)
        coEvery { dao.obtenerPorId(1) } returns DatosPrueba.emprendimiento()

        assertEquals(1L, repo.observarPorId(1).first()?.id)
        assertEquals(1, repo.observarCuentasLocales().first().size)
        assertEquals(1, repo.buscarDirectorio(" bolsos ", "Valparaíso", "", Rubro.ACCESORIOS).first().size)
        assertTrue(repo.observarParaAdmin(EstadoFicha.PENDIENTE, " ana ").first().isEmpty())
        assertEquals(mapOf(EstadoFicha.ACTIVA to 4), repo.contarPorEstado().first())
        assertEquals(2, repo.contarPendientes().first())
        assertEquals(1L, repo.obtener(1)?.id)
    }
}
