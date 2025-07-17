# Multi-stage build for GitHub Integration Service
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy parent POM and common dependencies first for better caching
COPY pom.xml .
COPY github-integration/pom.xml ./github-integration/

# Download dependencies
RUN mvn dependency:go-offline -pl github-integration

# Copy source code
COPY github-integration/src ./github-integration/src

# Build the application
RUN mvn clean package -pl github-integration -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Create non-root user for security
RUN addgroup -g 1001 -S kiro && \
    adduser -u 1001 -S kiro -G kiro

WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Copy the built JAR
COPY --from=builder /app/github-integration/target/github-integration-*.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs && chown -R kiro:kiro /app

# Switch to non-root user
USER kiro

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]