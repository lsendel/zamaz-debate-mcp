package com.zamaz.mcp.llm.testing;

import com.zamaz.mcp.llm.provider.LlmProvider;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Factory for creating pre-configured LLM provider mocks for common testing scenarios.
 */
public class LlmProviderFactory {

    /**
     * Creates a fast, reliable mock provider for unit tests.
     */
    public static MockLlmProvider fastMock() {
        return new MockLlmProvider("fast-mock")
            .withResponseDelay(Duration.ofMillis(10))
            .withQueuedResponses(
                "Quick response 1",
                "Quick response 2",
                "Quick response 3"
            );
    }

    /**
     * Creates a realistic Claude mock with proper pricing and models.
     */
    public static MockLlmProvider claudeMock() {
        return new MockLlmProvider(
                "claude", 
                List.of("claude-3-opus-20240229", "claude-3-sonnet-20240229", "claude-3-haiku-20240307"),
                "claude-3-sonnet-20240229"
            )
            .withTokenPricing(new BigDecimal("3.0"), new BigDecimal("15.0"))
            .withResponseDelay(Duration.ofMillis(100))
            .withResponseTemplate("debate", List.of(
                "I believe this argument has merit because...",
                "However, we should consider the counterpoint that...",
                "The evidence suggests a more nuanced view..."
            ));
    }

    /**
     * Creates a realistic OpenAI mock with GPT models.
     */
    public static MockLlmProvider openAiMock() {
        return new MockLlmProvider(
                "openai",
                List.of("gpt-4", "gpt-4-turbo", "gpt-3.5-turbo"),
                "gpt-4"
            )
            .withTokenPricing(new BigDecimal("30.0"), new BigDecimal("60.0"))
            .withResponseDelay(Duration.ofMillis(150))
            .withResponseTemplate("debate", List.of(
                "From an analytical perspective...",
                "The data indicates that...",
                "A logical approach would be..."
            ));
    }

    /**
     * Creates a Gemini mock with Google's models.
     */
    public static MockLlmProvider geminiMock() {
        return new MockLlmProvider(
                "gemini",
                List.of("gemini-pro", "gemini-ultra"),
                "gemini-pro"
            )
            .withTokenPricing(new BigDecimal("1.0"), new BigDecimal("2.0"))
            .withResponseDelay(Duration.ofMillis(120))
            .withResponseTemplate("debate", List.of(
                "Considering multiple perspectives...",
                "The research shows...",
                "An alternative viewpoint might be..."
            ));
    }

    /**
     * Creates an Ollama mock for local testing.
     */
    public static MockLlmProvider ollamaMock() {
        return new MockLlmProvider(
                "ollama",
                List.of("llama2:70b", "mistral:7b", "codellama:13b"),
                "llama2:70b"
            )
            .withTokenPricing(BigDecimal.ZERO, BigDecimal.ZERO) // Free local model
            .withResponseDelay(Duration.ofMillis(500)) // Slower local processing
            .withResponseTemplate("debate", List.of(
                "As an open-source model, I think...",
                "Based on my training data...",
                "Let me provide a balanced view..."
            ));
    }

    /**
     * Creates a provider that fails frequently for resilience testing.
     */
    public static MockLlmProvider unreliableMock() {
        return new MockLlmProvider("unreliable")
            .withFailureRate(0.3) // 30% failure rate
            .withResponseDelay(Duration.ofSeconds(2))
            .withQueuedResponse("This response came through despite failures");
    }

    /**
     * Creates a provider with rate limiting for testing rate limit handling.
     */
    public static MockLlmProvider rateLimitedMock() {
        return new MockLlmProvider("rate-limited")
            .withRateLimit(5) // Allow 5 requests then rate limit
            .withResponseDelay(Duration.ofMillis(50));
    }

    /**
     * Creates a provider that's temporarily disabled.
     */
    public static MockLlmProvider disabledMock() {
        return new MockLlmProvider("disabled")
            .withEnabled(false);
    }

    /**
     * Creates a provider with custom dynamic responses based on input.
     */
    public static MockLlmProvider smartMock() {
        MockLlmProvider provider = new MockLlmProvider("smart");
        
        // Add dynamic responses based on keywords
        provider.withDynamicResponse("hello", req -> "Hello! How can I help you today?");
        provider.withDynamicResponse("goodbye", req -> "Goodbye! Have a great day!");
        provider.withDynamicResponse("math", req -> "Let me calculate that for you...");
        provider.withDynamicResponse("code", req -> "Here's a code example:\n```\n// Sample code\n```");
        
        return provider;
    }

    /**
     * Creates a mock provider for performance testing.
     */
    public static MockLlmProvider performanceMock() {
        return new MockLlmProvider("performance")
            .withResponseDelay(Duration.ofMillis(1)) // Very fast
            .withQueuedResponses(generateRepeatedResponses("Performance test response", 1000));
    }

    /**
     * Creates a provider for testing large responses.
     */
    public static MockLlmProvider largeResponseMock() {
        StringBuilder largeResponse = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeResponse.append("This is a very long response to test handling of large content. ");
        }
        
        return new MockLlmProvider("large-response")
            .withQueuedResponse(largeResponse.toString());
    }

    /**
     * Creates a provider that simulates different response times for different models.
     */
    public static MockLlmProvider modelVariedMock() {
        MockLlmProvider provider = new MockLlmProvider(
            "model-varied",
            List.of("fast-model", "standard-model", "complex-model"),
            "standard-model"
        );
        
        // Dynamic responses with different delays based on model
        provider.withDynamicResponse("fast-model", req -> {
            try { Thread.sleep(50); } catch (InterruptedException e) {}
            return "Fast model response";
        });
        
        provider.withDynamicResponse("standard-model", req -> {
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            return "Standard model response";
        });
        
        provider.withDynamicResponse("complex-model", req -> {
            try { Thread.sleep(200); } catch (InterruptedException e) {}
            return "Complex model response with detailed analysis";
        });
        
        return provider;
    }

    /**
     * Creates a provider for testing conversation context.
     */
    public static MockLlmProvider conversationalMock() {
        MockLlmProvider provider = new MockLlmProvider("conversational");
        
        provider.withDynamicResponse("context", req -> {
            int messageCount = req.getMessages().size();
            return String.format("This is message %d in our conversation. I remember our previous %d messages.", 
                messageCount, messageCount - 1);
        });
        
        return provider;
    }

    /**
     * Creates a complete provider registry for integration testing.
     */
    public static Map<String, MockLlmProvider> createProviderRegistry() {
        return Map.of(
            "claude", claudeMock(),
            "openai", openAiMock(),
            "gemini", geminiMock(),
            "ollama", ollamaMock(),
            "fast", fastMock(),
            "unreliable", unreliableMock()
        );
    }

    /**
     * Creates providers configured for failover testing.
     */
    public static List<MockLlmProvider> createFailoverChain() {
        return List.of(
            unreliableMock().withEnabled(false), // Primary fails
            rateLimitedMock(),                   // Secondary has rate limits
            claudeMock()                         // Tertiary works reliably
        );
    }

    // Helper methods
    
    private static String[] generateRepeatedResponses(String base, int count) {
        String[] responses = new String[count];
        for (int i = 0; i < count; i++) {
            responses[i] = base + " #" + (i + 1);
        }
        return responses;
    }
}