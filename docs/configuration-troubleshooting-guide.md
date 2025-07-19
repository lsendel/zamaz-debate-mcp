# Configuration Troubleshooting Guide

This guide helps diagnose and resolve common issues with the MCP Configuration Management System.

## Quick Diagnostics

### Health Check Commands

```bash
# Config Server health
curl http://localhost:8888/actuator/health

# Service configuration endpoint
curl http://localhost:8888/{service-name}/{profile}

# Service refresh endpoint
curl -X POST http://service:port/actuator/refresh

# Bus refresh all services
curl -X POST http://localhost:8888/actuator/bus-refresh
```

## Common Issues and Solutions

### 1. Config Server Issues

#### Config Server Won't Start

**Symptoms:**
- Config Server fails to start
- Port 8888 not accessible
- Health check fails

**Diagnostics:**
```bash
# Check logs
docker logs mcp-config-server

# Check if port is in use
lsof -i :8888

# Verify environment variables
docker exec mcp-config-server env | grep CONFIG
```

**Solutions:**
1. Verify Git repository access:
   ```bash
   git ls-remote https://github.com/your-org/config-repo.git
   ```

2. Check encryption key:
   ```bash
   echo $CONFIG_ENCRYPTION_KEY
   ```

3. Validate application.yml syntax:
   ```bash
   yamllint mcp-config-server/src/main/resources/application.yml
   ```

#### Config Server Can't Access Git Repository

**Symptoms:**
- "Cannot clone repository" errors
- "Authentication failed" messages
- Timeout errors

**Solutions:**

1. **For HTTPS repositories:**
   ```yaml
   spring:
     cloud:
       config:
         server:
           git:
             uri: https://github.com/org/config-repo.git
             username: ${GIT_USERNAME}
             password: ${GIT_PASSWORD}
   ```

2. **For SSH repositories:**
   ```bash
   # Generate SSH key
   ssh-keygen -t rsa -b 4096 -f config-server-key
   
   # Add to Config Server
   docker exec -it mcp-config-server bash
   mkdir -p ~/.ssh
   echo "$SSH_PRIVATE_KEY" > ~/.ssh/id_rsa
   chmod 600 ~/.ssh/id_rsa
   ```

3. **For private repositories:**
   ```yaml
   spring:
     cloud:
       config:
         server:
           git:
             uri: git@github.com:org/config-repo.git
             ignore-local-ssh-settings: false
             private-key: ${GIT_PRIVATE_KEY}
   ```

### 2. Service Configuration Issues

#### Service Can't Connect to Config Server

**Symptoms:**
- "Could not locate PropertySource" error
- Service fails to start
- Connection refused errors

**Diagnostics:**
```bash
# Test connectivity
docker exec mcp-service ping mcp-config-server

# Check DNS resolution
docker exec mcp-service nslookup mcp-config-server

# Verify network
docker network ls
docker network inspect mcp-network
```

**Solutions:**

1. **Check bootstrap.yml:**
   ```yaml
   spring:
     cloud:
       config:
         uri: ${CONFIG_SERVER_URI:http://mcp-config-server:8888}
         fail-fast: true
         retry:
           max-attempts: 5
   ```

2. **Add service dependency:**
   ```yaml
   # docker-compose.yml
   services:
     mcp-service:
       depends_on:
         mcp-config-server:
           condition: service_healthy
   ```

3. **Implement fallback:**
   ```yaml
   spring:
     cloud:
       config:
         fail-fast: false
   ```

#### Properties Not Loading

**Symptoms:**
- Default values being used
- Missing configuration properties
- "Could not resolve placeholder" errors

**Diagnostics:**
```bash
# Check what Config Server returns
curl http://localhost:8888/mcp-organization/development | jq .

# Verify active profiles
curl http://service:port/actuator/env | jq '.activeProfiles'

# Check loaded properties
curl http://service:port/actuator/configprops | jq .
```

**Solutions:**

1. **Verify file naming:**
   - Pattern: `{application-name}-{profile}.yml`
   - Example: `mcp-organization-development.yml`

