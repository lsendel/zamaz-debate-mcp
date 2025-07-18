package com.zamaz.mcp.common.linting;

import java.util.Map;

/**
 * Represents a single linting issue found during code analysis.
 */
public class LintingIssue {
    
    private final String id;
    private final LintingSeverity severity;
    private final String message;
    private final String file;
    private final int line;
    private final int column;
    private final String rule;
    private final String linter;
    private final boolean autoFixable;
    private final String suggestion;
    private final Map<String, Object> metadata;
    
    private LintingIssue(Builder builder) {
        this.id = builder.id;
        this.severity = builder.severity;
        this.message = builder.message;
        this.file = builder.file;
        this.line = builder.line;
        this.column = builder.column;
        this.rule = builder.rule;
        this.linter = builder.linter;
        this.autoFixable = builder.autoFixable;
        this.suggestion = builder.suggestion;
        this.metadata = builder.metadata;
    }
    
    public String getId() {
        return id;
    }
    
    public LintingSeverity getSeverity() {
        return severity;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getFile() {
        return file;
    }
    
    public int getLine() {
        return line;
    }
    
    public int getColumn() {
        return column;
    }
    
    public String getRule() {
        return rule;
    }
    
    public String getLinter() {
        return linter;
    }
    
    public boolean isAutoFixable() {
        return autoFixable;
    }
    
    public String getSuggestion() {
        return suggestion;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String id;
        private LintingSeverity severity;
        private String message;
        private String file;
        private int line;
        private int column;
        private String rule;
        private String linter;
        private boolean autoFixable;
        private String suggestion;
        private Map<String, Object> metadata;
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder severity(LintingSeverity severity) {
            this.severity = severity;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder file(String file) {
            this.file = file;
            return this;
        }
        
        public Builder line(int line) {
            this.line = line;
            return this;
        }
        
        public Builder column(int column) {
            this.column = column;
            return this;
        }
        
        public Builder rule(String rule) {
            this.rule = rule;
            return this;
        }
        
        public Builder linter(String linter) {
            this.linter = linter;
            return this;
        }
        
        public Builder autoFixable(boolean autoFixable) {
            this.autoFixable = autoFixable;
            return this;
        }
        
        public Builder suggestion(String suggestion) {
            this.suggestion = suggestion;
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public LintingIssue build() {
            return new LintingIssue(this);
        }
    }
}