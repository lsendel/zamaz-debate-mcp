package com.zamaz.mcp.context.adapter.web.mapper;

import com.zamaz.mcp.common.architecture.mapper.DomainMapper;
import com.zamaz.mcp.context.adapter.web.dto.*;
import com.zamaz.mcp.context.application.command.*;
import com.zamaz.mcp.context.application.query.ContextView;
import com.zamaz.mcp.context.domain.model.ContextWindow;
import com.zamaz.mcp.context.domain.model.MessageSnapshot;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between web DTOs and domain/application objects.
 */
@Component
public class ContextWebMapper implements DomainMapper {
    
    /**
     * Map CreateContextRequest to CreateContextCommand.
     */
    public CreateContextCommand toCommand(CreateContextRequest request, String organizationId, String userId) {
        return new CreateContextCommand(
            organizationId,
            userId,
            request.name(),
            request.metadata()
        );
    }
    
    /**
     * Map AppendMessageRequest to AppendMessageCommand.
     */
    public AppendMessageCommand toCommand(
            AppendMessageRequest request,
            String contextId,
            String organizationId
    ) {
        return new AppendMessageCommand(
            contextId,
            organizationId,
            request.role(),
            request.content(),
            request.model()
        );
    }
    
    /**
     * Map UpdateMetadataRequest to UpdateContextMetadataCommand.
     */
    public UpdateContextMetadataCommand toCommand(
            UpdateMetadataRequest request,
            String contextId,
            String organizationId
    ) {
        return new UpdateContextMetadataCommand(
            contextId,
            organizationId,
            request.metadata()
        );
    }
    
    /**
     * Map ContextView to ContextResponse.
     */
    public ContextResponse toResponse(ContextView view) {
        var messages = view.messages().stream()
            .map(this::toMessageResponse)
            .collect(Collectors.toList());
        
        return new ContextResponse(
            view.id(),
            view.organizationId(),
            view.userId(),
            view.name(),
            view.metadata(),
            view.status(),
            view.totalTokens(),
            view.messageCount(),
            view.visibleMessageCount(),
            view.createdAt(),
            view.updatedAt(),
            messages
        );
    }
    
    /**
     * Map ContextWindow to ContextWindowResponse.
     */
    public ContextWindowResponse toResponse(ContextWindow window) {
        if (window.isEmpty()) {
            return ContextWindowResponse.empty(window.getContextId().asString());
        }
        
        var messages = window.toSnapshots().stream()
            .map(this::toMessageResponse)
            .collect(Collectors.toList());
        
        return new ContextWindowResponse(
            window.getContextId().asString(),
            messages,
            window.getTotalTokens().value(),
            window.getMessageCount()
        );
    }
    
    /**
     * Map MessageView to MessageResponse.
     */
    private ContextResponse.MessageResponse toMessageResponse(ContextView.MessageView message) {
        return new ContextResponse.MessageResponse(
            message.id(),
            message.role(),
            message.content(),
            message.tokenCount(),
            message.timestamp(),
            message.hidden()
        );
    }
    
    /**
     * Map MessageSnapshot to MessageResponse.
     */
    private ContextWindowResponse.MessageResponse toMessageResponse(MessageSnapshot snapshot) {
        return new ContextWindowResponse.MessageResponse(
            snapshot.messageId(),
            snapshot.role().getValue(),
            snapshot.content(),
            snapshot.tokenCount(),
            snapshot.timestamp(),
            snapshot.hidden()
        );
    }
}