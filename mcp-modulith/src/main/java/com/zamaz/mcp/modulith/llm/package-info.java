/**
 * LLM integration module.
 * Provides a unified interface for interacting with different LLM providers.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "LLM Gateway",
    allowedDependencies = {"shared"}
)
package com.zamaz.mcp.modulith.llm;