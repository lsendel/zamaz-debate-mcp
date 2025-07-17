package com.zamaz.mcp.github.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ReviewIssue Entity Tests")
class ReviewIssueTest {

    private ReviewIssue reviewIssue;
    private PullRequestReview mockReview;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.now();
        
        mockReview = PullRequestReview.builder()
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
        
        reviewIssue = ReviewIssue.builder()
                .id(1L)
                .review(mockReview)
                .filePath("src/main/java/TestFile.java")
                .lineStart(10)
                .lineEnd(15)
                .issueType("CODE_SMELL")
                .severity(ReviewIssue.IssueSeverity.MINOR)
                .description("This is a test issue description")
                .suggestion("Consider refactoring this code")
                .autoFixable(true)
                .fixDescription("Automatically refactor using IDE tools")
                .commentId(999L)
                .createdAt(testTime)
                .feedback(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Should create ReviewIssue with builder pattern")
    void testBuilderPattern() {
        ReviewIssue issue = ReviewIssue.builder()
                .review(mockReview)
                .filePath("src/test/java/TestFile.java")
                .lineStart(20)
                .lineEnd(25)
                .issueType("BUG")
                .severity(ReviewIssue.IssueSeverity.CRITICAL)
                .description("Critical bug found")
                .suggestion("Fix this immediately")
                .autoFixable(false)
                .fixDescription("Manual fix required")
                .commentId(888L)
                .build();

        assertNotNull(issue);
        assertEquals(mockReview, issue.getReview());
        assertEquals("src/test/java/TestFile.java", issue.getFilePath());
        assertEquals(20, issue.getLineStart());
        assertEquals(25, issue.getLineEnd());
        assertEquals("BUG", issue.getIssueType());
        assertEquals(ReviewIssue.IssueSeverity.CRITICAL, issue.getSeverity());
        assertEquals("Critical bug found", issue.getDescription());
        assertEquals("Fix this immediately", issue.getSuggestion());
        assertFalse(issue.isAutoFixable());
        assertEquals("Manual fix required", issue.getFixDescription());
        assertEquals(888L, issue.getCommentId());
        assertNotNull(issue.getFeedback());
    }

    @Test
    @DisplayName("Should create ReviewIssue with no-args constructor")
    void testNoArgsConstructor() {
        ReviewIssue issue = new ReviewIssue();
        assertNotNull(issue);
        assertNull(issue.getId());
        assertNull(issue.getReview());
        assertNull(issue.getFilePath());
        assertNull(issue.getLineStart());
        assertNull(issue.getLineEnd());
        assertNull(issue.getIssueType());
        assertNull(issue.getSeverity());
        assertNull(issue.getDescription());
        assertNull(issue.getSuggestion());
        assertFalse(issue.isAutoFixable());
        assertNull(issue.getFixDescription());
        assertNull(issue.getCommentId());
        assertNull(issue.getCreatedAt());
        assertNull(issue.getFeedback());
    }

    @Test
    @DisplayName("Should create ReviewIssue with all-args constructor")
    void testAllArgsConstructor() {
        List<Feedback> feedback = new ArrayList<>();
        
        ReviewIssue issue = new ReviewIssue(
                1L, mockReview, "src/main/java/TestFile.java", 10, 15,
                "CODE_SMELL", ReviewIssue.IssueSeverity.MINOR,
                "This is a test issue description", "Consider refactoring this code",
                true, "Automatically refactor using IDE tools", 999L, testTime, feedback
        );

        assertNotNull(issue);
        assertEquals(1L, issue.getId());
        assertEquals(mockReview, issue.getReview());
        assertEquals("src/main/java/TestFile.java", issue.getFilePath());
        assertEquals(10, issue.getLineStart());
        assertEquals(15, issue.getLineEnd());
        assertEquals("CODE_SMELL", issue.getIssueType());
        assertEquals(ReviewIssue.IssueSeverity.MINOR, issue.getSeverity());
        assertEquals("This is a test issue description", issue.getDescription());
        assertEquals("Consider refactoring this code", issue.getSuggestion());
        assertTrue(issue.isAutoFixable());
        assertEquals("Automatically refactor using IDE tools", issue.getFixDescription());
        assertEquals(999L, issue.getCommentId());
        assertEquals(testTime, issue.getCreatedAt());
        assertEquals(feedback, issue.getFeedback());
    }

    @Test
    @DisplayName("Should get and set review")
    void testReview() {
        assertEquals(mockReview, reviewIssue.getReview());
        
        PullRequestReview newReview = PullRequestReview.builder()
                .id(2L)
                .repositoryId(54321L)
                .prNumber(456)
                .prTitle("New PR")
                .prAuthor("newauthor")
                .baseBranch("develop")
                .headBranch("feature/new")
                .status(PullRequestReview.ReviewStatus.IN_PROGRESS)
                .requestedAt(testTime)
                .build();
        
        reviewIssue.setReview(newReview);
        assertEquals(newReview, reviewIssue.getReview());
    }

    @Test
    @DisplayName("Should get and set file path")
    void testFilePath() {
        assertEquals("src/main/java/TestFile.java", reviewIssue.getFilePath());
        
        reviewIssue.setFilePath("src/test/java/NewTestFile.java");
        assertEquals("src/test/java/NewTestFile.java", reviewIssue.getFilePath());
    }

    @Test
    @DisplayName("Should get and set line start")
    void testLineStart() {
        assertEquals(10, reviewIssue.getLineStart());
        
        reviewIssue.setLineStart(20);
        assertEquals(20, reviewIssue.getLineStart());
    }

    @Test
    @DisplayName("Should get and set line end")
    void testLineEnd() {
        assertEquals(15, reviewIssue.getLineEnd());
        
        reviewIssue.setLineEnd(25);
        assertEquals(25, reviewIssue.getLineEnd());
    }

    @Test
    @DisplayName("Should get and set issue type")
    void testIssueType() {
        assertEquals("CODE_SMELL", reviewIssue.getIssueType());
        
        reviewIssue.setIssueType("BUG");
        assertEquals("BUG", reviewIssue.getIssueType());
    }

    @Test
    @DisplayName("Should get and set severity")
    void testSeverity() {
        assertEquals(ReviewIssue.IssueSeverity.MINOR, reviewIssue.getSeverity());
        
        reviewIssue.setSeverity(ReviewIssue.IssueSeverity.CRITICAL);
        assertEquals(ReviewIssue.IssueSeverity.CRITICAL, reviewIssue.getSeverity());
    }

    @Test
    @DisplayName("Should get and set description")
    void testDescription() {
        assertEquals("This is a test issue description", reviewIssue.getDescription());
        
        reviewIssue.setDescription("Updated description");
        assertEquals("Updated description", reviewIssue.getDescription());
    }

    @Test
    @DisplayName("Should get and set suggestion")
    void testSuggestion() {
        assertEquals("Consider refactoring this code", reviewIssue.getSuggestion());
        
        reviewIssue.setSuggestion("New suggestion");
        assertEquals("New suggestion", reviewIssue.getSuggestion());
    }

    @Test
    @DisplayName("Should get and set auto fixable")
    void testAutoFixable() {
        assertTrue(reviewIssue.isAutoFixable());
        
        reviewIssue.setAutoFixable(false);
        assertFalse(reviewIssue.isAutoFixable());
    }

    @Test
    @DisplayName("Should get and set fix description")
    void testFixDescription() {
        assertEquals("Automatically refactor using IDE tools", reviewIssue.getFixDescription());
        
        reviewIssue.setFixDescription("Manual fix required");
        assertEquals("Manual fix required", reviewIssue.getFixDescription());
    }

    @Test
    @DisplayName("Should get and set comment ID")
    void testCommentId() {
        assertEquals(999L, reviewIssue.getCommentId());
        
        reviewIssue.setCommentId(777L);
        assertEquals(777L, reviewIssue.getCommentId());
    }

    @Test
    @DisplayName("Should get and set created at timestamp")
    void testCreatedAt() {
        assertEquals(testTime, reviewIssue.getCreatedAt());
        
        LocalDateTime newCreatedAt = testTime.plusMinutes(30);
        reviewIssue.setCreatedAt(newCreatedAt);
        assertEquals(newCreatedAt, reviewIssue.getCreatedAt());
    }

    @Test
    @DisplayName("Should get and set feedback list")
    void testFeedback() {
        assertNotNull(reviewIssue.getFeedback());
        assertTrue(reviewIssue.getFeedback().isEmpty());
        
        List<Feedback> newFeedback = new ArrayList<>();
        reviewIssue.setFeedback(newFeedback);
        assertEquals(newFeedback, reviewIssue.getFeedback());
    }

    @Test
    @DisplayName("Should test IssueSeverity enum values")
    void testIssueSeverityEnum() {
        assertEquals(4, ReviewIssue.IssueSeverity.values().length);
        
        assertTrue(containsValue(ReviewIssue.IssueSeverity.values(), ReviewIssue.IssueSeverity.CRITICAL));
        assertTrue(containsValue(ReviewIssue.IssueSeverity.values(), ReviewIssue.IssueSeverity.MAJOR));
        assertTrue(containsValue(ReviewIssue.IssueSeverity.values(), ReviewIssue.IssueSeverity.MINOR));
        assertTrue(containsValue(ReviewIssue.IssueSeverity.values(), ReviewIssue.IssueSeverity.SUGGESTION));
        
        assertEquals("CRITICAL", ReviewIssue.IssueSeverity.CRITICAL.toString());
        assertEquals("MAJOR", ReviewIssue.IssueSeverity.MAJOR.toString());
        assertEquals("MINOR", ReviewIssue.IssueSeverity.MINOR.toString());
        assertEquals("SUGGESTION", ReviewIssue.IssueSeverity.SUGGESTION.toString());
    }

    @Test
    @DisplayName("Should test equals and hashCode")
    void testEqualsAndHashCode() {
        ReviewIssue issue1 = ReviewIssue.builder()
                .id(1L)
                .review(mockReview)
                .filePath("src/main/java/TestFile.java")
                .lineStart(10)
                .lineEnd(15)
                .issueType("CODE_SMELL")
                .severity(ReviewIssue.IssueSeverity.MINOR)
                .description("This is a test issue description")
                .autoFixable(true)
                .build();

        ReviewIssue issue2 = ReviewIssue.builder()
                .id(1L)
                .review(mockReview)
                .filePath("src/main/java/TestFile.java")
                .lineStart(10)
                .lineEnd(15)
                .issueType("CODE_SMELL")
                .severity(ReviewIssue.IssueSeverity.MINOR)
                .description("This is a test issue description")
                .autoFixable(true)
                .build();

        ReviewIssue issue3 = ReviewIssue.builder()
                .id(2L)
                .review(mockReview)
                .filePath("src/test/java/OtherFile.java")
                .lineStart(20)
                .lineEnd(25)
                .issueType("BUG")
                .severity(ReviewIssue.IssueSeverity.CRITICAL)
                .description("Different issue")
                .autoFixable(false)
                .build();

        // Test equals
        assertEquals(issue1, issue2);
        assertNotEquals(issue1, issue3);
        assertNotEquals(issue1, null);
        assertNotEquals(issue1, new Object());

        // Test hashCode
        assertEquals(issue1.hashCode(), issue2.hashCode());
        assertNotEquals(issue1.hashCode(), issue3.hashCode());
    }

    @Test
    @DisplayName("Should test toString method")
    void testToString() {
        String toString = reviewIssue.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("ReviewIssue"));
        assertTrue(toString.contains("filePath=src/main/java/TestFile.java"));
        assertTrue(toString.contains("lineStart=10"));
        assertTrue(toString.contains("lineEnd=15"));
        assertTrue(toString.contains("issueType=CODE_SMELL"));
        assertTrue(toString.contains("severity=MINOR"));
        assertTrue(toString.contains("autoFixable=true"));
    }

