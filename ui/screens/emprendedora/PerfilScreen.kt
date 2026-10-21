@file:OptIn(ExperimentalMaterial3Api::class)

package cl.duoc.redtextil.ui.screens.emprendedora

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.redtextil.data.local.entity.EmprendimientoEntity
import cl.duoc.redtextil.domain.model.EstadoFicha
import cl.duoc.redtextil.domain.model.EstadoSync
import cl.duoc.redtextil.domain.model.OrigenFicha
import cl.duoc.redtextil.ui.components.AvatarIniciales
import cl.duoc.redtextil.ui.components.AvisoPrivacidad
import cl.duoc.redtextil.ui.components.BotonGrande
import cl.duoc.redtextil.ui.components.CargandoPantalla
import cl.duoc.redtextil.ui.components.ChipEstado
import cl.duoc.redtextil.ui.components.EtiquetaPequena
import cl.duoc.redtextil.ui.components.MensajeSnackbar
import cl.duoc.redtextil.ui.components.ProgresoFicha
import cl.duoc.redtextil.ui.components.TituloPantalla
import cl.duoc.redtextil.ui.theme.AmarilloAviso
import cl.duoc.redtextil.ui.theme.VerdeExito
import cl.duoc.redtextil.ui.viewmodel.AppViewModelProvider
import cl.duoc.redtextil.ui.viewmodel.PerfilUiState
import cl.duoc.redtextil.ui.viewmodel.PerfilViewModel

@Composable
fun PerfilScreen(
    enLinea: Boolean,
    pendientesTotales: Int,
    onSincronizar: () -> Unit,
    onCrearCuenta: () -> Unit,
    onIrAFicha: () -> Unit,
    onIrACatalogo: () -> Unit,
    onIrABandeja: () -> Unit,
    viewModel: PerfilViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    MensajeSnackbar(estado.mensaje, viewModel::mensajeMostrado)

    if (estado.cargando) {
        CargandoPantalla()
        return
    }

    AnimatedContent(
        targetState = estado.sesion == null,
        transitionSpec = { (fadeIn() + slideInVertically { it / 8 }) togetherWith fadeOut() },
        label = "accesoOPanel"
    ) { sinSesion ->
        if (sinSesion) {
            AccesoCuentas(
                cuentas = estado.cuentas,
                onElegir = viewModel::iniciarSesion,
                onCrearCuenta = onCrearCuenta
            )
        } else {
            PanelEmprendedora(
                estado = estado,
                enLinea = enLinea,
                pendientesTotales = pendientesTotales,
                onSincronizar = onSincronizar,
                onIrAFicha = onIrAFicha,
                onIrACatalogo = onIrACatalogo,
                onIrABandeja = onIrABandeja,
                onPausar = viewModel::pausarFicha,
                onReactivar = viewModel::reactivarFicha,
                onCerrarSesion = viewModel::cerrarSesion
            )
        }
    }
}

