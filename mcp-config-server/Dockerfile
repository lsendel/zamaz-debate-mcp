FROM openjdk:21-jre-slim

# Set working directory
WORKDIR /app

# Copy the JAR file
COPY target/mcp-config-server-*.jar app.jar

# Expose the Config Server port
EXPOSE 8888

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8888/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]