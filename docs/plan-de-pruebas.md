# Plan de pruebas

## 1. Pruebas unitarias de la app (JUnit + MockK)
Ubicación: `app/src/test/java/cl/duoc/redtextil/`
173 pruebas de ViewModels (`ui/viewmodel`), repositorios (`data/repository`) y validadores (`domain/validation`).
Las dependencias (DAOs de Room, Retrofit, estado de la red) se reemplazan por objetos simulados con MockK, así se prueba solo la lógica.
Cobertura medida con Kover: 99,2% de las líneas de ViewModel, Repository y dominio. El mínimo exigido es 80%.

Cómo correrlas (Android Studio, panel Gradle, app, Tasks, verification):
- `testDebugUnitTest`: ejecuta las pruebas.
- `koverHtmlReportDebug`: genera el reporte en `app/build/reports/kover/htmlDebug/index.html`.
- `koverVerifyDebug`: falla si la cobertura baja del 80%.

## 2. Pruebas del microservicio (JUnit 5 + MockMvc)
Ubicación: `backend/src/test/java/cl/duoc/redtextil/api/`
12 pruebas: CRUD de fichas, catálogo (con foto) y consultas; métricas, correos e histórico; errores 400 y 404.
Comando: `.\mvnw.cmd test` dentro de la carpeta `backend`.

## 3. Pruebas manuales (app + microservicio)
1. **Validación del registro.** Mi perfil, Crear mi cuenta nueva, escribir un RUT inválido.
   Esperado: el campo se marca en rojo con el mensaje de error.
2. **Crear trabajo.** Catálogo, Nuevo trabajo con foto, Guardar.
   Esperado: aparece en `/api/catalogo` y su foto en `/api/catalogo/{id}/foto`.
3. **Editar trabajo.** Cambiar el título y guardar.
   Esperado: el título cambia en `/api/catalogo`.
4. **Eliminar trabajo.** Eliminar y confirmar.
   Esperado: desaparece de `/api/catalogo`.
5. **Recibir consulta.** Crear una consulta en el servidor y tocar Actualizar en Consultas.
   Esperado: aparece en la bandeja y llega una notificación.
6. **Responder y eliminar consulta.** Responder y luego eliminar.
   Esperado: `/api/consultas` muestra la respuesta y después ya no la tiene.
7. **Sin señal.** Menú, Simular sin señal, crear un trabajo y luego desactivarlo.
   Esperado: queda "Por enviar" y se sube al volver la señal.
8. **API externa.** Abrir el Directorio y un emprendimiento.
   Esperado: se ven el dólar, el euro y la UF, y los precios en US\$.
9. **Persistencia.** Reiniciar el servidor.
   Esperado: los datos siguen en `/api/fichas`.
