# ===================================================================
# Multi-stage Dockerfile for Automated Library Management System
# Production-oriented, secure non-root runtime container
# ===================================================================

# Stage 1: Build stage using Maven and Eclipse Temurin JDK 21
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Copy project descriptor and source files
COPY pom.xml .
COPY src ./src

# Build production JAR package
RUN mvn clean package -DskipTests -B

# Stage 2: Slim, hardened runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Install curl for health check probe
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Create dedicated non-root application user and group
RUN groupadd -r spring && useradd -r -g spring -u 1000 spring

# Copy executable Spring Boot JAR from build stage
COPY --from=builder /build/target/library-management-system-*.jar app.jar

# Enforce secure ownership
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose internal application port
EXPOSE 8080
ENV PORT=8080

# Container healthcheck using Spring Boot Actuator probe
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1

# Launch Spring Boot with container-aware JVM tuning
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