2. **Check property sources order:**
   ```bash
   curl http://service:port/actuator/env | jq '.propertySources[].name'
   ```

3. **Validate YAML syntax:**
   ```bash
   yamllint config-repo/mcp-organization.yml
   ```

### 3. Encryption/Decryption Issues

#### Encrypted Values Not Decrypting

**Symptoms:**
- `{cipher}...` appearing in properties
- "Cannot decrypt" errors
- Invalid cipher text errors

**Diagnostics:**
```bash
# Test encryption endpoint
curl -X POST http://localhost:8888/encrypt -d "test"

# Test decryption endpoint
ENCRYPTED=$(curl -X POST http://localhost:8888/encrypt -d "test")
curl -X POST http://localhost:8888/decrypt -d "$ENCRYPTED"
```

**Solutions:**

1. **Verify encryption key:**
   ```bash
   # On Config Server
   echo $CONFIG_ENCRYPTION_KEY
   
   # Should match the key used for encryption
   ```

2. **Check cipher format:**
   ```yaml
   # Correct format
   password: {cipher}AQBcNxLW9Ex2Zshz...
   
   # Incorrect formats
   password: cipher:AQBcNxLW9Ex2Zshz...
   password: {cipher}:AQBcNxLW9Ex2Zshz...
   ```

3. **Re-encrypt values:**
   ```bash
   # Decrypt old value
   OLD_VALUE=$(curl -X POST http://localhost:8888/decrypt -d "old-cipher-text")
   
   # Re-encrypt with current key
   NEW_CIPHER=$(curl -X POST http://localhost:8888/encrypt -d "$OLD_VALUE")
   ```

#### Encryption Key Issues

**Symptoms:**
- "No encryption key configured" error
- Different results when encrypting same value
- Key rotation problems

**Solutions:**

1. **Generate proper key:**
   ```bash
   # For symmetric encryption
   export CONFIG_ENCRYPTION_KEY=$(openssl rand -base64 32)
   ```

2. **Use keystore for production:**
   ```yaml
   encrypt:
     keyStore:
       location: classpath:/server.jks
       password: ${KEYSTORE_PASSWORD}
       alias: configkey
       secret: ${KEY_PASSWORD}
   ```

### 4. Refresh Issues

#### Configuration Not Refreshing

**Symptoms:**
- Old values still in use after refresh
- Refresh endpoint returns empty array
- Bus refresh not propagating

**Diagnostics:**
```bash
# Check refresh scope
curl http://service:port/actuator/beans | jq '.contexts[].beans | to_entries[] | select(.value.scope == "refresh")'

# Monitor bus events
curl http://localhost:8888/actuator/trace
```

**Solutions:**

1. **Add @RefreshScope:**
   ```java
   @Component
   @RefreshScope
   public class ConfigProperties {
       @Value("${my.property}")
       private String property;
   }
   ```

2. **Verify Bus configuration:**
   ```yaml
   spring:
     cloud:
       bus:
         enabled: true
     rabbitmq:
       host: rabbitmq
       port: 5672
   ```

3. **Check RabbitMQ:**
   ```bash
   # Access RabbitMQ management
   open http://localhost:15672
   # Default: guest/guest
   ```

#### Partial Refresh

**Symptoms:**
- Some services refresh, others don't
- Inconsistent configuration state
- Refresh timeouts

**Solutions:**

1. **Target specific services:**
   ```bash
   # Refresh only organization service
   curl -X POST http://localhost:8888/actuator/bus-refresh/mcp-organization:**
   ```

2. **Increase timeout:**
   ```yaml
   spring:
     cloud:
       bus:
         ack:
           timeout: 10000
   ```

### 5. Performance Issues

#### Slow Startup

**Symptoms:**
- Service takes long to start
- Config Server timeouts
- Multiple retry attempts

**Solutions:**

1. **Enable clone on start:**
   ```yaml
   spring:
     cloud:
       config:
         server:
           git:
             clone-on-start: true
   ```

