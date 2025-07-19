# Configuration Properties Reference

This document provides a comprehensive reference for all configuration properties used in the MCP (Microservices Communication Platform) services.

## Table of Contents

1. [Global Properties](#global-properties)
2. [Service-Specific Properties](#service-specific-properties)
3. [Security Properties](#security-properties)
4. [Database Properties](#database-properties)
5. [Caching Properties](#caching-properties)
6. [Monitoring Properties](#monitoring-properties)
7. [Integration Properties](#integration-properties)

## Global Properties

These properties are defined in `application.yml` and apply to all services.

### Spring Core Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `spring.application.name` | String | - | Application name (set in each service) |
| `spring.profiles.active` | String | `default` | Active Spring profiles |
| `spring.jackson.default-property-inclusion` | Enum | `non_null` | JSON serialization inclusion |
| `spring.jackson.time-zone` | String | `UTC` | Default timezone for JSON |

### Server Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `server.port` | Integer | `8080` | HTTP server port |
| `server.servlet.context-path` | String | `/` | Context path for the application |
| `server.compression.enabled` | Boolean | `true` | Enable response compression |
| `server.http2.enabled` | Boolean | `true` | Enable HTTP/2 support |

### Logging Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `logging.level.root` | String | `INFO` | Root logging level |
| `logging.level.com.zamaz` | String | `INFO` | Application logging level |
| `logging.pattern.console` | String | See below | Console log pattern |
| `logging.pattern.file` | String | See below | File log pattern |

**Default Console Pattern:**
```
%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(${LOG_LEVEL_PATTERN:-%5p}) %clr(${PID:- }){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}
```

## Service-Specific Properties

### MCP-Config-Server

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `spring.cloud.config.server.git.uri` | String | - | Git repository URI |
| `spring.cloud.config.server.git.default-label` | String | `main` | Default Git branch |
| `spring.cloud.config.server.git.search-paths` | List | `['{application}']` | Search paths in repo |
| `spring.cloud.config.server.git.clone-on-start` | Boolean | `false` | Clone repo on startup |
| `spring.cloud.config.server.git.timeout` | Integer | `5` | Git operation timeout (seconds) |
| `encrypt.key` | String | - | Symmetric encryption key |

### MCP-Organization

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `mcp.organization.jwt.secret` | String | - | JWT signing secret |
| `mcp.organization.jwt.expiration` | Long | `3600000` | JWT expiration (ms) |
| `mcp.organization.cors.allowed-origins` | List | `['*']` | CORS allowed origins |
| `mcp.organization.cors.allowed-methods` | List | `['*']` | CORS allowed methods |
| `mcp.organization.multi-tenant.enabled` | Boolean | `false` | Enable multi-tenancy |
| `mcp.organization.cache.ttl` | Integer | `3600` | Cache TTL (seconds) |

### MCP-LLM

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `mcp.llm.providers.openai.api-key` | String | - | OpenAI API key |
| `mcp.llm.providers.openai.model` | String | `gpt-4` | Default OpenAI model |
| `mcp.llm.providers.openai.timeout` | Integer | `30000` | Request timeout (ms) |
| `mcp.llm.providers.anthropic.api-key` | String | - | Anthropic API key |
| `mcp.llm.providers.anthropic.model` | String | `claude-3` | Default Anthropic model |
| `mcp.llm.cache.enabled` | Boolean | `true` | Enable response caching |
| `mcp.llm.cache.ttl` | Integer | `3600` | Cache TTL (seconds) |

### MCP-Controller

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `mcp.controller.services.organization-url` | String | - | Organization service URL |
| `mcp.controller.services.llm-url` | String | - | LLM service URL |
| `mcp.controller.services.rag-url` | String | - | RAG service URL |
| `mcp.controller.resilience.circuit-breaker.failure-threshold` | Integer | `50` | CB failure threshold (%) |
| `mcp.controller.resilience.circuit-breaker.wait-duration` | Integer | `60000` | CB wait duration (ms) |
| `mcp.controller.resilience.retry.max-attempts` | Integer | `3` | Max retry attempts |
| `mcp.controller.resilience.retry.wait-duration` | Integer | `1000` | Retry wait duration (ms) |

### MCP-RAG

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `mcp.rag.qdrant.url` | String | - | Qdrant vector DB URL |
| `mcp.rag.qdrant.collection` | String | `documents` | Default collection name |
| `mcp.rag.embedding.model` | String | `text-embedding-ada-002` | Embedding model |
| `mcp.rag.chunk.size` | Integer | `1000` | Document chunk size |
| `mcp.rag.chunk.overlap` | Integer | `200` | Chunk overlap size |
| `mcp.rag.search.limit` | Integer | `10` | Default search results limit |

## Security Properties

### Authentication

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `spring.security.user.name` | String | `user` | Default username |
| `spring.security.user.password` | String | - | Default password |
| `mcp.security.jwt.header` | String | `Authorization` | JWT header name |
| `mcp.security.jwt.prefix` | String | `Bearer ` | JWT token prefix |
| `mcp.security.jwt.authorities-key` | String | `auth` | JWT authorities claim key |

### OAuth2

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `spring.security.oauth2.client.registration.{client}.client-id` | String | - | OAuth2 client ID |
| `spring.security.oauth2.client.registration.{client}.client-secret` | String | - | OAuth2 client secret |
| `spring.security.oauth2.client.registration.{client}.scope` | List | - | OAuth2 scopes |
| `spring.security.oauth2.client.provider.{provider}.authorization-uri` | String | - | Authorization endpoint |
| `spring.security.oauth2.client.provider.{provider}.token-uri` | String | - | Token endpoint |

## Database Properties

### DataSource

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `spring.datasource.url` | String | - | JDBC URL |
| `spring.datasource.username` | String | - | Database username |
| `spring.datasource.password` | String | - | Database password |
| `spring.datasource.driver-class-name` | String | Auto-detected | JDBC driver class |
| `spring.datasource.hikari.maximum-pool-size` | Integer | `10` | Max pool size |
| `spring.datasource.hikari.minimum-idle` | Integer | `5` | Min idle connections |
| `spring.datasource.hikari.connection-timeout` | Long | `30000` | Connection timeout (ms) |

### JPA/Hibernate

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `spring.jpa.hibernate.ddl-auto` | String | `validate` | DDL mode |
| `spring.jpa.show-sql` | Boolean | `false` | Show SQL statements |
| `spring.jpa.properties.hibernate.dialect` | String | Auto-detected | Hibernate dialect |
| `spring.jpa.properties.hibernate.format_sql` | Boolean | `false` | Format SQL |
| `spring.jpa.properties.hibernate.jdbc.batch_size` | Integer | `25` | JDBC batch size |

## Caching Properties

### Redis

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `spring.redis.host` | String | `localhost` | Redis host |
| `spring.redis.port` | Integer | `6379` | Redis port |
| `spring.redis.password` | String | - | Redis password |
| `spring.redis.timeout` | Duration | `2000ms` | Command timeout |
| `spring.redis.lettuce.pool.max-active` | Integer | `8` | Max pool connections |
| `spring.redis.lettuce.pool.max-idle` | Integer | `8` | Max idle connections |

### Cache Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `spring.cache.type` | String | `redis` | Cache type |
| `spring.cache.redis.time-to-live` | Duration | `60000ms` | Default TTL |
| `spring.cache.redis.cache-null-values` | Boolean | `false` | Cache null values |
| `spring.cache.redis.use-key-prefix` | Boolean | `true` | Use key prefix |
| `spring.cache.redis.key-prefix` | String | App name | Cache key prefix |

## Monitoring Properties

### Actuator

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `management.endpoints.web.exposure.include` | List | `['health','info']` | Exposed endpoints |
| `management.endpoint.health.show-details` | String | `when-authorized` | Health details visibility |
| `management.health.circuitbreakers.enabled` | Boolean | `true` | Circuit breaker health |
| `management.health.ratelimiters.enabled` | Boolean | `true` | Rate limiter health |

### Metrics

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `management.metrics.export.prometheus.enabled` | Boolean | `true` | Enable Prometheus export |
| `management.metrics.distribution.percentiles-histogram.http.server.requests` | Boolean | `true` | Request histograms |
| `management.metrics.tags.application` | String | `${spring.application.name}` | Application tag |
| `management.metrics.tags.environment` | String | `${spring.profiles.active}` | Environment tag |

### Tracing

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `management.tracing.sampling.probability` | Double | `1.0` | Trace sampling rate |
| `management.zipkin.tracing.endpoint` | String | `http://localhost:9411/api/v2/spans` | Zipkin endpoint |

## Integration Properties

### Message Queue (RabbitMQ)

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `spring.rabbitmq.host` | String | `localhost` | RabbitMQ host |
| `spring.rabbitmq.port` | Integer | `5672` | RabbitMQ port |
| `spring.rabbitmq.username` | String | `guest` | RabbitMQ username |
| `spring.rabbitmq.password` | String | `guest` | RabbitMQ password |
| `spring.rabbitmq.virtual-host` | String | `/` | Virtual host |

### Spring Cloud Bus

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `spring.cloud.bus.enabled` | Boolean | `true` | Enable Spring Cloud Bus |
| `spring.cloud.bus.refresh.enabled` | Boolean | `true` | Enable refresh events |
| `spring.cloud.bus.env.enabled` | Boolean | `true` | Enable env events |
| `spring.cloud.bus.ack.enabled` | Boolean | `true` | Enable acknowledgments |

### Resilience4j

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `resilience4j.circuitbreaker.instances.{name}.failure-rate-threshold` | Float | `50` | Failure rate threshold (%) |
| `resilience4j.circuitbreaker.instances.{name}.wait-duration-in-open-state` | Duration | `60s` | Wait in open state |
| `resilience4j.retry.instances.{name}.max-attempts` | Integer | `3` | Max retry attempts |
| `resilience4j.retry.instances.{name}.wait-duration` | Duration | `1s` | Wait between retries |

## Environment Variables Reference

Common environment variables used across services:

| Variable | Description | Example |
|----------|-------------|---------|
| `CONFIG_SERVER_URI` | Config Server URL | `http://mcp-config-server:8888` |
| `SPRING_PROFILES_ACTIVE` | Active profiles | `docker,production` |
| `CONFIG_ENCRYPTION_KEY` | Encryption key | Base64 encoded key |
| `DB_HOST` | Database host | `postgres` |
| `DB_PORT` | Database port | `5432` |
| `DB_NAME` | Database name | `mcp_db` |
| `DB_USER` | Database user | `mcp_user` |
| `DB_PASSWORD` | Database password | Encrypted value |
| `REDIS_HOST` | Redis host | `redis` |
| `REDIS_PORT` | Redis port | `6379` |
| `RABBITMQ_HOST` | RabbitMQ host | `rabbitmq` |
| `RABBITMQ_PORT` | RabbitMQ port | `5672` |

## Property Naming Conventions

1. **Prefix with service name**: `mcp.{service}.property`
2. **Use kebab-case**: `my-property-name`
3. **Group related properties**: `mcp.service.feature.property`
4. **Use meaningful names**: Avoid abbreviations
5. **Document all custom properties**: Include description and default

## Property Precedence

Properties are resolved in the following order (highest to lowest):

1. Command line arguments
2. Java System properties
3. OS environment variables
4. Config Server (profile-specific)
5. Config Server (default)
6. `application-{profile}.yml`
7. `application.yml`
8. `@PropertySource` annotations
9. Default values

## Best Practices

1. **Use placeholders for environment-specific values**:
   ```yaml
   database:
     url: ${DB_URL:jdbc:postgresql://localhost:5432/dev_db}
   ```

2. **Encrypt sensitive values**:
   ```yaml
   api:
     key: {cipher}AQBcAzBkDL...
   ```

3. **Provide meaningful defaults**:
   ```yaml
   cache:
     ttl: ${CACHE_TTL:3600}  # 1 hour default
   ```

4. **Group related properties**:
   ```yaml
   mcp:
     service:
       feature:
         enabled: true
         timeout: 5000
         retry-attempts: 3
   ```

5. **Document custom properties**:
   ```yaml
   # Maximum number of concurrent requests allowed
   mcp.service.rate-limit: ${RATE_LIMIT:100}