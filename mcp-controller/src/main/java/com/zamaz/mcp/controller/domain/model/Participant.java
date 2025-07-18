package com.zamaz.mcp.controller.domain.model;

import com.zamaz.mcp.common.domain.model.Entity;
import java.time.Instant;
import java.util.Objects;

/**
 * Entity representing a participant in a debate.
 */
public class Participant implements Entity<ParticipantId> {
    
    private final ParticipantId id;
    private final ParticipantType type;
    private final String name;
    private final Position position;
    private final LlmProvider provider;
    private final ProviderConfig config;
    private final Instant joinedAt;
    private boolean active;
    private int responseCount;
    private ArgumentQuality averageQuality;
    
    private Participant(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "Participant ID cannot be null");
        this.type = Objects.requireNonNull(builder.type, "Participant type cannot be null");
        this.name = Objects.requireNonNull(builder.name, "Participant name cannot be null");
        this.position = Objects.requireNonNull(builder.position, "Participant position cannot be null");
        this.provider = builder.provider;
        this.config = builder.config;
        this.joinedAt = Objects.requireNonNull(builder.joinedAt, "Joined timestamp cannot be null");
        this.active = builder.active;
        this.responseCount = Math.max(0, builder.responseCount);
        this.averageQuality = builder.averageQuality != null ? builder.averageQuality : ArgumentQuality.unknown();
        
        validateInvariants();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static Participant createHuman(ParticipantId id, String name, Position position) {
        return builder()
            .id(id)
            .type(ParticipantType.HUMAN)
            .name(name)
            .position(position)
            .joinedAt(Instant.now())
            .active(true)
            .build();
    }
    
    public static Participant createAI(ParticipantId id, String name, Position position, 
                                     LlmProvider provider, ProviderConfig config) {
        return builder()
            .id(id)
            .type(ParticipantType.AI)
            .name(name)
            .position(position)
            .provider(provider)
            .config(config)
            .joinedAt(Instant.now())
            .active(true)
            .build();
    }
    
    @Override
    public ParticipantId getId() {
        return id;
    }
    
    public ParticipantType getType() {
        return type;
    }
    
    public String getName() {
        return name;
    }
    
    public Position getPosition() {
        return position;
    }
    
    public LlmProvider getProvider() {
        return provider;
    }
    
    public ProviderConfig getConfig() {
        return config;
    }
    
    public Instant getJoinedAt() {
        return joinedAt;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public int getResponseCount() {
        return responseCount;
    }
    
    public ArgumentQuality getAverageQuality() {
        return averageQuality;
    }
    
    public boolean isHuman() {
        return type == ParticipantType.HUMAN;
    }
    
    public boolean isAI() {
        return type == ParticipantType.AI;
    }
    
    public void activate() {
        this.active = true;
    }
    
    public void deactivate() {
        this.active = false;
    }
    
    public void recordResponse(ArgumentQuality quality) {
        Objects.requireNonNull(quality, "Argument quality cannot be null");
        
        this.responseCount++;
        
        // Calculate running average of quality
        if (this.averageQuality.equals(ArgumentQuality.unknown())) {
            this.averageQuality = quality;
        } else {
            // Weighted average with more recent responses having slightly more weight
            double oldWeight = Math.min(0.8, responseCount / (responseCount + 1.0));
            double newWeight = 1.0 - oldWeight;
            
            this.averageQuality = ArgumentQuality.of(
                this.averageQuality.logicalStrength().doubleValue() * oldWeight + 
                    quality.logicalStrength().doubleValue() * newWeight,
                this.averageQuality.evidenceQuality().doubleValue() * oldWeight + 
                    quality.evidenceQuality().doubleValue() * newWeight,
                this.averageQuality.clarity().doubleValue() * oldWeight + 
                    quality.clarity().doubleValue() * newWeight,
                this.averageQuality.relevance().doubleValue() * oldWeight + 
                    quality.relevance().doubleValue() * newWeight,
                this.averageQuality.originality().doubleValue() * oldWeight + 
                    quality.originality().doubleValue() * newWeight
            );
        }
    }
    
    public boolean canParticipate() {
        return active && (type == ParticipantType.HUMAN || (provider != null && config != null));
    }
    
    public boolean hasOppositePosition(Participant other) {
        Objects.requireNonNull(other, "Other participant cannot be null");
        return this.position.equals(other.position.opposite());
    }
    
    public Participant withPosition(Position newPosition) {
        Objects.requireNonNull(newPosition, "New position cannot be null");
        return builder()
            .id(this.id)
            .type(this.type)
            .name(this.name)
            .position(newPosition)
            .provider(this.provider)
            .config(this.config)
            .joinedAt(this.joinedAt)
            .active(this.active)
            .responseCount(this.responseCount)
            .averageQuality(this.averageQuality)
            .build();
    }
    
    public Participant withProvider(LlmProvider newProvider, ProviderConfig newConfig) {
        if (type != ParticipantType.AI) {
            throw new IllegalStateException("Cannot set provider for non-AI participant");
        }
        
        return builder()
            .id(this.id)
            .type(this.type)
            .name(this.name)
            .position(this.position)
            .provider(newProvider)
            .config(newConfig)
            .joinedAt(this.joinedAt)
            .active(this.active)
            .responseCount(this.responseCount)
            .averageQuality(this.averageQuality)
            .build();
    }
    
    private void validateInvariants() {
        if (type == ParticipantType.AI) {
            if (provider == null) {
                throw new IllegalArgumentException("AI participants must have a provider");
            }
            if (config == null) {
                throw new IllegalArgumentException("AI participants must have provider config");
            }
        }
        
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Participant name cannot be empty");
        }
        
        if (name.length() > 255) {
            throw new IllegalArgumentException("Participant name cannot exceed 255 characters");
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Participant that = (Participant) obj;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("Participant{id=%s, name='%s', type=%s, position=%s, active=%s, responses=%d}", 
            id, name, type, position, active, responseCount);
    }
    
    public static class Builder {
        private ParticipantId id;
        private ParticipantType type;
        private String name;
        private Position position;
        private LlmProvider provider;
        private ProviderConfig config;
        private Instant joinedAt;
        private boolean active = true;
        private int responseCount = 0;
        private ArgumentQuality averageQuality;
        
        public Builder id(ParticipantId id) {
            this.id = id;
            return this;
        }
        
        public Builder type(ParticipantType type) {
            this.type = type;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder position(Position position) {
            this.position = position;
            return this;
        }
        
        public Builder provider(LlmProvider provider) {
            this.provider = provider;
            return this;
        }
        
        public Builder config(ProviderConfig config) {
            this.config = config;
            return this;
        }
        
        public Builder joinedAt(Instant joinedAt) {
            this.joinedAt = joinedAt;
            return this;
        }
        
        public Builder active(boolean active) {
            this.active = active;
            return this;
        }
        
        public Builder responseCount(int responseCount) {
            this.responseCount = responseCount;
            return this;
        }
        
        public Builder averageQuality(ArgumentQuality averageQuality) {
            this.averageQuality = averageQuality;
            return this;
        }
        
        public Participant build() {
            return new Participant(this);
        }
    }
}