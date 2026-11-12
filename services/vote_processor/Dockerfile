# ── Etapa 1: Compilación ───────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Copia solo el POM primero para aprovechar la caché de capas de dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia el código fuente y empaqueta
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Etapa 2: Ejecución ligera ──────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

# Parámetros ergonómicos de la JVM adaptados a contenedores con cgroups
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Principio de menor privilegio: usuario no root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /build/target/vote-processor.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
