package cl.duoc.redtextil.data.repository

import cl.duoc.redtextil.DatosPrueba
import cl.duoc.redtextil.data.local.dao.EmprendimientoDao
import cl.duoc.redtextil.data.local.dao.PortadaCatalogo
import cl.duoc.redtextil.data.local.dao.PublicacionDao
import cl.duoc.redtextil.data.local.database.AppDatabase
import cl.duoc.redtextil.data.remote.ApiClient
import cl.duoc.redtextil.data.remote.RedTextilApiService
import cl.duoc.redtextil.data.remote.dto.PublicacionDto
import cl.duoc.redtextil.data.remote.dto.RespuestaApi
import cl.duoc.redtextil.domain.model.CategoriaTrabajo
import cl.duoc.redtextil.domain.model.EstadoSync
import cl.duoc.redtextil.domain.model.PublicacionForm
import cl.duoc.redtextil.util.ImageCompressor
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
import java.io.File

class CatalogoRepositoryTest {

    private val dao = mockk<PublicacionDao>(relaxed = true)
    private val fichas = mockk<EmprendimientoDao>(relaxed = true)
    private val db = mockk<AppDatabase>().also {
        every { it.publicacionDao() } returns dao
        every { it.emprendimientoDao() } returns fichas
    }
    private val api = mockk<RedTextilApiService>(relaxed = true)
    private val enLinea = MutableStateFlow(true)
    private val red = mockk<NetworkMonitor>().also { every { it.enLinea } returns enLinea }
    private val compresor = mockk<ImageCompressor>(relaxed = true)
    private val repo = CatalogoRepository(db, { api }, red, compresor)

    private val formulario = PublicacionForm(
        titulo = "  Bolso de mezclilla ",
        categoria = CategoriaTrabajo.ACCESORIO,
        tecnica = "Patchwork",
        precio = "$25.000",
        descripcion = "Bolso hecho con jeans reciclados."
    )

    init {
        coEvery { fichas.obtenerPorId(1) } returns DatosPrueba.emprendimiento(remoteId = 100)
    }

    @Test
    fun guardarUnTrabajoNuevoLoEnviaConSuFoto() = runTest {
        val foto = File.createTempFile("trabajo", ".jpg").apply { writeBytes(byteArrayOf(1, 2, 3)); deleteOnExit() }
        coEvery { dao.insertar(any()) } returns 11
        coEvery { dao.obtenerPorId(11) } returns DatosPrueba.publicacion(id = 11, fotoRuta = foto.path)
        coEvery { api.crearPublicacion(any()) } returns PublicacionDto(id = 70)

        val id = repo.guardar(1, null, formulario.copy(fotoRuta = foto.path), 2400, 96)

        assertEquals(11L, id)
        coVerify { dao.insertar(match { it.titulo == "Bolso de mezclilla" && it.precioReferencial == 25_000 && it.kbOptimizado == 96L }) }
        coVerify { api.crearPublicacion(match { it.fichaId == 100L }) }
        coVerify { api.subirFoto(70, any()) }
        coVerify { dao.marcarSincronizada(11, 70, any()) }
    }

