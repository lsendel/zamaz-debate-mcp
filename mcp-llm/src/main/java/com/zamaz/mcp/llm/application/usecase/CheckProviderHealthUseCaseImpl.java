package com.zamaz.mcp.llm.application.usecase;

import com.zamaz.mcp.common.application.exception.ResourceNotFoundException;
import com.zamaz.mcp.llm.application.command.CheckProviderHealthCommand;
import com.zamaz.mcp.llm.application.port.inbound.CheckProviderHealthUseCase;
import com.zamaz.mcp.llm.application.port.outbound.LlmProviderGateway;
import com.zamaz.mcp.llm.application.port.outbound.ProviderRepository;
import com.zamaz.mcp.llm.application.query.ProviderHealthResult;
import com.zamaz.mcp.llm.domain.model.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the check provider health use case.
 * Orchestrates health checking, status updates, and result compilation.
 */
public class CheckProviderHealthUseCaseImpl implements CheckProviderHealthUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(CheckProviderHealthUseCaseImpl.class);
    private static final Duration HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    
    private final ProviderRepository providerRepository;
    private final LlmProviderGateway providerGateway;
    
    public CheckProviderHealthUseCaseImpl(
            ProviderRepository providerRepository,
            LlmProviderGateway providerGateway
    ) {
        this.providerRepository = Objects.requireNonNull(providerRepository, "Provider repository cannot be null");
        this.providerGateway = Objects.requireNonNull(providerGateway, "Provider gateway cannot be null");
    }
    
    @Override
    public ProviderHealthResult execute(CheckProviderHealthCommand command) {
        logger.info("Checking health for provider: {} in org: {} (includeModels: {}, forceRefresh: {})", 
            command.providerId(), command.organizationId(), 
            command.includeModelStatus(), command.forceRefresh());
        
        Instant startTime = Instant.now();
        
        try {
            // Get provider from repository
            ProviderId providerId = ProviderId.of(command.providerId());
            Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Provider not found: " + command.providerId()
                ));
            
            // Check if we can use cached health status
            if (!command.forceRefresh() && isCachedHealthValid(provider)) {
                logger.debug("Using cached health status for provider: {}", command.providerId());
                return createResultFromCachedHealth(provider, command, startTime);
            }
            
            // Perform fresh health check
            ProviderHealthResult result = performHealthCheck(provider, command, startTime);
            
            // Update provider status in repository
            updateProviderStatus(provider, result);
            
            logger.info("Health check completed for provider: {} - status: {} in {}ms", 
                command.providerId(), result.status(), result.responseTime().toMillis());
            
            return result;
            
        } catch (ResourceNotFoundException e) {
            logger.warn("Provider not found during health check: {}", command.providerId());
            throw e;
        } catch (Exception e) {
            logger.error("Health check failed for provider: {}: {}", 
                command.providerId(), e.getMessage(), e);
            
            // Create error result
            return ProviderHealthResult.unhealthy(
                command.providerId(),
                "Unknown Provider",
                ProviderStatus.ERROR,
                "Health check failed: " + e.getMessage(),
                Duration.between(startTime, Instant.now())
            );
        }
    }
    
    private boolean isCachedHealthValid(Provider provider) {
        return provider.getLastHealthCheck()
            .map(lastCheck -> {
                Duration age = Duration.between(lastCheck, Instant.now());
                return age.compareTo(CACHE_TTL) <= 0;
            })
            .orElse(false);
    }
    
    private ProviderHealthResult createResultFromCachedHealth(
            Provider provider, 
            CheckProviderHealthCommand command,
            Instant startTime
    ) {
        Duration responseTime = Duration.between(startTime, Instant.now());
        
        List<ProviderHealthResult.ModelHealthInfo> modelHealth = command.includeModelStatus()
            ? createCachedModelHealth(provider)
            : Collections.emptyList();
        
        ProviderHealthResult.HealthMetrics metrics = createHealthMetrics(provider);
        
        if (provider.getStatus().isHealthy()) {
            return ProviderHealthResult.comprehensive(
                provider.getProviderId().value(),
                provider.getName(),
                provider.getStatus(),
                responseTime,
                modelHealth,
                metrics
            );
        } else {
            return ProviderHealthResult.unhealthy(
                provider.getProviderId().value(),
                provider.getName(),
                provider.getStatus(),
                provider.getHealthCheckMessage() != null ? provider.getHealthCheckMessage() : "Provider is not healthy",
                responseTime
            );
        }
    }
    
    private ProviderHealthResult performHealthCheck(
            Provider provider, 
            CheckProviderHealthCommand command,
            Instant startTime
    ) {
        try {
            // Perform health check via gateway
            LlmProviderGateway.ProviderHealthCheck healthCheck = providerGateway
                .checkProviderHealth(provider.getProviderId())
                .timeout(HEALTH_CHECK_TIMEOUT)
                .block();
            
            if (healthCheck == null) {
                throw new RuntimeException("Health check returned null result");
            }
            
            Duration responseTime = Duration.ofMillis(healthCheck.responseTimeMs());
            
            // Check individual model health if requested
            List<ProviderHealthResult.ModelHealthInfo> modelHealth = command.includeModelStatus()
                ? checkModelHealth(provider, healthCheck.status())
                : Collections.emptyList();
            
            // Create health metrics
            ProviderHealthResult.HealthMetrics metrics = createHealthMetrics(provider);
            
            // Create result based on health check status
            return createHealthResult(provider, healthCheck, responseTime, modelHealth, metrics);
            
        } catch (Exception e) {
            logger.error("Provider health check failed for {}: {}", provider.getProviderId(), e.getMessage(), e);
            
            Duration responseTime = Duration.between(startTime, Instant.now());
            return ProviderHealthResult.unhealthy(
                provider.getProviderId().value(),
                provider.getName(),
                ProviderStatus.ERROR,
                e.getMessage(),
                responseTime
            );
        }
    }
    
    private ProviderHealthResult createHealthResult(
            Provider provider,
            LlmProviderGateway.ProviderHealthCheck healthCheck,
            Duration responseTime,
            List<ProviderHealthResult.ModelHealthInfo> modelHealth,
            ProviderHealthResult.HealthMetrics metrics
    ) {
        ProviderStatus status = healthCheck.status();
        
        switch (status) {
            case AVAILABLE:
                return ProviderHealthResult.comprehensive(
                    provider.getProviderId().value(),
                    provider.getName(),
                    status,
                    responseTime,
                    modelHealth,
                    metrics
                );
                
            case DEGRADED:
                return ProviderHealthResult.degraded(
                    provider.getProviderId().value(),
                    provider.getName(),
                    responseTime,
                    healthCheck.message()
                );
                
            case RATE_LIMITED:
                // Extract retry-after from message if available
                Duration retryAfter = extractRetryAfter(healthCheck.message());
                return ProviderHealthResult.rateLimited(
                    provider.getProviderId().value(),
                    provider.getName(),
                    responseTime,
                    retryAfter
                );
                
            default:
                return ProviderHealthResult.unhealthy(
                    provider.getProviderId().value(),
                    provider.getName(),
                    status,
                    healthCheck.message(),
                    responseTime
                );
        }
    }
    
    private List<ProviderHealthResult.ModelHealthInfo> checkModelHealth(
            Provider provider, 
            ProviderStatus providerStatus
    ) {
        return provider.getModels().values().stream()
            .map(model -> checkIndividualModelHealth(model, providerStatus))
            .collect(Collectors.toList());
    }
    
    private ProviderHealthResult.ModelHealthInfo checkIndividualModelHealth(
            LlmModel model, 
            ProviderStatus providerStatus
    ) {
        // If provider is unhealthy, all models are considered unhealthy
        if (!providerStatus.isHealthy()) {
            return ProviderHealthResult.ModelHealthInfo.unhealthy(
                model.getModelName().value(),
                model.getDisplayName(),
                "Provider is not healthy"
            );
        }
        
        // Check model-specific status
        if (model.getStatus() == LlmModel.ModelStatus.AVAILABLE) {
            return ProviderHealthResult.ModelHealthInfo.healthy(
                model.getModelName().value(),
                model.getDisplayName(),
                Duration.ofMillis(50) // Simulated response time
            );
        } else {
            return ProviderHealthResult.ModelHealthInfo.unhealthy(
                model.getModelName().value(),
                model.getDisplayName(),
                "Model is " + model.getStatus().name().toLowerCase()
            );
        }
    }
    
    private List<ProviderHealthResult.ModelHealthInfo> createCachedModelHealth(Provider provider) {
        return provider.getModels().values().stream()
            .map(model -> {
                if (model.getStatus() == LlmModel.ModelStatus.AVAILABLE) {
                    return ProviderHealthResult.ModelHealthInfo.healthy(
                        model.getModelName().value(),
                        model.getDisplayName(),
                        Duration.ofMillis(50) // Cached response time
                    );
                } else {
                    return ProviderHealthResult.ModelHealthInfo.unhealthy(
                        model.getModelName().value(),
                        model.getDisplayName(),
                        "Model is " + model.getStatus().name().toLowerCase()
                    );
                }
            })
            .collect(Collectors.toList());
    }
    
    private ProviderHealthResult.HealthMetrics createHealthMetrics(Provider provider) {
        // In a real implementation, this would query actual metrics from a metrics store
        long totalChecks = 0L; // Would come from metrics store
        long successfulChecks = 0L; // Would come from metrics store
        Duration avgResponseTime = Duration.ofMillis(100); // Would come from metrics store
        
        return ProviderHealthResult.HealthMetrics.of(totalChecks, successfulChecks, avgResponseTime);
    }
    
    private void updateProviderStatus(Provider provider, ProviderHealthResult result) {
        try {
            // Update provider status based on health check result
            provider.updateStatus(
                result.status(),
                result.errorMessage().orElse("Health check completed")
            );
            
            // Save updated provider
            providerRepository.save(provider);
            
            logger.debug("Updated provider status for {}: {}", 
                provider.getProviderId(), result.status());
                
        } catch (Exception e) {
            logger.warn("Failed to update provider status for {}: {}", 
                provider.getProviderId(), e.getMessage());
            // Don't fail the health check due to status update issues
        }
    }
    
    private Duration extractRetryAfter(String message) {
        // Simple extraction of retry-after from message
        if (message != null && message.contains("retry after")) {
            try {
                // Look for patterns like "retry after 60 seconds"
                String[] parts = message.toLowerCase().split("\\s+");
                for (int i = 0; i < parts.length - 1; i++) {
                    if (parts[i].equals("after") && i > 0 && parts[i-1].equals("retry")) {
                        if (i + 1 < parts.length) {
                            long seconds = Long.parseLong(parts[i + 1]);
                            return Duration.ofSeconds(seconds);
                        }
                    }
                }
            } catch (NumberFormatException e) {
                logger.debug("Could not parse retry-after duration from message: {}", message);
            }
        }
        
        // Default retry after 5 minutes for rate limiting
        return Duration.ofMinutes(5);
    }
}