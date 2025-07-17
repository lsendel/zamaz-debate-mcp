package com.zamaz.mcp.github.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ReviewComment Entity Tests")
class ReviewCommentTest {

    private ReviewComment reviewComment;
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
        
        reviewComment = ReviewComment.builder()
                .id(1L)
                .review(mockReview)
                .githubCommentId(999L)
                .filePath("src/main/java/TestFile.java")
                .line(42)
                .content("This is a test comment")
                .createdAt(testTime)
                .build();
    }

    @Test
    @DisplayName("Should create ReviewComment with builder pattern")
    void testBuilderPattern() {
        ReviewComment comment = ReviewComment.builder()
                .review(mockReview)
                .githubCommentId(888L)
                .filePath("src/test/java/TestFile.java")
                .line(100)
                .content("Builder pattern test comment")
                .build();

        assertNotNull(comment);
        assertEquals(mockReview, comment.getReview());
        assertEquals(888L, comment.getGithubCommentId());
        assertEquals("src/test/java/TestFile.java", comment.getFilePath());
        assertEquals(100, comment.getLine());
        assertEquals("Builder pattern test comment", comment.getContent());
    }

    @Test
    @DisplayName("Should create ReviewComment with no-args constructor")
    void testNoArgsConstructor() {
        ReviewComment comment = new ReviewComment();
        assertNotNull(comment);
        assertNull(comment.getId());
        assertNull(comment.getReview());
        assertNull(comment.getGithubCommentId());
        assertNull(comment.getFilePath());
        assertNull(comment.getLine());
        assertNull(comment.getContent());
        assertNull(comment.getCreatedAt());
    }

    @Test
    @DisplayName("Should create ReviewComment with all-args constructor")
    void testAllArgsConstructor() {
        ReviewComment comment = new ReviewComment(
                1L, mockReview, 999L, "src/main/java/TestFile.java", 
                42, "This is a test comment", testTime
        );

        assertNotNull(comment);
        assertEquals(1L, comment.getId());
        assertEquals(mockReview, comment.getReview());
        assertEquals(999L, comment.getGithubCommentId());
        assertEquals("src/main/java/TestFile.java", comment.getFilePath());
        assertEquals(42, comment.getLine());
        assertEquals("This is a test comment", comment.getContent());
        assertEquals(testTime, comment.getCreatedAt());
    }

    @Test
    @DisplayName("Should get and set review")
    void testReview() {
        assertEquals(mockReview, reviewComment.getReview());
        
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
        
        reviewComment.setReview(newReview);
        assertEquals(newReview, reviewComment.getReview());
    }

    @Test
    @DisplayName("Should get and set GitHub comment ID")
    void testGithubCommentId() {
        assertEquals(999L, reviewComment.getGithubCommentId());
        
        reviewComment.setGithubCommentId(777L);
        assertEquals(777L, reviewComment.getGithubCommentId());
    }

    @Test
    @DisplayName("Should get and set file path")
    void testFilePath() {
        assertEquals("src/main/java/TestFile.java", reviewComment.getFilePath());
        
        reviewComment.setFilePath("src/test/java/NewTestFile.java");
        assertEquals("src/test/java/NewTestFile.java", reviewComment.getFilePath());
    }

    @Test
    @DisplayName("Should get and set line number")
    void testLine() {
        assertEquals(42, reviewComment.getLine());
        
        reviewComment.setLine(100);
        assertEquals(100, reviewComment.getLine());
    }

    @Test
    @DisplayName("Should get and set content")
    void testContent() {
        assertEquals("This is a test comment", reviewComment.getContent());
        
        reviewComment.setContent("Updated comment content");
        assertEquals("Updated comment content", reviewComment.getContent());
    }

    @Test
    @DisplayName("Should get and set created at timestamp")
    void testCreatedAt() {
        assertEquals(testTime, reviewComment.getCreatedAt());
        
        LocalDateTime newCreatedAt = testTime.plusMinutes(30);
        reviewComment.setCreatedAt(newCreatedAt);
        assertEquals(newCreatedAt, reviewComment.getCreatedAt());
    }

    @Test
    @DisplayName("Should test equals and hashCode")
    void testEqualsAndHashCode() {
        ReviewComment comment1 = ReviewComment.builder()
                .id(1L)
                .review(mockReview)
                .githubCommentId(999L)
                .filePath("src/main/java/TestFile.java")
                .line(42)
                .content("This is a test comment")
                .createdAt(testTime)
                .build();

        ReviewComment comment2 = ReviewComment.builder()
                .id(1L)
                .review(mockReview)
                .githubCommentId(999L)
                .filePath("src/main/java/TestFile.java")
                .line(42)
                .content("This is a test comment")
                .createdAt(testTime)
                .build();

        ReviewComment comment3 = ReviewComment.builder()
                .id(2L)
                .review(mockReview)
                .githubCommentId(888L)
                .filePath("src/test/java/OtherFile.java")
                .line(100)
                .content("Different comment")
                .createdAt(testTime.plusMinutes(10))
                .build();

        // Test equals
        assertEquals(comment1, comment2);
        assertNotEquals(comment1, comment3);
        assertNotEquals(comment1, null);
        assertNotEquals(comment1, new Object());

        // Test hashCode
        assertEquals(comment1.hashCode(), comment2.hashCode());
        assertNotEquals(comment1.hashCode(), comment3.hashCode());
    }

    @Test
    @DisplayName("Should test toString method")
    void testToString() {
        String toString = reviewComment.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("ReviewComment"));
        assertTrue(toString.contains("githubCommentId=999"));
        assertTrue(toString.contains("filePath=src/main/java/TestFile.java"));
        assertTrue(toString.contains("line=42"));
        assertTrue(toString.contains("content=This is a test comment"));
    }

    @Test
    @DisplayName("Should handle null GitHub comment ID")
    void testNullGithubCommentId() {
        reviewComment.setGithubCommentId(null);
        assertNull(reviewComment.getGithubCommentId());
    }

    @Test
    @DisplayName("Should handle long file paths")
    void testLongFilePath() {
        String longPath = "src/main/java/com/example/very/long/package/structure/with/many/nested/directories/VeryLongClassNameThatExceedsNormalLengthButIsStillValid.java";
        reviewComment.setFilePath(longPath);
        assertEquals(longPath, reviewComment.getFilePath());
    }

    @Test
    @DisplayName("Should handle large line numbers")
    void testLargeLineNumber() {
        reviewComment.setLine(999999);
        assertEquals(999999, reviewComment.getLine());
    }

    @Test
    @DisplayName("Should handle multiline content")
    void testMultilineContent() {
        String multilineContent = "This is a comment\nthat spans multiple lines\nand contains line breaks";
        reviewComment.setContent(multilineContent);
        assertEquals(multilineContent, reviewComment.getContent());
    }

    @Test
    @DisplayName("Should handle empty content")
    void testEmptyContent() {
        reviewComment.setContent("");
        assertEquals("", reviewComment.getContent());
    }

    @Test
    @DisplayName("Should handle content with special characters")
    void testSpecialCharactersContent() {
        String specialContent = "Comment with special chars: @#$%^&*()[]{}|;':\",./<>?`~";
        reviewComment.setContent(specialContent);
        assertEquals(specialContent, reviewComment.getContent());
    }

    @Test
    @DisplayName("Should handle different file extensions")
    void testDifferentFileExtensions() {
        String[] filePaths = {
            "src/main/java/Test.java",
            "src/main/resources/application.yml",
            "src/test/java/TestClass.java",
            "pom.xml",
            "README.md",
            "docker-compose.yml",
            "src/main/resources/static/css/style.css",
            "src/main/resources/static/js/script.js"
        };
        
        for (String filePath : filePaths) {
            reviewComment.setFilePath(filePath);
            assertEquals(filePath, reviewComment.getFilePath());
        }
    }

    @Test
    @DisplayName("Should create comment for different line numbers")
    void testVariousLineNumbers() {
        int[] lineNumbers = {1, 10, 100, 1000, 5000};
        
        for (int lineNumber : lineNumbers) {
            reviewComment.setLine(lineNumber);
            assertEquals(lineNumber, reviewComment.getLine());
        }
    }

    @Test
    @DisplayName("Should handle very long content")
    void testVeryLongContent() {
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longContent.append("This is a very long comment that keeps repeating. ");
        }
        
        reviewComment.setContent(longContent.toString());
        assertEquals(longContent.toString(), reviewComment.getContent());
    }

    @Test
    @DisplayName("Should create comment without GitHub comment ID")
    void testCommentWithoutGithubId() {
        ReviewComment comment = ReviewComment.builder()
                .review(mockReview)
                .filePath("src/main/java/TestFile.java")
                .line(42)
                .content("Comment without GitHub ID")
                .build();

        assertNull(comment.getGithubCommentId());
        assertEquals("Comment without GitHub ID", comment.getContent());
    }

    @Test
    @DisplayName("Should handle UTF-8 content")
    void testUTF8Content() {
        String utf8Content = "Comment with UTF-8 characters: 🚀 📝 ✅ 🔥 💯 한국어 日本語 العربية";
        reviewComment.setContent(utf8Content);
        assertEquals(utf8Content, reviewComment.getContent());
    }
}