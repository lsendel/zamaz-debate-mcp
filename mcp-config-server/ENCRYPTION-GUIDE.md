# Configuration Encryption Guide

This guide explains how to use encryption in the Spring Cloud Config Server to protect sensitive configuration values.

## Overview

The Config Server supports encryption of sensitive properties like passwords, API keys, and secrets. Encrypted values are stored with a `{cipher}` prefix and are automatically decrypted when served to clients.

## Encryption Methods

### 1. Symmetric Encryption (Default)

Uses a shared secret key for both encryption and decryption.

#### Setup

Set the encryption key using environment variables:

```bash
export CONFIG_ENCRYPTION_KEY=your-secret-key-here
```

Or in `application.yml`:

```yaml
encrypt:
  key: ${CONFIG_ENCRYPTION_KEY}
```

### 2. Asymmetric Encryption (RSA)

Uses public/private key pairs for better security.

#### Generate Key Store

```bash
keytool -genkeypair -alias config-server-key \
  -keyalg RSA -keysize 2048 \
  -keystore config-server.jks \
  -storepass changeit \
  -keypass changeit \
  -dname "CN=Config Server,OU=MCP,O=Zamaz,L=City,ST=State,C=US" \
  -validity 365
```

#### Configure

```yaml
encrypt:
  key-store:
    location: classpath:/config-server.jks
    password: ${KEYSTORE_PASSWORD}
    alias: config-server-key
    secret: ${KEY_PASSWORD}
```

## Encrypting Values

### Using REST API

```bash
# Encrypt a value
curl -X POST http://localhost:8888/encryption/encrypt \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic <base64-encoded-credentials>" \
  -d '{"value": "mySecretPassword"}'

# Response:
{
  "encryptedValue": "{cipher}AQBvYml0bW9yZS5jb20..."
}
```

### Using Spring CLI

```bash
spring encrypt mySecretPassword --key mySecretKey
```

### Batch Encryption

```bash
curl -X POST http://localhost:8888/encryption/encrypt-batch \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic <base64-encoded-credentials>" \
  -d '{
    "values": {
      "db.password": "dbpass123",
      "api.key": "apikey456",
      "jwt.secret": "jwtsecret789"
    }
  }'
```

## Using Encrypted Values

### In Configuration Files

```yaml
# application-prod.yml
spring:
  datasource:
    password: '{cipher}AQBvYml0bW9yZS5jb20...'

jwt:
  secret: '{cipher}AQCmV5LmNvbS4uLi4...'

api:
  key: '{cipher}AQDnZXJ5LmNvbS4uLi4...'
```

### Environment Variables

```bash
export DB_PASSWORD='{cipher}AQBvYml0bW9yZS5jb20...'
```

## Key Rotation

### Manual Rotation

```bash
curl -X POST http://localhost:8888/encryption/rotate-key \
  -H "Authorization: Basic <super-admin-credentials>"
```

### Automatic Rotation

Configure in `application.yml`:

```yaml
encrypt:
  key-rotation:
    enabled: true
    interval-days: 90
    cron: "0 0 2 * * ?"  # Daily at 2 AM
    max-key-history: 5
```

## Security Best Practices

### 1. Key Management

- **Never commit encryption keys** to version control
- Store keys in secure locations (environment variables, vault, etc.)
- Use strong, randomly generated keys
- Rotate keys regularly

### 2. Access Control

- Restrict access to encryption endpoints
- Use strong authentication for Config Server
- Implement role-based access control

### 3. Network Security

- Always use HTTPS for Config Server communication
- Implement network segmentation
- Use VPN or private networks for sensitive environments

### 4. Audit and Monitoring

- Log all encryption/decryption operations
- Monitor for failed decryption attempts
- Track key rotation events

## Integration with External Key Management

### HashiCorp Vault

```yaml
spring:
  profiles: vault
  cloud:
    vault:
      host: vault.example.com
      port: 8200
      scheme: https
      authentication: TOKEN
      token: ${VAULT_TOKEN}
```

### AWS Secrets Manager

```yaml
spring:
  profiles: aws
  cloud:
    config:
      server:
        aws-secretsmanager:
          region: us-east-1
          prefix: /secret/mcp
```

### Azure Key Vault

```yaml
azure:
  keyvault:
    uri: https://mcp-keyvault.vault.azure.net/
    client-id: ${AZURE_CLIENT_ID}
    client-key: ${AZURE_CLIENT_KEY}
    tenant-id: ${AZURE_TENANT_ID}
```

## Troubleshooting

### Common Issues

1. **"Encryption key not configured"**
   - Ensure `CONFIG_ENCRYPTION_KEY` environment variable is set
   - Check that the key is properly formatted

2. **"Cannot decrypt value"**
   - Verify the value has `{cipher}` prefix
   - Check that the correct key is being used
   - Ensure the value hasn't been corrupted

3. **"Key store not found"**
   - Verify the key store file exists at the specified location
   - Check file permissions
   - Ensure the path is correct (classpath: vs file:)

### Validation

Test encryption setup:

```bash
# Validate an encrypted value
curl -X POST http://localhost:8888/encryption/validate \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic <credentials>" \
  -d '{"value": "{cipher}AQBvYml0bW9yZS5jb20..."}'
```

## Examples

### Example 1: Encrypting Database Password

```bash
# Encrypt
ENCRYPTED=$(curl -s -X POST http://localhost:8888/encryption/encrypt \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  -d '{"value": "myDbPassword"}' | jq -r .encryptedValue)

# Use in configuration
echo "spring.datasource.password: $ENCRYPTED" >> application-prod.yml
```

### Example 2: Encrypting Multiple Secrets

```bash
# Create a file with secrets
cat > secrets.json << EOF
{
  "values": {
    "db.password": "dbpass",
    "redis.password": "redispass",
    "jwt.secret": "jwtsecret",
    "api.key": "apikey"
  }
}
EOF

# Encrypt all at once
curl -X POST http://localhost:8888/encryption/encrypt-batch \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  -d @secrets.json > encrypted-secrets.json
```

### Example 3: Decrypting for Debugging

```bash
# Decrypt a value (admin only)
curl -X POST http://localhost:8888/encryption/decrypt \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  -d '{"value": "{cipher}AQBvYml0bW9yZS5jb20..."}'
```

## Migration Guide

### Migrating from Plain Text to Encrypted Values

1. Identify all sensitive properties
2. Encrypt values using the batch endpoint
3. Update configuration files with encrypted values
4. Test in a staging environment
5. Deploy to production
6. Remove plain text values from version control history

### Script for Migration

```bash
#!/bin/bash
# migrate-to-encrypted.sh

CONFIG_SERVER="http://localhost:8888"
AUTH="YWRtaW46YWRtaW4="  # Base64 encoded credentials

# Read properties to encrypt
while IFS='=' read -r key value; do
  if [[ $key =~ (password|secret|key|token) ]]; then
    encrypted=$(curl -s -X POST "$CONFIG_SERVER/encryption/encrypt" \
      -H "Content-Type: application/json" \
      -H "Authorization: Basic $AUTH" \
      -d "{\"value\": \"$value\"}" | jq -r .encryptedValue)
    
    echo "$key: $encrypted"
  else
    echo "$key: $value"
  fi
done < application.properties > application-encrypted.yml
```