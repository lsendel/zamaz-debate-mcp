package com.zamaz.debatetree.domain.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class DebateTreeNode {
    @NonNull
    private final String debateId;
    
    @NonNull
    private final String title;
    
    private final String description;
    
    @NonNull
    private final DebateStatus status;
    
    private final String parentDebateId;
    
    @Builder.Default
    private final List<DebateTreeNode> children = new ArrayList<>();
    
    private final int participantCount;
    
    private final int responseCount;
    
    private final Instant createdAt;
    
    private final Instant lastActivityAt;
    
    @Builder.Default
    private final int depth = 0;
    
    private final double relevanceScore;
    
    public void addChild(DebateTreeNode child) {
        children.add(child);
    }
    
    public boolean hasChildren() {
        return !children.isEmpty();
    }
    
    public int getTotalDescendants() {
        int count = children.size();
        for (DebateTreeNode child : children) {
            count += child.getTotalDescendants();
        }
        return count;
    }
    
    public enum DebateStatus {
        ACTIVE("Active", "#4CAF50"),
        CLOSED("Closed", "#9E9E9E"),
        PENDING("Pending", "#FF9800"),
        ARCHIVED("Archived", "#607D8B");
        
        private final String displayName;
        private final String color;
        
        DebateStatus(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getColor() {
            return color;
        }
    }
}