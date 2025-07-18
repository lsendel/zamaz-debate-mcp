package com.zamaz.mcp.debateengine.adapter.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.debateengine.adapter.persistence.entity.*;
import com.zamaz.mcp.debateengine.domain.model.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mapper for converting between domain models and JPA entities.
 */
@Component
public class DebateEntityMapper {
    
    private final ObjectMapper objectMapper;
    
    public DebateEntityMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Convert domain debate to JPA entity.
     */
    public DebateEntity toEntity(Debate debate) {
        DebateEntity entity = new DebateEntity();
        
        entity.setId(UUID.fromString(debate.getId().toString()));
        entity.setOrganizationId(UUID.fromString(debate.getOrganizationId().toString()));
        entity.setCreatedByUserId(debate.getCreatedByUserId());
        entity.setTopic(debate.getTopic().toString());
        entity.setDescription(debate.getDescription());
        entity.setStatus(mapDebateStatus(debate.getStatus()));
        
        // Map configuration
        DebateConfiguration config = debate.getConfiguration();
        entity.setMaxParticipants(config.maxParticipants());
        entity.setMaxRounds(config.maxRounds());
        entity.setRoundTimeoutMs(config.roundTimeout().toMillis());
        entity.setVisibility(config.visibility().name());
        
        // Serialize settings to JSON
        try {
            entity.setSettings(objectMapper.writeValueAsString(config.settings()));
        } catch (JsonProcessingException e) {
            entity.setSettings("{}");
        }
        
        entity.setCurrentRound(debate.getCurrentRoundNumber());
        entity.setContextId(debate.getContextId() != null ? 
            UUID.fromString(debate.getContextId().toString()) : null);
        entity.setStartedAt(debate.getStartedAt());
        entity.setCompletedAt(debate.getCompletedAt());
        entity.setCreatedAt(debate.getCreatedAt());
        entity.setUpdatedAt(debate.getUpdatedAt());
        
        return entity;
    }
    
    /**
     * Convert JPA entity to domain debate.
     */
    public Debate toDomain(DebateEntity entity) {
        // Note: This is a simplified implementation
        // In production, would properly reconstruct the aggregate
        
        DebateId debateId = DebateId.from(entity.getId().toString());
        OrganizationId organizationId = OrganizationId.from(entity.getOrganizationId().toString());
        DebateTopic topic = DebateTopic.of(entity.getTopic());
        
        // Parse settings
        Map<String, Object> settings = new HashMap<>();
        if (entity.getSettings() != null) {
            try {
                settings = objectMapper.readValue(entity.getSettings(), Map.class);
            } catch (JsonProcessingException e) {
                // Ignore, use empty settings
            }
        }
        
        // Map visibility
        DebateConfiguration.Visibility visibility = 
            DebateConfiguration.Visibility.valueOf(entity.getVisibility());
        
        DebateConfiguration config = DebateConfiguration.of(
            entity.getMaxParticipants(),
            entity.getMaxRounds(),
            Duration.ofMillis(entity.getRoundTimeoutMs()),
            visibility,
            settings
        );
        
        // Create debate
        Debate debate = Debate.create(
            debateId,
            organizationId,
            entity.getCreatedByUserId(),
            topic,
            entity.getDescription(),
            config
        );
        
        // Set context if present
        if (entity.getContextId() != null) {
            debate.setContext(ContextId.from(entity.getContextId().toString()));
        }
        
        // Note: In full implementation, would also reconstruct participants and rounds
        
        return debate;
    }
    
