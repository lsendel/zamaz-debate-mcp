package com.zamaz.mcp.controller.ai;

import lombok.Builder;
import lombok.Data;

/**
 * Structure analysis of an argument
 */
@Data
@Builder
public class ArgumentStructure {
    
    private boolean hasClaim;
    private boolean hasEvidence;
    private boolean hasWarrant;
    private boolean hasCounterargument;
    private boolean hasRebuttal;
    private double structureScore;
}