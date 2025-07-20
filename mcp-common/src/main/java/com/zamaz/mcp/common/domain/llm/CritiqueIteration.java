package com.zamaz.mcp.common.domain.llm;

import java.util.Objects;

/**
 * Value object representing a critique iteration in a self-critique loop.
 */
public class CritiqueIteration {
    private final String critique;
    private final String revisedResponse;

    /**
     * Creates a new CritiqueIteration with the specified parameters.
     *
     * @param critique        The critique of the previous response
     * @param revisedResponse The revised response based on the critique
     */
    public CritiqueIteration(String critique, String revisedResponse) {
        this.critique = Objects.requireNonNull(critique, "Critique cannot be null");
        this.revisedResponse = Objects.requireNonNull(revisedResponse, "Revised response cannot be null");
    }

    /**
     * Returns the critique of the previous response.
     *
     * @return The critique
     */
    public String getCritique() {
        return critique;
    }

    /**
     * Returns the revised response based on the critique.
     *
     * @return The revised response
     */
    public String getRevisedResponse() {
        return revisedResponse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CritiqueIteration that = (CritiqueIteration) o;
        return critique.equals(that.critique) &&
                revisedResponse.equals(that.revisedResponse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(critique, revisedResponse);
    }

    @Override
    public String toString() {
        return "CritiqueIteration{" +
                "critique='" + critique + '\'' +
                ", revisedResponse='" + revisedResponse + '\'' +
                '}';
    }
}