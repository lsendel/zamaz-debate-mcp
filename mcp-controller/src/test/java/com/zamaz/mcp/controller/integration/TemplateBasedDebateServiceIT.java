package com.zamaz.mcp.controller.integration;

import com.zamaz.mcp.controller.client.TemplateServiceClient;
import com.zamaz.mcp.controller.dto.DebateDto;
import com.zamaz.mcp.controller.dto.TemplateDto;
import com.zamaz.mcp.controller.dto.TemplateValidationResult;
import com.zamaz.mcp.controller.service.DebateService;
import com.zamaz.mcp.controller.service.TemplateBasedDebateService;
import com.zamaz.mcp.common.event.EventPublisher;
import com.zamaz.mcp.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for TemplateBasedDebateService
 */
@SpringBootTest
@ActiveProfiles("test")
class TemplateBasedDebateServiceIT {
    
    @Autowired
    private TemplateBasedDebateService templateBasedDebateService;
    
    @MockBean
    private TemplateServiceClient templateServiceClient;
    
    @MockBean
    private DebateService debateService;
    
    @MockBean
    private EventPublisher eventPublisher;
    
    private UUID organizationId;
    private String templateId;
    private TemplateDto templateDto;
    
    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        templateId = "template-123";
        
        // Setup template metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("maxRounds", 5);
        metadata.put("turnTimeoutSeconds", 300);
        metadata.put("rules", "Standard debate rules");
        metadata.put("isPublic", false);
        metadata.put("format", "structured");
        
