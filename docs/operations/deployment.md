# Deployment Guide

This guide provides instructions for deploying the Zamaz Debate MCP system to various environments.

## Deployment Environments

The system supports three deployment environments:

1. **Development**: For local development and testing
2. **Staging**: For pre-production testing
3. **Production**: For live deployment

## Prerequisites

Before deployment, ensure you have:

- Docker and Docker Compose installed
- Access to container registry (if using private registry)
- Required API keys for LLM providers
- SSL certificates for production deployment
- Database backup strategy in place
- Monitoring tools configured

## Environment Configuration

### Environment Files

Each environment uses a specific environment file:

- Development: `.env`
- Staging: `.env.staging`
- Production: `.env.production`

Create these files from the template:

```bash
cp .env.example .env.production
# Edit with production values
```

### Environment-Specific Docker Compose Files

- Development: `docker-compose.yml`
- Staging: `docker-compose.staging.yml`
- Production: `docker-compose.prod.yml`

## Deployment Steps

### 1. Prepare Environment Variables

```bash
# For production
cp .env.example .env.production
# Edit .env.production with production values
```

Ensure these critical variables are set:

- `POSTGRES_PASSWORD`: Strong database password
- `JWT_SECRET`: Secure JWT secret (environment variable)
- `ANTHROPIC_API_KEY`: Claude API key
- `OPENAI_API_KEY`: OpenAI API key
- `GOOGLE_API_KEY`: Google API key
- `REDIS_PASSWORD`: Redis password
- `SSL_CERT_PATH`: Path to SSL certificate
- `SSL_KEY_PATH`: Path to SSL key

### 2. Build Docker Images

```bash
# For production
docker-compose -f docker-compose.prod.yml build
```

### 3. Push Images to Registry (Optional)

```bash
# Login to registry
docker login your-registry.com

# Tag images
docker tag zamaz-mcp-llm:latest your-registry.com/zamaz-mcp-llm:latest

# Push images
docker push your-registry.com/zamaz-mcp-llm:latest
```

### 4. Initialize Databases

```bash
# Start PostgreSQL
docker-compose -f docker-compose.prod.yml up -d postgres

# Wait for PostgreSQL to be ready
docker-compose -f docker-compose.prod.yml exec postgres pg_isready

# Initialize databases
docker-compose -f docker-compose.prod.yml --profile setup up postgres-init
```

### 5. Deploy Services

```bash
# Deploy all services
docker-compose -f docker-compose.prod.yml up -d

# Check service status
docker-compose -f docker-compose.prod.yml ps
```

### 6. Verify Deployment

```bash
# Check service health
for service in mcp-organization mcp-llm mcp-controller mcp-rag mcp-template; do
  curl -f http://localhost:${service#mcp-}/actuator/health
done
```

## Scaling

### Horizontal Scaling

For production environments, you can scale services horizontally:

```bash
# Scale LLM service to 3 instances
docker-compose -f docker-compose.prod.yml up -d --scale mcp-llm=3
```

### Load Balancing

For production deployments, use a load balancer like Nginx or Traefik:

```bash
# Example Traefik configuration in docker-compose.prod.yml
services:
  traefik:
    image: traefik:v2.9
    command:
      - "--providers.docker=true"
      - "--providers.docker.exposedbydefault=false"
      - "--entrypoints.web.address=:80"
      - "--entrypoints.websecure.address=:443"
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro
      - ./certs:/certs
```

## Database Management

### Backup Strategy

Set up regular database backups:

```bash
# Create backup script
cat > backup-db.sh << 'EOF'
#!/bin/bash
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backups"
docker-compose -f docker-compose.prod.yml exec -T postgres \
  pg_dump -U postgres postgres > "$BACKUP_DIR/postgres_$TIMESTAMP.sql"
EOF
chmod +x backup-db.sh

# Add to crontab
echo "0 2 * * * /path/to/backup-db.sh" | crontab -
```

### Database Migrations

For schema updates:

```bash
# Run migrations
docker-compose -f docker-compose.prod.yml exec mcp-organization \
  java -jar /app/app.jar --spring.profiles.active=migration
```

## Monitoring Setup

Enable the monitoring stack:

```bash
# Start monitoring services
docker-compose -f docker-compose.prod.yml --profile monitoring up -d
```

Access monitoring tools:
- Prometheus: http://your-server:9090
- Grafana: http://your-server:3000

