# syntax=docker/dockerfile:1

# ---- Etapa 1: compilacion ----
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# pom.xml primero: la capa de dependencias se reutiliza mientras el pom no cambie.
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Etapa 2: ejecucion ----
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S appgroup && adduser -S -G appgroup appuser

WORKDIR /app
COPY --from=builder --chown=appuser:appgroup /build/target/global-dashboard-worker.jar app.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

USER appuser
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]