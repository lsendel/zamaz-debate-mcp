package com.zamaz.mcp.common.architecture.adapter.external;

/**
 * Marker interface for external service adapters (outbound adapters).
 * External service adapters implement interfaces for calling external APIs or services.
 * This is part of the adapter layer in hexagonal architecture.
 */
public interface ExternalServiceAdapter {
    // Marker interface for external service adapters
    // External service adapters should:
    // - Implement service interfaces from the application layer
    // - Handle external API calls
    // - Translate between external formats and domain objects
    // - Handle resilience patterns (circuit breakers, retries)
}