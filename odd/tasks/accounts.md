# Feature: accounts

Objective: REST slice for accounts (auth, profile, admin user management) implementing `docs/api/openapi.yaml`, working end to end with the Android app.
Branch: `feat/accounts` (from `feat/core-domain-persistence`) · Delivery strategy: `ask-on-risk` · Test runner: `./mvnw test` (JUnit 5, MockMvc, Testcontainers PostgreSQL).

## Specs

- **S1 Scope.** User: "Implementemos la feauture de cuentas, utiliza el flujo ODD para está implementacione." Contract operations: `registerCustomer`, `registerEntrepreneur`, `login`, `refreshSession`, `logout`, `getProfile`, `updateProfile`, `deleteAccount`, `changePassword`, `deactivateAccount`, and the admin account operations `listUsers`, `suspendUser`, `reactivateUser` (assumption: account lifecycle belongs to this feature). Also the lookups `/regions`, `/communes`, `/services`, `/age-ranges`, `/genders` (assumption: the entrepreneur sign-up screen needs them, so the flow cannot work without them). Out of scope: password recovery (no contract operation, no app screen), other feature slices.
- **S2 API foundation.** As the contract states: base path `/api/v1`, JSON camelCase, errors as `application/problem+json` with `code`, `detail`, `errors[{field,message}]` and `accountStatus`; Spanish texts from the domain. Bearer JWT (HS256) access token, `expiresIn` 900 s, identity from the token only; refresh token rotated on `/auth/refresh` (30 days), revoked on logout; `changePassword` "Revokes every other session of the account".
- **S3 Acceptance.** User: "para considerar lista está funcionalidad el flujo debe funcionar entre el backend y la app móvil @../Proyecto-Textil-Circular/Proyecto-Fundacion-Textil". Verified with the backend running (`dev` profile) and the debug app on the `Medium_Phone` emulator: customer sign-up, login, profile view and edit, password change, logout, session refresh, entrepreneur sign-up (lookups loaded), visible error for wrong credentials.
- **S4 Lean tests.** Memory "Lean tests, low tokens": happy path plus the main error per endpoint group, through MockMvc against PostgreSQL.

## Tasks

| ID | Specs | Route | Status | Commit / evidence |
|----|-------|-------|--------|-------------------|
| T1 | S2 S1 | inline | pending | web foundation (security, JWT, problem handler, `/api/v1` prefix) + lookups |
| T2 | S1 S2 | inline | pending | auth: register customer/entrepreneur, login, refresh, logout |
| T3 | S1 S2 | inline | pending | profile: get/patch/delete `/me`, password, deactivation |
| T4 | S1 | inline | pending | admin users: list, suspend, reactivate |
| T5 | S3 | inline | pending | end to end with the app on the emulator |

Route evidence: each task builds on T1 and touches the same security/error configuration, so no parallel independent units; all writes inline.

## Log

- **L1** (2026-11-10) User: "Implementemos la feauture de cuentas, utiliza el flujo ODD para está implementacione. - para considerar lista está funcionalidad el flujo debe funcionar entre el backend y la app móvil @../Proyecto-Textil-Circular/Proyecto-Fundacion-Textil"
- **L2** (2026-11-10) Baseline: no web layer existed (no controllers, security config or error handler). Forecast ~1200 authored lines → over 400; linear work-unit commits, chain strategy asked before any PR. The previous session's uncommitted code-review fixes were committed first on `feat/core-domain-persistence` so this feature starts from a clean tree.
