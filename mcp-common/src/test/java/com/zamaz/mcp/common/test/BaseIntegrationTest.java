package com.zamaz.mcp.common.test;

import com.zamaz.mcp.common.domain.model.valueobject.TenantId;
import com.zamaz.mcp.common.domain.model.valueobject.UserId;
import com.zamaz.mcp.common.test.fixtures.TenantIdBuilder;
import com.zamaz.mcp.common.test.fixtures.UserIdBuilder;

/**
 * Base class for integration tests providing common test data and utilities.
 */
public abstract class BaseIntegrationTest extends BaseUnitTest {
    
    // Common test data
    protected static final TenantId TEST_TENANT = TenantIdBuilder.defaultTestTenant();
    protected static final UserId TEST_USER = UserIdBuilder.testUserId().build();
    
    /**
     * Sets up test data before each test.
     * Subclasses should override and call super.setUp().
     */
    protected void setUp() {
        // Clear any test data
        clearTestData();
        
        // Set up common test context
        setUpTestContext();
    }
    
    /**
     * Tears down test data after each test.
     * Subclasses should override and call super.tearDown().
     */
    protected void tearDown() {
        clearTestData();
    }
    
    /**
     * Clears all test data.
     * Subclasses should implement this to clean their specific data.
     */
    protected abstract void clearTestData();
    
    /**
     * Sets up the test context (e.g., authentication, tenant context).
     * Can be overridden by subclasses for specific needs.
     */
    protected void setUpTestContext() {
        // Default implementation - can be overridden
    }
    
    /**
     * Creates a test transaction scope for testing transactional behavior.
     * 
     * @param runnable the code to run in transaction
     */
    protected void inTransaction(Runnable runnable) {
        // This would be implemented with actual transaction management
        // For now, just run the code
        runnable.run();
    }
    
    /**
     * Waits for async operations to complete.
     * Useful for testing event-driven systems.
     * 
     * @param timeoutMillis maximum time to wait
     */
    protected void waitForAsync(long timeoutMillis) {
        try {
            Thread.sleep(100); // Simple implementation - replace with proper async handling
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Test interrupted", e);
        }
    }
}