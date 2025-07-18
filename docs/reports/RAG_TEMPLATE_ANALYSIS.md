# MCP RAG and Template Services Analysis

## Issue Summary
The MCP RAG (port 5004) and Template (port 5006) services are not running due to multiple issues:

## 1. Port Conflicts and Container Naming Issues
- There are orphan containers with `-j` suffix (e.g., `mcp-organization-j-1`) blocking ports
- The RAG service is mapped to port 5018 instead of 5004 in some configurations
- Port 5005 is already allocated by the `-j` version of the organization service

## 2. Missing Application Configuration Files
Both services were missing their `application.yml` files:
- `/mcp-rag/src/main/resources/application.yml` - **NOW CREATED**
- `/mcp-template/src/main/resources/application.yml` - **NOW CREATED**

## 3. Incomplete Implementation
### RAG Service
- Only has `McpRagJApplication.java` main class
- Missing actual service implementation (controllers, services, repositories)
- No MCP endpoint implementations
- No vector database integration code

### Template Service
- Has basic implementation files but in wrong package structure:
  - Application class: `com.mcp.template.McpTemplateJApplication`
  - But pom.xml expects: `com.zamaz.mcp.template`
- Missing MCP-specific endpoints

## 4. Docker Build Issues
### Dockerfile JAR Name Mismatch
- RAG Dockerfile expects: `*.jar` (generic)
- Template Dockerfile expects: `mcp-template-j-1.0-SNAPSHOT.jar`
- Actual output would be: `mcp-rag-1.0.0.jar` and `mcp-template-1.0.0.jar`

### Java Version Conflict
- Project configured for Java 21
- Local environment has Java 24
- This causes Maven compilation failures

## 5. Database Schema Issues
- Template service expects `template_db` database
- RAG service expects `rag_db` database (per configuration)
- But init scripts only create `organization_db` and `debate_db`

## Immediate Steps to Fix

### 1. Clean up Docker environment
```bash
docker-compose down --remove-orphans
docker system prune -f
```

### 2. Fix Database Initialization
Add to `scripts/init-scripts/01-create-databases.sql`:
```sql
CREATE DATABASE IF NOT EXISTS rag_db;
CREATE DATABASE IF NOT EXISTS template_db;
GRANT ALL PRIVILEGES ON DATABASE rag_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE template_db TO postgres;
```

### 3. Fix Dockerfiles
Update RAG Dockerfile:
```dockerfile
# Runtime stage
FROM eclipse-temurin:21-jre-alpine  # Changed from 17
WORKDIR /app
COPY --from=builder /app/target/mcp-rag-*.jar app.jar  # More specific pattern
```

Update Template Dockerfile:
```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build  # Changed from 3.8.4-openjdk-17
# ... rest of the file
COPY --from=build /app/target/mcp-template-*.jar /app/mcp-template.jar
```

### 4. Fix Package Structure
For Template service, either:
- Move all classes from `com.mcp.template` to `com.zamaz.mcp.template`
- OR update pom.xml to use `com.mcp.template` as base package

### 5. Implement Missing Components
Both services need:
- MCP endpoint controllers (`McpEndpointController.java`)
- Health check endpoints
- Basic service implementations

## Root Cause
These services appear to be incomplete implementations that were started but not finished. They need substantial development work to match the functionality of the working services (mcp-llm, mcp-controller, mcp-organization).

## Recommendation
1. Focus on getting the existing services working properly first
2. Complete the RAG and Template services implementation later
3. For now, exclude them from docker-compose startup or mark them with profiles

To exclude them temporarily, add to docker-compose.yml:
```yaml
mcp-rag:
  profiles:
    - full
    - rag
  # ... rest of config

mcp-template:
  profiles:
    - full
    - template
  # ... rest of config
```

Then they won't start by default unless you use:
```bash
docker-compose --profile full up -d
```
