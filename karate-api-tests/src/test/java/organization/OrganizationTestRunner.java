package organization;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Organization Service Test Runner
 * 
 * This class runs all organization-related tests including:
 * - Organization CRUD operations
 * - User management within organizations
 * - Multi-tenant isolation
 * - Organization permissions and roles
 */
public class OrganizationTestRunner {
    
    @Test
    void testOrganizationCRUD() {
        Results results = Runner.path("classpath:organization")
                .tags("@smoke")
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
    
    @Test
    void testOrganizationRegression() {
        Results results = Runner.path("classpath:organization")
                .tags("@regression")
                .parallel(2);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
    
    @Test
    void testOrganizationSecurity() {
        Results results = Runner.path("classpath:organization")
                .tags("@security")
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
    
    @Test
    void testMultiTenantIsolation() {
        Results results = Runner.path("classpath:organization")
                .tags("@multitenant")
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}