package com.zamaz.mcp.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for Template information.
 * Represents a template configuration for debates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateDto {
    
    private String id;
    private String organizationId;
    private String name;
    private String description;
    private TemplateCategory category;
    private TemplateType type;
    private TemplateStatus status;
    private String content;
    private List<TemplateVariable> variables;
    private Integer version;
    private String parentId;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Template categories
     */
    public enum TemplateCategory {
        DEBATE,
        PROMPT,
        RESPONSE,
        EVALUATION,
        MODERATION,
        SYSTEM,
        CUSTOM
    }
    
    /**
     * Template types
     */
    public enum TemplateType {
        JINJA2,
        MARKDOWN,
        PLAIN_TEXT,
        JSON
    }
    
    /**
     * Template status
     */
    public enum TemplateStatus {
        DRAFT,
        ACTIVE,
        ARCHIVED,
        DEPRECATED
    }
    
    /**
     * Template variable definition
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateVariable {
        private String name;
        private String type;
        private boolean required;
        private Object defaultValue;
        private String description;
        private Map<String, Object> validation;
    }
}