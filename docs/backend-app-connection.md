# Conexión entre el backend y la app Android

Esta guía explica cómo se comunican la app Android y el backend, qué hace cada parte y cómo probarlo.

## Idea general

```
App Android (emulador)                          Backend (tu Mac)
───────────────────────                         ────────────────────────────
Pantalla → ViewModel → Repositorio              Controller → Service → Entidad
                 │                                        │
                 ▼                                        ▼
        Retrofit + OkHttp  ── HTTP JSON ──►  Spring MVC + Spring Security
        http://10.0.2.2:8080/api/v1/         http://localhost:8080/api/v1/
                                                          │
                                                          ▼
                                                PostgreSQL (Docker)
```

Las dos partes respetan un mismo contrato: `docs/api/openapi.yaml`. Ahí están las rutas, los cuerpos JSON y
los errores. Mientras ninguna de las dos se salga del contrato, se entienden.

## 1. Cómo llega la app al backend

### La dirección

- El emulador ve tu Mac en la dirección especial `10.0.2.2`. Por eso la URL por defecto de la app es
  `http://10.0.2.2:8080/api/v1/`, definida en `app/build.gradle.kts` como `BuildConfig.API_BASE_URL`.
- Se puede cambiar al compilar: `./gradlew installDebug -PapiBaseUrl=https://otro-servidor/api/v1/`
  (debe terminar en `/`).
- Solo el build debug permite HTTP sin cifrar (`usesCleartextTraffic`). El de release exige HTTPS.

### El permiso de red local (Android 17)

En Android 17, una app que apunta a SDK 37 no puede abrir conexiones a la red local (y `10.0.2.2` cuenta
como red local) sin el permiso `ACCESS_LOCAL_NETWORK`. Sin él, la conexión no se abre y la app muestra
"Sin conexión. Revisa tu internet e intenta de nuevo".

La solución en la app:

- `AndroidManifest.xml` declara el permiso.
- `MainActivity` lo pide al iniciar, en la misma solicitud que el de notificaciones, **solo** si el
  dispositivo tiene Android 17 o más y la URL de la API es de red local. La función que lo decide es
  `isLocalNetworkUrl` (`core/network/LocalNetwork.kt`): IPv4 privadas, `169.254.x.x` y nombres `.local`.
- El sistema lo muestra como el permiso de "dispositivos cercanos". Hay que tocar **Allow**.
- Lo que la app cargó antes de aceptar muestra "Sin conexión" hasta que tocas "Reintentar".

Alternativa sin permiso: `adb reverse tcp:8080 tcp:8080` y compilar con
`-PapiBaseUrl=http://127.0.0.1:8080/api/v1/`. `127.0.0.1` es loopback y no necesita el permiso.

### Los dos clientes HTTP de la app

`core/network/di/NetworkModule.kt` crea dos clientes OkHttp:

| Cliente | Para qué | Envía token |
|---------|----------|-------------|
| Público | Registro, login, renovación de sesión (`AuthApi`), listas de referencia (`LookupApi`) y contenidos (`ContentApi`) | No |
| Con sesión | Todo lo demás (`ProfileApi`, directorio, chat...) | Sí |

Retrofit convierte cada interfaz (`ProfileApi`, `AuthApi`...) en llamadas HTTP, y `kotlinx.serialization`
traduce el JSON a objetos Kotlin (`dto/AccountDtos.kt`).

## 2. La sesión: tokens

Al registrarse o iniciar sesión, el backend responde:

```json
{ "accessToken": "eyJ...", "refreshToken": "6Qai...", "expiresIn": 900, "user": { ... } }
```

| Token | Dura | Para qué |
|-------|------|----------|
| `accessToken` (JWT firmado con HS256) | 15 min | Va en cada llamada: `Authorization: Bearer <token>` |
| `refreshToken` (texto aleatorio; el backend guarda solo su hash) | 30 días | Pedir un par nuevo cuando el de acceso vence |

La app guarda ambos en DataStore (`SessionManager`), así que la sesión sobrevive aunque cierres la app.

### Lo que hace la app en cada llamada (`core/network/AuthInterceptor.kt`)