    @Test
    fun editarConOtraFotoBorraLaAnteriorYQuedaPendiente() = runTest {
        enLinea.value = false
        coEvery { dao.obtenerPorId(10) } returns DatosPrueba.publicacion(fotoRuta = "/fotos/antigua.jpg", remoteId = 70)

        repo.guardar(1, 10, formulario.copy(fotoRuta = "/fotos/nueva.jpg"), 3000, 120)

        verify { compresor.eliminarArchivo("/fotos/antigua.jpg") }
        coVerify {
            dao.actualizar(match {
                it.fotoRuta == "/fotos/nueva.jpg" && !it.fotoSincronizada && it.kbOriginal == 3000L &&
                    it.estadoSync == EstadoSync.PENDIENTE_SINCRONIZAR
            })
        }
        coVerify(exactly = 0) { api.actualizarPublicacion(any(), any()) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun guardarSinCategoriaNoSePermite() = runTest {
        repo.guardar(1, null, formulario.copy(categoria = null), 0, 0)
    }

    @Test
    fun eliminarUnTrabajoQueNuncaSeEnvioLoBorraAlTiro() = runTest {
        val local = DatosPrueba.publicacion(fotoRuta = "/fotos/a.jpg")
        coEvery { dao.obtenerPorId(10) } returns local

        repo.eliminar(10)

        verify { compresor.eliminarArchivo("/fotos/a.jpg") }
        coVerify { dao.eliminarDefinitivo(local) }
        coVerify(exactly = 0) { api.eliminarPublicacion(any()) }
    }

    @Test
    fun eliminarUnTrabajoPublicadoLoBorraTambienEnElServidor() = runTest {
        coEvery { dao.obtenerPorId(10) } returns DatosPrueba.publicacion(remoteId = 70, estadoSync = EstadoSync.SINCRONIZADO)
        coEvery { api.eliminarPublicacion(70) } throws DatosPrueba.error404()

        repo.eliminar(10)

        coVerify { dao.actualizar(match { it.eliminada && it.estadoSync == EstadoSync.PENDIENTE_SINCRONIZAR }) }
        coVerify { api.eliminarPublicacion(70) }
        coVerify { dao.eliminarDefinitivo(match { it.id == 10L }) }
    }

    @Test
    fun eliminarSinSenalSoloMarcaElTrabajo() = runTest {
        enLinea.value = false
        coEvery { dao.obtenerPorId(10) } returns DatosPrueba.publicacion(remoteId = 70)

        repo.eliminar(10)

        coVerify { dao.actualizar(match { it.eliminada }) }
        coVerify(exactly = 0) { dao.eliminarDefinitivo(any()) }
    }

    @Test
    fun losTrabajosEsperanQueSuFichaLlegueAlServidor() = runTest {
        coEvery { fichas.obtenerPorId(1) } returns DatosPrueba.emprendimiento(remoteId = null, estadoSync = EstadoSync.PENDIENTE_SINCRONIZAR)
        coEvery { dao.pendientesDeSincronizar() } returns listOf(DatosPrueba.publicacion())

        assertEquals(0, repo.sincronizarPendientes())
        coVerify(exactly = 0) { api.crearPublicacion(any()) }
    }

    @Test
    fun siElServidorPerdioElTrabajoSeVuelveACrear() = runTest {
        coEvery { dao.pendientesDeSincronizar() } returns listOf(DatosPrueba.publicacion(remoteId = 70))
        coEvery { api.actualizarPublicacion(70, any()) } throws DatosPrueba.error404()
        coEvery { api.crearPublicacion(any()) } returns PublicacionDto(id = 71)

        assertEquals(1, repo.sincronizarPendientes())
        coVerify { dao.marcarSincronizada(10, 71, any()) }
    }

    @Test
    fun actualizarDesdeServidorAgregaYActualizaTrabajos() = runTest {
        val publicado = DatosPrueba.publicacion(id = 12, remoteId = 72, estadoSync = EstadoSync.SINCRONIZADO, fotoRuta = "/f.jpg")
        coEvery { api.listarPublicaciones(100) } returns listOf(
            PublicacionDto(id = 71, titulo = "Chaqueta", categoria = "PRENDA_INTERVENIDA", precioReferencial = 30_000, urlFoto = "catalogo/71/foto"),
            PublicacionDto(id = 72, titulo = "Bolso XL", categoria = "ACCESORIO"),
            PublicacionDto(id = 73, titulo = "Editado en el teléfono"),
            PublicacionDto(id = null)
        )
        coEvery { dao.obtenerPorRemoteId(71) } returns null
        coEvery { dao.obtenerPorRemoteId(72) } returns publicado
        coEvery { dao.obtenerPorRemoteId(73) } returns DatosPrueba.publicacion(id = 13, remoteId = 73)

        assertEquals(2, repo.actualizarDesdeServidor(1).getOrThrow())
        coVerify {
            dao.insertar(match {
                it.remoteId == 71L && it.emprendimientoId == 1L && it.categoria == CategoriaTrabajo.PRENDA_INTERVENIDA &&
                    it.fotoRuta == ApiClient.BASE_URL + "catalogo/71/foto"
            })
        }
        coVerify(exactly = 1) { dao.actualizar(match { it.id == 12L && it.titulo == "Bolso XL" && it.fotoRuta == "/f.jpg" }) }
    }

    @Test
    fun actualizarDesdeServidorSinFichaPublicadaNoHaceNada() = runTest {
        coEvery { fichas.obtenerPorId(1) } returns DatosPrueba.emprendimiento(remoteId = null)

        assertEquals(0, repo.actualizarDesdeServidor(1).getOrThrow())
        coVerify(exactly = 0) { api.listarPublicaciones(any()) }
    }

    @Test
    fun actualizarDesdeServidorSinSenalFalla() = runTest {
        enLinea.value = false

        assertTrue(repo.actualizarDesdeServidor(1).isFailure)
    }

    @Test
    fun observaElCatalogoYSusPortadas() = runTest {
        every { dao.observarDeEmprendimiento(1) } returns flowOf(listOf(DatosPrueba.publicacion()))
        every { dao.contarDeEmprendimiento(1) } returns flowOf(1)
        every { dao.observarPortadas() } returns flowOf(listOf(PortadaCatalogo(1, "/a.jpg"), PortadaCatalogo(2, null)))
        every { dao.contarPendientes() } returns flowOf(4)
        coEvery { dao.obtenerPorId(10) } returns DatosPrueba.publicacion()
        coEvery { api.eliminarPublicacion(any()) } returns RespuestaApi(ok = true)

        assertEquals(1, repo.observarDe(1).first().size)
        assertEquals(1, repo.contarDe(1).first())
        assertEquals(mapOf(1L to "/a.jpg"), repo.portadas().first())
        assertEquals(4, repo.contarPendientes().first())
        assertEquals("Bolso de mezclilla", repo.obtener(10)?.titulo)
    }
}