        templateDto = TemplateDto.builder()
                .id(templateId)
                .organizationId(organizationId.toString())
                .name("Standard Debate Template")
                .description("A template for standard debates")
                .version("1.0")
                .status("ACTIVE")
                .metadata(metadata)
                .build();
    }
    
    @Test
    void testCreateDebateFromTemplate_Success() {
        // Mock template service responses
        when(templateServiceClient.isTemplateServiceAvailable()).thenReturn(true);
        when(templateServiceClient.getTemplate(templateId, organizationId.toString()))
                .thenReturn(templateDto);
        when(templateServiceClient.validateTemplate(templateId, organizationId.toString()))
                .thenReturn(new TemplateValidationResult(true, "Valid"));
        
        // Mock debate creation
        DebateDto createdDebate = DebateDto.builder()
                .id(UUID.randomUUID())
                .organizationId(organizationId)
                .title("Test Debate from Template")
                .topic("AI Ethics")
                .status("DRAFT")
                .build();
        
        when(debateService.createDebate(any())).thenReturn(createdDebate);
        
        // Test variables
        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("title", "Test Debate from Template");
        templateVariables.put("topic", "AI Ethics");
        
        // Execute
        DebateDto result = templateBasedDebateService.createDebateFromTemplate(
                organizationId.toString(), templateId, templateVariables);
        
        // Verify
        assertNotNull(result);
        assertEquals("Test Debate from Template", result.getTitle());
        assertEquals("AI Ethics", result.getTopic());
        
        // Verify template service interactions
        verify(templateServiceClient).isTemplateServiceAvailable();
        verify(templateServiceClient).getTemplate(templateId, organizationId.toString());
        verify(templateServiceClient).validateTemplate(templateId, organizationId.toString());
        
        // Verify debate was created
        verify(debateService).createDebate(any());
        
        // Verify event was published
        verify(eventPublisher).publishEvent(any());
    }
    
    @Test
    void testCreateDebateFromTemplate_TemplateServiceUnavailable() {
        when(templateServiceClient.isTemplateServiceAvailable()).thenReturn(false);
        
        Map<String, Object> templateVariables = new HashMap<>();
        
        BusinessException exception = assertThrows(BusinessException.class, () ->
                templateBasedDebateService.createDebateFromTemplate(
                        organizationId.toString(), templateId, templateVariables));
        
        assertEquals("Template service is not available", exception.getMessage());
        
        // Verify no debate was created
        verify(debateService, never()).createDebate(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
    
    @Test
    void testCreateDebateFromTemplate_TemplateValidationFailed() {
        when(templateServiceClient.isTemplateServiceAvailable()).thenReturn(true);
        when(templateServiceClient.getTemplate(templateId, organizationId.toString()))
                .thenReturn(templateDto);
        when(templateServiceClient.validateTemplate(templateId, organizationId.toString()))
                .thenReturn(new TemplateValidationResult(false, "Template is inactive"));
        
        Map<String, Object> templateVariables = new HashMap<>();
        
        BusinessException exception = assertThrows(BusinessException.class, () ->
                templateBasedDebateService.createDebateFromTemplate(
                        organizationId.toString(), templateId, templateVariables));
        
        assertTrue(exception.getMessage().contains("Template validation failed"));
        
        // Verify no debate was created
        verify(debateService, never()).createDebate(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
    
    @Test
    void testGetTemplate_Success() {
        when(templateServiceClient.getTemplate(templateId, organizationId.toString()))
                .thenReturn(templateDto);
        
        TemplateDto result = templateBasedDebateService.getTemplate(templateId, organizationId.toString());
        
        assertNotNull(result);
        assertEquals(templateId, result.getId());
        assertEquals("Standard Debate Template", result.getName());
    }
    
    @Test
    void testListTemplates_Success() {
        TemplateDto template1 = TemplateDto.builder()
                .id("template-1")
                .name("Template 1")
                .status("ACTIVE")
                .build();
        
        TemplateDto template2 = TemplateDto.builder()
                .id("template-2")
                .name("Template 2")
                .status("ACTIVE")
                .build();
        
        when(templateServiceClient.listTemplates(organizationId.toString(), "ACTIVE"))
                .thenReturn(java.util.Arrays.asList(template1, template2));
        
        var templates = templateBasedDebateService.listTemplates(organizationId.toString(), "ACTIVE");
        
        assertEquals(2, templates.size());
        assertEquals("Template 1", templates.get(0).getName());
        assertEquals("Template 2", templates.get(1).getName());
    }
    
    @Test
    void testValidateTemplate_Valid() {
        when(templateServiceClient.validateTemplate(templateId, organizationId.toString()))
                .thenReturn(new TemplateValidationResult(true, "Template is valid"));
        
        TemplateValidationResult result = templateBasedDebateService.validateTemplate(
                templateId, organizationId.toString());
        
        assertTrue(result.isValid());
        assertEquals("Template is valid", result.getMessage());
    }
    
    @Test
    void testValidateTemplate_Invalid() {
        when(templateServiceClient.validateTemplate(templateId, organizationId.toString()))
                .thenReturn(new TemplateValidationResult(false, "Template is expired"));
        
        TemplateValidationResult result = templateBasedDebateService.validateTemplate(
                templateId, organizationId.toString());
        
        assertFalse(result.isValid());
        assertEquals("Template is expired", result.getMessage());
    }
    
    @Test
    void testCreateDebateFromTemplate_WithDefaultValues() {
        // Template without some metadata fields
        Map<String, Object> minimalMetadata = new HashMap<>();
        minimalMetadata.put("format", "casual");
        
        TemplateDto minimalTemplate = TemplateDto.builder()
                .id(templateId)
                .organizationId(organizationId.toString())
                .name("Minimal Template")
                .description("A minimal template")
                .metadata(minimalMetadata)
                .build();
        
        when(templateServiceClient.isTemplateServiceAvailable()).thenReturn(true);
        when(templateServiceClient.getTemplate(templateId, organizationId.toString()))
                .thenReturn(minimalTemplate);
        when(templateServiceClient.validateTemplate(templateId, organizationId.toString()))
                .thenReturn(new TemplateValidationResult(true, "Valid"));
        
        DebateDto createdDebate = DebateDto.builder()
                .id(UUID.randomUUID())
                .organizationId(organizationId)
                .title("Debate from Minimal Template")
                .status("DRAFT")
                .build();
        
        when(debateService.createDebate(any())).thenReturn(createdDebate);
        
        Map<String, Object> templateVariables = new HashMap<>();
        
        DebateDto result = templateBasedDebateService.createDebateFromTemplate(
                organizationId.toString(), templateId, templateVariables);
        
        assertNotNull(result);
        
        // Verify default values were used
        verify(debateService).createDebate(argThat(request -> {
            // Check that default values were applied
            assertEquals(5, request.getMaxRounds()); // Default maxRounds
            assertNotNull(request.getSettings());
            return true;
        }));
    }
}