2. **Optimize retry strategy:**
   ```yaml
   spring:
     cloud:
       config:
         retry:
           initial-interval: 1000
           multiplier: 1.1
           max-interval: 2000
           max-attempts: 3
   ```

3. **Use local cache:**
   ```yaml
   spring:
     cloud:
       config:
         server:
           git:
             basedir: /tmp/config-repo-cache
   ```

#### High Memory Usage

**Symptoms:**
- OutOfMemoryError
- Slow response times
- Frequent garbage collection

**Solutions:**

1. **Tune JVM settings:**
   ```bash
   JAVA_OPTS="-Xmx1g -Xms512m -XX:+UseG1GC"
   ```

2. **Limit repository size:**
   - Use sparse checkout
   - Separate large files
   - Clean Git history

## Debug Mode Configuration

### Enable Debug Logging

```yaml
# Config Server
logging:
  level:
    org.springframework.cloud.config: DEBUG
    org.springframework.boot.env: DEBUG
    org.springframework.security: DEBUG

# Services
logging:
  level:
    org.springframework.cloud.config.client: DEBUG
    org.springframework.cloud.bus: DEBUG
```

### Trace HTTP Requests

```yaml
logging:
  level:
    org.springframework.web.client.RestTemplate: DEBUG
    org.apache.http: DEBUG
```

### Monitor Property Sources

```java
@Component
public class ConfigDebugger {
    @Autowired
    private Environment env;
    
    @EventListener
    public void handleContextRefresh(ContextRefreshedEvent event) {
        logger.info("Active profiles: {}", Arrays.toString(env.getActiveProfiles()));
        logger.info("Property sources: {}", env.getPropertySources());
    }
}
```

## Emergency Procedures

### Config Server Down

1. **Enable fallback mode:**
   ```yaml
   spring:
     cloud:
       config:
         fail-fast: false
   ```

2. **Use local overrides:**
   ```bash
   java -jar service.jar --spring.config.location=classpath:/,file:./emergency-config.yml
   ```

### Corrupted Configuration

1. **Revert to previous version:**
   ```bash
   cd config-repo
   git log --oneline
   git revert HEAD
   git push
   ```

2. **Force refresh:**
   ```bash
   curl -X POST http://localhost:8888/actuator/bus-refresh?force=true
   ```

### Complete Reset

```bash
# Stop all services
docker-compose down

# Clear Config Server cache
docker volume rm mcp_config_server_data

# Restart everything
docker-compose up -d
```

## Monitoring and Alerts

### Key Metrics to Monitor

1. **Config Server**:
   - `config.server.fetch.time` - Time to fetch from Git
   - `config.server.cache.hit.ratio` - Cache effectiveness
   - `http.server.requests` - Request latency

2. **Services**:
   - `config.refresh.time` - Refresh duration
   - `config.load.failures` - Failed loads
   - `spring.cloud.refresh` - Refresh events

### Alert Conditions

```yaml
# Prometheus alerts
groups:
  - name: config-alerts
    rules:
      - alert: ConfigServerDown
        expr: up{job="config-server"} == 0
        for: 5m
        
      - alert: ConfigLoadFailure
        expr: config_load_failures_total > 0
        for: 1m
        
      - alert: SlowConfigFetch
        expr: config_server_fetch_time_seconds > 5
        for: 5m
```

## Getting Help

### Collect Diagnostic Information

```bash
# Create diagnostic bundle
./scripts/collect-config-diagnostics.sh

# Information to include:
# - Config Server logs
# - Service logs
# - Configuration files
# - Network configuration
# - Environment variables
```

### Support Channels

- Internal Wiki: `wiki.company.com/mcp-config`
- Slack: `#mcp-config-support`
- Email: `mcp-team@company.com`

### Useful Resources

- [Spring Cloud Config Documentation](https://docs.spring.io/spring-cloud-config/docs/current/reference/html/)
- [Spring Cloud Bus Documentation](https://docs.spring.io/spring-cloud-bus/docs/current/reference/html/)
- [MCP Configuration Guide](./configuration-management-guide.md)
- [Migration Guide](./configuration-migration-guide.md)