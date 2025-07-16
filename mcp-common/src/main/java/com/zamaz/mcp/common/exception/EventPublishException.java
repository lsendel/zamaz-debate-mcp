package com.zamaz.mcp.common.exception;

/**
 * Exception thrown when event publishing fails
 */
public class EventPublishException extends RuntimeException {
    
    private final String eventType;
    private final String reason;
    
    public EventPublishException(String message) {
        super(message);
        this.eventType = null;
        this.reason = null;
    }
    
    public EventPublishException(String message, Throwable cause) {
        super(message, cause);
        this.eventType = null;
        this.reason = null;
    }
    
    public EventPublishException(String eventType, String reason, String message) {
        super(message);
        this.eventType = eventType;
        this.reason = reason;
    }
    
    public EventPublishException(String eventType, String reason, String message, Throwable cause) {
        super(message, cause);
        this.eventType = eventType;
        this.reason = reason;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public String getReason() {
        return reason;
    }
    
    @Override
    public String toString() {
        if (eventType != null && reason != null) {
            return String.format("EventPublishException{eventType='%s', reason='%s', message='%s'}", 
                eventType, reason, getMessage());
        }
        return super.toString();
    }
}