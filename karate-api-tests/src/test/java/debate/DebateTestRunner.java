package debate;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Debate Controller Test Runner
 * 
 * This class runs all debate-related tests including:
 * - Debate lifecycle management (create, start, complete)
 * - Participant management
 * - Response submission and validation
 * - Real-time WebSocket communication
 * - Debate analysis and statistics
 * - Performance and load testing
 */
public class DebateTestRunner {
    
    @Test
    void testDebateLifecycle() {
        Results results = Runner.path("classpath:debate")
                .tags("@smoke")
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
    
    @Test
    void testDebateRegression() {
        Results results = Runner.path("classpath:debate")
                .tags("@regression")
                .parallel(2);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
    
    @Test
    void testDebateSecurity() {
        Results results = Runner.path("classpath:debate")
                .tags("@security")
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
    
    @Test
    void testDebatePerformance() {
        Results results = Runner.path("classpath:debate")
                .tags("@performance")
                .parallel(4);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
    
    @Test
    void testDebateWebSocket() {
        Results results = Runner.path("classpath:debate")
                .tags("@websocket")
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
    
    @Test
    void testDebateIntegration() {
        Results results = Runner.path("classpath:debate")
                .tags("@integration")
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}