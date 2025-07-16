package com.zamaz.mcp.controller.service;

import com.zamaz.mcp.controller.client.TemplateServiceClient;
import com.zamaz.mcp.controller.dto.DebateDto;
import com.zamaz.mcp.controller.dto.TemplateDto;
import com.zamaz.mcp.controller.dto.TemplateValidationResult;
import com.zamaz.mcp.common.exception.BusinessException;
import com.zamaz.mcp.security.annotation.RequiresPermission;
import com.zamaz.mcp.security.rbac.Permission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for creating debates using templates.
 * Integrates with the template service to support template-based debate creation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateBasedDebateService {
    
    private final TemplateServiceClient templateServiceClient;
    private final DebateService debateService;
    
    /**
     * Get all debate templates for an organization.
     * 
     * @param organizationId The organization ID
     * @return List of debate templates
     */
    @RequiresPermission(Permission.TEMPLATE_LIST)
    public List<TemplateDto> getDebateTemplates(String organizationId) {
        log.debug("Getting debate templates for organization: {}", organizationId);
        
        if (!templateServiceClient.isTemplateServiceAvailable()) {
            log.warn("Template service not available for organization: {}", organizationId);
            return List.of();
        }
        
        return templateServiceClient.getDebateTemplates(organizationId);
    }
    
    /**
     * Create a debate using a template.
     * 
     * @param organizationId The organization ID
     * @param templateId The template ID
     * @param templateVariables Variables to populate the template
     * @return Created debate
     */
    @RequiresPermission(Permission.DEBATE_CREATE)
    public DebateDto createDebateFromTemplate(String organizationId, String templateId, 
                                            Map<String, Object> templateVariables) {
        log.info("Creating debate from template: {} for organization: {}", templateId, organizationId);
        
        // Check if template service is available
        if (!templateServiceClient.isTemplateServiceAvailable()) {
            throw new BusinessException("Template service is not available", "SERVICE_UNAVAILABLE");
        }
        
        // Get and validate template
        TemplateDto template = templateServiceClient.getTemplate(templateId, organizationId)
                .orElseThrow(() -> BusinessException.notFound("Template", templateId));
        
        // Validate template
        TemplateValidationResult validationResult = templateServiceClient.validateTemplate(templateId, organizationId);
        if (!validationResult.isValid()) {
            throw BusinessException.validationFailed("template", "Template validation failed: " + validationResult.getMessage());
        }
        
        // Create debate request from template
        DebateDto.CreateDebateRequest debateRequest = createDebateRequestFromTemplate(template, templateVariables);
        
        // Create the debate
        DebateDto debate = debateService.createDebate(debateRequest);
        
        log.info("Created debate {} from template {} for organization: {}", 
                debate.getId(), templateId, organizationId);
        
        return debate;
    }
    
    /**
     * Preview a debate template with variables.
     * 
     * @param organizationId The organization ID
     * @param templateId The template ID
     * @param templateVariables Variables to populate the template
     * @return Preview of the rendered template
     */
    @RequiresPermission(Permission.TEMPLATE_READ)
    public Map<String, Object> previewTemplate(String organizationId, String templateId, 
                                             Map<String, Object> templateVariables) {
        log.debug("Previewing template: {} for organization: {}", templateId, organizationId);
        
        // Get template
        TemplateDto template = templateServiceClient.getTemplate(templateId, organizationId)
                .orElseThrow(() -> BusinessException.notFound("Template", templateId));
        
        // Render template content
        Optional<String> renderedContent = templateServiceClient.renderTemplate(
                templateId, organizationId, templateVariables);
        
        Map<String, Object> preview = new HashMap<>();
        preview.put("template", template);
        preview.put("variables", templateVariables);
        preview.put("renderedContent", renderedContent.orElse("Failed to render template"));
        preview.put("valid", renderedContent.isPresent());
        
        return preview;
    }
    
    /**
     * Validate template variables for a debate template.
     * 
     * @param organizationId The organization ID
     * @param templateId The template ID
     * @param templateVariables Variables to validate
     * @return Validation result
     */
    @RequiresPermission(Permission.TEMPLATE_READ)
    public TemplateValidationResult validateTemplateVariables(String organizationId, String templateId, 
                                                             Map<String, Object> templateVariables) {
        log.debug("Validating template variables for: {} in organization: {}", templateId, organizationId);
        
        // Get template
        TemplateDto template = templateServiceClient.getTemplate(templateId, organizationId)
                .orElseThrow(() -> BusinessException.notFound("Template", templateId));
        
        // Validate required variables
        List<TemplateValidationResult.ValidationError> errors = new java.util.ArrayList<>();
        
        for (TemplateDto.TemplateVariable variable : template.getVariables()) {
            if (variable.isRequired() && !templateVariables.containsKey(variable.getName())) {
                errors.add(TemplateValidationResult.ValidationError.builder()
                        .field(variable.getName())
                        .code("REQUIRED")
                        .message("Required variable is missing")
                        .build());
            }
        }
        
        if (!errors.isEmpty()) {
            return TemplateValidationResult.invalid(errors);
        }
        
        return TemplateValidationResult.valid();
    }
    
    /**
     * Create a debate request from template configuration.
     */
    private DebateDto.CreateDebateRequest createDebateRequestFromTemplate(TemplateDto template, 
                                                               Map<String, Object> templateVariables) {
        
        // Extract debate configuration from template metadata
        Map<String, Object> metadata = template.getMetadata();
        
        // Build settings JSON
        Map<String, Object> settings = new HashMap<>();
        if (metadata != null) {
            settings.put("turnTimeoutSeconds", getIntValue(metadata, "turnTimeoutSeconds", 300));
            settings.put("rules", getStringValue(metadata, "rules", ""));
            settings.put("isPublic", getBooleanValue(metadata, "isPublic", false));
        }
        
        // Add template reference to settings
        settings.put("templateId", template.getId());
        settings.put("templateName", template.getName());
        settings.put("templateVersion", template.getVersion());
        settings.put("templateVariables", templateVariables);
        
        // Convert settings to JsonNode
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode settingsJson = mapper.valueToTree(settings);
        
        DebateDto.CreateDebateRequest request = DebateDto.CreateDebateRequest.builder()
                .organizationId(java.util.UUID.fromString(template.getOrganizationId()))
                .title(getStringValue(templateVariables, "title", "Debate from " + template.getName()))
                .description(getStringValue(templateVariables, "description", template.getDescription()))
                .topic(getStringValue(templateVariables, "topic", template.getName()))
                .format(getStringValue(metadata, "format", "standard"))
                .maxRounds(getIntValue(metadata, "maxRounds", 5))
                .settings(settingsJson)
                .build();
        
        return request;
    }
    
    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    private Integer getIntValue(Map<String, Object> map, String key, Integer defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    private Boolean getBooleanValue(Map<String, Object> map, String key, Boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
}