package com.zamaz.mcp.llm.application.port.inbound;

import com.zamaz.mcp.common.application.port.inbound.UseCase;
import com.zamaz.mcp.llm.application.command.GenerateCompletionCommand;
import com.zamaz.mcp.llm.application.query.CompletionResult;

/**
 * Use case for generating text completions using LLM providers.
 * This is an inbound port in hexagonal architecture.
 */
public interface GenerateCompletionUseCase extends UseCase<GenerateCompletionCommand, CompletionResult> {
}