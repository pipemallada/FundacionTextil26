package cl.duoc.redtextil.ui.screens.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.redtextil.data.local.entity.InteraccionEntity
import cl.duoc.redtextil.domain.model.Rubro
import cl.duoc.redtextil.domain.model.TipoInteraccion
import cl.duoc.redtextil.ui.components.BarraComparativa
import cl.duoc.redtextil.ui.components.CargandoPantalla
import cl.duoc.redtextil.ui.components.TituloPantalla
import cl.duoc.redtextil.ui.theme.AmarilloAviso
import cl.duoc.redtextil.ui.theme.AzulInfo
import cl.duoc.redtextil.ui.theme.VerdeExito
import cl.duoc.redtextil.ui.viewmodel.AppViewModelProvider
import cl.duoc.redtextil.ui.viewmodel.FilaMetrica
import cl.duoc.redtextil.ui.viewmodel.MetricasViewModel
import cl.duoc.redtextil.ui.viewmodel.VistaMetricas
import cl.duoc.redtextil.util.FechaUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricasScreen(
    viewModel: MetricasViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
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
                "Trazabilidad e impacto",
                "Cada búsqueda, consulta y clic del directorio queda registrado en el teléfono"
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TarjetaContador(
                    titulo = TipoInteraccion.BUSQUEDA.etiqueta,
                    valor = estado.porTipo[TipoInteraccion.BUSQUEDA] ?: 0,
                    icono = Icons.Filled.Search,
                    color = AzulInfo,
                    modifier = Modifier.weight(1f)
                )
                TarjetaContador(
                    titulo = TipoInteraccion.CONSULTA.etiqueta,
                    valor = estado.porTipo[TipoInteraccion.CONSULTA] ?: 0,
                    icono = Icons.AutoMirrored.Filled.Send,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TarjetaContador(
                    titulo = TipoInteraccion.CLIC_WHATSAPP.etiqueta,
                    valor = estado.porTipo[TipoInteraccion.CLIC_WHATSAPP] ?: 0,
                    icono = Icons.AutoMirrored.Filled.Chat,
                    color = VerdeExito,
                    modifier = Modifier.weight(1f)
                )
                TarjetaContador(
                    titulo = TipoInteraccion.CLIC_INSTAGRAM.etiqueta,
                    valor = estado.porTipo[TipoInteraccion.CLIC_INSTAGRAM] ?: 0,
                    icono = Icons.Filled.PhotoCamera,
                    color = AmarilloAviso,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Insights, contentDescription = null, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("${estado.totalEfectivas}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Interacciones efectivas (consultas + clics en WhatsApp e Instagram)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                VistaMetricas.entries.forEachIndexed { i, vista ->
                    SegmentedButton(
                        selected = estado.vista == vista,
                        onClick = { viewModel.cambiarVista(vista) },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = VistaMetricas.entries.size)
                    ) {
                        Text(vista.etiqueta)
                    }
                }
            }
        }
        item {
            ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                AnimatedContent(
                    targetState = estado.vista,
                    transitionSpec = {
                        val haciaDerecha = targetState.ordinal > initialState.ordinal
                        (fadeIn() + slideInHorizontally { if (haciaDerecha) it / 3 else -it / 3 }) togetherWith
                            (fadeOut() + slideOutHorizontally { if (haciaDerecha) -it / 3 else it / 3 })
                    },
                    label = "vistaMetricas"
                ) { vista ->
                    when (vista) {
                        VistaMetricas.TERRITORIO -> GraficoBarras(
                            titulo = "Contactos efectivos por región (16 regiones)",
                            filas = estado.porRegion,
                            maximo = estado.maximoRegion,
                            color = MaterialTheme.colorScheme.primary
                        )
                        VistaMetricas.RUBRO -> GraficoBarras(
                            titulo = "Contactos efectivos por rubro (${Rubro.entries.size} rubros)",
                            filas = estado.porRubro,
                            maximo = estado.maximoRubro,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(6.dp))
                Text(
                    if (estado.pendientesEnvio == 0) "Todas las métricas están enviadas al servidor"
                    else "${estado.pendientesEnvio} registros esperan sincronizarse con el servidor",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        item {
            Text("Últimos registros", style = MaterialTheme.typography.titleMedium)
        }
        items(estado.recientes, key = { it.id }) { registro ->
            FilaRegistro(registro)
        }
    }
}

@Composable
private fun TarjetaContador(
    titulo: String,
    valor: Int,
    icono: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icono, contentDescription = null, tint = color)
            Text("$valor", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
            Text(titulo, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun GraficoBarras(titulo: String, filas: List<FilaMetrica>, maximo: Int, color: Color) {
    Column(Modifier.padding(16.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        Text(
            "Barra: consultas y clics. Debajo: búsquedas filtradas en ese territorio o rubro.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        filas.forEach { fila ->
            BarraComparativa(
                etiqueta = fila.etiqueta,
                valor = fila.contactos,
                maximo = maximo,
                color = color,
                detalle = "${fila.busquedas} búsquedas"
            )
        }
    }
}

@Composable
private fun FilaRegistro(registro: InteraccionEntity) {
    val (icono, color) = when (registro.tipo) {
        TipoInteraccion.BUSQUEDA -> Icons.Filled.Search to AzulInfo
        TipoInteraccion.CONSULTA -> Icons.AutoMirrored.Filled.Send to MaterialTheme.colorScheme.primary
        TipoInteraccion.CLIC_WHATSAPP -> Icons.AutoMirrored.Filled.Chat to VerdeExito
        TipoInteraccion.CLIC_INSTAGRAM -> Icons.Filled.PhotoCamera to AmarilloAviso
    }
    val descripcion = when (registro.tipo) {
        TipoInteraccion.BUSQUEDA -> "Búsqueda \"${registro.detalle.ifBlank { "sin texto" }}\"" +
            if (registro.region.isNotBlank()) " en ${registro.region}" else ""
        TipoInteraccion.CONSULTA -> "Consulta enviada a ${registro.nombreComercial} (${registro.region})"
        TipoInteraccion.CLIC_WHATSAPP -> "Clic en WhatsApp de ${registro.nombreComercial} (${registro.region})"
        TipoInteraccion.CLIC_INSTAGRAM -> "Clic en Instagram de ${registro.nombreComercial} (${registro.region})"
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(descripcion, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(FechaUtil.haceCuanto(registro.fecha), style = MaterialTheme.typography.labelSmall)
    }
}
