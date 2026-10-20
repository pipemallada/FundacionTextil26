# Red Textil Circular — App móvil para la Fundación Textil Circular

Proyecto de la asignatura **DSY1105 · Desarrollo de Aplicaciones Móviles** (Duoc UC, sede Viña del Mar).
Resuelve el caso **"Gestión de Perfil, Catálogo Gráfico y Contactos para la Red Textil Circular"** de la Fundación Textil Circular (Región de Valparaíso).

| | |
|---|---|
| **Integrantes** | _Nombre Apellido 1_ · _Nombre Apellido 2_ |
| **Sección / Equipo** | _completar_ |
| **Docente** | _completar_ |
| **Paquete** | `cl.duoc.redtextil` |
| **Plataforma** | Android 7.0 (API 24) o superior · `targetSdk 35` |

---

## 1. Descripción del caso

La Fundación administra el **Directorio Textil Circular**, que conecta a más de 980 emprendedoras textiles (suprarreciclaje, reparación, segunda mano, arriendo, talleres, accesorios y tejido) de las 16 regiones de Chile con clientes interesados. Hoy funciona con Google Forms + Sheets + Apps Script dentro de un WordPress, y eso genera:

- **Fichas invisibles**: al cambiar de región en el formulario se crean varias columnas de comuna y la ficha deja de aparecer en las búsquedas.
- **Enlaces rotos**: no hay validación, muchos Instagram y WhatsApp no funcionan.
- **Sin fotos**: la emprendedora depende de Instagram para mostrar su trabajo.
- **Sin trazabilidad y gestión manual**: no se miden contactos, y las altas, bajas y correos de bienvenida se hacen a mano.

**Solución:** una app Android offline-first donde la emprendedora se registra, completa su ficha validada (32 campos con saltos condicionales), sube fotos comprimidas desde la cámara o galería y responde consultas. La administradora gestiona altas/bajas, ve métricas por región y rubro, supervisa los correos automáticos e integra el dataset histórico. El cliente busca y contacta desde un directorio público integrado en la misma app.

Como la evaluación se hace en Android Studio, la barra superior tiene un **selector de perfil** para cambiar en vivo entre:

- **Modo Emprendedora** (Mi perfil · Mi ficha · Catálogo · Consultas)
- **Modo Directorio Cliente** (Buscar · Mis consultas)
- **Modo Administradora** (Fichas · Métricas · Correos)

---

## 2. Tecnologías y versiones

| Herramienta | Versión |
|---|---|
| Android Gradle Plugin | 8.7.3 |
| Gradle (wrapper) | 8.11.1 |
| Kotlin / Compose Compiler | 2.0.21 |
| KSP | 2.0.21-1.0.28 |
| Compose BOM (Material 3) | 2024.12.01 |
| Navigation Compose | 2.8.5 |
| Lifecycle (ViewModel, runtime-compose) | 2.8.7 |
| Room (runtime, ktx, compiler con KSP) | 2.6.1 |
| Retrofit + Gson / OkHttp logging | 2.11.0 / 4.12.0 |
| Kotlin Coroutines | 1.9.0 |
| Coil Compose | 2.7.0 |
| JUnit | 4.13.2 |

Todas las versiones están centralizadas en `gradle/libs.versions.toml`.

---

## 3. Arquitectura (MVVM)

```
cl.duoc.redtextil
├── MainActivity.kt / RedTextilApp.kt / AppContainer.kt   (inyección de dependencias manual)
├── data
│   ├── local
│   │   ├── entity      EmprendimientoEntity, PublicacionEntity, ConsultaEntity, InteraccionEntity, CorreoEntity
│   │   ├── dao         DAOs con Flow (lecturas) y suspend (escrituras)
│   │   ├── database    AppDatabase, DatosSemilla, ImagenesSemilla
│   │   └── PreferenciasLocales.kt   (sesión, modo sin señal, servidor demo)
│   ├── remote          RedTextilApiService (Retrofit), ApiClient, ServidorDemoApi, DTOs y mapeos
│   └── repository      Emprendimiento, Catalogo, Consulta, Metricas, Correo, Historico, Sesion, Sincronizacion
├── domain
│   ├── model           FichaDatos (32 campos), CampoFicha, Rubro, estados, formularios, plantillas de correo
│   └── validation      FichaValidator, CatalogoValidator, ConsultaValidator, RegistroValidator,
│                       ReglasComunes (RUT, +569, correo, Instagram, URL), DepuradorHistorico, FechaSimple
├── ui
│   ├── viewmodel       13 ViewModels con StateFlow inmutable + AppViewModelProvider (factory)
│   ├── screens         emprendedora/, directorio/, admin/
│   ├── components      campos validados, secciones expandibles, banner offline, tarjetas, barras
│   ├── navigation      Rutas, ModoApp, AppNavHost, AppPrincipal (Scaffold + TopAppBar + NavigationBar)
│   └── theme           Paleta terracota / verde salvia / verde bosque / crema
└── util                ChileGeoData (16 regiones, 346 comunas), ImageCompressor, NotificationHelper,
                        NetworkMonitor, EnlacesContacto, CsvLector, TextoUtil, FechaUtil
```

