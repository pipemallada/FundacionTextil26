package cl.duoc.redtextil.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cl.duoc.redtextil.data.local.entity.EmprendimientoEntity
import cl.duoc.redtextil.domain.model.CategoriaTrabajo
import cl.duoc.redtextil.domain.model.Rubro
import cl.duoc.redtextil.ui.theme.VerdeExito
import coil.compose.AsyncImage
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeccionExpandible(
    titulo: String,
    descripcion: String,
    icono: ImageVector,
    abierta: Boolean,
    onAlternar: () -> Unit,
    errores: Int,
    completos: Int,
    total: Int,
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit
) {
    val giro by animateFloatAsState(targetValue = if (abierta) 180f else 0f, label = "giroFlecha")
    ElevatedCard(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAlternar)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text(
                    descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "$completos de $total datos listos",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (completos == total) VerdeExito else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when {
                errores > 0 -> Badge(containerColor = MaterialTheme.colorScheme.error) {
                    Text("$errores", modifier = Modifier.padding(horizontal = 2.dp))
                }
                completos == total -> Icon(Icons.Filled.CheckCircle, contentDescription = "Sección completa", tint = VerdeExito)
            }
            Icon(
                Icons.Filled.ExpandMore,
                contentDescription = if (abierta) "Cerrar sección" else "Abrir sección",
                modifier = Modifier.rotate(giro)
            )
        }
        AnimatedVisibility(
            visible = abierta,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                content = contenido
            )
        }
    }
}

@Composable
fun ImagenCatalogo(
    ruta: String?,
    categoria: CategoriaTrabajo?,
    modifier: Modifier = Modifier,
    descripcion: String? = null
) {
    if (ruta != null) {
        AsyncImage(
            model = File(ruta),
            contentDescription = descripcion,
            contentScale = ContentScale.Crop,
            placeholder = rememberVectorPainter(Icons.Filled.Image),
            error = rememberVectorPainter(categoria.icono()),
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                categoria.icono(),
                contentDescription = descripcion,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipsRubros(rubros: List<Rubro>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        rubros.forEach { rubro ->
            EtiquetaPequena(texto = rubro.etiqueta, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectorRubros(
    seleccionados: Set<Rubro>,
    onAlternar: (Rubro) -> Unit,
    habilitado: (Rubro) -> Boolean,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Rubro.entries.forEach { rubro ->
            val marcado = rubro in seleccionados
            FilterChip(
                selected = marcado,
                onClick = { onAlternar(rubro) },
                enabled = habilitado(rubro),
                label = { Text(rubro.etiqueta) },
                leadingIcon = {
                    Icon(
                        if (marcado) Icons.Filled.CheckCircle else rubro.icono(),
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            )
        }
    }
}

@Composable
fun TarjetaEmprendimiento(
    emprendimiento: EmprendimientoEntity,
    portada: String?,
    onVerFicha: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandida by rememberSaveable(emprendimiento.id) { mutableStateOf(false) }
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            ImagenCatalogo(
                ruta = portada,
                categoria = null,
                descripcion = "Trabajo de ${emprendimiento.nombreComercial}",
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    emprendimiento.nombreComercial,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    emprendimiento.nombreEmprendedora,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Place,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${emprendimiento.comuna}, ${emprendimiento.region}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                ChipsRubros(emprendimiento.listaRubros())
            }
        }
        AnimatedVisibility(visible = expandida) {
            Column(
                Modifier.padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    emprendimiento.descripcion.ifBlank { "Esta emprendedora aún no agrega una descripción." },
                    style = MaterialTheme.typography.bodyMedium
                )
                if (emprendimiento.horarioAtencion.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(emprendimiento.horarioAtencion, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { expandida = !expandida }) {
                Text(if (expandida) "Ver menos" else "Ver más")
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = onVerFicha, modifier = Modifier.padding(end = 8.dp, bottom = 6.dp)) {
                Text("Ver ficha y fotos")
            }
        }
    }
}

@Composable
fun EstadoVacio(
    icono: ImageVector,
    titulo: String,
    texto: String,
    modifier: Modifier = Modifier,
    accion: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (accion != null) {
            Spacer(Modifier.height(6.dp))
            accion()
        }
    }
}

@Composable
fun CargandoPantalla(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun TituloPantalla(titulo: String, subtitulo: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(titulo, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (subtitulo != null) {
            Text(
                subtitulo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
