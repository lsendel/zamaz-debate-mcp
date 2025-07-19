package com.example.workflow.domain;

/**
 * Value object representing a geographic bounding box
 */
public record BoundingBox(double minLatitude, double maxLatitude, double minLongitude, double maxLongitude) {
    
    public BoundingBox {
        if (minLatitude < -90.0 || minLatitude > 90.0) {
            throw new IllegalArgumentException("Min latitude must be between -90 and 90 degrees: " + minLatitude);
        }
        if (maxLatitude < -90.0 || maxLatitude > 90.0) {
            throw new IllegalArgumentException("Max latitude must be between -90 and 90 degrees: " + maxLatitude);
        }
        if (minLongitude < -180.0 || minLongitude > 180.0) {
            throw new IllegalArgumentException("Min longitude must be between -180 and 180 degrees: " + minLongitude);
        }
        if (maxLongitude < -180.0 || maxLongitude > 180.0) {
            throw new IllegalArgumentException("Max longitude must be between -180 and 180 degrees: " + maxLongitude);
        }
        if (minLatitude > maxLatitude) {
            throw new IllegalArgumentException("Min latitude cannot be greater than max latitude");
        }
        if (minLongitude > maxLongitude) {
            throw new IllegalArgumentException("Min longitude cannot be greater than max longitude");
        }
    }
    
    /**
     * Create bounding box from coordinates
     */
    public static BoundingBox of(double minLat, double maxLat, double minLng, double maxLng) {
        return new BoundingBox(minLat, maxLat, minLng, maxLng);
    }
    
    /**
     * Create bounding box for Stamford, Connecticut
     */
    public static BoundingBox stamfordCT() {
        return new BoundingBox(41.0200, 41.0900, -73.5800, -73.5000);
    }
    
    /**
     * Create bounding box for North America
     */
    public static BoundingBox northAmerica() {
        return new BoundingBox(25.0, 70.0, -170.0, -50.0);
    }
    
    /**
     * Create bounding box for Europe
     */
    public static BoundingBox europe() {
        return new BoundingBox(35.0, 70.0, -10.0, 40.0);
    }
    
    /**
     * Check if location is contained within this bounding box
     */
    public boolean contains(GeoLocation location) {
        return location.latitude() >= minLatitude &&
               location.latitude() <= maxLatitude &&
               location.longitude() >= minLongitude &&
               location.longitude() <= maxLongitude;
    }
    
    /**
     * Check if this bounding box intersects with another
     */
    public boolean intersects(BoundingBox other) {
        return !(other.maxLatitude < this.minLatitude ||
                 other.minLatitude > this.maxLatitude ||
                 other.maxLongitude < this.minLongitude ||
                 other.minLongitude > this.maxLongitude);
    }
    
    /**
     * Get center point of bounding box
     */
    public GeoLocation getCenter() {
        double centerLat = (minLatitude + maxLatitude) / 2.0;
        double centerLng = (minLongitude + maxLongitude) / 2.0;
        return new GeoLocation(centerLat, centerLng);
    }
    
    /**
     * Get width of bounding box in degrees
     */
    public double getWidthDegrees() {
        return maxLongitude - minLongitude;
    }
    
    /**
     * Get height of bounding box in degrees
     */
    public double getHeightDegrees() {
        return maxLatitude - minLatitude;
    }
    
    /**
     * Expand bounding box by specified margin in degrees
     */
    public BoundingBox expand(double marginDegrees) {
        return new BoundingBox(
            Math.max(-90.0, minLatitude - marginDegrees),
            Math.min(90.0, maxLatitude + marginDegrees),
            Math.max(-180.0, minLongitude - marginDegrees),
            Math.min(180.0, maxLongitude + marginDegrees)
        );
    }
    
    @Override
    public String toString() {
        return String.format("BoundingBox(%.6f,%.6f,%.6f,%.6f)", 
                           minLatitude, maxLatitude, minLongitude, maxLongitude);
    }
}