package com.zamaz.mcp.controller.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for debate configuration.
 */
@Schema(description = "Configuration parameters for a debate")
public record DebateConfigDto(
    
    @Schema(description = "Minimum number of participants", example = "2")
    @NotNull(message = "Min participants cannot be null")
    @Min(value = 2, message = "Min participants must be at least 2")
    @Max(value = 20, message = "Min participants cannot exceed 20")
    Integer minParticipants,
    
    @Schema(description = "Maximum number of participants", example = "6")
    @NotNull(message = "Max participants cannot be null")
    @Min(value = 2, message = "Max participants must be at least 2")
    @Max(value = 20, message = "Max participants cannot exceed 20")
    Integer maxParticipants,
    
    @Schema(description = "Maximum number of rounds", example = "5")
    @NotNull(message = "Max rounds cannot be null")
    @Min(value = 1, message = "Max rounds must be at least 1")
    @Max(value = 50, message = "Max rounds cannot exceed 50")
    Integer maxRounds,
    
    @Schema(description = "Time limit per round in minutes (null for no limit)", example = "10")
    @Min(value = 1, message = "Round time limit must be at least 1 minute")
    @Max(value = 1440, message = "Round time limit cannot exceed 24 hours")
    Integer roundTimeLimitMinutes,
    
    @Schema(description = "Maximum debate duration in hours (null for no limit)", example = "2")
    @Min(value = 1, message = "Max debate duration must be at least 1 hour")
    @Max(value = 168, message = "Max debate duration cannot exceed 1 week")
    Integer maxDebateDurationHours,
    
    @Schema(description = "Whether positions must be balanced", example = "true")
    @NotNull(message = "Require balanced positions cannot be null")
    Boolean requireBalancedPositions,
    
    @Schema(description = "Whether rounds advance automatically", example = "false")
    @NotNull(message = "Auto advance rounds cannot be null")
    Boolean autoAdvanceRounds,
    
    @Schema(description = "Whether spectators are allowed", example = "true")
    @NotNull(message = "Allow spectators cannot be null")
    Boolean allowSpectators,
    
    @Schema(description = "Maximum response length in characters", example = "5000")
    @NotNull(message = "Max response length cannot be null")
    @Min(value = 10, message = "Max response length must be at least 10")
    @Max(value = 100000, message = "Max response length cannot exceed 100,000")
    Integer maxResponseLength,
    
    @Schema(description = "Whether quality assessment is enabled", example = "true")
    @NotNull(message = "Enable quality assessment cannot be null")
    Boolean enableQualityAssessment
) {
    
    public static DebateConfigDto defaultConfig() {
        return new DebateConfigDto(
            2,      // minParticipants
            6,      // maxParticipants
            5,      // maxRounds
            10,     // roundTimeLimitMinutes
            2,      // maxDebateDurationHours
            true,   // requireBalancedPositions
            false,  // autoAdvanceRounds
            true,   // allowSpectators
            5000,   // maxResponseLength
            true    // enableQualityAssessment
        );
    }
    
    public static DebateConfigDto quickDebate() {
        return new DebateConfigDto(
            2,      // minParticipants
            4,      // maxParticipants
            3,      // maxRounds
            5,      // roundTimeLimitMinutes
            null,   // maxDebateDurationHours (30 minutes total)
            true,   // requireBalancedPositions
            true,   // autoAdvanceRounds
            false,  // allowSpectators
            1000,   // maxResponseLength
            false   // enableQualityAssessment
        );
    }
    
    public static DebateConfigDto aiOnlyDebate() {
        return new DebateConfigDto(
            2,      // minParticipants
            4,      // maxParticipants
            8,      // maxRounds
            2,      // roundTimeLimitMinutes
            null,   // maxDebateDurationHours (30 minutes total)
            true,   // requireBalancedPositions
            true,   // autoAdvanceRounds
            false,  // allowSpectators
            3000,   // maxResponseLength
            true    // enableQualityAssessment
        );
    }
}