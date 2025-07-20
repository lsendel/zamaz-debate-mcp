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
    INTERNAL_MONOLOGUE,

    /**
     * Implements a Generate-Critique-Revise pattern for self-improvement.
     */
    SELF_CRITIQUE_LOOP,

    /**
     * Simulates internal debates between different perspectives within a single AI
     * agent.
     */
    MULTI_AGENT_RED_TEAM,

    /**
     * Enables the model to use external tools for fact verification.
     */
    TOOL_CALLING_VERIFICATION,

    /**
     * Implements enhanced RAG with document re-ranking.
     */
    RAG_WITH_RERANKING,

    /**
     * Provides confidence scores with responses and improves low-confidence
     * answers.
     */
    CONFIDENCE_SCORING,

    /**
     * Applies constitutional guardrails to AI agent responses.
     */
    CONSTITUTIONAL_PROMPTING,

    /**
     * Uses ensemble voting across multiple AI responses for more reliable answers.
     */
    ENSEMBLE_VOTING,

    /**
     * Applies deterministic checks to validate AI outputs.
     */
    POST_PROCESSING_RULES,

    /**
     * Explores multiple reasoning paths simultaneously like a decision tree.
     */
    TREE_OF_THOUGHTS,

    /**
     * Prompts the model to generalize from specific questions to underlying
     * principles.
     */
    STEP_BACK_PROMPTING,

    /**
     * Decomposes complex tasks into a sequence of smaller, interconnected prompts.
     */
    PROMPT_CHAINING
}