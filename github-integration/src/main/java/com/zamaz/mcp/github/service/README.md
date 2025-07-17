# GitHub API Client Service

This package contains the GitHub API client service implementation for the Kiro GitHub Integration module.

## GitHubApiClient

The main service class that handles all GitHub API interactions including:

### Core Features

1. **Pull Request Management**
   - Retrieve pull request details
   - List pull requests with filtering and pagination
   - Update pull request status and labels

2. **Comment Management**
   - Post comments on pull requests
   - Update existing comments
   - Delete comments
   - List all comments on a pull request

3. **Repository Access**
   - Get repository information
   - Handle public and private repositories
   - Validate repository access permissions

4. **User Authentication**
   - Get authenticated user information
   - Support for GitHub Personal Access Tokens
   - Proper authorization header handling

### Error Handling

The service provides comprehensive error handling for:
- Network timeouts and connection issues
- HTTP errors (401, 403, 404, 429, 500, etc.)
- Rate limiting with proper error messages
- Malformed JSON responses
- Authentication failures

### Caching

The service implements Spring Cache abstraction for:
- Repository information (`@Cacheable` on `getRepository`)
- Pull request lists (`@Cacheable` on `listPullRequests`)

### Configuration

The service is configured via application properties:
- `github.api.base-url`: GitHub API base URL (default: https://api.github.com)
- `github.api.version`: API version header (default: application/vnd.github.v3+json)

### Usage Example

```java
@Autowired
private GitHubApiClient gitHubApiClient;

// Get pull request
GitHubPullRequest pr = gitHubApiClient.getPullRequest(
    token, "owner", "repo", 123);

// Post comment
GitHubComment comment = gitHubApiClient.postComment(
    token, "owner", "repo", 123, "Great work!");

// Update PR with labels
GitHubPullRequest updated = gitHubApiClient.updatePullRequest(
    token, "owner", "repo", 123, "closed", List.of("approved"));
```

## DTOs

### GitHubPullRequest
- Complete pull request information
- Includes user, head/base refs, merge status
- Maps all relevant GitHub API fields

### GitHubComment
- Comment details with user information
- HTML URL for direct access
- Creation and update timestamps

### GitHubRepository
- Repository metadata
- Owner information
- Public/private status
- Default branch information

### GitHubUser
- User profile information
- Avatar and HTML URLs
- Account type (User/Organization)

## Testing

The service includes comprehensive test coverage:

### Integration Tests (`GitHubApiClientIntegrationTest`)
- Tests all major API operations
- Uses WireMock for GitHub API mocking
- Validates request/response handling

### Error Handling Tests (`GitHubApiClientErrorHandlingTest`)
- Tests all error scenarios
- Network timeouts, HTTP errors, rate limits
- Malformed responses and edge cases

### Advanced Tests (`GitHubApiClientAdvancedTest`)
- Pagination handling
- Complex scenarios with multiple operations
- Special characters and edge cases

### Performance Tests (`GitHubApiClientPerformanceTest`)
- Concurrent request handling
- Memory usage validation
- Sequential vs concurrent performance

### Real-World Tests (`GitHubApiClientRealWorldTest`)
- Complete workflow scenarios
- Code review workflows
- Repository permission handling

## Security Considerations

- All API calls require proper authentication tokens
- Sensitive information is never logged
- Rate limiting is handled gracefully
- Network timeouts prevent hanging requests
- Input validation on all parameters

## Dependencies

- Spring Boot Web (RestTemplate)
- Spring Boot Cache
- Lombok (for DTOs)
- WireMock (for testing)
- Jackson (JSON processing)