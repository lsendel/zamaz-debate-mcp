package com.zamaz.mcp.context.application.port.inbound;

import com.zamaz.mcp.common.application.port.inbound.UseCase;
import com.zamaz.mcp.context.application.command.CreateContextCommand;
import com.zamaz.mcp.context.domain.model.ContextId;

/**
 * Use case for creating a new context.
 * This is an inbound port in hexagonal architecture.
 */
public interface CreateContextUseCase extends UseCase<CreateContextCommand, ContextId> {
}