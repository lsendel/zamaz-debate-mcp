# OAuth2/SSO API Documentation

## Overview

The OAuth2/SSO API provides seamless integration with multiple identity providers including Google, Microsoft, GitHub, and custom OIDC providers. This enables users to authenticate using their existing accounts from these providers.

## Supported Providers

- **Google**: OAuth2 with OpenID Connect
- **Microsoft**: Azure AD OAuth2 with OpenID Connect
- **GitHub**: OAuth2 with user profile access
- **Custom OIDC**: Any OpenID Connect compliant provider

## Base URL

```
https://api.mcp.com/api/v1/oauth2
```

## Authentication Flow

### 1. Authorization Code Flow

The OAuth2 implementation uses the Authorization Code flow with PKCE (Proof Key for Code Exchange) for enhanced security.

```
1. Client redirects user to /oauth2/authorization/{provider}
2. User authenticates with the provider
3. Provider redirects back to /login/oauth2/code/{provider}
4. Server exchanges authorization code for access token
5. Server retrieves user information from provider
6. Server creates or updates user account
7. Server generates JWT token and redirects to success URL
```

### 2. JWT Token Integration

After successful OAuth2 authentication, the server generates a JWT token that can be used for API access.

## API Endpoints

### 1. Get Available Providers

Get a list of configured OAuth2 providers.

**Endpoint:** `GET /providers`

**Response (200 OK):**
```json
{
  "providers": [
    {
      "id": "google",
      "name": "Google",
      "loginUrl": "https://api.mcp.com/oauth2/authorization/google"
    },
    {
      "id": "microsoft",
      "name": "Microsoft",
      "loginUrl": "https://api.mcp.com/oauth2/authorization/microsoft"
    },
    {
      "id": "github",
      "name": "GitHub",
      "loginUrl": "https://api.mcp.com/oauth2/authorization/github"
    },
    {
      "id": "custom",
      "name": "Custom OIDC",
      "loginUrl": "https://api.mcp.com/oauth2/authorization/custom"
    }
  ]
}
```

### 2. Get Current OAuth2 User

Get current user information for OAuth2 authenticated users.

**Endpoint:** `GET /user`

**Authentication:** Required (Bearer token)

**Response (200 OK):**
```json
{
  "userId": "user-12345",
  "email": "john.doe@gmail.com",
  "firstName": "John",
  "lastName": "Doe",
  "emailVerified": true,
  "provider": "google",
  "roles": ["ROLE_USER", "OAUTH2_GOOGLE"],
  "organizations": ["org-123"],
  "lastLogin": "2025-01-17T10:30:00Z",
  "createdAt": "2025-01-15T08:00:00Z"
}
```

**Error Responses:**
- `401 Unauthorized`: Authentication required

### 3. Link OAuth2 Account

Link an OAuth2 account to an existing user account.

**Endpoint:** `POST /link`

**Authentication:** Required (Bearer token)

**Parameters:**
- `provider` (required): OAuth2 provider ID

**Request:**
```http
POST /api/v1/oauth2/link?provider=google
Authorization: Bearer <your-jwt-token>
```

**Response (200 OK):**
```json
{
  "message": "Account linking initiated",
  "linkUrl": "https://api.mcp.com/oauth2/authorization/google?link=true"
}
```

**Error Responses:**
- `400 Bad Request`: Invalid provider or account already linked
- `401 Unauthorized`: Authentication required

### 4. Unlink OAuth2 Account

Unlink an OAuth2 account from the current user account.

**Endpoint:** `DELETE /unlink`

**Authentication:** Required (Bearer token)

**Parameters:**
- `provider` (required): OAuth2 provider ID

**Request:**
```http
DELETE /api/v1/oauth2/unlink?provider=google
Authorization: Bearer <your-jwt-token>
```

**Response (200 OK):**
```json
{
  "message": "Account unlinked successfully"
}
```

**Error Responses:**
- `400 Bad Request`: Invalid provider or account not linked
- `401 Unauthorized`: Authentication required

