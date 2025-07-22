# Multi-stage build for MCP Config Server
FROM maven:3.9-eclipse-temurin-21 AS builder

# Set working directory
WORKDIR /app

# Copy parent POM
COPY pom.xml /app/
COPY mcp-config-server/pom.xml /app/mcp-config-server/

# Download dependencies
RUN mvn -B -f mcp-config-server/pom.xml dependency:go-offline

# Copy source code
COPY mcp-config-server/src /app/mcp-config-server/src

# Build the application
RUN mvn -B -f mcp-config-server/pom.xml clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Install necessary packages
RUN apk add --no-cache \
    curl \
    jq \
    bash \
    git \
    openssh-client \
    && rm -rf /var/cache/apk/*

# Create non-root user
RUN addgroup -g 1000 -S mcp && \
    adduser -u 1000 -S mcp -G mcp

# Set working directory
WORKDIR /app

# Copy JAR from builder
COPY --from=builder /app/mcp-config-server/target/mcp-config-server-*.jar app.jar

# Create directories for Git config and SSH keys
RUN mkdir -p /app/config-repo /app/.ssh && \
    chown -R mcp:mcp /app

# Copy entrypoint script
COPY mcp-config-server/docker-entrypoint.sh /app/
RUN chmod +x /app/docker-entrypoint.sh

# Switch to non-root user
USER mcp

# Expose port
HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost/ || exit 1
EXPOSE 8888
HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost:8080/health || exit 1

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
USER node

  CMD curl -f http://localhost:8080/health || exit 1

    CMD curl -f http://localhost:8888/actuator/health || exit 1

# Set environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m" \
    SERVER_PORT=8888 \
    SPRING_PROFILES_ACTIVE=docker

# Run the application
ENTRYPOINT ["/app/docker-entrypoint.sh"]