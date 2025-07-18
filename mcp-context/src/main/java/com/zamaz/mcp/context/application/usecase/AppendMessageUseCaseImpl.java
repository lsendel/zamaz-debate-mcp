package com.zamaz.mcp.context.application.usecase;

import com.zamaz.mcp.common.application.exception.ResourceNotFoundException;
import com.zamaz.mcp.common.application.service.TransactionManager;
import com.zamaz.mcp.common.domain.model.OrganizationId;
import com.zamaz.mcp.context.application.command.AppendMessageCommand;
import com.zamaz.mcp.context.application.port.inbound.AppendMessageUseCase;
import com.zamaz.mcp.context.application.port.outbound.ContextCacheService;
import com.zamaz.mcp.context.application.port.outbound.ContextRepository;
import com.zamaz.mcp.context.application.port.outbound.TokenCountingService;
import com.zamaz.mcp.context.domain.model.*;
import com.zamaz.mcp.context.domain.service.ContextDomainService;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the append message use case.
 * Orchestrates appending a message to an existing context.
 */
public class AppendMessageUseCaseImpl implements AppendMessageUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(AppendMessageUseCaseImpl.class);
    
    private final ContextRepository contextRepository;
    private final TokenCountingService tokenCountingService;
    private final ContextCacheService cacheService;
    private final ContextDomainService domainService;
    private final TransactionManager transactionManager;
    
    public AppendMessageUseCaseImpl(
            ContextRepository contextRepository,
            TokenCountingService tokenCountingService,
            ContextCacheService cacheService,
            ContextDomainService domainService,
            TransactionManager transactionManager
    ) {
        this.contextRepository = Objects.requireNonNull(contextRepository, "Context repository cannot be null");
        this.tokenCountingService = Objects.requireNonNull(tokenCountingService, "Token counting service cannot be null");
        this.cacheService = Objects.requireNonNull(cacheService, "Cache service cannot be null");
        this.domainService = Objects.requireNonNull(domainService, "Domain service cannot be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "Transaction manager cannot be null");
    }
    
    @Override
    public MessageId execute(AppendMessageCommand command) {
        logger.info("Appending message to context: {}", command.contextId());
        
        return transactionManager.executeInTransaction(() -> {
            // Load the context
            ContextId contextId = ContextId.from(command.contextId());
            OrganizationId organizationId = OrganizationId.from(command.organizationId());
            
            Context context = contextRepository.findById(contextId)
                .filter(c -> c.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Context not found: " + contextId + " in organization: " + organizationId
                ));
            
            // Prepare message components
            MessageRole role = MessageRole.fromValue(command.role());
            MessageContent content = MessageContent.of(command.content());
            
            // Validate the message
            var validationResult = domainService.validateMessage(context, role, content);
            if (!validationResult.isValid()) {
                throw new IllegalArgumentException(validationResult.errorMessage());
            }
            
            if (validationResult.hasWarnings()) {
                logger.warn("Message validation warning: {}", validationResult.warningMessage());
            }
            
            // Count tokens
            String model = command.model() != null ? command.model() : tokenCountingService.getDefaultModel();
            TokenCount tokenCount = tokenCountingService.countTokens(content, model);
            
            // Append the message using domain logic
            context.appendMessage(role, content, tokenCount);
            
            // Save the updated context
            Context savedContext = contextRepository.save(context);
            
            // Evict cache for this context
            cacheService.evictContext(contextId);
            
            // Get the last message (the one we just added)
            Message lastMessage = savedContext.getMessages().get(savedContext.getMessages().size() - 1);
            
            logger.info("Successfully appended message {} to context {}", 
                lastMessage.getId(), contextId);
            
            return lastMessage.getId();
        });
    }
}