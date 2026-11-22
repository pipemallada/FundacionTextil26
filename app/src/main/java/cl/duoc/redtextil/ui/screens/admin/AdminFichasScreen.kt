package cl.duoc.redtextil.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.redtextil.data.local.entity.EmprendimientoEntity
import cl.duoc.redtextil.data.repository.HistoricoRepository
import cl.duoc.redtextil.domain.model.EstadoFicha
import cl.duoc.redtextil.domain.model.OrigenFicha
import cl.duoc.redtextil.ui.components.AvatarIniciales
import cl.duoc.redtextil.ui.components.BotonGrande
import cl.duoc.redtextil.ui.components.CampoTexto
import cl.duoc.redtextil.ui.components.ChipEstado
import cl.duoc.redtextil.ui.components.EstadoVacio
import cl.duoc.redtextil.ui.components.EtiquetaPequena
import cl.duoc.redtextil.ui.components.MensajeSnackbar
import cl.duoc.redtextil.ui.components.TituloPantalla
import cl.duoc.redtextil.ui.components.color
import cl.duoc.redtextil.ui.theme.AmarilloAviso
import cl.duoc.redtextil.ui.theme.VerdeExito
import cl.duoc.redtextil.ui.viewmodel.AdminFichasUiState
import cl.duoc.redtextil.ui.viewmodel.AdminFichasViewModel
import cl.duoc.redtextil.ui.viewmodel.AppViewModelProvider
import cl.duoc.redtextil.ui.viewmodel.FichaAdmin

@Composable
fun AdminFichasScreen(
    viewModel: AdminFichasViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    MensajeSnackbar(estado.interfaz.mensaje, viewModel::mensajeMostrado)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TituloPantalla(
                "Gestión de fichas",
                "${estado.totalFichas} fichas en total · altas, correcciones y bajas en un toque"
            )
        }
        item { TarjetaDataset(estado, viewModel::integrarDatasetHistorico) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = estado.filtros.estado == null,
                        onClick = { viewModel.filtrarEstado(null) },
                        label = { Text("Todas (${estado.totalFichas})") }
                    )
                }
                items(EstadoFicha.entries) { estadoFicha ->
                    FilterChip(
                        selected = estado.filtros.estado == estadoFicha,
                        onClick = { viewModel.filtrarEstado(estadoFicha) },
                        label = { Text("${estadoFicha.etiqueta} (${estado.conteos[estadoFicha] ?: 0})") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = estadoFicha.color(),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = estado.filtros.texto,
                onValueChange = viewModel::onTexto,
                label = { Text("Buscar por nombre o comuna") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (estado.filtros.texto.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onTexto("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Borrar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            FilterChip(
                selected = estado.filtros.soloConProblemas,
                onClick = viewModel::alternarSoloProblemas,
                label = { Text("Solo fichas con datos incompletos o inconsistentes") },
                leadingIcon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = AmarilloAviso) }
            )
        }
        if (!estado.cargando && estado.fichas.isEmpty()) {
            item {
                EstadoVacio(
                    icono = Icons.Filled.TaskAlt,
                    titulo = "No hay fichas con este filtro",
                    texto = "Prueba con otro estado o borra la búsqueda."
                )
            }
        }
        items(estado.fichas, key = { it.emprendimiento.id }) { ficha ->
            TarjetaFichaAdmin(
                ficha = ficha,
                procesando = estado.interfaz.procesandoId == ficha.emprendimiento.id,
                onAprobar = { viewModel.aprobar(ficha.emprendimiento) },
                onObservar = { viewModel.abrirObservacion(ficha.emprendimiento) },
                onBaja = { viewModel.darDeBaja(ficha.emprendimiento) }
            )
        }
    }

    estado.interfaz.fichaObservar?.let { ficha ->
        DialogoObservacion(
            ficha = ficha,
            texto = estado.interfaz.textoObservacion,
            error = estado.interfaz.errorObservacion,
            onTexto = viewModel::onTextoObservacion,
            onConfirmar = viewModel::confirmarObservacion,
            onCancelar = viewModel::cerrarObservacion
        )
    }
}

