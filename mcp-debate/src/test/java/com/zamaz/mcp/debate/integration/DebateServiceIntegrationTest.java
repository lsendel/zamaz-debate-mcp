package com.zamaz.mcp.debate.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.zamaz.mcp.common.test.BaseIntegrationTest;
import com.zamaz.mcp.debate.model.*;
import com.zamaz.mcp.debate.service.DebateService;
import com.zamaz.mcp.debate.service.McpContextClient;
import com.zamaz.mcp.debate.service.McpLlmClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Debate Service Integration Tests")
class DebateServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private DebateService debateService;

    @MockBean
    private McpContextClient contextClient;

    @MockBean
    private McpLlmClient llmClient;

    private static final String DEBATE_ID = "test-debate-" + UUID.randomUUID();
    private static final String CONTEXT_ID = "test-context-" + UUID.randomUUID();
    private static final String TENANT_ID = "test-tenant";
    private static final String USER_ID = "test-user";

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(contextClient, llmClient);
    }

    @Test
    @Order(1)
    @DisplayName("Should create a new debate successfully")
    void shouldCreateDebateSuccessfully() {
        // Given
        CreateDebateRequest request = CreateDebateRequest.builder()
                .topic("Is AI beneficial for humanity?")
                .model1("claude-3")
                .model2("gpt-4")
                .maxRounds(3)
                .maxResponseLength(500)
                .temperature(0.7)
                .contextStrategy("SLIDING_WINDOW")
                .moderatorEnabled(true)
                .moderatorModel("claude-3")
                .tenantId(TENANT_ID)
                .userId(USER_ID)
                .build();

        // When - create context returns success
        when(contextClient.createContext(any()))
                .thenReturn(Mono.just(Map.of(
                        "contextId", CONTEXT_ID,
                        "success", true
                )));

        // Then
        StepVerifier.create(debateService.createDebate(request))
                .assertNext(debate -> {
                    assertThat(debate).isNotNull();
                    assertThat(debate.getId()).isNotBlank();
                    assertThat(debate.getTopic()).isEqualTo("Is AI beneficial for humanity?");
                    assertThat(debate.getModel1()).isEqualTo("claude-3");
                    assertThat(debate.getModel2()).isEqualTo("gpt-4");
                    assertThat(debate.getStatus()).isEqualTo(DebateStatus.CREATED);
                    assertThat(debate.getContextId()).isEqualTo(CONTEXT_ID);
                    assertThat(debate.getCurrentRound()).isEqualTo(0);
                    assertThat(debate.getMaxRounds()).isEqualTo(3);
                })
                .verifyComplete();

        // Verify context was created
        verify(contextClient, times(1)).createContext(argThat(params -> {
            Map<String, Object> map = (Map<String, Object>) params;
            return TENANT_ID.equals(map.get("tenantId")) &&
                   USER_ID.equals(map.get("userId"));
        }));
    }

    @Test
    @Order(2)
    @DisplayName("Should handle debate creation failure when context service fails")
    void shouldHandleDebateCreationFailureWhenContextFails() {
        // Given
        CreateDebateRequest request = CreateDebateRequest.builder()
                .topic("Test debate")
                .model1("claude-3")
                .model2("gpt-4")
                .tenantId(TENANT_ID)
                .userId(USER_ID)
                .build();

        // When - context creation fails
        when(contextClient.createContext(any()))
                .thenReturn(Mono.error(new RuntimeException("Context service unavailable")));

        // Then
        StepVerifier.create(debateService.createDebate(request))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                        throwable.getMessage().contains("Context service unavailable")
                )
                .verify();
    }

    @Test
    @Order(3)
    @DisplayName("Should run a complete debate with multiple rounds")
    void shouldRunCompleteDebateWithMultipleRounds() {
        // Given - existing debate
        Debate debate = createTestDebate();
        
        // Mock context operations
        when(contextClient.getContext(eq(CONTEXT_ID)))
                .thenReturn(Mono.just(Map.of(
                        "contextId", CONTEXT_ID,
                        "messages", List.of()
                )));

        when(contextClient.appendMessage(anyString(), any()))
                .thenReturn(Mono.just(Map.of("success", true)));

        // Mock LLM responses
        AtomicInteger responseCount = new AtomicInteger(0);
        when(llmClient.chat(any()))
                .thenAnswer(invocation -> {
                    int count = responseCount.incrementAndGet();
                    String model = ((Map<String, Object>) invocation.getArgument(0)).get("model").toString();
                    String response = String.format("%s response %d: This is a test argument.", model, count);
                    
                    return Mono.just(Map.of(
                            "response", response,
                            "model", model,
                            "tokens", 50
                    ));
                });

        // When - start debate
        StepVerifier.create(debateService.startDebate(debate.getId()))
                .assertNext(updatedDebate -> {
                    assertThat(updatedDebate.getStatus()).isEqualTo(DebateStatus.IN_PROGRESS);
                })
                .verifyComplete();

        // Then - verify all rounds completed
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Debate finalDebate = debateService.getDebate(debate.getId()).block();
            assertThat(finalDebate).isNotNull();
            assertThat(finalDebate.getStatus()).isEqualTo(DebateStatus.COMPLETED);
            assertThat(finalDebate.getCurrentRound()).isEqualTo(3);
            assertThat(finalDebate.getRounds()).hasSize(3);
        });

        // Verify interactions
        verify(llmClient, times(6)).chat(any()); // 2 models × 3 rounds
        verify(contextClient, atLeast(6)).appendMessage(eq(CONTEXT_ID), any());
    }

    @Test
    @Order(4)
    @DisplayName("Should handle LLM service failure gracefully")
    void shouldHandleLlmServiceFailureGracefully() {
        // Given
        Debate debate = createTestDebate();
        
        when(contextClient.getContext(eq(CONTEXT_ID)))
                .thenReturn(Mono.just(Map.of(
                        "contextId", CONTEXT_ID,
                        "messages", List.of()
                )));

        // When - LLM service fails
        when(llmClient.chat(any()))
                .thenReturn(Mono.error(new RuntimeException("LLM service timeout")));

        // Then
        StepVerifier.create(debateService.startDebate(debate.getId()))
                .assertNext(updatedDebate -> {
                    assertThat(updatedDebate.getStatus()).isEqualTo(DebateStatus.IN_PROGRESS);
                })
                .verifyComplete();

        // Wait and verify debate failed
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Debate finalDebate = debateService.getDebate(debate.getId()).block();
            assertThat(finalDebate).isNotNull();
            assertThat(finalDebate.getStatus()).isEqualTo(DebateStatus.FAILED);
            assertThat(finalDebate.getError()).contains("LLM service timeout");
        });
    }

    @Test
    @Order(5)
    @DisplayName("Should get debate by ID successfully")
    void shouldGetDebateByIdSuccessfully() {
        // Given
        Debate debate = createTestDebate();

        // When
        StepVerifier.create(debateService.getDebate(debate.getId()))
                .assertNext(retrievedDebate -> {
                    assertThat(retrievedDebate).isNotNull();
                    assertThat(retrievedDebate.getId()).isEqualTo(debate.getId());
                    assertThat(retrievedDebate.getTopic()).isEqualTo(debate.getTopic());
                })
                .verifyComplete();
    }

    @Test
    @Order(6)
    @DisplayName("Should list debates with pagination")
    void shouldListDebatesWithPagination() {
        // Given - create multiple debates
        for (int i = 0; i < 5; i++) {
            createTestDebate("Debate " + i);
        }

        // When
        StepVerifier.create(debateService.listDebates(TENANT_ID, 0, 3))
                .assertNext(debates -> {
                    assertThat(debates).hasSize(3);
                    assertThat(debates).allMatch(d -> d.getTenantId().equals(TENANT_ID));
                })
                .verifyComplete();

        // Test second page
        StepVerifier.create(debateService.listDebates(TENANT_ID, 3, 3))
                .assertNext(debates -> {
                    assertThat(debates).hasSizeLessThanOrEqualTo(3);
                })
                .verifyComplete();
    }

    @Test
    @Order(7)
    @DisplayName("Should update debate status correctly")
    void shouldUpdateDebateStatusCorrectly() {
        // Given
        Debate debate = createTestDebate();

        // When
        StepVerifier.create(debateService.updateDebateStatus(debate.getId(), DebateStatus.PAUSED))
                .assertNext(updatedDebate -> {
                    assertThat(updatedDebate.getStatus()).isEqualTo(DebateStatus.PAUSED);
                })
                .verifyComplete();
    }

    @Test
    @Order(8)
    @DisplayName("Should delete debate and cleanup context")
    void shouldDeleteDebateAndCleanupContext() {
        // Given
        Debate debate = createTestDebate();
        
        when(contextClient.deleteContext(eq(CONTEXT_ID)))
                .thenReturn(Mono.just(Map.of("success", true)));

        // When
        StepVerifier.create(debateService.deleteDebate(debate.getId()))
                .verifyComplete();

        // Then
        StepVerifier.create(debateService.getDebate(debate.getId()))
                .expectError()
                .verify();

        // Verify context was deleted
        verify(contextClient, times(1)).deleteContext(eq(CONTEXT_ID));
    }

    @Test
    @Order(9)
    @DisplayName("Should export debate to different formats")
    void shouldExportDebateToDifferentFormats() {
        // Given
        Debate debate = createTestDebateWithRounds();

        // When - export as JSON
        StepVerifier.create(debateService.exportDebate(debate.getId(), "json"))
                .assertNext(exportData -> {
                    assertThat(exportData).isNotNull();
                    assertThat(exportData).containsKey("debate");
                    assertThat(exportData).containsKey("rounds");
                    assertThat(exportData).containsKey("metadata");
                })
                .verifyComplete();

        // When - export as markdown
        StepVerifier.create(debateService.exportDebate(debate.getId(), "markdown"))
                .assertNext(exportData -> {
                    assertThat(exportData).isNotNull();
                    assertThat(exportData.get("content")).isNotNull();
                    assertThat(exportData.get("content").toString()).contains("# Debate:");
                })
                .verifyComplete();
    }

    @Test
    @Order(10)
    @DisplayName("Should handle concurrent debate operations")
    void shouldHandleConcurrentDebateOperations() {
        // Given
        List<CreateDebateRequest> requests = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            requests.add(CreateDebateRequest.builder()
                    .topic("Concurrent debate " + i)
                    .model1("claude-3")
                    .model2("gpt-4")
                    .tenantId(TENANT_ID)
                    .userId(USER_ID)
                    .build());
        }

        when(contextClient.createContext(any()))
                .thenReturn(Mono.just(Map.of(
                        "contextId", UUID.randomUUID().toString(),
                        "success", true
                )));

        // When - create debates concurrently
        List<Mono<Debate>> createOperations = requests.stream()
                .map(req -> debateService.createDebate(req))
                .toList();

        // Then
        StepVerifier.create(Mono.when(createOperations))
                .verifyComplete();

        // Verify all debates were created
        verify(contextClient, times(10)).createContext(any());
    }

    // Helper methods
    private Debate createTestDebate() {
        return createTestDebate("Test debate topic");
    }

    private Debate createTestDebate(String topic) {
        CreateDebateRequest request = CreateDebateRequest.builder()
                .topic(topic)
                .model1("claude-3")
                .model2("gpt-4")
                .maxRounds(3)
                .tenantId(TENANT_ID)
                .userId(USER_ID)
                .build();

        when(contextClient.createContext(any()))
                .thenReturn(Mono.just(Map.of(
                        "contextId", UUID.randomUUID().toString(),
                        "success", true
                )));

        return debateService.createDebate(request).block();
    }

    private Debate createTestDebateWithRounds() {
        Debate debate = createTestDebate();
        
        // Add some rounds
        List<Round> rounds = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Round round = Round.builder()
                    .roundNumber(i)
                    .model1Response("Model 1 argument for round " + i)
                    .model2Response("Model 2 argument for round " + i)
                    .model1Tokens(100)
                    .model2Tokens(120)
                    .duration(Duration.ofSeconds(5))
                    .build();
            rounds.add(round);
        }
        
        debate.setRounds(rounds);
        debate.setCurrentRound(3);
        debate.setStatus(DebateStatus.COMPLETED);
        
        return debate;
    }
}