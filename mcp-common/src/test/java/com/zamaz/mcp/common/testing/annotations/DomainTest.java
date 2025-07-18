package com.zamaz.mcp.common.testing.annotations;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.*;

/**
 * Annotation for domain logic tests that don't require Spring context.
 * Automatically configures Mockito for mocking support.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Tag("domain")
@Tag("unit")
@ExtendWith(MockitoExtension.class)
public @interface DomainTest {
}