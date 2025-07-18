package com.zamaz.mcp.controller.adapter.web;

import com.zamaz.mcp.controller.adapter.web.dto.*;
import com.zamaz.mcp.controller.application.command.CreateDebateCommand;
import com.zamaz.mcp.controller.application.command.JoinDebateCommand;
import com.zamaz.mcp.controller.application.command.SubmitResponseCommand;
import com.zamaz.mcp.controller.domain.model.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper between domain objects and DTOs.
 */
@Component
public class DebateMapper {
    
    public CreateDebateCommand toCommand(CreateDebateRequest request) {
        DebateConfig config = toDebateConfig(request.config());
        return CreateDebateCommand.of(request.topic(), config);
    }
    
    public JoinDebateCommand toCommand(DebateId debateId, JoinDebateRequest request) {
        ParticipantType type = ParticipantType.fromValue(request.participantType());
        Position position = Position.of(request.position());
        
        if (request.isHuman()) {
            return JoinDebateCommand.human(debateId, request.participantName(), position);
        } else {
            LlmProvider provider = LlmProvider.fromValue(request.provider());
            ProviderConfig config = toProviderConfig(request.providerConfig());
            return JoinDebateCommand.ai(debateId, request.participantName(), position, provider, config);
        }
    }
    
    public SubmitResponseCommand toCommand(DebateId debateId, SubmitResponseRequest request) {
        ParticipantId participantId = ParticipantId.from(request.participantId());
        ArgumentContent content = ArgumentContent.of(request.content());
        Duration responseTime = request.hasResponseTime() ? 
            Duration.ofSeconds(request.responseTimeSeconds()) : null;
        
        return SubmitResponseCommand.of(debateId, participantId, content, responseTime);
    }
    
    public DebateResponse toResponse(Debate debate) {
        return new DebateResponse(
            debate.getId().toString(),
            debate.getTopic(),
            debate.getStatus().getValue(),
            toDebateConfigDto(debate.getConfig()),
            debate.getCreatedAt(),
            debate.getStartedAt().orElse(null),
            debate.getCompletedAt().orElse(null),
            debate.getResult().orElse(null),
            debate.getCurrentRoundNumber(),
            debate.getTotalRounds(),
            toParticipantResponses(debate.getParticipants()),
            toRoundResponses(debate.getRounds()),
            toStatsResponse(debate)
        );
    }
    
    private DebateConfig toDebateConfig(DebateConfigDto dto) {
        Duration roundTimeLimit = dto.roundTimeLimitMinutes() != null ?
            Duration.ofMinutes(dto.roundTimeLimitMinutes()) : null;
        Duration maxDebateDuration = dto.maxDebateDurationHours() != null ?
            Duration.ofHours(dto.maxDebateDurationHours()) : null;
        
        return new DebateConfig(
            dto.minParticipants(),
            dto.maxParticipants(),
            dto.maxRounds(),
            roundTimeLimit,
            maxDebateDuration,
            dto.requireBalancedPositions(),
            dto.autoAdvanceRounds(),
            dto.allowSpectators(),
            dto.maxResponseLength(),
            dto.enableQualityAssessment()
        );
    }
    
    private DebateConfigDto toDebateConfigDto(DebateConfig config) {
        Integer roundTimeMinutes = config.hasRoundTimeLimit() ?
            (int) config.roundTimeLimit().toMinutes() : null;
        Integer maxDurationHours = config.hasMaxDebateDuration() ?
            (int) config.maxDebateDuration().toHours() : null;
        
        return new DebateConfigDto(
            config.minParticipants(),
            config.maxParticipants(),
            config.maxRounds(),
            roundTimeMinutes,
            maxDurationHours,
            config.requireBalancedPositions(),
            config.shouldAutoAdvanceRounds(),
            config.allowsSpectators(),
            config.maxResponseLength(),
            config.hasQualityAssessment()
        );
    }
    
    private ProviderConfig toProviderConfig(ProviderConfigDto dto) {
        return new ProviderConfig(
            dto.model(),
            dto.maxTokens(),
            dto.temperature(),
            dto.topP(),
            dto.systemPrompt(),
            dto.additionalParams() != null ? dto.additionalParams() : Map.of()
        );
    }
    
    private List<DebateResponse.ParticipantResponse> toParticipantResponses(Map<ParticipantId, Participant> participants) {
        return participants.values().stream()
            .map(this::toParticipantResponse)
            .toList();
    }
    
    private DebateResponse.ParticipantResponse toParticipantResponse(Participant participant) {
        return new DebateResponse.ParticipantResponse(
            participant.getId().toString(),
            participant.getName(),
            participant.getType().getValue(),
            participant.getPosition().value(),
            participant.getProvider() != null ? participant.getProvider().getValue() : null,
            participant.isActive(),
            participant.getResponseCount(),
            participant.getJoinedAt()
        );
    }
    
    private List<DebateResponse.RoundResponse> toRoundResponses(List<Round> rounds) {
        return rounds.stream()
            .map(this::toRoundResponse)
            .toList();
    }
    
    private DebateResponse.RoundResponse toRoundResponse(Round round) {
        return new DebateResponse.RoundResponse(
            round.getId().toString(),
            round.getRoundNumber(),
            round.getStatus().getValue(),
            round.getStartedAt(),
            round.getCompletedAt().orElse(null),
            round.getResponseCount(),
            toResponseResponses(round.getResponses())
        );
    }
    
    private List<DebateResponse.ResponseResponse> toResponseResponses(List<Response> responses) {
        return responses.stream()
            .map(this::toResponseResponse)
            .toList();
    }
    
    private DebateResponse.ResponseResponse toResponseResponse(Response response) {
        Long responseTimeSeconds = response.getResponseTime()
            .map(Duration::getSeconds)
            .orElse(null);
        
        return new DebateResponse.ResponseResponse(
            response.getId().toString(),
            response.getParticipantId().toString(),
            response.getPosition().value(),
            response.getContent().value(),
            response.getSubmittedAt(),
            responseTimeSeconds,
            response.isFlagged()
        );
    }
    
    private DebateResponse.DebateStatsResponse toStatsResponse(Debate debate) {
        List<Response> allResponses = debate.getAllResponses();
        Map<String, Integer> responsesByPosition = debate.getResponsesByPosition().entrySet().stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey().value(),
                entry -> entry.getValue().size()
            ));
        
        int totalWords = allResponses.stream()
            .mapToInt(Response::getWordCount)
            .sum();
        
        Long totalDurationSeconds = debate.getTotalDuration().getSeconds();
        String averageQuality = String.format("%.2f", debate.getAverageQuality().overallScore());
        
        return new DebateResponse.DebateStatsResponse(
            debate.getParticipants().size(),
            allResponses.size(),
            totalWords,
            responsesByPosition,
            averageQuality,
            totalDurationSeconds
        );
    }
}