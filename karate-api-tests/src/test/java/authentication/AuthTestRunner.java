package authentication;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Authentication Test Runner
 * 
 * This class runs all authentication-related tests including:
 * - User login/logout
 * - User registration
 * - Token refresh
 * - Authentication security tests
 */
public class AuthTestRunner {
    
    @Test
    void testAuthentication() {
        Results results = Runner.path("classpath:authentication")
                .tags("~@ignore")
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
    
    @Test
    void testAuthenticationSecurity() {
        Results results = Runner.path("classpath:authentication")
                .tags("@security")
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}