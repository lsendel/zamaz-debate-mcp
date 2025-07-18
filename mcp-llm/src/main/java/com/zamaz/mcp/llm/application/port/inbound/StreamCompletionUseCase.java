package com.zamaz.mcp.llm.application.port.inbound;

import com.zamaz.mcp.llm.application.command.StreamCompletionCommand;
import com.zamaz.mcp.llm.application.query.CompletionChunk;
import reactor.core.publisher.Flux;

/**
 * Use case for generating streaming text completions using LLM providers.
 * This is an inbound port in hexagonal architecture.
 */
public interface StreamCompletionUseCase {
    
    /**
     * Generate a streaming completion.
     */
    Flux<CompletionChunk> execute(StreamCompletionCommand command);
}