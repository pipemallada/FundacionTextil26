package cl.duoc.redtextil.ui.screens.emprendedora

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.redtextil.domain.model.CampoFicha
import cl.duoc.redtextil.domain.model.EstadoFicha
import cl.duoc.redtextil.domain.model.EstadoSync
import cl.duoc.redtextil.domain.model.OpcionesFicha
import cl.duoc.redtextil.domain.model.SeccionFicha
import cl.duoc.redtextil.ui.components.BotonGrande
import cl.duoc.redtextil.ui.components.CampoCheckbox
import cl.duoc.redtextil.ui.components.CampoSelector
import cl.duoc.redtextil.ui.components.CampoSwitch
import cl.duoc.redtextil.ui.components.CampoTexto
import cl.duoc.redtextil.ui.components.CargandoPantalla
import cl.duoc.redtextil.ui.components.ChipEstado
import cl.duoc.redtextil.ui.components.FilaError
import cl.duoc.redtextil.ui.components.MensajeSnackbar
import cl.duoc.redtextil.ui.components.ProgresoFicha
import cl.duoc.redtextil.ui.components.SeccionExpandible
import cl.duoc.redtextil.ui.components.SelectorRubros
import cl.duoc.redtextil.ui.theme.AmarilloAviso
import cl.duoc.redtextil.ui.theme.VerdeExito
import cl.duoc.redtextil.ui.viewmodel.AppViewModelProvider
import cl.duoc.redtextil.ui.viewmodel.FichaUiState
import cl.duoc.redtextil.ui.viewmodel.FichaViewModel
import cl.duoc.redtextil.util.ChileGeoData

@Composable
fun FichaScreen(
    onIrAPerfil: () -> Unit,
    viewModel: FichaViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    MensajeSnackbar(estado.mensaje, viewModel::mensajeMostrado)

    when {
        estado.cargando -> CargandoPantalla()
        estado.sinSesion -> AvisoSinSesion(onIrAPerfil)
        else -> FormularioFicha(estado, viewModel)
    }
}