1. `AuthInterceptor` agrega `Authorization: Bearer <accessToken>`.
2. Si el backend responde 401, `TokenAuthenticator` llama a `POST /auth/refresh` con el token de refresco,
   guarda el par nuevo y repite la llamada **una vez**.
3. Si varias llamadas fallan al mismo tiempo, se hace **una sola** renovación y las demás usan el token nuevo.
4. Si el backend rechaza el token de refresco (401/403), la sesión termina y la app vuelve a "Ingresar".
   Si en cambio no hay conexión, la sesión se conserva.

### Lo que hace el backend (`core/security`)

- `SecurityConfig`: la API no guarda estado de sesión; cada petición trae su token. Las rutas `/auth/**` y
  las listas de referencia son públicas, `/admin/**` exige rol ADMINISTRATOR y el resto exige token.
- `TokenService`: crea el JWT con `sub` (id del usuario), `role` y `sid` (id del token de refresco del que
  viene, es decir, de la sesión).
- `SessionValidator`: en cada petición verifica que la sesión `sid` siga viva. Por eso cerrar sesión, cambiar
  la contraseña, ser suspendida o desactivar la cuenta cortan el acceso **de inmediato**.
- Renovación (`AuthService.refresh`): el token de refresco usado se anula y se entrega uno nuevo. Si alguien
  presenta un token que **ya fue cambiado**, se asume que se filtró y se cierran todas las sesiones de la cuenta.
- La firma del JWT usa `app.security.jwt-secret`. Si no lo configuras, se genera una clave al azar en cada
  arranque: al reiniciar el backend todos los tokens de acceso dejan de valer, pero la app se recupera sola
  con el token de refresco (que vive en la base).

```
App                                   Backend
 │ POST /auth/login {email, password}   │
 │ ───────────────────────────────────► │ verifica contraseña (BCrypt)
 │ ◄─────────────────────────────────── │ 200 {accessToken, refreshToken}
 │ GET /me  (Bearer access)             │
 │ ───────────────────────────────────► │ firma OK + sesión viva → 200
 │   ... 15 min después ...             │
 │ GET /me  (Bearer access vencido)     │
 │ ───────────────────────────────────► │ 401
 │ POST /auth/refresh {refreshToken}    │
 │ ───────────────────────────────────► │ anula el viejo, crea uno nuevo
 │ ◄─────────────────────────────────── │ 200 {par nuevo}
 │ GET /me  (Bearer nuevo)  → 200       │
```

## 3. Los errores

El backend responde todos los errores con el mismo formato (`application/problem+json`):

```json
{ "status": 400, "code": "VALIDATION", "detail": "Revisa los datos ingresados",
  "errors": [ { "field": "password", "message": "..." } ] }
```

La app lee `code` en `core/network/ApiCall.kt` y lo convierte en un `AppError` que la pantalla sabe mostrar:

| `code` del backend | Lo que ve la usuaria |
|--------------------|----------------------|
| `VALIDATION` | El mensaje debajo del campo indicado en `errors[].field` |
| `INVALID_CREDENTIALS` | "Correo o contraseña incorrectos" |
| `ACCOUNT_NOT_ACTIVE` (+ `accountStatus`) | Aviso de cuenta suspendida (una cuenta desactivada se reactiva al iniciar sesión) |
| `EMAIL_TAKEN` / `RUT_TAKEN` | "Ese correo ya tiene una cuenta" / RUT en uso |
| 401 (`UNAUTHORIZED`) | Intenta renovar; si no puede, "Tu sesión expiró" |
| Sin respuesta | "Sin conexión. Revisa tu internet e intenta de nuevo" |

Los textos en español salen del backend (vienen de las reglas del dominio), así que la app no los duplica.

## 4. Cómo probarlo

### Requisitos

- Docker Desktop abierto (el backend levanta PostgreSQL con `compose.yaml`).
- Java 25 y Android Studio con un emulador de Android 17 (por ejemplo `Medium_Phone`).

### Paso 1: levantar el backend con datos de demostración

