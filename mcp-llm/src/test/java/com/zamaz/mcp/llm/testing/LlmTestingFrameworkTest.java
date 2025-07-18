package com.zamaz.mcp.llm.testing;

import com.zamaz.mcp.llm.domain.model.CompletionRequest;
import com.zamaz.mcp.llm.model.CompletionResponse;
import com.zamaz.mcp.llm.model.Message;
import com.zamaz.mcp.common.testing.annotations.DomainTest;
import com.zamaz.mcp.common.testing.annotations.FastTest;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DomainTest
@FastTest
class LlmTestingFrameworkTest {

    @Test
    void mockLlmProvider_shouldReturnConfiguredResponse() {
        // Given
        MockLlmProvider provider = new MockLlmProvider("test-provider")
            .withQueuedResponse("Test response from mock");

        CompletionRequest request = createTestRequest("Hello, world!");

        // When & Then
        StepVerifier.create(provider.complete(request))
            .assertNext(response -> {
                assertThat(response.getProvider()).isEqualTo("test-provider");
                assertThat(response.getChoices().get(0).getMessage().getContent())
                    .isEqualTo("Test response from mock");
            })
            .verifyComplete();
    }

    @Test
    void mockLlmProvider_shouldSimulateFailures() {
        // Given
        MockLlmProvider provider = new MockLlmProvider("unreliable")
            .withFailureRate(1.0); // 100% failure rate

        CompletionRequest request = createTestRequest("This will fail");

        // When & Then
        StepVerifier.create(provider.complete(request))
            .expectError(RuntimeException.class)
            .verify();
    }

    @Test
    void mockLlmProvider_shouldSimulateRateLimit() {
        // Given
        MockLlmProvider provider = new MockLlmProvider("rate-limited")
            .withRateLimit(2) // Allow 2 requests then rate limit
            .withQueuedResponses("Response 1", "Response 2");

        CompletionRequest request = createTestRequest("Test request");

        // When & Then
        // First two requests should succeed
        StepVerifier.create(provider.complete(request))
            .assertNext(response -> assertThat(response.getChoices().get(0).getMessage().getContent())
                .isEqualTo("Response 1"))
            .verifyComplete();

        StepVerifier.create(provider.complete(request))
            .assertNext(response -> assertThat(response.getChoices().get(0).getMessage().getContent())
                .isEqualTo("Response 2"))
            .verifyComplete();

        // Third request should fail with rate limit
        StepVerifier.create(provider.complete(request))
            .expectError(RuntimeException.class)
            .verify();
    }

    @Test
    void mockLlmProvider_shouldTrackUsage() {
        // Given
        MockLlmProvider provider = new MockLlmProvider("usage-tracker")
            .withQueuedResponses("Response 1", "Response 2", "Response 3");

        CompletionRequest request1 = createTestRequest("Request 1", "model-a");
        CompletionRequest request2 = createTestRequest("Request 2", "model-a");
        CompletionRequest request3 = createTestRequest("Request 3", "model-b");

        // When
        provider.complete(request1).block();
        provider.complete(request2).block();
        provider.complete(request3).block();

        // Then
        assertThat(provider.getRequestCount()).isEqualTo(3);
        assertThat(provider.getModelUsage()).containsEntry("model-a", 2);
        assertThat(provider.getModelUsage()).containsEntry("model-b", 1);
        assertThat(provider.getRequestHistory()).hasSize(3);
    }

    @Test
    void mockLlmProvider_shouldHandleStreamingResponses() {
        // Given
        MockLlmProvider provider = new MockLlmProvider("streaming")
            .withQueuedResponse("This is a streaming response for testing");

        CompletionRequest request = createTestRequest("Stream test");

        // When & Then
        StepVerifier.create(provider.streamComplete(request))
            .expectNextCount(4) // Response split into chunks
            .verifyComplete();
    }

