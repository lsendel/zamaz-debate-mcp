package com.zamaz.mcp.llm.testing;

import com.zamaz.mcp.llm.domain.model.CompletionRequest;
import com.zamaz.mcp.llm.model.CompletionResponse;
import com.zamaz.mcp.llm.model.Choice;
import com.zamaz.mcp.llm.model.Message;
import com.zamaz.mcp.llm.model.Usage;
import com.zamaz.mcp.llm.provider.LlmProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Mock LLM provider for testing purposes.
 * Supports configurable responses, delays, failures, and token tracking.
 */
public class MockLlmProvider implements LlmProvider {

    private final String name;
    private final List<String> supportedModels;
    private final String defaultModel;
    private boolean enabled = true;
    private boolean healthy = true;
    
    // Response configuration
    private final Map<String, List<String>> responseTemplates = new HashMap<>();
    private final Map<String, Function<CompletionRequest, String>> dynamicResponses = new HashMap<>();
    private final Queue<String> queuedResponses = new LinkedList<>();
    
    // Behavior configuration
    private Duration responseDelay = Duration.ZERO;
    private double failureRate = 0.0;
    private int maxRetries = 3;
    private boolean simulateRateLimit = false;
    private int rateLimitResetAfter = 0;
    
    // Usage tracking
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final List<CompletionRequest> requestHistory = new ArrayList<>();
    private final Map<String, Integer> modelUsage = new HashMap<>();
    private final Map<String, Integer> tokenUsage = new HashMap<>();
    
    // Token calculation
    private int baseTokensPerMessage = 10;
    private double tokensPerCharacter = 0.25;
    private BigDecimal inputCostPer1MTokens = new BigDecimal("3.0");
    private BigDecimal outputCostPer1MTokens = new BigDecimal("15.0");

    public MockLlmProvider(String name) {
        this.name = name;
        this.supportedModels = List.of(name + "-standard", name + "-fast", name + "-advanced");
        this.defaultModel = name + "-standard";
        initializeDefaultResponses();
    }

    public MockLlmProvider(String name, List<String> supportedModels, String defaultModel) {
        this.name = name;
        this.supportedModels = new ArrayList<>(supportedModels);
        this.defaultModel = defaultModel;
        initializeDefaultResponses();
    }

