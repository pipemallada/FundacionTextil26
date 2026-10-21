package cl.duoc.redtextil.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DryCleaning
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Texture
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import cl.duoc.redtextil.domain.model.CategoriaTrabajo
import cl.duoc.redtextil.domain.model.EstadoFicha
import cl.duoc.redtextil.domain.model.Rubro
import cl.duoc.redtextil.ui.theme.AmarilloAviso
import cl.duoc.redtextil.ui.theme.AzulInfo
import cl.duoc.redtextil.ui.theme.GrisBaja
import cl.duoc.redtextil.ui.theme.VerdeExito

fun Rubro.icono(): ImageVector = when (this) {
    Rubro.SUPRARRECICLAJE -> Icons.Filled.Recycling
    Rubro.REPARACION -> Icons.Filled.ContentCut
    Rubro.SEGUNDA_MANO -> Icons.Filled.Checkroom
    Rubro.ARRIENDO -> Icons.Filled.DryCleaning
    Rubro.TALLERES -> Icons.Filled.School
    Rubro.ACCESORIOS -> Icons.Filled.Diamond
    Rubro.TEJIDO -> Icons.Filled.Texture
}

fun CategoriaTrabajo?.icono(): ImageVector = when (this) {
    CategoriaTrabajo.PRENDA_INTERVENIDA -> Icons.Filled.Brush
    CategoriaTrabajo.ARREGLO -> Icons.Filled.ContentCut
    CategoriaTrabajo.SEGUNDA_MANO -> Icons.Filled.Checkroom
    CategoriaTrabajo.ARRIENDO -> Icons.Filled.DryCleaning
    CategoriaTrabajo.ACCESORIO -> Icons.Filled.Diamond
    CategoriaTrabajo.TALLER -> Icons.Filled.School
    CategoriaTrabajo.TEJIDO -> Icons.Filled.Texture
    null -> Icons.Filled.Checkroom
}

fun EstadoFicha.icono(): ImageVector = when (this) {
    EstadoFicha.ACTIVA -> Icons.Filled.CheckCircle
    EstadoFicha.PENDIENTE -> Icons.Filled.HourglassTop
    EstadoFicha.OBSERVADA -> Icons.Filled.Warning
    EstadoFicha.BAJA -> Icons.Filled.Block
}

fun EstadoFicha.color(): Color = when (this) {
    EstadoFicha.ACTIVA -> VerdeExito
    EstadoFicha.PENDIENTE -> AzulInfo
    EstadoFicha.OBSERVADA -> AmarilloAviso
    EstadoFicha.BAJA -> GrisBaja
}