```bash
cd proyecto-fundacion-backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Comprueba que responde:

```bash
curl -s http://localhost:8080/api/v1/regions | head -c 200
```

Cuentas demo (contraseña `Demo1234`): `martin.diaz@example.cl` (cliente),
`valentina.mora@example.cl` (emprendedora), `admin1@textilcircular.cl` (administradora).

### Paso 2: probar la API sin la app (opcional)

```bash
# Login: guarda los tokens
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"martin.diaz@example.cl","password":"Demo1234"}'

# Perfil, con el accessToken de la respuesta anterior
curl -s http://localhost:8080/api/v1/me -H "Authorization: Bearer <accessToken>"

# Renovar la sesión
curl -s -X POST http://localhost:8080/api/v1/auth/refresh \
  -H 'Content-Type: application/json' -d '{"refreshToken":"<refreshToken>"}'
```

También puedes explorar todo en Swagger: http://localhost:8080/swagger-ui.html

### Paso 3: instalar y abrir la app

```bash
cd Proyecto-Fundacion-Textil
./gradlew installDebug
```

1. Abre la app en el emulador. Aparece el permiso de dispositivos cercanos: toca **Allow**.
2. Ve a **Ingresar** y entra con `martin.diaz@example.cl` / `Demo1234`.
3. La barra inferior cambia a "Mensajes" y "Mi cuenta": la sesión está activa.

### Paso 4: recorridos para verificar

| Prueba | Qué hacer | Qué debe pasar |
|--------|-----------|----------------|
| Credenciales malas | Ingresar con contraseña incorrecta | "Correo o contraseña incorrectos" |
| Registro | "Crear cuenta" como cliente o emprendedora | Entra sola; las listas (regiones, comunas, servicios) vienen del backend |
| Perfil | Mi cuenta → Editar mis datos → Guardar | El cambio se ve en la app y en la base |
| Contraseña | Cambiar contraseña con la actual mal escrita | Error bajo el campo "contraseña actual" |
| Cerrar sesión | Mi cuenta → Cerrar sesión | Vuelve a "Ingresar"; el token queda anulado |
| Renovación automática | Ver abajo | La app sigue funcionando sin pedir login |

**Renovación automática.** Arranca el backend con tokens cortos:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev \
  -Dspring-boot.run.arguments=--app.security.access-ttl=PT30S
```

Inicia sesión, espera **más de 90 segundos** (30 s de vida + 60 s de tolerancia de reloj que Spring acepta
por defecto) y guarda un cambio en el perfil. Debe guardarse sin pedir login.

### Paso 5: mirar la base de datos

```bash
docker compose exec postgres psql -U textil -d textil_circular
```

```sql
-- Sesiones de una cuenta: revoked_at = anulada, rotated_at = cambiada por una nueva
select t.token_id, t.created_at, t.revoked_at, t.rotated_at
from refresh_token t join user_account a on a.user_id = t.user_id
where a.email = 'martin.diaz@example.cl'
order by t.token_id desc limit 5;
```

Después de la renovación automática verás el token viejo con `rotated_at` y uno nuevo activo. Después de
cerrar sesión, el último tendrá `revoked_at` y `rotated_at` vacío.

### Paso 6: pruebas automáticas

```bash
# Backend: API completa contra PostgreSQL real (Testcontainers)
./mvnw test

# App: pruebas unitarias, incluida la conversión de errores con MockWebServer
./gradlew testDebugUnitTest
```

## 5. Problemas comunes

| Síntoma | Causa | Solución |
|---------|-------|----------|
| "Sin conexión" en todo | Backend apagado, o permiso de red local negado | Revisa `curl localhost:8080/api/v1/regions`; en el emulador: Ajustes → Apps → Textil Circular → Permisos → Dispositivos cercanos |
| No aparece el diálogo del permiso | Ya se negó antes, o la app es una versión vieja | `adb shell pm grant cl.textilcircular.app android.permission.ACCESS_LOCAL_NETWORK`, o reinstala |
| La app cambió sola a "Ingresar" | La sesión terminó (cierre en otro lado, cambio de contraseña, token reutilizado) | Vuelve a iniciar sesión |
| "Tu sesión expiró" en Novedades de Inicio | `/contents` aún no existe en el backend (es de otra feature) | Esperado por ahora |
| El emulador "olvidó" la app nueva | Arrancó desde una instantánea vieja | `./gradlew installDebug` otra vez |
