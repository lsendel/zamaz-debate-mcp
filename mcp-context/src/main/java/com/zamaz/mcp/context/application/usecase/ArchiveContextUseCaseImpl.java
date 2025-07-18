package com.zamaz.mcp.context.application.usecase;

import com.zamaz.mcp.common.application.exception.ResourceNotFoundException;
import com.zamaz.mcp.common.application.service.TransactionManager;
import com.zamaz.mcp.common.domain.model.OrganizationId;
import com.zamaz.mcp.context.application.command.ArchiveContextCommand;
import com.zamaz.mcp.context.application.port.inbound.ArchiveContextUseCase;
import com.zamaz.mcp.context.application.port.outbound.ContextCacheService;
import com.zamaz.mcp.context.application.port.outbound.ContextRepository;
import com.zamaz.mcp.context.domain.model.Context;
import com.zamaz.mcp.context.domain.model.ContextId;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the archive context use case.
 * Archives a context making it read-only while preserving it for historical purposes.
 */
public class ArchiveContextUseCaseImpl implements ArchiveContextUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(ArchiveContextUseCaseImpl.class);
    
    private final ContextRepository contextRepository;
    private final ContextCacheService cacheService;
    private final TransactionManager transactionManager;
    
    public ArchiveContextUseCaseImpl(
            ContextRepository contextRepository,
            ContextCacheService cacheService,
            TransactionManager transactionManager
    ) {
        this.contextRepository = Objects.requireNonNull(contextRepository, "Context repository cannot be null");
        this.cacheService = Objects.requireNonNull(cacheService, "Cache service cannot be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "Transaction manager cannot be null");
    }
    
    @Override
    public void execute(ArchiveContextCommand command) {
        logger.info("Archiving context: {} for organization: {}", 
            command.contextId(), command.organizationId());
        
        transactionManager.executeInTransaction(() -> {
            // Create domain objects from command
            ContextId contextId = ContextId.from(command.contextId());
            OrganizationId organizationId = OrganizationId.from(command.organizationId());
            
            // Load the context
            Context context = contextRepository.findById(contextId)
                .filter(c -> c.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Context not found: " + contextId + " in organization: " + organizationId
                ));
            
            // Archive using domain logic
            context.archive();
            
            // Save the updated context
            contextRepository.save(context);
            
            // Evict from cache
            cacheService.evictContext(contextId);
            
            logger.info("Successfully archived context: {}", contextId);
            
            return null;
        });
    }
}