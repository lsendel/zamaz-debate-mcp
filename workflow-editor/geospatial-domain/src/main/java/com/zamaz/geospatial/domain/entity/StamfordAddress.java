package com.zamaz.geospatial.domain.entity;

import com.zamaz.telemetry.domain.valueobject.GeoLocation;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.UUID;

@Getter
@Builder
public class StamfordAddress {
    @NonNull
    @Builder.Default
    private final String id = UUID.randomUUID().toString();
    
    @NonNull
    private final String streetAddress;
    
    @NonNull
    private final String city = "Stamford";
    
    @NonNull
    private final String state = "CT";
    
    @NonNull
    private final String zipCode;
    
    @NonNull
    private final GeoLocation location;
    
    private final String neighborhood;
    
    @Builder.Default
    private final AddressType type = AddressType.RESIDENTIAL;
    
    public String getFullAddress() {
        return String.format("%s, %s, %s %s", streetAddress, city, state, zipCode);
    }
    
    public enum AddressType {
        RESIDENTIAL,
        COMMERCIAL,
        INDUSTRIAL,
        MUNICIPAL
    }
}