package com.zamaz.mcp.controller.adapter.persistence.mapper;

import com.zamaz.mcp.controller.adapter.persistence.entity.DebateEntity;
import com.zamaz.mcp.controller.adapter.persistence.entity.ParticipantEntity;
import com.zamaz.mcp.controller.adapter.persistence.entity.ResponseEntity;
import com.zamaz.mcp.controller.adapter.persistence.entity.RoundEntity;
import com.zamaz.mcp.controller.domain.model.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper between domain objects and JPA entities.
 */
@Component
public class DebateEntityMapper {
    
    public DebateEntity toEntity(Debate debate) {
        DebateEntity entity = new DebateEntity(
            debate.getId().value(),
            debate.getTopic(),
            debate.getStatus().getValue(),
            debate.getCreatedAt()
        );
        
        entity.setStartedAt(debate.getStartedAt().orElse(null));
        entity.setCompletedAt(debate.getCompletedAt().orElse(null));
        entity.setResult(debate.getResult().orElse(null));
        
        // Map configuration
        DebateConfig config = debate.getConfig();
        entity.setMinParticipants(config.minParticipants());
        entity.setMaxParticipants(config.maxParticipants());
        entity.setMaxRounds(config.maxRounds());
        entity.setRoundTimeLimitMinutes(config.hasRoundTimeLimit() ? (int) config.roundTimeLimit().toMinutes() : null);
        entity.setMaxDebateDurationHours(config.hasMaxDebateDuration() ? (int) config.maxDebateDuration().toHours() : null);
        entity.setRequireBalancedPositions(config.requireBalancedPositions());
        entity.setAutoAdvanceRounds(config.shouldAutoAdvanceRounds());
        entity.setAllowSpectators(config.allowsSpectators());
        entity.setMaxResponseLength(config.maxResponseLength());
        entity.setEnableQualityAssessment(config.hasQualityAssessment());
        
        // Map participants
        for (Participant participant : debate.getParticipants().values()) {
            ParticipantEntity participantEntity = toParticipantEntity(participant);
            entity.addParticipant(participantEntity);
        }
        
        // Map rounds
        for (Round round : debate.getRounds()) {
            RoundEntity roundEntity = toRoundEntity(round);
            entity.addRound(roundEntity);
        }
        
        return entity;
    }
    
    public Debate toDomain(DebateEntity entity) {
        // Create config
        Duration roundTimeLimit = entity.getRoundTimeLimitMinutes() != null ?
            Duration.ofMinutes(entity.getRoundTimeLimitMinutes()) : null;
        Duration maxDebateDuration = entity.getMaxDebateDurationHours() != null ?
            Duration.ofHours(entity.getMaxDebateDurationHours()) : null;
        
        DebateConfig config = new DebateConfig(
            entity.getMinParticipants(),
            entity.getMaxParticipants(),
            entity.getMaxRounds(),
            roundTimeLimit,
            maxDebateDuration,
            entity.getRequireBalancedPositions(),
            entity.getAutoAdvanceRounds(),
            entity.getAllowSpectators(),
            entity.getMaxResponseLength(),
            entity.getEnableQualityAssessment()
        );
        
        // Create participants map
        Map<ParticipantId, Participant> participants = entity.getParticipants().stream()
            .map(this::toParticipantDomain)
            .collect(Collectors.toMap(Participant::getId, p -> p));
        
        // Create rounds list
        List<Round> rounds = entity.getRounds().stream()
            .map(this::toRoundDomain)
            .toList();
        
        // Build debate
        return Debate.builder()
            .id(DebateId.from(entity.getId()))
            .topic(entity.getTopic())
            .config(config)
            .createdAt(entity.getCreatedAt())
            .participants(participants)
            .rounds(rounds)
            .status(DebateStatus.fromValue(entity.getStatus()))
            .startedAt(entity.getStartedAt())
            .completedAt(entity.getCompletedAt())
            .result(entity.getResult())
            .build();
    }
    
    private ParticipantEntity toParticipantEntity(Participant participant) {
        ParticipantEntity entity = new ParticipantEntity(
            participant.getId().value(),
            participant.getName(),
            participant.getType().getValue(),
            participant.getPosition().value(),
            participant.getJoinedAt()
        );
        
        entity.setProvider(participant.getProvider() != null ? participant.getProvider().getValue() : null);
        entity.setActive(participant.isActive());
        entity.setResponseCount(participant.getResponseCount());
        
        // Map provider config
        if (participant.getConfig() != null) {
            ProviderConfig config = participant.getConfig();
            entity.setProviderModel(config.model());
            entity.setProviderMaxTokens(config.maxTokens());
            entity.setProviderTemperature(config.temperature());
            entity.setProviderTopP(config.topP());
            entity.setProviderSystemPrompt(config.systemPrompt());
        }
        
        // Map quality metrics
        ArgumentQuality quality = participant.getAverageQuality();
        entity.setAvgLogicalStrength(quality.logicalStrength());
        entity.setAvgEvidenceQuality(quality.evidenceQuality());
        entity.setAvgClarity(quality.clarity());
        entity.setAvgRelevance(quality.relevance());
        entity.setAvgOriginality(quality.originality());
        
        return entity;
    }
    
