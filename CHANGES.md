# Change Summary

This document summarizes the work completed on the codebase.

## Core fixes and features
- Fixed DI issue by removing circular dependency between `TaskService` and `FileService`.
- Added counter task type with fields `x`, `y`, `progress`, `status`, `cancelRequested` in `ProjectGenerationTask`.
- Implemented background execution with per-second progress updates and idempotent cancellation.
- Preserved legacy file-generation task (serving `challenge.zip`).
- Added periodic cleanup of week-old pending tasks and enabled scheduling.

## Robustness and concurrency
- Replaced unbounded pool with bounded executor (fixed thread pool).
- Added graceful shutdown of executor on application stop.
- Introduced back-pressure handling with 429 (RejectedExecution) via `OverloadedException`.
- Made cancellation idempotent and resilient to race conditions.

## API quality and validation
- Bean Validation for inputs (e.g., `@NotBlank name`, `@Min(0) x/y`).
- Consistent JSON error responses (400/401/404/429/500) via centralized `ErrorController`.
- Added springdoc OpenAPI UI dependency for API docs.

## Observability
- Added Spring Boot Actuator and Prometheus metrics registry.
- Exposed health, metrics, and Prometheus endpoints; allowed unauthenticated access for probes.
- Added correlation ID support via `X-Request-ID` using SLF4J MDC in `SimpleHeaderFilter`.

## Configuration and security
- Externalized auth secret via `security.header.value` property.
- Introduced `SecurityProperties` (Spring `@ConfigurationProperties`) to inject config (SOLID/DI).
- Disabled Open-Session-In-View (`spring.jpa.open-in-view=false`).

## Build and packaging
- Added Spring Boot Maven plugin to repackage executable fat jar.
- Verified `mvn package && java -jar target/*.jar` starts the app; health endpoint shows `UP`.

## Tests
- Service tests: counter completion and cancellation.
- Controller tests: create/execute/progress flow (with auth header).
- Security/validation tests: 401 for missing auth, 400 JSON for invalid payloads.
- Actuator test: health endpoint accessible without auth.
- All tests passing locally.

## Endpoints (quick reference)
- `GET /api/tasks/` — list tasks
- `POST /api/tasks/` — create task (counter: `{name,x,y}`; legacy: `{name}`)
- `GET /api/tasks/{id}` — get task
- `PUT /api/tasks/{id}` — update task (name/date)
- `DELETE /api/tasks/{id}` — delete task
- `POST /api/tasks/{id}/execute` — execute task
- `GET /api/tasks/{id}/progress` — task with `status` and `progress`
- `POST /api/tasks/{id}/cancel` — cancel counter task
- `GET /api/tasks/{id}/result` — download legacy zip result
- Actuator: `GET /actuator/health`, `GET /actuator/metrics`, `GET /actuator/prometheus`

## Branches/PRs
- Pushed main implementation to `main`.
- Added tests to `celv1_1` (PR opened).
- Hardening, observability, and config changes on `hardenings` (create PR to `main`).

## Principles
- SOLID: extracted `SecurityProperties`; centralized error handling; cohesive `TaskService` lifecycle.
- DRY: helpers for status/progress updates; unified error JSON construction.

## How to run
- `mvn spring-boot:run` OR `mvn -DskipTests package && java -jar target/challenge-java-broken-1.1-SNAPSHOT.jar`
- Required header for API: `Celonis-Auth: totally_secret`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
