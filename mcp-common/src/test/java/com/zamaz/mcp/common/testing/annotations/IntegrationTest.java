package com.zamaz.mcp.common.testing.annotations;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;

/**
 * Annotation for integration tests that require full Spring context.
 * Automatically configures test profile and transaction support.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
@Transactional
public @interface IntegrationTest {
    /**
     * Whether to use a random port for the web environment.
     */
    boolean randomPort() default false;
    
    /**
     * Additional test properties.
     */
    String[] properties() default {};
}