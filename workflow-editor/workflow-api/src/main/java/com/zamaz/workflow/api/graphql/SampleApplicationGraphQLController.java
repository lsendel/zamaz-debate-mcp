package com.zamaz.workflow.api.graphql;

import com.zamaz.sample.geospatial.dto.GeospatialSampleResponse;
import com.zamaz.sample.geospatial.dto.StamfordAddressDto;
import com.zamaz.sample.geospatial.service.GeospatialSampleApplicationService;
import com.zamaz.workflow.api.graphql.type.MapTiles;
import com.zamaz.workflow.api.graphql.type.MapTile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SampleApplicationGraphQLController {
    private final GeospatialSampleApplicationService geospatialService;
    
    @QueryMapping
    @PreAuthorize("hasRole('WORKFLOW_USER')")
    public MapTiles mapTiles(@Argument GeoRegion region) {
        log.debug("Fetching map tiles for region: {}", region);
        
        // Generate map tile URLs for the requested region
        List<MapTile> tiles = generateMapTiles(region);
        
        return MapTiles.builder()
                .tiles(tiles)
                .bounds(region.getBounds())
                .zoomLevel(region.getZoomLevel())
                .build();
    }
    
    @QueryMapping(name = "geospatialSample")
    @PreAuthorize("hasRole('WORKFLOW_USER')")
    public GeospatialSampleResponse getGeospatialSample() {
        log.debug("Fetching geospatial sample data");
        return geospatialService.initializeSample();
    }
    
    @QueryMapping(name = "stamfordAddresses")
    @PreAuthorize("hasRole('WORKFLOW_USER')")
    public List<StamfordAddressDto> getStamfordAddresses() {
        log.debug("Fetching Stamford addresses with telemetry");
        return geospatialService.getAddressesWithTelemetry();
    }
    
    @QueryMapping(name = "telemetryStatistics")
    @PreAuthorize("hasRole('WORKFLOW_USER')")
    public Map<String, Object> getTelemetryStatistics(@Argument List<String> deviceIds) {
        log.debug("Fetching telemetry statistics for devices: {}", deviceIds);
        // This would return real-time statistics
        return Map.of(
                "deviceCount", deviceIds.size(),
                "dataRate", 10.0 * deviceIds.size(), // 10Hz per device
                "status", "active"
        );
    }
    
    private List<MapTile> generateMapTiles(GeoRegion region) {
        List<MapTile> tiles = new ArrayList<>();
        
        // Calculate tile coordinates based on region bounds and zoom level
        int zoom = region.getZoomLevel();
        double north = region.getBounds().getNorth();
        double south = region.getBounds().getSouth();
        double east = region.getBounds().getEast();
        double west = region.getBounds().getWest();
        
        // Convert lat/lon to tile numbers
        int xMin = lon2tile(west, zoom);
        int xMax = lon2tile(east, zoom);
        int yMin = lat2tile(north, zoom);
        int yMax = lat2tile(south, zoom);
        
        // Generate tile URLs
        for (int x = xMin; x <= xMax; x++) {
            for (int y = yMin; y <= yMax; y++) {
                MapTile tile = MapTile.builder()
                        .url(String.format("https://tile.openstreetmap.org/%d/%d/%d.png", zoom, x, y))
                        .x(x)
                        .y(y)
                        .z(zoom)
                        .build();
                tiles.add(tile);
            }
        }
        
        return tiles;
    }
    
    private int lon2tile(double lon, int zoom) {
        return (int) Math.floor((lon + 180) / 360 * (1 << zoom));
    }
    
    private int lat2tile(double lat, int zoom) {
        return (int) Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 
                1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1 << zoom));
    }
    
    @lombok.Data
    public static class GeoRegion {
        private GeoBounds bounds;
        private int zoomLevel;
    }
    
    @lombok.Data
    public static class GeoBounds {
        private double north;
        private double south;
        private double east;
        private double west;
    }
}