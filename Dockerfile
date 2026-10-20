# Multi-stage build for kiosk-admin
# Stage 1: Build the application
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY mvnw.cmd .
COPY pom.xml .
COPY .mvn .mvn

# Download dependencies
RUN chmod +x mvnw && \
    ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Create non-root user for security
RUN useradd -m -u 1000 appuser

# Copy jar file from builder
COPY --from=builder /app/target/*.jar app.jar

# Change ownership to appuser
RUN chown appuser:appuser app.jar

# Switch to non-root user
USER appuser

# Expose port (default: 8090, can be overridden with SERVER_PORT - khac webview dung 8080)
EXPOSE 8090

# Health check (chi dung /actuator/health - xem application.yml, endpoint nay duoc expose rieng
# va KHONG qua JwtAuthFilter vi khong nam duoi /api/admin/*)
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:${SERVER_PORT:-8090}/actuator/health || exit 1

# Run the application with environment variable support
ENTRYPOINT ["java", "-jar", "app.jar"]