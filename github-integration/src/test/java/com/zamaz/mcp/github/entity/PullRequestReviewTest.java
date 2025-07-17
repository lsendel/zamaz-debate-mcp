package com.zamaz.mcp.github.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PullRequestReview Entity Tests")
class PullRequestReviewTest {

    private PullRequestReview pullRequestReview;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.now();
        pullRequestReview = PullRequestReview.builder()
                .id(1L)
                .repositoryId(12345L)
                .prNumber(123)
                .prTitle("Test PR")
                .prAuthor("testauthor")
                .baseBranch("main")
                .headBranch("feature/test")
                .status(PullRequestReview.ReviewStatus.PENDING)
                .requestedAt(testTime)
                .startedAt(testTime.plusMinutes(5))
                .completedAt(testTime.plusMinutes(30))
                .filesReviewed(5)
                .linesReviewed(150)
                .criticalIssues(1)
                .majorIssues(2)
                .minorIssues(3)
                .suggestions(4)
                .autoFixable(2)
                .issues(new ArrayList<>())
                .comments(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Should create PullRequestReview with builder pattern")
    void testBuilderPattern() {
        PullRequestReview review = PullRequestReview.builder()
                .repositoryId(99999L)
                .prNumber(456)
                .prTitle("Builder Test PR")
                .prAuthor("builderauthor")
                .baseBranch("develop")
                .headBranch("feature/builder")
                .status(PullRequestReview.ReviewStatus.IN_PROGRESS)
                .requestedAt(testTime)
                .filesReviewed(10)
                .linesReviewed(300)
                .build();

        assertNotNull(review);
        assertEquals(99999L, review.getRepositoryId());
        assertEquals(456, review.getPrNumber());
        assertEquals("Builder Test PR", review.getPrTitle());
        assertEquals("builderauthor", review.getPrAuthor());
        assertEquals("develop", review.getBaseBranch());
        assertEquals("feature/builder", review.getHeadBranch());
        assertEquals(PullRequestReview.ReviewStatus.IN_PROGRESS, review.getStatus());
        assertEquals(testTime, review.getRequestedAt());
        assertEquals(10, review.getFilesReviewed());
        assertEquals(300, review.getLinesReviewed());
        assertNotNull(review.getIssues());
        assertNotNull(review.getComments());
    }

    @Test
    @DisplayName("Should create PullRequestReview with no-args constructor")
    void testNoArgsConstructor() {
        PullRequestReview review = new PullRequestReview();
        assertNotNull(review);
        assertNull(review.getId());
        assertNull(review.getRepositoryId());
        assertNull(review.getPrNumber());
        assertNull(review.getPrTitle());
        assertNull(review.getPrAuthor());
        assertNull(review.getBaseBranch());
        assertNull(review.getHeadBranch());
        assertNull(review.getStatus());
        assertNull(review.getRequestedAt());
        assertNull(review.getStartedAt());
        assertNull(review.getCompletedAt());
        assertNull(review.getFilesReviewed());
        assertNull(review.getLinesReviewed());
        assertNull(review.getCriticalIssues());
        assertNull(review.getMajorIssues());
        assertNull(review.getMinorIssues());
        assertNull(review.getSuggestions());
        assertNull(review.getAutoFixable());
        assertNull(review.getIssues());
        assertNull(review.getComments());
    }

    @Test
    @DisplayName("Should create PullRequestReview with all-args constructor")
    void testAllArgsConstructor() {
        List<ReviewIssue> issues = new ArrayList<>();
        List<ReviewComment> comments = new ArrayList<>();
        
        PullRequestReview review = new PullRequestReview(
                1L, 12345L, 123, "Test PR", "testauthor", "main", "feature/test",
                PullRequestReview.ReviewStatus.PENDING, testTime, testTime.plusMinutes(5),
                testTime.plusMinutes(30), 5, 150, 1, 2, 3, 4, 2, issues, comments
        );

        assertNotNull(review);
        assertEquals(1L, review.getId());
        assertEquals(12345L, review.getRepositoryId());
        assertEquals(123, review.getPrNumber());
        assertEquals("Test PR", review.getPrTitle());
        assertEquals("testauthor", review.getPrAuthor());
        assertEquals("main", review.getBaseBranch());
        assertEquals("feature/test", review.getHeadBranch());
        assertEquals(PullRequestReview.ReviewStatus.PENDING, review.getStatus());
        assertEquals(testTime, review.getRequestedAt());
        assertEquals(testTime.plusMinutes(5), review.getStartedAt());
        assertEquals(testTime.plusMinutes(30), review.getCompletedAt());
        assertEquals(5, review.getFilesReviewed());
        assertEquals(150, review.getLinesReviewed());
        assertEquals(1, review.getCriticalIssues());
        assertEquals(2, review.getMajorIssues());
        assertEquals(3, review.getMinorIssues());
        assertEquals(4, review.getSuggestions());
        assertEquals(2, review.getAutoFixable());
        assertEquals(issues, review.getIssues());
        assertEquals(comments, review.getComments());
    }

    @Test
    @DisplayName("Should get and set repository ID")
    void testRepositoryId() {
        assertEquals(12345L, pullRequestReview.getRepositoryId());
        
        pullRequestReview.setRepositoryId(54321L);
        assertEquals(54321L, pullRequestReview.getRepositoryId());
    }

    @Test
    @DisplayName("Should get and set PR number")
    void testPrNumber() {
        assertEquals(123, pullRequestReview.getPrNumber());
        
        pullRequestReview.setPrNumber(456);
        assertEquals(456, pullRequestReview.getPrNumber());
    }

    @Test
    @DisplayName("Should get and set PR title")
    void testPrTitle() {
        assertEquals("Test PR", pullRequestReview.getPrTitle());
        
        pullRequestReview.setPrTitle("Updated PR Title");
        assertEquals("Updated PR Title", pullRequestReview.getPrTitle());
    }

    @Test
    @DisplayName("Should get and set PR author")
    void testPrAuthor() {
        assertEquals("testauthor", pullRequestReview.getPrAuthor());
        
        pullRequestReview.setPrAuthor("newauthor");
        assertEquals("newauthor", pullRequestReview.getPrAuthor());
    }

    @Test
    @DisplayName("Should get and set base branch")
    void testBaseBranch() {
        assertEquals("main", pullRequestReview.getBaseBranch());
        
        pullRequestReview.setBaseBranch("develop");
        assertEquals("develop", pullRequestReview.getBaseBranch());
    }

    @Test
    @DisplayName("Should get and set head branch")
    void testHeadBranch() {
        assertEquals("feature/test", pullRequestReview.getHeadBranch());
        
        pullRequestReview.setHeadBranch("feature/newfeature");
        assertEquals("feature/newfeature", pullRequestReview.getHeadBranch());
    }

    @Test
    @DisplayName("Should get and set status")
    void testStatus() {
        assertEquals(PullRequestReview.ReviewStatus.PENDING, pullRequestReview.getStatus());
        
        pullRequestReview.setStatus(PullRequestReview.ReviewStatus.COMPLETED);
        assertEquals(PullRequestReview.ReviewStatus.COMPLETED, pullRequestReview.getStatus());
    }

    @Test
    @DisplayName("Should get and set requested at timestamp")
    void testRequestedAt() {
        assertEquals(testTime, pullRequestReview.getRequestedAt());
        
        LocalDateTime newRequestedAt = testTime.minusHours(1);
        pullRequestReview.setRequestedAt(newRequestedAt);
        assertEquals(newRequestedAt, pullRequestReview.getRequestedAt());
    }

    @Test
    @DisplayName("Should get and set started at timestamp")
    void testStartedAt() {
        assertEquals(testTime.plusMinutes(5), pullRequestReview.getStartedAt());
        
        LocalDateTime newStartedAt = testTime.plusMinutes(10);
        pullRequestReview.setStartedAt(newStartedAt);
        assertEquals(newStartedAt, pullRequestReview.getStartedAt());
    }

    @Test
    @DisplayName("Should get and set completed at timestamp")
    void testCompletedAt() {
        assertEquals(testTime.plusMinutes(30), pullRequestReview.getCompletedAt());
        
        LocalDateTime newCompletedAt = testTime.plusMinutes(45);
        pullRequestReview.setCompletedAt(newCompletedAt);
        assertEquals(newCompletedAt, pullRequestReview.getCompletedAt());
    }

    @Test
    @DisplayName("Should get and set files reviewed")
    void testFilesReviewed() {
        assertEquals(5, pullRequestReview.getFilesReviewed());
        
        pullRequestReview.setFilesReviewed(10);
        assertEquals(10, pullRequestReview.getFilesReviewed());
    }

    @Test
    @DisplayName("Should get and set lines reviewed")
    void testLinesReviewed() {
        assertEquals(150, pullRequestReview.getLinesReviewed());
        
        pullRequestReview.setLinesReviewed(300);
        assertEquals(300, pullRequestReview.getLinesReviewed());
    }

    @Test
    @DisplayName("Should get and set critical issues")
    void testCriticalIssues() {
        assertEquals(1, pullRequestReview.getCriticalIssues());
        
        pullRequestReview.setCriticalIssues(3);
        assertEquals(3, pullRequestReview.getCriticalIssues());
    }

    @Test
    @DisplayName("Should get and set major issues")
    void testMajorIssues() {
        assertEquals(2, pullRequestReview.getMajorIssues());
        
        pullRequestReview.setMajorIssues(5);
        assertEquals(5, pullRequestReview.getMajorIssues());
    }

    @Test
    @DisplayName("Should get and set minor issues")
    void testMinorIssues() {
        assertEquals(3, pullRequestReview.getMinorIssues());
        
        pullRequestReview.setMinorIssues(7);
        assertEquals(7, pullRequestReview.getMinorIssues());
    }

    @Test
    @DisplayName("Should get and set suggestions")
    void testSuggestions() {
        assertEquals(4, pullRequestReview.getSuggestions());
        
        pullRequestReview.setSuggestions(8);
        assertEquals(8, pullRequestReview.getSuggestions());
    }

    @Test
    @DisplayName("Should get and set auto fixable")
    void testAutoFixable() {
        assertEquals(2, pullRequestReview.getAutoFixable());
        
        pullRequestReview.setAutoFixable(4);
        assertEquals(4, pullRequestReview.getAutoFixable());
    }

    @Test
    @DisplayName("Should get and set issues list")
    void testIssues() {
        assertNotNull(pullRequestReview.getIssues());
        assertTrue(pullRequestReview.getIssues().isEmpty());
        
        List<ReviewIssue> newIssues = new ArrayList<>();
        pullRequestReview.setIssues(newIssues);
        assertEquals(newIssues, pullRequestReview.getIssues());
    }

    @Test
    @DisplayName("Should get and set comments list")
    void testComments() {
        assertNotNull(pullRequestReview.getComments());
        assertTrue(pullRequestReview.getComments().isEmpty());
        
        List<ReviewComment> newComments = new ArrayList<>();
        pullRequestReview.setComments(newComments);
        assertEquals(newComments, pullRequestReview.getComments());
    }

    @Test
    @DisplayName("Should test ReviewStatus enum values")
    void testReviewStatusEnum() {
        assertEquals(4, PullRequestReview.ReviewStatus.values().length);
        
        assertTrue(containsValue(PullRequestReview.ReviewStatus.values(), PullRequestReview.ReviewStatus.PENDING));
        assertTrue(containsValue(PullRequestReview.ReviewStatus.values(), PullRequestReview.ReviewStatus.IN_PROGRESS));
        assertTrue(containsValue(PullRequestReview.ReviewStatus.values(), PullRequestReview.ReviewStatus.COMPLETED));
        assertTrue(containsValue(PullRequestReview.ReviewStatus.values(), PullRequestReview.ReviewStatus.FAILED));
        
        assertEquals("PENDING", PullRequestReview.ReviewStatus.PENDING.toString());
        assertEquals("IN_PROGRESS", PullRequestReview.ReviewStatus.IN_PROGRESS.toString());
        assertEquals("COMPLETED", PullRequestReview.ReviewStatus.COMPLETED.toString());
        assertEquals("FAILED", PullRequestReview.ReviewStatus.FAILED.toString());
    }

    @Test
    @DisplayName("Should test equals and hashCode")
    void testEqualsAndHashCode() {
        PullRequestReview review1 = PullRequestReview.builder()
                .id(1L)
                .repositoryId(12345L)
                .prNumber(123)
                .prTitle("Test PR")
                .prAuthor("testauthor")
                .baseBranch("main")
                .headBranch("feature/test")
                .status(PullRequestReview.ReviewStatus.PENDING)
                .requestedAt(testTime)
                .build();

        PullRequestReview review2 = PullRequestReview.builder()
                .id(1L)
                .repositoryId(12345L)
                .prNumber(123)
                .prTitle("Test PR")
                .prAuthor("testauthor")
                .baseBranch("main")
                .headBranch("feature/test")
                .status(PullRequestReview.ReviewStatus.PENDING)
                .requestedAt(testTime)
                .build();

        PullRequestReview review3 = PullRequestReview.builder()
                .id(2L)
                .repositoryId(54321L)
                .prNumber(456)
                .prTitle("Other PR")
                .prAuthor("otherauthor")
                .baseBranch("develop")
                .headBranch("feature/other")
                .status(PullRequestReview.ReviewStatus.COMPLETED)
                .requestedAt(testTime.plusHours(1))
                .build();

        // Test equals
        assertEquals(review1, review2);
        assertNotEquals(review1, review3);
        assertNotEquals(review1, null);
        assertNotEquals(review1, new Object());

        // Test hashCode
        assertEquals(review1.hashCode(), review2.hashCode());
        assertNotEquals(review1.hashCode(), review3.hashCode());
    }

    @Test
    @DisplayName("Should test toString method")
    void testToString() {
        String toString = pullRequestReview.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("PullRequestReview"));
        assertTrue(toString.contains("repositoryId=12345"));
        assertTrue(toString.contains("prNumber=123"));
        assertTrue(toString.contains("prTitle=Test PR"));
        assertTrue(toString.contains("prAuthor=testauthor"));
        assertTrue(toString.contains("baseBranch=main"));
        assertTrue(toString.contains("headBranch=feature/test"));
        assertTrue(toString.contains("status=PENDING"));
    }

