# Kiro GitHub Integration - API Documentation

## Overview

The Kiro GitHub Integration provides a comprehensive REST API for managing configurations, accessing analytics, and integrating with external systems.

## Authentication

### Access Tokens

All API requests require authentication using Bearer tokens.

```http
Authorization: Bearer kiro_abc123def456_1234567890
```

### Token Management

#### Create Access Token
```http
POST /api/auth/tokens
Content-Type: application/json

{
  "user_id": "user@company.com",
  "scope": "repo:read,repo:write,analytics:read",
  "expires_in_hours": 24
}
```

**Response:**
```json
{
  "token": "kiro_abc123def456_1234567890",
  "expires_at": "2024-01-16T10:00:00Z",
  "scope": "repo:read,repo:write,analytics:read"
}
```

#### Validate Token
```http
GET /api/auth/tokens/validate
Authorization: Bearer {token}
```

**Response:**
```json
{
  "valid": true,
  "user_id": "user@company.com",
  "scope": "repo:read,repo:write,analytics:read",
  "expires_at": "2024-01-16T10:00:00Z"
}
```

## Configuration API

### Repository Configuration

#### Get Configuration
```http
GET /api/config/repos/{owner}/{repo}
Authorization: Bearer {token}
```

**Response:**
```json
{
  "review": {
    "depth": "standard",
    "focus_areas": ["security", "performance", "style"],
    "auto_fix": true,
    "comment_style": "educational"
  },
  "rules": {
    "custom_rules_enabled": true,
    "rule_sets": [
      {
        "name": "Security Rules",
        "enabled": true,
        "rules": [
          {
            "id": "hardcoded-secrets",
            "severity": "critical",
            "description": "Detect hardcoded secrets"
          }
        ]
      }
    ]
  },
  "notifications": {
    "channels": ["github", "slack"],
    "events": ["review_complete", "critical_issue"]
  }
}
```

#### Update Configuration
```http
PUT /api/config/repos/{owner}/{repo}
Authorization: Bearer {token}
Content-Type: application/json

{
  "review": {
    "depth": "thorough",
    "focus_areas": ["security", "performance"]
  }
}
```

**Response:**
```json
{
  "success": true,
  "message": "Configuration updated successfully"
}
```

#### Create Default Configuration
```http
POST /api/config/repos/{owner}/{repo}/default
Authorization: Bearer {token}
```

### Custom Rules

#### Add Custom Rule
```http
POST /api/config/repos/{owner}/{repo}/rules
Authorization: Bearer {token}
Content-Type: application/json

{
  "rule_set": "Security Rules",
  "rule": {
    "id": "custom-security-rule",
    "severity": "major",
    "description": "Custom security check",
    "pattern": "eval\\(",
    "message": "Avoid using eval() function",
    "file_patterns": ["*.js", "*.ts"]
  }
}
```

#### Remove Custom Rule
```http
DELETE /api/config/repos/{owner}/{repo}/rules/{rule_set}/{rule_id}
Authorization: Bearer {token}
```

### Team Standards

#### Get Team Standards
```http
GET /api/config/repos/{owner}/{repo}/standards
Authorization: Bearer {token}
```

**Response:**
```json
{
  "indentation": {
    "style": "spaces",
    "size": 2
  },
  "line_length": 100,
  "naming_conventions": {
    "variables": "camelCase",
    "functions": "camelCase",
    "classes": "PascalCase"
  }
}
```

#### Update Team Standards
```http
PUT /api/config/repos/{owner}/{repo}/standards
Authorization: Bearer {token}
Content-Type: application/json

{
  "line_length": 120,
  "naming_conventions": {
    "variables": "snake_case"
  }
}
```

## Analytics API

### Review Statistics

#### Get Review Stats
```http
GET /api/analytics/stats
Authorization: Bearer {token}
Query Parameters:
  - repo_owner (optional): Repository owner
  - repo_name (optional): Repository name
  - days (optional): Number of days (default: 30)
```

