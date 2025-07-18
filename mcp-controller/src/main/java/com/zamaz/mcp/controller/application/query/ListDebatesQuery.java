package com.zamaz.mcp.controller.application.query;

import com.zamaz.mcp.controller.domain.model.DebateStatus;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Query to list debates with optional filtering.
 */
public record ListDebatesQuery(
    Set<DebateStatus> statuses,
    String topicFilter,
    Integer limit,
    Integer offset
) {
    
    public ListDebatesQuery {
        Objects.requireNonNull(statuses, "Statuses cannot be null");
        
        if (limit != null && limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        
        if (offset != null && offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }
        
        if (topicFilter != null && topicFilter.trim().isEmpty()) {
            throw new IllegalArgumentException("Topic filter cannot be empty");
        }
    }
    
    public static ListDebatesQuery all() {
        return new ListDebatesQuery(Set.of(DebateStatus.values()), null, null, null);
    }
    
    public static ListDebatesQuery active() {
        return new ListDebatesQuery(
            Set.of(DebateStatus.IN_PROGRESS, DebateStatus.INITIALIZED), 
            null, null, null
        );
    }
    
    public static ListDebatesQuery completed() {
        return new ListDebatesQuery(
            Set.of(DebateStatus.COMPLETED, DebateStatus.ARCHIVED), 
            null, null, null
        );
    }
    
    public static ListDebatesQuery byStatus(DebateStatus status) {
        Objects.requireNonNull(status, "Status cannot be null");
        return new ListDebatesQuery(Set.of(status), null, null, null);
    }
    
    public static ListDebatesQuery withPagination(int limit, int offset) {
        return new ListDebatesQuery(Set.of(DebateStatus.values()), null, limit, offset);
    }
    
    public static ListDebatesQuery withTopicFilter(String topicFilter) {
        Objects.requireNonNull(topicFilter, "Topic filter cannot be null");
        return new ListDebatesQuery(Set.of(DebateStatus.values()), topicFilter.trim(), null, null);
    }
    
    public ListDebatesQuery withStatus(DebateStatus status) {
        Objects.requireNonNull(status, "Status cannot be null");
        return new ListDebatesQuery(Set.of(status), topicFilter, limit, offset);
    }
    
    public ListDebatesQuery withStatuses(Set<DebateStatus> newStatuses) {
        Objects.requireNonNull(newStatuses, "Statuses cannot be null");
        return new ListDebatesQuery(Set.copyOf(newStatuses), topicFilter, limit, offset);
    }
    
    public ListDebatesQuery withTopicFilter(String newTopicFilter) {
        return new ListDebatesQuery(statuses, newTopicFilter, limit, offset);
    }
    
    public ListDebatesQuery withLimit(int newLimit) {
        return new ListDebatesQuery(statuses, topicFilter, newLimit, offset);
    }
    
    public ListDebatesQuery withOffset(int newOffset) {
        return new ListDebatesQuery(statuses, topicFilter, limit, newOffset);
    }
    
    public Optional<String> getTopicFilter() {
        return Optional.ofNullable(topicFilter);
    }
    
    public Optional<Integer> getLimit() {
        return Optional.ofNullable(limit);
    }
    
    public Optional<Integer> getOffset() {
        return Optional.ofNullable(offset);
    }
    
    public boolean hasStatusFilter() {
        return !statuses.isEmpty() && statuses.size() < DebateStatus.values().length;
    }
    
    public boolean hasTopicFilter() {
        return topicFilter != null && !topicFilter.trim().isEmpty();
    }
    
    public boolean hasPagination() {
        return limit != null || offset != null;
    }
}