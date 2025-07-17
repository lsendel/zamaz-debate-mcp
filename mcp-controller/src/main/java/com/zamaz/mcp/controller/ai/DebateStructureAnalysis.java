package com.zamaz.mcp.controller.ai;

import lombok.Builder;
import lombok.Data;

/**
 * Analysis of debate structure and flow
 */
@Data
@Builder
public class DebateStructureAnalysis {
    
    private double balanceScore; // How balanced participation is
    private double engagementScore; // Level of engagement between participants
    private double progressionScore; // Quality of argument progression
    private int totalRounds;
    private int totalResponses;
    private double averageResponseLength;
    
    // Default constructor for empty analysis
    public DebateStructureAnalysis() {
        this.balanceScore = 0.0;
        this.engagementScore = 0.0;
        this.progressionScore = 0.0;
        this.totalRounds = 0;
        this.totalResponses = 0;
        this.averageResponseLength = 0.0;
    }
}