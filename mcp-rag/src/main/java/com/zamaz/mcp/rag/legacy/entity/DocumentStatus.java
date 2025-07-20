package com.zamaz.mcp.rag.entity;

/**
 * Status of document processing.
 */
public enum DocumentStatus {
    PENDING,       // Document uploaded but not processed
    PROCESSING,    // Currently being processed
    COMPLETED,     // Successfully processed and indexed
    FAILED,        // Processing failed
    DELETED        // Marked for deletion
}