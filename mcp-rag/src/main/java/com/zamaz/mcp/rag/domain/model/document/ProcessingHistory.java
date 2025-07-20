package com.zamaz.mcp.rag.domain.model.document;

import java.time.Instant;
import java.util.*;

/**
 * Value Object representing the processing history of a document.
 * Immutable log of all processing steps and errors.
 */
public class ProcessingHistory {
    
    private final List<ProcessingStep> steps;
    
    private ProcessingHistory(List<ProcessingStep> steps) {
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
    }
    
    /**
     * Create an empty processing history
     */
    public static ProcessingHistory empty() {
        return new ProcessingHistory(new ArrayList<>());
    }
    
    /**
     * Add a processing step
     */
    public ProcessingHistory addStep(ProcessingStep step) {
        Objects.requireNonNull(step, "Processing step cannot be null");
        List<ProcessingStep> newSteps = new ArrayList<>(steps);
        newSteps.add(step);
        return new ProcessingHistory(newSteps);
    }
    
    /**
     * Add an error to the history
     */
    public ProcessingHistory addError(String error, Instant timestamp) {
        return addStep(ProcessingStep.error(error, timestamp));
    }
    
    /**
     * Get all steps
     */
    public List<ProcessingStep> getSteps() {
        return steps;
    }
    
    /**
     * Get steps of a specific type
     */
    public List<ProcessingStep> getStepsOfType(StepType type) {
        return steps.stream()
                .filter(step -> step.type() == type)
                .toList();
    }
    
    /**
     * Get the last step
     */
    public Optional<ProcessingStep> getLastStep() {
        return steps.isEmpty() ? Optional.empty() : Optional.of(steps.get(steps.size() - 1));
    }
    
    /**
     * Check if history contains errors
     */
    public boolean hasErrors() {
        return steps.stream().anyMatch(step -> step.type() == StepType.ERROR);
    }
    
    /**
     * Get total processing time
     */
    public Optional<Long> getTotalProcessingTimeMillis() {
        if (steps.size() < 2) {
            return Optional.empty();
        }
        
        Instant first = steps.get(0).timestamp();
        Instant last = steps.get(steps.size() - 1).timestamp();
        return Optional.of(last.toEpochMilli() - first.toEpochMilli());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessingHistory that = (ProcessingHistory) o;
        return Objects.equals(steps, that.steps);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(steps);
    }
}

/**
 * Record representing a single processing step
 */
public record ProcessingStep(
        StepType type,
        String description,
        Map<String, String> metadata,
        Instant timestamp
) {
    public ProcessingStep {
        Objects.requireNonNull(type, "Step type cannot be null");
        Objects.requireNonNull(description, "Description cannot be null");
        Objects.requireNonNull(metadata, "Metadata cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        metadata = Map.copyOf(metadata); // Ensure immutability
    }
    
    /**
     * Factory method for upload step
     */
    public static ProcessingStep upload(String fileName, long fileSize, Instant timestamp) {
        return new ProcessingStep(
                StepType.UPLOAD,
                "Document uploaded: " + fileName,
                Map.of("fileName", fileName, "fileSize", String.valueOf(fileSize)),
                timestamp
        );
    }
    
    /**
     * Factory method for chunking step
     */
    public static ProcessingStep chunking(int chunkCount, Instant timestamp) {
        return new ProcessingStep(
                StepType.CHUNKING,
                "Document split into " + chunkCount + " chunks",
                Map.of("chunkCount", String.valueOf(chunkCount)),
                timestamp
        );
    }
    
    /**
     * Factory method for embedding step
     */
    public static ProcessingStep embedding(int embeddedCount, Instant timestamp) {
        return new ProcessingStep(
                StepType.EMBEDDING,
                "Generated embeddings for " + embeddedCount + " chunks",
                Map.of("embeddedCount", String.valueOf(embeddedCount)),
                timestamp
        );
    }
    
    /**
     * Factory method for error step
     */
    public static ProcessingStep error(String error, Instant timestamp) {
        return new ProcessingStep(
                StepType.ERROR,
                "Error: " + error,
                Map.of("error", error),
                timestamp
        );
    }
}

/**
 * Enum for processing step types
 */
public enum StepType {
    UPLOAD,
    CHUNKING,
    EMBEDDING,
    INDEXING,
    COMPLETION,
    ERROR
}