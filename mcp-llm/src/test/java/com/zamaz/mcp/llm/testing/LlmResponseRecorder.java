package com.zamaz.mcp.llm.testing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zamaz.mcp.llm.domain.model.CompletionRequest;
import com.zamaz.mcp.llm.model.CompletionResponse;
import com.zamaz.mcp.llm.provider.LlmProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Records and replays LLM provider responses for testing.
 * Enables testing with real API responses without making actual API calls.
 */
public class LlmResponseRecorder {

    private final LlmProvider realProvider;
    private final ObjectMapper objectMapper;
    private final Path recordingDirectory;
    private final Map<String, RecordedResponse> recordings = new ConcurrentHashMap<>();
    private boolean recordingMode = false;
    private boolean replayMode = false;
    private String currentSession;

    public LlmResponseRecorder(LlmProvider realProvider) {
        this(realProvider, Paths.get("target/test-recordings"));
    }

    public LlmResponseRecorder(LlmProvider realProvider, Path recordingDirectory) {
        this.realProvider = realProvider;
        this.recordingDirectory = recordingDirectory;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        try {
            Files.createDirectories(recordingDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create recording directory", e);
        }
    }

    /**
     * Starts recording mode with a session name.
     */
    public LlmResponseRecorder startRecording(String sessionName) {
        this.recordingMode = true;
        this.replayMode = false;
        this.currentSession = sessionName;
        return this;
    }

    /**
     * Starts replay mode with a session name.
     */
    public LlmResponseRecorder startReplay(String sessionName) {
        this.replayMode = true;
        this.recordingMode = false;
        this.currentSession = sessionName;
        loadRecordings(sessionName);
        return this;
    }

    /**
     * Stops recording/replay mode and saves recordings.
     */
    public LlmResponseRecorder stop() {
        if (recordingMode) {
            saveRecordings(currentSession);
        }
        this.recordingMode = false;
        this.replayMode = false;
        this.currentSession = null;
        return this;
    }

    /**
     * Completes a request using recording/replay or real provider.
     */
    public Mono<CompletionResponse> complete(CompletionRequest request) {
        String requestKey = generateRequestKey(request);
        
        if (replayMode && recordings.containsKey(requestKey)) {
            // Replay recorded response
            RecordedResponse recorded = recordings.get(requestKey);
            return Mono.just(recorded.response)
                .delayElement(recorded.responseTime); // Simulate original response time
        }
        
        if (recordingMode) {
            // Record real response
            Instant startTime = Instant.now();
            return realProvider.complete(request)
                .doOnNext(response -> {
                    Instant endTime = Instant.now();
                    Duration responseTime = Duration.between(startTime, endTime);
                    recordings.put(requestKey, new RecordedResponse(
                        request, response, responseTime, Instant.now()
                    ));
                });
        }
        
        // Pass through to real provider
        return realProvider.complete(request);
    }

    /**
     * Streams completion using recording/replay or real provider.
     */
    public Flux<String> streamComplete(CompletionRequest request) {
        String requestKey = generateRequestKey(request) + "_stream";
        
        if (replayMode && recordings.containsKey(requestKey)) {
            RecordedResponse recorded = recordings.get(requestKey);
            return Flux.fromIterable(recorded.streamChunks)
                .delayElements(recorded.responseTime.dividedBy(recorded.streamChunks.size()));
        }
        
        if (recordingMode) {
            List<String> chunks = new ArrayList<>();
            Instant startTime = Instant.now();
            
            return realProvider.streamComplete(request)
                .doOnNext(chunks::add)
                .doOnComplete(() -> {
                    Instant endTime = Instant.now();
                    Duration responseTime = Duration.between(startTime, endTime);
                    recordings.put(requestKey, new RecordedResponse(
                        request, null, responseTime, Instant.now(), chunks
                    ));
                });
        }
        
        return realProvider.streamComplete(request);
    }

    /**
     * Loads a specific recording by name.
     */
    public LlmResponseRecorder loadRecording(String name) {
        Path recordingFile = recordingDirectory.resolve(name + ".json");
        if (!Files.exists(recordingFile)) {
            throw new RuntimeException("Recording not found: " + name);
        }
        
        try {
            String json = Files.readString(recordingFile);
            Map<String, RecordedResponse> loaded = objectMapper.readValue(
                json, new TypeReference<Map<String, RecordedResponse>>() {}
            );
            recordings.putAll(loaded);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load recording: " + name, e);
        }
        
        return this;
    }

    /**
     * Saves current recordings with a name.
     */
    public LlmResponseRecorder saveRecording(String name) {
        Path recordingFile = recordingDirectory.resolve(name + ".json");
        
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(recordings);
            Files.writeString(recordingFile, json);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save recording: " + name, e);
        }
        
        return this;
    }

    /**
     * Creates a mock provider that uses recorded responses.
     */
    public MockLlmProvider createMockFromRecordings() {
        MockLlmProvider mock = new MockLlmProvider(realProvider.getName() + "-recorded");
        
        for (RecordedResponse recorded : recordings.values()) {
            if (recorded.response != null) {
                mock.withQueuedResponse(recorded.response.getChoices().get(0).getMessage().getContent());
            }
        }
        
        return mock;
    }

