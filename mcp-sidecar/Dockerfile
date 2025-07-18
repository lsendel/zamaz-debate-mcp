# Multi-stage Dockerfile for MCP Sidecar
FROM openjdk:21-jdk-slim as builder

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy Maven files
COPY pom.xml ./
COPY ../mcp-common/pom.xml ./mcp-common/

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src
COPY ../mcp-common/src ./mcp-common/src

# Build application
RUN mvn clean package -DskipTests

# Production stage
FROM openjdk:21-jre-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -r sidecar && useradd -r -g sidecar sidecar

# Set working directory
WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership
RUN chown sidecar:sidecar app.jar

# Switch to non-root user
USER sidecar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Environment variables
ENV SPRING_PROFILES_ACTIVE=production
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseStringDeduplication"

# Start application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]