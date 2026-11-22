package cl.duoc.redtextil.domain.validation

import cl.duoc.redtextil.domain.model.CampoConsulta
import cl.duoc.redtextil.domain.model.ConsultaForm

object ConsultaValidator {

    fun validarCampo(campo: CampoConsulta, form: ConsultaForm, serviciosDisponibles: List<String>): String? =
        when (campo) {
            CampoConsulta.NOMBRE -> ReglasComunes.validarNombrePersona(form.nombre)
            CampoConsulta.CORREO -> ReglasComunes.validarCorreo(form.correo)
            CampoConsulta.TELEFONO -> ReglasComunes.validarTelefono(form.telefono)
            CampoConsulta.SERVICIO ->
                ReglasComunes.validarOpcion(form.servicio, serviciosDisponibles, "Elige el servicio que te interesa")
            CampoConsulta.MENSAJE -> ReglasComunes.validarLargo(form.mensaje, 15, 500, "tu mensaje")
        }

    fun validarTodo(form: ConsultaForm, serviciosDisponibles: List<String>): Map<CampoConsulta, String> =
        CampoConsulta.entries
            .mapNotNull { campo -> validarCampo(campo, form, serviciosDisponibles)?.let { campo to it } }
            .toMap()

    fun validarRespuesta(texto: String): String? =
        ReglasComunes.validarLargo(texto, 10, 500, "tu respuesta")
}
