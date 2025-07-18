package com.zamaz.mcp.controller.domain.model;

import com.zamaz.mcp.common.domain.model.Entity;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Entity representing a participant's response in a debate round.
 */
public class Response implements Entity<ResponseId> {
    
    private final ResponseId id;
    private final ParticipantId participantId;
    private final Position position;
    private final ArgumentContent content;
    private final Instant submittedAt;
    private final Duration responseTime;
    private ArgumentQuality quality;
    private boolean flagged;
    private String flagReason;
    
    private Response(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "Response ID cannot be null");
        this.participantId = Objects.requireNonNull(builder.participantId, "Participant ID cannot be null");
        this.position = Objects.requireNonNull(builder.position, "Position cannot be null");
        this.content = Objects.requireNonNull(builder.content, "Content cannot be null");
        this.submittedAt = Objects.requireNonNull(builder.submittedAt, "Submitted timestamp cannot be null");
        this.responseTime = builder.responseTime;
        this.quality = builder.quality != null ? builder.quality : ArgumentQuality.unknown();
        this.flagged = builder.flagged;
        this.flagReason = builder.flagReason;
        
        validateInvariants();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static Response create(ResponseId id, ParticipantId participantId, Position position, 
                                ArgumentContent content, Duration responseTime) {
        return builder()
            .id(id)
            .participantId(participantId)
            .position(position)
            .content(content)
            .submittedAt(Instant.now())
            .responseTime(responseTime)
            .build();
    }
    
    @Override
    public ResponseId getId() {
        return id;
    }
    
    public ParticipantId getParticipantId() {
        return participantId;
    }
    
    public Position getPosition() {
        return position;
    }
    
    public ArgumentContent getContent() {
        return content;
    }
    
    public Instant getSubmittedAt() {
        return submittedAt;
    }
    
    public Optional<Duration> getResponseTime() {
        return Optional.ofNullable(responseTime);
    }
    
    public ArgumentQuality getQuality() {
        return quality;
    }
    
    public boolean isFlagged() {
        return flagged;
    }
    
    public Optional<String> getFlagReason() {
        return Optional.ofNullable(flagReason);
    }
    
    public boolean hasQualityAssessment() {
        return !quality.equals(ArgumentQuality.unknown());
    }
    
    public boolean isQuickResponse() {
        return responseTime != null && responseTime.toSeconds() < 30;
    }
    
    public boolean isSlowResponse() {
        return responseTime != null && responseTime.toMinutes() > 5;
    }
    
    public boolean isHighQuality() {
        return quality.getCategory() == ArgumentQuality.QualityCategory.EXCELLENT ||
               quality.getCategory() == ArgumentQuality.QualityCategory.GOOD;
    }
    
    public boolean isLowQuality() {
        return quality.getCategory() == ArgumentQuality.QualityCategory.POOR;
    }
    
    public void assessQuality(ArgumentQuality newQuality) {
        Objects.requireNonNull(newQuality, "Quality cannot be null");
        this.quality = newQuality;
    }
    
    public void flag(String reason) {
        Objects.requireNonNull(reason, "Flag reason cannot be null");
        if (reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Flag reason cannot be empty");
        }
        if (reason.length() > 1000) {
            throw new IllegalArgumentException("Flag reason cannot exceed 1000 characters");
        }
        
        this.flagged = true;
        this.flagReason = reason.trim();
    }
    
    public void unflag() {
        this.flagged = false;
        this.flagReason = null;
    }
    
    public Response withQuality(ArgumentQuality newQuality) {
        Objects.requireNonNull(newQuality, "Quality cannot be null");
        return builder()
            .id(this.id)
            .participantId(this.participantId)
            .position(this.position)
            .content(this.content)
            .submittedAt(this.submittedAt)
            .responseTime(this.responseTime)
            .quality(newQuality)
            .flagged(this.flagged)
            .flagReason(this.flagReason)
            .build();
    }
    
    public Response withContent(ArgumentContent newContent) {
        Objects.requireNonNull(newContent, "Content cannot be null");
        return builder()
            .id(this.id)
            .participantId(this.participantId)
            .position(this.position)
            .content(newContent)
            .submittedAt(this.submittedAt)
            .responseTime(this.responseTime)
            .quality(this.quality)
            .flagged(this.flagged)
            .flagReason(this.flagReason)
            .build();
    }
    
    /**
     * Calculate a comprehensive response score based on quality, timing, and other factors.
     */
    public double calculateScore() {
        double qualityScore = quality.overallScore().doubleValue();
        
        // Apply timing bonus/penalty
        double timingMultiplier = 1.0;
        if (responseTime != null) {
            long seconds = responseTime.toSeconds();
            if (seconds < 10) {
                // Very quick responses might be lower quality
                timingMultiplier = 0.9;
            } else if (seconds > 300) {
                // Very slow responses might be more thoughtful
                timingMultiplier = 1.1;
            }
        }
        
        // Apply flagging penalty
        double flagPenalty = flagged ? 0.5 : 1.0;
        
        return qualityScore * timingMultiplier * flagPenalty;
    }
    
    public int getWordCount() {
        return content.wordCount();
    }
    
    public int getCharacterCount() {
        return content.length();
    }
    
    public int getEstimatedReadingTime() {
        return content.estimateReadingTimeSeconds();
    }
    
    private void validateInvariants() {
        if (responseTime != null && responseTime.isNegative()) {
            throw new IllegalArgumentException("Response time cannot be negative");
        }
        
        if (flagged && (flagReason == null || flagReason.trim().isEmpty())) {
            throw new IllegalArgumentException("Flagged responses must have a reason");
        }
        
        if (!flagged && flagReason != null) {
            throw new IllegalArgumentException("Non-flagged responses cannot have a flag reason");
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Response response = (Response) obj;
        return Objects.equals(id, response.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("Response{id=%s, participant=%s, position=%s, words=%d, quality=%.2f%s}",
            id, participantId, position, getWordCount(), quality.overallScore(), 
            flagged ? " [FLAGGED]" : "");
    }
    
    public static class Builder {
        private ResponseId id;
        private ParticipantId participantId;
        private Position position;
        private ArgumentContent content;
        private Instant submittedAt;
        private Duration responseTime;
        private ArgumentQuality quality;
        private boolean flagged = false;
        private String flagReason;
        
        public Builder id(ResponseId id) {
            this.id = id;
            return this;
        }
        
        public Builder participantId(ParticipantId participantId) {
            this.participantId = participantId;
            return this;
        }
        
        public Builder position(Position position) {
            this.position = position;
            return this;
        }
        
        public Builder content(ArgumentContent content) {
            this.content = content;
            return this;
        }
        
        public Builder submittedAt(Instant submittedAt) {
            this.submittedAt = submittedAt;
            return this;
        }
        
        public Builder responseTime(Duration responseTime) {
            this.responseTime = responseTime;
            return this;
        }
        
        public Builder quality(ArgumentQuality quality) {
            this.quality = quality;
            return this;
        }
        
        public Builder flagged(boolean flagged) {
            this.flagged = flagged;
            return this;
        }
        
        public Builder flagReason(String flagReason) {
            this.flagReason = flagReason;
            return this;
        }
        
        public Response build() {
            return new Response(this);
        }
    }
}