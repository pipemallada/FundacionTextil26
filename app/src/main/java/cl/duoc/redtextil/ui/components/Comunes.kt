package cl.duoc.redtextil.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

val LocalSnackbar = staticCompositionLocalOf { SnackbarHostState() }

@Composable
fun MensajeSnackbar(mensaje: String?, onMostrado: () -> Unit) {
    val snackbar = LocalSnackbar.current
    val alTerminar by rememberUpdatedState(onMostrado)
    LaunchedEffect(mensaje) {
        if (mensaje != null) {
            snackbar.showSnackbar(mensaje)
            alTerminar()
        }
    }
}

@Composable
fun BotonGrande(
    texto: String,
    icono: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    cargando: Boolean = false,
    colorFondo: Color = MaterialTheme.colorScheme.primary,
    colorTexto: Color = MaterialTheme.colorScheme.onPrimary
) {
    Button(
        onClick = onClick,
        enabled = habilitado && !cargando,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colorFondo, contentColor = colorTexto),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
    ) {
        if (cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = colorTexto
            )
        } else {
            Icon(icono, contentDescription = null)
        }
        Spacer(Modifier.width(10.dp))
        Text(texto, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun AvisoPrivacidad(onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
        title = { Text("Aviso de privacidad") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "La Fundación Textil Circular usa tus datos solo para publicar tu emprendimiento en el " +
                        "Directorio Textil Circular y para contactarte por tu inscripción.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Tu RUT, fecha de nacimiento y datos personales no se muestran a los clientes. " +
                        "Solo se publican el nombre comercial, la comuna, los rubros, el catálogo y los canales de contacto que tú autorices.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Puedes pedir que se corrijan o eliminen tus datos en cualquier momento, según la Ley N.º 19.628 " +
                        "sobre protección de la vida privada y la Ley N.º 21.719 de protección de datos personales.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Todos los datos de prueba de esta app son ficticios.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Entendido") } }
    )
}

@Composable
fun FilaDato(icono: ImageVector, titulo: String, valor: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
