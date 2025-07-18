package com.zamaz.mcp.context.application.usecase;

import com.zamaz.mcp.common.application.exception.ResourceNotFoundException;
import com.zamaz.mcp.common.application.service.TransactionManager;
import com.zamaz.mcp.common.domain.model.OrganizationId;
import com.zamaz.mcp.context.application.command.UpdateContextMetadataCommand;
import com.zamaz.mcp.context.application.port.inbound.UpdateContextMetadataUseCase;
import com.zamaz.mcp.context.application.port.outbound.ContextCacheService;
import com.zamaz.mcp.context.application.port.outbound.ContextRepository;
import com.zamaz.mcp.context.domain.model.Context;
import com.zamaz.mcp.context.domain.model.ContextId;
import com.zamaz.mcp.context.domain.model.ContextMetadata;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the update context metadata use case.
 * Updates the metadata of an existing context.
 */
public class UpdateContextMetadataUseCaseImpl implements UpdateContextMetadataUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(UpdateContextMetadataUseCaseImpl.class);
    
    private final ContextRepository contextRepository;
    private final ContextCacheService cacheService;
    private final TransactionManager transactionManager;
    
    public UpdateContextMetadataUseCaseImpl(
            ContextRepository contextRepository,
            ContextCacheService cacheService,
            TransactionManager transactionManager
    ) {
        this.contextRepository = Objects.requireNonNull(contextRepository, "Context repository cannot be null");
        this.cacheService = Objects.requireNonNull(cacheService, "Cache service cannot be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "Transaction manager cannot be null");
    }
    
    @Override
    public void execute(UpdateContextMetadataCommand command) {
        logger.info("Updating metadata for context: {} in organization: {}", 
            command.contextId(), command.organizationId());
        
        transactionManager.executeInTransaction(() -> {
            // Create domain objects from command
            ContextId contextId = ContextId.from(command.contextId());
            OrganizationId organizationId = OrganizationId.from(command.organizationId());
            ContextMetadata newMetadata = ContextMetadata.of(command.metadata());
            
            // Load the context
            Context context = contextRepository.findById(contextId)
                .filter(c -> c.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Context not found: " + contextId + " in organization: " + organizationId
                ));
            
            // Update metadata using domain logic
            context.updateMetadata(newMetadata);
            
            // Save the updated context
            contextRepository.save(context);
            
            // Evict from cache
            cacheService.evictContext(contextId);
            
            logger.info("Successfully updated metadata for context: {}", contextId);
            
            return null;
        });
    }
}