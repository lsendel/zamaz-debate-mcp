# User Management API Documentation

## Overview

The User Management API provides comprehensive functionality for user registration, authentication, profile management, and account lifecycle operations in the MCP Platform.

## Base URL

```
https://api.mcp.com/api/v1/users
```

## Authentication

Most endpoints require authentication via Bearer token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

## Rate Limits

- Registration: 5 requests per IP per hour
- Login attempts: 10 requests per IP per minute
- Password reset: 3 requests per email per hour
- Email verification resend: 3 requests per user per hour

## Endpoints

### 1. User Registration

Register a new user account with email verification.

**Endpoint:** `POST /register`

**Request Body:**
```json
{
  "email": "john.doe@example.com",
  "password": "SecurePassword123!",
  "firstName": "John",
  "lastName": "Doe",
  "organizationId": "org-123",
  "acceptTerms": true,
  "acceptPrivacyPolicy": true
}
```

**Response (201 Created):**
```json
{
  "userId": "user-12345",
  "email": "john.doe@example.com",
  "message": "Registration successful. Please check your email to verify your account.",
  "verificationRequired": true
}
```

**Error Responses:**
- `400 Bad Request`: Invalid data or email already exists
- `422 Unprocessable Entity`: Validation errors

**Password Requirements:**
- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one number
- At least one special character

### 2. Email Verification

Verify email address using verification token sent via email.

**Endpoint:** `POST /verify-email`

**Parameters:**
- `token` (required): Email verification token

**Response (200 OK):**
```json
{
  "message": "Email verified successfully",
  "verified": true
}
```

**Error Responses:**
- `400 Bad Request`: Invalid or expired token

### 3. Password Reset Request

Initiate password reset process by sending reset email.

**Endpoint:** `POST /forgot-password`

**Parameters:**
- `email` (required): User's email address

**Response (200 OK):**
```json
{
  "message": "If the email address exists in our system, a password reset link has been sent."
}
```

**Note:** Response is the same regardless of whether email exists (security measure).

### 4. Password Reset

Reset password using reset token and new password.

**Endpoint:** `POST /reset-password`

**Parameters:**
- `token` (required): Password reset token
- `newPassword` (required): New password

**Response (200 OK):**
```json
{
  "message": "Password reset successfully"
}
```

**Error Responses:**
- `400 Bad Request`: Invalid token or weak password

### 5. Get User Profile

Get current user's profile information.

**Endpoint:** `GET /profile`

**Authentication:** Required

**Response (200 OK):**
```json
{
  "userId": "user-12345",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "emailVerified": true,
  "createdAt": "2025-01-15T10:30:00Z",
  "lastLoginAt": "2025-01-17T09:15:00Z",
  "organizationIds": ["org-123"]
}
```

**Error Responses:**
- `401 Unauthorized`: Authentication required
- `404 Not Found`: User not found

### 6. Update User Profile

Update current user's profile information.

**Endpoint:** `PUT /profile`

**Authentication:** Required

**Request Body:**
```json
{
  "firstName": "Jane",
  "lastName": "Smith"
}
```

**Response (200 OK):**
```json
{
  "userId": "user-12345",
  "email": "john.doe@example.com",
  "firstName": "Jane",
  "lastName": "Smith",
  "emailVerified": true,
  "createdAt": "2025-01-15T10:30:00Z",
  "lastLoginAt": "2025-01-17T09:15:00Z",
  "organizationIds": ["org-123"]
}
```

**Error Responses:**
- `400 Bad Request`: Invalid profile data
- `401 Unauthorized`: Authentication required

### 7. Change Password

Change current user's password.

**Endpoint:** `POST /change-password`

**Authentication:** Required

**Request Body:**
```json
{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewPassword456!",
  "confirmPassword": "NewPassword456!"
}
```

**Response (200 OK):**
```json
{
  "message": "Password changed successfully"
}
```

**Error Responses:**
- `400 Bad Request`: Invalid current password or password mismatch
- `401 Unauthorized`: Authentication required

### 8. Deactivate Account

Deactivate current user's account.

**Endpoint:** `POST /deactivate`

**Authentication:** Required

**Parameters:**
- `reason` (optional): Reason for deactivation

**Response (200 OK):**
```json
{
  "message": "Account has been deactivated successfully",
  "deactivatedAt": "2025-01-17T10:45:00Z"
}
```

**Error Responses:**
- `401 Unauthorized`: Authentication required

### 9. Resend Email Verification

Resend email verification for current user.

**Endpoint:** `POST /resend-verification`

**Authentication:** Required

**Response (200 OK):**
```json
{
  "message": "Verification email sent successfully"
}
```

