package com.zamaz.mcp.common.application.port.outbound;

import java.util.Optional;

/**
 * Base interface for all repositories (outbound ports).
 * Repositories provide persistence operations for aggregates.
 * This is part of the application layer in hexagonal architecture.
 * 
 * @param <T> The aggregate type
 * @param <ID> The aggregate identifier type
 */
public interface Repository<T, ID> {
    
    /**
     * Saves an aggregate.
     * 
     * @param aggregate the aggregate to save
     * @return the saved aggregate
     */
    T save(T aggregate);
    
    /**
     * Finds an aggregate by its identifier.
     * 
     * @param id the aggregate identifier
     * @return an Optional containing the aggregate if found
     */
    Optional<T> findById(ID id);
    
    /**
     * Checks if an aggregate exists with the given identifier.
     * 
     * @param id the aggregate identifier
     * @return true if exists, false otherwise
     */
    boolean existsById(ID id);
    
    /**
     * Deletes an aggregate by its identifier.
     * 
     * @param id the aggregate identifier
     */
    void deleteById(ID id);
}