package cl.duoc.redtextil.ui.screens.emprendedora

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.runtime.Composable
import cl.duoc.redtextil.ui.components.BotonGrande
import cl.duoc.redtextil.ui.components.EstadoVacio

@Composable
fun AvisoSinSesion(onIrAPerfil: () -> Unit) {
    EstadoVacio(
        icono = Icons.Filled.AccountCircle,
        titulo = "Primero entra a tu cuenta",
        texto = "Ve a \"Mi perfil\", elige tu cuenta o crea una nueva para usar esta sección.",
        accion = {
            BotonGrande(texto = "Ir a Mi perfil", icono = Icons.AutoMirrored.Filled.Login, onClick = onIrAPerfil)
        }
    )
}
