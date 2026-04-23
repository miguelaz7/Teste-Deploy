# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml e efetuar download de dependências
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy código source e recursos
COPY _src ./_src
COPY resources ./resources

# Build da aplicação
RUN mvn clean package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy jar da build anterior
COPY --from=builder /app/target/ticketub-0.0.1-SNAPSHOT.jar app.jar

# Expor porta
EXPOSE 8080

# Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Correr aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]



