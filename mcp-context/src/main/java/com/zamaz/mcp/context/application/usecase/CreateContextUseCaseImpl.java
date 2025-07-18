package com.zamaz.mcp.context.application.usecase;

import com.zamaz.mcp.common.application.service.TransactionManager;
import com.zamaz.mcp.common.domain.model.OrganizationId;
import com.zamaz.mcp.common.domain.model.UserId;
import com.zamaz.mcp.context.application.command.CreateContextCommand;
import com.zamaz.mcp.context.application.port.inbound.CreateContextUseCase;
import com.zamaz.mcp.context.application.port.outbound.ContextRepository;
import com.zamaz.mcp.context.domain.model.Context;
import com.zamaz.mcp.context.domain.model.ContextId;
import com.zamaz.mcp.context.domain.model.ContextMetadata;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the create context use case.
 * Orchestrates the creation of a new context.
 */
public class CreateContextUseCaseImpl implements CreateContextUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(CreateContextUseCaseImpl.class);
    
    private final ContextRepository contextRepository;
    private final TransactionManager transactionManager;
    
    public CreateContextUseCaseImpl(
            ContextRepository contextRepository,
            TransactionManager transactionManager
    ) {
        this.contextRepository = Objects.requireNonNull(contextRepository, "Context repository cannot be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "Transaction manager cannot be null");
    }
    
    @Override
    public ContextId execute(CreateContextCommand command) {
        logger.info("Creating new context for organization: {} and user: {}", 
            command.organizationId(), command.userId());
        
        return transactionManager.executeInTransaction(() -> {
            // Create domain objects from command
            OrganizationId organizationId = OrganizationId.from(command.organizationId());
            UserId userId = UserId.from(command.userId());
            ContextMetadata metadata = ContextMetadata.of(command.metadata());
            
            // Create the context using domain logic
            Context context = Context.create(
                organizationId,
                userId,
                command.name(),
                metadata
            );
            
            // Persist the context
            Context savedContext = contextRepository.save(context);
            
            logger.info("Successfully created context with ID: {}", savedContext.getId());
            
            // Return the ID of the created context
            return savedContext.getId();
        });
    }
}