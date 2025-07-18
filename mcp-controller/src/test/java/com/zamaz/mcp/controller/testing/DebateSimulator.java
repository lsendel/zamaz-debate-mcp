package com.zamaz.mcp.controller.testing;

import com.zamaz.mcp.controller.domain.model.Debate;
import com.zamaz.mcp.controller.domain.model.DebateStatus;
import com.zamaz.mcp.controller.domain.model.Participant;
import com.zamaz.mcp.controller.domain.model.Turn;
import com.zamaz.mcp.controller.service.DebateService;
import com.zamaz.mcp.llm.testing.MockLlmProvider;
import com.zamaz.mcp.security.testing.SecurityTestContext;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Simulates complete debate flows for testing.
 * Provides realistic debate scenarios with configurable participants, rules, and outcomes.
 */
public class DebateSimulator {

    private final DebateService debateService;
    private final Map<String, MockLlmProvider> llmProviders;
    private final AtomicInteger simulationCounter = new AtomicInteger(0);
    
    // Simulation configuration
    private String organizationId = "test-org";
    private String userId = "test-user";
    private int maxRounds = 5;
    private Duration turnTimeout = Duration.ofMinutes(2);
    private boolean strictTurnOrder = true;
    private boolean allowObservers = false;
    
    // Event handlers
    private final List<Consumer<DebateEvent>> eventHandlers = new ArrayList<>();
    private final List<DebateScenario> scenarios = new ArrayList<>();

    public DebateSimulator(DebateService debateService, Map<String, MockLlmProvider> llmProviders) {
        this.debateService = debateService;
        this.llmProviders = new HashMap<>(llmProviders);
        initializeDefaultScenarios();
    }

    /**
     * Simulates a complete debate from start to finish.
     */
    public DebateSimulationResult simulateCompleteDebate(DebateConfig config) {
        String simulationId = "sim-" + simulationCounter.incrementAndGet();
        DebateSimulationResult result = new DebateSimulationResult(simulationId);
        
        try {
            // Setup security context
            SecurityTestContext.runAs(SecurityTestContext.organizationAdmin(organizationId), () -> {
                
                // Create debate
                Debate debate = createDebate(config);
                result.setDebateId(debate.getId());
                emitEvent(new DebateEvent(DebateEventType.DEBATE_CREATED, debate, null));
                
                // Add participants
                for (ParticipantConfig participantConfig : config.getParticipants()) {
                    Participant participant = addParticipant(debate, participantConfig);
                    result.addParticipant(participant);
                    emitEvent(new DebateEvent(DebateEventType.PARTICIPANT_ADDED, debate, participant));
                }
                
                // Start debate
                debate = startDebate(debate);
                result.setStartTime(Instant.now());
                emitEvent(new DebateEvent(DebateEventType.DEBATE_STARTED, debate, null));
                
                // Simulate rounds
                for (int round = 1; round <= maxRounds && !isDebateFinished(debate); round++) {
                    result.setCurrentRound(round);
                    simulateRound(debate, config, result, round);
                    
                    if (config.shouldStopAfterRound(round)) {
                        break;
                    }
                }
                
                // Finish debate
                debate = finishDebate(debate);
                result.setEndTime(Instant.now());
                result.setFinalStatus(debate.getStatus());
                emitEvent(new DebateEvent(DebateEventType.DEBATE_FINISHED, debate, null));
                
            });
            
        } catch (Exception e) {
            result.setError(e);
            emitEvent(new DebateEvent(DebateEventType.SIMULATION_ERROR, null, null, e));
        }
        
        return result;
    }

    /**
     * Simulates multiple debates concurrently.
     */
    public List<DebateSimulationResult> simulateConcurrentDebates(List<DebateConfig> configs) {
        return configs.parallelStream()
            .map(this::simulateCompleteDebate)
            .toList();
    }

