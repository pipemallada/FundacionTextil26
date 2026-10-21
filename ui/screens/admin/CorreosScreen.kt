package cl.duoc.redtextil.ui.screens.admin

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
import androidx.compose.material.icons.automirrored.filled.ScheduleSend
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.redtextil.data.local.entity.CorreoEntity
import cl.duoc.redtextil.domain.model.EstadoCorreo
import cl.duoc.redtextil.ui.components.BotonGrande
import cl.duoc.redtextil.ui.components.CargandoPantalla
import cl.duoc.redtextil.ui.components.EstadoVacio
import cl.duoc.redtextil.ui.components.EtiquetaPequena
import cl.duoc.redtextil.ui.components.MensajeSnackbar
import cl.duoc.redtextil.ui.components.TituloPantalla
import cl.duoc.redtextil.ui.theme.AmarilloAviso
import cl.duoc.redtextil.ui.theme.VerdeExito
import cl.duoc.redtextil.ui.viewmodel.AppViewModelProvider
import cl.duoc.redtextil.ui.viewmodel.CorreosViewModel
import cl.duoc.redtextil.util.FechaUtil

@Composable
fun CorreosScreen(
    viewModel: CorreosViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    MensajeSnackbar(estado.mensaje, viewModel::mensajeMostrado)

    if (estado.cargando) {
        CargandoPantalla()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TituloPantalla(
                "Correos automáticos",
                "Bienvenidas, confirmaciones de inscripción y avisos que el sistema envía solo"
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = estado.filtro == null,
                        onClick = { viewModel.cambiarFiltro(null) },
                        label = { Text("Todos (${estado.total})") }
                    )
                }
                items(EstadoCorreo.entries) { estadoCorreo ->
                    FilterChip(
                        selected = estado.filtro == estadoCorreo,
                        onClick = { viewModel.cambiarFiltro(estadoCorreo) },
                        label = { Text("${estadoCorreo.name} (${estado.conteos[estadoCorreo] ?: 0})") }
                    )
                }
            }
        }
        if (estado.noEnviados > 0) {
            item {
                BotonGrande(
                    texto = "Reenviar pendientes (${estado.noEnviados})",
                    icono = Icons.Filled.Refresh,
                    onClick = viewModel::reenviarTodos,
                    cargando = estado.reenviandoTodos,
                    colorFondo = MaterialTheme.colorScheme.tertiary,
                    colorTexto = MaterialTheme.colorScheme.onTertiary
                )
            }
        }
        if (estado.correos.isEmpty()) {
            item {
                EstadoVacio(
                    icono = Icons.Filled.MarkEmailRead,
                    titulo = "No hay correos con este filtro",
                    texto = "Cuando una emprendedora se registre o se apruebe su ficha, el correo aparecerá aquí."
                )
            }
        }
        items(estado.correos, key = { it.id }) { correo ->
            TarjetaCorreo(
                correo = correo,
                reenviando = estado.reenviandoId == correo.id,
                onReenviar = { viewModel.reenviar(correo) }
            )
        }
    }
}

@Composable
private fun TarjetaCorreo(correo: CorreoEntity, reenviando: Boolean, onReenviar: () -> Unit) {
    var abierto by rememberSaveable(correo.id) { mutableStateOf(false) }
    val (icono, color) = when (correo.estado) {
        EstadoCorreo.ENVIADO_OK -> Icons.Filled.MarkEmailRead to VerdeExito
        EstadoCorreo.EN_COLA_OFFLINE -> Icons.AutoMirrored.Filled.ScheduleSend to AmarilloAviso
        EstadoCorreo.FALLIDO -> Icons.Filled.ErrorOutline to MaterialTheme.colorScheme.error
    }
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(
            Modifier
                .clickable { abierto = !abierto }
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icono, contentDescription = correo.estado.etiqueta, tint = color, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        correo.asunto,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = if (abierto) 3 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Para: ${correo.nombreDestinatario} <${correo.destinatario.ifBlank { "sin correo" }}>",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                EtiquetaPequena(correo.estado.name, color)
                EtiquetaPequena(correo.tipo.etiqueta, MaterialTheme.colorScheme.secondary)
                Text(
                    FechaUtil.fechaHora(correo.fechaEnvio ?: correo.fechaCreacion),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            AnimatedVisibility(visible = abierto) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(correo.cuerpo, style = MaterialTheme.typography.bodyMedium)
                    Text("Intentos de envío: ${correo.intentos}", style = MaterialTheme.typography.bodySmall)
                    if (correo.detalleError.isNotBlank()) {
                        Text(
                            "Detalle: ${correo.detalleError}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            if (correo.estado != EstadoCorreo.ENVIADO_OK) {
                OutlinedButton(onClick = onReenviar, enabled = !reenviando) {
                    if (reenviando) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Reenviar")
                }
            } else {
                Text(
                    if (abierto) "Toca para cerrar" else "Toca para ver el correo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
