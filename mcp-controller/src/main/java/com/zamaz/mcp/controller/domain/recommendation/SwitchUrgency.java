package com.zamaz.mcp.controller.domain.recommendation;

/**
 * Urgency level for switching agentic flow types.
 */
public enum SwitchUrgency {
    NONE("No action needed"),
    LOW("Consider switching when convenient"),
    MEDIUM("Switch recommended soon"),
    HIGH("Switch recommended immediately");
    
    private final String description;
    
    SwitchUrgency(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean requiresAction() {
        return this != NONE;
    }
    
    public boolean isUrgent() {
        return this == HIGH || this == MEDIUM;
    }
}