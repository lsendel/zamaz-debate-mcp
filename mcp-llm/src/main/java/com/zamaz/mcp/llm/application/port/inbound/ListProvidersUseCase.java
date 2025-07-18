package com.zamaz.mcp.llm.application.port.inbound;

import com.zamaz.mcp.common.application.port.inbound.UseCase;
import com.zamaz.mcp.llm.application.query.ListProvidersQuery;
import com.zamaz.mcp.llm.application.query.ProviderListResult;

/**
 * Use case for listing available LLM providers and their capabilities.
 * This is an inbound port in hexagonal architecture.
 */
public interface ListProvidersUseCase extends UseCase<ListProvidersQuery, ProviderListResult> {
}