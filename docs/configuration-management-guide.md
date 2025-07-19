# MCP Configuration Management Guide

## Overview

The MCP (Microservices Communication Platform) uses Spring Cloud Config Server for centralized configuration management. This guide covers how to use, manage, and troubleshoot the configuration system.

## Table of Contents

1. [Architecture](#architecture)
2. [Getting Started](#getting-started)
3. [Configuration Structure](#configuration-structure)
4. [Working with Configurations](#working-with-configurations)
5. [Security and Encryption](#security-and-encryption)
6. [Dynamic Configuration Updates](#dynamic-configuration-updates)
7. [Best Practices](#best-practices)
8. [Troubleshooting](#troubleshooting)

## Architecture

### Components

- **Config Server**: Centralized server that serves configuration to all microservices
- **Config Repository**: Git repository containing all configuration files
- **Spring Cloud Bus**: Enables dynamic configuration refresh across services
- **RabbitMQ**: Message broker for configuration change events

### How It Works

1. Configuration files are stored in a Git repository
2. Config Server pulls configurations from the Git repository
3. Microservices fetch their configuration from Config Server on startup
4. Changes to configuration can be propagated dynamically via Spring Cloud Bus

## Getting Started

### Prerequisites

- Java 21
- Docker and Docker Compose
- Git
- Access to configuration repository

### Quick Start

1. **Start Config Server**:
   ```bash
   cd infrastructure/docker-compose
   docker-compose up -d rabbitmq mcp-config-server
   ```

2. **Verify Config Server**:
   ```bash
   curl http://localhost:8888/actuator/health
   ```

3. **Fetch Configuration**:
   ```bash
   # Get configuration for a specific service
   curl http://localhost:8888/mcp-organization/development
   ```

## Configuration Structure

### Repository Layout

```
config-repo/
├── application.yml                    # Global defaults
├── application-{profile}.yml         # Profile-specific globals
├── {service-name}.yml                # Service-specific defaults
├── {service-name}-{profile}.yml      # Service and profile specific
└── shared/                           # Shared configurations
    ├── database.yml
    ├── security.yml
    └── monitoring.yml
```

### Configuration Hierarchy

Configurations are loaded in the following order (later overrides earlier):

1. `application.yml` - Global defaults
2. `application-{profile}.yml` - Profile-specific globals
3. `{service-name}.yml` - Service defaults
4. `{service-name}-{profile}.yml` - Service and profile specific

### Example Configuration Files

**application.yml** (Global defaults):
```yaml
spring:
  jackson:
    default-property-inclusion: non_null
    time-zone: UTC

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus

logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

**mcp-organization.yml** (Service-specific):
```yaml
server:
  port: ${SERVER_PORT:5005}

spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/organization_db}
    username: ${DB_USER:orguser}
    password: ${DB_PASSWORD:{cipher}encrypted_password_here}

mcp:
  organization:
    cache:
      ttl: ${CACHE_TTL:3600}
    features:
      multi-tenant: ${ENABLE_MULTI_TENANT:false}
```

## Working with Configurations

### Adding a New Service

1. Create service configuration file:
   ```yaml
   # config-repo/mcp-newservice.yml
   server:
     port: ${SERVER_PORT:8080}
   
   spring:
     application:
       name: mcp-newservice
   ```

2. Add profile-specific configurations:
   ```yaml
   # config-repo/mcp-newservice-development.yml
   logging:
     level:
       com.zamaz: DEBUG
   ```

3. Configure service to use Config Server:
   ```yaml
   # mcp-newservice/src/main/resources/bootstrap.yml
   spring:
     application:
       name: mcp-newservice
     cloud:
       config:
         uri: ${CONFIG_SERVER_URI:http://localhost:8888}
         fail-fast: true
         retry:
           max-attempts: 5
   ```

### Environment Variables

Use environment variables for environment-specific values:

```yaml
database:
  host: ${DB_HOST:localhost}
  port: ${DB_PORT:5432}
  name: ${DB_NAME:mydb}
```

### Property Resolution

Properties are resolved in this order:
1. System properties
2. Environment variables
3. Configuration files (in hierarchy order)
4. Default values in `${VARIABLE:default}`

## Security and Encryption

### Encrypting Sensitive Values

1. **Generate encryption key**:
   ```bash
   export CONFIG_ENCRYPTION_KEY=$(openssl rand -base64 32)
   ```

2. **Encrypt a value**:
   ```bash
   curl -X POST http://localhost:8888/encrypt -d "mysecretpassword"
   # Returns: AQBc2Fv3Rz9...
   ```

3. **Use in configuration**:
   ```yaml
   spring:
     datasource:
       password: {cipher}AQBc2Fv3Rz9...
   ```

### Security Best Practices

- **Never commit plain text secrets**
- Always use `{cipher}` prefix for encrypted values
- Store encryption keys securely (not in Git)
- Use different encryption keys per environment
- Rotate credentials regularly

### Example: Encrypting Database Password

```bash
# Encrypt the password
ENCRYPTED=$(curl -X POST http://localhost:8888/encrypt -d "myDatabasePassword")

# Update configuration
echo "spring.datasource.password: {cipher}$ENCRYPTED" >> config-repo/mcp-organization.yml

# Commit and push
cd config-repo
git add mcp-organization.yml
git commit -m "Update encrypted database password"
git push
```

## Dynamic Configuration Updates

### Manual Refresh

Refresh a single service:
```bash
curl -X POST http://service-host:port/actuator/refresh
```

### Bus Refresh

Refresh all services:
```bash
curl -X POST http://localhost:8888/actuator/bus-refresh
```

Refresh specific services:
```bash
curl -X POST http://localhost:8888/actuator/bus-refresh/mcp-organization:*
```

### Webhook Integration

Configure Git webhook for automatic refresh:

1. In GitHub/GitLab, add webhook:
   - URL: `http://config-server:8888/monitor`
   - Content-Type: `application/json`
   - Events: Push events

2. Config Server will automatically trigger refresh on push

### @RefreshScope Usage

Mark beans that should be refreshed:

```java
@Component
@RefreshScope
public class DynamicConfig {
    @Value("${feature.flag:false}")
    private boolean featureEnabled;
    
    public boolean isFeatureEnabled() {
        return featureEnabled;
    }
}
```

## Best Practices

### 1. Configuration Organization

- Use meaningful property names
- Group related properties
- Keep configurations DRY (Don't Repeat Yourself)
- Use shared configurations for common settings

### 2. Security

- Encrypt all sensitive data
- Use environment variables for secrets
- Never hardcode credentials
- Implement proper access controls

### 3. Versioning

- Use semantic versioning for configuration changes
- Document all changes in commit messages
- Tag stable configuration versions
- Maintain backward compatibility

### 4. Testing

- Test configurations in all environments
- Validate configuration changes before deployment
- Use configuration profiles for testing
- Monitor configuration loading metrics

### 5. Documentation

- Document all custom properties
- Maintain property descriptions
- Include examples with placeholders
- Keep README files updated

## Troubleshooting

### Common Issues

#### 1. Service Cannot Connect to Config Server

**Symptoms**:
- Service fails to start
- "Could not locate PropertySource" error

**Solutions**:
- Verify Config Server is running: `curl http://localhost:8888/actuator/health`
- Check network connectivity
- Verify `CONFIG_SERVER_URI` environment variable
- Check service logs for connection errors

#### 2. Configuration Not Loading

**Symptoms**:
- Service using default values
- Properties not found

**Solutions**:
- Verify configuration file naming: `{application-name}-{profile}.yml`
- Check Git repository synchronization
- Validate YAML syntax
- Check Config Server logs

#### 3. Encrypted Values Not Decrypting

**Symptoms**:
- `{cipher}` text appearing in properties
- "Cannot decrypt" errors

**Solutions**:
- Verify encryption key is set: `echo $CONFIG_ENCRYPTION_KEY`
- Check key matches between encryption and decryption
- Ensure `{cipher}` prefix is present
- Validate encrypted value format

#### 4. Configuration Not Refreshing

**Symptoms**:
- Changes not reflected after refresh
- Old values still in use

**Solutions**:
- Verify `@RefreshScope` annotation on beans
- Check Spring Cloud Bus connectivity
- Validate RabbitMQ is running
- Review refresh endpoint response

### Debug Commands

```bash
# Check what configuration Config Server is serving
curl http://localhost:8888/mcp-organization/development | jq .

# View Config Server environment
curl http://localhost:8888/actuator/env | jq .

# Check bus refresh status
curl http://localhost:8888/actuator/bus-refresh

# View service configuration
curl http://service:port/actuator/configprops | jq .
```

### Logging Configuration

Enable debug logging for troubleshooting:

```yaml
logging:
  level:
    org.springframework.cloud.config: DEBUG
    org.springframework.cloud.bus: DEBUG
    org.springframework.boot.env: DEBUG
```

## Migration Checklist

When migrating a service to use Config Server:

- [ ] Move configuration to Git repository
- [ ] Encrypt sensitive values
- [ ] Add Config Client dependency
- [ ] Create bootstrap.yml
- [ ] Update deployment scripts
- [ ] Test in all environments
- [ ] Update documentation
- [ ] Remove local configuration files

## Additional Resources

- [Spring Cloud Config Documentation](https://spring.io/projects/spring-cloud-config)
- [Spring Cloud Bus Documentation](https://spring.io/projects/spring-cloud-bus)
- [Configuration Properties Reference](./configuration-properties-reference.md)
- [Security Best Practices](./security-review-checklist.md)