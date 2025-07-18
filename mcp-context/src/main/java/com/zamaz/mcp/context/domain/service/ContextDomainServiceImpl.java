package com.zamaz.mcp.context.domain.service;

import com.zamaz.mcp.context.domain.model.*;
import java.time.Duration;
import java.time.Instant;

/**
 * Implementation of domain service for context-related business logic.
 * This is pure domain logic with no infrastructure dependencies.
 */
public class ContextDomainServiceImpl implements ContextDomainService {
    
    private static final Duration AUTO_ARCHIVE_AFTER = Duration.ofDays(90);
    private static final int MIN_MESSAGES_FOR_SHARING = 2;
    private static final TokenCount MIN_TOKENS_FOR_WINDOW = TokenCount.of(100);
    private static final int MAX_CONTENT_LENGTH = 100_000;
    
    @Override
    public boolean canShareContext(Context context, String targetOrganizationId) {
        // Cannot share with same organization
        if (context.getOrganizationId().value().equals(targetOrganizationId)) {
            return false;
        }
        
        // Cannot share deleted contexts
        if (context.isDeleted()) {
            return false;
        }
        
        // Must have minimum content to be worth sharing
        if (context.getVisibleMessageCount() < MIN_MESSAGES_FOR_SHARING) {
            return false;
        }
        
        // Check if context has sensitive metadata that prevents sharing
        if (context.getMetadata().get("shareable", Boolean.class).orElse(true).equals(false)) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public WindowConfiguration calculateOptimalWindow(Context context, TokenCount targetTokenLimit) {
        // Ensure minimum viable window
        TokenCount effectiveLimit = targetTokenLimit.isLessThan(MIN_TOKENS_FOR_WINDOW) 
            ? MIN_TOKENS_FOR_WINDOW 
            : targetTokenLimit;
        
        // Calculate average tokens per message
        if (context.getVisibleMessageCount() == 0) {
            return new WindowConfiguration(effectiveLimit, 0, false);
        }
        
        int avgTokensPerMessage = context.getTotalTokens().value() / context.getVisibleMessageCount();
        
        // Estimate optimal message count
        int optimalMessageCount = effectiveLimit.value() / Math.max(avgTokensPerMessage, 1);
        
        // Apply heuristics for better conversation flow
        // Prefer even numbers for request/response pairs
        if (optimalMessageCount > 2 && optimalMessageCount % 2 != 0) {
            optimalMessageCount--;
        }
        
        // Don't truncate if we can fit all messages
        boolean shouldTruncate = context.getTotalTokens().isGreaterThan(effectiveLimit);
        
        return new WindowConfiguration(
            effectiveLimit,
            optimalMessageCount,
            shouldTruncate && optimalMessageCount > 1
        );
    }
    
    @Override
    public boolean shouldAutoArchive(Context context) {
        // Already archived or deleted
        if (!context.isActive()) {
            return false;
        }
        
        // Check age
        Instant archiveThreshold = Instant.now().minus(AUTO_ARCHIVE_AFTER);
        if (context.getUpdatedAt().isBefore(archiveThreshold)) {
            return true;
        }
        
        // Check if marked for auto-archive in metadata
        if (context.getMetadata().get("autoArchive", Boolean.class).orElse(false)) {
            Instant archiveDate = context.getMetadata()
                .get("archiveAfter", String.class)
                .map(Instant::parse)
                .orElse(null);
            
            if (archiveDate != null && Instant.now().isAfter(archiveDate)) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public MessageValidationResult validateMessage(
            Context context, 
            MessageRole role, 
            MessageContent content
    ) {
        // Check context state
        if (!context.isActive()) {
            return MessageValidationResult.invalid("Cannot add messages to inactive context");
        }
        
        // Validate content length
        if (content.length() > MAX_CONTENT_LENGTH) {
            return MessageValidationResult.invalid(
                "Message content exceeds maximum length of " + MAX_CONTENT_LENGTH
            );
        }
        
        // Check for empty content on non-function messages
        if (content.isEmpty() && role != MessageRole.FUNCTION) {
            return MessageValidationResult.invalid("Message content cannot be empty");
        }
        
        // Validate role transitions
        if (!isValidRoleTransition(context, role)) {
            return MessageValidationResult.validWithWarning(
                "Unusual message role sequence detected"
            );
        }
        
        // Check for potential token limit issues
        if (context.getMessageCount() > 0) {
            int avgTokensPerChar = 4; // Rough estimate
            TokenCount estimatedTokens = TokenCount.of(content.length() / avgTokensPerChar);
            
            if (context.getTotalTokens().add(estimatedTokens).value() > 500_000) {
                return MessageValidationResult.validWithWarning(
                    "Context is approaching token limit"
                );
            }
        }
        
        return MessageValidationResult.valid();
    }
    
    private boolean isValidRoleTransition(Context context, MessageRole newRole) {
        var messages = context.getVisibleMessages();
        if (messages.isEmpty()) {
            // First message can be any role
            return true;
        }
        
        MessageRole lastRole = messages.get(messages.size() - 1).getRole();
        
        // Typical conversation pattern validations
        if (lastRole == MessageRole.USER && newRole == MessageRole.USER) {
            // Multiple user messages in a row is unusual but valid
            return true;
        }
        
        if (lastRole == MessageRole.ASSISTANT && newRole == MessageRole.ASSISTANT) {
            // Multiple assistant messages might indicate an issue
            return true;
        }
        
        // All transitions are technically valid, this is just for warnings
        return true;
    }
}