package com.zamaz.mcp.common.linting;

import java.util.Map;

/**
 * Configuration for a specific linter.
 */
public class LinterConfig {
    
    private final String name;
    private final boolean enabled;
    private final String configFile;
    private final Map<String, Object> properties;
    
    private LinterConfig(Builder builder) {
        this.name = builder.name;
        this.enabled = builder.enabled;
        this.configFile = builder.configFile;
        this.properties = builder.properties;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public String getConfigFile() {
        return configFile;
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String name;
        private boolean enabled = true;
        private String configFile;
        private Map<String, Object> properties;
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        public Builder configFile(String configFile) {
            this.configFile = configFile;
            return this;
        }
        
        public Builder properties(Map<String, Object> properties) {
            this.properties = properties;
            return this;
        }
        
        public LinterConfig build() {
            return new LinterConfig(this);
        }
    }
}