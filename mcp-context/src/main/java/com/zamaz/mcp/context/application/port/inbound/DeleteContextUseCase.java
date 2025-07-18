package com.zamaz.mcp.context.application.port.inbound;

import com.zamaz.mcp.common.application.port.inbound.VoidUseCase;
import com.zamaz.mcp.context.application.command.DeleteContextCommand;

/**
 * Use case for permanently deleting a context.
 * This is an inbound port in hexagonal architecture.
 * 
 * This operation completely removes the context and all associated
 * messages from the system. This is a destructive operation that
 * cannot be undone. Consider archiving instead if you need to 
 * preserve historical data.
 */
public interface DeleteContextUseCase extends VoidUseCase<DeleteContextCommand> {
}