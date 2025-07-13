# Build stage
FROM maven:3.8.4-openjdk-17 AS build
COPY src /app/src
COPY pom.xml /app
RUN mvn -f /app/pom.xml clean package

# Package stage
FROM openjdk:17-jdk-slim
COPY --from=build /app/target/mcp-template-j-1.0-SNAPSHOT.jar /app/mcp-template-j.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/mcp-template-j.jar"]
