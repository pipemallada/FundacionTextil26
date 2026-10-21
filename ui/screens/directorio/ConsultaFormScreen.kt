package cl.duoc.redtextil.ui.screens.directorio

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.redtextil.domain.model.CampoConsulta
import cl.duoc.redtextil.ui.components.BotonGrande
import cl.duoc.redtextil.ui.components.CampoSelector
import cl.duoc.redtextil.ui.components.CampoTexto
import cl.duoc.redtextil.ui.components.CargandoPantalla
import cl.duoc.redtextil.ui.components.EstadoVacio
import cl.duoc.redtextil.ui.components.MensajeSnackbar
import cl.duoc.redtextil.ui.components.TituloPantalla
import cl.duoc.redtextil.ui.theme.VerdeExito
import cl.duoc.redtextil.ui.viewmodel.AppViewModelProvider
import cl.duoc.redtextil.ui.viewmodel.ConsultaFormUiState
import cl.duoc.redtextil.ui.viewmodel.ConsultaFormViewModel

@Composable
fun ConsultaFormScreen(
    onVolver: () -> Unit,
    onVerMisConsultas: () -> Unit,
    viewModel: ConsultaFormViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    MensajeSnackbar(estado.mensaje, viewModel::mensajeMostrado)

    when {
        estado.cargando -> CargandoPantalla()
        estado.emprendimiento == null -> EstadoVacio(
            icono = Icons.Filled.SearchOff,
            titulo = "No encontramos este emprendimiento",
            texto = "Vuelve al directorio y elige otra ficha."
        )
        else -> AnimatedContent(
            targetState = estado.enviada,
            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.92f)) togetherWith fadeOut() },
            label = "consultaEnviada"
        ) { enviada ->
            if (enviada) {
                ConsultaEnviada(
                    nombreComercial = estado.emprendimiento?.nombreComercial.orEmpty(),
                    onVolver = onVolver,
                    onVerMisConsultas = onVerMisConsultas,
                    onOtra = viewModel::nuevaConsulta
                )
            } else {
                FormularioConsulta(estado, viewModel)
            }
        }
    }
}

@Composable
private fun FormularioConsulta(estado: ConsultaFormUiState, vm: ConsultaFormViewModel) {
    val form = estado.form
    fun errorDe(campo: CampoConsulta) = estado.errores[campo]
    fun okDe(campo: CampoConsulta) = campo in estado.validos

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TituloPantalla(
            "Consulta para ${estado.emprendimiento?.nombreComercial.orEmpty()}",
            "La emprendedora recibirá tu mensaje al instante y te responderá por correo o WhatsApp."
        )
        Spacer(Modifier.height(8.dp))
        CampoTexto(
            valor = form.nombre,
            onCambio = vm::onNombre,
            etiqueta = "Tu nombre",
            icono = Icons.Filled.Person,
            error = errorDe(CampoConsulta.NOMBRE),
            valido = okDe(CampoConsulta.NOMBRE),
            mayusculas = KeyboardCapitalization.Words
        )
        CampoTexto(
            valor = form.correo,
            onCambio = vm::onCorreo,
            etiqueta = "Tu correo electrónico",
            icono = Icons.Filled.Email,
            error = errorDe(CampoConsulta.CORREO),
            valido = okDe(CampoConsulta.CORREO),
            teclado = KeyboardType.Email,
            mayusculas = KeyboardCapitalization.None
        )
        CampoTexto(
            valor = form.telefono,
            onCambio = vm::onTelefono,
            etiqueta = "Tu celular",
            icono = Icons.Filled.Phone,
            error = errorDe(CampoConsulta.TELEFONO),
            valido = okDe(CampoConsulta.TELEFONO),
            teclado = KeyboardType.Phone,
            placeholder = "+569 1234 5678",
            ayuda = "Formato +569 y 8 números"
        )
        CampoSelector(
            valor = form.servicio,
            opciones = estado.servicios,
            onSeleccion = vm::onServicio,
            etiqueta = "Servicio que te interesa",
            icono = Icons.Filled.Category,
            error = errorDe(CampoConsulta.SERVICIO),
            valido = okDe(CampoConsulta.SERVICIO)
        )
        CampoTexto(
            valor = form.mensaje,
            onCambio = vm::onMensaje,
            etiqueta = "Tu mensaje",
            icono = Icons.Filled.Edit,
            error = errorDe(CampoConsulta.MENSAJE),
            valido = okDe(CampoConsulta.MENSAJE),
            lineas = 4,
            ayuda = "Cuéntale qué necesitas (${form.mensaje.trim().length}/500)"
        )
        Text(
            "Tus datos solo se comparten con esta emprendedora para responder tu consulta.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 6.dp)
        )
        BotonGrande(
            texto = "Enviar consulta",
            icono = Icons.AutoMirrored.Filled.Send,
            onClick = vm::enviar,
            cargando = estado.enviando
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ConsultaEnviada(
    nombreComercial: String,
    onVolver: () -> Unit,
    onVerMisConsultas: () -> Unit,
    onOtra: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = VerdeExito, modifier = Modifier.size(96.dp))
        Text("¡Consulta enviada!", style = MaterialTheme.typography.headlineSmall)
        Text(
            "$nombreComercial recibió una notificación con tu mensaje. Te responderá pronto.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Text(
            "Si no hay señal, la consulta queda guardada y se envía sola cuando vuelva la conexión.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        BotonGrande(texto = "Ver mis consultas", icono = Icons.Filled.QuestionAnswer, onClick = onVerMisConsultas)
        OutlinedButton(onClick = onVolver, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Text("  Volver a la ficha")
        }
        TextButton(onClick = onOtra) {
            Text("Enviar otra consulta")
        }
    }
}
