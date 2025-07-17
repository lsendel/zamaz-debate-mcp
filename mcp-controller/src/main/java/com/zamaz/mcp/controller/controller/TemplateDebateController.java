package com.zamaz.mcp.controller.controller;

import com.zamaz.mcp.controller.dto.DebateDto;
import com.zamaz.mcp.controller.dto.TemplateDto;
import com.zamaz.mcp.controller.dto.TemplateValidationResult;
import com.zamaz.mcp.controller.service.TemplateBasedDebateService;
import org.springframework.security.core.Authentication;

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
     * @param authentication The authenticated user's information
     * @return List of debate templates
     */
    @GetMapping
    public ResponseEntity<List<TemplateDto>> getDebateTemplates(
            Authentication authentication) {
        
        String organizationId = (String) authentication.getDetails(); // Assuming organizationId is in details
        log.debug("Getting debate templates for organization: {}", organizationId);
        
        TemplateDto template = templateDebateService.getDebateTemplate(organizationId, templateId);
        
        return ResponseEntity.ok(template);
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
            Authentication authentication,
            @PathVariable String templateId,
            @RequestBody TemplateVariablesRequest request) {
        
        String organizationId = (String) authentication.getDetails(); // Assuming organizationId is in details
        log.debug("Creating debate from template: {} for organization: {}", templateId, organizationId);
        
        Map<String, Object> templateVariables = request.getVariables();
        
        DebateDto debate = templateDebateService.createDebateFromTemplate(
                organizationId, templateId, templateVariables);
        
        return ResponseEntity.ok(debate);
    }
    
    /**
     * Preview a debate template with variables.
     * 
     * @param authentication The authenticated user's information
     * @param templateId The template ID
     * @param request Request body containing template variables
     * @return Preview of the rendered template
     */
    @PostMapping("/{templateId}/preview")
    public ResponseEntity<Map<String, Object>> previewTemplate(
            Authentication authentication,
            @PathVariable String templateId,
            @RequestBody TemplateVariablesRequest request) {
        
        String organizationId = (String) authentication.getDetails();
        log.debug("Previewing template: {} for organization: {}", templateId, organizationId);
        
        Map<String, Object> templateVariables = request.getVariables();
        
        Map<String, Object> preview = templateDebateService.previewTemplate(
                organizationId, templateId, templateVariables);
        
        return ResponseEntity.ok(preview);
    }
    
    /**
     * Validate template variables for a debate template.
     * 
     * @param authentication The authenticated user's information
     * @param templateId The template ID
     * @param request Request body containing template variables
     * @return Validation result
     */
    @PostMapping("/{templateId}/validate")
    public ResponseEntity<TemplateValidationResult> validateTemplateVariables(
            Authentication authentication,
            @PathVariable String templateId,
            @RequestBody TemplateVariablesRequest request) {
        
        String organizationId = (String) authentication.getDetails();
        log.debug("Validating template variables for: {} in organization: {}", templateId, organizationId);
        
        Map<String, Object> templateVariables = request.getVariables();
        
        TemplateValidationResult result = templateDebateService.validateTemplateVariables(
                organizationId, templateId, templateVariables);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Get template information for a specific template.
     * 
     * @param authentication The authenticated user's information
     * @param templateId The template ID
     * @return Template information
     */
    @GetMapping("/{templateId}")
    public ResponseEntity<TemplateDto> getTemplate(
            Authentication authentication,
            @PathVariable String templateId) {
        
        String organizationId = (String) authentication.getDetails();
        log.debug("Getting template: {} for organization: {}", templateId, organizationId);
        
        TemplateDto template = templateDebateService.getDebateTemplate(organizationId, templateId);
        
        return ResponseEntity.ok(template);
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