### 5. OAuth2 Callback Handler

Handle OAuth2 callback and return JWT token.

**Endpoint:** `GET /callback`

**Parameters:**
- `code` (optional): Authorization code from provider
- `state` (optional): State parameter for CSRF protection
- `error` (optional): Error from provider

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "message": "Authentication successful"
}
```

**Error Response (400 Bad Request):**
```json
{
  "error": "access_denied",
  "message": "User denied access"
}
```

## Provider-Specific Details

### Google OAuth2

**Authorization URL:** `https://accounts.google.com/o/oauth2/v2/auth`

**Scopes:** `openid`, `profile`, `email`

**User Attributes Retrieved:**
- `sub` (User ID)
- `email` (Email address)
- `given_name` (First name)
- `family_name` (Last name)
- `name` (Full name)
- `picture` (Profile picture URL)
- `email_verified` (Email verification status)

**Configuration:**
```yaml
oauth2:
  google:
    client-id: ${GOOGLE_CLIENT_ID}
    client-secret: ${GOOGLE_CLIENT_SECRET}
    hosted-domain: ${GOOGLE_HOSTED_DOMAIN:} # Optional: restrict to domain
```

### Microsoft OAuth2

**Authorization URL:** `https://login.microsoftonline.com/common/oauth2/v2.0/authorize`

**Scopes:** `openid`, `profile`, `email`

**User Attributes Retrieved:**
- `sub` (User ID)
- `email` (Email address)
- `given_name` (First name)
- `family_name` (Last name)
- `name` (Full name)
- `preferred_username` (Username)

**Configuration:**
```yaml
oauth2:
  microsoft:
    client-id: ${MICROSOFT_CLIENT_ID}
    client-secret: ${MICROSOFT_CLIENT_SECRET}
    tenant-id: ${MICROSOFT_TENANT_ID:common}
```

### GitHub OAuth2

**Authorization URL:** `https://github.com/login/oauth/authorize`

**Scopes:** `user:email`, `read:user`

**User Attributes Retrieved:**
- `id` (User ID)
- `email` (Email address)
- `name` (Full name)
- `login` (Username)
- `avatar_url` (Profile picture URL)
- `company` (Company)
- `location` (Location)

**Configuration:**
```yaml
oauth2:
  github:
    client-id: ${GITHUB_CLIENT_ID}
    client-secret: ${GITHUB_CLIENT_SECRET}
```

### Custom OIDC Provider

**Configuration:**
```yaml
oauth2:
  custom:
    client-id: ${CUSTOM_CLIENT_ID}
    client-secret: ${CUSTOM_CLIENT_SECRET}
    issuer-uri: ${CUSTOM_ISSUER_URI}
    jwk-set-uri: ${CUSTOM_JWK_SET_URI}
    provider-name: ${CUSTOM_PROVIDER_NAME:Custom OIDC}
```

## Security Features

### 1. CSRF Protection

- State parameter validation
- CSRF token verification
- Secure redirect URI validation

### 2. PKCE (Proof Key for Code Exchange)

- Code challenge and verifier
- Enhanced security for public clients
- Protection against authorization code interception

### 3. Token Security

- JWT tokens with expiration
- Refresh token support
- Token revocation on logout

### 4. Rate Limiting

- Authentication attempts per IP: 10/minute
- Authentication attempts per user: 5/minute
- Account linking attempts: 5/hour

## Error Handling

### Common Error Responses

**Invalid Provider:**
```json
{
  "error": "invalid_provider",
  "message": "Unsupported OAuth2 provider: invalid_provider"
}
```

**Authentication Failed:**
```json
{
  "error": "authentication_failed",
  "message": "OAuth2 authentication failed: Invalid credentials"
}
```

**Account Already Linked:**
```json
{
  "error": "account_already_linked",
  "message": "This OAuth2 account is already linked to another user"
}
```

**Rate Limit Exceeded:**
```json
{
  "error": "rate_limit_exceeded",
  "message": "Too many authentication attempts. Please try again later."
}
```

