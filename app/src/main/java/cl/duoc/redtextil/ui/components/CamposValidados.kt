package cl.duoc.redtextil.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.duoc.redtextil.domain.model.EstadoCampo
import cl.duoc.redtextil.ui.theme.VerdeExito

private val FormaCampo = RoundedCornerShape(14.dp)

@Composable
fun IconoEstadoCampo(error: String?, valido: Boolean) {
    val estado = when {
        error != null -> EstadoCampo.ERROR
        valido -> EstadoCampo.VALIDO
        else -> EstadoCampo.NEUTRO
    }
    AnimatedContent(
        targetState = estado,
        transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.6f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.6f)) },
        label = "iconoEstadoCampo"
    ) { actual ->
        when (actual) {
            EstadoCampo.ERROR -> Icon(
                Icons.Filled.Error,
                contentDescription = "Dato con error",
                tint = MaterialTheme.colorScheme.error
            )
            EstadoCampo.VALIDO -> Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Dato correcto",
                tint = VerdeExito
            )
            EstadoCampo.NEUTRO -> Spacer(Modifier.size(24.dp))
        }
    }
}

@Composable
fun TextoAyudaError(error: String?, ayuda: String?) {
    Column {
        AnimatedVisibility(
            visible = error != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Text(
                text = error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (error == null && ayuda != null) {
            Text(text = ayuda, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    error: String? = null,
    valido: Boolean = false,
    ayuda: String? = null,
    placeholder: String? = null,
    teclado: KeyboardType = KeyboardType.Text,
    mayusculas: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    lineas: Int = 1,
    habilitado: Boolean = true
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        placeholder = if (placeholder != null) {
            { Text(placeholder) }
        } else {
            null
        },
        leadingIcon = { Icon(icono, contentDescription = null) },
        trailingIcon = { IconoEstadoCampo(error = error, valido = valido) },
        isError = error != null,
        supportingText = { TextoAyudaError(error = error, ayuda = ayuda) },
        singleLine = lineas == 1,
        minLines = lineas,
        maxLines = if (lineas == 1) 1 else lineas + 4,
        enabled = habilitado,
        keyboardOptions = KeyboardOptions(
            capitalization = mayusculas,
            keyboardType = teclado,
            imeAction = if (lineas == 1) ImeAction.Next else ImeAction.Default
        ),
        shape = FormaCampo,
        modifier = modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoSelector(
    valor: String,
    opciones: List<String>,
    onSeleccion: (String) -> Unit,
    etiqueta: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    error: String? = null,
    valido: Boolean = false,
    ayuda: String? = null,
    habilitado: Boolean = true,
    textoVacio: String = "Toca para elegir"
) {
    var abierto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = abierto && habilitado,
        onExpandedChange = { if (habilitado) abierto = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = valor,
            onValueChange = {},
            readOnly = true,
            enabled = habilitado,
            label = { Text(etiqueta) },
            placeholder = { Text(textoVacio) },
            leadingIcon = { Icon(icono, contentDescription = null) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconoEstadoCampo(error = error, valido = valido)
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto)
                }
            },
            isError = error != null,
            supportingText = { TextoAyudaError(error = error, ayuda = ayuda) },
            singleLine = true,
            shape = FormaCampo,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, habilitado)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = abierto && habilitado,
            onDismissRequest = { abierto = false }
        ) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion, style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        onSeleccion(opcion)
                        abierto = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
fun FilaError(mensaje: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 2.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(mensaje, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun CampoSwitch(
    titulo: String,
    marcado: Boolean,
    onCambio: (Boolean) -> Unit,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    descripcion: String? = null,
    error: String? = null
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(FormaCampo)
                .toggleable(value = marcado, role = Role.Switch, onValueChange = onCambio)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.bodyLarge)
                if (descripcion != null) {
                    Text(
                        descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = marcado, onCheckedChange = null)
        }
        AnimatedVisibility(
            visible = error != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            FilaError(error.orEmpty())
        }
    }
}

@Composable
fun CampoCheckbox(
    texto: String,
    marcado: Boolean,
    onCambio: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(FormaCampo)
                .toggleable(value = marcado, role = Role.Checkbox, onValueChange = onCambio)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = marcado, onCheckedChange = null)
            Spacer(Modifier.width(10.dp))
            Text(texto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            IconoEstadoCampo(error = error, valido = marcado)
        }
        AnimatedVisibility(
            visible = error != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            FilaError(error.orEmpty())
        }
    }
}
