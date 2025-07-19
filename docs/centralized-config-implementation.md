# Centralized Configuration Implementation

## Overview

All MCP services have been migrated to use Spring Cloud Config Server for centralized configuration management. This document describes the implementation details and usage instructions.

## What Was Done

### 1. Added Config Client Dependencies

All Spring Boot services now include the Spring Cloud Config Client dependency:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

Services updated:
- mcp-gateway
- mcp-auth-server  
- mcp-sidecar
- mcp-organization
- mcp-llm
- mcp-rag
- mcp-debate-engine
- mcp-controller
- mcp-context
- mcp-pattern-recognition
- github-integration
- mcp-modulith
- mcp-template
- mcp-docs
- mcp-context-client
- mcp-debate

### 2. Created Bootstrap Configuration

Each service now has a `bootstrap.yml` file that configures the connection to Config Server:

```yaml
spring:
  application:
    name: ${service-name}
  cloud:
    config:
      uri: ${CONFIG_SERVER_URI:http://localhost:8888}
      fail-fast: true
      retry:
        initial-interval: 1000
        max-attempts: 5
        max-interval: 2000
        multiplier: 1.1
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:development}
```

### 3. Migrated Configurations to Config Repository

All service configurations have been moved to the `config-repo/` directory with the following structure:

```
config-repo/
├── application.yml                          # Global defaults
├── application-{profile}.yml               # Profile-specific globals
├── {service-name}.yml                      # Service defaults
├── {service-name}-{profile}.yml           # Service + profile specific
└── shared/                                 # Shared configurations
    ├── database-common.yml
    ├── logging-common.yml
    ├── monitoring-common.yml
    └── security-common.yml
```

### 4. Implemented Security

- Sensitive values use placeholders: `${VARIABLE_NAME}`
- Passwords and secrets can be encrypted: `{cipher}encrypted_value`
- Encryption key management via environment variables
- Pre-commit hooks for security scanning

### 5. Docker Integration

Created `docker-compose.config.yml` override file that adds Config Server integration to all services:

```yaml
environment:
  - CONFIG_SERVER_URI=http://mcp-config-server:8888
  - SPRING_PROFILES_ACTIVE=docker
depends_on:
  mcp-config-server:
    condition: service_healthy
```

## Usage Instructions

### Starting Config Server

```bash
# Using Docker Compose
cd infrastructure/docker-compose
docker-compose up -d rabbitmq mcp-config-server

# Verify it's running
curl http://localhost:8888/actuator/health
```

### Encrypting Values

```bash
# Encrypt a value
curl -X POST http://localhost:8888/encrypt -d "mysecret"

# Use in configuration
database:
  password: {cipher}AQBc2Fv3Rz9...

# Decrypt for verification
curl -X POST http://localhost:8888/decrypt -d "{cipher}AQBc2Fv3Rz9..."
```

### Testing Configuration

```bash
# Test all services
./scripts/test-config-loading.sh

# View specific configuration
curl http://localhost:8888/mcp-organization/development

# Get configuration as properties
curl http://localhost:8888/mcp-organization-development.properties
```

### Running Services with Config Server

```bash
# Option 1: Use the override file
cd infrastructure/docker-compose
docker-compose -f docker-compose.yml -f docker-compose.config.yml up -d

# Option 2: Set environment variables
export CONFIG_SERVER_URI=http://localhost:8888
export SPRING_PROFILES_ACTIVE=development
java -jar service.jar
```

### Dynamic Configuration Updates

```bash
# Refresh all services
curl -X POST http://localhost:8888/actuator/bus-refresh

# Refresh specific service
curl -X POST http://localhost:8888/actuator/bus-refresh/mcp-organization:**

# Refresh individual service instance
curl -X POST http://service-host:port/actuator/refresh
```

## Configuration Hierarchy

Properties are resolved in this order (later overrides earlier):

1. `application.yml` - Global defaults
2. `application-{profile}.yml` - Global profile-specific
3. `{service}.yml` - Service defaults  
4. `{service}-{profile}.yml` - Service profile-specific
5. Environment variables
6. Command line arguments

## Environment-Specific Configurations

### Development
- Debug logging enabled
- Local database connections
- Relaxed security settings
- All actuator endpoints exposed

### Staging
- Info level logging
- Staging database connections
- Standard security settings
- Limited actuator endpoints

### Production
- Warn level logging
- Production database connections
- Strict security settings
- Minimal actuator endpoints

## Security Best Practices

1. **Never commit plaintext secrets**
   - Use `{cipher}` for encrypted values
   - Use `${ENV_VAR}` for environment variables

2. **Encryption key management**
   - Store keys in secure vault
   - Rotate keys regularly
   - Different keys per environment

3. **Access control**
   - Config Server uses basic auth
   - Network policies restrict access
   - Audit all configuration changes

4. **Git security**
   - Private repository for configurations
   - Branch protection rules
   - Signed commits

## Troubleshooting

### Service Can't Connect to Config Server

```bash
# Check Config Server is running
curl http://localhost:8888/actuator/health

# Check network connectivity
docker exec <service> ping mcp-config-server

# Check bootstrap.yml exists
ls src/main/resources/bootstrap.yml
```

### Configuration Not Loading

```bash
# Verify Config Server has the configuration
curl http://localhost:8888/<service-name>/default

# Check service logs
docker logs <service-name>

# Verify CONFIG_SERVER_URI is set
docker exec <service> env | grep CONFIG
```

### Encrypted Values Not Decrypting

```bash
# Verify encryption key is set
echo $CONFIG_ENCRYPTION_KEY

# Test encryption/decryption
curl -X POST http://localhost:8888/encrypt -d "test"
curl -X POST http://localhost:8888/decrypt -d "{cipher}..."
```

## Migration Scripts

Several scripts have been created to facilitate the migration:

- `scripts/migrate-to-config-server.sh` - Automated migration script
- `scripts/encrypt-config-values.sh` - Encrypt sensitive values
- `scripts/update-docker-compose.sh` - Update Docker configurations
- `scripts/test-config-loading.sh` - Test configuration loading
- `scripts/security-scan.sh` - Scan for security issues

## Next Steps

1. **Production deployment**
   - Set up production Config Server cluster
   - Configure proper Git repository
   - Implement key management solution

2. **Monitoring**
   - Set up alerts for Config Server availability
   - Monitor configuration refresh events
   - Track configuration access patterns

3. **Advanced features**
   - Implement configuration versioning
   - Add webhook for automatic refresh
   - Integrate with secret management tools

## Benefits Achieved

1. **Centralized management** - All configurations in one place
2. **Environment consistency** - Same configuration structure across environments
3. **Security** - Encrypted sensitive values
4. **Dynamic updates** - Change configurations without restart
5. **Version control** - Full history of configuration changes
6. **Simplified deployment** - No need to rebuild images for config changes

## References

- [Spring Cloud Config Documentation](https://spring.io/projects/spring-cloud-config)
- [Configuration Management Guide](./configuration-management-guide.md)
- [Migration Guide](./configuration-migration-guide.md)
- [Troubleshooting Guide](./configuration-troubleshooting-guide.md)