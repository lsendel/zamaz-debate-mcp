package com.zamaz.mcp.common.testing;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

/**
 * Test profiles and configurations for different testing scenarios.
 */
public class TestProfiles {
    
    /**
     * Profile for unit tests - minimal Spring context.
     */
    public static final String UNIT = "unit-test";
    
    /**
     * Profile for integration tests - full Spring context with real databases.
     */
    public static final String INTEGRATION = "integration-test";
    
    /**
     * Profile for contract tests - API contract validation.
     */
    public static final String CONTRACT = "contract-test";
    
    /**
     * Profile for performance tests - optimized for performance testing.
     */
    public static final String PERFORMANCE = "performance-test";
    
    /**
     * Profile for security tests - enhanced security configurations.
     */
    public static final String SECURITY = "security-test";
    
    /**
     * Profile for chaos tests - failure injection enabled.
     */
    public static final String CHAOS = "chaos-test";
    
    /**
     * Profile that uses in-memory databases and mocks all external services.
     */
    public static final String IN_MEMORY = "in-memory-test";
    
    /**
     * Profile for end-to-end tests - full stack with external dependencies.
     */
    public static final String E2E = "e2e-test";

    /**
     * Base test configuration that provides common test beans.
     */
    @TestConfiguration
    public static class BaseTestConfig {
        
        @Bean
        @Profile({UNIT, IN_MEMORY})
        @ConditionalOnMissingBean
        public MockFactory mockFactory() {
            return new MockFactory();
        }
        
        @Bean
        @Profile({UNIT, INTEGRATION, IN_MEMORY})
        @ConditionalOnMissingBean
        public TestDataManager testDataManager() {
            return new TestDataManager();
        }
        
        @Bean
        @Profile({INTEGRATION, E2E})
        @ConditionalOnMissingBean
        public TestContainersManager testContainersManager() {
            return new TestContainersManager();
        }
    }

    /**
     * Manager for test data lifecycle.
     */
    public static class TestDataManager {
        
        public void setupTestData() {
            // Override in specific test configurations
        }
        
        public void cleanupTestData() {
            // Override in specific test configurations
        }
        
        public void resetTestData() {
            cleanupTestData();
            setupTestData();
        }
    }

    /**
     * Manager for TestContainers lifecycle.
     */
    public static class TestContainersManager {
        
        public void startContainers() {
            // Override in specific test configurations
        }
        
        public void stopContainers() {
            // Override in specific test configurations
        }
        
        public boolean areContainersRunning() {
            // Override in specific test configurations
            return false;
        }
    }

    /**
     * Utility methods for working with test profiles.
     */
    public static class ProfileUtils {
        
        /**
         * Checks if the current test is running with a specific profile.
         */
        public static boolean isProfileActive(String profile) {
            String activeProfiles = System.getProperty("spring.profiles.active", "");
            return activeProfiles.contains(profile);
        }
        
        /**
         * Sets the active profile for the current test.
         */
        public static void setActiveProfile(String profile) {
            System.setProperty("spring.profiles.active", profile);
        }
        
        /**
         * Adds a profile to the currently active profiles.
         */
        public static void addActiveProfile(String profile) {
            String current = System.getProperty("spring.profiles.active", "");
            if (!current.isEmpty()) {
                current += ",";
            }
            System.setProperty("spring.profiles.active", current + profile);
        }
        
        /**
         * Clears all active profiles.
         */
        public static void clearActiveProfiles() {
            System.clearProperty("spring.profiles.active");
        }
    }
}