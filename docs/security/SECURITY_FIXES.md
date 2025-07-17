# Security Fixes - Database Password Configuration

## 🚨 Critical Security Issues Fixed

I've fixed **3 BLOCKER security vulnerabilities** where database passwords were hardcoded in application.yml files. These have been replaced with environment variable placeholders.

### Files Updated:
1. `mcp-modulith/src/main/resources/application.yml`
2. `mcp-rag/src/main/resources/application.yml`
3. `mcp-template/src/main/resources/application.yml`

### What Changed:
```yaml
# Before (VULNERABLE):
password: postgres

# After (SECURE):
password: ${DB_PASSWORD:changeme}
```

## 🔐 How to Configure Securely

### Option 1: Environment Variables (Recommended)

Set the environment variable before running your application:

```bash
# For development
export DB_PASSWORD="your-secure-password-here"

# Or add to your shell profile (~/.zshrc or ~/.bashrc)
echo 'export DB_PASSWORD="your-secure-password-here"' >> ~/.zshrc
source ~/.zshrc
```

### Option 2: Application Properties Override

Create an `application-local.yml` file (add to .gitignore):

```yaml
spring:
  datasource:
    password: your-secure-password-here
```

Run with: `java -jar app.jar --spring.profiles.active=local`

### Option 3: System Properties

```bash
java -jar app.jar -Dspring.datasource.password=your-secure-password-here
```

### Option 4: Docker Compose

Update your `docker-compose.yml`:

```yaml
services:
  app:
    environment:
      - DB_PASSWORD=${DB_PASSWORD}
```

Then create a `.env` file (add to .gitignore):
```
DB_PASSWORD=your-secure-password-here
```

## 🛡️ Best Practices

1. **Never commit passwords** to version control
2. **Use strong passwords** (min 16 characters, mixed case, numbers, symbols)
3. **Rotate passwords regularly**
4. **Use different passwords** for each environment (dev, staging, prod)
5. **Consider using a secrets manager**:
   - AWS Secrets Manager
   - HashiCorp Vault
   - Azure Key Vault
   - Kubernetes Secrets

## 🔍 Verify the Fix

After setting your environment variable, verify it works:

```bash
# Check if variable is set
echo $DB_PASSWORD

# Test your application
mvn spring-boot:run

# Or with Docker
docker-compose up
```

## 📝 Additional Security Recommendations

1. **Update default username**: Also use environment variables for database usernames
2. **Use SSL/TLS**: Add `?sslmode=require` to your database URLs
3. **Implement connection pooling**: With proper timeout settings
4. **Add database access logs**: Monitor for suspicious activity
5. **Use least privilege**: Create specific database users with minimal permissions

## 🚀 Next Steps

1. Set your `DB_PASSWORD` environment variable
2. Test all services to ensure they connect properly
3. Run SonarCloud analysis again to verify issues are resolved
4. Consider implementing other security recommendations above

---

**Note**: The default fallback value `changeme` is intentionally obvious to remind you to set a proper password. Never use this in production!