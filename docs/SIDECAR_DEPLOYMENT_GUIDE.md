# MCP Sidecar Deployment Guide

## 🚀 Overview

This guide provides step-by-step instructions for deploying the MCP Sidecar pattern implementation. The sidecar provides enterprise-grade security, authentication, API management, and AI service routing for the Zamaz Debate MCP system.

## 📋 Prerequisites

### System Requirements
- **Java 21** or higher
- **Docker** and **Docker Compose**
- **Redis** (for session management)
- **PostgreSQL** (for data persistence)
- **Kubernetes** (for production deployment)

### Network Requirements
- Port 8080 (Sidecar main port)
- Port 6379 (Redis)
- Port 5432 (PostgreSQL)
- Access to all MCP services (5002, 5003, 5004, 5005, 5013, 8082)

## 🔧 Environment Setup

### 1. Configure Environment Variables

Update your `.env` file with sidecar-specific configuration:

```bash
# MCP Sidecar Configuration
MCP_SIDECAR_PORT=8080
SIDECAR_JWT_SECRET=your-production-secret-key-here
SIDECAR_JWT_ISSUER=zamaz-mcp-sidecar
SIDECAR_JWT_EXPIRATION=86400
SIDECAR_JWT_REFRESH_EXPIRATION=604800

# Sidecar Security Configuration
SIDECAR_RATE_LIMIT_REPLENISH_RATE=10
SIDECAR_RATE_LIMIT_BURST_CAPACITY=20
SIDECAR_RATE_LIMIT_REQUESTED_TOKENS=1

# Circuit Breaker Configuration
CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE=10
CIRCUIT_BREAKER_MINIMUM_CALLS=5
CIRCUIT_BREAKER_FAILURE_THRESHOLD=50
CIRCUIT_BREAKER_WAIT_DURATION=30s
```

### 2. Security Configuration

**IMPORTANT**: Generate a secure JWT secret for production:

```bash
# Generate a secure JWT secret
openssl rand -base64 32
```

Update the `SIDECAR_JWT_SECRET` in your `.env` file with the generated secret.

## 🐳 Local Development Deployment

### Option 1: Docker Compose (Recommended)

1. **Start all services with sidecar**:
   ```bash
   docker-compose -f docker-compose.sidecar.yml up -d
   ```

2. **Verify services are running**:
   ```bash
   docker-compose -f docker-compose.sidecar.yml ps
   ```

3. **Check logs**:
   ```bash
   docker-compose -f docker-compose.sidecar.yml logs -f mcp-sidecar
   ```

### Option 2: Manual Build and Run

1. **Build the sidecar service**:
   ```bash
   cd mcp-sidecar
   mvn clean package -DskipTests
   ```

2. **Run locally**:
   ```bash
   java -jar target/mcp-sidecar-*.jar
   ```

## ☸️ Production Deployment (Kubernetes)

### 1. Create Namespace

```bash
kubectl create namespace zamaz-mcp
```

### 2. Deploy Secrets

```bash
kubectl apply -f k8s/sidecar/sidecar-secrets.yml
```

### 3. Deploy Sidecar

```bash
kubectl apply -f k8s/sidecar/sidecar-deployment.yml
```

### 4. Verify Deployment

```bash
# Check pod status
kubectl get pods -n zamaz-mcp -l app=mcp-sidecar

# Check service
kubectl get svc -n zamaz-mcp mcp-sidecar-service

# Check logs
kubectl logs -n zamaz-mcp deployment/mcp-sidecar -f
```

## 🔍 Health Checks and Monitoring

### 1. Health Check Endpoints

```bash
# Sidecar health
curl http://localhost:8080/actuator/health

# Authentication health
curl http://localhost:8080/api/v1/auth/health

# Fallback health
curl http://localhost:8080/fallback/health
```

### 2. Monitoring Setup

1. **Prometheus Metrics**:
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```

2. **Grafana Dashboard**:
   - Import dashboard from `monitoring/grafana/dashboards/sidecar-dashboard.json`
   - Access at `http://localhost:3000`

3. **Service Monitoring**:
   ```bash
   # Start monitoring stack
   docker-compose -f docker-compose.sidecar.yml up -d prometheus grafana
   ```

## 🧪 Testing the Deployment

### 1. Authentication Tests

```bash
# Test login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'

# Test protected endpoint
curl -X GET http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 2. Service Routing Tests

```bash
# Test organization service routing
curl -X GET http://localhost:8080/api/v1/organizations \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# Test LLM service routing
curl -X GET http://localhost:8080/api/v1/llm/providers \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# Test debate service routing
curl -X GET http://localhost:8080/api/v1/debates \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 3. Fallback Tests

```bash
# Test fallback responses (when services are down)
curl http://localhost:8080/fallback/organization
curl http://localhost:8080/fallback/llm
curl http://localhost:8080/fallback/debate
```

