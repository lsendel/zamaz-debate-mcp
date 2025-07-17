package com.zamaz.mcp.common.application.port.inbound;

/**
 * Base interface for use cases that don't return a value.
 * This is part of the application layer in hexagonal architecture.
 * 
 * @param <I> Input type (Command)
 */
public interface VoidUseCase<I> {
    
    /**
     * Executes the use case with the given input.
     * 
     * @param input the use case input
     */
    void execute(I input);
}