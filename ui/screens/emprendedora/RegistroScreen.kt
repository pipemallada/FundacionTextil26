package cl.duoc.redtextil.ui.screens.emprendedora

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.redtextil.domain.model.CampoRegistro
import cl.duoc.redtextil.ui.components.AvisoPrivacidad
import cl.duoc.redtextil.ui.components.BotonGrande
import cl.duoc.redtextil.ui.components.CampoCheckbox
import cl.duoc.redtextil.ui.components.CampoSelector
import cl.duoc.redtextil.ui.components.CampoTexto
import cl.duoc.redtextil.ui.components.MensajeSnackbar
import cl.duoc.redtextil.ui.components.TituloPantalla
import cl.duoc.redtextil.ui.viewmodel.AppViewModelProvider
import cl.duoc.redtextil.ui.viewmodel.RegistroViewModel
import cl.duoc.redtextil.util.ChileGeoData

@Composable
fun RegistroScreen(
    onRegistrado: () -> Unit,
    viewModel: RegistroViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    var verPrivacidad by remember { mutableStateOf(false) }
    MensajeSnackbar(estado.mensaje, viewModel::mensajeMostrado)

    LaunchedEffect(estado.registrado) {
        if (estado.registrado) onRegistrado()
    }

    val form = estado.form
    fun errorDe(campo: CampoRegistro) = estado.errores[campo]
    fun okDe(campo: CampoRegistro) = campo in estado.validos

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TituloPantalla(
            "Crear mi cuenta",
            "Con estos datos te inscribimos. Después podrás completar tu ficha y subir tus fotos."
        )
        Spacer(Modifier.height(8.dp))

        CampoTexto(
            valor = form.nombreEmprendedora,
            onCambio = viewModel::onNombre,
            etiqueta = "Tu nombre y apellido",
            icono = Icons.Filled.Person,
            error = errorDe(CampoRegistro.NOMBRE_EMPRENDEDORA),
            valido = okDe(CampoRegistro.NOMBRE_EMPRENDEDORA),
            mayusculas = KeyboardCapitalization.Words,
            ayuda = "Ej: María José Pérez"
        )
        CampoTexto(
            valor = form.nombreComercial,
            onCambio = viewModel::onNombreComercial,
            etiqueta = "Nombre de tu emprendimiento",
            icono = Icons.Filled.Storefront,
            error = errorDe(CampoRegistro.NOMBRE_COMERCIAL),
            valido = okDe(CampoRegistro.NOMBRE_COMERCIAL),
            mayusculas = KeyboardCapitalization.Words
        )
        CampoTexto(
            valor = form.rut,
            onCambio = viewModel::onRut,
            etiqueta = "RUT",
            icono = Icons.Filled.Badge,
            error = errorDe(CampoRegistro.RUT),
            valido = okDe(CampoRegistro.RUT),
            mayusculas = KeyboardCapitalization.Characters,
            ayuda = "Con guion y dígito verificador. Ej: 12.345.678-5"
        )
        CampoTexto(
            valor = form.correo,
            onCambio = viewModel::onCorreo,
            etiqueta = "Correo electrónico",
            icono = Icons.Filled.Email,
            error = errorDe(CampoRegistro.CORREO),
            valido = okDe(CampoRegistro.CORREO),
            teclado = KeyboardType.Email,
            mayusculas = KeyboardCapitalization.None,
            ayuda = "Aquí te llegará el correo de bienvenida"
        )
        CampoTexto(
            valor = form.telefono,
            onCambio = viewModel::onTelefono,
            etiqueta = "Celular",
            icono = Icons.Filled.Phone,
            error = errorDe(CampoRegistro.TELEFONO),
            valido = okDe(CampoRegistro.TELEFONO),
            teclado = KeyboardType.Phone,
            placeholder = "+569 1234 5678",
            ayuda = "Formato +569 y 8 números"
        )
        CampoSelector(
            valor = form.region,
            opciones = ChileGeoData.nombresRegiones,
            onSeleccion = viewModel::onRegion,
            etiqueta = "Región",
            icono = Icons.Filled.Map,
            error = errorDe(CampoRegistro.REGION),
            valido = okDe(CampoRegistro.REGION),
            ayuda = "Si cambias la región, tendrás que elegir la comuna de nuevo"
        )
        CampoSelector(
            valor = form.comuna,
            opciones = estado.comunas,
            onSeleccion = viewModel::onComuna,
            etiqueta = "Comuna",
            icono = Icons.Filled.Place,
            error = errorDe(CampoRegistro.COMUNA),
            valido = okDe(CampoRegistro.COMUNA),
            habilitado = form.region.isNotBlank(),
            textoVacio = if (form.region.isBlank()) "Primero elige la región" else "Toca para elegir",
            ayuda = if (form.region.isBlank()) "Primero elige la región" else "${estado.comunas.size} comunas en ${form.region}"
        )

        Spacer(Modifier.height(6.dp))
        CampoCheckbox(
            texto = "Acepto el aviso de privacidad y autorizo el uso de mis datos para inscribirme en el directorio (Ley 19.628 y Ley 21.719).",
            marcado = form.aceptaPrivacidad,
            onCambio = viewModel::onPrivacidad,
            error = errorDe(CampoRegistro.ACEPTA_PRIVACIDAD)
        )
        TextButton(onClick = { verPrivacidad = true }) {
            Icon(Icons.Filled.PrivacyTip, contentDescription = null)
            Text("  Leer el aviso de privacidad")
        }

        Spacer(Modifier.height(8.dp))
        BotonGrande(
            texto = "Crear mi cuenta",
            icono = Icons.Filled.HowToReg,
            onClick = viewModel::registrar,
            cargando = estado.guardando
        )
        Text(
            "Al registrarte te enviaremos un correo de bienvenida y otro de confirmación. Si no tienes señal, se envían después.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )
    }

    if (verPrivacidad) {
        AvisoPrivacidad(onCerrar = { verPrivacidad = false })
    }
}
