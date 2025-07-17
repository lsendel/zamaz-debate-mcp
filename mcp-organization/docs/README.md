# MCP-Organization Service Documentation

The MCP-Organization service manages organizations and multi-tenant functionality in the Zamaz Debate MCP system. It provides the foundation for multi-tenant isolation and organization management.

## Overview

The MCP-Organization service handles organization creation, management, and authentication. It maintains organization data, API keys, and user associations, ensuring complete isolation between different organizations using the system.

## Features

- **Organization Management**: Create, update, and manage organizations
- **API Key Management**: Generate and validate API keys
- **User Management**: Associate users with organizations
- **Role-based Access Control**: Manage permissions within organizations
- **Multi-tenant Isolation**: Ensure data isolation between organizations
- **Usage Tracking**: Monitor and track organization usage
- **Billing Tiers**: Support for different service tiers

## Architecture

The Organization service follows a clean architecture pattern:

- **Controllers**: Handle HTTP requests and responses
- **Services**: Implement business logic for organization management
- **Repositories**: Manage data persistence
- **Models**: Define organization-related data structures
- **Security**: Implement authentication and authorization

## API Endpoints

### Organizations

- `POST /api/v1/organizations`: Create organization
- `GET /api/v1/organizations`: List organizations
- `GET /api/v1/organizations/{id}`: Get organization details
- `PUT /api/v1/organizations/{id}`: Update organization
- `DELETE /api/v1/organizations/{id}`: Delete organization

### API Keys

- `POST /api/v1/organizations/{id}/api-keys`: Create API key
- `GET /api/v1/organizations/{id}/api-keys`: List API keys
- `GET /api/v1/organizations/{id}/api-keys/{keyId}`: Get API key details
- `DELETE /api/v1/organizations/{id}/api-keys/{keyId}`: Revoke API key
- `POST /api/v1/auth/verify-key`: Verify API key

### Users

- `POST /api/v1/organizations/{id}/users`: Add user to organization
- `GET /api/v1/organizations/{id}/users`: List organization users
- `PUT /api/v1/organizations/{id}/users/{userId}`: Update user role
- `DELETE /api/v1/organizations/{id}/users/{userId}`: Remove user from organization

### Authentication

- `POST /api/v1/auth/login`: User login
- `POST /api/v1/auth/refresh`: Refresh JWT token
- `POST /api/v1/auth/logout`: User logout

### MCP Tools

The service exposes the following MCP tools:

- `create_organization`: Create new organization
- `get_organization`: Get organization details
- `update_organization`: Update organization
- `list_organizations`: List organizations
- `create_api_key`: Create new API key
- `verify_api_key`: Verify API key validity
- `add_user`: Add user to organization
- `list_users`: List organization users

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | postgres |
| `DB_PORT` | PostgreSQL port | 5432 |
| `DB_NAME` | PostgreSQL database name | organization_db |
| `DB_USER` | PostgreSQL username | postgres |
| `DB_PASSWORD` | PostgreSQL password | postgres |
| `REDIS_HOST` | Redis host | redis |
| `REDIS_PORT` | Redis port | 6379 |
| `JWT_SECRET` | Secret for JWT token generation | your-256-bit-secret-key-for-jwt-token-generation |
| `SERVER_PORT` | Server port | 5005 |
| `CORS_ORIGINS` | Allowed CORS origins | http://localhost:3000,http://localhost:3001 |

### Organization Configuration

Organization-specific settings can be configured in `config/organization.yml`:

```yaml
organization:
  tiers:
    basic:
      max_api_keys: 3
      max_users: 5
      rate_limits:
        requests_per_minute: 30
        requests_per_day: 5000
      features:
        rag_enabled: false
        custom_templates: false
    pro:
      max_api_keys: 10
      max_users: 20
      rate_limits:
        requests_per_minute: 60
        requests_per_day: 10000
      features:
        rag_enabled: true
        custom_templates: true
    enterprise:
      max_api_keys: 50
      max_users: 100
      rate_limits:
        requests_per_minute: 120
        requests_per_day: 50000
      features:
        rag_enabled: true
        custom_templates: true
        dedicated_support: true
  
  default_tier: "basic"
  
  security:
    api_key_expiration_days: 90
    jwt_expiration_minutes: 60
    refresh_token_expiration_days: 30
    password_min_length: 12
    require_mfa: false
```

## Usage Examples

### Create an Organization

```bash
curl -X POST http://localhost:5005/api/v1/organizations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer admin-token" \
  -d '{
    "name": "Acme Corporation",
    "tier": "pro",
    "contactEmail": "admin@acmecorp.com",
    "settings": {
      "defaultLanguage": "en",
      "timezone": "America/New_York",
      "allowedModels": ["claude-3-opus", "gpt-4"]
    }
  }'
```

### Create an API Key

```bash
curl -X POST http://localhost:5005/api/v1/organizations/org-123/api-keys \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer admin-token" \
  -d '{
    "name": "Production API Key",
    "expiresAt": "2026-07-16T00:00:00Z",
    "scopes": ["debate:read", "debate:write", "llm:complete"]
  }'
```

### Verify API Key

