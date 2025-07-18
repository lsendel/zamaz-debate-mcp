package com.zamaz.mcp.common.linting;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Context object containing configuration and settings for linting operations.
 */
public class LintingContext {
    
    private final Path projectRoot;
    private final LintingConfiguration configuration;
    private final Map<String, Object> properties;
    private final List<String> excludePatterns;
    private final boolean parallelExecution;
    private final boolean autoFix;
    
    private LintingContext(Builder builder) {
        this.projectRoot = builder.projectRoot;
        this.configuration = builder.configuration;
        this.properties = builder.properties;
        this.excludePatterns = builder.excludePatterns;
        this.parallelExecution = builder.parallelExecution;
        this.autoFix = builder.autoFix;
    }
    
    public Path getProjectRoot() {
        return projectRoot;
    }
    
    public LintingConfiguration getConfiguration() {
        return configuration;
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    public List<String> getExcludePatterns() {
        return excludePatterns;
    }
    
    public boolean isParallelExecution() {
        return parallelExecution;
    }
    
    public boolean isAutoFix() {
        return autoFix;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Path projectRoot;
        private LintingConfiguration configuration;
        private Map<String, Object> properties;
        private List<String> excludePatterns;
        private boolean parallelExecution = true;
        private boolean autoFix = false;
        
        public Builder projectRoot(Path projectRoot) {
            this.projectRoot = projectRoot;
            return this;
        }
        
        public Builder configuration(LintingConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }
        
        public Builder properties(Map<String, Object> properties) {
            this.properties = properties;
            return this;
        }
        
        public Builder excludePatterns(List<String> excludePatterns) {
            this.excludePatterns = excludePatterns;
            return this;
        }
        
        public Builder parallelExecution(boolean parallelExecution) {
            this.parallelExecution = parallelExecution;
            return this;
        }
        
        public Builder autoFix(boolean autoFix) {
            this.autoFix = autoFix;
            return this;
        }
        
        public LintingContext build() {
            return new LintingContext(this);
        }
    }
}