@Composable
private fun TarjetaDataset(estado: AdminFichasUiState, onIntegrar: () -> Unit) {
    val ui = estado.interfaz
    ElevatedCard(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .padding(16.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Migración de datos históricos", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "Integra las ${HistoricoRepository.TOTAL_ESPERADO} fichas de la planilla de Google Sheets. " +
                    "Se unen las columnas de comuna duplicadas, se corrigen teléfonos y enlaces de Instagram, " +
                    "y las emprendedoras no tienen que registrarse de nuevo.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Integradas: ${estado.historicosIntegrados} de ${HistoricoRepository.TOTAL_ESPERADO}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            AnimatedVisibility(
                visible = ui.importando,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { estado.progresoImportacion },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Depurando e integrando ${ui.procesadas} de ${ui.totalImportar}…",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            AnimatedVisibility(
                visible = ui.resultado != null && !ui.importando,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val r = ui.resultado
                if (r != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Resultado de la integración", fontWeight = FontWeight.Bold, color = VerdeExito)
                        Text("Fuente: ${r.fuente}", style = MaterialTheme.typography.bodySmall)
                        Text("Filas leídas: ${r.total} · Nuevas: ${r.integradas} · Ya existían: ${r.yaExistian}", style = MaterialTheme.typography.bodySmall)
                        Text("Activas: ${r.activas} · Observadas para revisar: ${r.observadas}", style = MaterialTheme.typography.bodySmall)
                        Text("Correcciones automáticas aplicadas: ${r.correcciones}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            BotonGrande(
                texto = "Sincronizar Dataset Histórico (982 emprendimientos)",
                icono = Icons.Filled.CloudDownload,
                onClick = onIntegrar,
                cargando = ui.importando,
                colorFondo = MaterialTheme.colorScheme.tertiary,
                colorTexto = MaterialTheme.colorScheme.onTertiary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaFichaAdmin(
    ficha: FichaAdmin,
    procesando: Boolean,
    onAprobar: () -> Unit,
    onObservar: () -> Unit,
    onBaja: () -> Unit
) {
    val e = ficha.emprendimiento
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarIniciales(e.nombreComercial, color = e.estado.color())
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(e.nombreComercial, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${e.nombreEmprendedora} · ${e.comuna.ifBlank { "sin comuna" }}, ${e.region.ifBlank { "sin región" }}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                        EtiquetaPequena(
                            e.origen.etiqueta,
                            if (e.origen == OrigenFicha.HISTORICO) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                        EtiquetaPequena("${ficha.completos}/${ficha.aplicables} datos", MaterialTheme.colorScheme.tertiary)
                    }
                }
                ChipEstado(e.estado)
            }

            if (ficha.problemas.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = VerdeExito, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ficha consistente: visible en búsquedas y con contacto válido", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    ficha.problemas.forEach { problema ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = AmarilloAviso, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(problema, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (e.observacion.isNotBlank()) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Nota: ${e.observacion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (procesando) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (e.estado != EstadoFicha.ACTIVA) {
                        Button(onClick = onAprobar) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(if (e.estado == EstadoFicha.BAJA) "  Reactivar" else "  Aprobar")
                        }
                    }
                    if (e.estado != EstadoFicha.OBSERVADA) {
                        OutlinedButton(onClick = onObservar) {
                            Icon(Icons.Filled.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("  Observar")
                        }
                    }
                    if (e.estado != EstadoFicha.BAJA) {
                        TextButton(onClick = onBaja) {
                            Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("  Dar de baja")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogoObservacion(
    ficha: EmprendimientoEntity,
    texto: String,
    error: String?,
    onTexto: (String) -> Unit,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        icon = { Icon(Icons.Filled.EditNote, contentDescription = null) },
        title = { Text("Observar ${ficha.nombreComercial}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Explica qué dato debe corregir. Le llegará por correo y lo verá en su perfil.",
                    style = MaterialTheme.typography.bodySmall
                )
                CampoTexto(
                    valor = texto,
                    onCambio = onTexto,
                    etiqueta = "Observación",
                    icono = Icons.Filled.EditNote,
                    error = error,
                    valido = error == null && texto.trim().length >= 10,
                    lineas = 3,
                    ayuda = "Entre 10 y 200 caracteres"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirmar) { Text("Enviar observación") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}
