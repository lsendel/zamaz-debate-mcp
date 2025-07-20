package com.zamaz.mcp.common.domain.agentic;

/**
 * Enumeration of possible statuses for an agentic flow.
 */
public enum AgenticFlowStatus {
    /**
     * The flow is active and available for use.
     */
    ACTIVE,

    /**
     * The flow is inactive and not available for use.
     */
    INACTIVE,

    /**
     * The flow is in draft state and not yet ready for production use.
     */
    DRAFT
}