## Frontend Integration

### JavaScript/React Example

```javascript
// Get available providers
const getProviders = async () => {
  const response = await fetch('/api/v1/oauth2/providers');
  const data = await response.json();
  return data.providers;
};

// Initiate OAuth2 login
const loginWithProvider = (provider) => {
  window.location.href = `/oauth2/authorization/${provider}`;
};

// Handle OAuth2 callback
const handleCallback = async () => {
  const urlParams = new URLSearchParams(window.location.search);
  const code = urlParams.get('code');
  const error = urlParams.get('error');
  
  if (error) {
    console.error('OAuth2 error:', error);
    return;
  }
  
  if (code) {
    const response = await fetch('/api/v1/oauth2/callback?' + urlParams.toString());
    const data = await response.json();
    
    if (data.token) {
      localStorage.setItem('jwt-token', data.token);
      window.location.href = '/dashboard';
    }
  }
};

// Get current user
const getCurrentUser = async () => {
  const token = localStorage.getItem('jwt-token');
  const response = await fetch('/api/v1/oauth2/user', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  if (response.ok) {
    return response.json();
  }
  
  throw new Error('Failed to get user');
};

// Link account
const linkAccount = async (provider) => {
  const token = localStorage.getItem('jwt-token');
  const response = await fetch(`/api/v1/oauth2/link?provider=${provider}`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  if (response.ok) {
    const data = await response.json();
    window.location.href = data.linkUrl;
  }
};

// Unlink account
const unlinkAccount = async (provider) => {
  const token = localStorage.getItem('jwt-token');
  const response = await fetch(`/api/v1/oauth2/unlink?provider=${provider}`, {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  if (response.ok) {
    const data = await response.json();
    console.log(data.message);
  }
};
```

### React Component Example

```jsx
import React, { useState, useEffect } from 'react';

const OAuth2Login = () => {
  const [providers, setProviders] = useState([]);
  const [user, setUser] = useState(null);

  useEffect(() => {
    fetchProviders();
    fetchCurrentUser();
  }, []);

  const fetchProviders = async () => {
    try {
      const response = await fetch('/api/v1/oauth2/providers');
      const data = await response.json();
      setProviders(data.providers);
    } catch (error) {
      console.error('Failed to fetch providers:', error);
    }
  };

  const fetchCurrentUser = async () => {
    try {
      const token = localStorage.getItem('jwt-token');
      if (!token) return;

      const response = await fetch('/api/v1/oauth2/user', {
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (response.ok) {
        const userData = await response.json();
        setUser(userData);
      }
    } catch (error) {
      console.error('Failed to fetch user:', error);
    }
  };

  const handleLogin = (provider) => {
    window.location.href = `/oauth2/authorization/${provider}`;
  };

  const handleLogout = () => {
    localStorage.removeItem('jwt-token');
    setUser(null);
  };

  if (user) {
    return (
      <div>
        <h2>Welcome, {user.firstName}!</h2>
        <p>Email: {user.email}</p>
        <p>Provider: {user.provider}</p>
        <button onClick={handleLogout}>Logout</button>
      </div>
    );
  }

  return (
    <div>
      <h2>Login with OAuth2</h2>
      {providers.map(provider => (
        <button
          key={provider.id}
          onClick={() => handleLogin(provider.id)}
          style={{ margin: '5px', padding: '10px' }}
        >
          Login with {provider.name}
        </button>
      ))}
    </div>
  );
};

export default OAuth2Login;
```

## Configuration

### Environment Variables

