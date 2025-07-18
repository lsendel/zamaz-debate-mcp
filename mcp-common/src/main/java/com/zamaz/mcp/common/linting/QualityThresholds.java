package com.zamaz.mcp.common.linting;

/**
 * Quality thresholds for linting operations.
 */
public class QualityThresholds {
    
    private final int maxErrors;
    private final int maxWarnings;
    private final double minCoverage;
    private final int maxComplexity;
    
    public QualityThresholds() {
        this(0, 10, 0.80, 10);
    }
    
    public QualityThresholds(int maxErrors, int maxWarnings, double minCoverage, int maxComplexity) {
        this.maxErrors = maxErrors;
        this.maxWarnings = maxWarnings;
        this.minCoverage = minCoverage;
        this.maxComplexity = maxComplexity;
    }
    
    public int getMaxErrors() {
        return maxErrors;
    }
    
    public int getMaxWarnings() {
        return maxWarnings;
    }
    
    public double getMinCoverage() {
        return minCoverage;
    }
    
    public int getMaxComplexity() {
        return maxComplexity;
    }
    
    /**
     * Check if the given metrics meet the quality thresholds.
     *
     * @param errorCount number of errors
     * @param warningCount number of warnings
     * @param coverage code coverage percentage
     * @param complexity maximum complexity
     * @return true if thresholds are met
     */
    public boolean meetsThresholds(int errorCount, int warningCount, double coverage, int complexity) {
        return errorCount <= maxErrors &&
               warningCount <= maxWarnings &&
               coverage >= minCoverage &&
               complexity <= maxComplexity;
    }
}