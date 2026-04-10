# ══════════════════════════════════════════════════
# Dockerfile — Orivya Rice Backend (Spring Boot)
# Multi-stage build: smaller final image
# ══════════════════════════════════════════════════

# ── STAGE 1: Build the JAR ──
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml first (Docker caches this layer if pom doesn't change)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── STAGE 2: Run the JAR ──
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create uploads directory for product images
RUN mkdir -p /app/uploads

# Copy the built JAR from stage 1
COPY --from=builder /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8080/api/products || exit 1

# Run the application
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=${SPRING_PROFILE:prod}", \
  "-jar", "app.jar"]
