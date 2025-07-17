package com.zamaz.mcp.common.architecture.adapter.persistence;

/**
 * Marker interface for persistence adapters (outbound adapters).
 * Persistence adapters implement repository interfaces and handle database operations.
 * This is part of the adapter layer in hexagonal architecture.
 * 
 * Note: Actual implementations will use JPA/Spring Data,
 * but they should implement this interface to mark them as adapters.
 */
public interface PersistenceAdapter {
    // Marker interface for persistence adapters
    // Persistence adapters should:
    // - Implement repository interfaces from the application layer
    // - Handle database-specific operations
    // - Map between domain entities and persistence entities
    // - Handle transactions at the infrastructure level
}