```bash
curl -X POST http://localhost:5005/api/v1/auth/verify-key \
  -H "Content-Type: application/json" \
  -d '{
    "apiKey": "key-abcdef123456",
    "requestedScopes": ["debate:read", "llm:complete"]
  }'
```

### Add User to Organization

```bash
curl -X POST http://localhost:5005/api/v1/organizations/org-123/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer admin-token" \
  -d '{
    "email": "user@example.com",
    "role": "member",
    "permissions": ["debate:read", "debate:write"]
  }'
```

## Data Models

### Organization

```json
{
  "id": "org-123",
  "name": "Acme Corporation",
  "tier": "pro",
  "contactEmail": "admin@acmecorp.com",
  "settings": {
    "defaultLanguage": "en",
    "timezone": "America/New_York",
    "allowedModels": ["claude-3-opus", "gpt-4"]
  },
  "usage": {
    "currentMonthRequests": 5280,
    "currentMonthTokens": 1250000
  },
  "createdAt": "2025-01-15T10:30:00Z",
  "updatedAt": "2025-07-10T14:22:15Z"
}
```

### API Key

```json
{
  "id": "key-456",
  "organizationId": "org-123",
  "name": "Production API Key",
  "key": "sk-org123-abcdef123456",
  "scopes": ["debate:read", "debate:write", "llm:complete"],
  "createdAt": "2025-06-15T10:30:00Z",
  "expiresAt": "2026-07-16T00:00:00Z",
  "lastUsedAt": "2025-07-16T14:22:15Z",
  "status": "active"
}
```

### User

```json
{
  "id": "user-789",
  "email": "user@example.com",
  "name": "John Doe",
  "organizationId": "org-123",
  "role": "admin",
  "permissions": ["debate:read", "debate:write", "debate:admin"],
  "createdAt": "2025-02-10T09:15:00Z",
  "lastLoginAt": "2025-07-15T11:42:30Z",
  "status": "active"
}
```

## Authentication and Authorization

### JWT Authentication

The service uses JWT (JSON Web Token) for authentication:

1. User logs in with credentials
2. Service issues JWT token with organization claims
3. Token is used for subsequent API requests
4. Token includes organization ID and user permissions

### API Key Authentication

For service-to-service communication:

1. Service includes API key in request
2. Organization service validates API key
3. If valid, request is authorized based on key scopes
4. Usage is tracked against organization quota

### Role-Based Access Control

The service supports the following roles:

- **Owner**: Full access to organization settings and users
- **Admin**: Manage organization resources and users
- **Member**: Use organization resources
- **Guest**: Limited access to specific resources

## Multi-tenant Isolation

The Organization service implements strict multi-tenant isolation:

- Each organization's data is completely isolated
- API requests require organization identification
- Database queries filter by organization ID
- Rate limiting is applied per organization
- Resources are scoped to organizations

## Monitoring and Metrics

The service exposes the following metrics:

- Organization count
- API key count per organization
- User count per organization
- Authentication success/failure rate
- API key usage patterns

Access metrics at: `http://localhost:5005/actuator/metrics`

## Troubleshooting

### Common Issues

1. **Authentication Failures**
   - Check API key validity and expiration
   - Verify JWT token is not expired
   - Ensure correct scopes for the requested operation

2. **Rate Limiting Issues**
   - Check organization tier limits
   - Monitor usage patterns
   - Consider upgrading organization tier

3. **Database Issues**
   - Verify database connection
   - Check for schema migration issues
   - Monitor database performance

### Logs

Service logs can be accessed via:

```bash
docker-compose logs mcp-organization
```

## Development

### Building the Service

```bash
cd mcp-organization
mvn clean install
```

### Running Tests

```bash
cd mcp-organization
mvn test
```

### Local Development

```bash
cd mcp-organization
mvn spring-boot:run
```

## Advanced Features

### Custom Organization Settings

Configure custom organization settings:

```bash
curl -X PUT http://localhost:5005/api/v1/organizations/org-123/settings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer admin-token" \
  -d '{
    "customBranding": {
      "logoUrl": "https://example.com/logo.png",
      "primaryColor": "#3366CC",
      "secondaryColor": "#FF9900"
    },
    "securitySettings": {
      "requireMfa": true,
      "sessionTimeoutMinutes": 30,
      "allowedIpRanges": ["192.168.1.0/24", "10.0.0.0/8"]
    },
    "integrationSettings": {
      "slackWebhookUrl": "https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXX",
      "jiraApiKey": "jira-api-key-123"
    }
  }'
```

### Usage Reporting

Generate organization usage reports:

```bash
curl -X GET http://localhost:5005/api/v1/organizations/org-123/usage-report \
  -H "Authorization: Bearer admin-token" \
  -d '{
    "startDate": "2025-06-01T00:00:00Z",
    "endDate": "2025-07-01T00:00:00Z",
    "groupBy": "day",
    "includeServices": ["llm", "debate", "rag"]
  }'
```

### Bulk User Management

Manage multiple users at once:

```bash
curl -X POST http://localhost:5005/api/v1/organizations/org-123/users/bulk \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer admin-token" \
  -d '{
    "users": [
      {"email": "user1@example.com", "role": "member"},
      {"email": "user2@example.com", "role": "member"},
      {"email": "user3@example.com", "role": "admin"}
    ],
    "sendInvitations": true
  }'
```
