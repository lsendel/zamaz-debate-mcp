package com.zamaz.mcp.llm.application.port.inbound;

import com.zamaz.mcp.common.application.port.inbound.UseCase;
import com.zamaz.mcp.llm.application.command.CheckProviderHealthCommand;
import com.zamaz.mcp.llm.application.query.ProviderHealthResult;

/**
 * Use case for checking the health status of LLM providers.
 * This is an inbound port in hexagonal architecture.
 */
public interface CheckProviderHealthUseCase extends UseCase<CheckProviderHealthCommand, ProviderHealthResult> {
}