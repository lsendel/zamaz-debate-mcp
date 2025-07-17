package com.zamaz.mcp.common.audit;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Statistics for event sourcing-based audit trail
 */
@Data
@Builder
public class EventSourcingAuditStatistics {
    
    /**
     * Total number of events in the period
     */
    private int totalEvents;
    
    /**
     * Organization ID these statistics are for
     */
    private String organizationId;
    
    /**
     * Start date of the statistics period
     */
    private LocalDateTime fromDate;
    
    /**
     * End date of the statistics period
     */
    private LocalDateTime toDate;
    
    /**
     * Number of events by event type
     */
    private Map<String, Integer> eventsByType;
    
    /**
     * Number of events by user
     */
    private Map<String, Integer> eventsByUser;
    
    /**
     * Number of events by aggregate type
     */
    private Map<String, Integer> eventsByAggregateType;
    
    /**
     * Number of events by hour of day
     */
    private Map<Integer, Integer> eventsByHour;
    
    /**
     * Number of events by day of week
     */
    private Map<Integer, Integer> eventsByDayOfWeek;
    
    /**
     * Most active users (top 10)
     */
    private Map<String, Integer> topUsers;
    
    /**
     * Most common event types (top 10)
     */
    private Map<String, Integer> topEventTypes;
    
    /**
     * Average events per day in the period
     */
    private double averageEventsPerDay;
    
    /**
     * Peak events per hour
     */
    private int peakEventsPerHour;
    
    /**
     * Number of unique users who performed actions
     */
    private int uniqueUsers;
    
    /**
     * Number of unique correlation IDs (indicating unique business processes)
     */
    private int uniqueCorrelationIds;
    
    /**
     * Calculate event rate per hour
     */
    public double getEventRatePerHour() {
        if (fromDate == null || toDate == null) {
            return 0.0;
        }
        
        long hours = java.time.Duration.between(fromDate, toDate).toHours();
        if (hours == 0) {
            return totalEvents;
        }
        
        return (double) totalEvents / hours;
    }
    
    /**
     * Calculate most active event type
     */
    public String getMostActiveEventType() {
        if (eventsByType == null || eventsByType.isEmpty()) {
            return null;
        }
        
        return eventsByType.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    /**
     * Calculate most active user
     */
    public String getMostActiveUser() {
        if (eventsByUser == null || eventsByUser.isEmpty()) {
            return null;
        }
        
        return eventsByUser.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    /**
     * Calculate user activity percentage
     */
    public double getUserActivityPercentage(String userId) {
        if (eventsByUser == null || !eventsByUser.containsKey(userId) || totalEvents == 0) {
            return 0.0;
        }
        
        return (double) eventsByUser.get(userId) / totalEvents * 100;
    }
    
    /**
     * Calculate event type percentage
     */
    public double getEventTypePercentage(String eventType) {
        if (eventsByType == null || !eventsByType.containsKey(eventType) || totalEvents == 0) {
            return 0.0;
        }
        
        return (double) eventsByType.get(eventType) / totalEvents * 100;
    }
    
    /**
     * Get peak activity hour
     */
    public Integer getPeakActivityHour() {
        if (eventsByHour == null || eventsByHour.isEmpty()) {
            return null;
        }
        
        return eventsByHour.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    /**
     * Get most active day of week
     */
    public Integer getMostActiveDayOfWeek() {
        if (eventsByDayOfWeek == null || eventsByDayOfWeek.isEmpty()) {
            return null;
        }
        
        return eventsByDayOfWeek.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    /**
     * Calculate diversity score (how evenly distributed events are across types)
     */
    public double getDiversityScore() {
        if (eventsByType == null || eventsByType.isEmpty() || totalEvents == 0) {
            return 0.0;
        }
        
        // Shannon entropy calculation
        double entropy = 0.0;
        for (int count : eventsByType.values()) {
            if (count > 0) {
                double probability = (double) count / totalEvents;
                entropy -= probability * Math.log(probability) / Math.log(2);
            }
        }
        
        // Normalize to 0-1 range
        double maxEntropy = Math.log(eventsByType.size()) / Math.log(2);
        return maxEntropy > 0 ? entropy / maxEntropy : 0.0;
    }
}