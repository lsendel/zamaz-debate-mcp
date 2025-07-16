package com.zamaz.mcp.controller.client;

import com.zamaz.mcp.controller.dto.TemplateDto;
import com.zamaz.mcp.controller.dto.TemplateValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Client for communicating with the Template Service.
 * Provides methods to retrieve, validate, and use debate templates.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TemplateServiceClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${template.service.url:http://localhost:5006}")
    private String templateServiceUrl;
    
    /**
     * Get all debate templates for an organization.
     * 
     * @param organizationId The organization ID
     * @return List of debate templates
     */
    public List<TemplateDto> getDebateTemplates(String organizationId) {
        log.debug("Fetching debate templates for organization: {}", organizationId);
        
        try {
            String url = templateServiceUrl + "/api/templates?organizationId=" + organizationId + "&category=DEBATE";
            ResponseEntity<TemplateDto[]> response = restTemplate.getForEntity(url, TemplateDto[].class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return List.of(response.getBody());
            }
            
            log.warn("Failed to fetch debate templates: {}", response.getStatusCode());
            return List.of();
            
        } catch (Exception e) {
            log.error("Error fetching debate templates for organization: {}", organizationId, e);
            return List.of();
        }
    }
    
    /**
     * Get a specific template by ID.
     * 
     * @param templateId The template ID
     * @param organizationId The organization ID
     * @return Optional template
     */
    public Optional<TemplateDto> getTemplate(String templateId, String organizationId) {
        log.debug("Fetching template: {} for organization: {}", templateId, organizationId);
        
        try {
            String url = templateServiceUrl + "/api/templates/" + templateId + "?organizationId=" + organizationId;
            ResponseEntity<TemplateDto> response = restTemplate.getForEntity(url, TemplateDto.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(response.getBody());
            }
            
            log.warn("Template not found: {} for organization: {}", templateId, organizationId);
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("Error fetching template: {} for organization: {}", templateId, organizationId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Validate a template configuration.
     * 
     * @param templateId The template ID
     * @param organizationId The organization ID
     * @return Template validation result
     */
    public TemplateValidationResult validateTemplate(String templateId, String organizationId) {
        log.debug("Validating template: {} for organization: {}", templateId, organizationId);
        
        try {
            String url = templateServiceUrl + "/api/templates/" + templateId + "/validate";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Organization-Id", organizationId);
            
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<TemplateValidationResult> response = restTemplate.exchange(
                url, HttpMethod.POST, requestEntity, TemplateValidationResult.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            
            log.warn("Template validation failed: {} for organization: {}", templateId, organizationId);
            return TemplateValidationResult.invalid("Template validation failed");
            
        } catch (Exception e) {
            log.error("Error validating template: {} for organization: {}", templateId, organizationId, e);
            return TemplateValidationResult.invalid("Template validation error: " + e.getMessage());
        }
    }
    
    /**
     * Render a template with variables.
     * 
     * @param templateId The template ID
     * @param organizationId The organization ID
     * @param variables Template variables
     * @return Rendered template content
     */
    public Optional<String> renderTemplate(String templateId, String organizationId, 
                                         java.util.Map<String, Object> variables) {
        log.debug("Rendering template: {} for organization: {}", templateId, organizationId);
        
        try {
            String url = templateServiceUrl + "/api/templates/" + templateId + "/render";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Organization-Id", organizationId);
            
            HttpEntity<java.util.Map<String, Object>> requestEntity = new HttpEntity<>(variables, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, requestEntity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(response.getBody());
            }
            
            log.warn("Template rendering failed: {} for organization: {}", templateId, organizationId);
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("Error rendering template: {} for organization: {}", templateId, organizationId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Check if template service is available.
     * 
     * @return true if service is available
     */
    public boolean isTemplateServiceAvailable() {
        try {
            String url = templateServiceUrl + "/actuator/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.debug("Template service not available: {}", e.getMessage());
            return false;
        }
    }
}