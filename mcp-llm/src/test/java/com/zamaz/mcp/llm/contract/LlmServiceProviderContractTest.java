package com.zamaz.mcp.llm.contract;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.zamaz.mcp.llm.config.LlmProperties;
import com.zamaz.mcp.llm.provider.LlmProvider;
import com.zamaz.mcp.llm.service.LlmService;
import com.zamaz.mcp.llm.service.ProviderRegistry;
import com.zamaz.mcp.llm.service.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("llm-service")
@PactFolder("pacts")
@ActiveProfiles("test")
@DisplayName("LLM Service Provider Contract Tests")
public class LlmServiceProviderContractTest {

    @LocalServerPort
    private int port;

    @MockBean
    private LlmService llmService;

    @MockBean
    private ProviderRegistry providerRegistry;

    @MockBean
    private RateLimitService rateLimitService;

    @MockBean
    private LlmProvider claudeProvider;

    @MockBean
    private LlmProvider openAiProvider;

    @BeforeEach
    void setUp(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
        
        // Reset mocks
        reset(llmService, providerRegistry, rateLimitService, claudeProvider, openAiProvider);
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("providers are available")
    public void providersAreAvailable() {
        // Setup provider registry
        when(claudeProvider.getName()).thenReturn("claude");
        when(claudeProvider.isEnabled()).thenReturn(true);
        when(openAiProvider.getName()).thenReturn("openai");
        when(openAiProvider.isEnabled()).thenReturn(true);

        when(providerRegistry.getEnabledProviders())
                .thenReturn(Arrays.asList(claudeProvider, openAiProvider));
        
        when(providerRegistry.getProvider("claude"))
                .thenReturn(Optional.of(claudeProvider));
        
        when(providerRegistry.getProvider("openai"))
                .thenReturn(Optional.of(openAiProvider));
    }

    @State("claude provider is available for chat")
    public void claudeProviderIsAvailableForChat() {
        // Setup Claude provider
        when(claudeProvider.getName()).thenReturn("claude");
        when(claudeProvider.isEnabled()).thenReturn(true);
        when(providerRegistry.getProvider("claude"))
                .thenReturn(Optional.of(claudeProvider));

        // Setup rate limiting
        when(rateLimitService.checkRateLimit("claude"))
                .thenReturn(Mono.empty());

        // Setup chat response
        Map<String, Object> chatResponse = new HashMap<>();
        chatResponse.put("response", "Hello! I'm Claude, an AI assistant. How can I help you today?");
        chatResponse.put("model", "claude-3-opus");
        chatResponse.put("tokens", 25);
        chatResponse.put("finishReason", "stop");

        when(llmService.chat(any()))
                .thenReturn(Mono.just(chatResponse));
    }

    @State("openai provider is rate limited")
    public void openaiProviderIsRateLimited() {
        // Setup OpenAI provider
        when(openAiProvider.getName()).thenReturn("openai");
        when(openAiProvider.isEnabled()).thenReturn(true);
        when(providerRegistry.getProvider("openai"))
                .thenReturn(Optional.of(openAiProvider));

        // Setup rate limiting to fail
        when(rateLimitService.checkRateLimit("openai"))
                .thenReturn(Mono.error(new RuntimeException("Rate limit exceeded for provider: openai")));
    }

    @State("unknown provider is requested")
    public void unknownProviderIsRequested() {
        // Setup empty provider
        when(providerRegistry.getProvider("unknown-provider"))
                .thenReturn(Optional.empty());
    }

    @State("providers support streaming")
    public void providersSupportStreaming() {
        // Setup streaming support
        when(claudeProvider.getName()).thenReturn("claude");
        when(claudeProvider.isEnabled()).thenReturn(true);
        when(claudeProvider.supportsStreaming()).thenReturn(true);
        
        when(providerRegistry.getProvider("claude"))
                .thenReturn(Optional.of(claudeProvider));

        when(rateLimitService.checkRateLimit("claude"))
                .thenReturn(Mono.empty());

        // Setup streaming response
        when(llmService.streamChat(any()))
                .thenReturn(Mono.just(Map.of(
                        "streamId", UUID.randomUUID().toString(),
                        "status", "streaming"
                )));
    }

    @State("provider has specific model available")
    public void providerHasSpecificModelAvailable() {
        // Setup model availability
        when(claudeProvider.getName()).thenReturn("claude");
        when(claudeProvider.isEnabled()).thenReturn(true);
        when(claudeProvider.getSupportedModels())
                .thenReturn(Arrays.asList("claude-3-opus", "claude-3-sonnet", "claude-instant"));
        
        when(providerRegistry.getProvider("claude"))
                .thenReturn(Optional.of(claudeProvider));
    }

    @State("context is provided for chat")
    public void contextIsProvidedForChat() {
        // Setup provider with context support
        when(claudeProvider.getName()).thenReturn("claude");
        when(claudeProvider.isEnabled()).thenReturn(true);
        when(providerRegistry.getProvider("claude"))
                .thenReturn(Optional.of(claudeProvider));

        when(rateLimitService.checkRateLimit("claude"))
                .thenReturn(Mono.empty());

        // Setup contextual response
        Map<String, Object> contextualResponse = new HashMap<>();
        contextualResponse.put("response", "Based on our previous discussion about AI ethics, I believe...");
        contextualResponse.put("model", "claude-3-opus");
        contextualResponse.put("tokens", 45);
        contextualResponse.put("contextTokens", 150);
        contextualResponse.put("finishReason", "stop");

        when(llmService.chat(argThat(params -> 
                params.containsKey("messages") && 
                ((List<?>) params.get("messages")).size() > 1)))
                .thenReturn(Mono.just(contextualResponse));
    }

    @State("provider supports function calling")
    public void providerSupportsFunctionCalling() {
        // Setup function calling support
        when(openAiProvider.getName()).thenReturn("openai");
        when(openAiProvider.isEnabled()).thenReturn(true);
        when(openAiProvider.supportsFunctionCalling()).thenReturn(true);
        
        when(providerRegistry.getProvider("openai"))
                .thenReturn(Optional.of(openAiProvider));

        when(rateLimitService.checkRateLimit("openai"))
                .thenReturn(Mono.empty());

        // Setup function call response
        Map<String, Object> functionResponse = new HashMap<>();
        functionResponse.put("response", null);
        functionResponse.put("functionCall", Map.of(
                "name", "get_weather",
                "arguments", "{\"location\": \"San Francisco\", \"unit\": \"celsius\"}"
        ));
        functionResponse.put("model", "gpt-4");
        functionResponse.put("tokens", 15);

        when(llmService.chat(argThat(params -> 
                params.containsKey("functions"))))
                .thenReturn(Mono.just(functionResponse));
    }

    @State("provider metrics are available")
    public void providerMetricsAreAvailable() {
        // Setup metrics
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalRequests", 1000);
        metrics.put("successfulRequests", 950);
        metrics.put("failedRequests", 50);
        metrics.put("averageLatency", 250.5);
        metrics.put("tokensUsed", 150000);

        when(llmService.getProviderMetrics("claude"))
                .thenReturn(Mono.just(metrics));
    }

    @State("temperature parameter affects response")
    public void temperatureParameterAffectsResponse() {
        // Setup temperature-based responses
        when(claudeProvider.getName()).thenReturn("claude");
        when(claudeProvider.isEnabled()).thenReturn(true);
        when(providerRegistry.getProvider("claude"))
                .thenReturn(Optional.of(claudeProvider));

        when(rateLimitService.checkRateLimit("claude"))
                .thenReturn(Mono.empty());

        // Low temperature response (more deterministic)
        when(llmService.chat(argThat(params -> 
                params.containsKey("temperature") && 
                (Double) params.get("temperature") < 0.5)))
                .thenReturn(Mono.just(Map.of(
                        "response", "The capital of France is Paris.",
                        "model", "claude-3-opus",
                        "tokens", 10
                )));

        // High temperature response (more creative)
        when(llmService.chat(argThat(params -> 
                params.containsKey("temperature") && 
                (Double) params.get("temperature") >= 0.5)))
                .thenReturn(Mono.just(Map.of(
                        "response", "Ah, Paris! The enchanting capital of France, known as the City of Light...",
                        "model", "claude-3-opus",
                        "tokens", 20
                )));
    }
}