package com.zamaz.mcp.context.application.usecase;

import com.zamaz.mcp.common.application.exception.ResourceNotFoundException;
import com.zamaz.mcp.common.domain.model.OrganizationId;
import com.zamaz.mcp.context.application.port.inbound.GetContextUseCase;
import com.zamaz.mcp.context.application.port.outbound.ContextRepository;
import com.zamaz.mcp.context.application.query.ContextView;
import com.zamaz.mcp.context.application.query.GetContextQuery;
import com.zamaz.mcp.context.domain.model.Context;
import com.zamaz.mcp.context.domain.model.ContextId;
import com.zamaz.mcp.context.domain.model.Message;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the get context use case.
 * Retrieves a context and transforms it to a view model.
 */
public class GetContextUseCaseImpl implements GetContextUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(GetContextUseCaseImpl.class);
    
    private final ContextRepository contextRepository;
    
    public GetContextUseCaseImpl(ContextRepository contextRepository) {
        this.contextRepository = Objects.requireNonNull(contextRepository, "Context repository cannot be null");
    }
    
    @Override
    public ContextView execute(GetContextQuery query) {
        logger.debug("Retrieving context: {} for organization: {}", 
            query.contextId(), query.organizationId());
        
        // Load the context
        ContextId contextId = ContextId.from(query.contextId());
        OrganizationId organizationId = OrganizationId.from(query.organizationId());
        
        Context context = contextRepository.findById(contextId)
            .filter(c -> c.getOrganizationId().equals(organizationId))
            .orElseThrow(() -> new ResourceNotFoundException(
                "Context not found: " + contextId + " in organization: " + organizationId
            ));
        
        // Transform to view model
        return toContextView(context);
    }
    
    private ContextView toContextView(Context context) {
        var messageViews = context.getMessages().stream()
            .map(this::toMessageView)
            .collect(Collectors.toList());
        
        return new ContextView(
            context.getId().asString(),
            context.getOrganizationId().value(),
            context.getUserId().value(),
            context.getName(),
            context.getStatus().getValue(),
            messageViews,
            context.getMetadata().asMap(),
            context.getTotalTokens().value(),
            context.getMessageCount(),
            context.getVisibleMessageCount(),
            context.getCreatedAt(),
            context.getUpdatedAt()
        );
    }
    
    private ContextView.MessageView toMessageView(Message message) {
        return new ContextView.MessageView(
            message.getId().asString(),
            message.getRole().getValue(),
            message.getContent().value(),
            message.getTokenCount().value(),
            message.getTimestamp(),
            message.isHidden()
        );
    }
}