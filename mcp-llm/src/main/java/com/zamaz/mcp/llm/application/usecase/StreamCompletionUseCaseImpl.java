package com.zamaz.mcp.llm.application.usecase;

import com.zamaz.mcp.common.application.exception.ResourceNotFoundException;
import com.zamaz.mcp.llm.application.command.StreamCompletionCommand;
import com.zamaz.mcp.llm.application.port.inbound.StreamCompletionUseCase;
import com.zamaz.mcp.llm.application.port.outbound.LlmProviderGateway;
import com.zamaz.mcp.llm.application.port.outbound.ProviderRepository;
import com.zamaz.mcp.llm.application.query.CompletionChunk;
import com.zamaz.mcp.llm.domain.model.*;
import com.zamaz.mcp.llm.domain.service.ProviderSelectionService;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

/**
 * Implementation of the stream completion use case.
 * Orchestrates provider selection and streaming completion generation.
 */
public class StreamCompletionUseCaseImpl implements StreamCompletionUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(StreamCompletionUseCaseImpl.class);
    
    private final ProviderRepository providerRepository;
    private final LlmProviderGateway providerGateway;
    private final ProviderSelectionService selectionService;
    
    public StreamCompletionUseCaseImpl(
            ProviderRepository providerRepository,
            LlmProviderGateway providerGateway,
            ProviderSelectionService selectionService
    ) {
        this.providerRepository = Objects.requireNonNull(providerRepository, "Provider repository cannot be null");
        this.providerGateway = Objects.requireNonNull(providerGateway, "Provider gateway cannot be null");
        this.selectionService = Objects.requireNonNull(selectionService, "Selection service cannot be null");
    }
    
    @Override
    public Flux<CompletionChunk> execute(StreamCompletionCommand command) {
        logger.info("Starting streaming completion for user: {} in org: {}", 
            command.userId(), command.organizationId());
        
        // Create domain request
        CompletionRequest request = createCompletionRequest(command);
        
        // Select provider and model
        ProviderSelection selection = selectProviderAndModel(request);
        
        // Generate unique stream and request IDs
        String streamId = command.streamId().orElse(generateStreamId());
        String requestId = generateRequestId();
        
        // Generate streaming completion
        return generateStreamingCompletion(selection, request, streamId, requestId, command)
            .doOnSubscribe(subscription -> {
                logger.info("Starting stream with ID: {} using {}/{}", 
                    streamId, selection.provider().getName(), selection.model().getModelName());
                request.markAsProcessing();
            })
            .doOnComplete(() -> {
                logger.info("Completed streaming completion for stream: {}", streamId);
                request.markAsCompleted();
            })
            .doOnError(error -> {
                logger.error("Streaming completion failed for stream: {}: {}", 
                    streamId, error.getMessage(), error);
                request.markAsFailed(error.getMessage());
            });
    }
    
    private CompletionRequest createCompletionRequest(StreamCompletionCommand command) {
        return CompletionRequest.create(
            PromptContent.of(command.prompt()),
            command.preferredModel().map(ModelName::of),
            command.preferredProvider().map(ProviderId::of),
            command.maxTokens(),
            command.temperature(),
            true, // Streaming enabled
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
                    .filter(LlmModel::isSupportsStreaming)
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Preferred model not available, incompatible, or doesn't support streaming: " + modelName
                    ));
                
                return new ProviderSelection(provider, model);
            }
        }
        
        // Use selection service to find best streaming-capable provider/model
        ProviderSelectionService.SelectionCriteria criteria = 
            ProviderSelectionService.SelectionCriteria.builder()
                .requiredTokens(request.getTotalEstimatedTokens())
                .preferredProvider(request.getPreferredProvider())
                .preferredModel(request.getPreferredModel())
                .requiredCapabilities(List.of(LlmModel.ModelCapability.STREAMING))
                .build();
        
        ProviderSelectionService.ProviderSelection serviceSelection = selectionService.selectBestProvider(criteria)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No suitable streaming-capable provider found for the request"
            ));
        
        return new ProviderSelection(serviceSelection.provider(), serviceSelection.model());
    }
    
    private Flux<CompletionChunk> generateStreamingCompletion(
            ProviderSelection selection,
            CompletionRequest request,
            String streamId,
            String requestId,
            StreamCompletionCommand command
    ) {
        try {
            // Generate streaming completion from provider gateway
            Flux<LlmProviderGateway.CompletionChunk> providerStream = providerGateway
                .generateStreamingCompletion(
                    selection.provider().getProviderId(), 
                    selection.model().getModelName(), 
                    request
                );
            
            // Transform provider chunks to application chunks
            AtomicInteger chunkIndex = new AtomicInteger(0);
            StringBuilder contentBuffer = new StringBuilder();
            
            return providerStream
                .bufferTimeout(command.bufferSize(), java.time.Duration.ofMillis(100))
                .flatMap(chunks -> {
                    if (chunks.isEmpty()) {
                        return Flux.empty();
                    }
                    
                    return Flux.fromIterable(chunks)
                        .map(providerChunk -> transformChunk(
                            providerChunk, 
                            requestId, 
                            streamId, 
                            chunkIndex.getAndIncrement(),
                            contentBuffer,
                            command.enableDelta()
                        ));
                })
                .concatWith(Flux.defer(() -> {
                    // Send final chunk
                    return Flux.just(CompletionChunk.last(
                        requestId, 
                        streamId, 
                        chunkIndex.get(),
                        "stop"
                    ));
                }))
                .onErrorResume(error -> {
                    logger.error("Error in streaming completion: {}", error.getMessage(), error);
                    return Flux.just(CompletionChunk.error(
                        requestId,
                        streamId,
                        chunkIndex.get(),
                        error.getMessage()
                    ));
                });
                
        } catch (Exception e) {
            logger.error("Failed to initiate streaming completion using {}/{}: {}", 
                selection.provider().getName(), selection.model().getModelName(), e.getMessage(), e);
            return Flux.error(new RuntimeException("Streaming completion initiation failed: " + e.getMessage(), e));
        }
    }
    
    private CompletionChunk transformChunk(
            LlmProviderGateway.CompletionChunk providerChunk,
            String requestId,
            String streamId,
            int chunkIndex,
            StringBuilder contentBuffer,
            boolean enableDelta
    ) {
        if (providerChunk.isComplete()) {
            // This is the final chunk from provider
            return CompletionChunk.last(
                requestId,
                streamId,
                chunkIndex,
                providerChunk.finishReason() != null ? providerChunk.finishReason() : "stop"
            );
        }
        
        // Accumulate content
        contentBuffer.append(providerChunk.content());
        
        if (enableDelta) {
            // Return delta (incremental) content
            return CompletionChunk.delta(
                requestId,
                streamId,
                chunkIndex,
                providerChunk.content()
            );
        } else {
            // Return full content so far
            return CompletionChunk.full(
                requestId,
                streamId,
                chunkIndex,
                contentBuffer.toString()
            );
        }
    }
    
    private String generateStreamId() {
        return "stream_" + UUID.randomUUID().toString().replace("-", "");
    }
    
    private String generateRequestId() {
        return "req_" + UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * Internal record for provider and model selection.
     */
    private record ProviderSelection(Provider provider, LlmModel model) {}
}