package com.zamaz.workflow.domain.valueobject;

import lombok.NonNull;
import lombok.Value;

@Value
public class OrganizationId {
    @NonNull
    String value;
    
    private OrganizationId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("OrganizationId cannot be null or empty");
        }
        this.value = value;
    }
    
    public static OrganizationId of(String value) {
        return new OrganizationId(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}