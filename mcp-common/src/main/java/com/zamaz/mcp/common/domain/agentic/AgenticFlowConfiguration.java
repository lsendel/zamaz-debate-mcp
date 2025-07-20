package com.zamaz.mcp.common.domain.agentic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Value object representing the configuration for an agentic flow.
 * Contains parameters that control the behavior of the flow.
 */
public class AgenticFlowConfiguration {
    private final Map<String, Object> parameters;

    /**
     * Creates a new AgenticFlowConfiguration with the specified parameters.
     *
     * @param parameters The configuration parameters
     */
    public AgenticFlowConfiguration(Map<String, Object> parameters) {
        this.parameters = new HashMap<>(Objects.requireNonNull(parameters, "Parameters cannot be null"));
    }

    /**
     * Creates a new empty AgenticFlowConfiguration.
     */
    public AgenticFlowConfiguration() {
        this(new HashMap<>());
    }

    /**
     * Returns the configuration parameters.
     *
     * @return An unmodifiable view of the parameters
     */
    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    /**
     * Returns the value of the specified parameter.
     *
     * @param key The parameter key
     * @return The parameter value, or null if not found
     */
    public Object getParameter(String key) {
        return parameters.get(key);
    }

    /**
     * Returns the value of the specified parameter, or the default value if not
     * found.
     *
     * @param key          The parameter key
     * @param defaultValue The default value to return if the parameter is not found
     * @return The parameter value, or the default value if not found
     */
    public Object getParameter(String key, Object defaultValue) {
        return parameters.getOrDefault(key, defaultValue);
    }

    /**
     * Creates a new AgenticFlowConfiguration with the specified parameter added.
     *
     * @param key   The parameter key
     * @param value The parameter value
     * @return A new AgenticFlowConfiguration with the parameter added
     */
    public AgenticFlowConfiguration withParameter(String key, Object value) {
        Map<String, Object> newParameters = new HashMap<>(this.parameters);
        newParameters.put(key, value);
        return new AgenticFlowConfiguration(newParameters);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AgenticFlowConfiguration that = (AgenticFlowConfiguration) o;
        return parameters.equals(that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameters);
    }

    @Override
    public String toString() {
        return "AgenticFlowConfiguration{" +
                "parameters=" + parameters +
                '}';
    }
}