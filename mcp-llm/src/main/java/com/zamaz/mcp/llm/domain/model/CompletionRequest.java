package com.zamaz.mcp.llm.domain.model;

import com.zamaz.mcp.common.domain.model.AggregateRoot;
import com.zamaz.mcp.common.domain.exception.DomainRuleViolationException;
import com.zamaz.mcp.llm.domain.event.CompletionRequestCreatedEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregate root representing a request for LLM text completion.
 */
public class CompletionRequest extends AggregateRoot<RequestId> {
    
    private final PromptContent prompt;
    private final Optional<ModelName> preferredModel;
    private final Optional<ProviderId> preferredProvider;
    private final int maxTokens;
    private final double temperature;
    private final boolean streaming;
    private final boolean systemMessageSupport;
    private final String organizationId;
    private final String userId;
    private final Instant createdAt;
    private RequestStatus status;
    private Optional<String> errorMessage;
    
    private CompletionRequest(
            RequestId requestId,
            PromptContent prompt,
            Optional<ModelName> preferredModel,
            Optional<ProviderId> preferredProvider,
            int maxTokens,
            double temperature,
            boolean streaming,
            boolean systemMessageSupport,
            String organizationId,
            String userId,
            Instant createdAt,
            RequestStatus status,
            Optional<String> errorMessage
    ) {
        super(requestId);
        this.prompt = Objects.requireNonNull(prompt, "Prompt cannot be null");
        this.preferredModel = Objects.requireNonNull(preferredModel, "Preferred model cannot be null");
        this.preferredProvider = Objects.requireNonNull(preferredProvider, "Preferred provider cannot be null");
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        this.streaming = streaming;
        this.systemMessageSupport = systemMessageSupport;
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created at cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.errorMessage = Objects.requireNonNull(errorMessage, "Error message cannot be null");
    }
    
    public static CompletionRequest create(
            PromptContent prompt,
            Optional<ModelName> preferredModel,
            Optional<ProviderId> preferredProvider,
            int maxTokens,
            double temperature,
            boolean streaming,
            boolean systemMessageSupport,
            String organizationId,
            String userId
    ) {
        RequestId requestId = RequestId.generate();
        Instant now = Instant.now();
        
        CompletionRequest request = new CompletionRequest(
            requestId,
            prompt,
            preferredModel,
            preferredProvider,
            maxTokens,
            temperature,
            streaming,
            systemMessageSupport,
            organizationId,
            userId,
            now,
            RequestStatus.PENDING,
            Optional.empty()
        );
        
        request.registerEvent(new CompletionRequestCreatedEvent(
            requestId.asString(),
            organizationId,
            userId,
            prompt.estimateTokenCount(),
            preferredModel.map(ModelName::value).orElse(null),
            preferredProvider.map(ProviderId::value).orElse(null),
            now
        ));
        
        return request;
    }
    
    public void markAsProcessing() {
        if (status != RequestStatus.PENDING) {
            throw new DomainRuleViolationException(
                "CompletionRequest.status.invalid",
                "Can only mark pending requests as processing"
            );
        }
        this.status = RequestStatus.PROCESSING;
        this.errorMessage = Optional.empty();
    }
    
    public void markAsCompleted() {
        if (status != RequestStatus.PROCESSING) {
            throw new DomainRuleViolationException(
                "CompletionRequest.status.invalid",
                "Can only mark processing requests as completed"
            );
        }
        this.status = RequestStatus.COMPLETED;
        this.errorMessage = Optional.empty();
    }
    
    public void markAsFailed(String error) {
        Objects.requireNonNull(error, "Error message cannot be null");
        this.status = RequestStatus.FAILED;
        this.errorMessage = Optional.of(error);
    }
    
    public boolean isCompatibleWith(LlmModel model) {
        // Check if the model can handle the request
        if (!model.canHandleRequest(prompt.estimateTokenCount() + maxTokens)) {
            return false;
        }
        
        // Check required capabilities
        if (streaming && !model.hasCapability(LlmModel.ModelCapability.STREAMING)) {
            return false;
        }
        
        if (systemMessageSupport && !model.hasCapability(LlmModel.ModelCapability.SYSTEM_MESSAGES)) {
            return false;
        }
        
        return true;
    }
    
    public int estimateInputTokens() {
        return prompt.estimateTokenCount();
    }
    
    public int getTotalEstimatedTokens() {
        return estimateInputTokens() + maxTokens;
    }
    
    @Override
    public void validateInvariants() {
        if (prompt == null) {
            throw new DomainRuleViolationException(
                "CompletionRequest.prompt.required",
                "Prompt is required"
            );
        }
        
        if (maxTokens <= 0) {
            throw new DomainRuleViolationException(
                "CompletionRequest.maxTokens.invalid",
                "Max tokens must be positive"
            );
        }
        
        if (temperature < 0.0 || temperature > 2.0) {
            throw new DomainRuleViolationException(
                "CompletionRequest.temperature.invalid",
                "Temperature must be between 0.0 and 2.0"
            );
        }
        
        if (organizationId == null || organizationId.trim().isEmpty()) {
            throw new DomainRuleViolationException(
                "CompletionRequest.organizationId.required",
                "Organization ID is required"
            );
        }
        
        if (userId == null || userId.trim().isEmpty()) {
            throw new DomainRuleViolationException(
                "CompletionRequest.userId.required",
                "User ID is required"
            );
        }
    }
    
    // Getters
    public RequestId getRequestId() {
        return getId();
    }
    
    public PromptContent getPrompt() {
        return prompt;
    }
    
    public Optional<ModelName> getPreferredModel() {
        return preferredModel;
    }
    
    public Optional<ProviderId> getPreferredProvider() {
        return preferredProvider;
    }
    
    public int getMaxTokens() {
        return maxTokens;
    }
    
    public double getTemperature() {
        return temperature;
    }
    
    public boolean isStreaming() {
        return streaming;
    }
    
    public boolean requiresSystemMessageSupport() {
        return systemMessageSupport;
    }
    
    public String getOrganizationId() {
        return organizationId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public RequestStatus getStatus() {
        return status;
    }
    
    public Optional<String> getErrorMessage() {
        return errorMessage;
    }
    
    public boolean isPending() {
        return status == RequestStatus.PENDING;
    }
    
    public boolean isProcessing() {
        return status == RequestStatus.PROCESSING;
    }
    
    public boolean isCompleted() {
        return status == RequestStatus.COMPLETED;
    }
    
    public boolean isFailed() {
        return status == RequestStatus.FAILED;
    }
    
    /**
     * Enum representing the status of a completion request.
     */
    public enum RequestStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}