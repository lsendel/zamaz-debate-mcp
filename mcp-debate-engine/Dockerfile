# Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy parent pom and dependencies
COPY pom.xml ./
COPY mcp-common/pom.xml ./mcp-common/
COPY mcp-security/pom.xml ./mcp-security/
COPY mcp-debate-engine/pom.xml ./mcp-debate-engine/

# Download dependencies
RUN mvn dependency:go-offline -B -pl mcp-debate-engine -am

# Copy source code
COPY mcp-common/src ./mcp-common/src
COPY mcp-security/src ./mcp-security/src
COPY mcp-debate-engine/src ./mcp-debate-engine/src

# Build the application
RUN mvn clean package -DskipTests -pl mcp-debate-engine -am

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add metadata labels
LABEL org.opencontainers.image.title="mcp-debate-engine" \
      org.opencontainers.image.description="Zamaz MCP Debate Engine Service" \
      org.opencontainers.image.vendor="Zamaz" \
      org.opencontainers.image.licenses="Proprietary"

# Install curl for health checks and security updates
RUN apk add --no-cache curl tzdata && \
    apk upgrade --no-cache

# Create non-root user
RUN addgroup -g 1000 -S appgroup && \
    adduser -u 1000 -S appuser -G appgroup

# Copy JAR from builder
COPY --from=builder /app/mcp-debate-engine/target/*.jar app.jar

# Create config directory with proper permissions
RUN mkdir -p /app/config /app/logs && \
    chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 5013

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:5013/actuator/health || exit 1

# JVM options for container environment
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -Djava.security.egd=file:/dev/./urandom"

# Run the application
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-XX:InitialRAMPercentage=50.0", "-XX:+UseG1GC", "-XX:+UseStringDeduplication", "-XX:+OptimizeStringConcat", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]