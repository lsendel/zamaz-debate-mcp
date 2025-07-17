package com.zamaz.mcp.common.architecture.adapter.web;

/**
 * Marker interface for web adapters (inbound adapters).
 * Web adapters handle HTTP requests and translate them to use case calls.
 * This is part of the adapter layer in hexagonal architecture.
 * 
 * Note: Actual REST controllers will use Spring annotations,
 * but they should implement this interface to mark them as adapters.
 */
public interface WebAdapter {
    // Marker interface for web adapters
    // Web adapters should:
    // - Handle HTTP requests/responses
    // - Translate between DTOs and domain objects
    // - Call use cases
    // - Handle HTTP-specific concerns (status codes, headers, etc.)
}