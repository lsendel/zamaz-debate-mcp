package com.zamaz.mcp.common.testing.annotations;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;

import java.lang.annotation.*;

/**
 * Annotation for fast-running tests that should complete quickly.
 * Automatically sets a timeout and tags the test as "fast".
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Tag("fast")
@Timeout(5) // 5 seconds timeout
public @interface FastTest {
}