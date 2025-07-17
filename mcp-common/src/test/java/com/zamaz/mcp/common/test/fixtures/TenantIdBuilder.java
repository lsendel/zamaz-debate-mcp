package com.zamaz.mcp.common.test.fixtures;

import com.zamaz.mcp.common.domain.model.valueobject.TenantId;
import java.util.UUID;

/**
 * Test data builder for TenantId value objects.
 */
public class TenantIdBuilder implements TestDataBuilder<TenantId> {
    
    private UUID value;
    
    private TenantIdBuilder() {
        this.value = UUID.randomUUID();
    }
    
    public static TenantIdBuilder aTenantId() {
        return new TenantIdBuilder();
    }
    
    public TenantIdBuilder withValue(UUID value) {
        this.value = value;
        return this;
    }
    
    public TenantIdBuilder withValue(String value) {
        this.value = UUID.fromString(value);
        return this;
    }
    
    @Override
    public TenantId build() {
        return new TenantId(value);
    }
    
    /**
     * Creates a builder with a known test UUID.
     */
    public static TenantIdBuilder testTenantId() {
        return new TenantIdBuilder()
            .withValue("00000000-0000-0000-0000-000000000100");
    }
    
    /**
     * Creates the default test tenant ID used across tests.
     */
    public static TenantId defaultTestTenant() {
        return testTenantId().build();
    }
}