package com.zamaz.mcp.debateengine.domain.model;

import com.zamaz.mcp.common.domain.ValueObject;

/**
 * Value object representing participant position in debate.
 */
public enum Position implements ValueObject {
    PRO("Supporting the proposition"),
    CON("Against the proposition"),
    MODERATOR("Moderating the debate"),
    JUDGE("Judging the debate"),
    OBSERVER("Observing the debate");
    
    private final String description;
    
    Position(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if position can participate in rounds.
     */
    public boolean canParticipate() {
        return this == PRO || this == CON;
    }
    
    /**
     * Check if position can moderate.
     */
    public boolean canModerate() {
        return this == MODERATOR;
    }
    
    /**
     * Check if position can judge.
     */
    public boolean canJudge() {
        return this == JUDGE;
    }
    
    /**
     * Get opposing position.
     */
    public Position getOpposite() {
        return switch (this) {
            case PRO -> CON;
            case CON -> PRO;
            default -> null;
        };
    }
}