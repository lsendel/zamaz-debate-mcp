package com.zamaz.mcp.controller.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic integration test to verify the system can compile and run tests
 */
class SystemCompilationIT {
    
    @Test
    void testSystemCanCompile() {
        // This test verifies that our system can compile and run tests
        assertTrue(true, "System should be able to compile and run this test");
    }
    
    @Test
    void testAuthenticationSystemExists() {
        // Verify authentication classes exist
        try {
            Class<?> authServiceClass = Class.forName("com.zamaz.mcp.security.service.AuthorizationService");
            assertNotNull(authServiceClass, "AuthorizationService class should exist");
            
            Class<?> jwtServiceClass = Class.forName("com.zamaz.mcp.security.service.JwtService");
            assertNotNull(jwtServiceClass, "JwtService class should exist");
            
        } catch (ClassNotFoundException e) {
            fail("Essential security classes are missing: " + e.getMessage());
        }
    }
    
    @Test
    void testTemplateServiceIntegrationExists() {
        // Verify template service integration classes exist
        try {
            Class<?> templateServiceClass = Class.forName("com.zamaz.mcp.controller.service.TemplateBasedDebateService");
            assertNotNull(templateServiceClass, "TemplateBasedDebateService class should exist");
            
            Class<?> templateClientClass = Class.forName("com.zamaz.mcp.controller.client.TemplateServiceClient");
            assertNotNull(templateClientClass, "TemplateServiceClient class should exist");
            
        } catch (ClassNotFoundException e) {
            fail("Template service integration classes are missing: " + e.getMessage());
        }
    }
    
    @Test
    void testDebateServiceExists() {
        // Verify core debate service classes exist
        try {
            Class<?> debateServiceClass = Class.forName("com.zamaz.mcp.controller.service.DebateService");
            assertNotNull(debateServiceClass, "DebateService class should exist");
            
            Class<?> orchestrationServiceClass = Class.forName("com.zamaz.mcp.controller.service.OrchestrationService");
            assertNotNull(orchestrationServiceClass, "OrchestrationService class should exist");
            
        } catch (ClassNotFoundException e) {
            fail("Core debate service classes are missing: " + e.getMessage());
        }
    }
}