**Flujo de datos:** `Screen (Compose)` → evento → `ViewModel` → valida con `domain/validation` → `Repository` → **Room** (siempre primero) → si hay red intenta **Retrofit** con `runCatching` → la UI observa `Flow`/`StateFlow` y se actualiza sola.

- Las pantallas **no tienen reglas de validación**: solo muestran `errores` y `validos` que vienen en el `UiState`.
- Los validadores son objetos Kotlin puros (sin Android), por eso tienen pruebas unitarias en `app/src/test`.

---

## 4. Cumplimiento de requisitos funcionales

| RF | Requisito | Dónde está implementado |
|---|---|---|
| RF01 | Registro y gestión autónoma de perfil | `RegistroScreen` + `RegistroViewModel` + `RegistroValidator`; selector de cuentas y panel en `PerfilScreen` (estado de cuenta, pausar/reactivar ficha, cerrar sesión). Sesión local en `PreferenciasLocales`. |
| RF02 | Ficha del emprendimiento (1 a 2 de 7 rubros) | `FichaScreen` + `FichaViewModel`; `Rubro` (7 categorías) y tope en `FichaValidator.puedeAgregarRubro`. |
| RF03 | Validación y 32 campos con saltos condicionales | `CampoFicha` (32 campos) en 5 secciones desplegables (`SeccionExpandible`). Región → reinicia comuna y carga solo sus comunas (`ChileGeoData`). Preguntas condicionales por formalización, atención presencial y rubro (donaciones, cupos de taller, tallas de arriendo). Cada campo con `leadingIcon`, `trailingIcon` dinámico (check verde / error rojo), `isError` y `supportingText`. |
| RF04 | Catálogo fotográfico (CRUD) | `CatalogoScreen`, `PublicacionFormScreen`, `CatalogoRepository` (crear, listar, editar y eliminar con confirmación). |
| RF05 | Cámara, galería y compresión | `TakePicture` + `FileProvider` (`AndroidManifest.xml`, `res/xml/file_paths.xml`), permiso de cámara en tiempo de ejecución, `PickVisualMedia`. `ImageCompressor`: lado máximo 1080 px, JPEG 75 % en `filesDir/catalogo`, corrige rotación EXIF y muestra KB original vs optimizado. |
| RF06 | Offline y sincronización diferida | Todo se guarda en Room con `EstadoSync.PENDIENTE_SINCRONIZAR`. `BannerConexion` animado "Modo Offline / Zona Rural". Botón "Sincronizar con servidor" y sincronización automática al volver la red (`AppViewModel` + `NetworkMonitor` + `SincronizacionRepository`). |
| RF07 | Bandeja de consultas | `BandejaScreen` + `BandejaViewModel`: filtros, cambio de estado PENDIENTE/RESPONDIDA, respuesta validada (`ConsultaValidator.validarRespuesta`) y botón a WhatsApp. |
| RF08 | Notificaciones locales | `NotificationHelper` (canales, `NotificationManager`, permiso `POST_NOTIFICATIONS` en Android 13+). Se disparan al recibir una consulta, al registrarse, al cambiar el estado de la ficha y al sincronizar. Tocar la notificación abre la bandeja. |
| RF09 | Altas, bajas y consistencia | `AdminFichasScreen`: filtros por ACTIVA/PENDIENTE/OBSERVADA/BAJA, búsqueda, "solo con problemas", revisión de consistencia (`FichaValidator.revisarConsistencia`) y acciones Aprobar / Observar (con nota validada) / Dar de baja. |
| RF10 | Trazabilidad y métricas | `InteraccionEntity` registra búsquedas, consultas y clics en WhatsApp/Instagram. `MetricasScreen`: contadores, interacciones efectivas y barras comparativas por las 16 regiones y por los 7 rubros. |
| RF11 | Correos automáticos | `CorreoRepository` + `PlantillasCorreo`: bienvenida y confirmación al registrarse, aviso al aprobar u observar. `CorreosScreen`: estados `ENVIADO_OK`, `EN_COLA_OFFLINE`, `FALLIDO`, reenvío individual y masivo. |
| RF12 | Dataset histórico (982) | Base pre-poblada con 20 fichas de distintas ciudades (`DatosSemilla`). Botón "Sincronizar Dataset Histórico (982 emprendimientos)" en Administradora: intenta la API y si no, usa `assets/dataset_historico_982.csv`, depura (`DepuradorHistorico`) e integra por lotes sin re-registro. |
| RF13 | Directorio con filtros | `DirectorioScreen`: búsqueda por texto + filtros combinados Región, Comuna y Rubro (consulta en Room), tarjetas expandibles, ficha ampliada y galería (`DetalleEmprendimientoScreen`). |
| RF14 | Contacto y consultas | WhatsApp / Instagram por `Intent` seguro que primero registra el clic. `ConsultaFormScreen` con formulario validado (nombre, correo, +569, servicio, mensaje): guarda en Room, suma métrica y dispara la notificación. |

