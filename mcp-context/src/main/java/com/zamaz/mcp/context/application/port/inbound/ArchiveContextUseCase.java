package com.zamaz.mcp.context.application.port.inbound;

import com.zamaz.mcp.common.application.port.inbound.VoidUseCase;
import com.zamaz.mcp.context.application.command.ArchiveContextCommand;

/**
 * Use case for archiving a context.
 * This is an inbound port in hexagonal architecture.
 * 
 * Archived contexts are marked as inactive but retained for historical
 * purposes. They can be retrieved but cannot be modified further.
 * This operation is idempotent - archiving an already archived context
 * has no additional effect.
 */
public interface ArchiveContextUseCase extends VoidUseCase<ArchiveContextCommand> {
}