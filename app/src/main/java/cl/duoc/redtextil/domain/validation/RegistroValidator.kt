package cl.duoc.redtextil.domain.validation

import cl.duoc.redtextil.domain.model.CampoRegistro
import cl.duoc.redtextil.domain.model.RegistroForm
import cl.duoc.redtextil.util.ChileGeoData

object RegistroValidator {

    fun validarCampo(campo: CampoRegistro, form: RegistroForm): String? = when (campo) {
        CampoRegistro.NOMBRE_EMPRENDEDORA -> ReglasComunes.validarNombreCompleto(form.nombreEmprendedora)
        CampoRegistro.NOMBRE_COMERCIAL -> ReglasComunes.validarNombreComercial(form.nombreComercial)
        CampoRegistro.RUT -> ReglasComunes.validarRut(form.rut)
        CampoRegistro.CORREO -> ReglasComunes.validarCorreo(form.correo)
        CampoRegistro.TELEFONO -> ReglasComunes.validarTelefono(form.telefono)
        CampoRegistro.REGION -> if (ChileGeoData.existeRegion(form.region)) null else "Elige tu región"
        CampoRegistro.COMUNA -> FichaValidator.validarComuna(form.region, form.comuna)
        // Ley 19.628 y Ley 21.719: sin consentimiento explicito no se guardan datos personales
        CampoRegistro.ACEPTA_PRIVACIDAD ->
            if (form.aceptaPrivacidad) null else "Debes aceptar el aviso de privacidad para crear tu cuenta"
    }

    fun validarTodo(form: RegistroForm): Map<CampoRegistro, String> =
        CampoRegistro.entries.mapNotNull { campo -> validarCampo(campo, form)?.let { campo to it } }.toMap()
}