## SSL Configuration

For production environments, configure SSL:

1. Place certificates in `./certs` directory:
   - `./certs/cert.pem`: SSL certificate
   - `./certs/key.pem`: SSL private key

2. Update environment variables:
   ```
   SSL_CERT_PATH=/certs/cert.pem
   SSL_KEY_PATH=/certs/key.pem
   ```

3. Configure services to use SSL:
   ```yaml
   # In docker-compose.prod.yml
   services:
     mcp-gateway:
       environment:
         - SSL_ENABLED=true
         - SSL_CERT_PATH=${SSL_CERT_PATH}
         - SSL_KEY_PATH=${SSL_KEY_PATH}
   ```

## Deployment Automation

### CI/CD Pipeline

Example GitHub Actions workflow:

```yaml
# .github/workflows/deploy.yml
name: Deploy to Production

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
          
      - name: Build with Maven
        run: mvn -B package --file pom.xml
        
      - name: Build and push Docker images
        uses: docker/build-push-action@v4
        with:
          context: .
          push: true
          tags: your-registry.com/zamaz-mcp:latest
          
      - name: Deploy to production
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.PROD_HOST }}
          username: ${{ secrets.PROD_USERNAME }}
          key: ${{ secrets.PROD_SSH_KEY }}
          script: |
            cd /opt/zamaz-debate-mcp
            docker-compose -f docker-compose.prod.yml pull
            docker-compose -f docker-compose.prod.yml up -d
```

## Rolling Updates

For zero-downtime updates:

```bash
# Update services one by one
for service in mcp-organization mcp-llm mcp-controller mcp-rag mcp-template; do
  docker-compose -f docker-compose.prod.yml up -d --no-deps --build $service
  # Wait for service to be healthy
  while ! curl -s http://localhost:${service#mcp-}/actuator/health | grep -q "UP"; do
    sleep 5
  done
done
```

## Rollback Procedure

If deployment fails:

```bash
# Rollback to previous version
docker-compose -f docker-compose.prod.yml down
docker tag your-registry.com/zamaz-mcp:previous your-registry.com/zamaz-mcp:latest
docker-compose -f docker-compose.prod.yml up -d
```

## Multi-Region Deployment

For high availability across regions:

1. Deploy to multiple regions
2. Use global DNS routing (e.g., Route 53)
3. Replicate databases across regions
4. Implement cross-region monitoring

## Security Hardening

For production deployments:

1. **Network Security**:
   - Use private networks for inter-service communication
   - Implement network policies to restrict traffic
   - Use VPC for cloud deployments

2. **Container Security**:
   - Run containers as non-root users
   - Use read-only file systems where possible
   - Implement resource limits

3. **API Security**:
   - Enable rate limiting
   - Implement IP allowlisting
   - Use API gateways for additional security

## Environment-Specific Configurations

### Development

```bash
# Start development environment
docker-compose up -d
```

### Staging

```bash
# Start staging environment
docker-compose -f docker-compose.staging.yml up -d
```

### Production

```bash
# Start production environment
docker-compose -f docker-compose.prod.yml up -d
```

## Deployment Checklist

Before deploying to production:

- [ ] All tests pass
- [ ] Security scan completed
- [ ] Environment variables configured
- [ ] Database backups configured
- [ ] Monitoring set up
- [ ] SSL certificates installed
- [ ] Load testing completed
- [ ] Rollback procedure tested
- [ ] Documentation updated

## Troubleshooting

### Common Deployment Issues

1. **Service won't start**:
   - Check logs: `docker-compose -f docker-compose.prod.yml logs mcp-service-name`
   - Verify environment variables
   - Check for port conflicts

2. **Database connection issues**:
   - Verify database is running: `docker-compose -f docker-compose.prod.yml ps postgres`
   - Check database credentials
   - Ensure database initialization completed

3. **Network issues**:
   - Check network configuration: `docker network ls`
   - Verify services are on the same network
   - Check firewall rules

4. **Performance issues**:
   - Monitor resource usage: `docker stats`
   - Check for memory leaks
   - Optimize JVM settings

## Post-Deployment Tasks

After successful deployment:

1. Verify all services are healthy
2. Run smoke tests
3. Monitor system for 24 hours
4. Update documentation
5. Tag release in version control
6. Notify stakeholders of successful deployment
