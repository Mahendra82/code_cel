# Deployment Guide

## Prerequisites
- Java 11 (or compatible JDK)
- Maven 3.6+

## Build
```bash
mvn -DskipTests package
```
Produces: `target/challenge-java-broken-1.1-SNAPSHOT.jar`

## Run (local)
```bash
java -jar target/challenge-java-broken-1.1-SNAPSHOT.jar
```
- API requires header: `Celonis-Auth: totally_secret`
- Health: `GET http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Configuration
All config in `src/main/resources/application.properties` (can be overridden via env vars or JVM `-D`):
- `security.header.value` (default: `totally_secret`)
- `management.endpoints.web.exposure.include=health,info,metrics,prometheus`
- `spring.jpa.open-in-view=false`

Override example:
```bash
java -Dsecurity.header.value=mysecret -jar target/challenge-java-broken-1.1-SNAPSHOT.jar
```

## Container (optional quick start)
If you create a Dockerfile:
```Dockerfile
FROM eclipse-temurin:11-jre
WORKDIR /app
COPY target/challenge-java-broken-1.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
```
Build and run:
```bash
docker build -t celonis-app .
docker run --rm -p 8080:8080 -e SECURITY_HEADER_VALUE=totally_secret celonis-app
```

## Health/Readiness
- `GET /actuator/health` (no auth)
- `GET /actuator/metrics` (no auth)
- `GET /actuator/prometheus` (no auth)

## Notes
- Counter task executes in background with bounded thread pool.
- Cancellation is idempotent.
- Cleanup job deletes week-old pending tasks (runs daily at 03:00).
