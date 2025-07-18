import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Main Karate Test Runner for Zamaz Debate MCP API Tests
 * 
 * This class serves as the entry point for running all Karate tests.
 * It provides different test execution strategies:
 * - Sequential execution for debugging
 * - Parallel execution for CI/CD
 * - Environment-specific configurations
 */
public class TestRunner {
    
    /**
     * Run all tests sequentially (for debugging and development)
     */
    @Test
    void testParallel() {
        Results results = Runner.path("classpath:")
                .tags("~@ignore")
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
    
    /**
     * Run tests in parallel (for CI/CD environments)
     */
    @Test
    void testParallelCI() {
        int threads = Integer.parseInt(System.getProperty("parallel.threads", "4"));
        Results results = Runner.path("classpath:")
                .tags("~@ignore", "~@slow")
                .parallel(threads);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
    
    /**
     * Run only smoke tests (quick validation)
     */
    @Test
    void testSmoke() {
        Results results = Runner.path("classpath:")
                .tags("@smoke")
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
    
    /**
     * Run regression tests (comprehensive validation)
     */
    @Test
    void testRegression() {
        Results results = Runner.path("classpath:")
                .tags("@regression")
                .parallel(2);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
    
    /**
     * Run security tests
     */
    @Test
    void testSecurity() {
        Results results = Runner.path("classpath:")
                .tags("@security")
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
    
    /**
     * Run integration tests
     */
    @Test
    void testIntegration() {
        Results results = Runner.path("classpath:")
                .tags("@integration")
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}