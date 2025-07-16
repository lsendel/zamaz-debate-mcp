package com.zamaz.mcp.llm.service;

import com.zamaz.mcp.llm.model.CompletionRequest;
import com.zamaz.mcp.llm.model.CompletionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LlmServiceTest {

    private CompletionRequest testRequest;
    private CompletionResponse testResponse;

    @BeforeEach
    void setUp() {
        testRequest = new CompletionRequest();
        testRequest.setProvider("claude");
        testRequest.setModel("claude-3-sonnet-20240229");
        testRequest.setMessages(List.of());
        testRequest.setMaxTokens(100);
        testRequest.setTemperature(0.7);

        testResponse = CompletionResponse.builder()
            .id("test-response-id")
            .provider("claude")
            .model("claude-3-sonnet-20240229")
            .choices(List.of(
                CompletionResponse.Choice.builder()
                    .index(0)
                    .message(CompletionResponse.Message.builder()
                        .role("assistant")
                        .content("Test response")
                        .build())
                    .finishReason("stop")
                    .build()
            ))
            .usage(CompletionResponse.Usage.builder()
                .promptTokens(10)
                .completionTokens(20)
                .totalTokens(30)
                .estimatedCost(0.001)
                .build())
            .build();
    }

    @Test
    void testRequestCreation() {
        assertNotNull(testRequest);
        assertEquals("claude", testRequest.getProvider());
        assertEquals("claude-3-sonnet-20240229", testRequest.getModel());
        assertEquals(100, testRequest.getMaxTokens());
        assertEquals(0.7, testRequest.getTemperature());
        assertNotNull(testRequest.getMessages());
        assertTrue(testRequest.getMessages().isEmpty());
    }

    @Test
    void testResponseCreation() {
        assertNotNull(testResponse);
        assertEquals("test-response-id", testResponse.getId());
        assertEquals("claude", testResponse.getProvider());
        assertEquals("claude-3-sonnet-20240229", testResponse.getModel());
        
        assertNotNull(testResponse.getChoices());
        assertEquals(1, testResponse.getChoices().size());
        
        CompletionResponse.Choice choice = testResponse.getChoices().get(0);
        assertEquals(0, choice.getIndex());
        assertEquals("stop", choice.getFinishReason());
        
        CompletionResponse.Message message = choice.getMessage();
        assertEquals("assistant", message.getRole());
        assertEquals("Test response", message.getContent());
        
        CompletionResponse.Usage usage = testResponse.getUsage();
        assertEquals(10, usage.getPromptTokens());
        assertEquals(20, usage.getCompletionTokens());
        assertEquals(30, usage.getTotalTokens());
        assertEquals(0.001, usage.getEstimatedCost());
    }
}