### Estructura de la ficha (32 campos)

| Sección | Campos |
|---|---|
| 1. Datos personales (7) | Nombre completo · RUT · Fecha de nacimiento (18+) · Nacionalidad · Correo · Teléfono +569 · Jefa de hogar |
| 2. Taller y formalización (8) | Nombre comercial · Descripción · Año de inicio · Lugar de trabajo · Personas · Inicio de actividades → *RUT de la actividad* · *Tipo de formalización* |
| 3. Ubicación (5) | Región (16) → Comuna (se reinicia) · Atención presencial → *Dirección* · Envíos |
| 4. Rubros y servicios (7) | Rubros (1 a 2) · Técnicas · *Recibe donaciones* → *Qué donaciones* (upcycling/segunda mano) · *Cupos* y *Modalidad* (talleres) · *Tallas* (arriendo) |
| 5. Contacto y redes (5) | WhatsApp +569 · Instagram (@usuario o URL) · Sitio web · Horario · Autorización de publicación |

*En cursiva: preguntas condicionales que solo aparecen (y solo se validan) cuando corresponde.*

---

## 5. Requisitos no funcionales

| RNF | Requisito | Cómo se cumple |
|---|---|---|
| RNF01 | Compatibilidad gama media/baja | `minSdk 24`, `targetSdk 35`. Imágenes decodificadas con `inSampleSize` y comprimidas; listas `Lazy`; inserción del dataset por lotes de 50. |
| RNF02 | MVVM y modularidad | Paquetes `data / domain / ui / util` separados (ver sección 3). ViewModels con `StateFlow` inmutable, repositorios y DAOs. |
| RNF03 | Persistencia y tolerancia a fallos | Room guarda fichas, catálogo, consultas, métricas y correos. Nada depende de la red para funcionar. |
| RNF04 | Integración con backend | `RedTextilApiService` (Retrofit) con endpoints de fichas, catálogo (incluye subida multipart de la foto), consultas, métricas, correos e histórico. Llamadas con `runCatching`: si no hay servidor, los datos quedan pendientes sin cerrar la app. |
| RNF05 | Usabilidad y accesibilidad | Material 3 con paleta terracota, verde salvia, verde bosque y fondo crema de alto contraste; letra un poco más grande; botones de 56 dp con ícono y texto; ayudas en lenguaje simple; `TopAppBar` + `NavigationBar`. |
| RNF06 | Privacidad (Ley 19.628 y 21.719) | Checkbox validado de consentimiento en el registro y autorización de publicación en la ficha. Aviso de privacidad accesible desde el menú. Datos semilla y dataset 100 % ficticios (teléfonos `+5690000…`, correos `@example.com`). `allowBackup=false`. |
| RNF07 | Configuración y APK release | `signingConfigs.release` leyendo `keystore.properties` (fuera del repo), `minify` + `shrinkResources`, reglas en `proguard-rules.pro`. Ver sección 7. |
| RNF08 | Validación centralizada | Toda regla (`Regex`, largos, obligatoriedad, tope de rubros, comuna/región, edad) vive en `domain/validation` y se invoca desde los ViewModels. Las pantallas solo pintan el estado. Pruebas en `app/src/test`. |
| RNF09 | Animaciones con propósito | `fadeIn + slideInHorizontally` en el `NavHost`; `AnimatedVisibility` (`expandVertically + fadeIn`) en errores, secciones, preguntas condicionales y banner offline; `animateContentSize()` en tarjetas y secciones; `AnimatedContent` al cambiar de perfil, en el estado de la ficha, en el ícono de validación y en las métricas; barras animadas con `animateFloatAsState`. |

---

## 6. Cómo ejecutar

