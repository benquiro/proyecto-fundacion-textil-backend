# proyecto-fundacion-textil-backend

REST API for the Fundación Textil Circular mobile app. Spring Boot 4 monolith organized by feature
(`account`, `business`, `product`, `messaging`, `outreach`, `media`, `analytics`, `reference`), with
PostgreSQL managed by Flyway.

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

## Tests

```bash
./mvnw test
```

Repository tests run against a real PostgreSQL 18 container (Testcontainers), with the Flyway schema and
`ddl-auto=validate`.
