package cl.duoc.redtextil.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cl.duoc.redtextil.ui.components.AvisoPrivacidad
import cl.duoc.redtextil.ui.components.BannerConexion
import cl.duoc.redtextil.ui.components.LocalSnackbar
import cl.duoc.redtextil.ui.viewmodel.AppUiState
import cl.duoc.redtextil.ui.viewmodel.AppViewModel
import cl.duoc.redtextil.ui.viewmodel.AppViewModelProvider
import cl.duoc.redtextil.util.NotificationHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppPrincipal(
    destinoNotificacion: String?,
    onDestinoAtendido: () -> Unit,
    appViewModel: AppViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val navController = rememberNavController()
    val estado by appViewModel.uiState.collectAsStateWithLifecycle()
    val modoGuardado by appViewModel.modo.collectAsStateWithLifecycle()
    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route
    val modo = Rutas.modoDe(rutaActual) ?: modoGuardado
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    val permisoNotificaciones = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // Android 13 o superior pide permiso explicito para mostrar notificaciones
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        appViewModel.mensajes.collect { snackbar.showSnackbar(it) }
    }

    LaunchedEffect(modo) {
        if (modo != modoGuardado) appViewModel.cambiarModo(modo)
    }

    val irAPestana: (String) -> Unit = { ruta ->
        navController.navigate(ruta) {
            popUpTo(Rutas.modoDe(ruta)?.rutaInicio ?: ruta) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val cambiarModo: (ModoApp) -> Unit = { nuevo ->
        appViewModel.cambiarModo(nuevo)
        navController.navigate(nuevo.rutaInicio) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    LaunchedEffect(destinoNotificacion) {
        when (destinoNotificacion) {
            NotificationHelper.DESTINO_BANDEJA -> {
                if (modo != ModoApp.EMPRENDEDORA) cambiarModo(ModoApp.EMPRENDEDORA)
                irAPestana(Rutas.BANDEJA)
            }
            NotificationHelper.DESTINO_PERFIL -> {
                if (modo != ModoApp.EMPRENDEDORA) cambiarModo(ModoApp.EMPRENDEDORA)
                irAPestana(Rutas.PERFIL)
            }
        }
        if (destinoNotificacion != null) onDestinoAtendido()
    }

    CompositionLocalProvider(LocalSnackbar provides snackbar) {
        Scaffold(
            topBar = {
                BarraSuperior(
                    modo = modo,
                    estado = estado,
                    puedeVolver = rutaActual != null && rutaActual !in modo.rutasPrincipales,
                    onVolver = { navController.navigateUp() },
                    onCambiarModo = cambiarModo,
                    onForzarOffline = appViewModel::cambiarForzarOffline,
                    onServidorDemo = appViewModel::cambiarServidorDemo,
                    onSincronizar = { appViewModel.sincronizar() }
                )
            },
            bottomBar = {
                BarraInferior(
                    modo = modo,
                    rutaActual = rutaActual,
                    visible = rutaActual == null || rutaActual in modo.rutasPrincipales,
                    consultasPendientes = estado.consultasPendientes,
                    onNavegar = irAPestana
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
            ) {
                BannerConexion(
                    enLinea = estado.enLinea,
                    pendientes = estado.pendientes,
                    sincronizando = estado.sincronizando,
                    onSincronizar = { appViewModel.sincronizar() }
                )
                AppNavHost(
                    navController = navController,
                    estadoApp = estado,
                    onSincronizar = { appViewModel.sincronizar() },
                    onIrAPestana = irAPestana,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarraSuperior(
    modo: ModoApp,
    estado: AppUiState,
    puedeVolver: Boolean,
    onVolver: () -> Unit,
    onCambiarModo: (ModoApp) -> Unit,
    onForzarOffline: (Boolean) -> Unit,
    onServidorDemo: (Boolean) -> Unit,
    onSincronizar: () -> Unit
) {
    var menuModo by remember { mutableStateOf(false) }
    var menuAjustes by remember { mutableStateOf(false) }
    var verPrivacidad by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column {
                Text("Red Textil", style = MaterialTheme.typography.titleLarge, maxLines = 1)
                AnimatedContent(
                    targetState = modo,
                    transitionSpec = {
                        (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                    },
                    label = "subtituloModo"
                ) { actual ->
                    Text(
                        actual.titulo,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            if (puedeVolver) {
                IconButton(onClick = onVolver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
            }
        },
        actions = {
            Box {
                FilledTonalButton(
                    onClick = { menuModo = true },
                    contentPadding = PaddingValues(start = 12.dp, end = 6.dp)
                ) {
                    AnimatedContent(targetState = modo, label = "iconoModo") { actual ->
                        Icon(actual.icono, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(modo.corto, maxLines = 1)
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "Cambiar de perfil")
                }
                DropdownMenu(expanded = menuModo, onDismissRequest = { menuModo = false }) {
                    Text(
                        "Cambiar de perfil",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    ModoApp.entries.forEach { opcion ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(opcion.titulo, fontWeight = FontWeight.Bold)
                                    Text(opcion.descripcion, style = MaterialTheme.typography.bodySmall)
                                }
                            },
                            leadingIcon = { Icon(opcion.icono, contentDescription = null) },
                            trailingIcon = {
                                if (opcion == modo) Icon(Icons.Filled.Check, contentDescription = "Perfil actual")
                            },
                            onClick = {
                                menuModo = false
                                if (opcion != modo) onCambiarModo(opcion)
                            }
                        )
                    }
                }
            }
            Box {
                IconButton(onClick = { menuAjustes = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Ajustes de prueba")
                }
                DropdownMenu(expanded = menuAjustes, onDismissRequest = { menuAjustes = false }) {
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("Simular sin señal")
                                Text("Probar el modo zona rural", style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        leadingIcon = { Icon(Icons.Filled.CloudOff, contentDescription = null) },
                        trailingIcon = { Switch(checked = estado.forzarOffline, onCheckedChange = null) },
                        onClick = { onForzarOffline(!estado.forzarOffline) }
                    )
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("Servidor de demostración")
                                Text(
                                    if (estado.servidorDemo) "Sin backend Spring Boot" else "API en 10.0.2.2:8080",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        leadingIcon = { Icon(Icons.Filled.Dns, contentDescription = null) },
                        trailingIcon = { Switch(checked = estado.servidorDemo, onCheckedChange = null) },
                        onClick = { onServidorDemo(!estado.servidorDemo) }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Sincronizar ahora") },
                        leadingIcon = { Icon(Icons.Filled.Sync, contentDescription = null) },
                        enabled = estado.enLinea,
                        onClick = {
                            menuAjustes = false
                            onSincronizar()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Aviso de privacidad") },
                        leadingIcon = { Icon(Icons.Filled.PrivacyTip, contentDescription = null) },
                        onClick = {
                            menuAjustes = false
                            verPrivacidad = true
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )

    if (verPrivacidad) {
        AvisoPrivacidad(onCerrar = { verPrivacidad = false })
    }
}

@Composable
private fun BarraInferior(
    modo: ModoApp,
    rutaActual: String?,
    visible: Boolean,
    consultasPendientes: Int,
    onNavegar: (String) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        AnimatedContent(
            targetState = modo,
            transitionSpec = {
                (fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 2 }) togetherWith fadeOut(tween(150))
            },
            label = "barraInferior"
        ) { actual ->
            NavigationBar {
                actual.pestanas.forEach { pestana ->
                    NavigationBarItem(
                        selected = rutaActual == pestana.ruta,
                        onClick = { onNavegar(pestana.ruta) },
                        icon = {
                            if (pestana.ruta == Rutas.BANDEJA && consultasPendientes > 0) {
                                BadgedBox(badge = { Badge { Text("$consultasPendientes") } }) {
                                    Icon(pestana.icono, contentDescription = pestana.titulo)
                                }
                            } else {
                                Icon(pestana.icono, contentDescription = pestana.titulo)
                            }
                        },
                        label = { Text(pestana.titulo, maxLines = 1) }
                    )
                }
            }
        }
    }
}
