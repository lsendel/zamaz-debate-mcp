package com.zamaz.mcp.controller.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.controller.controller.DebateController;
import com.zamaz.mcp.controller.dto.DebateDto;
import com.zamaz.mcp.controller.service.DebateService;
import com.zamaz.mcp.security.service.AuthorizationService;
import com.zamaz.mcp.security.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for DebateController
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DebateControllerIT {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private DebateService debateService;
    
    @MockBean
    private AuthorizationService authorizationService;
    
    @MockBean
    private JwtService jwtService;
    
    private String validToken;
    private UUID organizationId;
    private UUID userId;
    
    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        validToken = "Bearer valid-test-token";
        
        // Mock JWT validation
        when(jwtService.validateToken("valid-test-token")).thenReturn(true);
        when(jwtService.getUserId("valid-test-token")).thenReturn(userId.toString());
        when(jwtService.getOrganizationId("valid-test-token")).thenReturn(organizationId.toString());
        
        // Mock authorization
        when(authorizationService.hasPermission(any(), any(), any())).thenReturn(true);
    }
    
    @Test
    void testCreateDebate_Success() throws Exception {
        // Prepare test data
        DebateDto.CreateDebateRequest request = DebateDto.CreateDebateRequest.builder()
                .organizationId(organizationId)
                .title("Test Debate")
                .topic("AI Ethics")
                .description("A debate about AI ethics")
                .format("structured")
                .maxRounds(5)
                .build();
        
        DebateDto createdDebate = DebateDto.builder()
                .id(UUID.randomUUID())
                .organizationId(organizationId)
                .title("Test Debate")
                .topic("AI Ethics")
                .description("A debate about AI ethics")
                .format("structured")
                .status("DRAFT")
                .maxRounds(5)
                .currentRound(0)
                .createdAt(Instant.now())
                .createdBy(userId)
                .build();
        
        when(debateService.createDebate(any(DebateDto.CreateDebateRequest.class)))
                .thenReturn(createdDebate);
        
        // Execute and verify
        mockMvc.perform(post("/api/debates")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Test Debate"))
                .andExpect(jsonPath("$.topic").value("AI Ethics"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }
    
    @Test
    void testCreateDebate_Unauthorized() throws Exception {
        DebateDto.CreateDebateRequest request = DebateDto.CreateDebateRequest.builder()
                .organizationId(organizationId)
                .title("Test Debate")
                .topic("AI Ethics")
                .build();
        
        mockMvc.perform(post("/api/debates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testGetDebate_Success() throws Exception {
        UUID debateId = UUID.randomUUID();
        
        DebateDto debate = DebateDto.builder()
                .id(debateId)
                .organizationId(organizationId)
                .title("Test Debate")
                .topic("AI Ethics")
                .status("ACTIVE")
                .build();
        
        when(debateService.getDebate(debateId)).thenReturn(debate);
        
        mockMvc.perform(get("/api/debates/{id}", debateId)
                        .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(debateId.toString()))
                .andExpect(jsonPath("$.title").value("Test Debate"));
    }
    
    @Test
    void testListDebates_Success() throws Exception {
        DebateDto debate1 = DebateDto.builder()
                .id(UUID.randomUUID())
                .organizationId(organizationId)
                .title("Debate 1")
                .topic("Topic 1")
                .status("ACTIVE")
                .build();
        
        DebateDto debate2 = DebateDto.builder()
                .id(UUID.randomUUID())
                .organizationId(organizationId)
                .title("Debate 2")
                .topic("Topic 2")
                .status("DRAFT")
                .build();
        
        when(debateService.listDebates(organizationId, null, 0, 20))
                .thenReturn(java.util.Arrays.asList(debate1, debate2));
        
        mockMvc.perform(get("/api/debates")
                        .header("Authorization", validToken)
                        .param("organizationId", organizationId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Debate 1"))
                .andExpect(jsonPath("$[1].title").value("Debate 2"));
    }
    
    @Test
    void testUpdateDebate_Success() throws Exception {
        UUID debateId = UUID.randomUUID();
        
        DebateDto.UpdateDebateRequest updateRequest = DebateDto.UpdateDebateRequest.builder()
                .title("Updated Title")
                .description("Updated Description")
                .build();
        
        DebateDto updatedDebate = DebateDto.builder()
                .id(debateId)
                .organizationId(organizationId)
                .title("Updated Title")
                .description("Updated Description")
                .topic("AI Ethics")
                .status("DRAFT")
                .build();
        
        when(debateService.updateDebate(eq(debateId), any(DebateDto.UpdateDebateRequest.class)))
                .thenReturn(updatedDebate);
        
        mockMvc.perform(put("/api/debates/{id}", debateId)
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.description").value("Updated Description"));
    }
    
    @Test
    void testDeleteDebate_Success() throws Exception {
        UUID debateId = UUID.randomUUID();
        
        mockMvc.perform(delete("/api/debates/{id}", debateId)
                        .header("Authorization", validToken))
                .andExpect(status().isNoContent());
    }
    
    @Test
    void testStartDebate_Success() throws Exception {
        UUID debateId = UUID.randomUUID();
        
        DebateDto startedDebate = DebateDto.builder()
                .id(debateId)
                .organizationId(organizationId)
                .title("Test Debate")
                .status("ACTIVE")
                .startedAt(Instant.now())
                .build();
        
        when(debateService.startDebate(debateId)).thenReturn(startedDebate);
        
        mockMvc.perform(post("/api/debates/{id}/start", debateId)
                        .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.startedAt").isNotEmpty());
    }
    
    @Test
    void testEndDebate_Success() throws Exception {
        UUID debateId = UUID.randomUUID();
        
        DebateDto endedDebate = DebateDto.builder()
                .id(debateId)
                .organizationId(organizationId)
                .title("Test Debate")
                .status("COMPLETED")
                .endedAt(Instant.now())
                .build();
        
        when(debateService.endDebate(debateId)).thenReturn(endedDebate);
        
        mockMvc.perform(post("/api/debates/{id}/end", debateId)
                        .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.endedAt").isNotEmpty());
    }
    
    @Test
    void testValidationError_BadRequest() throws Exception {
        // Missing required fields
        DebateDto.CreateDebateRequest invalidRequest = DebateDto.CreateDebateRequest.builder()
                .organizationId(organizationId)
                // Missing title and topic
                .build();
        
        mockMvc.perform(post("/api/debates")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testPermissionDenied_Forbidden() throws Exception {
        // Mock permission denied
        when(authorizationService.hasPermission(any(), any(), any())).thenReturn(false);
        
        UUID debateId = UUID.randomUUID();
        
        mockMvc.perform(get("/api/debates/{id}", debateId)
                        .header("Authorization", validToken))
                .andExpect(status().isForbidden());
    }
}