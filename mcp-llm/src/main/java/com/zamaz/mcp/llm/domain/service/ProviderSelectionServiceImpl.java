package com.zamaz.mcp.llm.domain.service;

import com.zamaz.mcp.llm.domain.model.*;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of provider selection domain service.
 * Contains pure business logic for selecting optimal providers and models.
 */
public class ProviderSelectionServiceImpl implements ProviderSelectionService {
    
    private static final double PREFERRED_PROVIDER_BONUS = 10.0;
    private static final double PREFERRED_MODEL_BONUS = 15.0;
    private static final double HEALTH_PENALTY = -20.0;
    
    @Override
    public Optional<ProviderSelection> selectBestProvider(SelectionCriteria criteria) {
        return selectSuitableProviders(criteria)
            .stream()
            .max(Comparator.comparingDouble(ProviderSelection::score));
    }
    
    @Override
    public List<ProviderSelection> selectSuitableProviders(SelectionCriteria criteria) {
        // This would typically get providers from repository via dependency injection
        // For now, returning empty list - actual implementation would inject repository
        return List.of();
    }
    
    @Override
    public double calculateProviderScore(
            Provider provider, 
            LlmModel model, 
            SelectionCriteria criteria
    ) {
        if (!canSatisfyCriteria(provider, model, criteria)) {
            return -1.0; // Invalid option
        }
        
        double score = 0.0;
        
        // Base score from strategy
        score += calculateStrategyScore(provider, model, criteria);
        
        // Preference bonuses
        if (criteria.getPreferredProvider().isPresent() && 
            criteria.getPreferredProvider().get().equals(provider.getProviderId())) {
            score += PREFERRED_PROVIDER_BONUS;
        }
        
        if (criteria.getPreferredModel().isPresent() && 
            criteria.getPreferredModel().get().equals(model.getModelName())) {
            score += PREFERRED_MODEL_BONUS;
        }
        
        // Health penalty
        if (!provider.isHealthy()) {
            score += HEALTH_PENALTY;
        }
        
        // Priority bonus (lower priority number = higher score)
        score += (10 - provider.getPriority());
        
        return score;
    }
    
    @Override
    public boolean canSatisfyCriteria(
            Provider provider, 
            LlmModel model, 
            SelectionCriteria criteria
    ) {
        // Provider must be healthy and accepting requests
        if (!provider.canAcceptRequests()) {
            return false;
        }
        
        // Model must be available
        if (!model.isAvailable()) {
            return false;
        }
        
        // Check token capacity
        if (!model.canHandleRequest(criteria.getRequiredTokens())) {
            return false;
        }
        
        // Check required capabilities
        for (LlmModel.ModelCapability capability : criteria.getRequiredCapabilities()) {
            if (!model.hasCapability(capability)) {
                return false;
            }
        }
        
        // Check cost threshold if specified
        if (criteria.getMaxCostThreshold() < Double.MAX_VALUE) {
            TokenUsage estimatedCost = model.calculateCost(
                criteria.getRequiredTokens() / 2, // Rough input estimate
                criteria.getRequiredTokens() / 2  // Rough output estimate
            );
            if (estimatedCost.totalCost().doubleValue() > criteria.getMaxCostThreshold()) {
                return false;
            }
        }
        
        return true;
    }
    
    private double calculateStrategyScore(
            Provider provider, 
            LlmModel model, 
            SelectionCriteria criteria
    ) {
        return switch (criteria.getStrategy()) {
            case COST_OPTIMIZED -> calculateCostScore(model, criteria);
            case PERFORMANCE_FIRST -> calculatePerformanceScore(model);
            case QUALITY_FIRST -> calculateQualityScore(model);
            case BALANCED -> calculateBalancedScore(model, criteria);
        };
    }
    
    private double calculateCostScore(LlmModel model, SelectionCriteria criteria) {
        // Lower cost = higher score
        TokenUsage estimatedCost = model.calculateCost(
            criteria.getRequiredTokens() / 2,
            criteria.getRequiredTokens() / 2
        );
        
        double cost = estimatedCost.totalCost().doubleValue();
        
        // Invert cost for scoring (lower cost = higher score)
        // Use logarithmic scale to handle wide cost ranges
        return Math.max(0, 100 - (Math.log10(cost * 1000 + 1) * 10));
    }
    
    private double calculatePerformanceScore(LlmModel model) {
        double score = 50.0; // Base score
        
        // Bonus for capabilities
        if (model.hasCapability(LlmModel.ModelCapability.STREAMING)) {
            score += 10.0;
        }
        if (model.hasCapability(LlmModel.ModelCapability.SYSTEM_MESSAGES)) {
            score += 5.0;
        }
        if (model.hasCapability(LlmModel.ModelCapability.VISION)) {
            score += 15.0;
        }
        
        // Higher token limit = better performance
        score += Math.min(30.0, model.getMaxTokens() / 1000.0);
        
        return score;
    }
    
    private double calculateQualityScore(LlmModel model) {
        // This would ideally use actual quality metrics
        // For now, use model name as a proxy for quality
        double score = 50.0;
        
        String modelName = model.getModelName().value().toLowerCase();
        
        // Simple heuristic based on model naming
        if (modelName.contains("opus") || modelName.contains("gpt-4")) {
            score += 40.0;
        } else if (modelName.contains("sonnet") || modelName.contains("gpt-3.5")) {
            score += 25.0;
        } else if (modelName.contains("haiku")) {
            score += 15.0;
        }
        
        // Bonus for advanced capabilities
        if (model.hasCapability(LlmModel.ModelCapability.VISION)) {
            score += 10.0;
        }
        
        return score;
    }
    
    private double calculateBalancedScore(LlmModel model, SelectionCriteria criteria) {
        double costScore = calculateCostScore(model, criteria) * 0.4;
        double performanceScore = calculatePerformanceScore(model) * 0.3;
        double qualityScore = calculateQualityScore(model) * 0.3;
        
        return costScore + performanceScore + qualityScore;
    }
}