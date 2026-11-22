package cl.duoc.redtextil.ui.screens.emprendedora

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.redtextil.data.local.entity.PublicacionEntity
import cl.duoc.redtextil.domain.model.EstadoSync
import cl.duoc.redtextil.ui.components.BotonGrande
import cl.duoc.redtextil.ui.components.CargandoPantalla
import cl.duoc.redtextil.ui.components.EstadoVacio
import cl.duoc.redtextil.ui.components.EtiquetaPequena
import cl.duoc.redtextil.ui.components.ImagenCatalogo
import cl.duoc.redtextil.ui.components.MensajeSnackbar
import cl.duoc.redtextil.ui.components.TituloPantalla
import cl.duoc.redtextil.ui.theme.AmarilloAviso
import cl.duoc.redtextil.ui.theme.VerdeExito
import cl.duoc.redtextil.ui.viewmodel.AppViewModelProvider
import cl.duoc.redtextil.ui.viewmodel.CatalogoViewModel
import cl.duoc.redtextil.util.TextoUtil

@Composable
fun CatalogoScreen(
    onNueva: () -> Unit,
    onEditar: (Long) -> Unit,
    onIrAPerfil: () -> Unit,
    viewModel: CatalogoViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    var porEliminar by remember { mutableStateOf<PublicacionEntity?>(null) }
    MensajeSnackbar(estado.mensaje, viewModel::mensajeMostrado)

    when {
        estado.cargando -> CargandoPantalla()
        estado.sinSesion -> AvisoSinSesion(onIrAPerfil)
        estado.publicaciones.isEmpty() -> EstadoVacio(
            icono = Icons.Filled.PhotoLibrary,
            titulo = "Tu catálogo está vacío",
            texto = "Sube fotos de tus prendas, arreglos o talleres. Así los clientes ven tu trabajo sin necesidad de Instagram.",
            accion = { BotonGrande(texto = "Agregar mi primer trabajo", icono = Icons.Filled.AddAPhoto, onClick = onNueva) }
        )
        else -> Box(Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    TituloPantalla(
                        "Mi catálogo",
                        "${estado.publicaciones.size} trabajos de ${estado.nombreComercial}" +
                            if (estado.pendientesEnvio > 0) " · ${estado.pendientesEnvio} por enviar" else ""
                    )
                }
                items(estado.publicaciones, key = { it.id }) { publicacion ->
                    TarjetaPublicacion(
                        publicacion = publicacion,
                        onEditar = { onEditar(publicacion.id) },
                        onEliminar = { porEliminar = publicacion }
                    )
                }
            }
            ExtendedFloatingActionButton(
                onClick = onNueva,
                icon = { Icon(Icons.Filled.AddAPhoto, contentDescription = null) },
                text = { Text("Nuevo trabajo") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }

    porEliminar?.let { publicacion ->
        AlertDialog(
            onDismissRequest = { porEliminar = null },
            icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            title = { Text("¿Eliminar este trabajo?") },
            text = { Text("\"${publicacion.titulo}\" se quitará de tu catálogo y del directorio.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminar(publicacion)
                    porEliminar = null
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { porEliminar = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun TarjetaPublicacion(
    publicacion: PublicacionEntity,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    ElevatedCard(shape = RoundedCornerShape(18.dp)) {
        ImagenCatalogo(
            ruta = publicacion.fotoRuta,
            categoria = publicacion.categoria,
            descripcion = publicacion.titulo,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        )
        Column(Modifier.padding(10.dp)) {
            Text(
                publicacion.titulo,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                publicacion.categoria.etiqueta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                TextoUtil.formatearPesos(publicacion.precioReferencial),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (publicacion.estadoSync == EstadoSync.PENDIENTE_SINCRONIZAR) {
                EtiquetaPequena("Por enviar", AmarilloAviso)
            } else {
                EtiquetaPequena("Publicado", VerdeExito)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEditar) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar ${publicacion.titulo}")
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onEliminar) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Eliminar ${publicacion.titulo}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
