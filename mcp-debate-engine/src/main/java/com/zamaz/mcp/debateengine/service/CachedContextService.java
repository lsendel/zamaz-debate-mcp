package com.zamaz.mcp.debateengine.service;

import com.zamaz.mcp.debateengine.config.CacheConfiguration;
import com.zamaz.mcp.debateengine.dto.ContextDto;
import com.zamaz.mcp.debateengine.dto.MessageDto;
import com.zamaz.mcp.debateengine.entity.Context;
import com.zamaz.mcp.debateengine.entity.Message;
import com.zamaz.mcp.debateengine.repository.ContextRepository;
import com.zamaz.mcp.debateengine.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Cached context service with intelligent caching strategies.
 * Implements sliding window caching for active contexts.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@CacheConfig(cacheNames = CacheConfiguration.CONTEXT_CACHE)
public class CachedContextService {

    private final ContextRepository contextRepository;
    private final MessageRepository messageRepository;
    private final ContextMapper contextMapper;
    private final MessageMapper messageMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Get context by ID with caching
     */
    @Cacheable(key = "#contextId", condition = "#includeMessages == false")
    @Transactional(readOnly = true)
    public ContextDto getContext(UUID contextId, boolean includeMessages) {
        log.debug("Fetching context: {} (includeMessages: {})", contextId, includeMessages);
        
        Context context = contextRepository.findById(contextId)
                .orElseThrow(() -> new ResourceNotFoundException("Context not found: " + contextId));
        
        ContextDto dto = contextMapper.toDto(context);
        
        if (includeMessages) {
            List<Message> messages = getMessagesFromCache(contextId);
            dto.setMessages(messages.stream().map(messageMapper::toDto).toList());
        }
        
        return dto;
    }

    /**
     * Create context with cache population
     */
    @CacheEvict(cacheNames = CacheConfiguration.ORG_DEBATES_CACHE, key = "#organizationId")
    @Transactional
    public ContextDto createContext(UUID debateId, UUID organizationId, UUID userId, CreateContextRequest request) {
        log.info("Creating context for debate: {}", debateId);
        
        Context context = Context.builder()
                .debateId(debateId)
                .organizationId(organizationId)
                .userId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .status("ACTIVE")
                .totalTokens(0)
                .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 4096)
                .messageCount(0)
                .windowSize(request.getWindowSize() != null ? request.getWindowSize() : 4096)
                .version(1)
                .build();
        
        context = contextRepository.save(context);
        
        // Pre-populate cache
        ContextDto dto = contextMapper.toDto(context);
        cacheContext(dto);
        
