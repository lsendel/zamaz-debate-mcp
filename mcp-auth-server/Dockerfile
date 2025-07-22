FROM openjdk:21-jre-slim

# Set working directory
WORKDIR /app

# Create non-root user
RUN groupadd -r mcpauth && useradd -r -g mcpauth mcpauth

# Copy the JAR file
COPY target/mcp-auth-server-*.jar app.jar

# Change ownership
RUN chown mcpauth:mcpauth app.jar

# Switch to non-root user
USER mcpauth

# Expose port
HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost/ || exit 1
EXPOSE 9000

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:9000/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]