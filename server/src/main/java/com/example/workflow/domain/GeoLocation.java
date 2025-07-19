package com.example.workflow.domain;

/**
 * Value object representing a geographic location
 * Supports spatial operations and validation
 */
public record GeoLocation(double latitude, double longitude) {
    
    public GeoLocation {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees: " + latitude);
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees: " + longitude);
        }
    }
    
    /**
     * Create location from coordinates
     */
    public static GeoLocation of(double latitude, double longitude) {
        return new GeoLocation(latitude, longitude);
    }
    
    /**
     * Generate random location within Stamford, Connecticut bounds
     */
    public static GeoLocation randomStamfordLocation() {
        // Stamford, CT approximate bounds
        double minLat = 41.0200;
        double maxLat = 41.0900;
        double minLng = -73.5800;
        double maxLng = -73.5000;
        
        double lat = minLat + Math.random() * (maxLat - minLat);
        double lng = minLng + Math.random() * (maxLng - minLng);
        
        return new GeoLocation(lat, lng);
    }
    
    /**
     * Generate random location within North America bounds
     */
    public static GeoLocation randomNorthAmericaLocation() {
        // North America approximate bounds
        double minLat = 25.0;
        double maxLat = 70.0;
        double minLng = -170.0;
        double maxLng = -50.0;
        
        double lat = minLat + Math.random() * (maxLat - minLat);
        double lng = minLng + Math.random() * (maxLng - minLng);
        
        return new GeoLocation(lat, lng);
    }
    
    /**
     * Generate random location within Europe bounds
     */
    public static GeoLocation randomEuropeLocation() {
        // Europe approximate bounds
        double minLat = 35.0;
        double maxLat = 70.0;
        double minLng = -10.0;
        double maxLng = 40.0;
        
        double lat = minLat + Math.random() * (maxLat - minLat);
        double lng = minLng + Math.random() * (maxLng - minLng);
        
        return new GeoLocation(lat, lng);
    }
    
    /**
     * Calculate distance to another location in kilometers using Haversine formula
     */
    public double distanceToKm(GeoLocation other) {
        final double R = 6371.0; // Earth's radius in kilometers
        
        double lat1Rad = Math.toRadians(this.latitude);
        double lat2Rad = Math.toRadians(other.latitude);
        double deltaLatRad = Math.toRadians(other.latitude - this.latitude);
        double deltaLngRad = Math.toRadians(other.longitude - this.longitude);
        
        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLngRad / 2) * Math.sin(deltaLngRad / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    /**
     * Calculate distance to another location in meters
     */
    public double distanceToMeters(GeoLocation other) {
        return distanceToKm(other) * 1000.0;
    }
    
    /**
     * Check if location is within specified radius of another location
     */
    public boolean isWithinRadius(GeoLocation center, double radiusKm) {
        return distanceToKm(center) <= radiusKm;
    }
    
    /**
     * Check if location is within bounding box
     */
    public boolean isWithinBounds(double minLat, double maxLat, double minLng, double maxLng) {
        return latitude >= minLat && latitude <= maxLat &&
               longitude >= minLng && longitude <= maxLng;
    }
    
    /**
     * Check if location is in North America region
     */
    public boolean isInNorthAmerica() {
        return isWithinBounds(25.0, 70.0, -170.0, -50.0);
    }
    
    /**
     * Check if location is in Europe region
     */
    public boolean isInEurope() {
        return isWithinBounds(35.0, 70.0, -10.0, 40.0);
    }
    
    /**
     * Check if location is in Stamford, Connecticut area
     */
    public boolean isInStamford() {
        return isWithinBounds(41.0200, 41.0900, -73.5800, -73.5000);
    }
    
    /**
     * Get bearing to another location in degrees
     */
    public double bearingTo(GeoLocation other) {
        double lat1Rad = Math.toRadians(this.latitude);
        double lat2Rad = Math.toRadians(other.latitude);
        double deltaLngRad = Math.toRadians(other.longitude - this.longitude);
        
        double y = Math.sin(deltaLngRad) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                   Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLngRad);
        
        double bearingRad = Math.atan2(y, x);
        double bearingDeg = Math.toDegrees(bearingRad);
        
        return (bearingDeg + 360) % 360; // Normalize to 0-360 degrees
    }
    
    /**
     * Convert to Well-Known Text (WKT) format for PostGIS
     */
    public String toWKT() {
        return String.format("POINT(%f %f)", longitude, latitude);
    }
    
    /**
     * Get latitude in degrees
     */
    public double lat() {
        return latitude;
    }
    
    /**
     * Get longitude in degrees
     */
    public double lng() {
        return longitude;
    }
    
    @Override
    public String toString() {
        return String.format("GeoLocation(%.6f, %.6f)", latitude, longitude);
    }
}