package com.zamaz.mcp.rag.service;

import com.zamaz.mcp.rag.dto.SearchRequest;
import com.zamaz.mcp.rag.dto.SearchResponse;

/**
 * Service for integrating with the LLM service.
 */
public interface LlmIntegrationService {
    
    /**
     * Generate an enhanced prompt with RAG context.
     * 
     * @param organizationId The organization ID
     * @param originalPrompt The original user prompt
     * @param ragContext The context from RAG search
     * @return Enhanced prompt with context
     */
    String generateEnhancedPrompt(String organizationId, String originalPrompt, String ragContext);
    
    /**
     * Get relevant context for a prompt.
     * 
     * @param organizationId The organization ID
     * @param prompt The user prompt
     * @param maxTokens Maximum tokens for context
     * @return RAG context
     */
    String getContextForPrompt(String organizationId, String prompt, int maxTokens);
}