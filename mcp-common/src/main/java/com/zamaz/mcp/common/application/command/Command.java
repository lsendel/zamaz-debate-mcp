package com.zamaz.mcp.common.application.command;

/**
 * Marker interface for commands in CQRS pattern.
 * Commands represent intentions to change the state of the system.
 * This is part of the application layer in hexagonal architecture.
 */
public interface Command {
    // Marker interface for commands
    // Commands should:
    // - Be immutable
    // - Represent a single intention
    // - Contain all data needed to execute the command
    // - Have descriptive names (e.g., CreateOrganizationCommand)
}