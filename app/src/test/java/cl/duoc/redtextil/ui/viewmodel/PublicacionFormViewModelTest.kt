package cl.duoc.redtextil.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import cl.duoc.redtextil.DatosPrueba
import cl.duoc.redtextil.ReglaCorrutinas
import cl.duoc.redtextil.data.repository.CatalogoRepository
import cl.duoc.redtextil.data.repository.SesionRepository
import cl.duoc.redtextil.domain.model.CampoPublicacion
import cl.duoc.redtextil.domain.model.CategoriaTrabajo
import cl.duoc.redtextil.ui.navigation.Rutas
import cl.duoc.redtextil.util.ImageCompressor
import cl.duoc.redtextil.util.ResultadoCompresion
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class PublicacionFormViewModelTest {

    @get:Rule
    val corrutinas = ReglaCorrutinas()

    private val sesionId = MutableStateFlow<Long?>(1)
    private val sesion = mockk<SesionRepository>().also { every { it.sesionId } returns sesionId }
    private val catalogo = mockk<CatalogoRepository>(relaxed = true)
    private val compresor = mockk<ImageCompressor>(relaxed = true)
    private val uri = mockk<Uri>()

    private fun crear(id: Long? = null) =
        PublicacionFormViewModel(SavedStateHandle(mapOf(Rutas.ARG_ID to (id ?: -1L))), sesion, catalogo, compresor)

    private fun PublicacionFormViewModel.completar() {
        onTitulo("Bolso de mezclilla")
        onCategoria(CategoriaTrabajo.ACCESORIO.etiqueta)
        onTecnica("Patchwork")
        onPrecio("25.000")
        onDescripcion("Bolso hecho con jeans reciclados.")
    }

    @Test
    fun unTrabajoNuevoEmpiezaVacio() {
        val estado = crear().uiState.value

        assertFalse(estado.cargando)
        assertFalse(estado.esEdicion)
        assertTrue(estado.errores.isEmpty())
    }

    @Test
    fun editarCargaElTrabajoGuardado() = runTest {
        coEvery { catalogo.obtener(10) } returns DatosPrueba.publicacion(fotoRuta = "/f.jpg").copy(kbOriginal = 2000, kbOptimizado = 100)

        val estado = crear(10).uiState.value

        assertTrue(estado.esEdicion)
        assertEquals("Bolso de mezclilla", estado.form.titulo)
        assertEquals("25000", estado.form.precio)
        assertEquals(CampoPublicacion.entries.toSet(), estado.validos)
        assertEquals(95, estado.porcentajeAhorro)
    }

    @Test
    fun procesarUnaFotoDeLaCamaraLaComprimeYBorraLaCaptura() = runTest {
        coEvery { compresor.comprimir(uri) } returns ResultadoCompresion("/fotos/nueva.jpg", 3000, 150, 1080, 810)
        val vm = crear()

        vm.procesarFoto(uri, desdeCamara = true)

        val estado = vm.uiState.value
        assertEquals("/fotos/nueva.jpg", estado.form.fotoRuta)
        assertEquals("1080 x 810 px", estado.dimensiones)
        assertTrue(estado.fotoNueva)
        assertTrue(CampoPublicacion.FOTO in estado.validos)
        verify { compresor.eliminarCaptura(uri) }
    }

    @Test
    fun siLaFotoNoSePuedeProcesarAvisa() = runTest {
        coEvery { compresor.comprimir(uri) } throws IOException("imagen dañada")
        val vm = crear()

        vm.procesarFoto(uri, desdeCamara = false)

        assertEquals("No pudimos procesar la foto. Prueba con otra imagen.", vm.uiState.value.mensaje)
        assertFalse(vm.uiState.value.procesandoFoto)
        verify(exactly = 0) { compresor.eliminarCaptura(any()) }
    }

    @Test
    fun sinPermisoDeCamaraSugiereLaGaleria() {
        val vm = crear()
        every { compresor.crearUriCamara() } returns uri

        vm.permisoCamaraDenegado()

        assertTrue(vm.uiState.value.mensaje!!.contains("galería"))
        assertEquals(uri, vm.crearUriCamara())
        vm.mensajeMostrado()
        assertNull(vm.uiState.value.mensaje)
    }

    @Test
    fun guardarSinFotoNiDatosMarcaLosErrores() {
        val vm = crear()

        vm.guardar()

        assertEquals("Revisa los 6 datos marcados en rojo", vm.uiState.value.mensaje)
        assertEquals(CampoPublicacion.entries.toSet(), vm.uiState.value.errores.keys)
    }

    @Test
    fun guardarSinSesionPideIniciarla() = runTest {
        sesionId.value = null
        coEvery { compresor.comprimir(uri) } returns ResultadoCompresion("/fotos/a.jpg", 3000, 150, 1080, 810)
        val vm = crear()
        vm.procesarFoto(uri, desdeCamara = false)
        vm.completar()

        vm.guardar()

        assertEquals("Primero inicia sesión en Mi perfil", vm.uiState.value.mensaje)
    }

    @Test
    fun guardarConservaLaFotoElegidaYBorraLasDescartadas() = runTest {
        coEvery { compresor.comprimir(uri) } returnsMany listOf(
            ResultadoCompresion("/fotos/descartada.jpg", 3000, 150, 1080, 810),
            ResultadoCompresion("/fotos/elegida.jpg", 2800, 140, 1080, 810)
        )
        val vm = crear()
        vm.procesarFoto(uri, desdeCamara = false)
        vm.procesarFoto(uri, desdeCamara = false)
        vm.completar()

        vm.guardar()

        assertTrue(vm.uiState.value.guardado)
        coVerify { catalogo.guardar(1, null, match { it.fotoRuta == "/fotos/elegida.jpg" }, 2800, 140) }
        verify { compresor.eliminarArchivo("/fotos/descartada.jpg") }
        verify(exactly = 0) { compresor.eliminarArchivo("/fotos/elegida.jpg") }
    }
}
