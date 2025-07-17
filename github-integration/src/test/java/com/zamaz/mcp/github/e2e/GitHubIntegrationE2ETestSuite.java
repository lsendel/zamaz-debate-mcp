package com.zamaz.mcp.github.e2e;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Comprehensive End-to-End Test Suite for GitHub Integration
 * 
 * This suite runs all E2E tests in the correct order to validate:
 * 1. Complete workflow functionality
 * 2. Pull request review flows  
 * 3. Performance under load
 * 4. Error handling and recovery
 * 
 * Test execution order:
 * - GitHubIntegrationE2ETest: Basic functionality and workflows
 * - PullRequestReviewFlowE2ETest: Complex PR review scenarios
 * - PerformanceE2ETest: Load testing and performance validation
 * 
 * Prerequisites:
 * - Docker must be running (for TestContainers)
 * - Sufficient memory for concurrent test execution
 * - Network access for container image downloads
 */
@Suite
@SuiteDisplayName("GitHub Integration E2E Test Suite")
@SelectClasses({
    GitHubIntegrationE2ETest.class,
    PullRequestReviewFlowE2ETest.class,
    PerformanceE2ETest.class
})
public class GitHubIntegrationE2ETestSuite {
    // Test suite configuration class
    // Actual test execution is handled by the selected test classes
}