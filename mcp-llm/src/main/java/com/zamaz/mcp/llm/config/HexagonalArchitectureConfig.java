package com.zamaz.mcp.llm.config;

import com.zamaz.mcp.common.application.service.TransactionManager;
import com.zamaz.mcp.llm.adapter.infrastructure.ProviderSelectionServiceAdapter;
import com.zamaz.mcp.llm.application.port.inbound.*;
import com.zamaz.mcp.llm.application.port.outbound.*;
import com.zamaz.mcp.llm.application.usecase.*;
import com.zamaz.mcp.llm.domain.service.ProviderSelectionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for hexagonal architecture components.
 * Wires use cases with their dependencies following dependency inversion principle.
 */
@Configuration
public class HexagonalArchitectureConfig {
    
    @Bean
    public GenerateCompletionUseCase generateCompletionUseCase(
            ProviderRepository providerRepository,
            LlmProviderGateway providerGateway,
            CompletionCacheService cacheService,
            ProviderSelectionService selectionService
    ) {
        return new GenerateCompletionUseCaseImpl(
            providerRepository,
            providerGateway,
            cacheService,
            selectionService
        );
    }
    
    @Bean
    public StreamCompletionUseCase streamCompletionUseCase(
            ProviderRepository providerRepository,
            LlmProviderGateway providerGateway,
            ProviderSelectionService selectionService
    ) {
        return new StreamCompletionUseCaseImpl(
            providerRepository,
            providerGateway,
            selectionService
        );
    }
    
    @Bean
    public ListProvidersUseCase listProvidersUseCase(
            ProviderRepository providerRepository
    ) {
        return new ListProvidersUseCaseImpl(providerRepository);
    }
    
    @Bean
    public CheckProviderHealthUseCase checkProviderHealthUseCase(
            ProviderRepository providerRepository,
            LlmProviderGateway providerGateway,
            TransactionManager transactionManager
    ) {
        return new CheckProviderHealthUseCaseImpl(
            providerRepository,
            providerGateway,
            transactionManager
        );
    }
    
    @Bean
    public ProviderSelectionService providerSelectionService() {
        return new ProviderSelectionServiceAdapter();
    }
}