    @Test
    @DisplayName("Should handle null suggestion")
    void testNullSuggestion() {
        reviewIssue.setSuggestion(null);
        assertNull(reviewIssue.getSuggestion());
    }

    @Test
    @DisplayName("Should handle null fix description")
    void testNullFixDescription() {
        reviewIssue.setFixDescription(null);
        assertNull(reviewIssue.getFixDescription());
    }

    @Test
    @DisplayName("Should handle null comment ID")
    void testNullCommentId() {
        reviewIssue.setCommentId(null);
        assertNull(reviewIssue.getCommentId());
    }

    @Test
    @DisplayName("Should create issue with all severity levels")
    void testAllSeverityLevels() {
        ReviewIssue criticalIssue = ReviewIssue.builder()
                .review(mockReview)
                .filePath("src/main/java/CriticalFile.java")
                .lineStart(1)
                .lineEnd(1)
                .issueType("SECURITY")
                .severity(ReviewIssue.IssueSeverity.CRITICAL)
                .description("Critical security issue")
                .autoFixable(false)
                .build();

        ReviewIssue majorIssue = ReviewIssue.builder()
                .review(mockReview)
                .filePath("src/main/java/MajorFile.java")
                .lineStart(5)
                .lineEnd(5)
                .issueType("PERFORMANCE")
                .severity(ReviewIssue.IssueSeverity.MAJOR)
                .description("Major performance issue")
                .autoFixable(true)
                .build();

        ReviewIssue minorIssue = ReviewIssue.builder()
                .review(mockReview)
                .filePath("src/main/java/MinorFile.java")
                .lineStart(10)
                .lineEnd(10)
                .issueType("STYLE")
                .severity(ReviewIssue.IssueSeverity.MINOR)
                .description("Minor style issue")
                .autoFixable(true)
                .build();

        ReviewIssue suggestionIssue = ReviewIssue.builder()
                .review(mockReview)
                .filePath("src/main/java/SuggestionFile.java")
                .lineStart(15)
                .lineEnd(15)
                .issueType("IMPROVEMENT")
                .severity(ReviewIssue.IssueSeverity.SUGGESTION)
                .description("Suggestion for improvement")
                .autoFixable(true)
                .build();

        assertEquals(ReviewIssue.IssueSeverity.CRITICAL, criticalIssue.getSeverity());
        assertEquals(ReviewIssue.IssueSeverity.MAJOR, majorIssue.getSeverity());
        assertEquals(ReviewIssue.IssueSeverity.MINOR, minorIssue.getSeverity());
        assertEquals(ReviewIssue.IssueSeverity.SUGGESTION, suggestionIssue.getSeverity());
    }

