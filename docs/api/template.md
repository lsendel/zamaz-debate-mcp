# API Endpoint Documentation Template

This template provides a standardized format for documenting API endpoints across all services.

## Endpoint Structure

### Basic Information

```
# Endpoint: [HTTP Method] [Path]

**Description**: Brief description of what the endpoint does

**Service**: [Service Name]

**Authentication Required**: Yes/No

**Required Headers**:
- `Authorization`: Bearer token (if required)
- `X-Organization-ID`: Organization ID (if required)
- `Content-Type`: application/json
```

### Request Parameters

```
## Request

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Resource identifier |

### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `page` | integer | No | 0 | Page number for pagination |
| `size` | integer | No | 20 | Page size for pagination |
| `sort` | string | No | "createdAt,desc" | Sort field and direction |

### Request Body

```json
{
  "property1": "value1",
  "property2": 123,
  "nestedObject": {
    "nestedProperty": "value"
  },
  "arrayProperty": [
    "item1",
    "item2"
  ]
}
```

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `property1` | string | Yes | Description of property1 |
| `property2` | integer | No | Description of property2 |
| `nestedObject` | object | No | A nested object |
| `nestedObject.nestedProperty` | string | Yes | Description of nested property |
| `arrayProperty` | array | No | An array of strings |
```

### Response

```
## Response

### Success Response (200 OK)

```json
{
  "id": "resource-123",
  "property1": "value1",
  "property2": 123,
  "createdAt": "2025-07-16T14:22:15Z",
  "updatedAt": "2025-07-16T14:22:15Z"
}
```

| Property | Type | Description |
|----------|------|-------------|
| `id` | string | Resource identifier |
| `property1` | string | Description of property1 |
| `property2` | integer | Description of property2 |
| `createdAt` | string (ISO 8601) | Creation timestamp |
| `updatedAt` | string (ISO 8601) | Last update timestamp |

### Error Responses

#### 400 Bad Request

```json
{
  "error": {
    "code": "INVALID_REQUEST",
    "message": "Invalid request parameters",
    "details": {
      "property1": "must not be null"
    },
    "requestId": "request-456"
  }
}
```

#### 401 Unauthorized

```json
{
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Authentication required",
    "requestId": "request-456"
  }
}
```

#### 403 Forbidden

```json
{
  "error": {
    "code": "FORBIDDEN",
    "message": "Insufficient permissions",
    "requestId": "request-456"
  }
}
```

#### 404 Not Found

```json
{
  "error": {
    "code": "NOT_FOUND",
    "message": "Resource not found",
    "requestId": "request-456"
  }
}
```

#### 429 Too Many Requests

```json
{
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Rate limit exceeded",
    "details": {
      "retryAfter": 60
    },
    "requestId": "request-456"
  }
}
```

#### 500 Internal Server Error

```json
{
  "error": {
    "code": "INTERNAL_ERROR",
    "message": "An internal error occurred",
    "requestId": "request-456"
  }
}
```
```

### Example Usage

```
## Example

### cURL

```bash
curl -X POST "https://api.example.com/api/v1/resources" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-token" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "property1": "value1",
    "property2": 123,
    "nestedObject": {
      "nestedProperty": "value"
    },
    "arrayProperty": [
      "item1",
      "item2"
    ]
  }'
```

### Python

```python
import requests

url = "https://api.example.com/api/v1/resources"
headers = {
    "Content-Type": "application/json",
    "Authorization": "Bearer your-token",
    "X-Organization-ID": "org-123"
}
payload = {
    "property1": "value1",
    "property2": 123,
    "nestedObject": {
        "nestedProperty": "value"
    },
    "arrayProperty": [
        "item1",
        "item2"
    ]
}

response = requests.post(url, headers=headers, json=payload)
print(response.json())
```

### JavaScript

```javascript
fetch("https://api.example.com/api/v1/resources", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    "Authorization": "Bearer your-token",
    "X-Organization-ID": "org-123"
  },
  body: JSON.stringify({
    property1: "value1",
    property2: 123,
    nestedObject: {
      nestedProperty: "value"
    },
    arrayProperty: [
      "item1",
      "item2"
    ]
  })
})
.then(response => response.json())
.then(data => console.log(data))
.catch(error => console.error("Error:", error));
```
```

### Notes and Limitations

```
## Notes

- Rate limited to 60 requests per minute per organization
- Maximum request body size: 1MB
- Responses are paginated with a default page size of 20
- All timestamps are in UTC and follow ISO 8601 format
```
