# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy parent pom first
COPY pom.xml /app/parent-pom.xml
COPY mcp-common/pom.xml /app/mcp-common/pom.xml
COPY mcp-security/pom.xml /app/mcp-security/pom.xml
COPY mcp-context/pom.xml /app/mcp-context/pom.xml

# Download dependencies
WORKDIR /app
RUN mvn -f parent-pom.xml dependency:go-offline -pl mcp-context -am

# Copy source code
# Security: .dockerignore ensures only necessary files are copied
COPY mcp-common/src /app/mcp-common/src
COPY mcp-security/src /app/mcp-security/src
COPY mcp-context/src /app/mcp-context/src

# Build the application
WORKDIR /app
RUN mvn -f parent-pom.xml clean package -pl mcp-context -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -g 1000 spring && adduser -u 1000 -G spring -s /bin/sh -D spring

# Set working directory
WORKDIR /app

# Copy JAR from builder
COPY --from=builder /app/mcp-context/target/*.jar app.jar

# Change ownership
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose port
HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost/ || exit 1
EXPOSE 5007
HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost:8080/health || exit 1

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
USER node

  CMD curl -f http://localhost:8080/health || exit 1

    CMD curl -f http://localhost:5007/actuator/health || exit 1

# Set JVM options
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]