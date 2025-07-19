package com.example.workflow.graphql.ports;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Inbound port for map operations
 * Handles OpenMapTiles integration for North America and Europe
 */
public interface MapService {
    
    /**
     * Get map tiles for a specific region
     */
    Mono<MapTileResponse> getMapTiles(RegionInput region);
    
    /**
     * Get spatial telemetry overlays for map
     */
    Flux<SpatialOverlay> getSpatialOverlays(RegionInput region);
    
    /**
     * Search for locations within region
     */
    Flux<LocationResult> searchLocations(LocationSearchInput input);
    
    /**
     * Get geographic features for region
     */
    Flux<GeographicFeature> getGeographicFeatures(RegionInput region);
    
    /**
     * Validate geographic coordinates
     */
    Mono<Boolean> validateCoordinates(double lat, double lng);
}

/**
 * Input and response objects for map operations
 */
record RegionInput(
    double minLat,
    double maxLat,
    double minLng,
    double maxLng,
    int zoomLevel
) {}

record MapTileResponse(
    String tileUrl,
    String format,
    int zoomLevel,
    RegionInput bounds
) {}

record SpatialOverlay(
    String id,
    String type,
    double lat,
    double lng,
    Object data,
    String style
) {}

record LocationSearchInput(
    String query,
    RegionInput region,
    int maxResults
) {}

record LocationResult(
    String name,
    String address,
    double lat,
    double lng,
    String type
) {}

record GeographicFeature(
    String id,
    String name,
    String type,
    Object geometry,
    Object properties
) {}