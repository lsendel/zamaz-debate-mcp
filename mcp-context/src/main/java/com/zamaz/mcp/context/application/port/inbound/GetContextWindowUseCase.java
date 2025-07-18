package com.zamaz.mcp.context.application.port.inbound;

import com.zamaz.mcp.common.application.port.inbound.UseCase;
import com.zamaz.mcp.context.application.query.GetContextWindowQuery;
import com.zamaz.mcp.context.domain.model.ContextWindow;

/**
 * Use case for retrieving a windowed view of a context.
 * This is an inbound port in hexagonal architecture.
 * 
 * Returns a context window that respects token limits and includes
 * only the most relevant messages based on the windowing strategy.
 * This is essential for managing LLM token constraints.
 */
public interface GetContextWindowUseCase extends UseCase<GetContextWindowQuery, ContextWindow> {
}