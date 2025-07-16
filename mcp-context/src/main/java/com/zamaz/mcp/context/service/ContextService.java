package com.zamaz.mcp.context.service;

import com.zamaz.mcp.context.dto.AppendMessageRequest;
import com.zamaz.mcp.context.dto.ContextDto;
import com.zamaz.mcp.context.dto.CreateContextRequest;
import com.zamaz.mcp.context.dto.MessageDto;
import com.zamaz.mcp.context.entity.Context;
import com.zamaz.mcp.context.entity.Message;
import com.zamaz.mcp.context.exception.ContextNotFoundException;
import com.zamaz.mcp.context.exception.UnauthorizedAccessException;
import com.zamaz.mcp.context.repository.ContextRepository;
import com.zamaz.mcp.context.repository.MessageRepository;
import com.zamaz.mcp.security.annotation.RequiresPermission;
import com.zamaz.mcp.security.annotation.RequiresRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for managing contexts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ContextService {
    
    private final ContextRepository contextRepository;
    private final MessageRepository messageRepository;
    private final TokenCountingService tokenCountingService;
    private final ContextCacheService cacheService;
    private final ContextVersionService versionService;
    
    /**
     * Create a new context.
     */
    @RequiresPermission("context:create")
    public ContextDto createContext(CreateContextRequest request) {
        log.info("Creating context for organization: {} and user: {}", 
                request.getOrganizationId(), request.getUserId());
        
        Context context = Context.builder()
                .organizationId(request.getOrganizationId())
                .userId(request.getUserId())
                .name(request.getName())
                .description(request.getDescription())
                .metadata(request.getMetadata() != null ? request.getMetadata() : Map.of())
                .build();
        
        context = contextRepository.save(context);
        log.info("Created context with ID: {}", context.getId());
        
        return mapToDto(context);
    }
    
    /**
     * Get a context by ID with authorization check.
     */
    @RequiresPermission("context:read")
    @Transactional(readOnly = true)
    public ContextDto getContext(UUID contextId, UUID organizationId) {
        log.debug("Retrieving context: {} for organization: {}", contextId, organizationId);
        
        Context context = contextRepository.findByIdAndOrganizationId(contextId, organizationId)
                .orElseThrow(() -> new ContextNotFoundException("Context not found: " + contextId));
        
        // Update last accessed timestamp asynchronously
        contextRepository.updateLastAccessedAt(contextId, Instant.now());
        
        return mapToDto(context);
    }
    
    /**
     * List contexts for an organization.
     */
    @RequiresPermission("context:list")
    @Transactional(readOnly = true)
    public Page<ContextDto> listContexts(UUID organizationId, Pageable pageable) {
        log.debug("Listing contexts for organization: {}", organizationId);
        
        Page<Context> contexts = contextRepository.findByOrganizationIdAndStatus(
                organizationId, Context.ContextStatus.ACTIVE, pageable);
        
        return contexts.map(this::mapToDto);
    }
    
    /**
     * Search contexts by name or description.
     */
    @RequiresPermission("context:search")
    @Transactional(readOnly = true)
    public Page<ContextDto> searchContexts(UUID organizationId, String searchTerm, Pageable pageable) {
        log.debug("Searching contexts for organization: {} with term: {}", organizationId, searchTerm);
        
        Page<Context> contexts = contextRepository.searchContexts(
                organizationId, Context.ContextStatus.ACTIVE, searchTerm, pageable);
        
        return contexts.map(this::mapToDto);
    }
    
    /**
     * Append a message to a context.
     */
    @RequiresPermission("context:write")
    public MessageDto appendMessage(UUID contextId, UUID organizationId, AppendMessageRequest request) {
        log.info("Appending message to context: {}", contextId);
        
        Context context = contextRepository.findByIdAndOrganizationId(contextId, organizationId)
                .orElseThrow(() -> new ContextNotFoundException("Context not found: " + contextId));
        
        // Count tokens in the message
        int tokenCount = tokenCountingService.countTokens(request.getContent());
        
        Message message = Message.builder()
                .context(context)
                .role(Message.MessageRole.valueOf(request.getRole().toUpperCase()))
                .content(request.getContent())
                .tokenCount(tokenCount)
                .metadata(request.getMetadata() != null ? request.getMetadata() : Map.of())
                .build();
        
        context.addMessage(message);
        message = messageRepository.save(message);
        contextRepository.save(context);
        
        // Invalidate cache
        cacheService.evictContext(contextId);
        
        log.info("Appended message with {} tokens to context: {}", tokenCount, contextId);
        
        return mapToDto(message);
    }
    
    /**
     * Get messages for a context.
     */
    @Transactional(readOnly = true)
    public List<MessageDto> getMessages(UUID contextId, UUID organizationId) {
        log.debug("Retrieving messages for context: {}", contextId);
        
        // Verify access
        contextRepository.findByIdAndOrganizationId(contextId, organizationId)
                .orElseThrow(() -> new ContextNotFoundException("Context not found: " + contextId));
        
        List<Message> messages = messageRepository.findByContextIdAndIsHiddenFalseOrderByTimestampAsc(contextId);
        
        return messages.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Delete a context (soft delete).
     */
    public void deleteContext(UUID contextId, UUID organizationId) {
        log.info("Deleting context: {} for organization: {}", contextId, organizationId);
        
        Context context = contextRepository.findByIdAndOrganizationId(contextId, organizationId)
                .orElseThrow(() -> new ContextNotFoundException("Context not found: " + contextId));
        
        contextRepository.softDelete(contextId, Instant.now());
        cacheService.evictContext(contextId);
        
        log.info("Soft deleted context: {}", contextId);
    }
    
    /**
     * Archive a context.
     */
    public void archiveContext(UUID contextId, UUID organizationId) {
        log.info("Archiving context: {} for organization: {}", contextId, organizationId);
        
        Context context = contextRepository.findByIdAndOrganizationId(contextId, organizationId)
                .orElseThrow(() -> new ContextNotFoundException("Context not found: " + contextId));
        
        context.setStatus(Context.ContextStatus.ARCHIVED);
        contextRepository.save(context);
        cacheService.evictContext(contextId);
        
        log.info("Archived context: {}", contextId);
    }
    
    private ContextDto mapToDto(Context context) {
        return ContextDto.builder()
                .id(context.getId())
                .organizationId(context.getOrganizationId())
                .userId(context.getUserId())
                .name(context.getName())
                .description(context.getDescription())
                .status(context.getStatus().name())
                .metadata(context.getMetadata())
                .totalTokens(context.getTotalTokens())
                .messageCount(context.getMessages().size())
                .createdAt(context.getCreatedAt())
                .updatedAt(context.getUpdatedAt())
                .lastAccessedAt(context.getLastAccessedAt())
                .build();
    }
    
    private MessageDto mapToDto(Message message) {
        return MessageDto.builder()
                .id(message.getId())
                .role(message.getRole().name())
                .content(message.getContent())
                .tokenCount(message.getTokenCount())
                .metadata(message.getMetadata())
                .timestamp(message.getTimestamp())
                .build();
    }
}