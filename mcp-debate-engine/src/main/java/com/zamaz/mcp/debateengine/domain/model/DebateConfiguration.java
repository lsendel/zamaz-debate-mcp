package com.zamaz.mcp.debateengine.domain.model;

import com.zamaz.mcp.common.domain.ValueObject;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Value object representing debate configuration.
 */
public record DebateConfiguration(
    int maxParticipants,
    int maxRounds,
    Duration roundTimeout,
    Visibility visibility,
    Map<String, Object> settings
) implements ValueObject {
    
    public DebateConfiguration {
        if (maxParticipants < 2) {
            throw new IllegalArgumentException("Debate must have at least 2 participants");
        }
        if (maxParticipants > 10) {
            throw new IllegalArgumentException("Debate cannot have more than 10 participants");
        }
        if (maxRounds < 1) {
            throw new IllegalArgumentException("Debate must have at least 1 round");
        }
        if (maxRounds > 20) {
            throw new IllegalArgumentException("Debate cannot have more than 20 rounds");
        }
        Objects.requireNonNull(roundTimeout, "Round timeout cannot be null");
        if (roundTimeout.isNegative() || roundTimeout.isZero()) {
            throw new IllegalArgumentException("Round timeout must be positive");
        }
        if (roundTimeout.toMinutes() > 60) {
            throw new IllegalArgumentException("Round timeout cannot exceed 60 minutes");
        }
        Objects.requireNonNull(visibility, "Visibility cannot be null");
        Objects.requireNonNull(settings, "Settings cannot be null");
        settings = Map.copyOf(settings); // Make immutable
    }
    
    /**
     * Create default configuration.
     */
    public static DebateConfiguration defaults() {
        return new DebateConfiguration(
            2,
            5,
            Duration.ofMinutes(5),
            Visibility.PRIVATE,
            Map.of()
        );
    }
    
    /**
     * Create custom configuration.
     */
    public static DebateConfiguration of(
            int maxParticipants,
            int maxRounds,
            Duration roundTimeout,
            Visibility visibility,
            Map<String, Object> settings) {
        return new DebateConfiguration(
            maxParticipants,
            maxRounds,
            roundTimeout,
            visibility,
            settings
        );
    }
    
    /**
     * Update configuration with new values.
     */
    public DebateConfiguration withSettings(Map<String, Object> newSettings) {
        return new DebateConfiguration(
            maxParticipants,
            maxRounds,
            roundTimeout,
            visibility,
            newSettings
        );
    }
    
    /**
     * Debate visibility options.
     */
    public enum Visibility {
        PUBLIC("Public - visible to everyone"),
        PRIVATE("Private - visible only to participants"),
        ORGANIZATION("Organization - visible to organization members");
        
        private final String description;
        
        Visibility(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}