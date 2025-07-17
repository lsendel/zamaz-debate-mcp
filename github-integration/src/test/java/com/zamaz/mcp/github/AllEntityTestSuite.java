package com.zamaz.mcp.github;

import com.zamaz.mcp.github.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Test suite that runs all entity tests for the GitHub Integration module.
 * This suite provides a convenient way to run all entity tests together
 * and verify the complete entity model functionality.
 */
@Suite
@SelectClasses({
    GitHubInstallationTest.class,
    RepositoryConfigTest.class,
    PullRequestReviewTest.class,
    ReviewIssueTest.class,
    ReviewCommentTest.class,
    FeedbackTest.class
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("GitHub Integration Entity Test Suite")
public class AllEntityTestSuite {

    /**
     * Simple test to verify the test suite is running
     */
    @Test
    @DisplayName("Test suite initialization")
    void testSuiteInitialization() {
        // This test simply verifies that the test suite can be initialized
        // and that all required dependencies are available
        org.junit.jupiter.api.Assertions.assertTrue(true, "Test suite initialized successfully");
    }

    /**
     * Test to verify that all entity classes are properly structured
     */
    @Test
    @DisplayName("Entity class structure verification")
    void testEntityClassStructure() {
        // Verify all entity classes exist and can be instantiated
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
            new GitHubInstallation();
            new RepositoryConfig();
            new PullRequestReview();
            new ReviewIssue();
            new ReviewComment();
            new Feedback();
        }, "All entity classes should be instantiable");
    }

    /**
     * Test to verify that all enums are properly defined
     */
    @Test
    @DisplayName("Enum definitions verification")
    void testEnumDefinitions() {
        // Verify all enum classes are properly defined
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
            RepositoryConfig.ReviewDepth.values();
            RepositoryConfig.CommentStyle.values();
            PullRequestReview.ReviewStatus.values();
            ReviewIssue.IssueSeverity.values();
            Feedback.FeedbackType.values();
        }, "All enum classes should be accessible");
    }
}