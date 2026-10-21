package cl.duoc.redtextil.ui.screens.emprendedora

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.redtextil.data.local.entity.ConsultaEntity
import cl.duoc.redtextil.domain.model.EstadoConsulta
import cl.duoc.redtextil.ui.components.AvatarIniciales
import cl.duoc.redtextil.ui.components.CampoTexto
import cl.duoc.redtextil.ui.components.CargandoPantalla
import cl.duoc.redtextil.ui.components.EstadoVacio
import cl.duoc.redtextil.ui.components.EtiquetaPequena
import cl.duoc.redtextil.ui.components.MensajeSnackbar
import cl.duoc.redtextil.ui.components.TituloPantalla
import cl.duoc.redtextil.ui.theme.AmarilloAviso
import cl.duoc.redtextil.ui.theme.VerdeExito
import cl.duoc.redtextil.ui.viewmodel.AppViewModelProvider
import cl.duoc.redtextil.ui.viewmodel.BandejaViewModel
import cl.duoc.redtextil.ui.viewmodel.FiltroConsultas
import cl.duoc.redtextil.util.EnlacesContacto
import cl.duoc.redtextil.util.FechaUtil

@Composable
fun BandejaScreen(
    onIrAPerfil: () -> Unit,
    viewModel: BandejaViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    MensajeSnackbar(estado.mensaje, viewModel::mensajeMostrado)

    when {
        estado.cargando -> CargandoPantalla()
        estado.sinSesion -> AvisoSinSesion(onIrAPerfil)
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TituloPantalla(
                    "Consultas de clientes",
                    "${estado.pendientes} por responder · ${estado.respondidas} respondidas"
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FiltroConsultas.entries) { filtro ->
                        val cantidad = when (filtro) {
                            FiltroConsultas.TODAS -> estado.total
                            FiltroConsultas.PENDIENTES -> estado.pendientes
                            FiltroConsultas.RESPONDIDAS -> estado.respondidas
                        }
                        FilterChip(
                            selected = estado.filtro == filtro,
                            onClick = { viewModel.cambiarFiltro(filtro) },
                            label = { Text("${filtro.etiqueta} ($cantidad)") }
                        )
                    }
                }
            }
            if (estado.consultas.isEmpty()) {
                item {
                    EstadoVacio(
                        icono = Icons.Filled.Inbox,
                        titulo = "No hay consultas aquí",
                        texto = "Cuando un cliente te escriba desde el directorio, te llegará una notificación y aparecerá en esta bandeja."
                    )
                }
            }
            items(estado.consultas, key = { it.id }) { consulta ->
                TarjetaConsulta(
                    consulta = consulta,
                    borrador = estado.borradores[consulta.id] ?: consulta.respuesta,
                    error = estado.errores[consulta.id],
                    guardando = estado.guardandoId == consulta.id,
                    onBorrador = { viewModel.onBorrador(consulta.id, it) },
                    onGuardar = { viewModel.guardarRespuesta(consulta) },
                    onAlternarEstado = { viewModel.alternarEstado(consulta) },
                    onWhatsapp = {
                        val url = viewModel.urlWhatsapp(consulta)
                        if (url == null || !EnlacesContacto.abrir(context, url)) {
                            viewModel.avisar("No pudimos abrir WhatsApp para este número")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TarjetaConsulta(
    consulta: ConsultaEntity,
    borrador: String,
    error: String?,
    guardando: Boolean,
    onBorrador: (String) -> Unit,
    onGuardar: () -> Unit,
    onAlternarEstado: () -> Unit,
    onWhatsapp: () -> Unit
) {
    var abierta by rememberSaveable(consulta.id) { mutableStateOf(consulta.estado == EstadoConsulta.PENDIENTE) }
    val pendiente = consulta.estado == EstadoConsulta.PENDIENTE

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { abierta = !abierta }
            ) {
                AvatarIniciales(consulta.nombreCliente, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(consulta.nombreCliente, style = MaterialTheme.typography.titleMedium)
                    Text(consulta.servicio, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(FechaUtil.haceCuanto(consulta.fechaEnvio), style = MaterialTheme.typography.bodySmall)
                }
                EtiquetaPequena(
                    consulta.estado.etiqueta,
                    if (pendiente) AmarilloAviso else VerdeExito
                )
            }
            Text(consulta.mensaje, style = MaterialTheme.typography.bodyLarge)

            AnimatedVisibility(visible = abierta) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(consulta.correoCliente, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(12.dp))
                        Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(consulta.telefonoCliente, style = MaterialTheme.typography.bodySmall)
                    }
                    if (!pendiente && consulta.respuesta.isNotBlank() && borrador == consulta.respuesta) {
                        Surface(
                            color = VerdeExito.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Tu respuesta", fontWeight = FontWeight.Bold, color = VerdeExito)
                                Text(consulta.respuesta, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    CampoTexto(
                        valor = borrador,
                        onCambio = onBorrador,
                        etiqueta = if (pendiente) "Escribe tu respuesta" else "Editar respuesta",
                        icono = Icons.Filled.Edit,
                        error = error,
                        valido = error == null && borrador.trim().length >= 10,
                        lineas = 3,
                        ayuda = "Entre 10 y 500 caracteres (${borrador.trim().length}/500)"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = onGuardar, enabled = !guardando, modifier = Modifier.weight(1f)) {
                            if (guardando) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("Guardar")
                        }
                        OutlinedButton(onClick = onWhatsapp, modifier = Modifier.weight(1f)) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("WhatsApp")
                        }
                    }
                    TextButton(onClick = onAlternarEstado) {
                        Icon(
                            if (pendiente) Icons.Filled.CheckCircle else Icons.Filled.MarkEmailUnread,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (pendiente) "Marcar como respondida" else "Marcar como pendiente")
                    }
                }
            }
        }
    }
}
