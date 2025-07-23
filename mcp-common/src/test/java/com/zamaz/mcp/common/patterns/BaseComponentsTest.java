package com.zamaz.mcp.common.patterns;

import com.zamaz.mcp.common.exception.McpBusinessException;
import com.zamaz.mcp.common.infrastructure.logging.StructuredLoggerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.JpaRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for base components created in task 1.
 */
@ExtendWith(MockitoExtension.class)
class BaseComponentsTest {

    @Mock
    private JpaRepository<TestEntity, String> mockRepository;

    @Mock
    private StructuredLoggerFactory mockLoggerFactory;

    @Test
    void testMcpBusinessExceptionCreation() {
        // Test basic exception creation
        McpBusinessException exception = new McpBusinessException("Test message", "TEST_ERROR");

        assertEquals("Test message", exception.getMessage());
        assertEquals("TEST_ERROR", exception.getErrorCode());
        assertNotNull(exception.getErrorDetails());
        assertTrue(exception.getErrorDetails().isEmpty());
    }

    @Test
    void testMcpBusinessExceptionWithDetails() {
        // Test exception with details
        McpBusinessException exception = new McpBusinessException("Test message", "TEST_ERROR")
                .withDetail("key1", "value1")
                .withDetail("key2", "value2");

        assertEquals("Test message", exception.getMessage());
        assertEquals("TEST_ERROR", exception.getErrorCode());
        assertEquals(2, exception.getErrorDetails().size());
        assertEquals("value1", exception.getErrorDetails().get("key1"));
        assertEquals("value2", exception.getErrorDetails().get("key2"));
    }

    @Test
    void testMcpBusinessExceptionFactoryMethods() {
        // Test organization not found
        McpBusinessException orgException = McpBusinessException.organizationNotFound("org-123");
        assertEquals("Organization not found with id: org-123", orgException.getMessage());
        assertEquals("org-123", orgException.getErrorDetails().get("organizationId"));

        // Test validation failed
        McpBusinessException validationException = McpBusinessException.validationFailed("email", "Invalid format");
        assertEquals("Validation failed for field 'email': Invalid format", validationException.getMessage());
        assertEquals("email", validationException.getErrorDetails().get("field"));
        assertEquals("Invalid format", validationException.getErrorDetails().get("reason"));

        // Test rate limit exceeded
        McpBusinessException rateLimitException = McpBusinessException.rateLimitExceeded("API", "100/hour");
        assertEquals("Rate limit exceeded for API. Limit: 100/hour", rateLimitException.getMessage());
        assertEquals("API", rateLimitException.getErrorDetails().get("resource"));
        assertEquals("100/hour", rateLimitException.getErrorDetails().get("limit"));
    }

    @Test
    void testBaseServiceInstantiation() {
        // Test that BaseService can be instantiated (abstract class test)
        TestService service = new TestService(mockRepository, mockLoggerFactory);
        assertNotNull(service);
    }

    // Test entity for testing purposes
    private static class TestEntity {
        private String id;
        private String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    // Test service implementation for testing purposes
    private static class TestService extends BaseService<TestEntity, String> {

        public TestService(JpaRepository<TestEntity, String> repository, StructuredLoggerFactory loggerFactory) {
            super(repository, loggerFactory);
        }

        @Override
        protected Object getEntityId(TestEntity entity) {
            return entity.getId();
        }

        @Override
        protected RuntimeException createNotFoundException(String id) {
            return McpBusinessException.notFound("TestEntity", id);
        }
    }
}