    /**
     * Runs a predefined scenario.
     */
    public DebateSimulationResult runScenario(String scenarioName) {
        DebateScenario scenario = scenarios.stream()
            .filter(s -> s.getName().equals(scenarioName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Scenario not found: " + scenarioName));
        
        return simulateCompleteDebate(scenario.getConfig());
    }

    /**
     * Simulates a single round of debate.
     */
    private void simulateRound(Debate debate, DebateConfig config, DebateSimulationResult result, int round) {
        emitEvent(new DebateEvent(DebateEventType.ROUND_STARTED, debate, null, round));
        
        List<Participant> activeParticipants = getActiveParticipants(debate);
        
        for (Participant participant : activeParticipants) {
            if (strictTurnOrder && !isParticipantTurn(debate, participant)) {
                continue;
            }
            
            try {
                Turn turn = simulateParticipantTurn(debate, participant, config);
                result.addTurn(turn);
                emitEvent(new DebateEvent(DebateEventType.TURN_COMPLETED, debate, participant, turn));
                
                // Check if debate should end after this turn
                if (shouldEndDebate(debate, config)) {
                    break;
                }
                
            } catch (Exception e) {
                result.addError(participant.getId(), e);
                emitEvent(new DebateEvent(DebateEventType.TURN_FAILED, debate, participant, e));
            }
        }
        
        emitEvent(new DebateEvent(DebateEventType.ROUND_COMPLETED, debate, null, round));
    }

    /**
     * Simulates a single participant's turn.
     */
    private Turn simulateParticipantTurn(Debate debate, Participant participant, DebateConfig config) {
        MockLlmProvider provider = llmProviders.get(participant.getLlmProvider());
        if (provider == null) {
            throw new RuntimeException("LLM provider not found: " + participant.getLlmProvider());
        }
        
        // Generate context for the turn
        String context = buildTurnContext(debate, participant);
        String prompt = config.getPromptTemplate().replace("{{context}}", context);
        
        // Get response from LLM provider
        // Note: This would normally use the actual LLM service
        // For simulation, we use the mock provider directly
        String response = getSimulatedResponse(provider, prompt, participant);
        
        // Create and save turn
        Turn turn = Turn.builder()
            .debateId(debate.getId())
            .participantId(participant.getId())
            .content(response)
            .timestamp(Instant.now())
            .turnNumber(getNextTurnNumber(debate))
            .build();
        
        // Save turn (in real implementation)
        // debateService.addTurn(debate.getId(), turn);
        
        return turn;
    }

    private String getSimulatedResponse(MockLlmProvider provider, String prompt, Participant participant) {
        // In a real implementation, this would call the provider
        // For simulation, we return a predefined response based on participant characteristics
        List<String> participantResponses = List.of(
            String.format("%s argues: %s", participant.getName(), generateArgumentResponse()),
            String.format("%s counters: %s", participant.getName(), generateCounterResponse()),
            String.format("%s concludes: %s", participant.getName(), generateConclusionResponse())
        );
        
        return participantResponses.get(getNextTurnNumber(null) % participantResponses.size());
    }

    private String generateArgumentResponse() {
        List<String> templates = List.of(
            "Based on the evidence presented, I believe that...",
            "The fundamental issue here is...",
            "We must consider the implications of...",
            "Historical precedent shows that...",
            "From a logical standpoint..."
        );
        return templates.get(new Random().nextInt(templates.size()));
    }

    private String generateCounterResponse() {
        List<String> templates = List.of(
            "However, this argument fails to consider...",
            "I respectfully disagree because...",
            "The counterpoint to this is...",
            "An alternative perspective would be...",
            "This overlooks the fact that..."
        );
        return templates.get(new Random().nextInt(templates.size()));
    }

    private String generateConclusionResponse() {
        List<String> templates = List.of(
            "In conclusion, the evidence clearly supports...",
            "To summarize my position...",
            "The logical conclusion is...",
            "Weighing all factors, I maintain that...",
            "The strongest argument remains..."
        );
        return templates.get(new Random().nextInt(templates.size()));
    }

    // Helper methods

    private Debate createDebate(DebateConfig config) {
        return Debate.builder()
            .id("debate-" + UUID.randomUUID())
            .title(config.getTitle())
            .description(config.getDescription())
            .organizationId(organizationId)
            .createdBy(userId)
            .status(DebateStatus.CREATED)
            .maxRounds(config.getMaxRounds())
            .timeLimit(config.getTurnTimeout())
            .createdAt(Instant.now())
            .build();
    }

    private Participant addParticipant(Debate debate, ParticipantConfig config) {
        return Participant.builder()
            .id("participant-" + UUID.randomUUID())
            .debateId(debate.getId())
            .name(config.getName())
            .llmProvider(config.getLlmProvider())
            .model(config.getModel())
            .systemPrompt(config.getSystemPrompt())
            .position(config.getPosition())
            .order(config.getOrder())
            .active(true)
            .build();
    }

    private Debate startDebate(Debate debate) {
        // In real implementation: return debateService.startDebate(debate.getId());
        return debate.toBuilder().status(DebateStatus.IN_PROGRESS).build();
    }

    private Debate finishDebate(Debate debate) {
        // In real implementation: return debateService.finishDebate(debate.getId());
        return debate.toBuilder().status(DebateStatus.COMPLETED).build();
    }

    private List<Participant> getActiveParticipants(Debate debate) {
        // In real implementation, this would fetch from database
        return Collections.emptyList(); // Placeholder
    }

    private boolean isParticipantTurn(Debate debate, Participant participant) {
        // Implement turn order logic
        return true; // Simplified for testing
    }

    private boolean isDebateFinished(Debate debate) {
        return debate.getStatus() == DebateStatus.COMPLETED ||
               debate.getStatus() == DebateStatus.CANCELLED;
    }

    private boolean shouldEndDebate(Debate debate, DebateConfig config) {
        return config.getEndCondition().test(debate);
    }

    private String buildTurnContext(Debate debate, Participant participant) {
        // Build context from previous turns
        return String.format("Debate: %s\nParticipant: %s\nPosition: %s", 
            debate.getTitle(), participant.getName(), participant.getPosition());
    }

    private int getNextTurnNumber(Debate debate) {
        // In real implementation, count existing turns
        return 1; // Simplified
    }

    private void emitEvent(DebateEvent event) {
        eventHandlers.forEach(handler -> {
            try {
                handler.accept(event);
            } catch (Exception e) {
                // Log but don't fail simulation
            }
        });
    }

    private void initializeDefaultScenarios() {
        // AI Ethics Debate
        scenarios.add(new DebateScenario("ai-ethics", DebateConfig.builder()
            .title("AI Ethics in Healthcare")
            .description("Should AI be allowed to make medical decisions?")
            .addParticipant("Claude", "claude", "claude-3-sonnet", "You argue in favor of AI in healthcare", "pro")
            .addParticipant("GPT-4", "openai", "gpt-4", "You argue against AI in healthcare", "con")
            .maxRounds(3)
            .promptTemplate("Continue the debate: {{context}}")
            .endCondition(debate -> false) // Run all rounds
            .build()));

        // Climate Change Debate
        scenarios.add(new DebateScenario("climate-change", DebateConfig.builder()
            .title("Climate Change Solutions")
            .description("What is the most effective approach to combat climate change?")
            .addParticipant("Gemini", "gemini", "gemini-pro", "Focus on renewable energy solutions", "renewable")
            .addParticipant("Claude", "claude", "claude-3-haiku", "Focus on carbon capture technology", "capture")
            .addParticipant("GPT-3.5", "openai", "gpt-3.5-turbo", "Focus on policy and regulation", "policy")
            .maxRounds(4)
            .promptTemplate("Debate topic: {{context}}")
            .build()));
    }

    // Configuration and builder methods

    public DebateSimulator withOrganization(String organizationId) {
        this.organizationId = organizationId;
        return this;
    }

    public DebateSimulator withUser(String userId) {
        this.userId = userId;
        return this;
    }

    public DebateSimulator withMaxRounds(int maxRounds) {
        this.maxRounds = maxRounds;
        return this;
    }

    public DebateSimulator withTurnTimeout(Duration timeout) {
        this.turnTimeout = timeout;
        return this;
    }

    public DebateSimulator withStrictTurnOrder(boolean strict) {
        this.strictTurnOrder = strict;
        return this;
    }

    public DebateSimulator onEvent(Consumer<DebateEvent> handler) {
        this.eventHandlers.add(handler);
        return this;
    }

    public DebateSimulator addScenario(DebateScenario scenario) {
        this.scenarios.add(scenario);
        return this;
    }

    public List<String> getAvailableScenarios() {
        return scenarios.stream().map(DebateScenario::getName).toList();
    }

    // Data classes for configuration and results

    public static class DebateConfig {
        private String title;
        private String description;
        private List<ParticipantConfig> participants = new ArrayList<>();
        private int maxRounds = 5;
        private Duration turnTimeout = Duration.ofMinutes(2);
        private String promptTemplate = "{{context}}";
        private Predicate<Debate> endCondition = debate -> false;
        private Map<Integer, Boolean> stopAfterRounds = new HashMap<>();

        // Builder pattern implementation
        public static DebateConfigBuilder builder() {
            return new DebateConfigBuilder();
        }

        // Getters
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public List<ParticipantConfig> getParticipants() { return participants; }
        public int getMaxRounds() { return maxRounds; }
        public Duration getTurnTimeout() { return turnTimeout; }
        public String getPromptTemplate() { return promptTemplate; }
        public Predicate<Debate> getEndCondition() { return endCondition; }
        public boolean shouldStopAfterRound(int round) { return stopAfterRounds.getOrDefault(round, false); }

        public static class DebateConfigBuilder {
            private final DebateConfig config = new DebateConfig();

            public DebateConfigBuilder title(String title) {
                config.title = title;
                return this;
            }

            public DebateConfigBuilder description(String description) {
                config.description = description;
                return this;
            }

            public DebateConfigBuilder addParticipant(String name, String provider, String model, String systemPrompt, String position) {
                config.participants.add(new ParticipantConfig(name, provider, model, systemPrompt, position, config.participants.size()));
                return this;
            }

            public DebateConfigBuilder maxRounds(int rounds) {
                config.maxRounds = rounds;
                return this;
            }

            public DebateConfigBuilder turnTimeout(Duration timeout) {
                config.turnTimeout = timeout;
                return this;
            }

            public DebateConfigBuilder promptTemplate(String template) {
                config.promptTemplate = template;
                return this;
            }

            public DebateConfigBuilder endCondition(Predicate<Debate> condition) {
                config.endCondition = condition;
                return this;
            }

            public DebateConfigBuilder stopAfterRound(int round) {
                config.stopAfterRounds.put(round, true);
                return this;
            }

            public DebateConfig build() {
                return config;
            }
        }
    }

    public static class ParticipantConfig {
        private final String name;
        private final String llmProvider;
        private final String model;
        private final String systemPrompt;
        private final String position;
        private final int order;

        public ParticipantConfig(String name, String llmProvider, String model, String systemPrompt, String position, int order) {
            this.name = name;
            this.llmProvider = llmProvider;
            this.model = model;
            this.systemPrompt = systemPrompt;
            this.position = position;
            this.order = order;
        }

        // Getters
        public String getName() { return name; }
        public String getLlmProvider() { return llmProvider; }
        public String getModel() { return model; }
        public String getSystemPrompt() { return systemPrompt; }
        public String getPosition() { return position; }
        public int getOrder() { return order; }
    }

    public static class DebateScenario {
        private final String name;
        private final DebateConfig config;

        public DebateScenario(String name, DebateConfig config) {
            this.name = name;
            this.config = config;
        }

        public String getName() { return name; }
        public DebateConfig getConfig() { return config; }
    }

    public enum DebateEventType {
        DEBATE_CREATED, DEBATE_STARTED, DEBATE_FINISHED,
        PARTICIPANT_ADDED, PARTICIPANT_REMOVED,
        ROUND_STARTED, ROUND_COMPLETED,
        TURN_COMPLETED, TURN_FAILED,
        SIMULATION_ERROR
    }

    public static class DebateEvent {
        private final DebateEventType type;
        private final Debate debate;
        private final Participant participant;
        private final Object data;
        private final Instant timestamp;

        public DebateEvent(DebateEventType type, Debate debate, Participant participant) {
            this(type, debate, participant, null);
        }

        public DebateEvent(DebateEventType type, Debate debate, Participant participant, Object data) {
            this.type = type;
            this.debate = debate;
            this.participant = participant;
            this.data = data;
            this.timestamp = Instant.now();
        }

        // Getters
        public DebateEventType getType() { return type; }
        public Debate getDebate() { return debate; }
        public Participant getParticipant() { return participant; }
        public Object getData() { return data; }
        public Instant getTimestamp() { return timestamp; }
    }

    public static class DebateSimulationResult {
        private final String simulationId;
        private String debateId;
        private Instant startTime;
        private Instant endTime;
        private int currentRound;
        private DebateStatus finalStatus;
        private final List<Participant> participants = new ArrayList<>();
        private final List<Turn> turns = new ArrayList<>();
        private final Map<String, Exception> errors = new HashMap<>();
        private Exception error;

        public DebateSimulationResult(String simulationId) {
            this.simulationId = simulationId;
        }

        // Getters and setters
        public String getSimulationId() { return simulationId; }
        public String getDebateId() { return debateId; }
        public void setDebateId(String debateId) { this.debateId = debateId; }
        public Instant getStartTime() { return startTime; }
        public void setStartTime(Instant startTime) { this.startTime = startTime; }
        public Instant getEndTime() { return endTime; }
        public void setEndTime(Instant endTime) { this.endTime = endTime; }
        public int getCurrentRound() { return currentRound; }
        public void setCurrentRound(int currentRound) { this.currentRound = currentRound; }
        public DebateStatus getFinalStatus() { return finalStatus; }
        public void setFinalStatus(DebateStatus finalStatus) { this.finalStatus = finalStatus; }
        public List<Participant> getParticipants() { return participants; }
        public void addParticipant(Participant participant) { this.participants.add(participant); }
        public List<Turn> getTurns() { return turns; }
        public void addTurn(Turn turn) { this.turns.add(turn); }
        public Map<String, Exception> getErrors() { return errors; }
        public void addError(String participantId, Exception error) { this.errors.put(participantId, error); }
        public Exception getError() { return error; }
        public void setError(Exception error) { this.error = error; }

        public Duration getDuration() {
            if (startTime == null || endTime == null) return Duration.ZERO;
            return Duration.between(startTime, endTime);
        }

        public boolean isSuccessful() {
            return error == null && finalStatus == DebateStatus.COMPLETED;
        }

        public int getTurnCount() {
            return turns.size();
        }

        public Map<String, Integer> getParticipantTurnCounts() {
            Map<String, Integer> counts = new HashMap<>();
            for (Turn turn : turns) {
                counts.merge(turn.getParticipantId(), 1, Integer::sum);
            }
            return counts;
        }
    }
}