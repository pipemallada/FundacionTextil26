package cl.duoc.redtextil.ui.screens.directorio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.redtextil.domain.model.Rubro
import cl.duoc.redtextil.ui.components.CampoSelector
import cl.duoc.redtextil.ui.components.EstadoVacio
import cl.duoc.redtextil.ui.components.TarjetaEmprendimiento
import cl.duoc.redtextil.ui.components.TituloPantalla
import cl.duoc.redtextil.ui.components.icono
import cl.duoc.redtextil.ui.viewmodel.AppViewModelProvider
import cl.duoc.redtextil.ui.viewmodel.DirectorioViewModel
import cl.duoc.redtextil.util.ChileGeoData

@Composable
fun DirectorioScreen(
    onAbrirFicha: (Long) -> Unit,
    viewModel: DirectorioViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val filtros = estado.filtros

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            TituloPantalla(
                "Directorio Textil Circular",
                "Encuentra emprendedoras que reparan, transforman y reutilizan ropa en todo Chile"
            )
        }
        item {
            OutlinedTextField(
                value = filtros.texto,
                onValueChange = viewModel::onTexto,
                label = { Text("¿Qué buscas?") },
                placeholder = { Text("Ej: bolsos, arreglo de jeans, lana") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (filtros.texto.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onTexto("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Borrar búsqueda")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            CampoSelector(
                valor = filtros.region.ifEmpty { DirectorioViewModel.TODAS_LAS_REGIONES },
                opciones = listOf(DirectorioViewModel.TODAS_LAS_REGIONES) + ChileGeoData.nombresRegiones,
                onSeleccion = viewModel::onRegion,
                etiqueta = "Región",
                icono = Icons.Filled.Map
            )
        }
        item {
            CampoSelector(
                valor = filtros.comuna.ifEmpty { DirectorioViewModel.TODAS_LAS_COMUNAS },
                opciones = listOf(DirectorioViewModel.TODAS_LAS_COMUNAS) + estado.comunas,
                onSeleccion = viewModel::onComuna,
                etiqueta = "Comuna",
                icono = Icons.Filled.Place,
                habilitado = filtros.region.isNotEmpty(),
                ayuda = if (filtros.region.isEmpty()) "Elige una región para filtrar por comuna" else null
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filtros.rubro == null,
                        onClick = { viewModel.onRubro(null) },
                        label = { Text("Todos los rubros") }
                    )
                }
                items(Rubro.entries) { rubro ->
                    FilterChip(
                        selected = filtros.rubro == rubro,
                        onClick = { viewModel.onRubro(rubro) },
                        label = { Text(rubro.etiqueta) },
                        leadingIcon = {
                            Icon(rubro.icono(), contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                        }
                    )
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (estado.cargando) "Buscando…" else "${estado.resultados.size} emprendimientos encontrados",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (filtros.tieneCriterios) {
                    TextButton(onClick = viewModel::limpiarFiltros) {
                        Icon(Icons.Filled.FilterAltOff, contentDescription = null)
                        Text(" Limpiar")
                    }
                }
            }
        }
        if (!estado.cargando && estado.resultados.isEmpty()) {
            item {
                EstadoVacio(
                    icono = Icons.Filled.SearchOff,
                    titulo = "No encontramos emprendimientos",
                    texto = "Prueba con otra comuna, otro rubro o borra la búsqueda."
                )
            }
        }
        items(estado.resultados, key = { it.id }) { emprendimiento ->
            TarjetaEmprendimiento(
                emprendimiento = emprendimiento,
                portada = estado.portadas[emprendimiento.id],
                onVerFicha = { onAbrirFicha(emprendimiento.id) }
            )
        }
    }
}