    /**
     * Convert domain participant to JPA entity.
     */
    public ParticipantEntity toParticipantEntity(Participant participant, DebateEntity debateEntity) {
        ParticipantEntity entity = new ParticipantEntity();
        
        entity.setId(UUID.fromString(participant.getId().toString()));
        entity.setDebate(debateEntity);
        entity.setParticipantType(mapParticipantType(participant.getType()));
        entity.setPosition(mapPosition(participant.getPosition()));
        entity.setUserId(participant.getUserId());
        
        // Map AI model if present
        if (participant.getAiModel() != null) {
            AIModel model = participant.getAiModel();
            entity.setModelProvider(model.provider());
            entity.setModelName(model.name());
            try {
                entity.setModelConfig(objectMapper.writeValueAsString(model.config()));
            } catch (JsonProcessingException e) {
                entity.setModelConfig("{}");
            }
        }
        
        entity.setTotalResponses(participant.getTotalResponses());
        entity.setAvgResponseTime(participant.getAverageResponseTimeMs());
        entity.setJoinedAt(participant.getJoinedAt());
        entity.setLeftAt(participant.getLeftAt());
        entity.setCreatedAt(participant.getJoinedAt());
        
        return entity;
    }
    
    /**
     * Convert domain context to JPA entity.
     */
    public ContextEntity toContextEntity(Context context) {
        ContextEntity entity = new ContextEntity();
        
        entity.setId(UUID.fromString(context.getId().toString()));
        entity.setOrganizationId(UUID.fromString(context.getOrganizationId().toString()));
        entity.setUserId(context.getUserId());
        entity.setName(context.getName());
        entity.setDescription(context.getDescription());
        entity.setStatus(mapContextStatus(context.getStatus()));
        entity.setTotalTokens(context.getTotalTokens());
        entity.setMaxTokens(context.getMaxTokens());
        entity.setMessageCount(context.getMessages().size());
        entity.setWindowSize(context.getWindowSize());
        entity.setVersion(context.getVersion());
        entity.setLastActivityAt(context.getLastActivityAt());
        entity.setCreatedAt(context.getCreatedAt());
        entity.setUpdatedAt(context.getUpdatedAt());
        
        return entity;
    }
    
    /**
     * Convert JPA entity to domain context.
     */
    public Context toDomainContext(ContextEntity entity) {
        ContextId contextId = ContextId.from(entity.getId().toString());
        DebateId debateId = entity.getDebate() != null ? 
            DebateId.from(entity.getDebate().getId().toString()) : null;
        OrganizationId organizationId = OrganizationId.from(entity.getOrganizationId().toString());
        
        Context context = Context.create(
            contextId,
            debateId,
            organizationId,
            entity.getUserId(),
            entity.getName(),
            entity.getDescription(),
            entity.getMaxTokens(),
            entity.getWindowSize()
        );
        
        // Note: In full implementation, would also reconstruct messages
        
        return context;
    }
    
    // Status mapping methods
    
    private DebateEntity.DebateStatusEnum mapDebateStatus(DebateStatus status) {
        return switch (status) {
            case DRAFT -> DebateEntity.DebateStatusEnum.DRAFT;
            case ACTIVE -> DebateEntity.DebateStatusEnum.ACTIVE;
            case COMPLETED -> DebateEntity.DebateStatusEnum.COMPLETED;
            case CANCELLED -> DebateEntity.DebateStatusEnum.CANCELLED;
        };
    }
    
    private ParticipantEntity.ParticipantTypeEnum mapParticipantType(ParticipantType type) {
        return switch (type) {
            case HUMAN -> ParticipantEntity.ParticipantTypeEnum.HUMAN;
            case AI -> ParticipantEntity.ParticipantTypeEnum.AI;
        };
    }
    
    private ParticipantEntity.PositionEnum mapPosition(Position position) {
        return switch (position) {
            case PRO -> ParticipantEntity.PositionEnum.PRO;
            case CON -> ParticipantEntity.PositionEnum.CON;
            case MODERATOR -> ParticipantEntity.PositionEnum.MODERATOR;
            case JUDGE -> ParticipantEntity.PositionEnum.JUDGE;
            case OBSERVER -> ParticipantEntity.PositionEnum.OBSERVER;
        };
    }
    
    private ContextEntity.ContextStatusEnum mapContextStatus(Context.ContextStatus status) {
        return switch (status) {
            case ACTIVE -> ContextEntity.ContextStatusEnum.ACTIVE;
            case ARCHIVED -> ContextEntity.ContextStatusEnum.ARCHIVED;
            case DELETED -> ContextEntity.ContextStatusEnum.DELETED;
        };
    }
}