package cl.duoc.redtextil.ui.screens.directorio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.redtextil.data.local.entity.EmprendimientoEntity
import cl.duoc.redtextil.data.local.entity.PublicacionEntity
import cl.duoc.redtextil.ui.components.BotonGrande
import cl.duoc.redtextil.ui.components.CargandoPantalla
import cl.duoc.redtextil.ui.components.ChipsRubros
import cl.duoc.redtextil.ui.components.EstadoVacio
import cl.duoc.redtextil.ui.components.FilaDato
import cl.duoc.redtextil.ui.components.ImagenCatalogo
import cl.duoc.redtextil.ui.components.LocalSnackbar
import cl.duoc.redtextil.ui.viewmodel.AppViewModelProvider
import cl.duoc.redtextil.ui.viewmodel.DetalleUiState
import cl.duoc.redtextil.ui.viewmodel.DetalleViewModel
import cl.duoc.redtextil.ui.viewmodel.EventoDetalle
import cl.duoc.redtextil.util.EnlacesContacto
import cl.duoc.redtextil.util.TextoUtil

private val VerdeWhatsapp = Color(0xFF1E7E45)

@Composable
fun DetalleEmprendimientoScreen(
    onConsultar: (Long) -> Unit,
    viewModel: DetalleViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    var fotoAbierta by remember { mutableStateOf<PublicacionEntity?>(null) }

    LaunchedEffect(Unit) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                is EventoDetalle.AbrirEnlace -> if (!EnlacesContacto.abrir(context, evento.url)) viewModel.enlaceNoAbrio()
                is EventoDetalle.Mensaje -> snackbar.showSnackbar(evento.texto)
            }
        }
    }

    val emprendimiento = estado.emprendimiento
    when {
        estado.cargando -> CargandoPantalla()
        emprendimiento == null -> EstadoVacio(
            icono = Icons.Filled.SearchOff,
            titulo = "Ficha no disponible",
            texto = "Este emprendimiento ya no está publicado en el directorio."
        )
        else -> ContenidoDetalle(
            estado = estado,
            emprendimiento = emprendimiento,
            onWhatsapp = viewModel::abrirWhatsapp,
            onInstagram = viewModel::abrirInstagram,
            onSitio = viewModel::abrirSitio,
            onConsultar = { onConsultar(emprendimiento.id) },
            onAbrirFoto = { fotoAbierta = it }
        )
    }

    fotoAbierta?.let { publicacion ->
        AlertDialog(
            onDismissRequest = { fotoAbierta = null },
            title = { Text(publicacion.titulo) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ImagenCatalogo(
                        ruta = publicacion.fotoRuta,
                        categoria = publicacion.categoria,
                        descripcion = publicacion.titulo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Text(
                        "${publicacion.categoria.etiqueta} · ${TextoUtil.formatearPesos(publicacion.precioReferencial)}",
                        fontWeight = FontWeight.Bold
                    )
                    Text("Técnica: ${publicacion.tecnica}", style = MaterialTheme.typography.bodyMedium)
                    Text(publicacion.descripcion, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = { TextButton(onClick = { fotoAbierta = null }) { Text("Cerrar") } }
        )
    }
}

@Composable
private fun ContenidoDetalle(
    estado: DetalleUiState,
    emprendimiento: EmprendimientoEntity,
    onWhatsapp: () -> Unit,
    onInstagram: () -> Unit,
    onSitio: () -> Unit,
    onConsultar: () -> Unit,
    onAbrirFoto: (PublicacionEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ImagenCatalogo(
                ruta = estado.publicaciones.firstOrNull { it.fotoRuta != null }?.fotoRuta,
                categoria = estado.publicaciones.firstOrNull()?.categoria,
                descripcion = "Portada de ${emprendimiento.nombreComercial}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            )
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(emprendimiento.nombreComercial, style = MaterialTheme.typography.headlineSmall)
                Text("por ${emprendimiento.nombreEmprendedora}", style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("${emprendimiento.comuna}, región de ${emprendimiento.region}", style = MaterialTheme.typography.bodyMedium)
                }
                ChipsRubros(emprendimiento.listaRubros())
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonGrande(
                    texto = if (estado.urlWhatsapp != null) "Escribir por WhatsApp" else "WhatsApp no disponible",
                    icono = Icons.AutoMirrored.Filled.Chat,
                    onClick = onWhatsapp,
                    habilitado = estado.urlWhatsapp != null,
                    colorFondo = VerdeWhatsapp,
                    colorTexto = Color.White
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onInstagram,
                        enabled = estado.urlInstagram != null,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Instagram")
                    }
                    if (estado.urlSitio != null) {
                        OutlinedButton(
                            onClick = onSitio,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                        ) {
                            Icon(Icons.Filled.Language, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Sitio web")
                        }
                    }
                }
                Button(
                    onClick = onConsultar,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Enviar consulta por formulario")
                }
            }
        }
        item {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Sobre el emprendimiento", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        emprendimiento.descripcion.ifBlank { "Sin descripción." },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(6.dp))
                    if (emprendimiento.tecnicas.isNotBlank()) {
                        FilaDato(Icons.Filled.Brush, "Técnicas", emprendimiento.tecnicas)
                    }
                    estado.aniosTrabajando?.let { anios ->
                        FilaDato(
                            Icons.Filled.CalendarMonth,
                            "Trayectoria",
                            if (anios <= 0) "Comenzó este año" else "$anios años trabajando (desde ${emprendimiento.anioInicio})"
                        )
                    }
                    if (emprendimiento.lugarTrabajo.isNotBlank()) {
                        FilaDato(Icons.Filled.HomeWork, "Lugar de trabajo", emprendimiento.lugarTrabajo)
                    }
                    emprendimiento.cantidadPersonas?.let {
                        FilaDato(Icons.Filled.Groups, "Equipo", if (it == 1) "Trabaja sola" else "$it personas")
                    }
                    if (emprendimiento.formalizado) {
                        FilaDato(Icons.Filled.Business, "Formalización", emprendimiento.tipoFormalizacion.ifBlank { "Con inicio de actividades" })
                    }
                }
            }
        }
        item {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Atención y servicios", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    if (emprendimiento.horarioAtencion.isNotBlank()) {
                        FilaDato(Icons.Filled.Schedule, "Horario", emprendimiento.horarioAtencion)
                    }
                    FilaDato(
                        Icons.Filled.Place,
                        "Atención presencial",
                        if (emprendimiento.atencionPresencial) emprendimiento.direccionTaller.ifBlank { "Sí" } else "No, solo a distancia"
                    )
                    FilaDato(
                        Icons.Filled.LocalShipping,
                        "Envíos",
                        if (emprendimiento.realizaEnvios) "Hace envíos y despachos" else "No hace envíos"
                    )
                    emprendimiento.cuposTaller?.let {
                        FilaDato(
                            Icons.Filled.EventSeat,
                            "Talleres",
                            "$it cupos por taller · modalidad ${emprendimiento.modalidadTaller.lowercase()}"
                        )
                    }
                    if (emprendimiento.recibeDonaciones && emprendimiento.tiposDonacion.isNotBlank()) {
                        FilaDato(Icons.Filled.VolunteerActivism, "Recibe donaciones", emprendimiento.tiposDonacion)
                    }
                    if (emprendimiento.tallasArriendo.isNotBlank()) {
                        FilaDato(Icons.Filled.Straighten, "Tallas para arriendo", emprendimiento.tallasArriendo)
                    }
                    if (emprendimiento.modalidadTaller.isNotBlank() && emprendimiento.cuposTaller == null) {
                        FilaDato(Icons.Filled.School, "Modalidad de talleres", emprendimiento.modalidadTaller)
                    }
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text("Catálogo (${estado.publicaciones.size} trabajos)", style = MaterialTheme.typography.titleMedium)
            }
        }
        item {
            if (estado.publicaciones.isEmpty()) {
                Text(
                    "Esta emprendedora aún no sube fotos de sus trabajos.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(estado.publicaciones, key = { it.id }) { publicacion ->
                        ElevatedCard(
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .width(170.dp)
                                .clickable { onAbrirFoto(publicacion) }
                        ) {
                            ImagenCatalogo(
                                ruta = publicacion.fotoRuta,
                                categoria = publicacion.categoria,
                                descripcion = publicacion.titulo,
                                modifier = Modifier.size(170.dp)
                            )
                            Column(Modifier.padding(10.dp)) {
                                Text(
                                    publicacion.titulo,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    TextoUtil.formatearPesos(publicacion.precioReferencial),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
