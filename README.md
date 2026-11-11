# proyecto-fundacion-textil-backend

API REST de la app móvil de la Fundación Textil Circular. Monolito Spring Boot 4 con PostgreSQL gestionado
por Flyway.

Para implementar una feature nueva, lee primero la [guía para implementar features](docs/backend-feature-guide.md).

## Estructura del proyecto

El código sigue la misma división `core` + `feature` que la app móvil:

```
cl.textilcircular.api
├── core/                  compartido por todas las features
│   ├── model/<área>/      entidades JPA con las reglas de negocio (account, business, product, messaging...)
│   ├── repository/        repositorios de Spring Data
│   ├── error/ validation/ persistence/ crypto/ config/ security/ web/ realtime/
└── feature/<nombre>/      un paquete por feature de la app: controlador, servicio y DTOs
```

El esquema está en `src/main/resources/db/migration`. Reglas: `core` nunca depende de `feature`, una feature
nunca depende de otra, y los servicios solo coordinan: las reglas de negocio viven en las entidades.

## Requisitos

- Java 25+
- Docker (PostgreSQL para correr la API en local y Testcontainers para los tests)

## Correr en local con datos de demo

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Spring Boot levanta el servicio de PostgreSQL definido en `compose.yaml`; no hay que crear la base a mano. El
perfil `dev` carga además `db/seed-dev/R__demo_data.sql`, los datos de demo de la app móvil. Todas las cuentas
de demo usan la contraseña `Demo1234`, por ejemplo `admin1@textilcircular.cl` (administrador),
`valentina.mora@example.cl` (emprendedora) y `martin.diaz@example.cl` (cliente).

Sin el perfil `dev` solo se cargan el esquema y los datos de referencia (regiones, comunas, servicios, rangos
de edad y géneros).

## API y sesiones

La API REST vive bajo `/api/v1` y sigue `docs/api/openapi.yaml`. Las operaciones protegidas necesitan
`Authorization: Bearer <accessToken>`, obtenido con `/auth/login` o con un registro. Configuración:

| Propiedad | Por defecto | Significado |
|-----------|-------------|-------------|
| `app.security.jwt-secret` (variable `APP_SECURITY_JWT_SECRET`) | aleatoria en cada inicio | Clave HMAC de al menos 32 bytes. Sin ella, las sesiones terminan en cada reinicio. |
| `app.security.access-ttl` | `PT15M` | Duración del token de acceso. |
| `app.security.refresh-ttl` | `P30D` | Duración del token de refresco. |

### Con la app Android en el emulador

La app de debug llega a este backend en `http://10.0.2.2:8080/api/v1/`. Android 17 trata esa dirección como
red local, así que en el primer inicio la app pide el permiso de dispositivos cercanos
(`ACCESS_LOCAL_NETWORK`). Acéptalo: si se rechaza, todas las llamadas fallan con "Sin conexión". Para evitar
el permiso, redirige el puerto y compila la app apuntando a loopback:

```bash
adb reverse tcp:8080 tcp:8080
./gradlew installDebug -PapiBaseUrl=http://127.0.0.1:8080/api/v1/   # en el repositorio de la app
```

## Tests

```bash
./mvnw test
```

Los tests corren contra un contenedor real de PostgreSQL 18 (Testcontainers), con el esquema de Flyway y
`ddl-auto=validate`. Necesitan Docker abierto.

## Convenciones

- Los DTOs de petición y respuesta usan `Instant` (u `OffsetDateTime`) para fechas con hora, nunca
  `LocalDateTime`, porque la app Android las lee con `OffsetDateTime.parse` y necesita el desfase horario.
- Las URLs de imágenes que devuelve la API son siempre absolutas.
