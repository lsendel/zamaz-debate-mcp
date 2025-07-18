package com.zamaz.mcp.debateengine.adapter.web;

import com.zamaz.mcp.debateengine.adapter.web.dto.*;
import com.zamaz.mcp.debateengine.application.command.CreateDebateCommand;
import com.zamaz.mcp.debateengine.domain.model.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mapper for converting between domain models and DTOs.
 */
@Component
public class DebateMapper {
    
    /**
     * Convert create request to command.
     */
    public CreateDebateCommand toCreateCommand(
            OrganizationId organizationId,
            UUID userId,
            CreateDebateRequest request) {
        
        // Map visibility
        DebateConfiguration.Visibility visibility = switch (request.visibility()) {
            case "PUBLIC" -> DebateConfiguration.Visibility.PUBLIC;
            case "PRIVATE" -> DebateConfiguration.Visibility.PRIVATE;
            case "ORGANIZATION" -> DebateConfiguration.Visibility.ORGANIZATION;
            default -> DebateConfiguration.Visibility.PRIVATE;
        };
        
        // Create configuration
        DebateConfiguration config = DebateConfiguration.of(
            request.maxParticipants() != null ? request.maxParticipants() : 2,
            request.maxRounds() != null ? request.maxRounds() : 5,
            Duration.ofMillis(request.roundTimeoutMs() != null ? request.roundTimeoutMs() : 300000),
            visibility,
            request.settings() != null ? request.settings() : Map.of()
        );
        
        return new CreateDebateCommand(
            organizationId,
            userId,
            request.topic(),
            request.description(),
            config
        );
    }
    
    /**
     * Convert domain debate to response DTO.
     */
    public DebateResponse toResponse(Debate debate) {
        return new DebateResponse(
            debate.getId().toString(),
            debate.getOrganizationId().toString(),
            debate.getCreatedByUserId().toString(),
            debate.getTopic().toString(),
            debate.getDescription(),
            debate.getStatus().name().toLowerCase(),
            toConfigurationDto(debate.getConfiguration()),
            debate.getCurrentRoundNumber(),
            debate.getContextId() != null ? debate.getContextId().toString() : null,
            toParticipantDtos(debate.getParticipants()),
            toRoundDtos(debate.getRounds()),
            debate.getStartedAt(),
            debate.getCompletedAt(),
            debate.getCreatedAt(),
            debate.getUpdatedAt()
        );
    }
    
    /**
     * Convert configuration to DTO.
     */
    private DebateResponse.ConfigurationDto toConfigurationDto(DebateConfiguration config) {
        return new DebateResponse.ConfigurationDto(
            config.maxParticipants(),
            config.maxRounds(),
            config.roundTimeout().toMillis(),
            config.visibility().name(),
            config.settings()
        );
    }
    
    /**
     * Convert participants to DTOs.
     */
    private List<DebateResponse.ParticipantDto> toParticipantDtos(List<Participant> participants) {
        return participants.stream()
            .map(this::toParticipantDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Convert participant to DTO.
     */
    private DebateResponse.ParticipantDto toParticipantDto(Participant participant) {
        return new DebateResponse.ParticipantDto(
            participant.getId().toString(),
            participant.getType().name(),
            participant.getPosition().name(),
            participant.getUserId() != null ? participant.getUserId().toString() : null,
            participant.getAiModel() != null ? toAIModelDto(participant.getAiModel()) : null,
            participant.getJoinedAt(),
            participant.getLeftAt(),
            participant.getTotalResponses(),
            participant.getAverageResponseTimeMs()
        );
    }
    
    /**
     * Convert AI model to DTO.
     */
    private DebateResponse.AIModelDto toAIModelDto(AIModel model) {
        return new DebateResponse.AIModelDto(
            model.provider(),
            model.name(),
            model.config()
        );
    }
    
    /**
     * Convert rounds to DTOs.
     */
    private List<DebateResponse.RoundDto> toRoundDtos(List<Round> rounds) {
        return rounds.stream()
            .map(this::toRoundDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Convert round to DTO.
     */
    private DebateResponse.RoundDto toRoundDto(Round round) {
        return new DebateResponse.RoundDto(
            round.getId().toString(),
            round.getRoundNumber(),
            round.getStatus().name().toLowerCase(),
            round.getTimeout().toMillis(),
            round.getPromptTemplate(),
            round.getStartedAt(),
            round.getCompletedAt(),
            toResponseDtos(round.getResponses())
        );
    }
    
    /**
     * Convert responses to DTOs.
     */
    private List<DebateResponse.ResponseDto> toResponseDtos(List<Response> responses) {
        return responses.stream()
            .map(this::toResponseDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Convert response to DTO.
     */
    private DebateResponse.ResponseDto toResponseDto(Response response) {
        return new DebateResponse.ResponseDto(
            response.getId().toString(),
            response.getParticipantId().toString(),
            response.getContent().toString(),
            response.getResponseOrder(),
            response.getResponseTimeMs(),
            response.getTokenCount(),
            response.getQualityScore() != null ? toQualityScoreDto(response.getQualityScore()) : null,
            response.getCreatedAt()
        );
    }
    
    /**
     * Convert quality score to DTO.
     */
    private DebateResponse.QualityScoreDto toQualityScoreDto(QualityScore score) {
        return new DebateResponse.QualityScoreDto(
            score.overall(),
            score.sentiment(),
            score.coherence(),
            score.factuality()
        );
    }
}