    @Test
    void mockLlmProvider_shouldCalculateTokensAndCosts() {
        // Given
        MockLlmProvider provider = new MockLlmProvider("cost-calculator")
            .withTokenPricing(new BigDecimal("5.0"), new BigDecimal("10.0"))
            .withQueuedResponse("Cost calculation test response");

        CompletionRequest request = createTestRequest("Calculate costs for this");

        // When
        CompletionResponse response = provider.complete(request).block();

        // Then
        assertThat(response.getUsage().getTotalTokens()).isGreaterThan(0);
        assertThat(response.getUsage().getTotalCost()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void llmProviderFactory_shouldCreatePreConfiguredProviders() {
        // When
        MockLlmProvider claude = LlmProviderFactory.claudeMock();
        MockLlmProvider openAi = LlmProviderFactory.openAiMock();
        MockLlmProvider fast = LlmProviderFactory.fastMock();

        // Then
        assertThat(claude.getName()).isEqualTo("claude");
        assertThat(claude.getSupportedModels()).contains("claude-3-sonnet-20240229");
        
        assertThat(openAi.getName()).isEqualTo("openai");
        assertThat(openAi.getSupportedModels()).contains("gpt-4");
        
        assertThat(fast.getName()).isEqualTo("fast-mock");
    }

    @Test
    void llmProviderFactory_shouldCreateProviderRegistry() {
        // When
        Map<String, MockLlmProvider> registry = LlmProviderFactory.createProviderRegistry();

        // Then
        assertThat(registry).hasSize(6);
        assertThat(registry).containsKeys("claude", "openai", "gemini", "ollama", "fast", "unreliable");
        
        // Test that providers are properly configured
        MockLlmProvider claude = registry.get("claude");
        assertThat(claude.isEnabled()).isTrue();
        assertThat(claude.getSupportedModels()).isNotEmpty();
    }

    @Test
    void llmProviderFactory_shouldCreateFailoverChain() {
        // When
        List<MockLlmProvider> chain = LlmProviderFactory.createFailoverChain();

        // Then
        assertThat(chain).hasSize(3);
        assertThat(chain.get(0).isEnabled()).isFalse(); // Primary is disabled
        assertThat(chain.get(1).isEnabled()).isTrue();  // Secondary enabled but rate limited
        assertThat(chain.get(2).isEnabled()).isTrue();  // Tertiary is reliable
    }

    @Test
    void mockLlmProvider_shouldHandleDynamicResponses() {
        // Given
        MockLlmProvider provider = LlmProviderFactory.smartMock();
        
        // When & Then
        CompletionRequest helloRequest = createTestRequest("Hello there!");
        StepVerifier.create(provider.complete(helloRequest))
            .assertNext(response -> assertThat(response.getChoices().get(0).getMessage().getContent())
                .contains("Hello! How can I help you today?"))
            .verifyComplete();

        CompletionRequest mathRequest = createTestRequest("Can you help me with math?");
        StepVerifier.create(provider.complete(mathRequest))
            .assertNext(response -> assertThat(response.getChoices().get(0).getMessage().getContent())
                .contains("Let me calculate that for you"))
            .verifyComplete();
    }

    @Test
    void mockLlmProvider_shouldSimulateResponseDelay() {
        // Given
        MockLlmProvider provider = new MockLlmProvider("delayed")
            .withResponseDelay(Duration.ofMillis(100))
            .withQueuedResponse("Delayed response");

        CompletionRequest request = createTestRequest("Test delay");

        // When & Then
        StepVerifier.create(provider.complete(request))
            .expectSubscription()
            .expectNoEvent(Duration.ofMillis(50)) // Should not complete before delay
            .assertNext(response -> assertThat(response.getChoices().get(0).getMessage().getContent())
                .isEqualTo("Delayed response"))
            .verifyComplete();
    }

    @Test
    void mockLlmProvider_shouldReset() {
        // Given
        MockLlmProvider provider = new MockLlmProvider("resettable")
            .withFailureRate(0.5)
            .withQueuedResponse("Test response");

        CompletionRequest request = createTestRequest("Test");
        provider.complete(request).onErrorReturn(null).block(); // May fail due to failure rate

        // When
        provider.reset();

        // Then
        assertThat(provider.getRequestCount()).isEqualTo(0);
        assertThat(provider.getRequestHistory()).isEmpty();
        assertThat(provider.getModelUsage()).isEmpty();
        assertThat(provider.isEnabled()).isTrue();
        
        // Should work reliably after reset
        StepVerifier.create(provider.complete(request))
            .assertNext(response -> assertThat(response).isNotNull())
            .verifyComplete();
    }

    private CompletionRequest createTestRequest(String content) {
        return createTestRequest(content, "test-model");
    }

    private CompletionRequest createTestRequest(String content, String model) {
        return CompletionRequest.builder()
            .organizationId("test-org")
            .userId("test-user")
            .model(model)
            .messages(List.of(new Message("user", content)))
            .maxTokens(100)
            .temperature(0.7)
            .build();
    }
}