package com.zamaz.mcp.llm.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;

/**
 * Value object representing the operational status of an LLM provider.
 */
public enum ProviderStatus implements ValueObject {
    AVAILABLE("available", "Provider is healthy and accepting requests"),
    DEGRADED("degraded", "Provider is experiencing issues but still functional"),
    UNAVAILABLE("unavailable", "Provider is down or not responding"),
    RATE_LIMITED("rate_limited", "Provider is rate limiting requests"),
    MAINTENANCE("maintenance", "Provider is under maintenance");
    
    private final String value;
    private final String description;
    
    ProviderStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isHealthy() {
        return this == AVAILABLE || this == DEGRADED;
    }
    
    public boolean isAvailable() {
        return this == AVAILABLE;
    }
    
    public boolean canAcceptRequests() {
        return this == AVAILABLE || this == DEGRADED;
    }
    
    public static ProviderStatus fromValue(String value) {
        for (ProviderStatus status : ProviderStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid provider status: " + value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}