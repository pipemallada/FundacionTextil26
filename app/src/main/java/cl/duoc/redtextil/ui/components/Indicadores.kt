package cl.duoc.redtextil.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cl.duoc.redtextil.domain.model.EstadoFicha
import cl.duoc.redtextil.ui.theme.AmarilloAviso
import cl.duoc.redtextil.ui.theme.AzulInfo
import cl.duoc.redtextil.ui.theme.CafeTexto
import cl.duoc.redtextil.ui.theme.Crema
import cl.duoc.redtextil.util.TextoUtil

private enum class EstadoBanner { OCULTO, SIN_SENAL, SINCRONIZANDO, PENDIENTES }

@Composable
fun BannerConexion(
    enLinea: Boolean,
    pendientes: Int,
    sincronizando: Boolean,
    onSincronizar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado = when {
        !enLinea -> EstadoBanner.SIN_SENAL
        sincronizando -> EstadoBanner.SINCRONIZANDO
        pendientes > 0 -> EstadoBanner.PENDIENTES
        else -> EstadoBanner.OCULTO
    }
    AnimatedVisibility(
        visible = estado != EstadoBanner.OCULTO,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        AnimatedContent(
            targetState = estado,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            label = "bannerConexion"
        ) { actual ->
            when (actual) {
                EstadoBanner.SIN_SENAL -> {
                    val pulso = rememberInfiniteTransition(label = "pulsoOffline")
                    val opacidad by pulso.animateFloat(
                        initialValue = 0.35f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                        label = "opacidadOffline"
                    )
                    FranjaAviso(
                        color = AmarilloAviso,
                        titulo = "Modo Offline / Zona Rural",
                        texto = if (pendientes > 0) {
                            "Sin señal. Tienes $pendientes cambios guardados en el teléfono; se enviarán cuando vuelva la conexión."
                        } else {
                            "Sin señal. Puedes seguir trabajando: todo se guarda en el teléfono."
                        },
                        iconoModifier = Modifier.alpha(opacidad),
                        conIconoOffline = true
                    )
                }
                EstadoBanner.SINCRONIZANDO -> Column(Modifier.fillMaxWidth().background(AzulInfo)) {
                    FranjaAviso(
                        color = AzulInfo,
                        titulo = "Sincronizando con el servidor",
                        texto = "Enviando tus cambios a la Fundación…",
                        iconoModifier = Modifier
                    )
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Crema,
                        trackColor = AzulInfo
                    )
                }
                EstadoBanner.PENDIENTES -> FranjaAviso(
                    color = MaterialTheme.colorScheme.tertiary,
                    titulo = "Cambios por enviar: $pendientes",
                    texto = "Hay conexión. Envía tus cambios para que se vean en el directorio.",
                    iconoModifier = Modifier,
                    accion = "Sincronizar",
                    onAccion = onSincronizar
                )
                EstadoBanner.OCULTO -> Spacer(Modifier.height(0.dp))
            }
        }
    }
}

@Composable
private fun FranjaAviso(
    color: Color,
    titulo: String,
    texto: String,
    iconoModifier: Modifier,
    conIconoOffline: Boolean = false,
    accion: String? = null,
    onAccion: () -> Unit = {}
) {
    Surface(color = color, contentColor = Crema, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    conIconoOffline -> Icons.Filled.CloudOff
                    accion != null -> Icons.Filled.CloudUpload
                    else -> Icons.Filled.Sync
                },
                contentDescription = null,
                modifier = iconoModifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text(texto, style = MaterialTheme.typography.bodySmall)
            }
            if (accion != null) {
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = onAccion) { Text(accion) }
            }
        }
    }
}

@Composable
fun ChipEstado(estado: EstadoFicha, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = estado,
        transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.8f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.8f)) },
        label = "chipEstado",
        modifier = modifier
    ) { actual ->
        val color = actual.color()
        Surface(
            color = color.copy(alpha = 0.14f),
            contentColor = color,
            shape = RoundedCornerShape(50)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(actual.icono(), contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(actual.etiqueta, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EtiquetaPequena(texto: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun BarraComparativa(
    etiqueta: String,
    valor: Int,
    maximo: Int,
    color: Color,
    modifier: Modifier = Modifier,
    detalle: String? = null
) {
    val fraccion = if (maximo > 0) valor.toFloat() / maximo else 0f
    val animada by animateFloatAsState(
        targetValue = fraccion,
        animationSpec = tween(durationMillis = 800),
        label = "barraComparativa"
    )
    Column(modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                etiqueta,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text("$valor", style = MaterialTheme.typography.titleMedium, color = CafeTexto)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(color.copy(alpha = 0.15f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animada)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(7.dp))
                    .background(color)
            )
        }
        if (detalle != null) {
            Text(
                detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AvatarIniciales(nombre: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            TextoUtil.iniciales(nombre),
            color = Crema,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun ProgresoFicha(completos: Int, total: Int, modifier: Modifier = Modifier) {
    val fraccion = if (total > 0) completos.toFloat() / total else 0f
    val animada by animateFloatAsState(targetValue = fraccion, animationSpec = tween(600), label = "progresoFicha")
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text("Ficha completa", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("$completos de $total datos", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { animada },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    }
}
