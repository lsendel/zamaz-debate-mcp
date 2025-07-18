package com.zamaz.mcp.llm.domain.model;

import com.zamaz.mcp.common.domain.model.AggregateRoot;
import com.zamaz.mcp.common.domain.exception.DomainRuleViolationException;
import com.zamaz.mcp.llm.domain.event.ProviderStatusChangedEvent;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregate root representing an LLM provider with its models and capabilities.
 */
public class Provider extends AggregateRoot<ProviderId> {
    
    private final String name;
    private final String displayName;
    private final String description;
    private final String baseUrl;
    private final Map<ModelName, LlmModel> models;
    private ProviderStatus status;
    private final Instant createdAt;
    private Instant lastHealthCheck;
    private String healthCheckMessage;
    private final int priority; // Lower number = higher priority
    
    private Provider(
            ProviderId providerId,
            String name,
            String displayName,
            String description,
            String baseUrl,
            Map<ModelName, LlmModel> models,
            ProviderStatus status,
            Instant createdAt,
            Instant lastHealthCheck,
            String healthCheckMessage,
            int priority
    ) {
        super(providerId);
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.displayName = Objects.requireNonNull(displayName, "Display name cannot be null");
        this.description = Objects.requireNonNull(description, "Description cannot be null");
        this.baseUrl = Objects.requireNonNull(baseUrl, "Base URL cannot be null");
        this.models = new HashMap<>(models);
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created at cannot be null");
        this.lastHealthCheck = lastHealthCheck;
        this.healthCheckMessage = healthCheckMessage;
        this.priority = priority;
    }
    
    public static Provider create(
            ProviderId providerId,
            String name,
            String displayName,
            String description,
            String baseUrl,
            int priority
    ) {
        Instant now = Instant.now();
        return new Provider(
            providerId,
            name,
            displayName,
            description,
            baseUrl,
            new HashMap<>(),
            ProviderStatus.AVAILABLE,
            now,
            now,
            "Provider initialized",
            priority
        );
    }
    
    public void addModel(LlmModel model) {
        Objects.requireNonNull(model, "Model cannot be null");
        
        if (!model.getProviderId().equals(getId())) {
            throw new DomainRuleViolationException(
                "Provider.model.mismatch",
                "Model provider ID does not match this provider"
            );
        }
        
        models.put(model.getModelName(), model);
    }
    
    public void removeModel(ModelName modelName) {
        Objects.requireNonNull(modelName, "Model name cannot be null");
        models.remove(modelName);
    }
    
    public Optional<LlmModel> getModel(ModelName modelName) {
        return Optional.ofNullable(models.get(modelName));
    }
    
    public List<LlmModel> getAvailableModels() {
        return models.values().stream()
            .filter(LlmModel::isAvailable)
            .collect(Collectors.toList());
    }
    
    public List<LlmModel> getModelsWithCapability(LlmModel.ModelCapability capability) {
        return models.values().stream()
            .filter(model -> model.hasCapability(capability))
            .filter(LlmModel::isAvailable)
            .collect(Collectors.toList());
    }
    
    public Optional<LlmModel> selectBestModel(int requiredTokens, LlmModel.ModelCapability... capabilities) {
        return models.values().stream()
            .filter(model -> model.canHandleRequest(requiredTokens))
            .filter(model -> Arrays.stream(capabilities).allMatch(model::hasCapability))
            .min(Comparator.comparing(LlmModel::getInputTokenCost));
    }
    
    public void updateStatus(ProviderStatus newStatus, String message) {
        Objects.requireNonNull(newStatus, "Status cannot be null");
        
        if (this.status != newStatus) {
            ProviderStatus oldStatus = this.status;
            this.status = newStatus;
            this.lastHealthCheck = Instant.now();
            this.healthCheckMessage = message != null ? message : "";
            
            registerEvent(new ProviderStatusChangedEvent(
                getId().value(),
                oldStatus.getValue(),
                newStatus.getValue(),
                message,
                lastHealthCheck
            ));
        }
    }
    
    public boolean isHealthy() {
        return status.isHealthy();
    }
    
    public boolean canAcceptRequests() {
        return status.canAcceptRequests() && !getAvailableModels().isEmpty();
    }
    
    public boolean hasModel(ModelName modelName) {
        return models.containsKey(modelName);
    }
    
    public int getModelCount() {
        return models.size();
    }
    
    public int getAvailableModelCount() {
        return getAvailableModels().size();
    }
    
    @Override
    public void validateInvariants() {
        if (name == null || name.trim().isEmpty()) {
            throw new DomainRuleViolationException(
                "Provider.name.required",
                "Provider name is required"
            );
        }
        
        if (displayName == null || displayName.trim().isEmpty()) {
            throw new DomainRuleViolationException(
                "Provider.displayName.required",
                "Provider display name is required"
            );
        }
        
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new DomainRuleViolationException(
                "Provider.baseUrl.required",
                "Provider base URL is required"
            );
        }
        
        if (priority < 0) {
            throw new DomainRuleViolationException(
                "Provider.priority.invalid",
                "Provider priority cannot be negative"
            );
        }
        
        // Validate all models
        models.values().forEach(LlmModel::validateInvariants);
    }
    
    // Getters
    public ProviderId getProviderId() {
        return getId();
    }
    
    public String getName() {
        return name;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public ProviderStatus getStatus() {
        return status;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getLastHealthCheck() {
        return lastHealthCheck;
    }
    
    public String getHealthCheckMessage() {
        return healthCheckMessage;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public Map<ModelName, LlmModel> getModels() {
        return Collections.unmodifiableMap(models);
    }
}