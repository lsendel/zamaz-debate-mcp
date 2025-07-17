# API Gateway Routing and Load Balancing Documentation

## Overview

The MCP API Gateway provides comprehensive routing, load balancing, and traffic management for all microservices in the system. It acts as the single entry point for all client requests, handling authentication, rate limiting, circuit breaking, and intelligent routing.

## Architecture

```
┌─────────────┐     ┌──────────────────────────────────────┐     ┌─────────────────┐
│   Clients   │────▶│         API Gateway (8080)           │────▶│  Microservices  │
└─────────────┘     │                                      │     └─────────────────┘
                    │  • Routing & Load Balancing          │     
                    │  • Authentication & Authorization    │     ┌─────────────────┐
                    │  • Rate Limiting                     │────▶│ Organization    │
                    │  • Circuit Breaking                  │     │ Service (5005)  │
                    │  • Request/Response Caching          │     └─────────────────┘
                    │  • Request Validation               │     
                    │  • Logging & Monitoring             │     ┌─────────────────┐
                    └──────────────────────────────────────┘────▶│ Debate Service  │
                                                                 │    (5013)       │
                                                                 └─────────────────┘
```

## Service Routes

### 1. Health Check Routes

**Path:** `/health`, `/actuator/health/**`  
**Authentication:** Not required  
**Rate Limit:** None  

```yaml
Example:
GET /health
GET /actuator/health/liveness
GET /actuator/health/readiness
```

### 2. Organization Service Routes

**Path:** `/api/v*/organizations/**`  
**Service URL:** `http://localhost:5005`  
**Authentication:** Required  
**Rate Limit:** 100 req/sec (burst: 200)  
**Circuit Breaker:** Enabled  
**Retry:** 3 attempts for GET requests  

```yaml
Examples:
GET /api/v1/organizations
POST /api/v1/organizations
GET /api/v1/organizations/{orgId}
PUT /api/v1/organizations/{orgId}
DELETE /api/v1/organizations/{orgId}
```

### 3. Debate Controller Service Routes

**Path:** `/api/v*/debates/**`  
**Service URL:** `http://localhost:5013`  
**Authentication:** Required  
**Rate Limit:** 100 req/sec (burst: 200)  
**Circuit Breaker:** Enabled  
**Cache:** 5 minutes (org-specific)  
**Retry:** 2 attempts on 503 errors  

```yaml
Examples:
GET /api/v1/debates
POST /api/v1/debates
GET /api/v1/debates/{debateId}
PUT /api/v1/debates/{debateId}
POST /api/v1/debates/{debateId}/rounds
```

### 4. LLM Service Routes (Load Balanced)

**Path:** `/api/v*/llm/**`, `/api/v*/providers/**`  
**Service URL:** Load balanced across multiple instances  
**Authentication:** Required  
**Rate Limit:** 10 req/sec (burst: 20) - Lower due to resource intensity  
**Circuit Breaker:** Enabled (30s slow call threshold)  
**Load Balancing:** Round-robin  

```yaml
Examples:
GET /api/v1/providers
POST /api/v1/llm/generate
POST /api/v1/llm/chat
GET /api/v1/llm/models
```

### 5. RAG Service Routes

**Path:** `/api/v*/rag/**`, `/api/v*/search/**`  
**Service URL:** `http://localhost:5004`  
**Authentication:** Required  
**Rate Limit:** 100 req/sec (burst: 200)  
**Circuit Breaker:** Enabled  
**Cache:** 15 minutes (user and org-specific)  

```yaml
Examples:
POST /api/v1/rag/index
POST /api/v1/search
GET /api/v1/rag/documents/{docId}
DELETE /api/v1/rag/documents/{docId}
```

### 6. Template Service Routes

**Path:** `/api/v*/templates/**`  
**Service URL:** `http://localhost:5006`  
**Authentication:** Required  
**Rate Limit:** 100 req/sec (burst: 200)  
**Cache:** 1 hour (org-specific)  

```yaml
Examples:
GET /api/v1/templates
POST /api/v1/templates
GET /api/v1/templates/{templateId}
PUT /api/v1/templates/{templateId}
```

### 7. Context Service Routes

