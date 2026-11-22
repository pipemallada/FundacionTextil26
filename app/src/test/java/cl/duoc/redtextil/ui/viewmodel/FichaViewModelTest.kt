package cl.duoc.redtextil.ui.viewmodel

import cl.duoc.redtextil.DatosPrueba
import cl.duoc.redtextil.ReglaCorrutinas
import cl.duoc.redtextil.data.local.entity.EmprendimientoEntity
import cl.duoc.redtextil.data.repository.EmprendimientoRepository
import cl.duoc.redtextil.data.repository.SesionRepository
import cl.duoc.redtextil.domain.model.CampoFicha
import cl.duoc.redtextil.domain.model.EstadoFicha
import cl.duoc.redtextil.domain.model.EstadoSync
import cl.duoc.redtextil.domain.model.Rubro
import cl.duoc.redtextil.domain.model.SeccionFicha
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FichaViewModelTest {

    @get:Rule
    val corrutinas = ReglaCorrutinas()

    private val actual = MutableStateFlow<EmprendimientoEntity?>(DatosPrueba.emprendimiento())
    private val sesion = mockk<SesionRepository>().also { every { it.emprendimientoActual } returns actual }
    private val emprendimientos = mockk<EmprendimientoRepository>()

    private fun crear() = FichaViewModel(sesion, emprendimientos)

    @Test
    fun sinSesionPideIniciarSesion() {
        actual.value = null

        assertTrue(crear().uiState.value.sinSesion)
    }

    @Test
    fun cargaLaFichaDeLaCuentaActivaSinMarcarErrores() {
        val estado = crear().uiState.value

        assertFalse(estado.cargando)
        assertEquals(1L, estado.emprendimientoId)
        assertEquals("Renace Textil", estado.datos.nombreComercial)
        assertTrue(estado.errores.isEmpty())
        assertTrue(CampoFicha.NOMBRE_COMERCIAL in estado.validos)
        assertTrue("Viña del Mar" in estado.comunas)
        assertEquals(estado.camposAplicables, estado.camposCompletos)
    }

    @Test
    fun siLaFichaCambiaDeEstadoSeActualizaSinPerderLoEscrito() {
        val vm = crear()
        vm.onTexto(CampoFicha.TECNICAS, "Bordado")

        actual.value = DatosPrueba.emprendimiento(estado = EstadoFicha.OBSERVADA).copy(observacion = "Revisa la comuna")

        val estado = vm.uiState.value
        assertEquals(EstadoFicha.OBSERVADA, estado.estadoFicha)
        assertEquals("Revisa la comuna", estado.observacion)
        assertEquals("Bordado", estado.datos.tecnicas)
    }

    @Test
    fun cadaCampoDeTextoActualizaSuDatoYSeValida() {
        val vm = crear()
        val camposTexto = CampoFicha.entries - setOf(
            CampoFicha.FORMALIZADO, CampoFicha.ATENCION_PRESENCIAL, CampoFicha.REALIZA_ENVIOS,
            CampoFicha.RECIBE_DONACIONES, CampoFicha.ACEPTA_PUBLICACION, CampoFicha.REGION, CampoFicha.RUBROS
        )

        camposTexto.forEach { vm.onTexto(it, "x") }

        val estado = vm.uiState.value
        assertEquals("x", estado.datos.nombreEmprendedora)
        assertEquals("x", estado.datos.horarioAtencion)
        assertTrue(CampoFicha.RUT in estado.errores)
        assertTrue(CampoFicha.CORREO in estado.errores)
        // Los campos condicionales escondidos no muestran error aunque se hayan escrito
        assertFalse(CampoFicha.RUT_EMPRESA in estado.visibles)
        assertTrue(estado.erroresPorSeccion.getValue(SeccionFicha.PERSONALES) > 0)
    }

    @Test
    fun losSwitchesMuestranOEsconDenLasPreguntasCondicionales() {
        val vm = crear()

        vm.onSwitch(CampoFicha.FORMALIZADO, true)
        vm.onSwitch(CampoFicha.ATENCION_PRESENCIAL, true)
        vm.onSwitch(CampoFicha.REALIZA_ENVIOS, false)
        vm.onSwitch(CampoFicha.ACEPTA_PUBLICACION, true)
        vm.onTexto(CampoFicha.RUT_EMPRESA, "76.543.210-3")
        vm.onTexto(CampoFicha.DIRECCION_TALLER, "Pasaje Las Gaviotas 245")
        assertTrue(CampoFicha.RUT_EMPRESA in vm.uiState.value.visibles)
        assertTrue(CampoFicha.DIRECCION_TALLER in vm.uiState.value.visibles)

        vm.onSwitch(CampoFicha.FORMALIZADO, false)
        vm.onSwitch(CampoFicha.NOMBRE_COMERCIAL, true)

        val estado = vm.uiState.value
        assertFalse(CampoFicha.RUT_EMPRESA in estado.visibles)
        assertEquals("", estado.datos.rutEmpresa)
        assertEquals("Pasaje Las Gaviotas 245", estado.datos.direccionTaller)
    }

    @Test
    fun losRubrosActivanSusPreguntasYSeLimitanADos() {
        val vm = crear()

        // La ficha ya trae ACCESORIOS: se suma TALLERES y un tercero queda bloqueado
        vm.onRubro(Rubro.TALLERES)
        vm.onRubro(Rubro.ARRIENDO)
        assertEquals(setOf(Rubro.ACCESORIOS, Rubro.TALLERES), vm.uiState.value.datos.rubros)
        assertEquals("Puedes elegir máximo 2 rubros. Quita uno para elegir otro.", vm.uiState.value.mensaje)
        vm.mensajeMostrado()
        assertNull(vm.uiState.value.mensaje)

        vm.onRubro(Rubro.ACCESORIOS)
        vm.onRubro(Rubro.SUPRARRECICLAJE)
        vm.onSwitch(CampoFicha.RECIBE_DONACIONES, true)
        vm.onTexto(CampoFicha.TIPOS_DONACION, "Jeans y poleras")
        vm.onTexto(CampoFicha.CUPOS_TALLER, "10")
        vm.onTexto(CampoFicha.MODALIDAD_TALLER, "Presencial")

        val estado = vm.uiState.value
        assertEquals(setOf(Rubro.TALLERES, Rubro.SUPRARRECICLAJE), estado.datos.rubros)
        assertTrue(CampoFicha.TIPOS_DONACION in estado.validos)
        assertTrue(CampoFicha.CUPOS_TALLER in estado.validos)
        assertEquals(Rubro.entries.toSet() - estado.datos.rubros, estado.rubrosBloqueados)
    }

    @Test
    fun arriendoPideLasTallas() {
        val vm = crear()

        vm.onRubro(Rubro.ARRIENDO)
        vm.onTexto(CampoFicha.TALLAS_ARRIENDO, "S, M y L")

        assertTrue(CampoFicha.TALLAS_ARRIENDO in vm.uiState.value.validos)
    }

    @Test
    fun cambiarLaRegionBorraLaComuna() {
        val vm = crear()

        vm.onRegion("Biobío")

        assertEquals("", vm.uiState.value.datos.comuna)
        assertTrue(CampoFicha.COMUNA !in vm.uiState.value.validos)
    }

    @Test
    fun alternarSeccionLaAbreYLaCierra() {
        val vm = crear()

        vm.alternarSeccion(SeccionFicha.UBICACION)
        assertTrue(SeccionFicha.UBICACION in vm.uiState.value.seccionesAbiertas)
        vm.alternarSeccion(SeccionFicha.UBICACION)
        assertFalse(SeccionFicha.UBICACION in vm.uiState.value.seccionesAbiertas)
    }

    @Test
    fun guardarConErroresAbreLasSeccionesAfectadas() {
        val vm = crear()
        vm.onTexto(CampoFicha.CORREO, "correo-malo")

        vm.guardar()

        val estado = vm.uiState.value
        assertEquals("Faltan 1 datos por corregir. Están marcados en rojo.", estado.mensaje)
        assertTrue(CampoFicha.CORREO.seccion in estado.seccionesAbiertas)
        coVerify(exactly = 0) { emprendimientos.guardarFicha(any(), any()) }
    }

    @Test
    fun guardarEnviaLaFichaAlServidor() = runTest {
        coEvery { emprendimientos.guardarFicha(1, any()) } returns DatosPrueba.emprendimiento()
        val vm = crear()

        vm.guardar()

        assertEquals("Ficha guardada y enviada al servidor", vm.uiState.value.mensaje)
        assertFalse(vm.uiState.value.guardando)
    }

    @Test
    fun guardarSinSenalAvisaQueSeEnviaraDespues() = runTest {
        coEvery { emprendimientos.guardarFicha(1, any()) } returns
            DatosPrueba.emprendimiento(estadoSync = EstadoSync.PENDIENTE_SINCRONIZAR)
        val vm = crear()

        vm.guardar()

        assertEquals("Ficha guardada en el teléfono. Se enviará cuando haya señal.", vm.uiState.value.mensaje)
        assertEquals(EstadoSync.PENDIENTE_SINCRONIZAR, vm.uiState.value.estadoSync)
    }

    @Test
    fun guardarSinSesionNoHaceNada() {
        actual.value = null
        val vm = crear()

        vm.guardar()

        coVerify(exactly = 0) { emprendimientos.guardarFicha(any(), any()) }
    }
}
