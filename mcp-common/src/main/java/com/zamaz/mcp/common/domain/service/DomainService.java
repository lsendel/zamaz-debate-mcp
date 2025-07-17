package com.zamaz.mcp.common.domain.service;

/**
 * Marker interface for domain services.
 * Domain services encapsulate domain logic that doesn't naturally fit within a single entity or value object.
 * They are stateless and represent domain operations.
 * This is a pure domain interface with no framework dependencies.
 */
public interface DomainService {
    // Marker interface for domain services
    // Domain services should:
    // - Be stateless
    // - Contain domain logic that spans multiple entities
    // - Not depend on any infrastructure or framework
}