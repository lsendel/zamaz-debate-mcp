package com.zamaz.mcp.github.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.github.entity.GitHubInstallation;
import com.zamaz.mcp.github.entity.RepositoryConfig;
import com.zamaz.mcp.github.entity.PullRequestReview;
import com.zamaz.mcp.github.entity.ReviewComment;
import com.zamaz.mcp.github.entity.ReviewIssue;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Random;

/**
 * Utility class for E2E tests
 * Provides common helper methods and test data generation
 */
public class E2ETestUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Random random = new Random();

    /**
     * Create a test GitHub installation
     */
    public static GitHubInstallation createTestInstallation(String accountLogin, String accountType) {
        GitHubInstallation installation = new GitHubInstallation();
        installation.setId(generateInstallationId());
        installation.setAccountLogin(accountLogin);
        installation.setAccountType(accountType);
        installation.setStatus("ACTIVE");
        installation.setAccessToken("test-token-" + installation.getId());
        installation.setCreatedAt(LocalDateTime.now());
        installation.setUpdatedAt(LocalDateTime.now());
        return installation;
    }

    /**
     * Create a test repository configuration
     */
    public static RepositoryConfig createTestRepositoryConfig(GitHubInstallation installation, String repoName) {
        RepositoryConfig config = new RepositoryConfig();
        config.setInstallationId(installation.getId());
        config.setRepositoryFullName(repoName);
        config.setAutoReviewEnabled(true);
        config.setNotificationsEnabled(true);
        config.setBranchPatterns("main,develop,feature/*");
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        return config;
    }

    /**
     * Create a test pull request review
     */
    public static PullRequestReview createTestReview(GitHubInstallation installation, String repoName, int prNumber) {
        PullRequestReview review = new PullRequestReview();
        review.setInstallationId(installation.getId());
        review.setRepositoryFullName(repoName);
        review.setPrNumber(prNumber);
        review.setPrTitle("Test PR #" + prNumber);
        review.setPrAuthor("test-author-" + prNumber);
        review.setStatus("PENDING");
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        return review;
    }

    /**
     * Create a test review comment
     */
    public static ReviewComment createTestComment(PullRequestReview review, String body) {
        ReviewComment comment = new ReviewComment();
        comment.setReview(review);
        comment.setBody(body);
        comment.setCommentType("REVIEW");
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        return comment;
    }

    /**
     * Create a test review issue
     */
    public static ReviewIssue createTestIssue(PullRequestReview review, String type, String severity) {
        ReviewIssue issue = new ReviewIssue();
        issue.setReview(review);
        issue.setIssueType(type);
        issue.setDescription("Test issue of type " + type);
        issue.setSeverity(severity);
        issue.setStatus("OPEN");
        issue.setCreatedAt(LocalDateTime.now());
        issue.setUpdatedAt(LocalDateTime.now());
        return issue;
    }

    /**
     * Create pull request opened webhook payload
     */
    public static Map<String, Object> createPullRequestOpenedWebhook(Long installationId, String repoName, int prNumber) {
        return createPullRequestWebhook(installationId, "opened", repoName, prNumber, "open");
    }

    /**
     * Create pull request closed webhook payload
     */
    public static Map<String, Object> createPullRequestClosedWebhook(Long installationId, String repoName, int prNumber, boolean merged) {
        Map<String, Object> webhook = createPullRequestWebhook(installationId, "closed", repoName, prNumber, "closed");
        ((Map<String, Object>) webhook.get("pull_request")).put("merged", merged);
        return webhook;
    }

    /**
     * Create pull request synchronized webhook payload
     */
    public static Map<String, Object> createPullRequestSyncWebhook(Long installationId, String repoName, int prNumber) {
        return createPullRequestWebhook(installationId, "synchronize", repoName, prNumber, "open");
    }

    /**
     * Create generic pull request webhook payload
     */
    public static Map<String, Object> createPullRequestWebhook(Long installationId, String action, String repoName, int prNumber, String state) {
        Map<String, Object> webhook = new HashMap<>();
        webhook.put("action", action);
        webhook.put("installation", Map.of("id", installationId));
        
        String[] repoParts = repoName.split("/");
        String owner = repoParts[0];
        String repo = repoParts[1];
        
        webhook.put("pull_request", Map.of(
            "id", (long) prNumber,
            "number", prNumber,
            "title", generatePRTitle(),
            "body", generatePRBody(),
            "state", state,
            "html_url", String.format("https://github.com/%s/pull/%d", repoName, prNumber),
            "user", Map.of("login", "test-author-" + prNumber),
            "head", Map.of("ref", "feature-branch-" + prNumber),
            "base", Map.of("ref", "main"),
            "created_at", "2024-01-01T00:00:00Z",
            "updated_at", "2024-01-01T00:00:00Z"
        ));
        
        webhook.put("repository", Map.of(
            "id", 1L,
            "name", repo,
            "full_name", repoName,
            "html_url", "https://github.com/" + repoName,
            "owner", Map.of("login", owner)
        ));
        
        return webhook;
    }

    /**
     * Create installation webhook payload
     */
    public static Map<String, Object> createInstallationWebhook(String action, long installationId, String accountLogin, String accountType) {
        Map<String, Object> webhook = new HashMap<>();
        webhook.put("action", action);
        webhook.put("installation", Map.of(
            "id", installationId,
            "account", Map.of(
                "login", accountLogin,
                "type", accountType
            )
        ));
        return webhook;
    }

    /**
     * Create GitHub API response for pull request
     */
    public static String createPullRequestApiResponse(int prNumber, String title, String body) {
        return String.format("""
            {
                "id": %d,
                "number": %d,
                "title": "%s",
                "body": "%s",
                "state": "open",
                "html_url": "https://github.com/test-owner/test-repo/pull/%d",
                "user": {
                    "id": 1,
                    "login": "test-author-%d",
                    "avatar_url": "https://github.com/test-author-%d.png",
                    "html_url": "https://github.com/test-author-%d",
                    "type": "User"
                },
                "head": {
                    "ref": "feature-branch-%d"
                },
                "base": {
                    "ref": "main"
                },
                "created_at": "2024-01-01T00:00:00Z",
                "updated_at": "2024-01-01T00:00:00Z"
            }
            """, prNumber, prNumber, title, body, prNumber, prNumber, prNumber, prNumber, prNumber);
    }

    /**
     * Create GitHub API response for comment
     */
    public static String createCommentApiResponse(long commentId, String body) {
        return String.format("""
            {
                "id": %d,
                "body": "%s",
                "html_url": "https://github.com/test-owner/test-repo/pull/123#issuecomment-%d",
                "user": {
                    "id": 1,
                    "login": "github-actions[bot]",
                    "type": "Bot"
                },
                "created_at": "2024-01-01T00:00:00Z",
                "updated_at": "2024-01-01T00:00:00Z"
            }
            """, commentId, body, commentId);
    }

    /**
     * Generate realistic PR titles
     */
    public static String generatePRTitle() {
        String[] types = {"feat", "fix", "docs", "style", "refactor", "test", "chore"};
        String[] subjects = {"user authentication", "database connection", "API endpoints", "error handling", "performance", "security"};
        
        String type = types[random.nextInt(types.length)];
        String subject = subjects[random.nextInt(subjects.length)];
        
        return String.format("%s: improve %s", type, subject);
    }

    /**
     * Generate realistic PR bodies
     */
    public static String generatePRBody() {
        String[] summaries = {
            "This PR improves the overall system performance by optimizing database queries.",
            "Added comprehensive error handling to prevent application crashes.",
            "Implemented new authentication mechanism for better security.",
            "Fixed critical bug in the payment processing system.",
            "Enhanced user interface with better accessibility features."
        };
        
        String summary = summaries[random.nextInt(summaries.length)];
        
        return String.format("""
            ## Summary
            %s
            
            ## Changes
            - Updated core functionality
            - Added comprehensive tests
            - Improved documentation
            
            ## Testing
            - [ ] Unit tests passing
            - [ ] Integration tests passing
            - [ ] Manual testing completed
            """, summary);
    }

    /**
     * Generate unique installation ID
     */
    public static Long generateInstallationId() {
        return System.currentTimeMillis() + random.nextInt(1000);
    }

    /**
     * Generate test webhook signature
     */
    public static String generateWebhookSignature(String payload, String secret) {
        // In a real implementation, this would calculate the actual HMAC-SHA256
        // For testing, we'll use a simple hash
        return "sha256=" + Integer.toHexString((payload + secret).hashCode());
    }

    /**
     * Create test data set for bulk operations
     */
    public static Map<String, Object> createBulkTestData(int count) {
        Map<String, Object> data = new HashMap<>();
        data.put("installations", count);
        data.put("repositoriesPerInstallation", 5);
        data.put("pullRequestsPerRepository", 10);
        data.put("commentsPerPullRequest", 3);
        data.put("issuesPerPullRequest", 2);
        return data;
    }

    /**
     * Wait for async operations to complete
     */
    public static void waitForAsyncCompletion(long timeoutMs) {
        try {
            Thread.sleep(timeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Verify webhook processing metrics
     */
    public static boolean verifyProcessingMetrics(long startTime, long endTime, int webhookCount, double expectedThroughput) {
        double actualThroughput = webhookCount / ((endTime - startTime) / 1000.0);
        return actualThroughput >= expectedThroughput;
    }

    /**
     * Create test scenario configurations
     */
    public static Map<String, Object> createTestScenario(String scenarioName) {
        Map<String, Object> scenario = new HashMap<>();
        
        switch (scenarioName) {
            case "high_volume":
                scenario.put("webhookCount", 100);
                scenario.put("concurrentThreads", 10);
                scenario.put("expectedThroughput", 10.0);
                break;
            case "stress_test":
                scenario.put("webhookCount", 500);
                scenario.put("concurrentThreads", 20);
                scenario.put("expectedThroughput", 20.0);
                break;
            case "low_volume":
                scenario.put("webhookCount", 10);
                scenario.put("concurrentThreads", 2);
                scenario.put("expectedThroughput", 5.0);
                break;
            default:
                scenario.put("webhookCount", 50);
                scenario.put("concurrentThreads", 5);
                scenario.put("expectedThroughput", 10.0);
        }
        
        return scenario;
    }

    private E2ETestUtils() {
        // Utility class
    }
}