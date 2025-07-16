package com.zamaz.mcp.context.service;

import com.zamaz.mcp.context.dto.ContextWindowRequest;
import com.zamaz.mcp.context.dto.ContextWindowResponse;
import com.zamaz.mcp.context.dto.MessageDto;
import com.zamaz.mcp.context.entity.Context;
import com.zamaz.mcp.context.entity.Message;
import com.zamaz.mcp.context.exception.ContextNotFoundException;
import com.zamaz.mcp.context.repository.ContextRepository;
import com.zamaz.mcp.context.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing context windows and token limits.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ContextWindowService {
    
    private final ContextRepository contextRepository;
    private final MessageRepository messageRepository;
    private final TokenCountingService tokenCountingService;
    private final ContextCacheService cacheService;
    
    /**
     * Get a context window with token limit management.
     */
    public ContextWindowResponse getContextWindow(UUID contextId, UUID organizationId, ContextWindowRequest request) {
        log.info("Getting context window for context: {} with max tokens: {}", contextId, request.getMaxTokens());
        
        // Try to get from cache first
        ContextWindowResponse cached = cacheService.getContextWindow(contextId, request);
        if (cached != null) {
            log.debug("Returning cached context window for: {}", contextId);
            return cached;
        }
        
        // Verify access
        Context context = contextRepository.findByIdAndOrganizationId(contextId, organizationId)
                .orElseThrow(() -> new ContextNotFoundException("Context not found: " + contextId));
        
        // Get all messages
        List<Message> messages = messageRepository.findByContextIdAndIsHiddenFalseOrderByTimestampAsc(contextId);
        
        // Filter by message type if requested
        if (!request.getIncludeSystemMessages()) {
            messages = messages.stream()
                    .filter(m -> m.getRole() != Message.MessageRole.SYSTEM)
                    .collect(Collectors.toList());
        }
        
        // Apply windowing strategy
        ContextWindowResponse response = applyWindowingStrategy(messages, request);
        
        // Cache the result
        cacheService.putContextWindow(contextId, request, response);
        
        return response;
    }
    
    /**
     * Apply windowing strategy to fit messages within token limit.
     */
    private ContextWindowResponse applyWindowingStrategy(List<Message> messages, ContextWindowRequest request) {
        int maxTokens = request.getMaxTokens();
        Integer messageLimit = request.getMessageLimit();
        boolean preserveBoundaries = request.getPreserveMessageBoundaries();
        
        List<MessageDto> windowMessages = new ArrayList<>();
        int totalTokens = 0;
        boolean truncated = false;
        String truncationStrategy = "none";
        
        // Start from most recent messages
        List<Message> reversedMessages = new ArrayList<>(messages);
        Collections.reverse(reversedMessages);
        
        int messageCount = 0;
        for (Message message : reversedMessages) {
            // Check message limit
            if (messageLimit != null && messageCount >= messageLimit) {
                truncated = true;
                truncationStrategy = "message_limit";
                break;
            }
            
            int messageTokens = message.getTokenCount() != null ? message.getTokenCount() : 
                               tokenCountingService.countTokens(message.getContent());
            
            // Check if adding this message would exceed token limit
            if (totalTokens + messageTokens > maxTokens) {
                if (preserveBoundaries) {
                    // Don't include partial messages
                    truncated = true;
                    truncationStrategy = "token_limit_boundary";
                    break;
                } else {
                    // Truncate the message content to fit
                    int remainingTokens = maxTokens - totalTokens;
                    if (remainingTokens > 0) {
                        String truncatedContent = tokenCountingService.truncateToTokenLimit(
                                message.getContent(), remainingTokens);
                        
                        MessageDto truncatedMessage = MessageDto.builder()
                                .id(message.getId())
                                .role(message.getRole().name())
                                .content(truncatedContent + "\n[TRUNCATED]")
                                .tokenCount(remainingTokens)
                                .metadata(message.getMetadata())
                                .timestamp(message.getTimestamp())
                                .build();
                        
                        windowMessages.add(truncatedMessage);
                        totalTokens += remainingTokens;
                    }
                    truncated = true;
                    truncationStrategy = "token_limit_truncated";
                    break;
                }
            }
            
            // Add the full message
            MessageDto messageDto = MessageDto.builder()
                    .id(message.getId())
                    .role(message.getRole().name())
                    .content(message.getContent())
                    .tokenCount(messageTokens)
                    .metadata(message.getMetadata())
                    .timestamp(message.getTimestamp())
                    .build();
            
            windowMessages.add(messageDto);
            totalTokens += messageTokens;
            messageCount++;
        }
        
        // Reverse back to chronological order
        Collections.reverse(windowMessages);
        
        return ContextWindowResponse.builder()
                .messages(windowMessages)
                .totalTokens(totalTokens)
                .messageCount(windowMessages.size())
                .truncated(truncated)
                .truncationStrategy(truncationStrategy)
                .build();
    }
}