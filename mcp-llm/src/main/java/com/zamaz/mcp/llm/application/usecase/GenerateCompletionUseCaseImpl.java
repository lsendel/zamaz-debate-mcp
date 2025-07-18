package com.zamaz.mcp.llm.application.usecase;

import com.zamaz.mcp.common.application.exception.ResourceNotFoundException;
import com.zamaz.mcp.llm.application.command.GenerateCompletionCommand;
import com.zamaz.mcp.llm.application.port.inbound.GenerateCompletionUseCase;
import com.zamaz.mcp.llm.application.port.outbound.CompletionCacheService;
import com.zamaz.mcp.llm.application.port.outbound.LlmProviderGateway;
import com.zamaz.mcp.llm.application.port.outbound.ProviderRepository;
import com.zamaz.mcp.llm.application.query.CompletionResult;
import com.zamaz.mcp.llm.domain.model.*;
import com.zamaz.mcp.llm.domain.service.ProviderSelectionService;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the generate completion use case.
 * Orchestrates provider selection, caching, and completion generation.
 */
public class GenerateCompletionUseCaseImpl implements GenerateCompletionUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(GenerateCompletionUseCaseImpl.class);
    private static final Duration DEFAULT_CACHE_TTL = Duration.ofHours(1);
    
    private final ProviderRepository providerRepository;
    private final LlmProviderGateway providerGateway;
    private final CompletionCacheService cacheService;
    private final ProviderSelectionService selectionService;
    
    public GenerateCompletionUseCaseImpl(
            ProviderRepository providerRepository,
            LlmProviderGateway providerGateway,
            CompletionCacheService cacheService,
            ProviderSelectionService selectionService
    ) {
        this.providerRepository = Objects.requireNonNull(providerRepository, "Provider repository cannot be null");
        this.providerGateway = Objects.requireNonNull(providerGateway, "Provider gateway cannot be null");
        this.cacheService = Objects.requireNonNull(cacheService, "Cache service cannot be null");
        this.selectionService = Objects.requireNonNull(selectionService, "Selection service cannot be null");
    }
    
    @Override
    public CompletionResult execute(GenerateCompletionCommand command) {
        logger.info("Generating completion for user: {} in org: {}", 
            command.userId(), command.organizationId());
        
        Instant startTime = Instant.now();
        
        // Create domain request
        CompletionRequest request = createCompletionRequest(command);
        
        // Select provider and model
        ProviderSelection selection = selectProviderAndModel(request);
        
        // Check cache first if enabled
        if (command.enableCaching()) {
            Optional<LlmProviderGateway.CompletionResponse> cached = checkCache(
                request, selection.model(), selection.provider().getProviderId()
            );
            if (cached.isPresent()) {
                logger.debug("Cache hit for completion request");
                return createResultFromCache(cached.get(), selection, startTime);
            }
        }
        
        // Generate completion
        LlmProviderGateway.CompletionResponse response = generateCompletion(
            selection.provider(), selection.model(), request
        );
        
        // Cache the response if enabled
        if (command.enableCaching()) {
            cacheCompletion(request, selection, response);
        }
        
        // Create result
        CompletionResult result = createResult(response, selection, startTime);
        
        logger.info("Completion generated successfully in {}ms using {}/{}", 
            result.durationMs(), selection.provider().getName(), selection.model().getModelName());
        
        return result;
    }
    
    private CompletionRequest createCompletionRequest(GenerateCompletionCommand command) {
        return CompletionRequest.create(
            PromptContent.of(command.prompt()),
            command.preferredModel().map(ModelName::of),
            command.preferredProvider().map(ProviderId::of),
            command.maxTokens(),
            command.temperature(),
            false, // Not streaming
            false, // System message support not specified
            command.organizationId(),
            command.userId()
        );
    }
    
    private ProviderSelection selectProviderAndModel(CompletionRequest request) {
        // Try preferred provider/model first
        if (request.getPreferredProvider().isPresent()) {
            ProviderId providerId = request.getPreferredProvider().get();
            Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Preferred provider not found: " + providerId
                ));
            
            if (request.getPreferredModel().isPresent()) {
                ModelName modelName = request.getPreferredModel().get();
                LlmModel model = provider.getModel(modelName)
                    .filter(m -> request.isCompatibleWith(m))
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Preferred model not available or incompatible: " + modelName
                    ));
                
                return new ProviderSelection(provider, model);
            }
        }
        
        // Use selection service to find best provider/model
        ProviderSelectionService.SelectionCriteria criteria = 
            ProviderSelectionService.SelectionCriteria.builder()
                .requiredTokens(request.getTotalEstimatedTokens())
                .preferredProvider(request.getPreferredProvider())
                .preferredModel(request.getPreferredModel())
                .build();
        
        return selectionService.selectBestProvider(criteria)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No suitable provider found for the request"
            ));
    }
    
    private Optional<LlmProviderGateway.CompletionResponse> checkCache(
            CompletionRequest request,
            LlmModel model,
            ProviderId providerId
    ) {
        String cacheKey = cacheService.generateCacheKey(request, model.getModelName(), providerId);
        return cacheService.getCachedCompletion(cacheKey);
    }
    
    private LlmProviderGateway.CompletionResponse generateCompletion(
            Provider provider,
            LlmModel model,
            CompletionRequest request
    ) {
        try {
            request.markAsProcessing();
            
            LlmProviderGateway.CompletionResponse response = providerGateway
                .generateCompletion(provider.getProviderId(), model.getModelName(), request)
                .block(); // Blocking for synchronous use case
            
            request.markAsCompleted();
            return response;
            
        } catch (Exception e) {
            request.markAsFailed(e.getMessage());
            logger.error("Failed to generate completion using {}/{}: {}", 
                provider.getName(), model.getModelName(), e.getMessage(), e);
            throw new RuntimeException("Completion generation failed: " + e.getMessage(), e);
        }
    }
    
    private void cacheCompletion(
            CompletionRequest request,
            ProviderSelection selection,
            LlmProviderGateway.CompletionResponse response
    ) {
        try {
            String cacheKey = cacheService.generateCacheKey(
                request, 
                selection.model().getModelName(), 
                selection.provider().getProviderId()
            );
            cacheService.cacheCompletion(cacheKey, response, DEFAULT_CACHE_TTL);
        } catch (Exception e) {
            logger.warn("Failed to cache completion response: {}", e.getMessage());
            // Don't fail the request due to caching issues
        }
    }
    
    private CompletionResult createResult(
            LlmProviderGateway.CompletionResponse response,
            ProviderSelection selection,
            Instant startTime
    ) {
        return new CompletionResult(
            response.content(),
            response.usage().inputTokens(),
            response.usage().outputTokens(),
            response.usage().totalCost(),
            selection.provider().getName(),
            selection.model().getModelName().value(),
            response.finishReason(),
            startTime,
            Instant.now(),
            false, // Not from cache
            null // No error
        );
    }
    
    private CompletionResult createResultFromCache(
            LlmProviderGateway.CompletionResponse response,
            ProviderSelection selection,
            Instant startTime
    ) {
        return new CompletionResult(
            response.content(),
            response.usage().inputTokens(),
            response.usage().outputTokens(),
            response.usage().totalCost(),
            selection.provider().getName(),
            selection.model().getModelName().value(),
            response.finishReason(),
            startTime,
            Instant.now(),
            true, // From cache
            null // No error
        );
    }
    
    /**
     * Internal record for provider and model selection.
     */
    private record ProviderSelection(Provider provider, LlmModel model) {}
}