1. Abrir la carpeta del proyecto en **Android Studio** (Ladybug 2024.2 o superior) y esperar la sincronización de Gradle.
2. Si Android Studio lo pide, instalar **Android SDK Platform 35**.
3. Ejecutar en un emulador o teléfono con Android 7.0 o superior (botón ▶ Run).

Por consola:

```bash
./gradlew assembleDebug        # APK de depuración en app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # pruebas unitarias de los validadores
```

### Guía rápida para probar todos los requisitos

1. **Modo Emprendedora → Mi perfil**: elegir una cuenta demo (ej. *Renace Textil*) o **Crear mi cuenta nueva** (llegan dos correos automáticos y una notificación).
2. **Mi ficha**: abrir las 5 secciones, cambiar la región y ver cómo se borra la comuna; marcar o desmarcar rubros para ver las preguntas condicionales; probar un RUT o un Instagram inválido.
3. **Catálogo → Nuevo trabajo**: tomar una foto o elegir de la galería y ver el tamaño original vs optimizado.
4. Menú ⋮ → **Simular sin señal**: aparece el banner "Modo Offline / Zona Rural"; crear o editar algo. Al desactivarlo se sincroniza solo y llega la notificación de sincronización.
5. **Modo Directorio Cliente**: buscar, filtrar por región/comuna/rubro, abrir una ficha, tocar WhatsApp/Instagram y enviar una consulta (llega la notificación a la emprendedora).
6. **Modo Emprendedora → Consultas**: responder la consulta o abrir WhatsApp.
7. **Modo Administradora**: aprobar/observar/dar de baja fichas, **Sincronizar Dataset Histórico (982 emprendimientos)**, revisar **Métricas** y **Correos** (reenviar los que fallaron).

---

## 7. APK firmado (release)

1. Crear la llave (una sola vez):

   ```bash
   mkdir keystore
   keytool -genkeypair -v -keystore keystore/redtextil-release.jks -alias redtextil \
           -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Copiar `keystore.properties.example` como `keystore.properties` y completar las claves. Este archivo y el `.jks` están en `.gitignore`.
3. Generar el APK: `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`.
   También se puede hacer con **Build → Generate Signed App Bundle / APK** en Android Studio.

Sin `keystore.properties` el build release igual compila, pero queda sin firmar.

---

## 8. Backend Spring Boot (API REST)

URL base: `http://10.0.2.2:8080/api/` (localhost del computador visto desde el emulador). Se cambia en `data/remote/ApiClient.kt`; para un teléfono físico usar la IP del computador y agregarla en `res/xml/network_security_config.xml`.

| Método | Endpoint | Uso |
|---|---|---|
| GET | `/fichas?region=` | Listar fichas |
| POST / PUT | `/fichas` · `/fichas/{id}` | Crear / actualizar ficha |
| POST / PUT / DELETE | `/catalogo` · `/catalogo/{id}` | Publicaciones del catálogo |
| POST (multipart) | `/catalogo/{id}/foto` | Subir la foto ya comprimida |
| POST / PUT | `/consultas` · `/consultas/{id}` | Consultas y respuestas |
| POST | `/metricas/interacciones` | Lote de interacciones (RF10) |
| POST | `/correos/enviar` | Envío de correos automáticos (RF11) |
| GET | `/historico/emprendimientos` | Dataset histórico (RF12) |

Mientras no esté levantado el backend, la app trae un **servidor de demostración** (`ServidorDemoApi`) que responde igual que la API. Se activa o desactiva desde el menú ⋮ de la barra superior. Con el servidor demo desactivado y sin backend, la app sigue funcionando y los cambios quedan pendientes.

---

## 9. Dataset histórico

`app/src/main/assets/dataset_historico_982.csv` simula la exportación de la planilla de Google Sheets: 982 filas ficticias con una columna de comuna por región (`comuna_VA`, `comuna_RM`, …), teléfonos con distintos formatos, regiones escritas de varias formas, rubros escritos a mano e Instagram como usuario, URL o enlace roto.

`DepuradorHistorico` normaliza nombres, teléfonos (+569), regiones y comunas (sin tildes ni mayúsculas), resuelve las columnas de comuna duplicadas según la región, recorta a 2 rubros y limpia los enlaces de Instagram rotos. Las fichas sin comuna o sin contacto válido quedan **OBSERVADAS** para que la administradora las revise. La importación se puede repetir sin duplicar (índice único por `idHistorico`).

---

## 10. Privacidad

- Se pide consentimiento explícito (checkbox validado) antes de guardar datos personales.
- El RUT, la fecha de nacimiento y otros datos personales no se muestran en el directorio público.
- Todos los datos de prueba son ficticios o anonimizados, según la Ley N.º 19.628 y la Ley N.º 21.719.
