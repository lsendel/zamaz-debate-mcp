package com.zamaz.mcp.context.application.port.inbound;

import com.zamaz.mcp.common.application.port.inbound.VoidUseCase;
import com.zamaz.mcp.context.application.command.UpdateContextMetadataCommand;

/**
 * Use case for updating context metadata.
 * This is an inbound port in hexagonal architecture.
 * 
 * Allows updating context metadata such as name, description, tags,
 * and other non-message attributes. The context version is incremented
 * to track changes. Only active (non-archived) contexts can be updated.
 */
public interface UpdateContextMetadataUseCase extends VoidUseCase<UpdateContextMetadataCommand> {
}