**Path:** `/api/v*/contexts/**`  
**Service URL:** `http://localhost:5007`  
**Authentication:** Required  
**Rate Limit:** 100 req/sec (burst: 200)  
**Circuit Breaker:** Enabled  

```yaml
Examples:
GET /api/v1/contexts/{contextId}
POST /api/v1/contexts
PUT /api/v1/contexts/{contextId}
DELETE /api/v1/contexts/{contextId}
```

### 8. Security Service Routes

**Path:** `/api/v*/auth/**`, `/api/v*/users/**`  
**Service URL:** `http://localhost:5008`  
**Authentication:** Not required for auth endpoints  
**Rate Limit:** 5 req/min (strict for security)  

```yaml
Examples:
POST /api/v1/auth/login
POST /api/v1/auth/logout
POST /api/v1/auth/refresh
POST /api/v1/users/register
POST /api/v1/users/forgot-password
```

### 9. OAuth2/SSO Routes

**Path:** `/oauth2/**`, `/login/oauth2/**`  
**Service URL:** `http://localhost:5008`  
**Authentication:** Special handling for OAuth2 flow  

```yaml
Examples:
GET /oauth2/authorization/google
GET /oauth2/authorization/microsoft
GET /login/oauth2/code/google
```

### 10. WebSocket Routes

**Path:** `/ws/**`  
**Service URL:** `ws://localhost:5014`  
**Authentication:** Required  
**Protocol:** WebSocket  

```yaml
Examples:
ws://gateway/ws/debates/{debateId}
ws://gateway/ws/notifications
```

## Load Balancing Strategies

### 1. Round-Robin Load Balancing

Used for stateless services like LLM service:

```java
Services using Round-Robin:
- llm-service
- rag-service (when scaled)
```

**Algorithm:**
- Requests are distributed evenly across all healthy instances
- Each instance gets requests in sequential order
- Wraps around to first instance after reaching the last

### 2. Least Connections Load Balancing

Used for stateful or long-running services:

```java
Services using Least Connections:
- debate-service
- context-service (when scaled)
```

**Algorithm:**
- Routes requests to instance with fewest active connections
- Tracks connection count per instance
- Better for services with varying request processing times

### 3. Sticky Sessions

Enabled for services requiring session affinity:

```java
Services with Sticky Sessions:
- debate-service (active debates)
- websocket connections
```

## Circuit Breaker Configuration

### Default Settings

```yaml
failure-rate-threshold: 50%
slow-call-rate-threshold: 50%
slow-call-duration-threshold: 3s
sliding-window-size: 100
minimum-number-of-calls: 10
wait-duration-in-open-state: 60s
permitted-number-of-calls-in-half-open-state: 10
```

### Service-Specific Settings

**LLM Service:**
```yaml
slow-call-duration-threshold: 30s
failure-rate-threshold: 30%
```

**RAG Service:**
```yaml
slow-call-duration-threshold: 10s
failure-rate-threshold: 40%
```

## Rate Limiting

### Default Rate Limits

```yaml
Default: 100 requests/second (burst: 200)
Auth endpoints: 5 requests/minute (burst: 10)
LLM endpoints: 10 requests/minute (burst: 20)
Public endpoints: 50 requests/second (burst: 100)
```

### Rate Limiting Keys

1. **User-based:** Uses `X-User-ID` header
2. **IP-based:** Uses client IP address
3. **API Key-based:** Uses `X-API-Key` header

## Caching Strategy

### Cache Configuration

```yaml
debates:
  ttl: 5m
  user-specific: false
  org-specific: true

templates:
  ttl: 1h
  user-specific: false
  org-specific: true

rag-search:
  ttl: 15m
  user-specific: true
  org-specific: true

api-docs:
  ttl: 24h
  user-specific: false
  org-specific: false
```

### Cache Keys

Format: `{cacheName}:{method}:{path}[:user:{userId}][:org:{orgId}]`

Example: `debates:GET:/api/v1/debates:org:org-123`

## Request Validation

### Validation Rules

1. **Organization ID:** Required for org-specific endpoints
2. **Content-Type:** Must be `application/json` for POST/PUT
3. **Request Size:** Maximum 10MB (configurable)
4. **ID Format:** UUID or custom format (e.g., `org-123`)
5. **SQL Injection:** Pattern detection in headers and query params

