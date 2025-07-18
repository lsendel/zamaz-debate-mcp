package com.zamaz.mcp.controller.application.command;

import com.zamaz.mcp.controller.domain.model.ArgumentContent;
import com.zamaz.mcp.controller.domain.model.DebateId;
import com.zamaz.mcp.controller.domain.model.ParticipantId;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Command to submit a response to the current round of a debate.
 */
public record SubmitResponseCommand(
    DebateId debateId,
    ParticipantId participantId,
    ArgumentContent content,
    Duration responseTime
) {
    
    public SubmitResponseCommand {
        Objects.requireNonNull(debateId, "Debate ID cannot be null");
        Objects.requireNonNull(participantId, "Participant ID cannot be null");
        Objects.requireNonNull(content, "Content cannot be null");
        
        if (responseTime != null && responseTime.isNegative()) {
            throw new IllegalArgumentException("Response time cannot be negative");
        }
    }
    
    public static SubmitResponseCommand of(DebateId debateId, ParticipantId participantId, 
                                         ArgumentContent content, Duration responseTime) {
        return new SubmitResponseCommand(debateId, participantId, content, responseTime);
    }
    
    public static SubmitResponseCommand of(DebateId debateId, ParticipantId participantId, 
                                         String content, Duration responseTime) {
        return new SubmitResponseCommand(
            debateId, 
            participantId, 
            ArgumentContent.of(content), 
            responseTime
        );
    }
    
    public static SubmitResponseCommand withoutTiming(DebateId debateId, ParticipantId participantId, 
                                                    ArgumentContent content) {
        return new SubmitResponseCommand(debateId, participantId, content, null);
    }
    
    public static SubmitResponseCommand withoutTiming(DebateId debateId, ParticipantId participantId, 
                                                    String content) {
        return new SubmitResponseCommand(
            debateId, 
            participantId, 
            ArgumentContent.of(content), 
            null
        );
    }
    
    public Optional<Duration> getResponseTime() {
        return Optional.ofNullable(responseTime);
    }
    
    public boolean hasResponseTime() {
        return responseTime != null;
    }
    
    public int getWordCount() {
        return content.wordCount();
    }
    
    public int getCharacterCount() {
        return content.length();
    }
}