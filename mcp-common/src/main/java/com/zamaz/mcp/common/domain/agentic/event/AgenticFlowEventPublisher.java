package com.zamaz.mcp.common.domain.agentic.event;

/**
 * Interface for publishing agentic flow events.
 */
public interface AgenticFlowEventPublisher {

    /**
     * Publishes an agentic flow event.
     *
     * @param event The event to publish
     */
    void publish(AgenticFlowEvent event);
}