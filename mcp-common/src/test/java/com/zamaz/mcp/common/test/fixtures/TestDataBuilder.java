package com.zamaz.mcp.common.test.fixtures;

/**
 * Base interface for test data builders.
 * Implements the builder pattern for creating test fixtures.
 * 
 * @param <T> the type being built
 */
public interface TestDataBuilder<T> {
    
    /**
     * Builds the test object with the configured values.
     * 
     * @return the built object
     */
    T build();
    
    /**
     * Creates a builder with default valid values.
     * Each implementation should provide sensible defaults.
     * 
     * @return a builder with defaults
     */
    static <T> TestDataBuilder<T> withDefaults() {
        throw new UnsupportedOperationException(
            "Implementations must provide their own withDefaults method"
        );
    }
}