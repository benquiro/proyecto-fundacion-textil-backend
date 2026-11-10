# proyecto-fundacion-textil-backend

REST API for the Fundación Textil Circular mobile app. Spring Boot 4 monolith with PostgreSQL managed by
Flyway.

## Project structure

The code follows the same `core` + `feature` split as the mobile app:

```
cl.textilcircular.api
├── core/                  shared by every feature
│   ├── model/<area>/      JPA entities with the business rules (account, business, product, messaging...)
│   ├── repository/        Spring Data repositories
│   ├── error/ validation/ persistence/ crypto/ config/
└── feature/<name>/        one package per app feature: controller, service, DTOs, MapStruct mapper
```

The schema lives in `src/main/resources/db/migration`. Rules: `core` never depends on `feature`, a feature
never depends on another feature, and services only coordinate while business rules stay in the entities.

## Requirements

- Java 25+
- Docker (PostgreSQL for local runs and Testcontainers for tests)

## Run locally with demo data

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Spring Boot starts the PostgreSQL service from `compose.yaml`. The `dev` profile also loads
`db/seed-dev/R__demo_data.sql`, the mobile app's demo data. Every demo account uses the password `Demo1234`,
for example `admin1@textilcircular.cl` (administrator), `valentina.mora@example.cl` (entrepreneur) and
`martin.diaz@example.cl` (customer).

Without the `dev` profile only the schema and reference data (regions, communes, services, age ranges and
genders) are loaded.

## API and sessions

The REST API lives under `/api/v1` and follows `docs/api/openapi.yaml`. Protected operations need
`Authorization: Bearer <accessToken>` from `/auth/login` or a sign-up. Settings:

| Property | Default | Meaning |
|----------|---------|---------|
| `app.security.jwt-secret` (env `APP_SECURITY_JWT_SECRET`) | random per start | HMAC key, at least 32 bytes. Without it sessions end on every restart. |
| `app.security.access-ttl` | `PT15M` | Access token lifetime. |
| `app.security.refresh-ttl` | `P30D` | Refresh token lifetime. |

### With the Android app on the emulator

The debug app reaches this backend at `http://10.0.2.2:8080/api/v1/`. Android 17 treats that address as
local network, so on first launch the app asks for the nearby devices permission (`ACCESS_LOCAL_NETWORK`).
Allow it; if it is denied, every call fails with "Sin conexión". To skip the permission, forward the port
and build the app against loopback:

```bash
adb reverse tcp:8080 tcp:8080
./gradlew installDebug -PapiBaseUrl=http://127.0.0.1:8080/api/v1/   # in the app repository
```

## Tests

```bash
./mvnw test
```

Repository tests run against a real PostgreSQL 18 container (Testcontainers), with the Flyway schema and
`ddl-auto=validate`.
