package com.zamaz.mcp.common.domain.agentic;

/**
 * Enumeration of supported agentic flow types.
 * Each type represents a different reasoning or decision-making pattern for AI
 * agents.
 */
public enum AgenticFlowType {
    /**
     * Uses step-by-step reasoning through internal monologue.
     */
    INTERNAL_MONOLOGUE("Internal Monologue"),

    /**
     * Implements a Generate-Critique-Revise pattern for self-improvement.
     */
    SELF_CRITIQUE_LOOP("Self-Critique Loop"),

    /**
     * Simulates internal debates between different perspectives within a single AI
     * agent.
     */
    MULTI_AGENT_RED_TEAM("Multi-Agent Red Team"),

    /**
     * Enables the model to use external tools for fact verification.
     */
    TOOL_CALLING_VERIFICATION("Tool-Calling Verification"),

    /**
     * Implements enhanced RAG with document re-ranking.
     */
    RAG_WITH_RERANKING("RAG with Re-ranking"),

    /**
     * Provides confidence scores with responses and improves low-confidence
     * answers.
     */
    CONFIDENCE_SCORING("Confidence Scoring"),

    /**
     * Applies constitutional guardrails to AI agent responses.
     */
    CONSTITUTIONAL_PROMPTING("Constitutional Prompting"),

    /**
     * Uses ensemble voting across multiple AI responses for more reliable answers.
     */
    ENSEMBLE_VOTING("Ensemble Voting"),

    /**
     * Applies deterministic checks to validate AI outputs.
     */
    POST_PROCESSING_RULES("Post-processing Rules"),

    /**
     * Explores multiple reasoning paths simultaneously like a decision tree.
     */
    TREE_OF_THOUGHTS("Tree of Thoughts"),

    /**
     * Prompts the model to generalize from specific questions to underlying
     * principles.
     */
    STEP_BACK_PROMPTING("Step-Back Prompting"),

    /**
     * Decomposes complex tasks into a sequence of smaller, interconnected prompts.
     */
    PROMPT_CHAINING("Prompt Chaining");

    private final String displayName;

    AgenticFlowType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human-readable display name for this flow type.
     *
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }
}