package com.zamaz.mcp.debateengine.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.debateengine.adapter.web.dto.CreateDebateRequest;
import com.zamaz.mcp.debateengine.adapter.web.dto.CreateDebateResponse;
import com.zamaz.mcp.debateengine.domain.model.Debate;
import com.zamaz.mcp.debateengine.domain.model.DebateId;
import com.zamaz.mcp.debateengine.domain.model.OrganizationId;
import com.zamaz.mcp.debateengine.domain.port.DebateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for debate operations.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
public class DebateIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private DebateRepository debateRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private OrganizationId organizationId;
    private UUID userId;
    
    @BeforeEach
    void setUp() {
        organizationId = OrganizationId.from("550e8400-e29b-41d4-a716-446655440000");
        userId = UUID.fromString("660e8400-e29b-41d4-a716-446655440000");
    }
    
    @Test
    void shouldCreateDebateSuccessfully() throws Exception {
        // Given
        CreateDebateRequest request = new CreateDebateRequest(
            "Should artificial intelligence be regulated by governments?",
            "A comprehensive debate on AI regulation policies",
            2,
            5,
            300000L,
            "PRIVATE",
            Map.of("theme", "technology", "difficulty", "advanced")
        );
        
        String requestJson = objectMapper.writeValueAsString(request);
        
        // When & Then
        String responseContent = mockMvc.perform(post("/api/debates")
                .header("X-Organization-Id", organizationId.toString())
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.debateId").exists())
                .andExpect(jsonPath("$.message").value("Debate created successfully"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        // Verify debate was saved
        CreateDebateResponse response = objectMapper.readValue(responseContent, CreateDebateResponse.class);
        DebateId debateId = DebateId.from(response.debateId());
        
        Optional<Debate> savedDebate = debateRepository.findById(debateId);
        assertThat(savedDebate).isPresent();
        assertThat(savedDebate.get().getTopic().toString()).contains("artificial intelligence");
    }
    
    @Test
    void shouldGetDebateById() throws Exception {
        // Given - create a debate first
        DebateId debateId = createTestDebate();
        
        // When & Then
        mockMvc.perform(get("/api/debates/{debateId}", debateId.toString())
                .header("X-Organization-Id", organizationId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(debateId.toString()))
                .andExpect(jsonPath("$.topic").value("Test Debate Topic"))
                .andExpect(jsonPath("$.status").value("draft"));
    }
    
    @Test
    void shouldStartDebate() throws Exception {
        // Given
        DebateId debateId = createTestDebate();
        
        // When & Then
        mockMvc.perform(post("/api/debates/{debateId}/start", debateId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.debateId").value(debateId.toString()))
                .andExpect(jsonPath("$.status").value("started"))
                .andExpect(jsonPath("$.message").value("Debate started successfully"));
    }
    
    @Test
    void shouldAddParticipantToDebate() throws Exception {
        // Given
        DebateId debateId = createTestDebate();
        
        String participantRequest = """
            {
                "userId": "770e8400-e29b-41d4-a716-446655440000",
                "position": "PRO"
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/debates/{debateId}/participants", debateId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(participantRequest))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.debateId").value(debateId.toString()))
                .andExpect(jsonPath("$.participantId").exists())
                .andExpect(jsonPath("$.message").value("Participant added successfully"));
    }
    
    @Test
    void shouldSubmitResponseToRound() throws Exception {
        // Given
        DebateId debateId = createTestDebate();
        String roundId = "880e8400-e29b-41d4-a716-446655440000";
        
        String responseRequest = """
            {
                "participantId": "770e8400-e29b-41d4-a716-446655440000",
                "content": "I believe that artificial intelligence should be regulated because it poses significant risks to society and privacy.",
                "responseTimeMs": 15000,
                "tokenCount": 25
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/debates/{debateId}/rounds/{roundId}/responses", 
                debateId.toString(), roundId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(responseRequest))
                .andExpected(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.debateId").value(debateId.toString()))
                .andExpect(jsonPath("$.roundId").value(roundId))
                .andExpect(jsonPath("$.responseId").exists())
                .andExpect(jsonPath("$.message").value("Response submitted successfully"));
    }
    
    @Test
    void shouldListDebates() throws Exception {
        // Given - create a test debate
        createTestDebate();
        
        // When & Then
        mockMvc.perform(get("/api/debates")
                .header("X-Organization-Id", organizationId.toString())
                .param("limit", "10")
                .param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }
    
    @Test
    void shouldReturnNotFoundForNonExistentDebate() throws Exception {
        // Given
        String nonExistentId = "550e8400-e29b-41d4-a716-999999999999";
        
        // When & Then
        mockMvc.perform(get("/api/debates/{debateId}", nonExistentId)
                .header("X-Organization-Id", organizationId.toString()))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void shouldValidateCreateDebateRequest() throws Exception {
        // Given - invalid request with empty topic
        CreateDebateRequest invalidRequest = new CreateDebateRequest(
            "", // Empty topic
            "Description",
            2,
            5,
            300000L,
            "PRIVATE",
            Map.of()
        );
        
        String requestJson = objectMapper.writeValueAsString(invalidRequest);
        
        // When & Then
        mockMvc.perform(post("/api/debates")
                .header("X-Organization-Id", organizationId.toString())
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void shouldRequireOrganizationHeader() throws Exception {
        // Given
        CreateDebateRequest request = CreateDebateRequest.withDefaults(
            "Test topic", 
            "Test description"
        );
        
        String requestJson = objectMapper.writeValueAsString(request);
        
        // When & Then
        mockMvc.perform(post("/api/debates")
                .header("X-User-Id", userId.toString())
                // Missing X-Organization-Id header
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }
    
    /**
     * Helper method to create a test debate.
     */
    private DebateId createTestDebate() {
        // This would typically use the test container or embedded database
        // For now, return a mock debate ID
        return DebateId.generate();
    }
}