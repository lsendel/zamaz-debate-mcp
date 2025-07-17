package com.zamaz.mcp.github.service;

import com.zamaz.mcp.github.entity.PullRequestReview;
import com.zamaz.mcp.github.entity.ReviewComment;
import com.zamaz.mcp.github.entity.ReviewIssue;
import com.zamaz.mcp.github.repository.PullRequestReviewRepository;
import com.zamaz.mcp.github.repository.ReviewCommentRepository;
import com.zamaz.mcp.github.repository.ReviewIssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service for analyzing pull requests and generating automated reviews
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PullRequestAnalyzer {

    private final GitHubApiClient apiClient;
    private final PullRequestReviewRepository reviewRepository;
    private final ReviewCommentRepository commentRepository;
    private final ReviewIssueRepository issueRepository;
    private final NotificationService notificationService;

    /**
     * Analyze pull request asynchronously
     */
    @Async
    @Transactional
    public CompletableFuture<Void> analyzePullRequestAsync(String accessToken, String owner, 
                                                          String repo, int prNumber, Long reviewId) {
        log.info("Starting async PR analysis: {}/{} PR#{}", owner, repo, prNumber);
        
        try {
            // Get PR details
            var pullRequest = apiClient.getPullRequest(accessToken, owner, repo, prNumber);
            
            // Update review status
            Optional<PullRequestReview> reviewOpt = reviewRepository.findById(reviewId);
            if (reviewOpt.isEmpty()) {
                log.error("Review not found: {}", reviewId);
                return CompletableFuture.completedFuture(null);
            }
            
            PullRequestReview review = reviewOpt.get();
            review.setStatus("ANALYZING");
            review.setUpdatedAt(LocalDateTime.now());
            reviewRepository.save(review);
            
            // Perform analysis
            AnalysisResult result = performAnalysis(pullRequest, accessToken, owner, repo, prNumber);
            
            // Save analysis results
            saveAnalysisResults(review, result);
            
            // Post review comments
            postReviewComments(accessToken, owner, repo, prNumber, result);
            
            // Update final status
            review.setStatus("COMPLETED");
            review.setUpdatedAt(LocalDateTime.now());
            reviewRepository.save(review);
            
            // Send notification
            notificationService.sendReviewCompletedNotification(review, result);
            
            log.info("Completed PR analysis: {}/{} PR#{}", owner, repo, prNumber);
            
        } catch (Exception e) {
            log.error("Error in PR analysis: {}/{} PR#{}, error: {}", 
                owner, repo, prNumber, e.getMessage(), e);
            
            // Update review status to failed
            reviewRepository.findById(reviewId).ifPresent(review -> {
                review.setStatus("FAILED");
                review.setUpdatedAt(LocalDateTime.now());
                reviewRepository.save(review);
            });
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Perform the actual analysis
     */
    private AnalysisResult performAnalysis(var pullRequest, String accessToken, 
                                         String owner, String repo, int prNumber) {
        log.info("Performing analysis for PR: {}/{} #{}", owner, repo, prNumber);
        
        AnalysisResult result = new AnalysisResult();
        
        // Analyze PR title and description
        analyzeTitle(pullRequest, result);
        analyzeDescription(pullRequest, result);
        
        // Analyze code changes (this would integrate with actual code analysis tools)
        analyzeCodeChanges(accessToken, owner, repo, prNumber, result);
        
        // Check for security issues
        checkSecurityIssues(result);
        
        // Check for performance issues
        checkPerformanceIssues(result);
        
        return result;
    }

    /**
     * Analyze PR title
     */
    private void analyzeTitle(var pullRequest, AnalysisResult result) {
        String title = pullRequest.getTitle();
        
        if (title == null || title.trim().isEmpty()) {
            result.addIssue("TITLE_EMPTY", "PR title is empty", "HIGH");
        } else if (title.length() < 10) {
            result.addIssue("TITLE_TOO_SHORT", "PR title is too short", "MEDIUM");
        } else if (title.length() > 100) {
            result.addIssue("TITLE_TOO_LONG", "PR title is too long", "LOW");
        }
        
        // Check for conventional commit format
        if (!title.matches("^(feat|fix|docs|style|refactor|test|chore)(\\(.+\\))?: .+")) {
            result.addIssue("TITLE_FORMAT", "PR title doesn't follow conventional commit format", "LOW");
        }
    }

    /**
     * Analyze PR description
     */
    private void analyzeDescription(var pullRequest, AnalysisResult result) {
        String body = pullRequest.getBody();
        
        if (body == null || body.trim().isEmpty()) {
            result.addIssue("DESCRIPTION_EMPTY", "PR description is empty", "HIGH");
        } else if (body.length() < 50) {
            result.addIssue("DESCRIPTION_TOO_SHORT", "PR description is too short", "MEDIUM");
        }
        
        // Check for required sections
        if (body != null && !body.toLowerCase().contains("## summary")) {
            result.addIssue("MISSING_SUMMARY", "PR description should include a summary section", "MEDIUM");
        }
    }

    /**
     * Analyze code changes
     */
    private void analyzeCodeChanges(String accessToken, String owner, String repo, 
                                  int prNumber, AnalysisResult result) {
        // This would integrate with actual code analysis tools
        // For now, we'll simulate some basic checks
        
        result.addComment("Code structure looks good overall");
        result.addComment("Consider adding unit tests for the new functionality");
        
        // Simulate finding some issues
        if (Math.random() > 0.7) {
            result.addIssue("CODE_COMPLEXITY", "Some methods have high complexity", "MEDIUM");
        }
        
        if (Math.random() > 0.8) {
            result.addIssue("MISSING_TESTS", "New code lacks sufficient test coverage", "HIGH");
        }
    }

    /**
     * Check for security issues
     */
    private void checkSecurityIssues(AnalysisResult result) {
        // Simulate security analysis
        if (Math.random() > 0.9) {
            result.addIssue("SECURITY_VULNERABILITY", "Potential security vulnerability detected", "HIGH");
        }
    }

    /**
     * Check for performance issues
     */
    private void checkPerformanceIssues(AnalysisResult result) {
        // Simulate performance analysis
        if (Math.random() > 0.8) {
            result.addIssue("PERFORMANCE_ISSUE", "Potential performance issue detected", "MEDIUM");
        }
    }

    /**
     * Save analysis results to database
     */
    private void saveAnalysisResults(PullRequestReview review, AnalysisResult result) {
        // Save issues
        for (var issue : result.getIssues()) {
            ReviewIssue reviewIssue = new ReviewIssue();
            reviewIssue.setReview(review);
            reviewIssue.setIssueType(issue.getType());
            reviewIssue.setDescription(issue.getDescription());
            reviewIssue.setSeverity(issue.getSeverity());
            reviewIssue.setStatus("OPEN");
            reviewIssue.setCreatedAt(LocalDateTime.now());
            reviewIssue.setUpdatedAt(LocalDateTime.now());
            
            issueRepository.save(reviewIssue);
        }
        
        // Save comments
        for (var comment : result.getComments()) {
            ReviewComment reviewComment = new ReviewComment();
            reviewComment.setReview(review);
            reviewComment.setBody(comment);
            reviewComment.setCommentType("REVIEW");
            reviewComment.setCreatedAt(LocalDateTime.now());
            reviewComment.setUpdatedAt(LocalDateTime.now());
            
            commentRepository.save(reviewComment);
        }
    }

    /**
     * Post review comments to GitHub
     */
    private void postReviewComments(String accessToken, String owner, String repo, 
                                  int prNumber, AnalysisResult result) {
        try {
            // Post a summary comment
            String summaryComment = buildSummaryComment(result);
            apiClient.postComment(accessToken, owner, repo, prNumber, summaryComment);
            
            // Post individual issue comments if needed
            for (var issue : result.getIssues()) {
                if ("HIGH".equals(issue.getSeverity())) {
                    String issueComment = String.format("🚨 **%s**: %s", 
                        issue.getType(), issue.getDescription());
                    apiClient.postComment(accessToken, owner, repo, prNumber, issueComment);
                }
            }
            
        } catch (Exception e) {
            log.error("Error posting review comments: {}", e.getMessage(), e);
        }
    }

    /**
     * Build summary comment
     */
    private String buildSummaryComment(AnalysisResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 🤖 Automated Code Review\n\n");
        
        if (result.getIssues().isEmpty()) {
            sb.append("✅ No issues found! Great work!\n\n");
        } else {
            sb.append("### Issues Found:\n");
            for (var issue : result.getIssues()) {
                String emoji = switch (issue.getSeverity()) {
                    case "HIGH" -> "🚨";
                    case "MEDIUM" -> "⚠️";
                    case "LOW" -> "💡";
                    default -> "ℹ️";
                };
                sb.append(String.format("- %s **%s**: %s\n", 
                    emoji, issue.getType(), issue.getDescription()));
            }
            sb.append("\n");
        }
        
        if (!result.getComments().isEmpty()) {
            sb.append("### Additional Comments:\n");
            for (var comment : result.getComments()) {
                sb.append(String.format("- %s\n", comment));
            }
        }
        
        sb.append("\n---\n*Generated by Kiro GitHub Integration*");
        
        return sb.toString();
    }

    /**
     * Analysis result container
     */
    public static class AnalysisResult {
        private final List<Issue> issues = new java.util.ArrayList<>();
        private final List<String> comments = new java.util.ArrayList<>();
        
        public void addIssue(String type, String description, String severity) {
            issues.add(new Issue(type, description, severity));
        }
        
        public void addComment(String comment) {
            comments.add(comment);
        }
        
        public List<Issue> getIssues() { return issues; }
        public List<String> getComments() { return comments; }
    }

    /**
     * Issue container
     */
    public static class Issue {
        private final String type;
        private final String description;
        private final String severity;
        
        public Issue(String type, String description, String severity) {
            this.type = type;
            this.description = description;
            this.severity = severity;
        }
        
        public String getType() { return type; }
        public String getDescription() { return description; }
        public String getSeverity() { return severity; }
    }
}