package com.zamaz.mcp.github.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Feedback Entity Tests")
class FeedbackTest {

    private Feedback feedback;
    private ReviewIssue mockReviewIssue;
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
        
        mockReviewIssue = ReviewIssue.builder()
                .id(1L)
                .review(mockReview)
                .filePath("src/main/java/TestFile.java")
                .lineStart(10)
                .lineEnd(15)
                .issueType("CODE_SMELL")
                .severity(ReviewIssue.IssueSeverity.MINOR)
                .description("Test issue")
                .autoFixable(true)
                .build();
        
        feedback = Feedback.builder()
                .id(1L)
                .reviewIssue(mockReviewIssue)
                .feedbackType(Feedback.FeedbackType.HELPFUL)
                .content("This feedback is helpful")
                .createdAt(testTime)
                .build();
    }

    @Test
    @DisplayName("Should create Feedback with builder pattern")
    void testBuilderPattern() {
        Feedback fb = Feedback.builder()
                .reviewIssue(mockReviewIssue)
                .feedbackType(Feedback.FeedbackType.NOT_HELPFUL)
                .content("Not helpful feedback")
                .build();

        assertNotNull(fb);
        assertEquals(mockReviewIssue, fb.getReviewIssue());
        assertEquals(Feedback.FeedbackType.NOT_HELPFUL, fb.getFeedbackType());
        assertEquals("Not helpful feedback", fb.getContent());
    }

    @Test
    @DisplayName("Should create Feedback with no-args constructor")
    void testNoArgsConstructor() {
        Feedback fb = new Feedback();
        assertNotNull(fb);
        assertNull(fb.getId());
        assertNull(fb.getReviewIssue());
        assertNull(fb.getFeedbackType());
        assertNull(fb.getContent());
        assertNull(fb.getCreatedAt());
    }

    @Test
    @DisplayName("Should create Feedback with all-args constructor")
    void testAllArgsConstructor() {
        Feedback fb = new Feedback(
                1L, mockReviewIssue, Feedback.FeedbackType.HELPFUL, 
                "This feedback is helpful", testTime
        );

        assertNotNull(fb);
        assertEquals(1L, fb.getId());
        assertEquals(mockReviewIssue, fb.getReviewIssue());
        assertEquals(Feedback.FeedbackType.HELPFUL, fb.getFeedbackType());
        assertEquals("This feedback is helpful", fb.getContent());
        assertEquals(testTime, fb.getCreatedAt());
    }

    @Test
    @DisplayName("Should get and set review issue")
    void testReviewIssue() {
        assertEquals(mockReviewIssue, feedback.getReviewIssue());
        
        ReviewIssue newIssue = ReviewIssue.builder()
                .id(2L)
                .review(mockReview)
                .filePath("src/test/java/NewTestFile.java")
                .lineStart(20)
                .lineEnd(25)
                .issueType("BUG")
                .severity(ReviewIssue.IssueSeverity.MAJOR)
                .description("New test issue")
                .autoFixable(false)
                .build();
        
        feedback.setReviewIssue(newIssue);
        assertEquals(newIssue, feedback.getReviewIssue());
    }

    @Test
    @DisplayName("Should get and set feedback type")
    void testFeedbackType() {
        assertEquals(Feedback.FeedbackType.HELPFUL, feedback.getFeedbackType());
        
        feedback.setFeedbackType(Feedback.FeedbackType.INCORRECT);
        assertEquals(Feedback.FeedbackType.INCORRECT, feedback.getFeedbackType());
    }

    @Test
    @DisplayName("Should get and set content")
    void testContent() {
        assertEquals("This feedback is helpful", feedback.getContent());
        
        feedback.setContent("Updated feedback content");
        assertEquals("Updated feedback content", feedback.getContent());
    }

    @Test
    @DisplayName("Should get and set created at timestamp")
    void testCreatedAt() {
        assertEquals(testTime, feedback.getCreatedAt());
        
        LocalDateTime newCreatedAt = testTime.plusMinutes(30);
        feedback.setCreatedAt(newCreatedAt);
        assertEquals(newCreatedAt, feedback.getCreatedAt());
    }

    @Test
    @DisplayName("Should test FeedbackType enum values")
    void testFeedbackTypeEnum() {
        assertEquals(5, Feedback.FeedbackType.values().length);
        
        assertTrue(containsValue(Feedback.FeedbackType.values(), Feedback.FeedbackType.HELPFUL));
        assertTrue(containsValue(Feedback.FeedbackType.values(), Feedback.FeedbackType.NOT_HELPFUL));
        assertTrue(containsValue(Feedback.FeedbackType.values(), Feedback.FeedbackType.INCORRECT));
        assertTrue(containsValue(Feedback.FeedbackType.values(), Feedback.FeedbackType.SPAM));
        assertTrue(containsValue(Feedback.FeedbackType.values(), Feedback.FeedbackType.OTHER));
        
        assertEquals("HELPFUL", Feedback.FeedbackType.HELPFUL.toString());
        assertEquals("NOT_HELPFUL", Feedback.FeedbackType.NOT_HELPFUL.toString());
        assertEquals("INCORRECT", Feedback.FeedbackType.INCORRECT.toString());
        assertEquals("SPAM", Feedback.FeedbackType.SPAM.toString());
        assertEquals("OTHER", Feedback.FeedbackType.OTHER.toString());
    }

    @Test
    @DisplayName("Should test equals and hashCode")
    void testEqualsAndHashCode() {
        Feedback fb1 = Feedback.builder()
                .id(1L)
                .reviewIssue(mockReviewIssue)
                .feedbackType(Feedback.FeedbackType.HELPFUL)
                .content("This feedback is helpful")
                .createdAt(testTime)
                .build();

        Feedback fb2 = Feedback.builder()
                .id(1L)
                .reviewIssue(mockReviewIssue)
                .feedbackType(Feedback.FeedbackType.HELPFUL)
                .content("This feedback is helpful")
                .createdAt(testTime)
                .build();

        Feedback fb3 = Feedback.builder()
                .id(2L)
                .reviewIssue(mockReviewIssue)
                .feedbackType(Feedback.FeedbackType.NOT_HELPFUL)
                .content("Different feedback")
                .createdAt(testTime.plusMinutes(10))
                .build();

        // Test equals
        assertEquals(fb1, fb2);
        assertNotEquals(fb1, fb3);
        assertNotEquals(fb1, null);
        assertNotEquals(fb1, new Object());

        // Test hashCode
        assertEquals(fb1.hashCode(), fb2.hashCode());
        assertNotEquals(fb1.hashCode(), fb3.hashCode());
    }

    @Test
    @DisplayName("Should test toString method")
    void testToString() {
        String toString = feedback.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("Feedback"));
        assertTrue(toString.contains("feedbackType=HELPFUL"));
        assertTrue(toString.contains("content=This feedback is helpful"));
    }

    @Test
    @DisplayName("Should handle null content")
    void testNullContent() {
        feedback.setContent(null);
        assertNull(feedback.getContent());
    }

    @Test
    @DisplayName("Should handle empty content")
    void testEmptyContent() {
        feedback.setContent("");
        assertEquals("", feedback.getContent());
    }

    @Test
    @DisplayName("Should create feedback with all feedback types")
    void testAllFeedbackTypes() {
        Feedback helpfulFeedback = Feedback.builder()
                .reviewIssue(mockReviewIssue)
                .feedbackType(Feedback.FeedbackType.HELPFUL)
                .content("This is helpful")
                .build();

        Feedback notHelpfulFeedback = Feedback.builder()
                .reviewIssue(mockReviewIssue)
                .feedbackType(Feedback.FeedbackType.NOT_HELPFUL)
                .content("This is not helpful")
                .build();

        Feedback incorrectFeedback = Feedback.builder()
                .reviewIssue(mockReviewIssue)
                .feedbackType(Feedback.FeedbackType.INCORRECT)
                .content("This is incorrect")
                .build();

        Feedback spamFeedback = Feedback.builder()
                .reviewIssue(mockReviewIssue)
                .feedbackType(Feedback.FeedbackType.SPAM)
                .content("This is spam")
                .build();

        Feedback otherFeedback = Feedback.builder()
                .reviewIssue(mockReviewIssue)
                .feedbackType(Feedback.FeedbackType.OTHER)
                .content("This is other type")
                .build();

        assertEquals(Feedback.FeedbackType.HELPFUL, helpfulFeedback.getFeedbackType());
        assertEquals(Feedback.FeedbackType.NOT_HELPFUL, notHelpfulFeedback.getFeedbackType());
        assertEquals(Feedback.FeedbackType.INCORRECT, incorrectFeedback.getFeedbackType());
        assertEquals(Feedback.FeedbackType.SPAM, spamFeedback.getFeedbackType());
        assertEquals(Feedback.FeedbackType.OTHER, otherFeedback.getFeedbackType());
    }

    @Test
    @DisplayName("Should handle long content")
    void testLongContent() {
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longContent.append("This is a very long feedback content that keeps repeating. ");
        }
        
        feedback.setContent(longContent.toString());
        assertEquals(longContent.toString(), feedback.getContent());
    }

    @Test
    @DisplayName("Should handle multiline content")
    void testMultilineContent() {
        String multilineContent = "This is feedback\nthat spans multiple lines\nand contains line breaks";
        feedback.setContent(multilineContent);
        assertEquals(multilineContent, feedback.getContent());
    }

    @Test
    @DisplayName("Should handle content with special characters")
    void testSpecialCharactersContent() {
        String specialContent = "Feedback with special chars: @#$%^&*()[]{}|;':\",./<>?`~";
        feedback.setContent(specialContent);
        assertEquals(specialContent, feedback.getContent());
    }

    @Test
    @DisplayName("Should handle UTF-8 content")
    void testUTF8Content() {
        String utf8Content = "Feedback with UTF-8 characters: 🚀 📝 ✅ 🔥 💯 한국어 日本語 العربية";
        feedback.setContent(utf8Content);
        assertEquals(utf8Content, feedback.getContent());
    }

    @Test
    @DisplayName("Should create feedback without content")
    void testFeedbackWithoutContent() {
        Feedback fb = Feedback.builder()
                .reviewIssue(mockReviewIssue)
                .feedbackType(Feedback.FeedbackType.HELPFUL)
                .build();

        assertNull(fb.getContent());
        assertEquals(Feedback.FeedbackType.HELPFUL, fb.getFeedbackType());
    }

    @Test
    @DisplayName("Should handle different review issues")
    void testDifferentReviewIssues() {
        ReviewIssue[] issues = {
            ReviewIssue.builder()
                .id(1L)
                .review(mockReview)
                .filePath("src/main/java/File1.java")
                .lineStart(10)
                .lineEnd(10)
                .issueType("BUG")
                .severity(ReviewIssue.IssueSeverity.CRITICAL)
                .description("Critical bug")
                .autoFixable(false)
                .build(),
            ReviewIssue.builder()
                .id(2L)
                .review(mockReview)
                .filePath("src/main/java/File2.java")
                .lineStart(20)
                .lineEnd(25)
                .issueType("CODE_SMELL")
                .severity(ReviewIssue.IssueSeverity.MINOR)
                .description("Code smell issue")
                .autoFixable(true)
                .build()
        };
        
        for (ReviewIssue issue : issues) {
            feedback.setReviewIssue(issue);
            assertEquals(issue, feedback.getReviewIssue());
        }
    }

    @Test
    @DisplayName("Should validate feedback type ordering")
    void testFeedbackTypeOrdering() {
        // Test that enum values are in expected order
        Feedback.FeedbackType[] types = Feedback.FeedbackType.values();
        assertEquals(Feedback.FeedbackType.HELPFUL, types[0]);
        assertEquals(Feedback.FeedbackType.NOT_HELPFUL, types[1]);
        assertEquals(Feedback.FeedbackType.INCORRECT, types[2]);
        assertEquals(Feedback.FeedbackType.SPAM, types[3]);
        assertEquals(Feedback.FeedbackType.OTHER, types[4]);
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