    private void initializeDefaultResponses() {
        // Default responses for different types of prompts
        responseTemplates.put("default", List.of(
            "This is a mock response from " + name,
            "Another simulated response for testing",
            "Mock LLM provider " + name + " responding"
        ));
        
        responseTemplates.put("debate", List.of(
            "I argue that this topic is complex and requires careful consideration.",
            "From my perspective, the evidence suggests a different approach.",
            "Let me present a counterargument to this position."
        ));
        
        responseTemplates.put("question", List.of(
            "Based on the information provided, the answer is...",
            "This is an interesting question that requires analysis of...",
            "To address your question, I would consider..."
        ));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public List<String> getSupportedModels() {
        return new ArrayList<>(supportedModels);
    }

    @Override
    public String getDefaultModel() {
        return defaultModel;
    }

    @Override
    public Mono<CompletionResponse> complete(CompletionRequest request) {
        return Mono.fromCallable(() -> {
            requestCount.incrementAndGet();
            requestHistory.add(request);
            modelUsage.merge(request.getModel(), 1, Integer::sum);
            
            // Simulate failures
            if (shouldSimulateFailure()) {
                throw new RuntimeException("Simulated LLM provider failure");
            }
            
            // Simulate rate limiting
            if (simulateRateLimit && rateLimitResetAfter > 0) {
                rateLimitResetAfter--;
                throw new RuntimeException("Rate limit exceeded");
            }
            
            return createResponse(request);
        })
        .delayElement(responseDelay);
    }

    @Override
    public Flux<String> streamComplete(CompletionRequest request) {
        return Flux.fromIterable(generateStreamingResponse(request))
            .delayElements(Duration.ofMillis(50)) // Simulate streaming delay
            .doOnSubscribe(sub -> {
                requestCount.incrementAndGet();
                requestHistory.add(request);
                modelUsage.merge(request.getModel(), 1, Integer::sum);
            });
    }

    @Override
    public Mono<Integer> countTokens(String text) {
        return Mono.fromCallable(() -> {
            int tokens = baseTokensPerMessage + (int) (text.length() * tokensPerCharacter);
            tokenUsage.merge("counted", tokens, Integer::sum);
            return tokens;
        });
    }

    @Override
    public Mono<Boolean> checkHealth() {
        return Mono.fromCallable(() -> healthy)
            .delayElement(Duration.ofMillis(10)); // Simulate health check delay
    }

    // Configuration methods

    public MockLlmProvider withEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public MockLlmProvider withHealthy(boolean healthy) {
        this.healthy = healthy;
        return this;
    }

    public MockLlmProvider withResponseDelay(Duration delay) {
        this.responseDelay = delay;
        return this;
    }

    public MockLlmProvider withFailureRate(double rate) {
        this.failureRate = Math.max(0, Math.min(1, rate));
        return this;
    }

    public MockLlmProvider withRateLimit(int requestsBeforeLimit) {
        this.simulateRateLimit = true;
        this.rateLimitResetAfter = requestsBeforeLimit;
        return this;
    }

    public MockLlmProvider withQueuedResponse(String response) {
        this.queuedResponses.offer(response);
        return this;
    }

    public MockLlmProvider withQueuedResponses(String... responses) {
        Collections.addAll(this.queuedResponses, responses);
        return this;
    }

    public MockLlmProvider withResponseTemplate(String key, List<String> responses) {
        this.responseTemplates.put(key, new ArrayList<>(responses));
        return this;
    }

    public MockLlmProvider withDynamicResponse(String condition, Function<CompletionRequest, String> responseFunction) {
        this.dynamicResponses.put(condition, responseFunction);
        return this;
    }

    public MockLlmProvider withTokenPricing(BigDecimal inputCost, BigDecimal outputCost) {
        this.inputCostPer1MTokens = inputCost;
        this.outputCostPer1MTokens = outputCost;
        return this;
    }

    // Helper methods

    private CompletionResponse createResponse(CompletionRequest request) {
        String responseText = generateResponseText(request);
        int inputTokens = estimateTokens(getRequestText(request));
        int outputTokens = estimateTokens(responseText);
        
        Usage usage = new Usage(
            inputTokens,
            outputTokens,
            inputTokens + outputTokens,
            calculateCost(inputTokens, outputTokens)
        );

        Choice choice = new Choice(
            0,
            new Message("assistant", responseText),
            "stop"
        );

        return new CompletionResponse(
            UUID.randomUUID().toString(),
            name,
            request.getModel(),
            List.of(choice),
            usage,
            Instant.now(),
            responseDelay.toMillis(),
            Map.of("mock", true, "provider", name)
        );
    }

    private List<String> generateStreamingResponse(CompletionRequest request) {
        String fullResponse = generateResponseText(request);
        List<String> chunks = new ArrayList<>();
        
        // Split response into chunks for streaming
        String[] words = fullResponse.split(" ");
        StringBuilder chunk = new StringBuilder();
        
        for (String word : words) {
            chunk.append(word).append(" ");
            if (chunk.length() > 20) { // Chunk size
                chunks.add(chunk.toString());
                chunk = new StringBuilder();
            }
        }
        
        if (chunk.length() > 0) {
            chunks.add(chunk.toString());
        }
        
        return chunks;
    }

    private String generateResponseText(CompletionRequest request) {
        // Check for queued responses first
        if (!queuedResponses.isEmpty()) {
            return queuedResponses.poll();
        }
        
        // Check for dynamic responses
        String requestText = getRequestText(request).toLowerCase();
        for (Map.Entry<String, Function<CompletionRequest, String>> entry : dynamicResponses.entrySet()) {
            if (requestText.contains(entry.getKey().toLowerCase())) {
                return entry.getValue().apply(request);
            }
        }
        
        // Use template responses
        String template = determineTemplate(requestText);
        List<String> responses = responseTemplates.get(template);
        if (responses != null && !responses.isEmpty()) {
            return responses.get(requestCount.get() % responses.size());
        }
        
        return responseTemplates.get("default").get(0);
    }

    private String determineTemplate(String requestText) {
        if (requestText.contains("debate") || requestText.contains("argue")) {
            return "debate";
        } else if (requestText.contains("?") || requestText.contains("question")) {
            return "question";
        }
        return "default";
    }

    private String getRequestText(CompletionRequest request) {
        return request.getMessages().stream()
            .map(Message::getContent)
            .reduce("", (a, b) -> a + " " + b);
    }

    private int estimateTokens(String text) {
        return baseTokensPerMessage + (int) (text.length() * tokensPerCharacter);
    }

    private BigDecimal calculateCost(int inputTokens, int outputTokens) {
        BigDecimal inputCost = inputCostPer1MTokens
            .multiply(BigDecimal.valueOf(inputTokens))
            .divide(BigDecimal.valueOf(1_000_000), 6, BigDecimal.ROUND_HALF_UP);
        
        BigDecimal outputCost = outputCostPer1MTokens
            .multiply(BigDecimal.valueOf(outputTokens))
            .divide(BigDecimal.valueOf(1_000_000), 6, BigDecimal.ROUND_HALF_UP);
        
        return inputCost.add(outputCost);
    }

    private boolean shouldSimulateFailure() {
        return failureRate > 0 && Math.random() < failureRate;
    }

    // Inspection methods for testing

    public int getRequestCount() {
        return requestCount.get();
    }

    public List<CompletionRequest> getRequestHistory() {
        return new ArrayList<>(requestHistory);
    }

    public Map<String, Integer> getModelUsage() {
        return new HashMap<>(modelUsage);
    }

    public Map<String, Integer> getTokenUsage() {
        return new HashMap<>(tokenUsage);
    }

    public void reset() {
        requestCount.set(0);
        requestHistory.clear();
        modelUsage.clear();
        tokenUsage.clear();
        queuedResponses.clear();
        enabled = true;
        healthy = true;
        responseDelay = Duration.ZERO;
        failureRate = 0.0;
        simulateRateLimit = false;
        rateLimitResetAfter = 0;
    }
}