**Response:**
```json
{
  "total_reviews": 45,
  "avg_review_time_seconds": 180.5,
  "total_issues": 123,
  "avg_issues_per_review": 2.7,
  "total_suggestions": 67,
  "total_applied_suggestions": 42,
  "suggestion_acceptance_rate": 0.627,
  "issues_by_severity": {
    "critical": 5,
    "major": 23,
    "minor": 67,
    "suggestion": 28
  },
  "issues_by_category": {
    "security": 15,
    "performance": 28,
    "style": 45,
    "documentation": 35
  }
}
```

#### Get Feedback Stats
```http
GET /api/analytics/feedback
Authorization: Bearer {token}
Query Parameters:
  - repo_owner (optional): Repository owner
  - repo_name (optional): Repository name
  - days (optional): Number of days (default: 30)
```

**Response:**
```json
{
  "feedback_by_type": {
    "helpful": {
      "count": 89,
      "avg_score": 4.2
    },
    "not_helpful": {
      "count": 12,
      "avg_score": 2.1
    }
  }
}
```

### Learning Insights

#### Get Learning Insights
```http
GET /api/analytics/insights
Authorization: Bearer {token}
Query Parameters:
  - repo_owner (optional): Repository owner
  - repo_name (optional): Repository name
```

**Response:**
```json
{
  "top_performing_rules": [
    {
      "rule_id": "security-hardcoded-password",
      "category": "security",
      "effectiveness_score": 0.89,
      "total_occurrences": 23
    }
  ],
  "most_confident_patterns": [
    {
      "pattern_type": "naming_convention",
      "pattern_value": "camelCase",
      "file_extension": ".js",
      "confidence_score": 0.92
    }
  ],
  "suggestion_improvements": [
    {
      "improvement_type": "language_specific",
      "count": 15,
      "avg_success_rate": 0.78
    }
  ]
}
```

#### Get Rule Recommendations
```http
GET /api/analytics/recommendations
Authorization: Bearer {token}
Query Parameters:
  - repo_owner: Repository owner
  - repo_name: Repository name
  - developer (optional): Developer username
```

**Response:**
```json
{
  "recommendations": [
    {
      "rule_id": "performance-n-plus-one",
      "category": "performance",
      "effectiveness_score": 0.85,
      "developer_preference_score": 0.9,
      "combined_score": 0.875,
      "total_occurrences": 12
    }
  ]
}
```

### Metrics

#### Get Metrics
```http
GET /api/analytics/metrics
Authorization: Bearer {token}
Query Parameters:
  - repo_owner (optional): Repository owner
  - repo_name (optional): Repository name
  - metric_name (optional): Specific metric name
  - days (optional): Number of days (default: 30)
```

**Response:**
```json
{
  "metrics": [
    {
      "repo_owner": "myorg",
      "repo_name": "myrepo",
      "metric_name": "review_time_seconds",
      "metric_value": 145.2,
      "timestamp": "2024-01-15T14:30:00Z"
    }
  ]
}
```

## Review Management API

### Pull Request Reviews

#### Trigger Review
```http
POST /api/reviews/trigger
Authorization: Bearer {token}
Content-Type: application/json

{
  "repo_owner": "myorg",
  "repo_name": "myrepo",
  "pr_number": 123,
  "force": false
}
```

**Response:**
```json
{
  "review_id": "rev_abc123def456",
  "status": "queued",
  "estimated_completion": "2024-01-15T14:35:00Z"
}
```

#### Get Review Status
```http
GET /api/reviews/{review_id}
Authorization: Bearer {token}
```

**Response:**
```json
{
  "review_id": "rev_abc123def456",
  "status": "completed",
  "repo_owner": "myorg",
  "repo_name": "myrepo",
  "pr_number": 123,
  "start_time": "2024-01-15T14:30:00Z",
  "end_time": "2024-01-15T14:33:45Z",
  "issues_found": 5,
  "suggestions_provided": 3
}
```

#### Get Review Results
```http
GET /api/reviews/{review_id}/results
Authorization: Bearer {token}
```

