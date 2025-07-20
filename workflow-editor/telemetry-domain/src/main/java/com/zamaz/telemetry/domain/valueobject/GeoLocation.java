package com.zamaz.telemetry.domain.valueobject;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class GeoLocation {
    private final double latitude;
    private final double longitude;
    private final Double altitude;
    
    private GeoLocation(double latitude, double longitude, Double altitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
        
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
    }
    
    public static GeoLocation of(double latitude, double longitude) {
        return new GeoLocation(latitude, longitude, null);
    }
    
    public static GeoLocation of(double latitude, double longitude, double altitude) {
        return new GeoLocation(latitude, longitude, altitude);
    }
    
    public double distanceTo(@NonNull GeoLocation other) {
        // Haversine formula for calculating distance between two points
        double earthRadius = 6371; // km
        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(this.latitude)) * Math.cos(Math.toRadians(other.latitude)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return earthRadius * c;
    }
    
    public String toWKT() {
        return String.format("POINT(%f %f)", longitude, latitude);
    }
    
    @Override
    public String toString() {
        return String.format("%.6f,%.6f", latitude, longitude);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        GeoLocation that = (GeoLocation) o;
        return Double.compare(that.latitude, latitude) == 0 &&
               Double.compare(that.longitude, longitude) == 0 &&
               Double.compare(that.altitude != null ? that.altitude : 0, 
                           altitude != null ? altitude : 0) == 0;
    }
    
    @Override
    public int hashCode() {
        int result = Double.hashCode(latitude);
        result = 31 * result + Double.hashCode(longitude);
        result = 31 * result + (altitude != null ? Double.hashCode(altitude) : 0);
        return result;
    }
}