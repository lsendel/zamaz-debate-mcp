FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy only necessary files for build
COPY pom.xml .
COPY mcp-common/ mcp-common/
COPY mcp-template/pom.xml mcp-template/
COPY mcp-template/src/ mcp-template/src/

# Build the specific module and its dependencies
RUN mvn clean package -pl mcp-template -am -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -g 1000 spring && \
    adduser -u 1000 -G spring -s /bin/sh -D spring

# Copy JAR from builder
COPY --from=builder /build/mcp-template/target/mcp-template-*.jar app.jar

# Change ownership
RUN chown -R spring:spring /app

USER spring

HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost/ || exit 1
EXPOSE 5006
HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost:8080/health || exit 1

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
USER node

  CMD curl -f http://localhost:8080/health || exit 1

  CMD curl -f http://localhost:5006/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
