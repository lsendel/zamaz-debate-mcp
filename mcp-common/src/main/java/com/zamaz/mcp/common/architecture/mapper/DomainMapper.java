package com.zamaz.mcp.common.architecture.mapper;

/**
 * Base interface for mapping between domain objects and other representations.
 * This is used by adapters to convert between domain models and DTOs/entities.
 * This is part of the adapter layer in hexagonal architecture.
 * 
 * @param <D> Domain object type
 * @param <E> External representation type (DTO, Entity, etc.)
 */
public interface DomainMapper<D, E> {
    
    /**
     * Converts from external representation to domain object.
     * 
     * @param external the external representation
     * @return the domain object
     */
    D toDomain(E external);
    
    /**
     * Converts from domain object to external representation.
     * 
     * @param domain the domain object
     * @return the external representation
     */
    E fromDomain(D domain);
}