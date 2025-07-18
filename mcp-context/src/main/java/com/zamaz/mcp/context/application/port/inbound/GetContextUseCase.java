package com.zamaz.mcp.context.application.port.inbound;

import com.zamaz.mcp.common.application.port.inbound.UseCase;
import com.zamaz.mcp.context.application.query.GetContextQuery;
import com.zamaz.mcp.context.application.view.ContextView;

/**
 * Use case for retrieving a complete context by its ID.
 * This is an inbound port in hexagonal architecture.
 * 
 * Returns a read-only view of the context including all messages,
 * metadata, and associated information.
 */
public interface GetContextUseCase extends UseCase<GetContextQuery, ContextView> {
}