**Error Responses:**
- `400 Bad Request`: Email already verified
- `401 Unauthorized`: Authentication required

## Admin Endpoints

### 10. Get User Profile by ID (Admin)

Get any user's profile information (admin only).

**Endpoint:** `GET /{userId}/profile`

**Authentication:** Required (Admin role)

**Response (200 OK):**
```json
{
  "userId": "user-67890",
  "email": "target.user@example.com",
  "firstName": "Target",
  "lastName": "User",
  "emailVerified": true,
  "createdAt": "2025-01-10T08:00:00Z",
  "lastLoginAt": "2025-01-16T14:20:00Z",
  "organizationIds": ["org-456"]
}
```

**Error Responses:**
- `403 Forbidden`: Admin access required
- `404 Not Found`: User not found

### 11. Deactivate User Account (Admin)

Deactivate any user's account (admin only).

**Endpoint:** `POST /{userId}/deactivate`

**Authentication:** Required (Admin role)

**Parameters:**
- `reason` (required): Reason for deactivation

**Response (200 OK):**
```json
{
  "message": "Account has been deactivated successfully",
  "deactivatedAt": "2025-01-17T11:00:00Z"
}
```

**Error Responses:**
- `403 Forbidden`: Admin access required
- `404 Not Found`: User not found

## Error Response Format

All error responses follow this format:

```json
{
  "error": "Error Type",
  "message": "Detailed error message",
  "timestamp": 1642680000000
}
```

## Security Features

### Password Policy
- Minimum 8 characters
- Must contain uppercase, lowercase, numbers, and special characters
- Password history prevention (last 5 passwords)
- Maximum age: 90 days
- Expiry warnings: 7 days before

### Account Lockout
- Maximum failed attempts: 5
- Lockout duration: 30 minutes (with exponential backoff)
- Maximum lockout duration: 24 hours

### Token Security
- Email verification tokens expire in 24 hours
- Password reset tokens expire in 1 hour
- Tokens are cryptographically secure and single-use

### Rate Limiting
- Per-IP and per-user rate limits
- Exponential backoff for repeated failures
- Suspicious activity detection

## Audit Logging

All user management operations are logged for security audit:

- User registration and verification
- Password changes and resets
- Profile updates
- Account deactivation
- Failed authentication attempts
- Admin operations

## GDPR Compliance

The API supports GDPR requirements:

- Data export functionality
- Account deletion with data removal
- Consent tracking for terms and privacy policy
- Data retention policies
- Right to be forgotten

## Integration Examples

### JavaScript/Node.js

```javascript
// User registration
const registerUser = async (userData) => {
  const response = await fetch('/api/v1/users/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(userData)
  });
  
  if (!response.ok) {
    throw new Error('Registration failed');
  }
  
  return response.json();
};

// Get user profile
const getUserProfile = async (token) => {
  const response = await fetch('/api/v1/users/profile', {
    headers: {
      'Authorization': `Bearer ${token}`,
    }
  });
  
  if (!response.ok) {
    throw new Error('Failed to get profile');
  }
  
  return response.json();
};
```

### Python

```python
import requests

# User registration
def register_user(user_data):
    response = requests.post(
        'https://api.mcp.com/api/v1/users/register',
        json=user_data
    )
    response.raise_for_status()
    return response.json()

# Get user profile
def get_user_profile(token):
    headers = {'Authorization': f'Bearer {token}'}
    response = requests.get(
        'https://api.mcp.com/api/v1/users/profile',
        headers=headers
    )
    response.raise_for_status()
    return response.json()
```

### cURL

```bash
# User registration
curl -X POST https://api.mcp.com/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "password": "SecurePassword123!",
    "firstName": "John",
    "lastName": "Doe",
    "organizationId": "org-123",
    "acceptTerms": true,
    "acceptPrivacyPolicy": true
  }'

# Get user profile
curl -X GET https://api.mcp.com/api/v1/users/profile \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Testing

### Test Credentials

For testing purposes, use the following test environment:

```
Base URL: https://api-test.mcp.com/api/v1/users
Test Organization ID: test-org-123
```

### Postman Collection

A comprehensive Postman collection is available for testing all endpoints:

[Download Postman Collection](./postman/user-management-api.postman_collection.json)

## Support

For API support and questions:

- Documentation: https://docs.mcp.com
- Support Email: api-support@mcp.com
- Developer Portal: https://developers.mcp.com

## Changelog

### v1.0.0 (2025-01-17)
- Initial release
- User registration and verification
- Password reset functionality
- Profile management
- Admin endpoints
- Comprehensive security features