    private Participant toParticipantDomain(ParticipantEntity entity) {
        ParticipantId participantId = ParticipantId.from(entity.getId());
        ParticipantType type = ParticipantType.fromValue(entity.getType());
        Position position = Position.of(entity.getPosition());
        
        LlmProvider provider = entity.getProvider() != null ? 
            LlmProvider.fromValue(entity.getProvider()) : null;
        
        ProviderConfig config = null;
        if (entity.getProviderModel() != null) {
            config = new ProviderConfig(
                entity.getProviderModel(),
                entity.getProviderMaxTokens(),
                entity.getProviderTemperature(),
                entity.getProviderTopP(),
                entity.getProviderSystemPrompt(),
                Map.of()
            );
        }
        
        ArgumentQuality quality = ArgumentQuality.of(
            entity.getAvgLogicalStrength() != null ? entity.getAvgLogicalStrength().doubleValue() : 0.0,
            entity.getAvgEvidenceQuality() != null ? entity.getAvgEvidenceQuality().doubleValue() : 0.0,
            entity.getAvgClarity() != null ? entity.getAvgClarity().doubleValue() : 0.0,
            entity.getAvgRelevance() != null ? entity.getAvgRelevance().doubleValue() : 0.0,
            entity.getAvgOriginality() != null ? entity.getAvgOriginality().doubleValue() : 0.0
        );
        
        return Participant.builder()
            .id(participantId)
            .type(type)
            .name(entity.getName())
            .position(position)
            .provider(provider)
            .config(config)
            .joinedAt(entity.getJoinedAt())
            .active(entity.getActive())
            .responseCount(entity.getResponseCount())
            .averageQuality(quality)
            .build();
    }
    
    private RoundEntity toRoundEntity(Round round) {
        RoundEntity entity = new RoundEntity(
            round.getId().value(),
            round.getRoundNumber(),
            round.getStatus().getValue(),
            round.getStartedAt()
        );
        
        entity.setCompletedAt(round.getCompletedAt().orElse(null));
        entity.setTimeLimitMinutes(round.hasTimeLimit() ? (int) round.getTimeLimit().toMinutes() : null);
        
        // Map responses
        for (Response response : round.getResponses()) {
            ResponseEntity responseEntity = toResponseEntity(response);
            entity.addResponse(responseEntity);
        }
        
        return entity;
    }
    
    private Round toRoundDomain(RoundEntity entity) {
        Duration timeLimit = entity.getTimeLimitMinutes() != null ?
            Duration.ofMinutes(entity.getTimeLimitMinutes()) : null;
        
        List<Response> responses = entity.getResponses().stream()
            .map(this::toResponseDomain)
            .toList();
        
        return Round.builder()
            .id(RoundId.from(entity.getId()))
            .roundNumber(entity.getRoundNumber())
            .startedAt(entity.getStartedAt())
            .timeLimit(timeLimit)
            .responses(responses)
            .completedAt(entity.getCompletedAt())
            .status(RoundStatus.fromValue(entity.getStatus()))
            .build();
    }
    
    private ResponseEntity toResponseEntity(Response response) {
        ResponseEntity entity = new ResponseEntity(
            response.getId().value(),
            response.getParticipantId().value(),
            response.getPosition().value(),
            response.getContent().value(),
            response.getSubmittedAt()
        );
        
        entity.setResponseTimeSeconds(response.getResponseTime().map(Duration::getSeconds).orElse(null));
        entity.setFlagged(response.isFlagged());
        entity.setFlagReason(response.getFlagReason().orElse(null));
        
        // Map quality metrics
        ArgumentQuality quality = response.getQuality();
        entity.setQualityLogicalStrength(quality.logicalStrength());
        entity.setQualityEvidenceQuality(quality.evidenceQuality());
        entity.setQualityClarity(quality.clarity());
        entity.setQualityRelevance(quality.relevance());
        entity.setQualityOriginality(quality.originality());
        
        return entity;
    }
    
    private Response toResponseDomain(ResponseEntity entity) {
        Duration responseTime = entity.getResponseTimeSeconds() != null ?
            Duration.ofSeconds(entity.getResponseTimeSeconds()) : null;
        
        ArgumentQuality quality = ArgumentQuality.of(
            entity.getQualityLogicalStrength() != null ? entity.getQualityLogicalStrength().doubleValue() : 0.0,
            entity.getQualityEvidenceQuality() != null ? entity.getQualityEvidenceQuality().doubleValue() : 0.0,
            entity.getQualityClarity() != null ? entity.getQualityClarity().doubleValue() : 0.0,
            entity.getQualityRelevance() != null ? entity.getQualityRelevance().doubleValue() : 0.0,
            entity.getQualityOriginality() != null ? entity.getQualityOriginality().doubleValue() : 0.0
        );
        
        return Response.builder()
            .id(ResponseId.from(entity.getId()))
            .participantId(ParticipantId.from(entity.getParticipantId()))
            .position(Position.of(entity.getPosition()))
            .content(ArgumentContent.of(entity.getContent()))
            .submittedAt(entity.getSubmittedAt())
            .responseTime(responseTime)
            .quality(quality)
            .flagged(entity.getFlagged())
            .flagReason(entity.getFlagReason())
            .build();
    }
}