**Response:**
```json
{
  "review_id": "rev_abc123def456",
  "summary": {
    "files_reviewed": 8,
    "lines_reviewed": 245,
    "issues_found": {
      "critical": 1,
      "major": 2,
      "minor": 2,
      "suggestions": 0
    }
  },
  "issues": [
    {
      "id": "issue_123",
      "file": "src/main.py",
      "line_start": 42,
      "line_end": 42,
      "severity": "critical",
      "category": "security",
      "message": "Hardcoded password detected",
      "suggestion": {
        "original_text": "password = \"secret123\"",
        "replacement_text": "password = os.environ.get(\"PASSWORD\")",
        "description": "Use environment variable for password"
      }
    }
  ]
}
```

### Feedback Management

#### Submit Feedback
```http
POST /api/reviews/{review_id}/feedback
Authorization: Bearer {token}
Content-Type: application/json

{
  "feedback_type": "helpful",
  "score": 5,
  "comment": "Great suggestions, very helpful!",
  "issue_id": "issue_123"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Feedback recorded successfully"
}
```

## Administration API

### User Management

#### List Users
```http
GET /api/admin/users
Authorization: Bearer {admin_token}
Query Parameters:
  - limit (optional): Number of users to return (default: 50)
  - offset (optional): Offset for pagination (default: 0)
```

**Response:**
```json
{
  "users": [
    {
      "user_id": "user@company.com",
      "permissions": ["repo:read", "repo:write"],
      "last_active": "2024-01-15T14:30:00Z",
      "repositories": ["org/repo1", "org/repo2"]
    }
  ],
  "total": 25,
  "limit": 50,
  "offset": 0
}
```

#### Grant Permission
```http
POST /api/admin/permissions
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "user_id": "user@company.com",
  "resource_type": "repository",
  "resource_id": "org/repo",
  "permission": "review",
  "expires_in_days": 90
}
```

#### Revoke Permission
```http
DELETE /api/admin/permissions
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "user_id": "user@company.com",
  "resource_type": "repository",
  "resource_id": "org/repo",
  "permission": "review"
}
```

### Repository Management

#### List Repositories
```http
GET /api/admin/repositories
Authorization: Bearer {admin_token}
```

**Response:**
```json
{
  "repositories": [
    {
      "repo_owner": "myorg",
      "repo_name": "myrepo",
      "review_count": 45,
      "last_review": "2024-01-15T14:30:00Z",
      "configuration_status": "configured"
    }
  ]
}
```

#### Get Repository Details
```http
GET /api/admin/repositories/{owner}/{repo}
Authorization: Bearer {admin_token}
```

**Response:**
```json
{
  "repo_owner": "myorg",
  "repo_name": "myrepo",
  "installation_id": "12345",
  "configuration": {
    "review": {
      "depth": "standard"
    }
  },
  "statistics": {
    "total_reviews": 45,
    "avg_review_time": 180.5,
    "issues_found": 123
  },
  "users": [
    {
      "user_id": "user@company.com",
      "permissions": ["review"]
    }
  ]
}
```

### Audit and Compliance

#### Get Audit Logs
```http
GET /api/admin/audit
Authorization: Bearer {admin_token}
Query Parameters:
  - event_type (optional): Filter by event type
  - user_id (optional): Filter by user
  - start_date (optional): Start date (ISO format)
  - end_date (optional): End date (ISO format)
  - limit (optional): Number of logs to return (default: 100)
```

**Response:**
```json
{
  "logs": [
    {
      "id": "audit_123",
      "timestamp": "2024-01-15T14:30:00Z",
      "event_type": "authentication",
      "user_id": "user@company.com",
      "action": "token_created",
      "resource_type": "token",
      "success": true,
      "ip_address": "192.168.1.100"
    }
  ]
}
```

#### Generate Compliance Report
```http
GET /api/admin/compliance/report
Authorization: Bearer {admin_token}
Query Parameters:
  - start_date: Start date (ISO format)
  - end_date: End date (ISO format)
  - format (optional): Report format (json, pdf, csv)
```

**Response:**
```json
{
  "period": {
    "start": "2024-01-01T00:00:00Z",
    "end": "2024-01-31T23:59:59Z"
  },
  "processing_activities": [
    {
      "data_type": "source_code",
      "operation": "analysis",
      "count": 1250,
      "total_size": 52428800,
      "purpose": "code_review",
      "legal_basis": "legitimate_interest"
    }
  ],
  "data_subjects": 45,
  "retention_compliance": "compliant"
}
```

