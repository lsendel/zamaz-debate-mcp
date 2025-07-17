package com.zamaz.mcp.common.application.port.inbound;

/**
 * Base interface for all use cases (inbound ports).
 * Use cases define the application's business operations.
 * This is part of the application layer in hexagonal architecture.
 * 
 * @param <I> Input type (Command or Query)
 * @param <O> Output type (Response)
 */
public interface UseCase<I, O> {
    
    /**
     * Executes the use case with the given input.
     * 
     * @param input the use case input
     * @return the use case output
     */
    O execute(I input);
}