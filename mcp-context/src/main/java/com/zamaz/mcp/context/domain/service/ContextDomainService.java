package com.zamaz.mcp.context.domain.service;

import com.zamaz.mcp.common.domain.service.DomainService;
import com.zamaz.mcp.context.domain.model.*;

/**
 * Domain service for complex business logic that spans multiple aggregates
 * or requires domain expertise beyond a single aggregate.
 */
public interface ContextDomainService extends DomainService {
    
    /**
     * Validates whether a context can be shared with another organization.
     * 
     * @param context The context to share
     * @param targetOrganizationId The organization to share with
     * @return true if sharing is allowed, false otherwise
     */
    boolean canShareContext(Context context, String targetOrganizationId);
    
    /**
     * Calculates the optimal window size for a given token limit.
     * Takes into account message boundaries and token distribution.
     * 
     * @param context The context to analyze
     * @param targetTokenLimit The desired token limit
     * @return The optimal window configuration
     */
    WindowConfiguration calculateOptimalWindow(Context context, TokenCount targetTokenLimit);
    
    /**
     * Determines if a context should be automatically archived based on
     * business rules (age, inactivity, size, etc.).
     * 
     * @param context The context to evaluate
     * @return true if the context should be archived
     */
    boolean shouldAutoArchive(Context context);
    
    /**
     * Validates message content against business rules before appending.
     * 
     * @param context The context to append to
     * @param role The message role
     * @param content The message content
     * @return validation result with any warnings or errors
     */
    MessageValidationResult validateMessage(Context context, MessageRole role, MessageContent content);
    
    /**
     * Configuration for creating an optimal context window.
     */
    record WindowConfiguration(
        TokenCount maxTokens,
        int maxMessages,
        boolean truncateLastMessage
    ) {}
    
    /**
     * Result of message validation.
     */
    record MessageValidationResult(
        boolean isValid,
        String errorMessage,
        boolean hasWarnings,
        String warningMessage
    ) {
        public static MessageValidationResult valid() {
            return new MessageValidationResult(true, null, false, null);
        }
        
        public static MessageValidationResult invalid(String error) {
            return new MessageValidationResult(false, error, false, null);
        }
        
        public static MessageValidationResult validWithWarning(String warning) {
            return new MessageValidationResult(true, null, true, warning);
        }
    }
}