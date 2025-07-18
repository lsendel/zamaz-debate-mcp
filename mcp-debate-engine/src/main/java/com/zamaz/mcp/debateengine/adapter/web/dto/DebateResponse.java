package com.zamaz.mcp.debateengine.adapter.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for debate details.
 */
public record DebateResponse(
    String id,
    String organizationId,
    String createdByUserId,
    String topic,
    String description,
    String status,
    ConfigurationDto configuration,
    int currentRound,
    String contextId,
    List<ParticipantDto> participants,
    List<RoundDto> rounds,
    LocalDateTime startedAt,
    LocalDateTime completedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    
    /**
     * Configuration DTO.
     */
    public record ConfigurationDto(
        int maxParticipants,
        int maxRounds,
        long roundTimeoutMs,
        String visibility,
        Map<String, Object> settings
    ) {}
    
    /**
     * Participant DTO.
     */
    public record ParticipantDto(
        String id,
        String type,
        String position,
        String userId,
        AIModelDto aiModel,
        LocalDateTime joinedAt,
        LocalDateTime leftAt,
        int totalResponses,
        long averageResponseTimeMs
    ) {}
    
    /**
     * AI Model DTO.
     */
    public record AIModelDto(
        String provider,
        String name,
        Map<String, Object> config
    ) {}
    
    /**
     * Round DTO.
     */
    public record RoundDto(
        String id,
        int roundNumber,
        String status,
        long timeoutMs,
        String promptTemplate,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        List<ResponseDto> responses
    ) {}
    
    /**
     * Response DTO.
     */
    public record ResponseDto(
        String id,
        String participantId,
        String content,
        int responseOrder,
        long responseTimeMs,
        int tokenCount,
        QualityScoreDto qualityScore,
        LocalDateTime createdAt
    ) {}
    
    /**
     * Quality score DTO.
     */
    public record QualityScoreDto(
        double overall,
        double sentiment,
        double coherence,
        double factuality
    ) {}
}