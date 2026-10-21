package cl.duoc.redtextil.ui.screens.directorio

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.redtextil.domain.model.EstadoConsulta
import cl.duoc.redtextil.ui.components.CargandoPantalla
import cl.duoc.redtextil.ui.components.EstadoVacio
import cl.duoc.redtextil.ui.components.EtiquetaPequena
import cl.duoc.redtextil.ui.components.TituloPantalla
import cl.duoc.redtextil.ui.theme.AmarilloAviso
import cl.duoc.redtextil.ui.theme.VerdeExito
import cl.duoc.redtextil.ui.viewmodel.AppViewModelProvider
import cl.duoc.redtextil.ui.viewmodel.MisConsultasViewModel
import cl.duoc.redtextil.util.FechaUtil

@Composable
fun MisConsultasScreen(
    viewModel: MisConsultasViewModel = viewModel(factory = AppViewModelProvider.Factory)
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
            TituloPantalla("Mis consultas", "Las consultas que enviaste desde este teléfono y sus respuestas")
        }
        if (estado.consultas.isEmpty()) {
            item {
                EstadoVacio(
                    icono = Icons.Filled.QuestionAnswer,
                    titulo = "Aún no envías consultas",
                    texto = "Busca un emprendimiento en el directorio y usa el botón \"Enviar consulta\"."
                )
            }
        }
        items(estado.consultas, key = { it.consulta.id }) { fila ->
            val consulta = fila.consulta
            val respondida = consulta.estado == EstadoConsulta.RESPONDIDA
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(fila.nombreComercial, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${consulta.servicio} · ${FechaUtil.fechaHora(consulta.fechaEnvio)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        EtiquetaPequena(consulta.estado.etiqueta, if (respondida) VerdeExito else AmarilloAviso)
                    }
                    Text(consulta.mensaje, style = MaterialTheme.typography.bodyMedium)
                    if (respondida && consulta.respuesta.isNotBlank()) {
                        Surface(
                            color = VerdeExito.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Respuesta de ${fila.nombreComercial}", fontWeight = FontWeight.Bold, color = VerdeExito)
                                Text(consulta.respuesta, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
