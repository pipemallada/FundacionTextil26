# Microservicio Red Textil Circular (Spring Boot)
API REST que usa la app Android para guardar y leer fichas, catálogo, consultas, métricas, correos y el dataset histórico.

## Tecnologías
- Spring Boot 3.5 y Java 17
- Spring Web, Spring Data JPA y Bean Validation
- Base de datos H2 guardada en archivo (`data/redtextil.mv.db`)
- Pruebas con JUnit 5 y MockMvc

## Estructura (por capas)
- `controller`: endpoints REST de fichas, catálogo, consultas, métricas, correos e histórico.
- `service`: reglas de negocio (no duplicar registros, guardar fotos, cargar el histórico).
- `repository`: interfaces de Spring Data JPA para leer y escribir en la base.
- `model`: entidades JPA, una por tabla.
- `exception`: errores 400 y 404 con el formato `{"ok": false, "mensaje": "..."}`.

## Cómo ejecutarlo (Windows, desde la carpeta `backend`)
`$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"`
`.\mvnw.cmd spring-boot:run`
Queda en `http://localhost:8080/api`.
El emulador lo llama con `http://10.0.2.2:8080/api/`.

## Base de datos
- Consola web: `http://localhost:8080/api/h2-console`
- JDBC URL: `jdbc:h2:file:./data/redtextil;AUTO_SERVER=TRUE`, usuario `sa`, sin clave.
- Tablas: `FICHA`, `FICHA_RUBRO`, `PUBLICACION`, `CONSULTA`, `INTERACCION`, `CORREO`, `FICHA_HISTORICA` y `FICHA_HISTORICA_COMUNA`.
- Para partir de cero: detener el servidor y borrar la carpeta `data`.

## Endpoints
- Fichas: `GET/POST /fichas` y `GET/PUT/DELETE /fichas/{id}`
- Catálogo: `GET/POST /catalogo` y `GET/PUT/DELETE /catalogo/{id}`
- Fotos del catálogo: `POST/GET /catalogo/{id}/foto`
- Consultas: `GET/POST /consultas` y `GET/PUT/DELETE /consultas/{id}`
- Métricas: `POST/GET /metricas/interacciones` y `GET /metricas/resumen`
- Correos: `POST /correos/enviar` y `GET /correos`
- Histórico: `GET /historico/emprendimientos`

## Pruebas
`.\mvnw.cmd test`
12 pruebas con MockMvc que recorren el CRUD de cada recurso y los errores 400 y 404. Usan una base H2 en memoria, por lo que no tocan los datos reales.
