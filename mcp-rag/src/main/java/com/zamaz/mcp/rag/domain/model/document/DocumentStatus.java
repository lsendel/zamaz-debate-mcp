package com.zamaz.mcp.rag.domain.model.document;

/**
 * Enum representing the various states of a document in its lifecycle.
 * The state transitions follow specific business rules enforced by the Document aggregate.
 */
public enum DocumentStatus {
    /**
     * Document has been uploaded but not yet processed
     */
    UPLOADED("Uploaded", true),
    
    /**
     * Document is currently being processed
     */
    PROCESSING("Processing", false),
    
    /**
     * Document has been split into chunks
     */
    CHUNKED("Chunked", true),
    
    /**
     * Document chunks have been embedded
     */
    EMBEDDED("Embedded", true),
    
    /**
     * Document processing failed
     */
    FAILED("Failed", true),
    
    /**
     * Document has been archived
     */
    ARCHIVED("Archived", false);
    
    private final String displayName;
    private final boolean canBeReprocessed;
    
    DocumentStatus(String displayName, boolean canBeReprocessed) {
        this.displayName = displayName;
        this.canBeReprocessed = canBeReprocessed;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean canBeReprocessed() {
        return canBeReprocessed;
    }
    
    /**
     * Check if the document is in a final successful state
     */
    public boolean isComplete() {
        return this == EMBEDDED;
    }
    
    /**
     * Check if the document is in a state where it can be searched
     */
    public boolean isSearchable() {
        return this == EMBEDDED;
    }
    
    /**
     * Check if transition to another status is allowed
     */
    public boolean canTransitionTo(DocumentStatus newStatus) {
        return switch (this) {
            case UPLOADED -> newStatus == PROCESSING || newStatus == ARCHIVED;
            case PROCESSING -> newStatus == CHUNKED || newStatus == FAILED;
            case CHUNKED -> newStatus == EMBEDDED || newStatus == FAILED || newStatus == PROCESSING;
            case EMBEDDED -> newStatus == ARCHIVED || newStatus == PROCESSING;
            case FAILED -> newStatus == PROCESSING || newStatus == ARCHIVED;
            case ARCHIVED -> false; // No transitions from archived
        };
    }
}