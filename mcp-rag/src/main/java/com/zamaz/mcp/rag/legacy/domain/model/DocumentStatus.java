package com.zamaz.mcp.rag.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;

/**
 * Value object representing the processing status of a document.
 */
public enum DocumentStatus implements ValueObject {
    PENDING("pending", "Document uploaded but not yet processed"),
    PROCESSING("processing", "Document is currently being processed"),
    COMPLETED("completed", "Document successfully processed and indexed"),
    FAILED("failed", "Document processing failed"),
    ARCHIVED("archived", "Document has been archived");
    
    private final String value;
    private final String description;
    
    DocumentStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean canTransitionTo(DocumentStatus newStatus) {
        return switch (this) {
            case PENDING -> newStatus == PROCESSING || newStatus == FAILED;
            case PROCESSING -> newStatus == COMPLETED || newStatus == FAILED;
            case COMPLETED -> newStatus == ARCHIVED;
            case FAILED -> newStatus == PENDING || newStatus == ARCHIVED;
            case ARCHIVED -> false; // Terminal state
        };
    }
    
    public boolean isTerminal() {
        return this == ARCHIVED;
    }
    
    public boolean isProcessable() {
        return this == PENDING;
    }
    
    public boolean isSearchable() {
        return this == COMPLETED;
    }
    
    public boolean hasError() {
        return this == FAILED;
    }
    
    public static DocumentStatus fromValue(String value) {
        for (DocumentStatus status : DocumentStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid document status: " + value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}