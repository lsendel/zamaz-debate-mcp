package com.zamaz.mcp.controller.controller;

import com.zamaz.mcp.controller.dto.DebateDto;
import com.zamaz.mcp.controller.dto.TemplateDto;
import com.zamaz.mcp.controller.dto.TemplateValidationResult;
import com.zamaz.mcp.controller.service.TemplateBasedDebateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for template-based debate operations.
 * Provides endpoints for creating debates using templates.
 */
@RestController
@RequestMapping("/api/debates/templates")
@RequiredArgsConstructor
@Slf4j
public class TemplateDebateController {
    
    private final TemplateBasedDebateService templateDebateService;
    
    /**
     * Get all debate templates for an organization.
     * 
     * @param organizationId The organization ID
     * @return List of debate templates
     */
    @GetMapping
    public ResponseEntity<List<TemplateDto>> getDebateTemplates(
            @RequestParam String organizationId) {
        
        log.info("Getting debate templates for organization: {}", organizationId);
        
        List<TemplateDto> templates = templateDebateService.getDebateTemplates(organizationId);
        return ResponseEntity.ok(templates);
    }
    
    /**
     * Create a debate using a template.
     * 
     * @param organizationId The organization ID
     * @param templateId The template ID
     * @param request Request body containing template variables
     * @return Created debate
     */
    @PostMapping("/{templateId}/create")
    public ResponseEntity<DebateDto> createDebateFromTemplate(
            @RequestParam String organizationId,
            @PathVariable String templateId,
            @RequestBody Map<String, Object> request) {
        
        log.info("Creating debate from template: {} for organization: {}", templateId, organizationId);
        
        // Extract template variables from request
        @SuppressWarnings("unchecked")
        Map<String, Object> templateVariables = (Map<String, Object>) request.getOrDefault("variables", Map.of());
        
        DebateDto debate = templateDebateService.createDebateFromTemplate(
                organizationId, templateId, templateVariables);
        
        return ResponseEntity.ok(debate);
    }
    
    /**
     * Preview a debate template with variables.
     * 
     * @param organizationId The organization ID
     * @param templateId The template ID
     * @param request Request body containing template variables
     * @return Preview of the rendered template
     */
    @PostMapping("/{templateId}/preview")
    public ResponseEntity<Map<String, Object>> previewTemplate(
            @RequestParam String organizationId,
            @PathVariable String templateId,
            @RequestBody Map<String, Object> request) {
        
        log.debug("Previewing template: {} for organization: {}", templateId, organizationId);
        
        // Extract template variables from request
        @SuppressWarnings("unchecked")
        Map<String, Object> templateVariables = (Map<String, Object>) request.getOrDefault("variables", Map.of());
        
        Map<String, Object> preview = templateDebateService.previewTemplate(
                organizationId, templateId, templateVariables);
        
        return ResponseEntity.ok(preview);
    }
    
    /**
     * Validate template variables for a debate template.
     * 
     * @param organizationId The organization ID
     * @param templateId The template ID
     * @param request Request body containing template variables
     * @return Validation result
     */
    @PostMapping("/{templateId}/validate")
    public ResponseEntity<TemplateValidationResult> validateTemplateVariables(
            @RequestParam String organizationId,
            @PathVariable String templateId,
            @RequestBody Map<String, Object> request) {
        
        log.debug("Validating template variables for: {} in organization: {}", templateId, organizationId);
        
        // Extract template variables from request
        @SuppressWarnings("unchecked")
        Map<String, Object> templateVariables = (Map<String, Object>) request.getOrDefault("variables", Map.of());
        
        TemplateValidationResult result = templateDebateService.validateTemplateVariables(
                organizationId, templateId, templateVariables);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Get template information for a specific template.
     * 
     * @param organizationId The organization ID
     * @param templateId The template ID
     * @return Template information
     */
    @GetMapping("/{templateId}")
    public ResponseEntity<TemplateDto> getTemplate(
            @RequestParam String organizationId,
            @PathVariable String templateId) {
        
        log.debug("Getting template: {} for organization: {}", templateId, organizationId);
        
        // This endpoint delegates to the template service through the debate service
        List<TemplateDto> templates = templateDebateService.getDebateTemplates(organizationId);
        
        return templates.stream()
                .filter(template -> template.getId().equals(templateId))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Health check endpoint for template integration.
     * 
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = Map.of(
                "status", "UP",
                "templateServiceIntegration", "active",
                "timestamp", java.time.Instant.now()
        );
        return ResponseEntity.ok(health);
    }
}