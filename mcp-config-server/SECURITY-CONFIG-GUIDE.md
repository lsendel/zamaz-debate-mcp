# Security Configuration Guide

This guide explains how to use the externalized security configuration system in the MCP Services platform.

## Overview

The MCP Config Server provides centralized, environment-specific security configuration with:
- Dynamic refresh without service restarts
- Integration with HashiCorp Vault and AWS Secrets Manager
- Configuration validation with minimum security requirements
- Environment-specific profiles (dev, staging, production)

## Configuration Structure

### Base Security Configuration

All security settings are prefixed with `security.` and organized into logical sections:

```yaml
security:
  jwt:
    issuer-uri: https://auth.example.com
    algorithm: RS256
    access-token-validity: 900
    
  cors:
    allowed-origins:
      - https://app.example.com
    allowed-methods: [GET, POST, PUT, DELETE]
    
  password-policy:
    min-length: 12
    require-uppercase: true
    breach-check-enabled: true
    
  # ... other sections
```

### Environment-Specific Overrides

Configuration files follow this naming pattern:
- `application-security.yml` - Base security configuration
- `application-security-dev.yml` - Development overrides
- `application-security-staging.yml` - Staging overrides
- `application-security-prod.yml` - Production overrides

## Secret Management

### Using HashiCorp Vault

1. Enable Vault profile:
```bash
export SPRING_PROFILES_ACTIVE=vault
export VAULT_HOST=vault.example.com
export VAULT_TOKEN=your-vault-token
```

2. Reference secrets in configuration:
```yaml
security:
  oauth2:
    client:
      client-secret: ${vault:secret/oauth2/client-secret}
```

### Using AWS Secrets Manager

1. Enable AWS profile:
```bash
export SPRING_PROFILES_ACTIVE=aws
export AWS_REGION=us-east-1
export AWS_SECRETS_PREFIX=/secret/mcp
```

2. Store secrets in AWS Secrets Manager:
```json
{
  "jwt.signing-key": "your-secret-key",
  "oauth2.client-secret": "your-client-secret"
}
```

## Dynamic Configuration Refresh

### Automatic Refresh

Services automatically check for configuration updates every 60 seconds (configurable).

### Manual Refresh

Trigger immediate refresh via REST API:

```bash
curl -X POST http://config-server:8888/security-config/refresh \
  -u config-admin:password
```

### Refresh Scope

Add `@RefreshScope` to beans that need dynamic updates:

```java
@Component
@RefreshScope
public class SecurityService {
    @Value("${security.jwt.access-token-validity}")
    private int tokenValidity;
}
```

## Configuration Validation

### Validation Rules

The Config Server validates security settings based on:
- Environment (dev, staging, production)
- Minimum security requirements
- Inter-property dependencies

### Validation Endpoint

Check configuration validity:

```bash
curl http://config-server:8888/security-config/validate \
  -u config-admin:password
```

### Enforcement Modes

Set validation enforcement in `application.yml`:

```yaml
validation:
  security:
    enforcement-mode: strict  # strict, warn, or disabled
```

- **strict**: Fail startup on validation errors
- **warn**: Log warnings but allow startup
- **disabled**: Skip validation

## Production Requirements

Production configurations must meet these requirements:

### JWT Security
- Algorithm: RS256, RS384, or RS512 (asymmetric only)
- Access token validity: ≤ 10 minutes
- Refresh token validity: ≤ 12 hours

### CORS
- Only HTTPS origins allowed
- Credentials must be explicitly configured

### Password Policy
- Minimum length: 14 characters
- Breach checking enabled with HIBP API
- Password history: 12 passwords

### Account Security
- Account lockout enabled
- Maximum attempts: 3
- MFA required for all users

### Session Management
- Timeout: ≤ 15 minutes
- Secure cookies required
- Redis-backed session storage

### Audit Requirements
- Audit logging enabled
- Retention: ≥ 365 days
- Real-time alerting configured

## Client Configuration

### Spring Boot Services

Add Config Client dependency:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

Configure in `bootstrap.yml`:

```yaml
spring:
  application:
    name: mcp-organization
  cloud:
    config:
      uri: http://config-server:8888
      username: config-admin
      password: ${CONFIG_SERVER_PASSWORD}
      fail-fast: true
      retry:
        max-attempts: 6
        initial-interval: 1000
```

### Using Security Properties

Inject validated security configuration:

```java
@Component
public class SecurityConfigConsumer {
    
    @Autowired
    private SecurityProperties securityProperties;
    
    public void configureJwt() {
        String issuer = securityProperties.getJwt().getIssuerUri();
        int validity = securityProperties.getJwt().getAccessTokenValidity();
        // Use configuration
    }
}
```

## Monitoring

### Health Check

Monitor Config Server health:

```bash
curl http://config-server:8888/security-config/health
```

### Metrics

Available metrics:
- `config.refresh.success` - Successful refreshes
- `config.refresh.failure` - Failed refreshes
- `config.validation.errors` - Validation errors
- `config.client.requests` - Client configuration requests

## Troubleshooting

### Configuration Not Loading

1. Check Config Server logs
2. Verify network connectivity
3. Ensure proper authentication
4. Check Git repository access

### Validation Failures

1. Review validation error details
2. Check environment-specific requirements
3. Verify all dependent properties exist
4. Ensure secrets are properly configured

### Refresh Not Working

1. Verify `@RefreshScope` annotation
2. Check actuator endpoints enabled
3. Review refresh endpoint security
4. Monitor refresh event logs

## Best Practices

1. **Use Environment Variables**: Never hardcode sensitive values
2. **Encrypt Sensitive Properties**: Use Config Server encryption
3. **Version Control**: Track configuration changes in Git
4. **Test Configuration**: Validate before deploying
5. **Monitor Changes**: Set up alerts for configuration updates
6. **Document Overrides**: Clearly document environment differences
7. **Regular Audits**: Review security settings quarterly