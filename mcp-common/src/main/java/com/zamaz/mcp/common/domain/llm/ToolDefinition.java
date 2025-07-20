package com.zamaz.mcp.common.domain.llm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Value object representing a tool that can be used by an LLM.
 */
public class ToolDefinition {
    private final String name;
    private final String description;
    private final Map<String, Object> schema;

    /**
     * Creates a new ToolDefinition with the specified parameters.
     *
     * @param name        The tool name
     * @param description The tool description
     * @param schema      The tool parameter schema
     */
    public ToolDefinition(String name, String description, Map<String, Object> schema) {
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.description = Objects.requireNonNull(description, "Description cannot be null");
        this.schema = new HashMap<>(Objects.requireNonNull(schema, "Schema cannot be null"));
    }

    /**
     * Returns the name of this tool.
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the description of this tool.
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the parameter schema of this tool.
     *
     * @return An unmodifiable view of the schema
     */
    public Map<String, Object> getSchema() {
        return Collections.unmodifiableMap(schema);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ToolDefinition that = (ToolDefinition) o;
        return name.equals(that.name) &&
                description.equals(that.description) &&
                schema.equals(that.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, schema);
    }

    @Override
    public String toString() {
        return "ToolDefinition{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", schema=" + schema +
                '}';
    }
}