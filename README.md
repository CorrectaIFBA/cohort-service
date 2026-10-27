# cohort-service

Starting point for new Correcta microservices. It keeps the identity-service
stack and conventions (Spring Boot 3.4, Kotlin, JPA/PostgreSQL, validation,
OpenAPI, Actuator, controller interface + implementation, Portuguese error
messages, annotation-based authorization) without the identity domain.

## Authentication and authorization

This service does not validate tokens. The gateway (`local/proxy` in
development, forward auth in the cluster) calls identity-service
`GET /account/me` with the caller credentials and sends the result as headers:

| Header | Source in `/account/me` |
| --- | --- |
| `X-User-Id` | `profile.profileId` |
| `X-User-Person-Id` | `person.personId` |
| `X-User-Profile-Type` | `profile.type` |
| `X-User-Login` | `account.login` |
| `X-User-Email` | `account.email` |
| `X-User-Traits` | `account.traits`, comma-separated |

`HeaderAuthenticationFilter` turns these headers into a `ProfilePrincipal`
with the traits as authorities. `SecurityAspect` enforces the annotations on
controller methods:

- `@IsLoggedUser`: requires `X-User-Id`, otherwise `401`.
- `@IsProfileType("STAFF")`: requires the profile type, otherwise `403`.
- `@IsTraitedWith("TRAIT")` / `@IsNotTraitedWith("TRAIT")`: checks traits, otherwise `403`.

Use `ScopeEvaluatorComponent.currentProfile()` in services to read the caller.

The headers are trusted as-is, so the service must only be reachable through
the gateway, and the gateway must strip client-supplied `X-User-*` headers.

## Creating a new service

1. Copy this directory and rename `cohort-service` in
   `settings.gradle.kts`, `build.gradle.kts`, `application.properties` and
   `.github/workflows/release.yml`.
2. Rename the `io.correcta.boilerplate` package.
3. Replace the `Cohort` resource (entity, repository, specifications,
   service, controller and models) with the service domain.
4. Add a route with `"auth": true` to `local/proxy/routes.json`.

## Configuration

| Variable | Default |
| --- | --- |
| `ENV_DATASOURCE_POSTGRES_URL` | required |
| `ENV_DATASOURCE_POSTGRES_USERNAME` | required |
| `ENV_DATASOURCE_POSTGRES_PASSWORD` | required |
| `ENV_JPA_DDL_AUTO` | `update` |
| `ENV_SPRINGDOC_ENABLED` | `false` |
| `ENV_IDENTITY_SERVICE_URL` | required |
| `ENV_IDENTITY_SERVICE_INTERNAL_TOKEN` | required |
| `ENV_IDENTITY_SERVICE_CONNECT_TIMEOUT_MS` | `2000` |
| `ENV_IDENTITY_SERVICE_READ_TIMEOUT_MS` | `5000` |

## Identity-service integration

`IdentityClient` (OpenFeign) calls identity-service `GET /api/v1/profiles/{profileId}`
and `POST /api/v1/profiles/batch-lookup` to check eligibility and enrich member and
invitation listings. Every call sends the `X-Internal-Token` header. Failures other
than `404` fail closed with `503`.

## Internal endpoints

`GET /api/v1/internal/cohorts/{cohortId}/members/{profileId}` returns the
current membership of a profile (`role`) and the cohort status, or `404`. It
has no authentication: other services (assessment-service) call it directly
inside the cluster. Do not expose `/api/v1/internal` through the gateway.

Run locally from the workspace root with `./local/run boilerplate` (port 8084)
and reach it through the proxy at `http://127.0.0.1:8080/api/boilerplate`.
Run the tests with `./gradlew test` (H2, no external services).
