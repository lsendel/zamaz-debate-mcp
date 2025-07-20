package com.zamaz.sample.geospatial.dto;

import com.zamaz.geospatial.domain.entity.StamfordAddress;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class StamfordAddressDto {
    private String id;
    private String streetAddress;
    private String city;
    private String state;
    private String zipCode;
    private double latitude;
    private double longitude;
    private String neighborhood;
    private String type;
    private Map<String, Object> latestTelemetry;
    
    public static StamfordAddressDto from(StamfordAddress address) {
        return StamfordAddressDto.builder()
                .id(address.getId())
                .streetAddress(address.getStreetAddress())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getZipCode())
                .latitude(address.getLocation().getLatitude())
                .longitude(address.getLocation().getLongitude())
                .neighborhood(address.getNeighborhood())
                .type(address.getType().name())
                .build();
    }
}