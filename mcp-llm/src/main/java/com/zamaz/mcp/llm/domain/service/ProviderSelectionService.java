package com.zamaz.mcp.llm.domain.service;

import com.zamaz.mcp.common.domain.service.DomainService;
import com.zamaz.mcp.llm.domain.model.*;
import java.util.List;
import java.util.Optional;

/**
 * Domain service for selecting the best provider and model for LLM requests.
 */
public interface ProviderSelectionService extends DomainService {
    
    /**
     * Select the best provider and model based on the given criteria.
     */
    Optional<ProviderSelection> selectBestProvider(SelectionCriteria criteria);
    
    /**
     * Select providers that can handle the given requirements.
     */
    List<ProviderSelection> selectSuitableProviders(SelectionCriteria criteria);
    
    /**
     * Calculate a score for a provider/model combination based on criteria.
     */
    double calculateProviderScore(
        Provider provider, 
        LlmModel model, 
        SelectionCriteria criteria
    );
    
    /**
     * Check if a provider/model can satisfy the selection criteria.
     */
    boolean canSatisfyCriteria(
        Provider provider, 
        LlmModel model, 
        SelectionCriteria criteria
    );
    
    /**
     * Selection criteria for provider/model selection.
     */
    class SelectionCriteria {
        private final int requiredTokens;
        private final Optional<ProviderId> preferredProvider;
        private final Optional<ModelName> preferredModel;
        private final List<LlmModel.ModelCapability> requiredCapabilities;
        private final SelectionStrategy strategy;
        private final double maxCostThreshold;
        private final boolean requireHighestQuality;
        
        private SelectionCriteria(Builder builder) {
            this.requiredTokens = builder.requiredTokens;
            this.preferredProvider = builder.preferredProvider;
            this.preferredModel = builder.preferredModel;
            this.requiredCapabilities = List.copyOf(builder.requiredCapabilities);
            this.strategy = builder.strategy;
            this.maxCostThreshold = builder.maxCostThreshold;
            this.requireHighestQuality = builder.requireHighestQuality;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        // Getters
        public int getRequiredTokens() { return requiredTokens; }
        public Optional<ProviderId> getPreferredProvider() { return preferredProvider; }
        public Optional<ModelName> getPreferredModel() { return preferredModel; }
        public List<LlmModel.ModelCapability> getRequiredCapabilities() { return requiredCapabilities; }
        public SelectionStrategy getStrategy() { return strategy; }
        public double getMaxCostThreshold() { return maxCostThreshold; }
        public boolean isRequireHighestQuality() { return requireHighestQuality; }
        
        public static class Builder {
            private int requiredTokens = 0;
            private Optional<ProviderId> preferredProvider = Optional.empty();
            private Optional<ModelName> preferredModel = Optional.empty();
            private List<LlmModel.ModelCapability> requiredCapabilities = List.of();
            private SelectionStrategy strategy = SelectionStrategy.COST_OPTIMIZED;
            private double maxCostThreshold = Double.MAX_VALUE;
            private boolean requireHighestQuality = false;
            
            public Builder requiredTokens(int tokens) {
                this.requiredTokens = tokens;
                return this;
            }
            
            public Builder preferredProvider(Optional<ProviderId> provider) {
                this.preferredProvider = provider;
                return this;
            }
            
            public Builder preferredModel(Optional<ModelName> model) {
                this.preferredModel = model;
                return this;
            }
            
            public Builder requiredCapabilities(List<LlmModel.ModelCapability> capabilities) {
                this.requiredCapabilities = capabilities;
                return this;
            }
            
            public Builder strategy(SelectionStrategy strategy) {
                this.strategy = strategy;
                return this;
            }
            
            public Builder maxCostThreshold(double threshold) {
                this.maxCostThreshold = threshold;
                return this;
            }
            
            public Builder requireHighestQuality(boolean require) {
                this.requireHighestQuality = require;
                return this;
            }
            
            public SelectionCriteria build() {
                return new SelectionCriteria(this);
            }
        }
    }
    
    /**
     * Strategy for provider selection.
     */
    enum SelectionStrategy {
        COST_OPTIMIZED,     // Prefer cheapest option
        PERFORMANCE_FIRST,  // Prefer fastest/most capable
        QUALITY_FIRST,      // Prefer highest quality models
        BALANCED           // Balance cost, performance, and quality
    }
    
    /**
     * Result of provider selection containing provider and model.
     */
    record ProviderSelection(
        Provider provider,
        LlmModel model,
        double score,
        String reasoning
    ) {}
}