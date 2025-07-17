package com.zamaz.mcp.common.domain.model;

/**
 * Base interface for all value objects in hexagonal architecture.
 * Value objects are immutable and are compared by their values rather than identity.
 * This is a pure domain interface with no framework dependencies.
 * 
 * Note: In Java 14+, value objects should be implemented as records
 * which automatically provide immutability, equals, hashCode, and toString.
 * 
 * Example:
 * <pre>
 * public record UserId(UUID value) implements ValueObject {
 *     public UserId {
 *         Objects.requireNonNull(value, "User ID cannot be null");
 *     }
 * }
 * </pre>
 */
public interface ValueObject {
    // Marker interface for value objects
    // Records implementing this interface automatically get:
    // - Immutability
    // - Value-based equals() and hashCode()
    // - Readable toString()
}