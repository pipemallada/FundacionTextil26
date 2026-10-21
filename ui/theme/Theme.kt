package cl.duoc.redtextil.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val EsquemaClaro = lightColorScheme(
    primary = Terracota,
    onPrimary = Crema,
    primaryContainer = TerracotaClaro,
    onPrimaryContainer = TerracotaOscuro,
    secondary = VerdeSalvia,
    onSecondary = Crema,
    secondaryContainer = VerdeSalviaClaro,
    onSecondaryContainer = VerdeSalviaOscuro,
    tertiary = VerdeBosque,
    onTertiary = Crema,
    tertiaryContainer = VerdeBosqueClaro,
    onTertiaryContainer = VerdeBosqueOscuro,
    background = Crema,
    onBackground = CafeTexto,
    surface = CremaSuperficie,
    onSurface = CafeTexto,
    surfaceVariant = CremaVariante,
    onSurfaceVariant = CafeSecundario,
    outline = Contorno
)

private val EsquemaOscuro = darkColorScheme(
    primary = TerracotaClaro,
    onPrimary = TerracotaOscuro,
    primaryContainer = Terracota,
    onPrimaryContainer = Crema,
    secondary = VerdeSalviaClaro,
    onSecondary = VerdeSalviaOscuro,
    secondaryContainer = VerdeSalvia,
    onSecondaryContainer = Crema,
    tertiary = VerdeBosqueClaro,
    onTertiary = VerdeBosqueOscuro,
    tertiaryContainer = VerdeBosque,
    onTertiaryContainer = Crema,
    background = NocheFondo,
    onBackground = NocheTexto,
    surface = NocheSuperficie,
    onSurface = NocheTexto,
    surfaceVariant = CafeSecundario,
    onSurfaceVariant = CremaVariante
)

private val Formas = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun RedTextilTheme(
    oscuro: Boolean = false,
    content: @Composable () -> Unit
) {
    // Fondo crema claro por defecto (alto contraste) y sin colores dinamicos, asi la paleta es igual en todos los telefonos
    MaterialTheme(
        colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
        typography = Tipografia,
        shapes = Formas,
        content = content
    )
}
