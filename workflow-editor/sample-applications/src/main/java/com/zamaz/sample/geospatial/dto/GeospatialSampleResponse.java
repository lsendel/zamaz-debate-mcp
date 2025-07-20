package com.zamaz.sample.geospatial.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class GeospatialSampleResponse {
    private List<StamfordAddressDto> addresses;
    private Map<String, Object> statistics;
    private boolean telemetryActive;
}