package com.zamaz.mcp.context.application.usecase;

import com.zamaz.mcp.common.application.exception.ResourceNotFoundException;
import com.zamaz.mcp.common.application.service.TransactionManager;
import com.zamaz.mcp.common.domain.model.OrganizationId;
import com.zamaz.mcp.context.application.command.DeleteContextCommand;
import com.zamaz.mcp.context.application.port.inbound.DeleteContextUseCase;
import com.zamaz.mcp.context.application.port.outbound.ContextCacheService;
import com.zamaz.mcp.context.application.port.outbound.ContextRepository;
import com.zamaz.mcp.context.domain.model.Context;
import com.zamaz.mcp.context.domain.model.ContextId;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the delete context use case.
 * Permanently deletes a context and all associated data.
 */
public class DeleteContextUseCaseImpl implements DeleteContextUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(DeleteContextUseCaseImpl.class);
    
    private final ContextRepository contextRepository;
    private final ContextCacheService cacheService;
    private final TransactionManager transactionManager;
    
    public DeleteContextUseCaseImpl(
            ContextRepository contextRepository,
            ContextCacheService cacheService,
            TransactionManager transactionManager
    ) {
        this.contextRepository = Objects.requireNonNull(contextRepository, "Context repository cannot be null");
        this.cacheService = Objects.requireNonNull(cacheService, "Cache service cannot be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "Transaction manager cannot be null");
    }
    
    @Override
    public void execute(DeleteContextCommand command) {
        logger.info("Deleting context: {} for organization: {}", 
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
            
            // Mark as deleted using domain logic
            context.delete();
            
            // Save the updated context (soft delete)
            contextRepository.save(context);
            
            // Evict from cache
            cacheService.evictContext(contextId);
            
            logger.info("Successfully deleted context: {}", contextId);
            
            return null;
        });
    }
}