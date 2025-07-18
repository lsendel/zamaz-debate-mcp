package com.zamaz.mcp.controller.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for debate information.
 */
@Schema(description = "Debate information response")
public record DebateResponse(
    
    @Schema(description = "Unique debate identifier")
    String id,
    
    @Schema(description = "Debate topic")
    String topic,
    
    @Schema(description = "Current status of the debate")
    String status,
    
    @Schema(description = "Debate configuration")
    DebateConfigDto config,
    
    @Schema(description = "When the debate was created")
    Instant createdAt,
    
    @Schema(description = "When the debate was started (if started)")
    Instant startedAt,
    
    @Schema(description = "When the debate was completed (if completed)")
    Instant completedAt,
    
    @Schema(description = "Debate result (if completed)")
    String result,
    
    @Schema(description = "Current round number")
    Integer currentRound,
    
    @Schema(description = "Total number of rounds")
    Integer totalRounds,
    
    @Schema(description = "List of participants")
    List<ParticipantResponse> participants,
    
    @Schema(description = "List of rounds")
    List<RoundResponse> rounds,
    
    @Schema(description = "Debate statistics")
    DebateStatsResponse stats
) {
    
    @Schema(description = "Participant information")
    public record ParticipantResponse(
        String id,
        String name,
        String type,
        String position,
        String provider,
        boolean active,
        Integer responseCount,
        Instant joinedAt
    ) {}
    
    @Schema(description = "Round information")
    public record RoundResponse(
        String id,
        Integer roundNumber,
        String status,
        Instant startedAt,
        Instant completedAt,
        Integer responseCount,
        List<ResponseResponse> responses
    ) {}
    
    @Schema(description = "Response information")
    public record ResponseResponse(
        String id,
        String participantId,
        String position,
        String content,
        Instant submittedAt,
        Long responseTimeSeconds,
        boolean flagged
    ) {}
    
    @Schema(description = "Debate statistics")
    public record DebateStatsResponse(
        Integer totalParticipants,
        Integer totalResponses,
        Integer totalWords,
        Map<String, Integer> responsesByPosition,
        String averageQuality,
        Long totalDurationSeconds
    ) {}
}