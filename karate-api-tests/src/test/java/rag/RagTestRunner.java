package rag;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * RAG Service Test Runner
 * 
 * This class runs all RAG-related tests including:
 * - Document upload and management
 * - Knowledge base operations
 * - Document search and retrieval
 * - Embedding generation and processing
 * - Performance and bulk operations
 */
public class RagTestRunner {
    
    @Test
    void testRagDocumentManagement() {
        Results results = Runner.path("classpath:rag")
                .tags("@smoke")
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
    
    @Test
    void testRagRegression() {
        Results results = Runner.path("classpath:rag")
                .tags("@regression")
                .parallel(2);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
    
    @Test
    void testRagSecurity() {
        Results results = Runner.path("classpath:rag")
                .tags("@security")
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
    
    @Test
    void testRagPerformance() {
        Results results = Runner.path("classpath:rag")
                .tags("@performance")
                .parallel(4);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
    
    @Test
    void testRagSearch() {
        Results results = Runner.path("classpath:rag")
                .tags("@search")
                .parallel(2);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}