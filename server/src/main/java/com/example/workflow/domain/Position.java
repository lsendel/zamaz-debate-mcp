package com.example.workflow.domain;

/**
 * Value object representing a 2D position
 */
public record Position(double x, double y) {
    
    /**
     * Create position at origin
     */
    public static Position origin() {
        return new Position(0, 0);
    }
    
    /**
     * Create position from coordinates
     */
    public static Position of(double x, double y) {
        return new Position(x, y);
    }
    
    /**
     * Calculate distance to another position
     */
    public double distanceTo(Position other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Move position by offset
     */
    public Position move(double deltaX, double deltaY) {
        return new Position(x + deltaX, y + deltaY);
    }
    
    @Override
    public String toString() {
        return String.format("Position(%.2f, %.2f)", x, y);
    }
}