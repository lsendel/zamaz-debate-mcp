package com.zamaz.mcp.context.adapter.persistence.mapper;

import com.zamaz.mcp.common.architecture.mapper.DomainMapper;
import com.zamaz.mcp.common.domain.model.OrganizationId;
import com.zamaz.mcp.common.domain.model.UserId;
import com.zamaz.mcp.context.adapter.persistence.entity.*;
import com.zamaz.mcp.context.domain.model.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between JPA entities and domain models.
 */
@Component
public class ContextPersistenceMapper implements DomainMapper {
    
    /**
     * Convert Context domain model to ContextEntity.
     */
    public ContextEntity toEntity(Context context) {
        ContextEntity entity = new ContextEntity();
        entity.setId(context.getId().asString());
        entity.setOrganizationId(context.getOrganizationId().value());
        entity.setUserId(context.getUserId().value());
        entity.setName(context.getName());
        entity.setStatus(toEntityStatus(context.getStatus()));
        entity.setMetadata(context.getMetadata().asMap());
        entity.setTotalTokens(context.getTotalTokens().value());
        entity.setCreatedAt(context.getCreatedAt());
        entity.setUpdatedAt(context.getUpdatedAt());
        
        // Map messages
        var messageEntities = context.getMessages().stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
        
        // Clear and add to maintain relationship
        entity.getMessages().clear();
        messageEntities.forEach(entity::addMessage);
        
        return entity;
    }
    
    /**
     * Convert ContextEntity to Context domain model.
     */
    public Context toDomain(ContextEntity entity) {
        // First restore messages
        var messages = entity.getMessages().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
        
        // Then restore the context
        return Context.restore(
            ContextId.from(entity.getId()),
            OrganizationId.from(entity.getOrganizationId()),
            UserId.from(entity.getUserId()),
            entity.getName(),
            ContextMetadata.of(entity.getMetadata()),
            toDomainStatus(entity.getStatus()),
            messages,
            TokenCount.of(entity.getTotalTokens()),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
    
    /**
     * Convert Message domain model to MessageEntity.
     */
    private MessageEntity toEntity(Message message) {
        MessageEntity entity = new MessageEntity();
        entity.setId(message.getId().asString());
        entity.setRole(toEntityRole(message.getRole()));
        entity.setContent(message.getContent().value());
        entity.setTokenCount(message.getTokenCount().value());
        entity.setTimestamp(message.getTimestamp());
        entity.setHidden(message.isHidden());
        
        return entity;
    }
    
    /**
     * Convert MessageEntity to Message domain model.
     */
    private Message toDomain(MessageEntity entity) {
        return Message.restore(
            MessageId.from(entity.getId()),
            toDomainRole(entity.getRole()),
            MessageContent.of(entity.getContent()),
            TokenCount.of(entity.getTokenCount()),
            entity.getTimestamp(),
            entity.getHidden()
        );
    }
    
    /**
     * Convert domain ContextStatus to entity ContextStatusEntity.
     */
    public ContextStatusEntity toEntityStatus(ContextStatus status) {
        return switch (status) {
            case ACTIVE -> ContextStatusEntity.ACTIVE;
            case ARCHIVED -> ContextStatusEntity.ARCHIVED;
            case DELETED -> ContextStatusEntity.DELETED;
        };
    }
    
    /**
     * Convert entity ContextStatusEntity to domain ContextStatus.
     */
    private ContextStatus toDomainStatus(ContextStatusEntity status) {
        return switch (status) {
            case ACTIVE -> ContextStatus.ACTIVE;
            case ARCHIVED -> ContextStatus.ARCHIVED;
            case DELETED -> ContextStatus.DELETED;
        };
    }
    
    /**
     * Convert domain MessageRole to entity MessageRoleEntity.
     */
    private MessageRoleEntity toEntityRole(MessageRole role) {
        return switch (role) {
            case USER -> MessageRoleEntity.USER;
            case ASSISTANT -> MessageRoleEntity.ASSISTANT;
            case SYSTEM -> MessageRoleEntity.SYSTEM;
            case FUNCTION -> MessageRoleEntity.FUNCTION;
        };
    }
    
    /**
     * Convert entity MessageRoleEntity to domain MessageRole.
     */
    private MessageRole toDomainRole(MessageRoleEntity role) {
        return switch (role) {
            case USER -> MessageRole.USER;
            case ASSISTANT -> MessageRole.ASSISTANT;
            case SYSTEM -> MessageRole.SYSTEM;
            case FUNCTION -> MessageRole.FUNCTION;
        };
    }
}