# Environment Variables Setup

## Overview

This project uses a `.env` file to manage sensitive configuration like database passwords. The `.env` file is already configured with the database password and is properly excluded from version control.

## Current Setup

✅ **Database Password**: Set in `.env` file  
✅ **Application Configuration**: Uses `${DB_PASSWORD}` environment variable  
✅ **Security**: `.env` is in `.gitignore` and won't be committed  

## How to Run Services

### Option 1: Using the run script (Recommended)
```bash
# Run individual services
./run-with-env.sh mcp-rag
./run-with-env.sh mcp-template
./run-with-env.sh mcp-modulith

# Run all services with Docker
./run-with-env.sh all
```

### Option 2: Manual with environment variables
```bash
# Load environment variables
export $(cat .env | grep -v '^#' | xargs)

# Run with Maven
cd mcp-rag && mvn spring-boot:run

# Or run with Java
java -jar target/mcp-rag.jar
```

### Option 3: Docker Compose (automatically uses .env)
```bash
# Docker Compose automatically loads .env file
docker-compose up -d
```

### Option 4: IDE Configuration

#### IntelliJ IDEA:
1. Run → Edit Configurations
2. Select your Spring Boot configuration
3. Environment variables: `DB_PASSWORD=postgres`

#### VS Code:
1. Create `.vscode/launch.json`
2. Add env variables:
```json
{
  "configurations": [{
    "type": "java",
    "env": {
      "DB_PASSWORD": "postgres"
    }
  }]
}
```

## Environment Variables Reference

| Variable | Description | Default | Used By |
|----------|-------------|---------|---------|
| `DB_PASSWORD` | PostgreSQL password | `changeme` | All Java services |
| `POSTGRES_PASSWORD` | PostgreSQL root password | `postgres` | Docker Compose |
| `POSTGRES_USER` | PostgreSQL username | `lsendel` | Docker Compose |
| `POSTGRES_DB` | Default database name | `context_db` | Docker Compose |

## Security Best Practices

1. **Never commit `.env` to git** - It's already in `.gitignore`
2. **Use different passwords per environment**:
   - Development: Current setup is fine
   - Staging: Use stronger passwords
   - Production: Use secrets management
3. **Rotate passwords regularly**
4. **Limit database user permissions**

## Troubleshooting

### Service won't start
```bash
# Check if environment variable is set
echo $DB_PASSWORD

# Check if .env file exists
ls -la .env

# View loaded environment variables
env | grep DB_
```

### Database connection refused
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Test connection
psql -h localhost -U lsendel -d context_db
```

### Permission denied on script
```bash
chmod +x run-with-env.sh
```

## Production Deployment

For production, consider using:
- **AWS**: AWS Secrets Manager or Parameter Store
- **Kubernetes**: Kubernetes Secrets
- **Docker Swarm**: Docker Secrets
- **Vault**: HashiCorp Vault

Example with Docker Secrets:
```yaml
services:
  app:
    environment:
      DB_PASSWORD_FILE: /run/secrets/db_password
    secrets:
      - db_password

secrets:
  db_password:
    external: true
```

## Next Steps

1. ✅ Database passwords are now in `.env`
2. ✅ Applications configured to use environment variables
3. ✅ Security documentation created
4. 🔜 Run SonarCloud analysis to verify BLOCKER issues are resolved
5. 🔜 Consider implementing secrets management for production