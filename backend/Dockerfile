# ══════════════════════════════════════════════════════════════
# Dockerfile — Orivya Rice Backend (Spring Boot)
# Place at: backend/Dockerfile
# ══════════════════════════════════════════════════════════════

# ── STAGE 1: BUILD ────────────────────────────────────────────
# Use Maven + JDK 17 to build the JAR
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copy pom.xml first — Docker caches dependencies layer
# (Only re-downloads dependencies if pom.xml changes)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── STAGE 2: RUN ──────────────────────────────────────────────
# Use slim JRE-only image for smaller final image
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create uploads directory for product images
RUN mkdir -p uploads

# Copy the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Render sets PORT env var — Spring Boot reads ${PORT:8080}
EXPOSE 8080

# Health check — Render uses this to know backend is ready
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Run the Spring Boot app
ENTRYPOINT ["java", \
    "-Xmx256m", \
    "-Xms128m", \
    "-XX:+UseContainerSupport", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]