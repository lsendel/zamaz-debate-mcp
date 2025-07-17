package com.zamaz.mcp.github.analyzer.model;

import com.zamaz.mcp.github.analyzer.model.ModelEnums.InsightType;
import com.zamaz.mcp.github.analyzer.model.ModelEnums.InsightSeverity;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Represents an insight or recommendation about repository structure
 */
@Data
@Builder
public class StructureInsight {
    
    /**
     * Type of insight
     */
    private InsightType type;
    
    /**
     * Severity level
     */
    private InsightSeverity severity;
    
    /**
     * Insight title
     */
    private String title;
    
    /**
     * Detailed description
     */
    private String description;
    
    /**
     * Recommended action
     */
    private String recommendation;
    
    /**
     * Category of the insight
     */
    private String category;
    
    /**
     * Confidence score (0.0 to 1.0)
     */
    private double confidence;
    
    /**
     * Impact score (0.0 to 1.0)
     */
    private double impact;
    
    /**
     * Effort required to address (0.0 to 1.0)
     */
    private double effort;
    
    /**
     * Files affected by this insight
     */
    private List<String> affectedFiles;
    
    /**
     * Packages/modules affected
     */
    private List<String> affectedPackages;
    
    /**
     * Dependencies affected
     */
    private List<String> affectedDependencies;
    
    /**
     * Related insights
     */
    private List<String> relatedInsights;
    
    /**
     * Evidence supporting this insight
     */
    private List<String> evidence;
    
    /**
     * Metrics that support this insight
     */
    private Map<String, Double> supportingMetrics;
    
    /**
     * Additional metadata
     */
    private Map<String, String> metadata;
    
    /**
     * Timestamp when insight was generated
     */
    private long timestamp;
    
    /**
     * Whether this insight can be auto-fixed
     */
    private boolean autoFixable;
    
    /**
     * Auto-fix command or script
     */
    private String autoFixCommand;
    
    /**
     * Documentation links
     */
    private List<String> documentationLinks;
    
    /**
     * Get priority score based on severity, impact, and confidence
     */
    public double getPriorityScore() {
        double severityWeight = switch (severity) {
            case CRITICAL -> 1.0;
            case ERROR -> 0.8;
            case WARNING -> 0.6;
            case INFO -> 0.4;
        };
        
        return (severityWeight * 0.4) + (impact * 0.4) + (confidence * 0.2);
    }
    
    /**
     * Get estimated time to fix in hours
     */
    public double getEstimatedTimeToFix() {
        // Base time estimates by severity
        double baseHours = switch (severity) {
            case CRITICAL -> 8.0;
            case ERROR -> 4.0;
            case WARNING -> 2.0;
            case INFO -> 1.0;
        };
        
        // Adjust by effort
        return baseHours * effort;
    }
    
    /**
     * Check if this is a high-priority insight
     */
    public boolean isHighPriority() {
        return getPriorityScore() > 0.7;
    }
    
    /**
     * Check if this insight affects multiple files
     */
    public boolean isWidespread() {
        return affectedFiles != null && affectedFiles.size() > 5;
    }
    
    /**
     * Get severity color for UI display
     */
    public String getSeverityColor() {
        return switch (severity) {
            case CRITICAL -> "#DC2626"; // Red
            case ERROR -> "#EA580C"; // Orange
            case WARNING -> "#D97706"; // Amber
            case INFO -> "#2563EB"; // Blue
        };
    }
    
    /**
     * Get severity icon for UI display
     */
    public String getSeverityIcon() {
        return switch (severity) {
            case CRITICAL -> "🚨";
            case ERROR -> "❌";
            case WARNING -> "⚠️";
            case INFO -> "ℹ️";
        };
    }
    
    /**
     * Get formatted description for display
     */
    public String getFormattedDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(getSeverityIcon()).append(" ").append(title).append("\n\n");
        sb.append(description);
        
        if (recommendation != null && !recommendation.isEmpty()) {
            sb.append("\n\n💡 Recommendation: ").append(recommendation);
        }
        
        if (evidence != null && !evidence.isEmpty()) {
            sb.append("\n\n📊 Evidence:\n");
            for (String e : evidence) {
                sb.append("• ").append(e).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Convert to summary format
     */
    public InsightSummary toSummary() {
        return InsightSummary.builder()
                .type(type)
                .severity(severity)
                .title(title)
                .confidence(confidence)
                .impact(impact)
                .effort(effort)
                .priorityScore(getPriorityScore())
                .estimatedTimeToFix(getEstimatedTimeToFix())
                .affectedFileCount(affectedFiles != null ? affectedFiles.size() : 0)
                .autoFixable(autoFixable)
                .build();
    }
}