```bash
# Google OAuth2
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# Microsoft OAuth2
MICROSOFT_CLIENT_ID=your-microsoft-client-id
MICROSOFT_CLIENT_SECRET=your-microsoft-client-secret
MICROSOFT_TENANT_ID=common

# GitHub OAuth2
GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret

# Custom OIDC
CUSTOM_CLIENT_ID=your-custom-client-id
CUSTOM_CLIENT_SECRET=your-custom-client-secret
CUSTOM_ISSUER_URI=https://your-oidc-provider.com
CUSTOM_JWK_SET_URI=https://your-oidc-provider.com/certs

# General Configuration
OAUTH2_BASE_URL=https://api.mcp.com
OAUTH2_SUCCESS_REDIRECT_URI=https://app.mcp.com/dashboard
OAUTH2_FAILURE_REDIRECT_URI=https://app.mcp.com/login?error=oauth2
```

### Application Properties

```yaml
oauth2:
  google:
    client-id: ${GOOGLE_CLIENT_ID}
    client-secret: ${GOOGLE_CLIENT_SECRET}
  microsoft:
    client-id: ${MICROSOFT_CLIENT_ID}
    client-secret: ${MICROSOFT_CLIENT_SECRET}
  github:
    client-id: ${GITHUB_CLIENT_ID}
    client-secret: ${GITHUB_CLIENT_SECRET}
  custom:
    client-id: ${CUSTOM_CLIENT_ID}
    client-secret: ${CUSTOM_CLIENT_SECRET}
    issuer-uri: ${CUSTOM_ISSUER_URI}
    jwk-set-uri: ${CUSTOM_JWK_SET_URI}
```

## Testing

### Unit Tests

Run OAuth2 unit tests:

```bash
mvn test -Dtest=OAuth2UserServiceTest
mvn test -Dtest=OAuth2ControllerTest
mvn test -Dtest=OAuth2ConfigTest
```

### Integration Tests

Test OAuth2 integration with mock providers:

```bash
mvn test -Dtest=OAuth2IntegrationTest
```

### Manual Testing

1. Configure OAuth2 providers in your development environment
2. Start the application
3. Navigate to `/api/v1/oauth2/providers` to see available providers
4. Test login flow with each provider
5. Test account linking and unlinking

## Monitoring and Logging

### Metrics

OAuth2 metrics are available at `/actuator/metrics`:

- `oauth2.authentication.attempts`
- `oauth2.authentication.success`
- `oauth2.authentication.failures`
- `oauth2.account.linking`
- `oauth2.account.unlinking`

### Logging

OAuth2 events are logged with the following format:

```
2025-01-17 10:30:00 [oauth2-thread] INFO [user-123] [org-456] [google] OAuth2UserService - OAuth2 login successful for user: user-123
```

### Audit Events

OAuth2 operations are audited with the following event types:

- `OAUTH2_LOGIN_SUCCESS`
- `OAUTH2_LOGIN_FAILED`
- `OAUTH2_ACCOUNT_LINKED`
- `OAUTH2_ACCOUNT_UNLINKED`
- `OAUTH2_PROVIDER_ERROR`
- `OAUTH2_TOKEN_REFRESH`

## Troubleshooting

### Common Issues

**1. Invalid Client Configuration**
```
Error: invalid_client
Solution: Check client ID and secret configuration
```

**2. Redirect URI Mismatch**
```
Error: redirect_uri_mismatch
Solution: Ensure redirect URI matches provider configuration
```

**3. Insufficient Scopes**
```
Error: insufficient_scope
Solution: Check required scopes for user information
```

**4. Token Expiration**
```
Error: token_expired
Solution: Implement token refresh mechanism
```

### Debug Mode

Enable debug logging:

```yaml
logging:
  level:
    org.springframework.security.oauth2: DEBUG
    com.zamaz.mcp.gateway.service.OAuth2UserService: DEBUG
```

## Support

For OAuth2/SSO support:

- Documentation: https://docs.mcp.com/oauth2
- Support Email: oauth2-support@mcp.com
- Developer Portal: https://developers.mcp.com/oauth2

## Changelog

### v1.0.0 (2025-01-17)
- Initial OAuth2/SSO implementation
- Support for Google, Microsoft, GitHub, and custom OIDC providers
- Account linking and unlinking functionality
- Comprehensive security features
- Full API documentation and testing suite