    /**
     * Gets statistics about the current recordings.
     */
    public RecordingStats getStats() {
        Map<String, Integer> modelCounts = new HashMap<>();
        Map<String, Long> avgResponseTimes = new HashMap<>();
        int totalRequests = recordings.size();
        
        for (RecordedResponse recorded : recordings.values()) {
            String model = recorded.request.getModel();
            modelCounts.merge(model, 1, Integer::sum);
            avgResponseTimes.merge(model, recorded.responseTime.toMillis(), Long::sum);
        }
        
        // Calculate averages
        avgResponseTimes.replaceAll((model, total) -> total / modelCounts.get(model));
        
        return new RecordingStats(totalRequests, modelCounts, avgResponseTimes);
    }

    /**
     * Lists available recordings.
     */
    public List<String> listRecordings() {
        try {
            return Files.list(recordingDirectory)
                .filter(path -> path.toString().endsWith(".json"))
                .map(path -> path.getFileName().toString().replace(".json", ""))
                .sorted()
                .toList();
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Clears all current recordings.
     */
    public LlmResponseRecorder clearRecordings() {
        recordings.clear();
        return this;
    }

    // Private helper methods

    private String generateRequestKey(CompletionRequest request) {
        // Create a deterministic key based on request content
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(request.getModel()).append(":");
        
        for (var message : request.getMessages()) {
            keyBuilder.append(message.getRole()).append(":")
                .append(message.getContent().hashCode()).append(";");
        }
        
        if (request.getSystemMessage() != null) {
            keyBuilder.append("system:").append(request.getSystemMessage().hashCode());
        }
        
        return keyBuilder.toString();
    }

    private void loadRecordings(String sessionName) {
        recordings.clear();
        Path sessionFile = recordingDirectory.resolve(sessionName + ".json");
        
        if (Files.exists(sessionFile)) {
            try {
                String json = Files.readString(sessionFile);
                Map<String, RecordedResponse> loaded = objectMapper.readValue(
                    json, new TypeReference<Map<String, RecordedResponse>>() {}
                );
                recordings.putAll(loaded);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load session: " + sessionName, e);
            }
        }
    }

    private void saveRecordings(String sessionName) {
        Path sessionFile = recordingDirectory.resolve(sessionName + ".json");
        
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(recordings);
            Files.writeString(sessionFile, json);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save session: " + sessionName, e);
        }
    }

    // Data classes

    public static class RecordedResponse {
        public CompletionRequest request;
        public CompletionResponse response;
        public Duration responseTime;
        public Instant recordedAt;
        public List<String> streamChunks;

        // Default constructor for Jackson
        public RecordedResponse() {}

        public RecordedResponse(CompletionRequest request, CompletionResponse response, 
                               Duration responseTime, Instant recordedAt) {
            this(request, response, responseTime, recordedAt, Collections.emptyList());
        }

        public RecordedResponse(CompletionRequest request, CompletionResponse response, 
                               Duration responseTime, Instant recordedAt, List<String> streamChunks) {
            this.request = request;
            this.response = response;
            this.responseTime = responseTime;
            this.recordedAt = recordedAt;
            this.streamChunks = new ArrayList<>(streamChunks);
        }
    }

    public static class RecordingStats {
        public final int totalRequests;
        public final Map<String, Integer> modelCounts;
        public final Map<String, Long> avgResponseTimes;

        public RecordingStats(int totalRequests, Map<String, Integer> modelCounts, 
                             Map<String, Long> avgResponseTimes) {
            this.totalRequests = totalRequests;
            this.modelCounts = new HashMap<>(modelCounts);
            this.avgResponseTimes = new HashMap<>(avgResponseTimes);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Recording Stats:\n");
            sb.append("  Total Requests: ").append(totalRequests).append("\n");
            sb.append("  Models Used:\n");
            
            modelCounts.forEach((model, count) -> {
                long avgTime = avgResponseTimes.getOrDefault(model, 0L);
                sb.append("    ").append(model).append(": ")
                  .append(count).append(" requests, ")
                  .append(avgTime).append("ms avg\n");
            });
            
            return sb.toString();
        }
    }

    /**
     * Builder for creating preconfigured recorders.
     */
    public static class Builder {
        private LlmProvider provider;
        private Path directory;
        private String session;
        private boolean autoRecord = false;
        private boolean autoReplay = false;

        public Builder withProvider(LlmProvider provider) {
            this.provider = provider;
            return this;
        }

        public Builder withDirectory(Path directory) {
            this.directory = directory;
            return this;
        }

        public Builder withSession(String session) {
            this.session = session;
            return this;
        }

        public Builder autoRecord() {
            this.autoRecord = true;
            this.autoReplay = false;
            return this;
        }

        public Builder autoReplay() {
            this.autoReplay = true;
            this.autoRecord = false;
            return this;
        }

        public LlmResponseRecorder build() {
            if (provider == null) {
                throw new IllegalStateException("Provider is required");
            }

            LlmResponseRecorder recorder = directory != null 
                ? new LlmResponseRecorder(provider, directory)
                : new LlmResponseRecorder(provider);

            if (session != null) {
                if (autoRecord) {
                    recorder.startRecording(session);
                } else if (autoReplay) {
                    recorder.startReplay(session);
                }
            }

            return recorder;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}