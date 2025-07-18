package com.zamaz.mcp.debate.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.zamaz.mcp.debate.service.McpLlmClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
@SpringBootTest
@ActiveProfiles("test")
@PactTestFor(providerName = "llm-service", port = "8080")
@DisplayName("Debate Service LLM Consumer Contract Tests")
public class DebateServiceLlmConsumerContractTest {

    @Pact(consumer = "debate-service")
    public RequestResponsePact chatWithClaudeProvider(PactDslWithProvider builder) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("provider", "claude");
        requestBody.put("model", "claude-3-opus");
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "You are participating in a debate."),
                Map.of("role", "user", "content", "What are your thoughts on AI ethics?")
        ));
        requestBody.put("temperature", 0.7);
        requestBody.put("maxTokens", 500);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("response", "AI ethics is a crucial topic that requires careful consideration...");
        responseBody.put("model", "claude-3-opus");
        responseBody.put("tokens", 45);
        responseBody.put("finishReason", "stop");

        return builder
                .given("claude provider is available for chat")
                .uponReceiving("a chat request for Claude")
                .path("/tools/chat")
                .method("POST")
                .headers("Content-Type", "application/json")
                .body(requestBody)
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(responseBody)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "chatWithClaudeProvider")
    void testChatWithClaude(MockServer mockServer) {
        // Given
        McpLlmClient llmClient = new McpLlmClient(
                WebClient.builder().baseUrl(mockServer.getUrl()).build()
        );

        Map<String, Object> chatRequest = new HashMap<>();
        chatRequest.put("provider", "claude");
        chatRequest.put("model", "claude-3-opus");
        chatRequest.put("messages", List.of(
                Map.of("role", "system", "content", "You are participating in a debate."),
                Map.of("role", "user", "content", "What are your thoughts on AI ethics?")
        ));
        chatRequest.put("temperature", 0.7);
        chatRequest.put("maxTokens", 500);

        // When
        Mono<Map<String, Object>> response = llmClient.chat(chatRequest);

        // Then
        StepVerifier.create(response)
                .assertNext(result -> {
                    assertThat(result).containsKey("response");
                    assertThat(result.get("response")).asString()
                            .contains("AI ethics is a crucial topic");
                    assertThat(result.get("model")).isEqualTo("claude-3-opus");
                    assertThat(result.get("tokens")).isEqualTo(45);
                })
                .verifyComplete();
    }

    @Pact(consumer = "debate-service")
    public RequestResponsePact chatWithOpenAIProvider(PactDslWithProvider builder) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("provider", "openai");
        requestBody.put("model", "gpt-4");
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", "Argue for renewable energy")
        ));
        requestBody.put("temperature", 0.8);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("response", "Renewable energy represents the future of sustainable power generation...");
        responseBody.put("model", "gpt-4");
        responseBody.put("tokens", 60);
        responseBody.put("finishReason", "stop");

        return builder
                .given("openai provider is available for chat")
                .uponReceiving("a chat request for OpenAI")
                .path("/tools/chat")
                .method("POST")
                .headers("Content-Type", "application/json")
                .body(requestBody)
                .willRespondWith()
                .status(200)
                .body(responseBody)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "chatWithOpenAIProvider")
    void testChatWithOpenAI(MockServer mockServer) {
        // Given
        McpLlmClient llmClient = new McpLlmClient(
                WebClient.builder().baseUrl(mockServer.getUrl()).build()
        );

        Map<String, Object> chatRequest = new HashMap<>();
        chatRequest.put("provider", "openai");
        chatRequest.put("model", "gpt-4");
        chatRequest.put("messages", List.of(
                Map.of("role", "user", "content", "Argue for renewable energy")
        ));
        chatRequest.put("temperature", 0.8);

        // When
        Mono<Map<String, Object>> response = llmClient.chat(chatRequest);

        // Then
        StepVerifier.create(response)
                .assertNext(result -> {
                    assertThat(result.get("response")).asString()
                            .contains("Renewable energy represents the future");
                    assertThat(result.get("model")).isEqualTo("gpt-4");
                })
                .verifyComplete();
    }

    @Pact(consumer = "debate-service")
    public RequestResponsePact handleRateLimitedProvider(PactDslWithProvider builder) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("provider", "openai");
        requestBody.put("model", "gpt-4");
        requestBody.put("messages", List.of(Map.of("role", "user", "content", "Test")));

        return builder
                .given("openai provider is rate limited")
                .uponReceiving("a chat request when rate limited")
                .path("/tools/chat")
                .method("POST")
                .body(requestBody)
                .willRespondWith()
                .status(429)
                .body(Map.of(
                        "error", "Rate limit exceeded",
                        "error_type", "RateLimit",
                        "retry_after", 60
                ))
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "handleRateLimitedProvider")
    void testRateLimitHandling(MockServer mockServer) {
        // Given
        McpLlmClient llmClient = new McpLlmClient(
                WebClient.builder().baseUrl(mockServer.getUrl()).build()
        );

        Map<String, Object> chatRequest = new HashMap<>();
        chatRequest.put("provider", "openai");
        chatRequest.put("model", "gpt-4");
        chatRequest.put("messages", List.of(Map.of("role", "user", "content", "Test")));

        // When
        Mono<Map<String, Object>> response = llmClient.chat(chatRequest);

        // Then - expect error
        StepVerifier.create(response)
                .expectErrorMatches(throwable ->
                        throwable.getMessage().contains("429") ||
                        throwable.getMessage().contains("Rate limit")
                )
                .verify();
    }

    @Pact(consumer = "debate-service")
    public RequestResponsePact listAvailableProviders(PactDslWithProvider builder) {
        List<Map<String, Object>> providers = List.of(
                Map.of("name", "claude", "enabled", true, "models", List.of("claude-3-opus", "claude-3-sonnet")),
                Map.of("name", "openai", "enabled", true, "models", List.of("gpt-4", "gpt-3.5-turbo"))
        );

        return builder
                .given("providers are available")
                .uponReceiving("a request to list providers")
                .path("/resources/providers")
                .method("GET")
                .willRespondWith()
                .status(200)
                .body(Map.of("providers", providers))
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "listAvailableProviders")
    void testListProviders(MockServer mockServer) {
        // Given
        McpLlmClient llmClient = new McpLlmClient(
                WebClient.builder().baseUrl(mockServer.getUrl()).build()
        );

        // When
        Mono<Map<String, Object>> response = llmClient.listProviders();

        // Then
        StepVerifier.create(response)
                .assertNext(result -> {
                    assertThat(result).containsKey("providers");
                    List<Map<String, Object>> providers = (List<Map<String, Object>>) result.get("providers");
                    assertThat(providers).hasSize(2);
                    assertThat(providers.get(0).get("name")).isEqualTo("claude");
                    assertThat(providers.get(1).get("name")).isEqualTo("openai");
                })
                .verifyComplete();
    }

    @Pact(consumer = "debate-service")
    public RequestResponsePact chatWithContext(PactDslWithProvider builder) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("provider", "claude");
        requestBody.put("model", "claude-3-opus");
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "Previous debate context..."),
                Map.of("role", "assistant", "content", "I previously argued that..."),
                Map.of("role", "user", "content", "Continue your argument")
        ));
        requestBody.put("contextId", "debate-context-123");

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("response", "Building on my previous point...");
        responseBody.put("model", "claude-3-opus");
        responseBody.put("tokens", 80);
        responseBody.put("contextTokens", 150);

        return builder
                .given("context is provided for chat")
                .uponReceiving("a contextual chat request")
                .path("/tools/chat")
                .method("POST")
                .body(requestBody)
                .willRespondWith()
                .status(200)
                .body(responseBody)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "chatWithContext")
    void testChatWithContext(MockServer mockServer) {
        // Given
        McpLlmClient llmClient = new McpLlmClient(
                WebClient.builder().baseUrl(mockServer.getUrl()).build()
        );

        Map<String, Object> chatRequest = new HashMap<>();
        chatRequest.put("provider", "claude");
        chatRequest.put("model", "claude-3-opus");
        chatRequest.put("messages", List.of(
                Map.of("role", "system", "content", "Previous debate context..."),
                Map.of("role", "assistant", "content", "I previously argued that..."),
                Map.of("role", "user", "content", "Continue your argument")
        ));
        chatRequest.put("contextId", "debate-context-123");

        // When
        Mono<Map<String, Object>> response = llmClient.chat(chatRequest);

        // Then
        StepVerifier.create(response)
                .assertNext(result -> {
                    assertThat(result.get("response")).asString()
                            .contains("Building on my previous point");
                    assertThat(result).containsKey("contextTokens");
                    assertThat(result.get("contextTokens")).isEqualTo(150);
                })
                .verifyComplete();
    }
}