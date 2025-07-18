package llm;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * LLM Service Test Runner
 * 
 * This class runs all LLM-related tests including:
 * - Completion generation (synchronous and streaming)
 * - Provider management and health checks
 * - Model listing and capabilities
 * - Token estimation and cost calculation
 * - Performance and rate limiting tests
 */
public class LlmTestRunner {
    
    @Test
    void testLlmCompletion() {
        Results results = Runner.path("classpath:llm")
                .tags("@smoke")
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
    
    @Test
    void testLlmRegression() {
        Results results = Runner.path("classpath:llm")
                .tags("@regression")
                .parallel(2);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
    
    @Test
    void testLlmSecurity() {
        Results results = Runner.path("classpath:llm")
                .tags("@security")
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
    
    @Test
    void testLlmPerformance() {
        Results results = Runner.path("classpath:llm")
                .tags("@performance")
                .parallel(4);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
    
    @Test
    void testLlmStreaming() {
        Results results = Runner.path("classpath:llm")
                .tags("@streaming")
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}