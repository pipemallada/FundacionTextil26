package cl.duoc.redtextil.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import cl.duoc.redtextil.ui.screens.admin.AdminFichasScreen
import cl.duoc.redtextil.ui.screens.admin.CorreosScreen
import cl.duoc.redtextil.ui.screens.admin.MetricasScreen
import cl.duoc.redtextil.ui.screens.directorio.ConsultaFormScreen
import cl.duoc.redtextil.ui.screens.directorio.DetalleEmprendimientoScreen
import cl.duoc.redtextil.ui.screens.directorio.DirectorioScreen
import cl.duoc.redtextil.ui.screens.directorio.MisConsultasScreen
import cl.duoc.redtextil.ui.screens.emprendedora.BandejaScreen
import cl.duoc.redtextil.ui.screens.emprendedora.CatalogoScreen
import cl.duoc.redtextil.ui.screens.emprendedora.FichaScreen
import cl.duoc.redtextil.ui.screens.emprendedora.PerfilScreen
import cl.duoc.redtextil.ui.screens.emprendedora.PublicacionFormScreen
import cl.duoc.redtextil.ui.screens.emprendedora.RegistroScreen
import cl.duoc.redtextil.ui.viewmodel.AppUiState

private const val DURACION = 320

@Composable
fun AppNavHost(
    navController: NavHostController,
    estadoApp: AppUiState,
    onSincronizar: () -> Unit,
    onIrAPestana: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Rutas.PERFIL,
        modifier = modifier,
        enterTransition = {
            fadeIn(tween(DURACION)) + slideInHorizontally(tween(DURACION)) { ancho -> ancho / 4 }
        },
        exitTransition = {
            fadeOut(tween(DURACION / 2)) + slideOutHorizontally(tween(DURACION)) { ancho -> -ancho / 4 }
        },
        popEnterTransition = {
            fadeIn(tween(DURACION)) + slideInHorizontally(tween(DURACION)) { ancho -> -ancho / 4 }
        },
        popExitTransition = {
            fadeOut(tween(DURACION / 2)) + slideOutHorizontally(tween(DURACION)) { ancho -> ancho / 4 }
        }
    ) {
        composable(Rutas.PERFIL) {
            PerfilScreen(
                enLinea = estadoApp.enLinea,
                pendientesTotales = estadoApp.pendientes,
                onSincronizar = onSincronizar,
                onCrearCuenta = { navController.navigate(Rutas.REGISTRO) },
                onIrAFicha = { onIrAPestana(Rutas.FICHA) },
                onIrACatalogo = { onIrAPestana(Rutas.CATALOGO) },
                onIrABandeja = { onIrAPestana(Rutas.BANDEJA) }
            )
        }
        composable(Rutas.REGISTRO) {
            RegistroScreen(onRegistrado = { navController.popBackStack() })
        }
        composable(Rutas.FICHA) {
            FichaScreen(onIrAPerfil = { onIrAPestana(Rutas.PERFIL) })
        }
        composable(Rutas.CATALOGO) {
            CatalogoScreen(
                onNueva = { navController.navigate(Rutas.publicacion(null)) },
                onEditar = { id -> navController.navigate(Rutas.publicacion(id)) },
                onIrAPerfil = { onIrAPestana(Rutas.PERFIL) }
            )
        }
        composable(
            route = Rutas.PUBLICACION,
            arguments = listOf(navArgument(Rutas.ARG_ID) {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) {
            PublicacionFormScreen(onListo = { navController.popBackStack() })
        }
        composable(Rutas.BANDEJA) {
            BandejaScreen(onIrAPerfil = { onIrAPestana(Rutas.PERFIL) })
        }

        composable(Rutas.DIRECTORIO) {
            DirectorioScreen(onAbrirFicha = { id -> navController.navigate(Rutas.detalle(id)) })
        }
        composable(
            route = Rutas.DETALLE,
            arguments = listOf(navArgument(Rutas.ARG_ID) { type = NavType.LongType })
        ) {
            DetalleEmprendimientoScreen(onConsultar = { id -> navController.navigate(Rutas.consulta(id)) })
        }
        composable(
            route = Rutas.CONSULTA,
            arguments = listOf(navArgument(Rutas.ARG_ID) { type = NavType.LongType })
        ) {
            ConsultaFormScreen(
                onVolver = { navController.popBackStack() },
                onVerMisConsultas = { onIrAPestana(Rutas.MIS_CONSULTAS) }
            )
        }
        composable(Rutas.MIS_CONSULTAS) {
            MisConsultasScreen()
        }

        composable(Rutas.ADMIN_FICHAS) {
            AdminFichasScreen()
        }
        composable(Rutas.ADMIN_METRICAS) {
            MetricasScreen()
        }
        composable(Rutas.ADMIN_CORREOS) {
            CorreosScreen()
        }
    }
}