### Validation Errors

```json
{
  "error": "Validation Error",
  "message": "Missing organization ID",
  "timestamp": 1642680000000,
  "path": "/api/v1/debates"
}
```

## API Versioning

### Version Headers

All requests include API version header:
```
X-API-Version: 1.0
```

### Version-Specific Routes

```yaml
/api/v1/** - Version 1 (current)
/api/v2/** - Version 2 (with breaking changes)
```

### Version Migration

Clients should:
1. Always specify version in URL
2. Monitor deprecation headers
3. Test against new versions before switching

## Canary Deployments

### Canary Configuration

```yaml
Routes:
- llm_canary: Routes to canary deployment when X-Canary: true
- ab_test_route: 10% traffic to new version
```

### A/B Testing

```yaml
Weight Distribution:
- 90% to stable version
- 10% to new version
- Based on consistent hashing of user ID
```

## Monitoring and Metrics

### Available Metrics

```
gateway.requests.total
gateway.requests.duration
gateway.requests.errors
gateway.circuit-breaker.state
gateway.rate-limit.remaining
gateway.cache.hits
gateway.cache.misses
```

### Health Endpoints

```
/actuator/health
/actuator/metrics
/actuator/prometheus
/actuator/gateway
/actuator/circuitbreakers
```

## Error Handling

### Gateway Errors

| Status Code | Description | Example |
|------------|-------------|---------|
| 400 | Bad Request | Invalid request format |
| 401 | Unauthorized | Missing or invalid token |
| 403 | Forbidden | Insufficient permissions |
| 429 | Too Many Requests | Rate limit exceeded |
| 502 | Bad Gateway | Service unavailable |
| 503 | Service Unavailable | Circuit breaker open |
| 504 | Gateway Timeout | Request timeout |

### Error Response Format

```json
{
  "timestamp": "2025-01-17T10:30:00Z",
  "status": 503,
  "error": "Service Unavailable",
  "message": "Circuit breaker open for service: llm-service",
  "path": "/api/v1/llm/generate",
  "requestId": "abc-123-def-456"
}
```

## Security Features

### Request Headers

Required headers for authenticated requests:
```
Authorization: Bearer <jwt-token>
X-Organization-ID: org-123
X-Request-ID: unique-request-id
```

### Response Headers

Security headers added by gateway:
```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Strict-Transport-Security: max-age=31536000
```

## Client Configuration

### Retry Configuration

Clients should implement:
- Exponential backoff
- Maximum 3 retry attempts
- Respect `Retry-After` header

### Timeout Configuration

Recommended client timeouts:
- Connect timeout: 5 seconds
- Read timeout: 30 seconds (60s for LLM)
- Write timeout: 10 seconds

### Connection Pooling

Recommended settings:
- Max connections: 100
- Max connections per route: 20
- Connection timeout: 5 seconds
- Keep-alive: 60 seconds

## Troubleshooting

### Common Issues

1. **503 Service Unavailable**
   - Check if service is running
   - Check circuit breaker status
   - Review service health endpoint

2. **429 Too Many Requests**
   - Check rate limit configuration
   - Implement client-side rate limiting
   - Use exponential backoff

3. **504 Gateway Timeout**
   - Increase timeout for long-running requests
   - Check service performance
   - Consider async processing

### Debug Headers

Enable debug mode with:
```
X-Debug-Mode: true
X-Debug-Level: TRACE
```

### Logging

Gateway logs include:
- Request ID
- User ID
- Organization ID
- Response time
- Error details

## Performance Optimization

### Best Practices

1. **Use caching** for read-heavy endpoints
2. **Batch requests** when possible
3. **Compress large payloads**
4. **Use appropriate timeouts**
5. **Monitor circuit breaker status**

### Performance Metrics

Target SLAs:
- P50 latency: < 100ms
- P95 latency: < 500ms
- P99 latency: < 1000ms
- Availability: 99.9%

## Migration Guide

### From Direct Service Access

1. Update base URL to gateway
2. Add authentication headers
3. Handle gateway-specific errors
4. Implement retry logic
5. Monitor rate limits

### API Version Migration

1. Test against new version
2. Update client code
3. Monitor for deprecation warnings
4. Switch version in production
5. Remove old version code