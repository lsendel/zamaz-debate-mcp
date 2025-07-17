package com.zamaz.mcp.github.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RepositoryConfig Entity Tests")
class RepositoryConfigTest {

    private RepositoryConfig repositoryConfig;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.now();
        repositoryConfig = RepositoryConfig.builder()
                .id(1L)
                .repositoryId(12345L)
                .repositoryName("test-repo")
                .ownerName("testowner")
                .reviewDepth(RepositoryConfig.ReviewDepth.STANDARD)
                .autoFixEnabled(true)
                .commentStyle(RepositoryConfig.CommentStyle.EDUCATIONAL)
                .createdAt(testTime)
                .updatedAt(testTime)
                .build();
    }

    @Test
    @DisplayName("Should create RepositoryConfig with builder pattern")
    void testBuilderPattern() {
        RepositoryConfig config = RepositoryConfig.builder()
                .repositoryId(99999L)
                .repositoryName("builder-repo")
                .ownerName("builderowner")
                .reviewDepth(RepositoryConfig.ReviewDepth.THOROUGH)
                .autoFixEnabled(false)
                .commentStyle(RepositoryConfig.CommentStyle.CONCISE)
                .build();

        assertNotNull(config);
        assertEquals(99999L, config.getRepositoryId());
        assertEquals("builder-repo", config.getRepositoryName());
        assertEquals("builderowner", config.getOwnerName());
        assertEquals(RepositoryConfig.ReviewDepth.THOROUGH, config.getReviewDepth());
        assertFalse(config.isAutoFixEnabled());
        assertEquals(RepositoryConfig.CommentStyle.CONCISE, config.getCommentStyle());
    }

    @Test
    @DisplayName("Should create RepositoryConfig with no-args constructor")
    void testNoArgsConstructor() {
        RepositoryConfig config = new RepositoryConfig();
        assertNotNull(config);
        assertNull(config.getId());
        assertNull(config.getRepositoryId());
        assertNull(config.getRepositoryName());
        assertNull(config.getOwnerName());
        assertNull(config.getReviewDepth());
        assertFalse(config.isAutoFixEnabled());
        assertNull(config.getCommentStyle());
        assertNull(config.getCreatedAt());
        assertNull(config.getUpdatedAt());
    }

    @Test
    @DisplayName("Should create RepositoryConfig with all-args constructor")
    void testAllArgsConstructor() {
        RepositoryConfig config = new RepositoryConfig(
                1L, 12345L, "test-repo", "testowner",
                RepositoryConfig.ReviewDepth.STANDARD, true,
                RepositoryConfig.CommentStyle.EDUCATIONAL, testTime, testTime
        );

        assertNotNull(config);
        assertEquals(1L, config.getId());
        assertEquals(12345L, config.getRepositoryId());
        assertEquals("test-repo", config.getRepositoryName());
        assertEquals("testowner", config.getOwnerName());
        assertEquals(RepositoryConfig.ReviewDepth.STANDARD, config.getReviewDepth());
        assertTrue(config.isAutoFixEnabled());
        assertEquals(RepositoryConfig.CommentStyle.EDUCATIONAL, config.getCommentStyle());
        assertEquals(testTime, config.getCreatedAt());
        assertEquals(testTime, config.getUpdatedAt());
    }

    @Test
    @DisplayName("Should get and set repository ID")
    void testRepositoryId() {
        assertEquals(12345L, repositoryConfig.getRepositoryId());
        
        repositoryConfig.setRepositoryId(54321L);
        assertEquals(54321L, repositoryConfig.getRepositoryId());
    }

    @Test
    @DisplayName("Should get and set repository name")
    void testRepositoryName() {
        assertEquals("test-repo", repositoryConfig.getRepositoryName());
        
        repositoryConfig.setRepositoryName("new-repo");
        assertEquals("new-repo", repositoryConfig.getRepositoryName());
    }

    @Test
    @DisplayName("Should get and set owner name")
    void testOwnerName() {
        assertEquals("testowner", repositoryConfig.getOwnerName());
        
        repositoryConfig.setOwnerName("newowner");
        assertEquals("newowner", repositoryConfig.getOwnerName());
    }

    @Test
    @DisplayName("Should get and set review depth")
    void testReviewDepth() {
        assertEquals(RepositoryConfig.ReviewDepth.STANDARD, repositoryConfig.getReviewDepth());
        
        repositoryConfig.setReviewDepth(RepositoryConfig.ReviewDepth.THOROUGH);
        assertEquals(RepositoryConfig.ReviewDepth.THOROUGH, repositoryConfig.getReviewDepth());
    }

    @Test
    @DisplayName("Should get and set auto fix enabled")
    void testAutoFixEnabled() {
        assertTrue(repositoryConfig.isAutoFixEnabled());
        
        repositoryConfig.setAutoFixEnabled(false);
        assertFalse(repositoryConfig.isAutoFixEnabled());
    }

    @Test
    @DisplayName("Should get and set comment style")
    void testCommentStyle() {
        assertEquals(RepositoryConfig.CommentStyle.EDUCATIONAL, repositoryConfig.getCommentStyle());
        
        repositoryConfig.setCommentStyle(RepositoryConfig.CommentStyle.DETAILED);
        assertEquals(RepositoryConfig.CommentStyle.DETAILED, repositoryConfig.getCommentStyle());
    }

    @Test
    @DisplayName("Should get and set created at timestamp")
    void testCreatedAt() {
        assertEquals(testTime, repositoryConfig.getCreatedAt());
        
        LocalDateTime newCreatedAt = testTime.minusHours(1);
        repositoryConfig.setCreatedAt(newCreatedAt);
        assertEquals(newCreatedAt, repositoryConfig.getCreatedAt());
    }

    @Test
    @DisplayName("Should get and set updated at timestamp")
    void testUpdatedAt() {
        assertEquals(testTime, repositoryConfig.getUpdatedAt());
        
        LocalDateTime newUpdatedAt = testTime.plusMinutes(30);
        repositoryConfig.setUpdatedAt(newUpdatedAt);
        assertEquals(newUpdatedAt, repositoryConfig.getUpdatedAt());
    }

    @Test
    @DisplayName("Should test ReviewDepth enum values")
    void testReviewDepthEnum() {
        assertEquals(3, RepositoryConfig.ReviewDepth.values().length);
        
        assertTrue(containsValue(RepositoryConfig.ReviewDepth.values(), RepositoryConfig.ReviewDepth.BASIC));
        assertTrue(containsValue(RepositoryConfig.ReviewDepth.values(), RepositoryConfig.ReviewDepth.STANDARD));
        assertTrue(containsValue(RepositoryConfig.ReviewDepth.values(), RepositoryConfig.ReviewDepth.THOROUGH));
        
        assertEquals("BASIC", RepositoryConfig.ReviewDepth.BASIC.toString());
        assertEquals("STANDARD", RepositoryConfig.ReviewDepth.STANDARD.toString());
        assertEquals("THOROUGH", RepositoryConfig.ReviewDepth.THOROUGH.toString());
    }

    @Test
    @DisplayName("Should test CommentStyle enum values")
    void testCommentStyleEnum() {
        assertEquals(3, RepositoryConfig.CommentStyle.values().length);
        
        assertTrue(containsValue(RepositoryConfig.CommentStyle.values(), RepositoryConfig.CommentStyle.CONCISE));
        assertTrue(containsValue(RepositoryConfig.CommentStyle.values(), RepositoryConfig.CommentStyle.EDUCATIONAL));
        assertTrue(containsValue(RepositoryConfig.CommentStyle.values(), RepositoryConfig.CommentStyle.DETAILED));
        
        assertEquals("CONCISE", RepositoryConfig.CommentStyle.CONCISE.toString());
        assertEquals("EDUCATIONAL", RepositoryConfig.CommentStyle.EDUCATIONAL.toString());
        assertEquals("DETAILED", RepositoryConfig.CommentStyle.DETAILED.toString());
    }

    @Test
    @DisplayName("Should test equals and hashCode")
    void testEqualsAndHashCode() {
        RepositoryConfig config1 = RepositoryConfig.builder()
                .id(1L)
                .repositoryId(12345L)
                .repositoryName("test-repo")
                .ownerName("testowner")
                .reviewDepth(RepositoryConfig.ReviewDepth.STANDARD)
                .autoFixEnabled(true)
                .commentStyle(RepositoryConfig.CommentStyle.EDUCATIONAL)
                .build();

        RepositoryConfig config2 = RepositoryConfig.builder()
                .id(1L)
                .repositoryId(12345L)
                .repositoryName("test-repo")
                .ownerName("testowner")
                .reviewDepth(RepositoryConfig.ReviewDepth.STANDARD)
                .autoFixEnabled(true)
                .commentStyle(RepositoryConfig.CommentStyle.EDUCATIONAL)
                .build();

        RepositoryConfig config3 = RepositoryConfig.builder()
                .id(2L)
                .repositoryId(54321L)
                .repositoryName("other-repo")
                .ownerName("otherowner")
                .reviewDepth(RepositoryConfig.ReviewDepth.BASIC)
                .autoFixEnabled(false)
                .commentStyle(RepositoryConfig.CommentStyle.CONCISE)
                .build();

        // Test equals
        assertEquals(config1, config2);
        assertNotEquals(config1, config3);
        assertNotEquals(config1, null);
        assertNotEquals(config1, new Object());

        // Test hashCode
        assertEquals(config1.hashCode(), config2.hashCode());
        assertNotEquals(config1.hashCode(), config3.hashCode());
    }

    @Test
    @DisplayName("Should test toString method")
    void testToString() {
        String toString = repositoryConfig.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("RepositoryConfig"));
        assertTrue(toString.contains("repositoryId=12345"));
        assertTrue(toString.contains("repositoryName=test-repo"));
        assertTrue(toString.contains("ownerName=testowner"));
        assertTrue(toString.contains("reviewDepth=STANDARD"));
        assertTrue(toString.contains("autoFixEnabled=true"));
        assertTrue(toString.contains("commentStyle=EDUCATIONAL"));
    }

    @Test
    @DisplayName("Should create config with all review depth options")
    void testAllReviewDepthOptions() {
        RepositoryConfig basicConfig = RepositoryConfig.builder()
                .repositoryId(1L)
                .repositoryName("basic-repo")
                .ownerName("owner")
                .reviewDepth(RepositoryConfig.ReviewDepth.BASIC)
                .commentStyle(RepositoryConfig.CommentStyle.CONCISE)
                .build();

        RepositoryConfig standardConfig = RepositoryConfig.builder()
                .repositoryId(2L)
                .repositoryName("standard-repo")
                .ownerName("owner")
                .reviewDepth(RepositoryConfig.ReviewDepth.STANDARD)
                .commentStyle(RepositoryConfig.CommentStyle.EDUCATIONAL)
                .build();

        RepositoryConfig thoroughConfig = RepositoryConfig.builder()
                .repositoryId(3L)
                .repositoryName("thorough-repo")
                .ownerName("owner")
                .reviewDepth(RepositoryConfig.ReviewDepth.THOROUGH)
                .commentStyle(RepositoryConfig.CommentStyle.DETAILED)
                .build();

        assertEquals(RepositoryConfig.ReviewDepth.BASIC, basicConfig.getReviewDepth());
        assertEquals(RepositoryConfig.ReviewDepth.STANDARD, standardConfig.getReviewDepth());
        assertEquals(RepositoryConfig.ReviewDepth.THOROUGH, thoroughConfig.getReviewDepth());
    }

    @Test
    @DisplayName("Should create config with all comment style options")
    void testAllCommentStyleOptions() {
        RepositoryConfig conciseConfig = RepositoryConfig.builder()
                .repositoryId(1L)
                .repositoryName("concise-repo")
                .ownerName("owner")
                .reviewDepth(RepositoryConfig.ReviewDepth.BASIC)
                .commentStyle(RepositoryConfig.CommentStyle.CONCISE)
                .build();

        RepositoryConfig educationalConfig = RepositoryConfig.builder()
                .repositoryId(2L)
                .repositoryName("educational-repo")
                .ownerName("owner")
                .reviewDepth(RepositoryConfig.ReviewDepth.STANDARD)
                .commentStyle(RepositoryConfig.CommentStyle.EDUCATIONAL)
                .build();

        RepositoryConfig detailedConfig = RepositoryConfig.builder()
                .repositoryId(3L)
                .repositoryName("detailed-repo")
                .ownerName("owner")
                .reviewDepth(RepositoryConfig.ReviewDepth.THOROUGH)
                .commentStyle(RepositoryConfig.CommentStyle.DETAILED)
                .build();

        assertEquals(RepositoryConfig.CommentStyle.CONCISE, conciseConfig.getCommentStyle());
        assertEquals(RepositoryConfig.CommentStyle.EDUCATIONAL, educationalConfig.getCommentStyle());
        assertEquals(RepositoryConfig.CommentStyle.DETAILED, detailedConfig.getCommentStyle());
    }

    @Test
    @DisplayName("Should create config with auto fix disabled")
    void testAutoFixDisabled() {
        RepositoryConfig config = RepositoryConfig.builder()
                .repositoryId(123L)
                .repositoryName("no-autofix-repo")
                .ownerName("owner")
                .reviewDepth(RepositoryConfig.ReviewDepth.STANDARD)
                .autoFixEnabled(false)
                .commentStyle(RepositoryConfig.CommentStyle.EDUCATIONAL)
                .build();

        assertFalse(config.isAutoFixEnabled());
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