package com.zamaz.mcp.context.config;

import com.zamaz.mcp.common.application.service.TransactionManager;
import com.zamaz.mcp.context.adapter.infrastructure.ContextDomainServiceAdapter;
import com.zamaz.mcp.context.application.port.inbound.*;
import com.zamaz.mcp.context.application.port.outbound.ContextCacheService;
import com.zamaz.mcp.context.application.port.outbound.ContextRepository;
import com.zamaz.mcp.context.application.port.outbound.TokenCountingService;
import com.zamaz.mcp.context.application.usecase.*;
import com.zamaz.mcp.context.domain.service.ContextDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for hexagonal architecture components.
 * Wires use cases with their dependencies following dependency inversion principle.
 */
@Configuration
public class HexagonalArchitectureConfig {
    
    @Bean
    public CreateContextUseCase createContextUseCase(
            ContextRepository contextRepository,
            TransactionManager transactionManager
    ) {
        return new CreateContextUseCaseImpl(contextRepository, transactionManager);
    }
    
    @Bean
    public GetContextUseCase getContextUseCase(
            ContextRepository contextRepository
    ) {
        return new GetContextUseCaseImpl(contextRepository);
    }
    
    @Bean
    public AppendMessageUseCase appendMessageUseCase(
            ContextRepository contextRepository,
            TokenCountingService tokenCountingService,
            ContextCacheService cacheService,
            ContextDomainService domainService,
            TransactionManager transactionManager
    ) {
        return new AppendMessageUseCaseImpl(
            contextRepository,
            tokenCountingService,
            cacheService,
            domainService,
            transactionManager
        );
    }
    
    @Bean
    public GetContextWindowUseCase getContextWindowUseCase(
            ContextRepository contextRepository,
            ContextCacheService cacheService
    ) {
        return new GetContextWindowUseCaseImpl(contextRepository, cacheService);
    }
    
    @Bean
    public UpdateContextMetadataUseCase updateContextMetadataUseCase(
            ContextRepository contextRepository,
            ContextCacheService cacheService,
            TransactionManager transactionManager
    ) {
        return new UpdateContextMetadataUseCaseImpl(
            contextRepository,
            cacheService,
            transactionManager
        );
    }
    
    @Bean
    public ArchiveContextUseCase archiveContextUseCase(
            ContextRepository contextRepository,
            ContextCacheService cacheService,
            TransactionManager transactionManager
    ) {
        return new ArchiveContextUseCaseImpl(
            contextRepository,
            cacheService,
            transactionManager
        );
    }
    
    @Bean
    public DeleteContextUseCase deleteContextUseCase(
            ContextRepository contextRepository,
            ContextCacheService cacheService,
            TransactionManager transactionManager
    ) {
        return new DeleteContextUseCaseImpl(
            contextRepository,
            cacheService,
            transactionManager
        );
    }
    
    @Bean
    public ContextDomainService contextDomainService() {
        return new ContextDomainServiceAdapter();
    }
}