package cl.duoc.redtextil.ui.screens.emprendedora

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.redtextil.domain.model.CampoPublicacion
import cl.duoc.redtextil.domain.model.CategoriaTrabajo
import cl.duoc.redtextil.ui.components.BotonGrande
import cl.duoc.redtextil.ui.components.CampoSelector
import cl.duoc.redtextil.ui.components.CampoTexto
import cl.duoc.redtextil.ui.components.CargandoPantalla
import cl.duoc.redtextil.ui.components.FilaError
import cl.duoc.redtextil.ui.components.ImagenCatalogo
import cl.duoc.redtextil.ui.components.MensajeSnackbar
import cl.duoc.redtextil.ui.components.TituloPantalla
import cl.duoc.redtextil.ui.components.icono
import cl.duoc.redtextil.ui.theme.VerdeExito
import cl.duoc.redtextil.ui.viewmodel.AppViewModelProvider
import cl.duoc.redtextil.ui.viewmodel.PublicacionFormUiState
import cl.duoc.redtextil.ui.viewmodel.PublicacionFormViewModel
import cl.duoc.redtextil.util.ImageCompressor

@Composable
fun PublicacionFormScreen(
    onListo: () -> Unit,
    viewModel: PublicacionFormViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    MensajeSnackbar(estado.mensaje, viewModel::mensajeMostrado)

    LaunchedEffect(estado.guardado) {
        if (estado.guardado) onListo()
    }

    // Se guarda como texto para no perder la foto si Android recrea la pantalla mientras la camara esta abierta
    var uriCamara by rememberSaveable { mutableStateOf<String?>(null) }

    val tomarFoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        val uri = uriCamara
        if (exito && uri != null) viewModel.procesarFoto(Uri.parse(uri), desdeCamara = true)
    }

    val abrirCamara = {
        val uri = viewModel.crearUriCamara()
        uriCamara = uri.toString()
        tomarFoto.launch(uri)
    }

    val pedirPermisoCamara = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) abrirCamara() else viewModel.permisoCamaraDenegado()
    }

    val elegirDeGaleria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.procesarFoto(uri, desdeCamara = false)
    }

    if (estado.cargando) {
        CargandoPantalla()
        return
    }

    FormularioPublicacion(
        estado = estado,
        onTomarFoto = {
            val tienePermiso = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            if (tienePermiso) abrirCamara() else pedirPermisoCamara.launch(Manifest.permission.CAMERA)
        },
        onGaleria = {
            elegirDeGaleria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        vm = viewModel
    )
}

@Composable
private fun FormularioPublicacion(
    estado: PublicacionFormUiState,
    onTomarFoto: () -> Unit,
    onGaleria: () -> Unit,
    vm: PublicacionFormViewModel
) {
    val form = estado.form
    fun errorDe(campo: CampoPublicacion) = estado.errores[campo]
    fun okDe(campo: CampoPublicacion) = campo in estado.validos

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TituloPantalla(
            if (estado.esEdicion) "Editar trabajo" else "Nuevo trabajo",
            "Una buena foto con luz natural ayuda a que te contacten"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(targetState = form.fotoRuta, label = "fotoPublicacion") { ruta ->
                if (ruta != null) {
                    ImagenCatalogo(
                        ruta = ruta,
                        categoria = form.categoria,
                        descripcion = "Foto del trabajo",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(
                            Icons.Filled.AddAPhoto,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Agrega una foto de tu trabajo", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (estado.procesandoFoto) {
                Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), modifier = Modifier.fillMaxSize()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Optimizando la foto…")
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = errorDe(CampoPublicacion.FOTO) != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            FilaError(errorDe(CampoPublicacion.FOTO).orEmpty())
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            FilledTonalButton(
                onClick = onTomarFoto,
                enabled = !estado.procesandoFoto,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
            ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Tomar foto")
            }
            FilledTonalButton(
                onClick = onGaleria,
                enabled = !estado.procesandoFoto,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
            ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Galería")
            }
        }

        AnimatedVisibility(
            visible = estado.kbOriginal > 0,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                color = VerdeExito.copy(alpha = 0.10f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Compress, contentDescription = null, tint = VerdeExito)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Foto optimizada para ahorrar datos",
                            style = MaterialTheme.typography.titleMedium,
                            color = VerdeExito
                        )
                        Text(
                            "Original: ${estado.kbOriginal} KB  →  Optimizada: ${estado.kbOptimizado} KB (${estado.porcentajeAhorro}% menos)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Máximo ${ImageCompressor.LADO_MAXIMO} px, JPEG al ${ImageCompressor.CALIDAD_JPEG}%" +
                                if (estado.dimensiones.isNotBlank()) " · ${estado.dimensiones}" else "",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        CampoTexto(
            valor = form.titulo,
            onCambio = vm::onTitulo,
            etiqueta = "Título",
            icono = Icons.Filled.Title,
            error = errorDe(CampoPublicacion.TITULO),
            valido = okDe(CampoPublicacion.TITULO),
            ayuda = "Ej: Bolso de jeans reciclado"
        )
        CampoSelector(
            valor = form.categoria?.etiqueta.orEmpty(),
            opciones = CategoriaTrabajo.entries.map { it.etiqueta },
            onSeleccion = vm::onCategoria,
            etiqueta = "Categoría",
            icono = form.categoria.icono(),
            error = errorDe(CampoPublicacion.CATEGORIA),
            valido = okDe(CampoPublicacion.CATEGORIA)
        )
        CampoTexto(
            valor = form.tecnica,
            onCambio = vm::onTecnica,
            etiqueta = "Técnica textil",
            icono = Icons.Filled.Brush,
            error = errorDe(CampoPublicacion.TECNICA),
            valido = okDe(CampoPublicacion.TECNICA),
            ayuda = "Ej: patchwork, crochet, bordado"
        )
        CampoTexto(
            valor = form.precio,
            onCambio = vm::onPrecio,
            etiqueta = "Precio de referencia (CLP)",
            icono = Icons.Filled.Sell,
            error = errorDe(CampoPublicacion.PRECIO),
            valido = okDe(CampoPublicacion.PRECIO),
            teclado = KeyboardType.Number,
            placeholder = "Ej: 15000",
            ayuda = "Escribe 0 si el precio es a convenir"
        )
        CampoTexto(
            valor = form.descripcion,
            onCambio = vm::onDescripcion,
            etiqueta = "Descripción",
            icono = Icons.Filled.Description,
            error = errorDe(CampoPublicacion.DESCRIPCION),
            valido = okDe(CampoPublicacion.DESCRIPCION),
            lineas = 3,
            ayuda = "Materiales, tallas o tiempo de entrega (${form.descripcion.trim().length}/300)"
        )

        Spacer(Modifier.height(8.dp))
        BotonGrande(
            texto = if (estado.esEdicion) "Guardar cambios" else "Publicar en mi catálogo",
            icono = Icons.Filled.Save,
            onClick = vm::guardar,
            cargando = estado.guardando,
            habilitado = !estado.procesandoFoto
        )
        Spacer(Modifier.height(24.dp))
    }
}
