# Feature: core-domain-persistence

Objective: backend core (domain, PostgreSQL schema, repositories) for the Fundación Textil Circular API, taken from the Android app's intent.
Branch: `feat/core-domain-persistence` · Delivery strategy: `ask-on-risk` (chain strategy asked before PR creation; commits are linear work units) · Test runner: `./mvnw test` (JUnit 5, Testcontainers PostgreSQL).

## Specs

- **S1 Scope.** User: "Con el flujo ODD me ayudas a implementar el core del backend, hablo de todo lo que sería el: - Dominio - BD - Repositorio". In scope: JPA entities with domain behavior, Flyway schema, Spring Data repositories, reference data, dev demo seed, tests. Out of scope: REST controllers, auth/JWT wiring, WebSocket, metrics aggregation queries (they belong to their API slices).
- **S2 Architecture.** User: "La arquitectura será un monolito" and "será monolito pero me parece sobreingeniería hacer una arquitectura hexagonal, a lo mucho utilizamos vertical slices". Packages by feature under `cl.textilcircular.api` (`shared`, `reference`, `account`, `business`, `product`, `messaging`, `outreach`, `media`, `analytics`). JPA entities are the domain model (rich entities: invariants and state transitions live in them). Spring Data JPA repositories used directly. No ports/adapters, no duplicated domain model.
- **S3 Dependencies.** User: "Se pueden agregar más dependencias según el caso y lo que veas que sea lo más optimo y recomendable. Por ejemplo ahora te recomiendo que agreges la dependencia MapStruct, porque ayuda bastante." Add MapStruct 1.6.3 (+ `lombok-mapstruct-binding`) and Testcontainers PostgreSQL for tests.
- **S4 Approved DB model changes.** User: "Los cambios del modelo los apruebo". Baseline = Room schema v1 (15 tables + `V_CATALOG`, `docs/DATABASE_MODEL.md`), plus:
  - Types: `bigint` ids from per-table sequences (allocation 50), `timestamptz`, `message.client_uuid uuid`, enums as `varchar` + `CHECK`, lowercase snake_case names.
  - Integrity: status/timestamp consistency `CHECK`s (`user_account`, `business`, `product`); app max lengths (alias 40, email 254, person name 60, business name 100, business description 500, url 500, product title 100, product description 1000, message 2000, comment 500, content title 150, event location 200) as `CHECK`s; `price >= 0`; unique `lower(email)`; RUT stored as `12345678-5`; a REJECTED vote requires `rejection_reason` and `comment`.
  - Indexes: explicit FK indexes; partial indexes for the directory (APPROVED, visible) and the review queue (PENDING); vote unique `(business_id, review_round, reviewer_id)`; partial unread index `read_at IS NULL`; `pg_trgm` + `unaccent` text search.
  - Concurrency: pessimistic lock of the business row when voting; `@Version` on mutable aggregates.
  - Model: `message.product_id` nullable; `business_review.round` → `review_round`; `ON DELETE CASCADE` only for `business_service` and `product_photo`.
  - Additions (earlier decision "Agrega lo necesario, la app tubo recortes los cuales el backend tendrá que arreglar e implementar"): `conversation_report`, `analytics_event`, product attributes (size, condition, material, offering type), `business_review.rejection_reason`, `refresh_token`, `media_file`, `password_reset_code`.
- **S5 Domain rules (fixes over the app).** Engram `project/mobile-business-rules` + `project/backend-design-decisions`: BCrypt hashes; email trim+lowercase; password ≥ 8 with letter and digit; RUT mod 11 check; business 1–2 distinct services; product 1–5 photos; vote: business PENDING and not deleted, reviewer not the owner, one vote per round, any REJECTED → REJECTED, 2 APPROVED → APPROVED; substantive edit of REJECTED/APPROVED → PENDING with round + 1; hide/show without review; account suspend/reactivate/deactivate/delete+anonymize; message idempotency by `client_uuid` with sender conflict detection; mark-read only by participants; outreach type-specific required fields and update operation. Error messages reuse the app's Spanish texts.
- **S6 No third-party integrations** (storage, push, email simulated).
- **S7 Seed.** Reference data (16 regions, 346 communes, 7 services, 7 age ranges, 4 genders) in every environment; demo data from the app seed JSON only in the `dev` profile, passwords re-hashed with BCrypt (`Demo1234`).

## Tasks

| ID | Specs | Route | Status | Commit / evidence |
|----|-------|-------|--------|-------------------|
| T1 | S2 S3 S6 S7 | inline | pending | foundation: pom, config, shared, Testcontainers base, reference module + reference data |
| T2 | S4 S5 | inline | pending | account: user_account, entrepreneur, refresh_token, password_reset_code |
| T3 | S4 S5 | inline | pending | business: business, services, reviews (vote lock), v_catalog + directory search |
| T4 | S4 S5 | inline | pending | product: product, photos, attributes |
| T5 | S4 S5 | inline | pending | messaging: conversation, message, report, inbox/unread queries |
| T6 | S4 S5 S6 | inline | pending | outreach, media_file, analytics_event |
| T7 | S7 | inline | pending | dev demo seed |
| T8 | all | verifier | pending | full suite + independent verifier (Risk items 2 and 4) |

Route evidence: units depend on each other in sequence (business → account, product → business, messaging → business/product), so no Writer trigger (parallel independent units) holds; all writes inline.

## Log

- **L1** (2026-11-09) User: "Con el flujo ODD me ayudas a implementar el core del backend, hablo de todo lo que sería el: - Dominio - BD - Repositorio También antes de la implementación analiza si el modelo de la BD es el más optimo, ya que fue hecho para funcionar, si necesita modificacione o adaptarse, comentamelo - La arquitectura será un monolito - Se pueden agregar más dependencias según el caso y lo que veas que sea lo más optimo y recomendable. Por ejemplo ahora te recomiendo que agreges la dependencia MapStruct, porque ayuda bastante."
- **L2** (2026-11-09) DB analysis presented (types, integrity, indexes, concurrency race on votes, `message.product_id`). User: "Los cambios del modelo los apruebo, sobre la arquitectura, será monolito pero me parece sobreingeniería hacer una arquitectura hexagonal, a lo mucho uilizamos vertical slices". Hexagonal dropped; ArchUnit dropped.
- **L3** Forecast ~4000 authored lines (generated seed SQL excluded) → over the 400-line budget; linear work-unit commits, chain strategy asked before any PR.
