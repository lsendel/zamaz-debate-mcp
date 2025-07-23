FROM maven:3.9-eclipse-temurin-21-alpine AS builder

# Set working directory
WORKDIR /build

# Copy all source code and build configuration
COPY pom.xml .
COPY mcp-common/ mcp-common/
COPY mcp-security/ mcp-security/
COPY mcp-docs/ mcp-docs/

# Build the specific module only
RUN mvn clean package -pl mcp-docs -am -DskipTests

# Security: Verify JAR file exists
RUN test -f mcp-docs/target/mcp-docs-*.jar || (echo "JAR file not found" && exit 1)

# Second stage: Runtime
FROM eclipse-temurin:21-jre-alpine

# Add metadata labels
LABEL org.opencontainers.image.title="mcp-docs" \
      org.opencontainers.image.description="Zamaz MCP Documentation Service" \
      org.opencontainers.image.vendor="Zamaz" \
      org.opencontainers.image.licenses="Proprietary"

# Set working directory
WORKDIR /app

# Install curl for health checks and security updates
RUN apk add --no-cache curl tzdata && \
    apk upgrade --no-cache

# Create non-root user
RUN addgroup -g 1000 spring && \
    adduser -u 1000 -G spring -s /bin/sh -D spring

# Copy JAR from builder
COPY --from=builder /build/mcp-docs/target/mcp-docs-*.jar app.jar

# Create config directory with proper permissions
RUN mkdir -p /app/config /app/logs && \
    chown -R spring:spring /app

# Switch to non-root user
USER spring

# Expose port
EXPOSE 5007

# Set healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:5007/actuator/health || exit 1

# Set entrypoint with JVM tuning
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]