@Composable
private fun FormularioFicha(estado: FichaUiState, vm: FichaViewModel) {
    val d = estado.datos
    fun errorDe(campo: CampoFicha) = estado.errores[campo]
    fun okDe(campo: CampoFicha) = campo in estado.validos
    fun visible(campo: CampoFicha) = campo in estado.visibles

    val entrada = TransicionCampos

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EncabezadoFicha(estado)

            SeccionExpandible(
                titulo = SeccionFicha.PERSONALES.titulo,
                descripcion = SeccionFicha.PERSONALES.descripcion,
                icono = Icons.Filled.Person,
                abierta = SeccionFicha.PERSONALES in estado.seccionesAbiertas,
                onAlternar = { vm.alternarSeccion(SeccionFicha.PERSONALES) },
                errores = estado.erroresPorSeccion[SeccionFicha.PERSONALES] ?: 0,
                completos = estado.completosPorSeccion[SeccionFicha.PERSONALES] ?: 0,
                total = estado.totalPorSeccion[SeccionFicha.PERSONALES] ?: 0
            ) {
                CampoTexto(
                    valor = d.nombreEmprendedora,
                    onCambio = { vm.onTexto(CampoFicha.NOMBRE_EMPRENDEDORA, it) },
                    etiqueta = CampoFicha.NOMBRE_EMPRENDEDORA.etiqueta,
                    icono = Icons.Filled.Person,
                    error = errorDe(CampoFicha.NOMBRE_EMPRENDEDORA),
                    valido = okDe(CampoFicha.NOMBRE_EMPRENDEDORA),
                    mayusculas = KeyboardCapitalization.Words
                )
                CampoTexto(
                    valor = d.rut,
                    onCambio = { vm.onTexto(CampoFicha.RUT, it) },
                    etiqueta = CampoFicha.RUT.etiqueta,
                    icono = Icons.Filled.Badge,
                    error = errorDe(CampoFicha.RUT),
                    valido = okDe(CampoFicha.RUT),
                    mayusculas = KeyboardCapitalization.Characters,
                    ayuda = "Ej: 12.345.678-5 (no se muestra a los clientes)"
                )
                CampoTexto(
                    valor = d.fechaNacimiento,
                    onCambio = { vm.onTexto(CampoFicha.FECHA_NACIMIENTO, it) },
                    etiqueta = CampoFicha.FECHA_NACIMIENTO.etiqueta,
                    icono = Icons.Filled.Cake,
                    error = errorDe(CampoFicha.FECHA_NACIMIENTO),
                    valido = okDe(CampoFicha.FECHA_NACIMIENTO),
                    placeholder = "dd/mm/aaaa",
                    ayuda = "Día/mes/año. Ej: 21/03/1985"
                )
                CampoSelector(
                    valor = d.nacionalidad,
                    opciones = OpcionesFicha.nacionalidades,
                    onSeleccion = { vm.onTexto(CampoFicha.NACIONALIDAD, it) },
                    etiqueta = CampoFicha.NACIONALIDAD.etiqueta,
                    icono = Icons.Filled.Flag,
                    error = errorDe(CampoFicha.NACIONALIDAD),
                    valido = okDe(CampoFicha.NACIONALIDAD)
                )
                CampoTexto(
                    valor = d.correo,
                    onCambio = { vm.onTexto(CampoFicha.CORREO, it) },
                    etiqueta = CampoFicha.CORREO.etiqueta,
                    icono = Icons.Filled.Email,
                    error = errorDe(CampoFicha.CORREO),
                    valido = okDe(CampoFicha.CORREO),
                    teclado = KeyboardType.Email,
                    mayusculas = KeyboardCapitalization.None
                )
                CampoTexto(
                    valor = d.telefono,
                    onCambio = { vm.onTexto(CampoFicha.TELEFONO, it) },
                    etiqueta = CampoFicha.TELEFONO.etiqueta,
                    icono = Icons.Filled.Phone,
                    error = errorDe(CampoFicha.TELEFONO),
                    valido = okDe(CampoFicha.TELEFONO),
                    teclado = KeyboardType.Phone,
                    ayuda = "+569 y 8 números"
                )
                CampoSelector(
                    valor = d.jefaHogar,
                    opciones = OpcionesFicha.jefaHogar,
                    onSeleccion = { vm.onTexto(CampoFicha.JEFA_HOGAR, it) },
                    etiqueta = CampoFicha.JEFA_HOGAR.etiqueta,
                    icono = Icons.Filled.FamilyRestroom,
                    error = errorDe(CampoFicha.JEFA_HOGAR),
                    valido = okDe(CampoFicha.JEFA_HOGAR),
                    ayuda = "Nos ayuda a postular a fondos de apoyo"
                )
            }

            SeccionExpandible(
                titulo = SeccionFicha.TALLER.titulo,
                descripcion = SeccionFicha.TALLER.descripcion,
                icono = Icons.Filled.Storefront,
                abierta = SeccionFicha.TALLER in estado.seccionesAbiertas,
                onAlternar = { vm.alternarSeccion(SeccionFicha.TALLER) },
                errores = estado.erroresPorSeccion[SeccionFicha.TALLER] ?: 0,
                completos = estado.completosPorSeccion[SeccionFicha.TALLER] ?: 0,
                total = estado.totalPorSeccion[SeccionFicha.TALLER] ?: 0
            ) {
                CampoTexto(
                    valor = d.nombreComercial,
                    onCambio = { vm.onTexto(CampoFicha.NOMBRE_COMERCIAL, it) },
                    etiqueta = CampoFicha.NOMBRE_COMERCIAL.etiqueta,
                    icono = Icons.Filled.Storefront,
                    error = errorDe(CampoFicha.NOMBRE_COMERCIAL),
                    valido = okDe(CampoFicha.NOMBRE_COMERCIAL),
                    mayusculas = KeyboardCapitalization.Words
                )
                CampoTexto(
                    valor = d.descripcion,
                    onCambio = { vm.onTexto(CampoFicha.DESCRIPCION, it) },
                    etiqueta = CampoFicha.DESCRIPCION.etiqueta,
                    icono = Icons.Filled.Description,
                    error = errorDe(CampoFicha.DESCRIPCION),
                    valido = okDe(CampoFicha.DESCRIPCION),
                    lineas = 3,
                    ayuda = "Cuenta qué haces y con qué materiales (${d.descripcion.trim().length}/400)"
                )
                CampoTexto(
                    valor = d.anioInicio,
                    onCambio = { vm.onTexto(CampoFicha.ANIO_INICIO, it) },
                    etiqueta = CampoFicha.ANIO_INICIO.etiqueta,
                    icono = Icons.Filled.CalendarMonth,
                    error = errorDe(CampoFicha.ANIO_INICIO),
                    valido = okDe(CampoFicha.ANIO_INICIO),
                    teclado = KeyboardType.Number,
                    placeholder = "Ej: 2019"
                )
                CampoSelector(
                    valor = d.lugarTrabajo,
                    opciones = OpcionesFicha.lugaresTrabajo,
                    onSeleccion = { vm.onTexto(CampoFicha.LUGAR_TRABAJO, it) },
                    etiqueta = CampoFicha.LUGAR_TRABAJO.etiqueta,
                    icono = Icons.Filled.HomeWork,
                    error = errorDe(CampoFicha.LUGAR_TRABAJO),
                    valido = okDe(CampoFicha.LUGAR_TRABAJO)
                )
                CampoTexto(
                    valor = d.cantidadPersonas,
                    onCambio = { vm.onTexto(CampoFicha.CANTIDAD_PERSONAS, it) },
                    etiqueta = CampoFicha.CANTIDAD_PERSONAS.etiqueta,
                    icono = Icons.Filled.Groups,
                    error = errorDe(CampoFicha.CANTIDAD_PERSONAS),
                    valido = okDe(CampoFicha.CANTIDAD_PERSONAS),
                    teclado = KeyboardType.Number,
                    ayuda = "Contándote a ti"
                )
                CampoSwitch(
                    titulo = CampoFicha.FORMALIZADO.etiqueta,
                    descripcion = "Si emites boletas o facturas en el SII",
                    marcado = d.formalizado,
                    onCambio = { vm.onSwitch(CampoFicha.FORMALIZADO, it) },
                    icono = Icons.Filled.Business
                )
                AnimatedVisibility(visible = visible(CampoFicha.RUT_EMPRESA), enter = entrada.entrar, exit = entrada.salir) {
                    Column {
                        CampoTexto(
                            valor = d.rutEmpresa,
                            onCambio = { vm.onTexto(CampoFicha.RUT_EMPRESA, it) },
                            etiqueta = CampoFicha.RUT_EMPRESA.etiqueta,
                            icono = Icons.Filled.Badge,
                            error = errorDe(CampoFicha.RUT_EMPRESA),
                            valido = okDe(CampoFicha.RUT_EMPRESA),
                            mayusculas = KeyboardCapitalization.Characters,
                            ayuda = "Si eres persona natural, es tu mismo RUT"
                        )
                        CampoSelector(
                            valor = d.tipoFormalizacion,
                            opciones = OpcionesFicha.tiposFormalizacion,
                            onSeleccion = { vm.onTexto(CampoFicha.TIPO_FORMALIZACION, it) },
                            etiqueta = CampoFicha.TIPO_FORMALIZACION.etiqueta,
                            icono = Icons.Filled.Business,
                            error = errorDe(CampoFicha.TIPO_FORMALIZACION),
                            valido = okDe(CampoFicha.TIPO_FORMALIZACION)
                        )
                    }
                }
            }

            SeccionExpandible(
                titulo = SeccionFicha.UBICACION.titulo,
                descripcion = SeccionFicha.UBICACION.descripcion,
                icono = Icons.Filled.Map,
                abierta = SeccionFicha.UBICACION in estado.seccionesAbiertas,
                onAlternar = { vm.alternarSeccion(SeccionFicha.UBICACION) },
                errores = estado.erroresPorSeccion[SeccionFicha.UBICACION] ?: 0,
                completos = estado.completosPorSeccion[SeccionFicha.UBICACION] ?: 0,
                total = estado.totalPorSeccion[SeccionFicha.UBICACION] ?: 0
            ) {
                CampoSelector(
                    valor = d.region,
                    opciones = ChileGeoData.nombresRegiones,
                    onSeleccion = vm::onRegion,
                    etiqueta = CampoFicha.REGION.etiqueta,
                    icono = Icons.Filled.Map,
                    error = errorDe(CampoFicha.REGION),
                    valido = okDe(CampoFicha.REGION),
                    ayuda = "Al cambiar la región se borra la comuna para que elijas una de esa región"
                )
                CampoSelector(
                    valor = d.comuna,
                    opciones = estado.comunas,
                    onSeleccion = { vm.onTexto(CampoFicha.COMUNA, it) },
                    etiqueta = CampoFicha.COMUNA.etiqueta,
                    icono = Icons.Filled.Place,
                    error = errorDe(CampoFicha.COMUNA),
                    valido = okDe(CampoFicha.COMUNA),
                    habilitado = d.region.isNotBlank(),
                    textoVacio = if (d.region.isBlank()) "Primero elige la región" else "Toca para elegir",
                    ayuda = if (d.region.isBlank()) "Primero elige la región" else "Solo comunas de ${d.region}"
                )
                CampoSwitch(
                    titulo = CampoFicha.ATENCION_PRESENCIAL.etiqueta,
                    descripcion = "Los clientes pueden ir a tu taller o puesto",
                    marcado = d.atencionPresencial,
                    onCambio = { vm.onSwitch(CampoFicha.ATENCION_PRESENCIAL, it) },
                    icono = Icons.Filled.Home
                )
                AnimatedVisibility(visible = visible(CampoFicha.DIRECCION_TALLER), enter = entrada.entrar, exit = entrada.salir) {
                    CampoTexto(
                        valor = d.direccionTaller,
                        onCambio = { vm.onTexto(CampoFicha.DIRECCION_TALLER, it) },
                        etiqueta = CampoFicha.DIRECCION_TALLER.etiqueta,
                        icono = Icons.Filled.Place,
                        error = errorDe(CampoFicha.DIRECCION_TALLER),
                        valido = okDe(CampoFicha.DIRECCION_TALLER),
                        ayuda = "Calle y número. Ej: Pasaje Las Gaviotas 245"
                    )
                }
                CampoSwitch(
                    titulo = CampoFicha.REALIZA_ENVIOS.etiqueta,
                    descripcion = "Envías por courier o haces entregas",
                    marcado = d.realizaEnvios,
                    onCambio = { vm.onSwitch(CampoFicha.REALIZA_ENVIOS, it) },
                    icono = Icons.Filled.LocalShipping,
                    error = errorDe(CampoFicha.REALIZA_ENVIOS)
                )
            }

            SeccionExpandible(
                titulo = SeccionFicha.SERVICIOS.titulo,
                descripcion = SeccionFicha.SERVICIOS.descripcion,
                icono = Icons.Filled.Category,
                abierta = SeccionFicha.SERVICIOS in estado.seccionesAbiertas,
                onAlternar = { vm.alternarSeccion(SeccionFicha.SERVICIOS) },
                errores = estado.erroresPorSeccion[SeccionFicha.SERVICIOS] ?: 0,
                completos = estado.completosPorSeccion[SeccionFicha.SERVICIOS] ?: 0,
                total = estado.totalPorSeccion[SeccionFicha.SERVICIOS] ?: 0
            ) {
                Text(
                    "Elige 1 o 2 rubros (${d.rubros.size}/2 elegidos)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                SelectorRubros(
                    seleccionados = d.rubros,
                    onAlternar = vm::onRubro,
                    habilitado = { it !in estado.rubrosBloqueados }
                )
                AnimatedVisibility(
                    visible = errorDe(CampoFicha.RUBROS) != null,
                    enter = entrada.entrar,
                    exit = entrada.salir
                ) {
                    FilaError(errorDe(CampoFicha.RUBROS).orEmpty())
                }
                Spacer(Modifier.height(4.dp))
                CampoTexto(
                    valor = d.tecnicas,
                    onCambio = { vm.onTexto(CampoFicha.TECNICAS, it) },
                    etiqueta = CampoFicha.TECNICAS.etiqueta,
                    icono = Icons.Filled.Brush,
                    error = errorDe(CampoFicha.TECNICAS),
                    valido = okDe(CampoFicha.TECNICAS),
                    ayuda = "Ej: patchwork, crochet, teñido natural"
                )
                AnimatedVisibility(visible = visible(CampoFicha.RECIBE_DONACIONES), enter = entrada.entrar, exit = entrada.salir) {
                    CampoSwitch(
                        titulo = CampoFicha.RECIBE_DONACIONES.etiqueta,
                        descripcion = "Para reutilizar o reparar",
                        marcado = d.recibeDonaciones,
                        onCambio = { vm.onSwitch(CampoFicha.RECIBE_DONACIONES, it) },
                        icono = Icons.Filled.VolunteerActivism
                    )
                }
                AnimatedVisibility(visible = visible(CampoFicha.TIPOS_DONACION), enter = entrada.entrar, exit = entrada.salir) {
                    CampoTexto(
                        valor = d.tiposDonacion,
                        onCambio = { vm.onTexto(CampoFicha.TIPOS_DONACION, it) },
                        etiqueta = CampoFicha.TIPOS_DONACION.etiqueta,
                        icono = Icons.Filled.VolunteerActivism,
                        error = errorDe(CampoFicha.TIPOS_DONACION),
                        valido = okDe(CampoFicha.TIPOS_DONACION),
                        ayuda = "Ej: jeans, retazos de algodón, lana"
                    )
                }
                AnimatedVisibility(visible = visible(CampoFicha.CUPOS_TALLER), enter = entrada.entrar, exit = entrada.salir) {
                    Column {
                        CampoTexto(
                            valor = d.cuposTaller,
                            onCambio = { vm.onTexto(CampoFicha.CUPOS_TALLER, it) },
                            etiqueta = CampoFicha.CUPOS_TALLER.etiqueta,
                            icono = Icons.Filled.EventSeat,
                            error = errorDe(CampoFicha.CUPOS_TALLER),
                            valido = okDe(CampoFicha.CUPOS_TALLER),
                            teclado = KeyboardType.Number,
                            ayuda = "Máximo 40 personas por taller"
                        )
                        CampoSelector(
                            valor = d.modalidadTaller,
                            opciones = OpcionesFicha.modalidadesTaller,
                            onSeleccion = { vm.onTexto(CampoFicha.MODALIDAD_TALLER, it) },
                            etiqueta = CampoFicha.MODALIDAD_TALLER.etiqueta,
                            icono = Icons.Filled.School,
                            error = errorDe(CampoFicha.MODALIDAD_TALLER),
                            valido = okDe(CampoFicha.MODALIDAD_TALLER)
                        )
                    }
                }
                AnimatedVisibility(visible = visible(CampoFicha.TALLAS_ARRIENDO), enter = entrada.entrar, exit = entrada.salir) {
                    CampoTexto(
                        valor = d.tallasArriendo,
                        onCambio = { vm.onTexto(CampoFicha.TALLAS_ARRIENDO, it) },
                        etiqueta = CampoFicha.TALLAS_ARRIENDO.etiqueta,
                        icono = Icons.Filled.Straighten,
                        error = errorDe(CampoFicha.TALLAS_ARRIENDO),
                        valido = okDe(CampoFicha.TALLAS_ARRIENDO),
                        ayuda = "Ej: S, M, L y tallas grandes"
                    )
                }
            }

            SeccionExpandible(
                titulo = SeccionFicha.CONTACTO.titulo,
                descripcion = SeccionFicha.CONTACTO.descripcion,
                icono = Icons.Filled.ContactPhone,
                abierta = SeccionFicha.CONTACTO in estado.seccionesAbiertas,
                onAlternar = { vm.alternarSeccion(SeccionFicha.CONTACTO) },
                errores = estado.erroresPorSeccion[SeccionFicha.CONTACTO] ?: 0,
                completos = estado.completosPorSeccion[SeccionFicha.CONTACTO] ?: 0,
                total = estado.totalPorSeccion[SeccionFicha.CONTACTO] ?: 0
            ) {
                CampoTexto(
                    valor = d.whatsapp,
                    onCambio = { vm.onTexto(CampoFicha.WHATSAPP, it) },
                    etiqueta = CampoFicha.WHATSAPP.etiqueta,
                    icono = Icons.AutoMirrored.Filled.Chat,
                    error = errorDe(CampoFicha.WHATSAPP),
                    valido = okDe(CampoFicha.WHATSAPP),
                    teclado = KeyboardType.Phone,
                    ayuda = "Los clientes te escribirán aquí. Formato +569XXXXXXXX"
                )
                CampoTexto(
                    valor = d.instagram,
                    onCambio = { vm.onTexto(CampoFicha.INSTAGRAM, it) },
                    etiqueta = CampoFicha.INSTAGRAM.etiqueta + " (opcional)",
                    icono = Icons.Filled.PhotoCamera,
                    error = errorDe(CampoFicha.INSTAGRAM),
                    valido = okDe(CampoFicha.INSTAGRAM),
                    mayusculas = KeyboardCapitalization.None,
                    ayuda = "@tuusuario o el enlace de tu perfil"
                )
                CampoTexto(
                    valor = d.sitioWeb,
                    onCambio = { vm.onTexto(CampoFicha.SITIO_WEB, it) },
                    etiqueta = CampoFicha.SITIO_WEB.etiqueta + " (opcional)",
                    icono = Icons.Filled.Language,
                    error = errorDe(CampoFicha.SITIO_WEB),
                    valido = okDe(CampoFicha.SITIO_WEB),
                    teclado = KeyboardType.Uri,
                    mayusculas = KeyboardCapitalization.None
                )
                CampoTexto(
                    valor = d.horarioAtencion,
                    onCambio = { vm.onTexto(CampoFicha.HORARIO_ATENCION, it) },
                    etiqueta = CampoFicha.HORARIO_ATENCION.etiqueta,
                    icono = Icons.Filled.Schedule,
                    error = errorDe(CampoFicha.HORARIO_ATENCION),
                    valido = okDe(CampoFicha.HORARIO_ATENCION),
                    ayuda = "Ej: Lunes a viernes de 10:00 a 18:00"
                )
                CampoCheckbox(
                    texto = "Autorizo a la Fundación a publicar mi nombre comercial, comuna, rubros, fotos y canales de contacto en el directorio.",
                    marcado = d.aceptaPublicacion,
                    onCambio = { vm.onSwitch(CampoFicha.ACEPTA_PUBLICACION, it) },
                    error = errorDe(CampoFicha.ACEPTA_PUBLICACION)
                )
            }

            BotonGrande(
                texto = "Guardar mi ficha",
                icono = Icons.Filled.Save,
                onClick = vm::guardar,
                cargando = estado.guardando
            )
            Text(
                "Puedes guardar sin señal: los cambios quedan en el teléfono y se envían solos al volver la conexión.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

// Misma animacion para todas las preguntas condicionales que aparecen y desaparecen
private object TransicionCampos {
    val entrar = expandVertically() + fadeIn()
    val salir = shrinkVertically() + fadeOut()
}

@Composable
private fun EncabezadoFicha(estado: FichaUiState) {
    ElevatedCard(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Ficha de mi emprendimiento", style = MaterialTheme.typography.titleLarge)
            Text(
                "Son 32 datos en 5 secciones. Las preguntas cambian según lo que respondas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ProgresoFicha(completos = estado.camposCompletos, total = estado.camposAplicables)
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChipEstado(estado.estadoFicha)
                Spacer(Modifier.width(8.dp))
                val enviada = estado.estadoSync == EstadoSync.SINCRONIZADO
                Icon(
                    if (enviada) Icons.Filled.CloudDone else Icons.Filled.CloudUpload,
                    contentDescription = null,
                    tint = if (enviada) VerdeExito else AmarilloAviso
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (enviada) "Enviada al servidor" else "Guardada en el teléfono, falta enviar",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (estado.estadoFicha == EstadoFicha.OBSERVADA && estado.observacion.isNotBlank()) {
                Surface(
                    color = AmarilloAviso.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp)) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = AmarilloAviso)
                        Spacer(Modifier.width(8.dp))
                        Text(estado.observacion, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
