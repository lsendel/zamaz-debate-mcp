package com.zamaz.mcp.common.application.query;

/**
 * Marker interface for queries in CQRS pattern.
 * Queries represent requests to read data without changing system state.
 * This is part of the application layer in hexagonal architecture.
 */
public interface Query {
    // Marker interface for queries
    // Queries should:
    // - Be immutable
    // - Not change system state
    // - Contain all parameters needed for the query
    // - Have descriptive names (e.g., GetOrganizationByIdQuery)
}