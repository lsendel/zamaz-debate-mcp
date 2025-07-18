package com.zamaz.mcp.controller.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.time.Duration;
import java.util.Objects;

/**
 * Value object representing configuration for a debate.
 */
public record DebateConfig(
    int minParticipants,
    int maxParticipants,
    int maxRounds,
    Duration roundTimeLimit,
    Duration maxDebateDuration,
    boolean requireBalancedPositions,
    boolean autoAdvanceRounds,
    boolean allowSpectators,
    int maxResponseLength,
    boolean enableQualityAssessment
) implements ValueObject {
    
    private static final int MIN_PARTICIPANTS = 2;
    private static final int MAX_PARTICIPANTS = 20;
    private static final int MIN_ROUNDS = 1;
    private static final int MAX_ROUNDS = 50;
    private static final Duration MIN_ROUND_TIME = Duration.ofMinutes(1);
    private static final Duration MAX_ROUND_TIME = Duration.ofHours(24);
    private static final Duration MIN_DEBATE_DURATION = Duration.ofMinutes(5);
    private static final Duration MAX_DEBATE_DURATION = Duration.ofDays(7);
    private static final int MIN_RESPONSE_LENGTH = 10;
    private static final int MAX_RESPONSE_LENGTH = 100_000;
    
    public DebateConfig {
        if (minParticipants < MIN_PARTICIPANTS || minParticipants > MAX_PARTICIPANTS) {
            throw new IllegalArgumentException(
                "Min participants must be between " + MIN_PARTICIPANTS + " and " + MAX_PARTICIPANTS
            );
        }
        
        if (maxParticipants < MIN_PARTICIPANTS || maxParticipants > MAX_PARTICIPANTS) {
            throw new IllegalArgumentException(
                "Max participants must be between " + MIN_PARTICIPANTS + " and " + MAX_PARTICIPANTS
            );
        }
        
        if (minParticipants > maxParticipants) {
            throw new IllegalArgumentException("Min participants cannot exceed max participants");
        }
        
        if (maxRounds < MIN_ROUNDS || maxRounds > MAX_ROUNDS) {
            throw new IllegalArgumentException(
                "Max rounds must be between " + MIN_ROUNDS + " and " + MAX_ROUNDS
            );
        }
        
        if (roundTimeLimit != null) {
            if (roundTimeLimit.compareTo(MIN_ROUND_TIME) < 0 || roundTimeLimit.compareTo(MAX_ROUND_TIME) > 0) {
                throw new IllegalArgumentException(
                    "Round time limit must be between " + MIN_ROUND_TIME + " and " + MAX_ROUND_TIME
                );
            }
        }
        
        if (maxDebateDuration != null) {
            if (maxDebateDuration.compareTo(MIN_DEBATE_DURATION) < 0 || maxDebateDuration.compareTo(MAX_DEBATE_DURATION) > 0) {
                throw new IllegalArgumentException(
                    "Max debate duration must be between " + MIN_DEBATE_DURATION + " and " + MAX_DEBATE_DURATION
                );
            }
        }
        
        if (maxResponseLength < MIN_RESPONSE_LENGTH || maxResponseLength > MAX_RESPONSE_LENGTH) {
            throw new IllegalArgumentException(
                "Max response length must be between " + MIN_RESPONSE_LENGTH + " and " + MAX_RESPONSE_LENGTH
            );
        }
        
        // Validate time constraints are consistent
        if (roundTimeLimit != null && maxDebateDuration != null) {
            Duration totalRoundTime = roundTimeLimit.multipliedBy(maxRounds);
            if (totalRoundTime.compareTo(maxDebateDuration) > 0) {
                throw new IllegalArgumentException(
                    "Total possible round time (" + totalRoundTime + 
                    ") exceeds max debate duration (" + maxDebateDuration + ")"
                );
            }
        }
    }
    
    public static DebateConfig defaultConfig() {
        return new DebateConfig(
            2,                              // minParticipants
            6,                              // maxParticipants
            5,                              // maxRounds
            Duration.ofMinutes(10),         // roundTimeLimit
            Duration.ofHours(2),            // maxDebateDuration
            true,                           // requireBalancedPositions
            false,                          // autoAdvanceRounds
            true,                           // allowSpectators
            5000,                           // maxResponseLength
            true                            // enableQualityAssessment
        );
    }
    
    public static DebateConfig quickDebate() {
        return new DebateConfig(
            2,                              // minParticipants
            4,                              // maxParticipants
            3,                              // maxRounds
            Duration.ofMinutes(5),          // roundTimeLimit
            Duration.ofMinutes(30),         // maxDebateDuration
            true,                           // requireBalancedPositions
            true,                           // autoAdvanceRounds
            false,                          // allowSpectators
            1000,                           // maxResponseLength
            false                           // enableQualityAssessment
        );
    }
    
    public static DebateConfig longFormDebate() {
        return new DebateConfig(
            2,                              // minParticipants
            8,                              // maxParticipants
            10,                             // maxRounds
            Duration.ofHours(1),            // roundTimeLimit
            Duration.ofDays(1),             // maxDebateDuration
            true,                           // requireBalancedPositions
            false,                          // autoAdvanceRounds
            true,                           // allowSpectators
            20000,                          // maxResponseLength
            true                            // enableQualityAssessment
        );
    }
    
    public static DebateConfig casualDebate() {
        return new DebateConfig(
            2,                              // minParticipants
            10,                             // maxParticipants
            7,                              // maxRounds
            Duration.ofMinutes(15),         // roundTimeLimit
            Duration.ofHours(4),            // maxDebateDuration
            false,                          // requireBalancedPositions
            false,                          // autoAdvanceRounds
            true,                           // allowSpectators
            8000,                           // maxResponseLength
            true                            // enableQualityAssessment
        );
    }
    
    public static DebateConfig aiOnlyDebate() {
        return new DebateConfig(
            2,                              // minParticipants
            4,                              // maxParticipants
            8,                              // maxRounds
            Duration.ofMinutes(2),          // roundTimeLimit - shorter for AI
            Duration.ofMinutes(30),         // maxDebateDuration
            true,                           // requireBalancedPositions
            true,                           // autoAdvanceRounds - AI can respond quickly
            false,                          // allowSpectators
            3000,                           // maxResponseLength
            true                            // enableQualityAssessment
        );
    }
    
    public boolean hasRoundTimeLimit() {
        return roundTimeLimit != null && !roundTimeLimit.isZero();
    }
    
    public boolean hasMaxDebateDuration() {
        return maxDebateDuration != null && !maxDebateDuration.isZero();
    }
    
    public boolean requiresBalancedPositions() {
        return requireBalancedPositions;
    }
    
    public boolean shouldAutoAdvanceRounds() {
        return autoAdvanceRounds;
    }
    
    public boolean allowsSpectators() {
        return allowSpectators;
    }
    
    public boolean hasQualityAssessment() {
        return enableQualityAssessment;
    }
    
    public boolean isQuickDebate() {
        return maxRounds <= 3 && 
               hasRoundTimeLimit() && roundTimeLimit.toMinutes() <= 5 &&
               maxResponseLength <= 1500;
    }
    
    public boolean isLongFormDebate() {
        return maxRounds >= 8 && 
               hasRoundTimeLimit() && roundTimeLimit.toMinutes() >= 30 &&
               maxResponseLength >= 10000;
    }
    
    public boolean isCasualDebate() {
        return !requireBalancedPositions && allowSpectators && maxParticipants >= 8;
    }
    
    public boolean isOptimizedForAI() {
        return autoAdvanceRounds && 
               hasRoundTimeLimit() && roundTimeLimit.toMinutes() <= 5 &&
               maxParticipants <= 6;
    }
    
    public DebateConfig withMinParticipants(int newMinParticipants) {
        return new DebateConfig(
            newMinParticipants, maxParticipants, maxRounds, roundTimeLimit, maxDebateDuration,
            requireBalancedPositions, autoAdvanceRounds, allowSpectators, maxResponseLength, enableQualityAssessment
        );
    }
    
    public DebateConfig withMaxParticipants(int newMaxParticipants) {
        return new DebateConfig(
            minParticipants, newMaxParticipants, maxRounds, roundTimeLimit, maxDebateDuration,
            requireBalancedPositions, autoAdvanceRounds, allowSpectators, maxResponseLength, enableQualityAssessment
        );
    }
    
    public DebateConfig withMaxRounds(int newMaxRounds) {
        return new DebateConfig(
            minParticipants, maxParticipants, newMaxRounds, roundTimeLimit, maxDebateDuration,
            requireBalancedPositions, autoAdvanceRounds, allowSpectators, maxResponseLength, enableQualityAssessment
        );
    }
    
    public DebateConfig withRoundTimeLimit(Duration newRoundTimeLimit) {
        return new DebateConfig(
            minParticipants, maxParticipants, maxRounds, newRoundTimeLimit, maxDebateDuration,
            requireBalancedPositions, autoAdvanceRounds, allowSpectators, maxResponseLength, enableQualityAssessment
        );
    }
    
    public DebateConfig withMaxDebateDuration(Duration newMaxDebateDuration) {
        return new DebateConfig(
            minParticipants, maxParticipants, maxRounds, roundTimeLimit, newMaxDebateDuration,
            requireBalancedPositions, autoAdvanceRounds, allowSpectators, maxResponseLength, enableQualityAssessment
        );
    }
    
    public DebateConfig withBalancedPositions(boolean newRequireBalancedPositions) {
        return new DebateConfig(
            minParticipants, maxParticipants, maxRounds, roundTimeLimit, maxDebateDuration,
            newRequireBalancedPositions, autoAdvanceRounds, allowSpectators, maxResponseLength, enableQualityAssessment
        );
    }
    
    public DebateConfig withAutoAdvanceRounds(boolean newAutoAdvanceRounds) {
        return new DebateConfig(
            minParticipants, maxParticipants, maxRounds, roundTimeLimit, maxDebateDuration,
            requireBalancedPositions, newAutoAdvanceRounds, allowSpectators, maxResponseLength, enableQualityAssessment
        );
    }
    
    public DebateConfig withSpectators(boolean newAllowSpectators) {
        return new DebateConfig(
            minParticipants, maxParticipants, maxRounds, roundTimeLimit, maxDebateDuration,
            requireBalancedPositions, autoAdvanceRounds, newAllowSpectators, maxResponseLength, enableQualityAssessment
        );
    }
    
    public DebateConfig withMaxResponseLength(int newMaxResponseLength) {
        return new DebateConfig(
            minParticipants, maxParticipants, maxRounds, roundTimeLimit, maxDebateDuration,
            requireBalancedPositions, autoAdvanceRounds, allowSpectators, newMaxResponseLength, enableQualityAssessment
        );
    }
    
    public DebateConfig withQualityAssessment(boolean newEnableQualityAssessment) {
        return new DebateConfig(
            minParticipants, maxParticipants, maxRounds, roundTimeLimit, maxDebateDuration,
            requireBalancedPositions, autoAdvanceRounds, allowSpectators, maxResponseLength, newEnableQualityAssessment
        );
    }
    
    /**
     * Calculate estimated total debate time based on configuration.
     */
    public Duration getEstimatedTotalTime() {
        if (!hasRoundTimeLimit()) {
            return maxDebateDuration != null ? maxDebateDuration : Duration.ofHours(1);
        }
        
        // Estimate: round time * max rounds + buffer time for transitions
        Duration roundsTime = roundTimeLimit.multipliedBy(maxRounds);
        Duration bufferTime = Duration.ofMinutes(maxRounds * 2); // 2 minutes buffer per round
        Duration totalEstimate = roundsTime.plus(bufferTime);
        
        // Cap by max debate duration if set
        if (hasMaxDebateDuration() && totalEstimate.compareTo(maxDebateDuration) > 0) {
            return maxDebateDuration;
        }
        
        return totalEstimate;
    }
    
    /**
     * Get participant capacity utilization as percentage.
     */
    public double getParticipantCapacityUtilization(int currentParticipants) {
        if (maxParticipants == 0) return 0.0;
        return (double) currentParticipants / maxParticipants * 100.0;
    }
    
    @Override
    public String toString() {
        return String.format(
            "DebateConfig{participants=%d-%d, rounds=%d, roundTime=%s, balanced=%s, auto=%s}",
            minParticipants, maxParticipants, maxRounds, 
            hasRoundTimeLimit() ? roundTimeLimit : "unlimited",
            requireBalancedPositions, autoAdvanceRounds
        );
    }
}