    @Test
    @DisplayName("Should handle single line issues")
    void testSingleLineIssue() {
        ReviewIssue singleLineIssue = ReviewIssue.builder()
                .review(mockReview)
                .filePath("src/main/java/SingleLine.java")
                .lineStart(42)
                .lineEnd(42)
                .issueType("NAMING")
                .severity(ReviewIssue.IssueSeverity.MINOR)
                .description("Variable naming issue")
                .autoFixable(true)
                .build();

        assertEquals(42, singleLineIssue.getLineStart());
        assertEquals(42, singleLineIssue.getLineEnd());
    }

    @Test
    @DisplayName("Should handle multi-line issues")
    void testMultiLineIssue() {
        ReviewIssue multiLineIssue = ReviewIssue.builder()
                .review(mockReview)
                .filePath("src/main/java/MultiLine.java")
                .lineStart(100)
                .lineEnd(150)
                .issueType("COMPLEXITY")
                .severity(ReviewIssue.IssueSeverity.MAJOR)
                .description("Method too complex")
                .autoFixable(false)
                .build();

        assertEquals(100, multiLineIssue.getLineStart());
        assertEquals(150, multiLineIssue.getLineEnd());
        assertTrue(multiLineIssue.getLineEnd() > multiLineIssue.getLineStart());
    }

    @Test
    @DisplayName("Should handle different issue types")
    void testDifferentIssueTypes() {
        String[] issueTypes = {"BUG", "CODE_SMELL", "VULNERABILITY", "SECURITY_HOTSPOT", "DUPLICATION"};
        
        for (String issueType : issueTypes) {
            ReviewIssue issue = ReviewIssue.builder()
                    .review(mockReview)
                    .filePath("src/main/java/Test.java")
                    .lineStart(1)
                    .lineEnd(1)
                    .issueType(issueType)
                    .severity(ReviewIssue.IssueSeverity.MINOR)
                    .description("Test issue of type " + issueType)
                    .autoFixable(false)
                    .build();
            
            assertEquals(issueType, issue.getIssueType());
        }
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