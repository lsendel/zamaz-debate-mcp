package com.zamaz.mcp.common.linting;

import java.util.List;
import java.util.Map;

/**
 * Configuration class for linting operations.
 */
public class LintingConfiguration {
    
    private final Map<String, LinterConfig> linters;
    private final List<String> excludePatterns;
    private final Map<String, Object> globalSettings;
    private final QualityThresholds thresholds;
    private final boolean parallelExecution;
    private final int maxThreads;
    
    private LintingConfiguration(Builder builder) {
        this.linters = builder.linters;
        this.excludePatterns = builder.excludePatterns;
        this.globalSettings = builder.globalSettings;
        this.thresholds = builder.thresholds;
        this.parallelExecution = builder.parallelExecution;
        this.maxThreads = builder.maxThreads;
    }
    
    public Map<String, LinterConfig> getLinters() {
        return linters;
    }
    
    public List<String> getExcludePatterns() {
        return excludePatterns;
    }
    
    public Map<String, Object> getGlobalSettings() {
        return globalSettings;
    }
    
    public QualityThresholds getThresholds() {
        return thresholds;
    }
    
    public boolean isParallelExecution() {
        return parallelExecution;
    }
    
    public int getMaxThreads() {
        return maxThreads;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Map<String, LinterConfig> linters;
        private List<String> excludePatterns;
        private Map<String, Object> globalSettings;
        private QualityThresholds thresholds;
        private boolean parallelExecution = true;
        private int maxThreads = 4;
        
        public Builder linters(Map<String, LinterConfig> linters) {
            this.linters = linters;
            return this;
        }
        
        public Builder excludePatterns(List<String> excludePatterns) {
            this.excludePatterns = excludePatterns;
            return this;
        }
        
        public Builder globalSettings(Map<String, Object> globalSettings) {
            this.globalSettings = globalSettings;
            return this;
        }
        
        public Builder thresholds(QualityThresholds thresholds) {
            this.thresholds = thresholds;
            return this;
        }
        
        public Builder parallelExecution(boolean parallelExecution) {
            this.parallelExecution = parallelExecution;
            return this;
        }
        
        public Builder maxThreads(int maxThreads) {
            this.maxThreads = maxThreads;
            return this;
        }
        
        public LintingConfiguration build() {
            return new LintingConfiguration(this);
        }
    }
}