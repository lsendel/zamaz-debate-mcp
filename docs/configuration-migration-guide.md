# Configuration Migration Guide

This guide provides step-by-step instructions for migrating MCP services from local configuration files to centralized configuration management using Spring Cloud Config Server.

## Table of Contents

1. [Pre-Migration Checklist](#pre-migration-checklist)
2. [Migration Steps](#migration-steps)
3. [Service-Specific Instructions](#service-specific-instructions)
4. [Validation and Testing](#validation-and-testing)
5. [Rollback Procedures](#rollback-procedures)
6. [Post-Migration Tasks](#post-migration-tasks)

## Pre-Migration Checklist

Before starting the migration:

- [ ] Config Server is deployed and running
- [ ] Configuration Git repository is accessible
- [ ] Encryption key is available
- [ ] All environments are identified (dev, staging, prod)
- [ ] Current configurations are backed up
- [ ] Team is notified of migration schedule

## Migration Steps

### Step 1: Analyze Current Configuration

1. **Inventory existing properties**:
   ```bash
   # List all configuration files
   find . -name "application*.yml" -o -name "application*.properties"
   ```

2. **Identify sensitive values**:
   ```bash
   # Search for potential secrets
   grep -r "password\|secret\|key\|token" --include="*.yml" --include="*.properties"
   ```

3. **Document environment-specific values**:
   - Database connections
   - API endpoints
   - Feature flags
   - Resource limits

### Step 2: Prepare Configuration Repository

1. **Create base configuration**:
   ```yaml
   # config-repo/application.yml
   spring:
     jackson:
       default-property-inclusion: non_null
   
   management:
     endpoints:
       web:
         exposure:
           include: health,info,metrics,prometheus
   ```

2. **Create service configuration**:
   ```yaml
   # config-repo/mcp-{service-name}.yml
   # Copy from existing application.yml
   # Replace hardcoded values with placeholders
   ```

3. **Encrypt sensitive values**:
   ```bash
   # For each sensitive value
   ENCRYPTED=$(curl -X POST http://localhost:8888/encrypt -d "actual-secret-value")
   # Update configuration with {cipher}prefix
   ```

### Step 3: Update Service Dependencies

1. **Add Config Client dependency**:
   ```xml
   <dependency>
       <groupId>org.springframework.cloud</groupId>
       <artifactId>spring-cloud-starter-config</artifactId>
   </dependency>
   ```

2. **Add Spring Cloud Bus (optional)**:
   ```xml
   <dependency>
       <groupId>org.springframework.cloud</groupId>
       <artifactId>spring-cloud-starter-bus-amqp</artifactId>
   </dependency>
   ```

### Step 4: Configure Bootstrap Properties

1. **Create bootstrap.yml**:
   ```yaml
   # src/main/resources/bootstrap.yml
   spring:
     application:
       name: mcp-{service-name}
     cloud:
       config:
         uri: ${CONFIG_SERVER_URI:http://localhost:8888}
         fail-fast: true
         retry:
           initial-interval: 1000
           max-attempts: 5
           max-interval: 2000
           multiplier: 1.1
   ```

2. **Configure profiles**:
   ```yaml
   spring:
     profiles:
       active: ${SPRING_PROFILES_ACTIVE:development}
   ```

### Step 5: Update Deployment Configuration

1. **Docker Compose**:
   ```yaml
   services:
     mcp-service:
       environment:
         - CONFIG_SERVER_URI=http://mcp-config-server:8888
         - SPRING_PROFILES_ACTIVE=docker
       depends_on:
         mcp-config-server:
           condition: service_healthy
   ```

2. **Kubernetes**:
   ```yaml
   env:
   - name: CONFIG_SERVER_URI
     valueFrom:
       configMapKeyRef:
         name: service-config
         key: config.server.uri
   - name: SPRING_PROFILES_ACTIVE
     value: kubernetes
   ```

### Step 6: Remove Local Configuration

1. **Move to backup**:
   ```bash
   mkdir -p backup/config
   mv src/main/resources/application*.yml backup/config/
   ```

2. **Keep only bootstrap.yml** and logging configuration

## Service-Specific Instructions

### MCP-Organization Service

1. **Special considerations**:
   - JWT secret must be shared across instances
   - Database migration scripts remain local
   - CORS settings need environment-specific values

2. **Configuration example**:
   ```yaml
   # config-repo/mcp-organization.yml
   mcp:
     organization:
       jwt:
         secret: {cipher}encrypted_jwt_secret
         expiration: ${JWT_EXPIRATION:3600000}
       cors:
         allowed-origins: ${CORS_ORIGINS:http://localhost:3000}
   ```

### MCP-LLM Service

1. **Special considerations**:
   - API keys for different providers
   - Model-specific configurations
   - Rate limiting settings

2. **Configuration example**:
   ```yaml
   # config-repo/mcp-llm.yml
   mcp:
     llm:
       providers:
         openai:
           api-key: {cipher}encrypted_openai_key
           model: ${OPENAI_MODEL:gpt-4}
         anthropic:
           api-key: {cipher}encrypted_anthropic_key
           model: ${ANTHROPIC_MODEL:claude-3}
   ```

### MCP-Controller Service

1. **Special considerations**:
   - Service discovery endpoints
   - Circuit breaker configurations
   - Retry policies

2. **Configuration example**:
   ```yaml
   # config-repo/mcp-controller.yml
   mcp:
     controller:
       services:
         organization-url: ${ORGANIZATION_SERVICE_URL:http://mcp-organization:5005}
         llm-url: ${LLM_SERVICE_URL:http://mcp-llm:5002}
       resilience:
         circuit-breaker:
           failure-threshold: ${CB_FAILURE_THRESHOLD:50}
   ```

## Validation and Testing

### 1. Local Testing

```bash
# Start Config Server locally
cd mcp-config-server
./mvnw spring-boot:run

# Test configuration retrieval
curl http://localhost:8888/mcp-organization/development

# Start service with Config Server
cd mcp-organization
export CONFIG_SERVER_URI=http://localhost:8888
./mvnw spring-boot:run
```

### 2. Integration Testing

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "spring.config.import=optional:configserver:"
})
class ConfigurationIntegrationTest {
    // Test with local configuration
}
```

### 3. Smoke Tests

- [ ] Service starts successfully
- [ ] All required properties are loaded
- [ ] Encrypted values are decrypted
- [ ] Database connections work
- [ ] External API connections work
- [ ] Health checks pass

### 4. Configuration Refresh Test

```bash
# Make a configuration change
cd config-repo
echo "test.property: new-value" >> mcp-organization-development.yml
git add . && git commit -m "Test configuration update" && git push

# Trigger refresh
curl -X POST http://localhost:8888/actuator/bus-refresh

# Verify new value
curl http://localhost:5005/actuator/env | grep "test.property"
```

## Rollback Procedures

### Quick Rollback

1. **Re-enable local configuration**:
   ```yaml
   # bootstrap.yml
   spring:
     cloud:
       config:
         enabled: false
   ```

2. **Restore local files**:
   ```bash
   cp backup/config/application*.yml src/main/resources/
   ```

3. **Restart service**

### Gradual Rollback

1. **Override with local properties**:
   ```yaml
   # application-local.yml
   spring:
     config:
       import: optional:configserver:
   # Add local overrides here
   ```

2. **Run with local profile**:
   ```bash
   export SPRING_PROFILES_ACTIVE=development,local
   ```

## Post-Migration Tasks

### 1. Documentation Updates

- [ ] Update README files
- [ ] Update deployment documentation
- [ ] Document new environment variables
- [ ] Update troubleshooting guides

### 2. Monitoring Setup

- [ ] Configure alerts for Config Server availability
- [ ] Set up configuration change notifications
- [ ] Monitor configuration refresh metrics
- [ ] Track configuration access patterns

### 3. Team Training

- [ ] Conduct training sessions
- [ ] Share configuration management best practices
- [ ] Document common tasks
- [ ] Set up configuration change process

### 4. Cleanup

- [ ] Remove old configuration files from repositories
- [ ] Archive backup configurations
- [ ] Update CI/CD pipelines
- [ ] Remove unused dependencies

## Migration Timeline Example

### Week 1: Preparation
- Day 1-2: Deploy Config Server
- Day 3-4: Prepare configuration repository
- Day 5: Team training

### Week 2: Development Environment
- Day 1-2: Migrate mcp-organization
- Day 3-4: Migrate mcp-llm and mcp-controller
- Day 5: Testing and validation

### Week 3: Staging Environment
- Day 1-2: Deploy to staging
- Day 3-4: Full integration testing
- Day 5: Performance testing

### Week 4: Production
- Day 1: Production deployment (off-hours)
- Day 2-3: Monitoring and validation
- Day 4-5: Documentation and cleanup

## Troubleshooting Migration Issues

### Service Won't Start

1. Check Config Server connectivity:
   ```bash
   curl http://config-server:8888/actuator/health
   ```

2. Verify bootstrap configuration:
   ```bash
   cat src/main/resources/bootstrap.yml
   ```

3. Check logs for specific errors:
   ```bash
   docker logs mcp-service | grep "config"
   ```

### Missing Properties

1. Verify property exists in Config Server:
   ```bash
   curl http://config-server:8888/service-name/profile | jq '.propertySources[].source'
   ```

2. Check property name spelling and case

3. Verify profile is active:
   ```bash
   curl http://service:port/actuator/env | jq '.activeProfiles'
   ```

### Performance Issues

1. Enable Config Server caching:
   ```yaml
   spring:
     cloud:
       config:
         server:
           git:
             clone-on-start: true
   ```

2. Optimize retry settings:
   ```yaml
   spring:
     cloud:
       config:
         retry:
           max-attempts: 3
           initial-interval: 500
   ```

## Success Criteria

Migration is considered successful when:

- [ ] All services use Config Server in all environments
- [ ] No local configuration files (except bootstrap.yml)
- [ ] All sensitive data is encrypted
- [ ] Configuration changes can be applied without restart
- [ ] Monitoring and alerting is in place
- [ ] Team is trained on new procedures
- [ ] Documentation is complete and accurate