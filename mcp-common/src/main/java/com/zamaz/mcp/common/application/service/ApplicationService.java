package com.zamaz.mcp.common.application.service;

/**
 * Marker interface for application services.
 * Application services coordinate use cases and orchestrate domain operations.
 * They should not contain business logic but rather coordinate domain objects.
 * This is part of the application layer in hexagonal architecture.
 */
public interface ApplicationService {
    // Marker interface for application services
    // Application services should:
    // - Coordinate use cases
    // - Handle transactions
    // - Orchestrate domain services and repositories
    // - Not contain business logic (that belongs in domain)
}