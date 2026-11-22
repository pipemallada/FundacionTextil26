package cl.duoc.redtextil.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.duoc.redtextil.data.repository.IndicadoresEconomicos
import cl.duoc.redtextil.ui.viewmodel.IndicadoresUiState

// Tarjeta con los datos de la API externa mindicador.cl (separados de los datos de la Fundacion)
@Composable
fun TarjetaIndicadores(estado: IndicadoresUiState, onReintentar: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(shape = RoundedCornerShape(18.dp), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CurrencyExchange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Indicadores del día", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Fuente externa: mindicador.cl",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            AnimatedContent(targetState = estado, label = "indicadores") { actual ->
                when (actual) {
                    IndicadoresUiState.Cargando -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Consultando mindicador.cl…", style = MaterialTheme.typography.bodyMedium)
                    }
                    is IndicadoresUiState.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            actual.mensaje,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onReintentar) { Text("Reintentar") }
                    }
                    is IndicadoresUiState.Listo -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val datos = actual.indicadores
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            ValorIndicador("Dólar", datos.dolar, Modifier.weight(1f))
                            datos.euro?.let { ValorIndicador("Euro", it, Modifier.weight(1f)) }
                            datos.uf?.let { ValorIndicador("UF", it, Modifier.weight(1f)) }
                        }
                        Text(
                            "Valores al ${datos.fecha}. Con el dólar del día verás los precios del catálogo en US$.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ValorIndicador(nombre: String, valor: Double, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(nombre, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(IndicadoresEconomicos.formatearPesos(valor), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}
