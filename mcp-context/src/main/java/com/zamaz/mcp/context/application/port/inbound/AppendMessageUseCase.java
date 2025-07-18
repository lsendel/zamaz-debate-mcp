package com.zamaz.mcp.context.application.port.inbound;

import com.zamaz.mcp.common.application.port.inbound.UseCase;
import com.zamaz.mcp.context.application.command.AppendMessageCommand;
import com.zamaz.mcp.context.domain.model.MessageId;

/**
 * Use case for appending a message to an existing context.
 * This is an inbound port in hexagonal architecture.
 */
public interface AppendMessageUseCase extends UseCase<AppendMessageCommand, MessageId> {
}