FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy only necessary files for build
COPY pom.xml .
COPY mcp-common/pom.xml mcp-common/
COPY mcp-controller/pom.xml mcp-controller/
COPY mcp-controller/src/ mcp-controller/src/

# Build the specific module and its dependencies
RUN mvn clean package -pl mcp-controller -am -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -g 1000 spring && \
    adduser -u 1000 -G spring -s /bin/sh -D spring

# Copy JAR from builder
COPY --from=builder /build/mcp-controller/target/mcp-controller-*.jar app.jar

# Change ownership
RUN chown -R spring:spring /app

USER spring

EXPOSE 5013

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:5013/actuator/health || exit 1

ENTRYPOINT ["runuser", "-u", "spring", "--", "java", "-jar", "app.jar"]