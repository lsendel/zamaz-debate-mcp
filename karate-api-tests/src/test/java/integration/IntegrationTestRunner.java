package integration;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Integration Test Runner
 * 
 * This class runs all integration tests that span multiple services:
 * - End-to-end workflow testing
 * - Cross-service communication validation
 * - Multi-tenant isolation testing
 * - Performance testing under load
 * - Security validation across services
 */
public class IntegrationTestRunner {
    
    @Test
    void testEndToEndFlows() {
        Results results = Runner.path("classpath:integration")
                .tags("@integration", "@smoke")
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
    
    @Test
    void testIntegrationRegression() {
        Results results = Runner.path("classpath:integration")
                .tags("@integration", "@regression")
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
    
    @Test
    void testCrossServiceSecurity() {
        Results results = Runner.path("classpath:integration")
                .tags("@integration", "@security")
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
    
    @Test
    void testIntegrationPerformance() {
        Results results = Runner.path("classpath:integration")
                .tags("@integration", "@performance")
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
    
    @Test
    void testAllIntegrationScenarios() {
        Results results = Runner.path("classpath:integration")
                .tags("@integration")
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}