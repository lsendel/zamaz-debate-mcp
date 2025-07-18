package com.zamaz.mcp.context.application.usecase;

import com.zamaz.mcp.common.application.exception.ResourceNotFoundException;
import com.zamaz.mcp.common.domain.model.OrganizationId;
import com.zamaz.mcp.context.application.port.inbound.GetContextWindowUseCase;
import com.zamaz.mcp.context.application.port.outbound.ContextCacheService;
import com.zamaz.mcp.context.application.port.outbound.ContextRepository;
import com.zamaz.mcp.context.application.query.GetContextWindowQuery;
import com.zamaz.mcp.context.domain.model.Context;
import com.zamaz.mcp.context.domain.model.ContextId;
import com.zamaz.mcp.context.domain.model.ContextWindow;
import com.zamaz.mcp.context.domain.model.TokenCount;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the get context window use case.
 * Retrieves a windowed view of a context that respects token limits.
 */
public class GetContextWindowUseCaseImpl implements GetContextWindowUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(GetContextWindowUseCaseImpl.class);
    
    private final ContextRepository contextRepository;
    private final ContextCacheService cacheService;
    
    public GetContextWindowUseCaseImpl(
            ContextRepository contextRepository,
            ContextCacheService cacheService
    ) {
        this.contextRepository = Objects.requireNonNull(contextRepository, "Context repository cannot be null");
        this.cacheService = Objects.requireNonNull(cacheService, "Cache service cannot be null");
    }
    
    @Override
    public ContextWindow execute(GetContextWindowQuery query) {
        logger.info("Getting context window for context: {} with max tokens: {}", 
            query.contextId(), query.maxTokens());
        
        // Create domain objects from query
        ContextId contextId = ContextId.from(query.contextId());
        OrganizationId organizationId = OrganizationId.from(query.organizationId());
        TokenCount maxTokens = TokenCount.of(query.maxTokens());
        
        // Build cache key for this specific window
        String cacheKey = buildCacheKey(contextId, maxTokens, query.maxMessages());
        
        // Check cache first
        Optional<ContextWindow> cachedWindow = cacheService.getContextWindow(cacheKey);
        if (cachedWindow.isPresent()) {
            logger.debug("Context window found in cache for key: {}", cacheKey);
            return cachedWindow.get();
        }
        
        // Load context from repository
        Context context = contextRepository.findById(contextId)
            .filter(c -> c.getOrganizationId().equals(organizationId))
            .orElseThrow(() -> new ResourceNotFoundException(
                "Context not found: " + contextId + " in organization: " + organizationId
            ));
        
        // Create window using domain logic
        ContextWindow window = context.createWindow(maxTokens, query.maxMessages());
        
        // Cache the result
        cacheService.cacheContextWindow(cacheKey, window);
        
        logger.info("Successfully created context window with {} messages and {} tokens", 
            window.getMessageCount(), window.getTotalTokens().value());
        
        return window;
    }
    
    private String buildCacheKey(ContextId contextId, TokenCount maxTokens, Optional<Integer> maxMessages) {
        StringBuilder key = new StringBuilder("window:")
            .append(contextId.value())
            .append(":")
            .append(maxTokens.value());
        
        maxMessages.ifPresent(max -> key.append(":").append(max));
        
        return key.toString();
    }
}