@Composable
private fun AccesoCuentas(
    cuentas: List<EmprendimientoEntity>,
    onElegir: (EmprendimientoEntity) -> Unit,
    onCrearCuenta: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TituloPantalla(
                "Bienvenida a la Red Textil Circular",
                "Entra con tu cuenta para manejar tu ficha, tus fotos y tus consultas."
            )
        }
        item {
            BotonGrande(texto = "Crear mi cuenta nueva", icono = Icons.Filled.PersonAdd, onClick = onCrearCuenta)
        }
        item {
            Text(
                "¿Ya tienes cuenta? Tócala para entrar",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        items(cuentas, key = { it.id }) { cuenta ->
            Card(
                onClick = { onElegir(cuenta) },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    AvatarIniciales(cuenta.nombreComercial)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(cuenta.nombreComercial, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${cuenta.nombreEmprendedora} · ${cuenta.comuna}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ChipEstado(cuenta.estado)
                            if (cuenta.origen == OrigenFicha.SEMILLA) {
                                EtiquetaPequena("Demo", MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Entrar")
                }
            }
        }
    }
}

@Composable
private fun PanelEmprendedora(
    estado: PerfilUiState,
    enLinea: Boolean,
    pendientesTotales: Int,
    onSincronizar: () -> Unit,
    onIrAFicha: () -> Unit,
    onIrACatalogo: () -> Unit,
    onIrABandeja: () -> Unit,
    onPausar: () -> Unit,
    onReactivar: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    val cuenta = estado.sesion ?: return
    var confirmarPausa by remember { mutableStateOf(false) }
    var verPrivacidad by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ElevatedCard(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarIniciales(cuenta.nombreComercial)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(cuenta.nombreComercial, style = MaterialTheme.typography.titleLarge)
                            Text(cuenta.nombreEmprendedora, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("${cuenta.comuna}, región de ${cuenta.region}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Estado de tu cuenta: ", style = MaterialTheme.typography.bodyMedium)
                        ChipEstado(cuenta.estado)
                    }
                    Text(cuenta.estado.explicacion, style = MaterialTheme.typography.bodyMedium)
                    if (cuenta.estado == EstadoFicha.OBSERVADA && cuenta.observacion.isNotBlank()) {
                        Surface(
                            color = AmarilloAviso.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.padding(12.dp)) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = AmarilloAviso)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("Observación de la administradora", fontWeight = FontWeight.Bold)
                                    Text(cuenta.observacion, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            ElevatedCard(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProgresoFicha(completos = estado.camposCompletos, total = estado.camposAplicables)
                    if (estado.problemas.isNotEmpty()) {
                        estado.problemas.forEach { problema ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = AmarilloAviso, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(problema, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    BotonGrande(
                        texto = if (estado.camposCompletos < estado.camposAplicables) "Completar mi ficha" else "Editar mi ficha",
                        icono = Icons.Filled.Edit,
                        onClick = onIrAFicha
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TarjetaAtajo(
                    numero = estado.totalPublicaciones,
                    texto = "Fotos en mi catálogo",
                    icono = Icons.Filled.PhotoLibrary,
                    onClick = onIrACatalogo,
                    modifier = Modifier.weight(1f)
                )
                TarjetaAtajo(
                    numero = estado.consultasPendientes,
                    texto = "Consultas por responder",
                    icono = Icons.Filled.Forum,
                    onClick = onIrABandeja,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            val sincronizada = cuenta.estadoSync == EstadoSync.SINCRONIZADO
            ElevatedCard(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            when {
                                !enLinea -> Icons.Filled.CloudOff
                                sincronizada && pendientesTotales == 0 -> Icons.Filled.CloudDone
                                else -> Icons.Filled.CloudUpload
                            },
                            contentDescription = null,
                            tint = if (sincronizada && pendientesTotales == 0) VerdeExito else AmarilloAviso,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Sincronización", style = MaterialTheme.typography.titleMedium)
                            Text(
                                when {
                                    pendientesTotales == 0 -> "Todo está enviado al servidor de la Fundación."
                                    enLinea -> "Tienes $pendientesTotales cambios guardados solo en el teléfono."
                                    else -> "Sin señal: $pendientesTotales cambios esperan conexión."
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    BotonGrande(
                        texto = "Sincronizar con servidor",
                        icono = Icons.Filled.Sync,
                        onClick = onSincronizar,
                        habilitado = enLinea,
                        colorFondo = MaterialTheme.colorScheme.tertiary,
                        colorTexto = MaterialTheme.colorScheme.onTertiary
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mi cuenta", style = MaterialTheme.typography.titleMedium)
                if (cuenta.estado == EstadoFicha.BAJA) {
                    OutlinedButton(onClick = onReactivar, enabled = !estado.procesando, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.PlayCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Reactivar mi ficha")
                    }
                } else {
                    OutlinedButton(onClick = { confirmarPausa = true }, enabled = !estado.procesando, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.PauseCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Pausar mi ficha (dar de baja)")
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { verPrivacidad = true }) {
                        Icon(Icons.Filled.PrivacyTip, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Mis datos y privacidad")
                    }
                    TextButton(onClick = onCerrarSesion) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Cerrar sesión")
                    }
                }
            }
        }
    }

    if (confirmarPausa) {
        AlertDialog(
            onDismissRequest = { confirmarPausa = false },
            icon = { Icon(Icons.Filled.PauseCircle, contentDescription = null) },
            title = { Text("¿Pausar tu ficha?") },
            text = { Text("Tu ficha dejará de aparecer en el directorio. Tus fotos y datos se mantienen y puedes reactivarla cuando quieras.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarPausa = false
                    onPausar()
                }) { Text("Sí, pausar") }
            },
            dismissButton = { TextButton(onClick = { confirmarPausa = false }) { Text("Cancelar") } }
        )
    }
    if (verPrivacidad) {
        AvisoPrivacidad(onCerrar = { verPrivacidad = false })
    }
}

@Composable
private fun TarjetaAtajo(
    numero: Int,
    texto: String,
    icono: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = modifier
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Text("$numero", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(texto, style = MaterialTheme.typography.bodySmall)
        }
    }
}
