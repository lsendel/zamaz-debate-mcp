package com.zamaz.mcp.github.service;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Test suite for all GitHubApiClient tests
 */
@Suite
@SelectClasses({
    GitHubApiClientIntegrationTest.class,
    GitHubApiClientErrorHandlingTest.class,
    GitHubApiClientAdvancedTest.class,
    GitHubApiClientPerformanceTest.class,
    GitHubApiClientRealWorldTest.class
})
public class GitHubApiClientTestSuite {
}