    @Test
    @DisplayName("Should handle null timestamps")
    void testNullTimestamps() {
        pullRequestReview.setStartedAt(null);
        pullRequestReview.setCompletedAt(null);
        
        assertNull(pullRequestReview.getStartedAt());
        assertNull(pullRequestReview.getCompletedAt());
    }

    @Test
    @DisplayName("Should handle zero issue counts")
    void testZeroIssueCounts() {
        pullRequestReview.setCriticalIssues(0);
        pullRequestReview.setMajorIssues(0);
        pullRequestReview.setMinorIssues(0);
        pullRequestReview.setSuggestions(0);
        pullRequestReview.setAutoFixable(0);
        
        assertEquals(0, pullRequestReview.getCriticalIssues());
        assertEquals(0, pullRequestReview.getMajorIssues());
        assertEquals(0, pullRequestReview.getMinorIssues());
        assertEquals(0, pullRequestReview.getSuggestions());
        assertEquals(0, pullRequestReview.getAutoFixable());
    }

    @Test
    @DisplayName("Should create review with all statuses")
    void testAllStatuses() {
        PullRequestReview pendingReview = PullRequestReview.builder()
                .repositoryId(1L)
                .prNumber(1)
                .prTitle("Pending PR")
                .prAuthor("author")
                .baseBranch("main")
                .headBranch("feature/pending")
                .status(PullRequestReview.ReviewStatus.PENDING)
                .requestedAt(testTime)
                .build();

        PullRequestReview inProgressReview = PullRequestReview.builder()
                .repositoryId(2L)
                .prNumber(2)
                .prTitle("In Progress PR")
                .prAuthor("author")
                .baseBranch("main")
                .headBranch("feature/inprogress")
                .status(PullRequestReview.ReviewStatus.IN_PROGRESS)
                .requestedAt(testTime)
                .build();

        PullRequestReview completedReview = PullRequestReview.builder()
                .repositoryId(3L)
                .prNumber(3)
                .prTitle("Completed PR")
                .prAuthor("author")
                .baseBranch("main")
                .headBranch("feature/completed")
                .status(PullRequestReview.ReviewStatus.COMPLETED)
                .requestedAt(testTime)
                .build();

        PullRequestReview failedReview = PullRequestReview.builder()
                .repositoryId(4L)
                .prNumber(4)
                .prTitle("Failed PR")
                .prAuthor("author")
                .baseBranch("main")
                .headBranch("feature/failed")
                .status(PullRequestReview.ReviewStatus.FAILED)
                .requestedAt(testTime)
                .build();

        assertEquals(PullRequestReview.ReviewStatus.PENDING, pendingReview.getStatus());
        assertEquals(PullRequestReview.ReviewStatus.IN_PROGRESS, inProgressReview.getStatus());
        assertEquals(PullRequestReview.ReviewStatus.COMPLETED, completedReview.getStatus());
        assertEquals(PullRequestReview.ReviewStatus.FAILED, failedReview.getStatus());
    }

    private <T> boolean containsValue(T[] array, T value) {
        for (T item : array) {
            if (item.equals(value)) {
                return true;
            }
        }
        return false;
    }
}