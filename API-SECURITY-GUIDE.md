# MCP Gateway API Security Guide

**Version**: 1.0  
**Last Updated**: 2025-07-16  
**For Developers**: Building secure applications with MCP Gateway

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Authentication](#authentication)
3. [Authorization](#authorization)
4. [Rate Limiting](#rate-limiting)
5. [Security Headers](#security-headers)
6. [Error Handling](#error-handling)
7. [Best Practices](#best-practices)
8. [API Reference](#api-reference)

---

## 🎯 Overview

The MCP Gateway provides a comprehensive security layer for all API interactions. This guide covers how to properly authenticate, handle errors, and implement security best practices when using the API.

### Key Security Features

- **JWT-based Authentication**: Stateless token authentication
- **Multi-tenant Authorization**: Organization-scoped permissions  
- **Rate Limiting**: Prevent API abuse and DDoS attacks
- **Circuit Breakers**: Protect backend services from failures
- **Security Headers**: OWASP-compliant security headers
- **Audit Logging**: Comprehensive security event tracking

---

## 🔐 Authentication

### Authentication Flow

1. **Login** with username/email and password
2. **Receive** JWT access token and refresh token
3. **Include** access token in subsequent API calls
4. **Refresh** token when it expires

### Login Example

```bash
# Login request
curl -X POST "https://api.mcp.com/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe@example.com",
    "password": "SecurePassword123!",
    "organizationId": "org-123"
  }'
```

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": "user-123",
    "username": "john.doe",
    "email": "john.doe@example.com",
    "currentOrganizationId": "org-123",
    "organizations": [
      {
        "id": "org-123",
        "name": "Acme Corp",
        "role": "ADMIN"
      }
    ],
    "roles": ["USER", "ADMIN"],
    "permissions": ["DEBATE_CREATE", "DEBATE_READ", "DEBATE_UPDATE"]
  }
}
```

### Using Access Tokens

Include the access token in the `Authorization` header:

```bash
curl -X GET "https://api.mcp.com/api/v1/debates" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Token Refresh

```bash
curl -X POST "https://api.mcp.com/api/v1/auth/refresh" \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

---

## 🛡️ Authorization

### Permission-Based Access Control

API endpoints are protected by permissions. Users must have the required permissions within their current organization context.

### Common Permissions

| Permission | Description |
|------------|-------------|
| `DEBATE_CREATE` | Create new debates |
| `DEBATE_READ` | View debates |
| `DEBATE_UPDATE` | Modify debates |
| `DEBATE_DELETE` | Delete debates |
| `ORGANIZATION_ADMIN` | Organization administration |
| `SYSTEM_ADMIN` | System-wide administration |

### Organization Context

Switch between organizations using the switch endpoint:

```bash
curl -X POST "https://api.mcp.com/api/v1/auth/switch-organization" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId": "org-456"
  }'
```

---

## ⚡ Rate Limiting

### Rate Limit Headers

Every API response includes rate limiting information:

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1642680060000
```

### Default Limits

| User Type | Per Minute | Per Hour | Notes |
|-----------|------------|----------|-------|
| **Anonymous** | 60 | 1,000 | IP-based limiting |
| **Authenticated** | 120 | 2,000 | User-based limiting |
| **Auth Endpoints** | 5 | 20 | Stricter limits |

### Rate Limit Exceeded Response

```json
{
  "error": "Too Many Requests",
  "message": "Rate limit exceeded",
  "timestamp": 1642680000000
}
```

**HTTP Status**: `429 Too Many Requests`

### Best Practices

1. **Check Headers**: Monitor rate limit headers
2. **Implement Backoff**: Use exponential backoff on 429 responses
3. **Cache Responses**: Reduce unnecessary API calls
4. **Batch Requests**: Combine multiple operations when possible

---

## 🔒 Security Headers

### Required Security Headers

All API responses include OWASP-recommended security headers:

```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Referrer-Policy: strict-origin-when-cross-origin
Content-Security-Policy: default-src 'self'
Strict-Transport-Security: max-age=31536000; includeSubDomains
```

### Request Headers

Include these headers in your requests:

```bash
curl -X POST "https://api.mcp.com/api/v1/debates" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: org-123" \
  -H "X-Request-ID: $(uuidgen)"
```

---

## ❌ Error Handling

### Standard Error Response

```json
{
  "error": "Error Type",
  "message": "Human-readable error message",
  "details": {
    "field": "validation details"
  },
  "timestamp": 1642680000000,
  "requestId": "req-123"
}
```

### HTTP Status Codes

| Code | Meaning | Description |
|------|---------|-------------|
| `200` | OK | Request successful |
| `201` | Created | Resource created |
| `400` | Bad Request | Invalid request data |
| `401` | Unauthorized | Authentication required |
| `403` | Forbidden | Insufficient permissions |
| `404` | Not Found | Resource not found |
| `429` | Too Many Requests | Rate limit exceeded |
| `500` | Internal Server Error | Server error |
| `503` | Service Unavailable | Service temporarily down |

### Security-Specific Errors

#### Authentication Errors

```json
{
  "error": "Unauthorized",
  "message": "Invalid or expired token",
  "timestamp": 1642680000000
}
```

#### Authorization Errors

```json
{
  "error": "Forbidden",
  "message": "Insufficient permissions for this operation",
  "details": {
    "required": "DEBATE_CREATE",
    "organizationId": "org-123"
  },
  "timestamp": 1642680000000
}
```

#### Rate Limiting Errors

```json
{
  "error": "Too Many Requests",
  "message": "Rate limit exceeded",
  "details": {
    "limit": 100,
    "remaining": 0,
    "resetTime": 1642680060000
  },
  "timestamp": 1642680000000
}
```

---

## 🏆 Best Practices

### Authentication Best Practices

1. **Secure Token Storage**
   ```javascript
   // ✅ Good: Store in memory or secure storage
   localStorage.setItem('accessToken', token); // Only if HTTPS
   
   // ❌ Bad: Store in cookies without secure flags
   document.cookie = `token=${token}`;
   ```

2. **Token Refresh Strategy**
   ```javascript
   // Refresh token before expiration
   if (tokenExpiresIn < 5 * 60 * 1000) { // 5 minutes
     await refreshToken();
   }
   ```

3. **Logout Cleanup**
   ```javascript
   // Clear all tokens on logout
   localStorage.removeItem('accessToken');
   localStorage.removeItem('refreshToken');
   ```

### API Usage Best Practices

1. **Always Use HTTPS**
   ```bash
   # ✅ Good
   curl https://api.mcp.com/api/v1/debates
   
   # ❌ Bad
   curl http://api.mcp.com/api/v1/debates
   ```

2. **Include Request IDs**
   ```bash
   curl -H "X-Request-ID: $(uuidgen)" https://api.mcp.com/api/v1/debates
   ```

3. **Handle Rate Limits**
   ```javascript
   async function apiCall(url, options) {
     try {
       const response = await fetch(url, options);
       
       if (response.status === 429) {
         const retryAfter = response.headers.get('Retry-After');
         await sleep(retryAfter * 1000);
         return apiCall(url, options); // Retry
       }
       
       return response;
     } catch (error) {
       // Handle network errors
       throw error;
     }
   }
   ```

### Security Best Practices

1. **Validate Input Client-Side**
   ```javascript
   // Basic validation before API calls
   if (!email.includes('@')) {
     throw new Error('Invalid email format');
   }
   ```

2. **Implement CSRF Protection**
   ```html
   <!-- Include CSRF token in forms -->
   <meta name="csrf-token" content="{{ csrf_token() }}">
   ```

3. **Use Content Security Policy**
   ```html
   <meta http-equiv="Content-Security-Policy" 
         content="default-src 'self'; connect-src 'self' https://api.mcp.com">
   ```

---

## 📚 API Reference

### Authentication Endpoints

#### POST /api/v1/auth/login
Authenticate user and receive tokens.

**Request:**
```json
{
  "username": "string",
  "password": "string",
  "organizationId": "string" // optional
}
```

**Response:** `AuthResponse`

#### POST /api/v1/auth/register
Register new user account.

**Request:**
```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "firstName": "string",
  "lastName": "string",
  "organizationId": "string", // optional
  "organizationName": "string" // optional
}
```

**Response:** `AuthResponse`

#### POST /api/v1/auth/refresh
Refresh access token.

**Request:**
```json
{
  "refreshToken": "string"
}
```

**Response:** `AuthResponse`

#### POST /api/v1/auth/logout
Logout and invalidate tokens.

**Headers:** `Authorization: Bearer <token>`

**Response:** `204 No Content`

#### GET /api/v1/auth/me
Get current user information.

**Headers:** `Authorization: Bearer <token>`

**Response:** `UserInfoResponse`

### Security Management Endpoints

#### GET /api/v1/security/rate-limit/status
Get rate limiting status (Admin only).

**Response:**
```json
{
  "violations": {
    "ip:192.168.1.100": 3
  },
  "blacklisted": [
    {
      "clientId": "ip:192.168.1.101",
      "reason": "Too many violations",
      "expiresIn": 3600
    }
  ],
  "timestamp": 1642680000000
}
```

#### POST /api/v1/security/block-ip
Manually block an IP address (Admin only).

**Parameters:**
- `ip`: IP address to block
- `reason`: Reason for blocking
- `durationSeconds`: Block duration

---

## 🔍 Testing Your Integration

### Authentication Test

```bash
#!/bin/bash
# Test authentication flow

# 1. Login
RESPONSE=$(curl -s -X POST "https://api.mcp.com/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test@example.com",
    "password": "TestPassword123!"
  }')

TOKEN=$(echo $RESPONSE | jq -r '.accessToken')

# 2. Use token
curl -X GET "https://api.mcp.com/api/v1/auth/me" \
  -H "Authorization: Bearer $TOKEN"

# 3. Test invalid token
curl -X GET "https://api.mcp.com/api/v1/auth/me" \
  -H "Authorization: Bearer invalid-token"
```

### Rate Limiting Test

```bash
#!/bin/bash
# Test rate limiting

for i in {1..200}; do
  curl -s -o /dev/null -w "%{http_code}\n" \
    "https://api.mcp.com/api/v1/health"
done
# Should see 429 responses after hitting limit
```

---

## 📞 Support

### Getting Help

- **Documentation**: https://docs.mcp.com
- **API Status**: https://status.mcp.com
- **Developer Support**: dev-support@mcp.com

### Security Issues

Report security vulnerabilities to: security@mcp.com

### Rate Limit Issues

Contact support with your client ID and timestamp of rate limit errors.

---

**Remember**: Always use HTTPS in production and never expose access tokens in client-side code or logs.