### 4. Circuit Breaker Tests

```bash
# Monitor circuit breaker metrics
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state
```

## 🛠️ Troubleshooting

### Common Issues

1. **Sidecar won't start**:
   - Check Java version: `java -version`
   - Verify environment variables are set
   - Check port availability: `netstat -tuln | grep 8080`

2. **Authentication failures**:
   - Verify JWT secret is set correctly
   - Check Redis connection
   - Validate user credentials

3. **Service routing issues**:
   - Verify backend services are running
   - Check network connectivity
   - Review circuit breaker status

4. **High memory usage**:
   - Adjust JVM heap size: `-Xms512m -Xmx1024m`
   - Monitor GC performance
   - Check for memory leaks

### Debug Commands

```bash
# Check service status
kubectl get pods -n zamaz-mcp -l app=mcp-sidecar

# View detailed logs
kubectl logs -n zamaz-mcp deployment/mcp-sidecar --tail=100

# Check environment variables
kubectl exec -it deployment/mcp-sidecar -n zamaz-mcp -- env | grep SIDECAR

# Test internal connectivity
kubectl exec -it deployment/mcp-sidecar -n zamaz-mcp -- curl http://mcp-security:8082/actuator/health
```

## 📊 Performance Tuning

### JVM Tuning

```bash
# Recommended JVM settings for production
JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication"
```

### Redis Optimization

```bash
# Redis configuration for high performance
REDIS_MAXMEMORY=1gb
REDIS_MAXMEMORY_POLICY=allkeys-lru
REDIS_TIMEOUT=5000ms
```

### Circuit Breaker Tuning

```bash
# Adjust based on your traffic patterns
CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE=20
CIRCUIT_BREAKER_MINIMUM_CALLS=10
CIRCUIT_BREAKER_FAILURE_THRESHOLD=60
```

## 🔒 Security Considerations

### Production Security Checklist

- [ ] Generate secure JWT secrets
- [ ] Enable HTTPS/TLS
- [ ] Configure proper CORS origins
- [ ] Set up rate limiting
- [ ] Enable audit logging
- [ ] Configure network policies
- [ ] Set up secret rotation
- [ ] Monitor for suspicious activity

### Security Headers

```yaml
# Add to your ingress/load balancer
headers:
  X-Content-Type-Options: "nosniff"
  X-Frame-Options: "DENY"
  X-XSS-Protection: "1; mode=block"
  Strict-Transport-Security: "max-age=31536000; includeSubDomains"
  Content-Security-Policy: "default-src 'self'"
```

## 📈 Scaling

### Horizontal Scaling

```bash
# Scale sidecar replicas
kubectl scale deployment/mcp-sidecar --replicas=5 -n zamaz-mcp

# Enable HPA
kubectl apply -f k8s/sidecar/sidecar-deployment.yml
```

### Load Balancing

```bash
# Configure load balancer
kubectl get svc mcp-sidecar-service -n zamaz-mcp
```

## 🔄 Updates and Maintenance

### Rolling Updates

```bash
# Update sidecar image
kubectl set image deployment/mcp-sidecar mcp-sidecar=zamaz/mcp-sidecar:v2.0.0 -n zamaz-mcp

# Monitor rollout
kubectl rollout status deployment/mcp-sidecar -n zamaz-mcp
```

### Backup and Recovery

```bash
# Backup Redis data
kubectl exec -it redis-0 -n zamaz-mcp -- redis-cli --rdb /tmp/backup.rdb

# Backup configuration
kubectl get configmap sidecar-config -n zamaz-mcp -o yaml > sidecar-config-backup.yaml
```

## 📞 Support and Documentation

### Additional Resources

- **API Documentation**: `http://localhost:8080/swagger-ui.html`
- **Metrics**: `http://localhost:8080/actuator/prometheus`
- **Health Checks**: `http://localhost:8080/actuator/health`
- **Architecture Documentation**: `docs/SIDECAR_ARCHITECTURE_PLANS.md`

### Getting Help

1. Check the logs first: `kubectl logs -n zamaz-mcp deployment/mcp-sidecar`
2. Review the troubleshooting section above
3. Check the GitHub issues for known problems
4. Contact the development team

## 🎯 Success Criteria

Your sidecar deployment is successful when:

- [ ] All health checks pass
- [ ] Authentication works correctly
- [ ] Services are routing properly
- [ ] Fallbacks work when services are down
- [ ] Monitoring shows healthy metrics
- [ ] Circuit breakers are functioning
- [ ] Rate limiting is active
- [ ] Security headers are present

---

*This deployment guide is part of the MCP Sidecar implementation. For architectural details, see `docs/SIDECAR_ARCHITECTURE_PLANS.md`.*