## Webhook API

### GitHub Webhooks

#### Webhook Endpoint
```http
POST /api/webhooks/github
Content-Type: application/json
X-GitHub-Event: pull_request
X-Hub-Signature: sha1=abc123def456

{
  "action": "assigned",
  "pull_request": {
    "number": 123,
    "assignee": {
      "login": "kiro-ai"
    }
  },
  "repository": {
    "full_name": "org/repo"
  }
}
```

#### Webhook Status
```http
GET /api/webhooks/status
```

**Response:**
```json
{
  "status": "healthy",
  "last_event": "2024-01-15T14:30:00Z",
  "events_processed": 1250,
  "errors": 5
}
```

## Error Handling

### Error Response Format

All API errors follow a consistent format:

```json
{
  "error": {
    "code": "INVALID_TOKEN",
    "message": "The provided access token is invalid or expired",
    "details": {
      "token_expired": true,
      "expires_at": "2024-01-15T14:30:00Z"
    }
  }
}
```

### Common Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `INVALID_TOKEN` | 401 | Access token is invalid or expired |
| `INSUFFICIENT_PERMISSIONS` | 403 | User lacks required permissions |
| `RESOURCE_NOT_FOUND` | 404 | Requested resource doesn't exist |
| `VALIDATION_ERROR` | 400 | Request validation failed |
| `RATE_LIMIT_EXCEEDED` | 429 | API rate limit exceeded |
| `INTERNAL_ERROR` | 500 | Internal server error |

## Rate Limits

### Default Limits

- **API Calls**: 1000 requests per hour per token
- **Reviews**: 100 reviews per hour per repository
- **Webhooks**: 10,000 events per hour

### Rate Limit Headers

```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1642248000
```

## SDKs and Libraries

### Python SDK

```python
from kiro_github import KiroClient

client = KiroClient(token="your_token")

# Get repository configuration
config = client.config.get_repo_config("owner", "repo")

# Trigger a review
review = client.reviews.trigger("owner", "repo", 123)

# Get analytics
stats = client.analytics.get_stats("owner", "repo", days=30)
```

### JavaScript SDK

```javascript
import { KiroClient } from '@kiro/github-integration';

const client = new KiroClient({ token: 'your_token' });

// Get repository configuration
const config = await client.config.getRepoConfig('owner', 'repo');

// Trigger a review
const review = await client.reviews.trigger('owner', 'repo', 123);

// Get analytics
const stats = await client.analytics.getStats('owner', 'repo', { days: 30 });
```

## Examples

### Complete Integration Example

```python
import requests
import json

# Configuration
API_BASE = "https://your-kiro-server.com/api"
TOKEN = "your_access_token"
HEADERS = {
    "Authorization": f"Bearer {TOKEN}",
    "Content-Type": "application/json"
}

# 1. Get repository configuration
response = requests.get(f"{API_BASE}/config/repos/myorg/myrepo", headers=HEADERS)
config = response.json()
print(f"Current config: {json.dumps(config, indent=2)}")

# 2. Update configuration
new_config = {
    "review": {
        "depth": "thorough",
        "focus_areas": ["security", "performance"]
    }
}
response = requests.put(f"{API_BASE}/config/repos/myorg/myrepo", 
                       headers=HEADERS, json=new_config)
print(f"Config updated: {response.json()}")

# 3. Trigger a review
review_request = {
    "repo_owner": "myorg",
    "repo_name": "myrepo",
    "pr_number": 123
}
response = requests.post(f"{API_BASE}/reviews/trigger", 
                        headers=HEADERS, json=review_request)
review = response.json()
print(f"Review triggered: {review['review_id']}")

# 4. Get analytics
response = requests.get(f"{API_BASE}/analytics/stats?repo_owner=myorg&repo_name=myrepo&days=30", 
                       headers=HEADERS)
stats = response.json()
print(f"Analytics: {json.dumps(stats, indent=2)}")
```

This API documentation provides comprehensive coverage of all available endpoints and functionality in the Kiro GitHub Integration.