        return dto;
    }

    /**
     * Add message to context with cache update
     */
    @CacheEvict(key = "#contextId")
    @Transactional
    public MessageDto addMessage(UUID contextId, AddMessageRequest request) {
        log.debug("Adding message to context: {}", contextId);
        
        Context context = contextRepository.findById(contextId)
                .orElseThrow(() -> new ResourceNotFoundException("Context not found: " + contextId));
        
        // Check token limits
        if (context.getTotalTokens() + request.getTokenCount() > context.getMaxTokens()) {
            throw new TokenLimitExceededException("Token limit would be exceeded");
        }
        
        // Create message
        Message message = Message.builder()
                .contextId(contextId)
                .role(request.getRole())
                .content(request.getContent())
                .sequenceNumber(context.getMessageCount() + 1)
                .tokenCount(request.getTokenCount())
                .isHidden(false)
                .roundId(request.getRoundId())
                .participantId(request.getParticipantId())
                .build();
        
        message = messageRepository.save(message);
        
        // Update context
        context.setMessageCount(context.getMessageCount() + 1);
        context.setTotalTokens(context.getTotalTokens() + request.getTokenCount());
        context.setLastActivityAt(Instant.now());
        contextRepository.save(context);
        
        // Update message cache
        cacheMessage(contextId, message);
        
        return messageMapper.toDto(message);
    }

    /**
     * Get messages with intelligent caching and windowing
     */
    @Cacheable(cacheNames = CacheConfiguration.MESSAGE_CACHE, key = "#contextId + ':' + #limit")
    @Transactional(readOnly = true)
    public List<MessageDto> getRecentMessages(UUID contextId, int limit) {
        log.debug("Fetching recent {} messages for context: {}", limit, contextId);
        
        Pageable pageable = PageRequest.of(0, limit, Sort.by("sequenceNumber").descending());
        Page<Message> messages = messageRepository.findByContextIdAndIsHiddenFalse(contextId, pageable);
        
        return messages.getContent().stream()
                .map(messageMapper::toDto)
                .toList();
    }

    /**
     * Get windowed context for token management
     */
    @Transactional(readOnly = true)
    public WindowedContext getWindowedContext(UUID contextId) {
        log.debug("Getting windowed context: {}", contextId);
        
        Context context = contextRepository.findById(contextId)
                .orElseThrow(() -> new ResourceNotFoundException("Context not found: " + contextId));
        
        // Get messages that fit within window
        List<Message> messages = getMessagesWithinTokenWindow(contextId, context.getWindowSize());
        
        return WindowedContext.builder()
                .contextId(contextId)
                .windowSize(context.getWindowSize())
                .currentTokens(messages.stream().mapToInt(Message::getTokenCount).sum())
                .messages(messages.stream().map(messageMapper::toDto).toList())
                .build();
    }

    /**
     * Update context version with cache invalidation
     */
    @CacheEvict(key = "#contextId")
    @Transactional
    public ContextDto createVersion(UUID contextId, String changeSummary) {
        log.info("Creating new version for context: {}", contextId);
        
        Context context = contextRepository.findById(contextId)
                .orElseThrow(() -> new ResourceNotFoundException("Context not found: " + contextId));
        
        // Save current state as version
        contextVersionService.saveVersion(context, changeSummary);
        
        // Increment version
        context.setVersion(context.getVersion() + 1);
        context = contextRepository.save(context);
        
        return contextMapper.toDto(context);
    }

    /**
     * Cache warming for active debates
     */
    @Transactional(readOnly = true)
    public void warmUpActiveContexts() {
        log.info("Warming up cache for active contexts");
        
        List<Context> activeContexts = contextRepository.findByStatus("ACTIVE");
        int warmedUp = 0;
        
        for (Context context : activeContexts) {
            // Check if recently active (within last hour)
            if (context.getLastActivityAt().isAfter(Instant.now().minus(Duration.ofHours(1)))) {
                cacheContext(contextMapper.toDto(context));
                cacheRecentMessages(context.getId(), 20);
                warmedUp++;
            }
        }
        
        log.info("Warmed up cache for {} active contexts", warmedUp);
    }

    /**
     * Clear context caches
     */
    @Caching(evict = {
        @CacheEvict(key = "#contextId"),
        @CacheEvict(cacheNames = CacheConfiguration.MESSAGE_CACHE, key = "#contextId + ':*'")
    })
    public void clearContextCache(UUID contextId) {
        log.debug("Clearing cache for context: {}", contextId);
    }

    // Private helper methods

    private void cacheContext(ContextDto context) {
        String key = CacheConfiguration.CONTEXT_CACHE + "::" + context.getId();
        redisTemplate.opsForValue().set(key, context, 15, TimeUnit.MINUTES);
    }

    private void cacheMessage(UUID contextId, Message message) {
        String key = CacheConfiguration.MESSAGE_CACHE + "::" + contextId + ":messages";
        redisTemplate.opsForList().rightPush(key, messageMapper.toDto(message));
        redisTemplate.expire(key, 5, TimeUnit.MINUTES);
        
        // Trim to keep only recent messages in cache
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size > 100) {
            redisTemplate.opsForList().leftPop(key);
        }
    }

    private List<Message> getMessagesFromCache(UUID contextId) {
        String key = CacheConfiguration.MESSAGE_CACHE + "::" + contextId + ":messages";
        List<Object> cached = redisTemplate.opsForList().range(key, 0, -1);
        
        if (cached != null && !cached.isEmpty()) {
            log.debug("Retrieved {} messages from cache", cached.size());
            return cached.stream()
                    .map(obj -> (Message) obj)
                    .toList();
        }
        
        // Cache miss - load from database
        List<Message> messages = messageRepository.findByContextIdOrderBySequenceNumber(contextId);
        cacheRecentMessages(contextId, 100);
        return messages;
    }

    private void cacheRecentMessages(UUID contextId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("sequenceNumber").descending());
        Page<Message> messages = messageRepository.findByContextIdAndIsHiddenFalse(contextId, pageable);
        
        String key = CacheConfiguration.MESSAGE_CACHE + "::" + contextId + ":messages";
        redisTemplate.delete(key);
        
        for (Message message : messages.getContent()) {
            redisTemplate.opsForList().leftPush(key, messageMapper.toDto(message));
        }
        
        redisTemplate.expire(key, 5, TimeUnit.MINUTES);
    }

    private List<Message> getMessagesWithinTokenWindow(UUID contextId, int windowSize) {
        List<Message> allMessages = messageRepository.findByContextIdOrderBySequenceNumberDesc(contextId);
        List<Message> windowedMessages = new ArrayList<>();
        int currentTokens = 0;
        
        for (Message message : allMessages) {
            if (currentTokens + message.getTokenCount() <= windowSize) {
                windowedMessages.add(0, message); // Add to beginning to maintain order
                currentTokens += message.getTokenCount();
            } else {
                break;
            }
        }
        
        return windowedMessages;
    }
}