package com.zamaz.mcp.common.domain.agentic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Value object representing a step in the processing of a prompt by an agentic
 * flow.
 */
public class ProcessingStep {
    private final String stepType;
    private final String input;
    private final String output;
    private final Map<String, Object> metadata;

    /**
     * Creates a new ProcessingStep with the specified parameters.
     *
     * @param stepType The type of the step
     * @param input    The input to the step
     * @param output   The output from the step
     * @param metadata Additional metadata about the step
     */
    public ProcessingStep(String stepType, String input, String output, Map<String, Object> metadata) {
        this.stepType = Objects.requireNonNull(stepType, "Step type cannot be null");
        this.input = Objects.requireNonNull(input, "Input cannot be null");
        this.output = Objects.requireNonNull(output, "Output cannot be null");
        this.metadata = new HashMap<>(Objects.requireNonNull(metadata, "Metadata cannot be null"));
    }

    /**
     * Creates a new ProcessingStep with the specified parameters and empty
     * metadata.
     *
     * @param stepType The type of the step
     * @param input    The input to the step
     * @param output   The output from the step
     */
    public ProcessingStep(String stepType, String input, String output) {
        this(stepType, input, output, new HashMap<>());
    }

    /**
     * Returns the type of this step.
     *
     * @return The step type
     */
    public String getStepType() {
        return stepType;
    }

    /**
     * Returns the input to this step.
     *
     * @return The input
     */
    public String getInput() {
        return input;
    }

    /**
     * Returns the output from this step.
     *
     * @return The output
     */
    public String getOutput() {
        return output;
    }

    /**
     * Returns the metadata about this step.
     *
     * @return An unmodifiable view of the metadata
     */
    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    /**
     * Creates a new ProcessingStep with the specified metadata added.
     *
     * @param key   The metadata key
     * @param value The metadata value
     * @return A new ProcessingStep with the metadata added
     */
    public ProcessingStep withMetadata(String key, Object value) {
        Map<String, Object> newMetadata = new HashMap<>(this.metadata);
        newMetadata.put(key, value);
        return new ProcessingStep(this.stepType, this.input, this.output, newMetadata);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ProcessingStep that = (ProcessingStep) o;
        return stepType.equals(that.stepType) &&
                input.equals(that.input) &&
                output.equals(that.output) &&
                metadata.equals(that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stepType, input, output, metadata);
    }

    @Override
    public String toString() {
        return "ProcessingStep{" +
                "stepType='" + stepType + '\'' +
                ", input='" + input + '\'' +
                ", output='" + output + '\'' +
                ", metadata=" + metadata +
                '}';
    }
}