package com.zamaz.mcp.rag.service.impl;

import com.zamaz.mcp.rag.dto.SearchRequest;
import com.zamaz.mcp.rag.dto.SearchResponse;
import com.zamaz.mcp.rag.dto.SearchResult;
import com.zamaz.mcp.rag.service.DocumentService;
import com.zamaz.mcp.rag.service.LlmIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementation of LlmIntegrationService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmIntegrationServiceImpl implements LlmIntegrationService {
    
    private final DocumentService documentService;
    
    @Override
    public String generateEnhancedPrompt(String organizationId, String originalPrompt, String ragContext) {
        StringBuilder enhancedPrompt = new StringBuilder();
        
        if (ragContext != null && !ragContext.isEmpty()) {
            enhancedPrompt.append("Use the following context to answer the question. ")
                          .append("If the answer cannot be found in the context, say so.\n\n");
            enhancedPrompt.append("Context:\n");
            enhancedPrompt.append(ragContext);
            enhancedPrompt.append("\n\n");
        }
        
        enhancedPrompt.append("Question: ").append(originalPrompt);
        
        return enhancedPrompt.toString();
    }
    
    @Override
    public String getContextForPrompt(String organizationId, String prompt, int maxTokens) {
        log.info("Getting RAG context for prompt in organization: {}", organizationId);
        
        try {
            SearchRequest request = SearchRequest.builder()
                    .organizationId(organizationId)
                    .query(prompt)
                    .limit(5)
                    .includeContent(true)
                    .build();
            
            SearchResponse response = documentService.searchDocuments(request);
            
            if (response.getResults().isEmpty()) {
                return "";
            }
            
            StringBuilder context = new StringBuilder();
            int currentTokens = 0;
            
            for (SearchResult result : response.getResults()) {
                String content = result.getContent();
                int estimatedTokens = content.length() / 4;
                
                if (currentTokens + estimatedTokens > maxTokens) {
                    break;
                }
                
                if (context.length() > 0) {
                    context.append("\n\n---\n\n");
                }
                
                context.append("[Source: ")
                       .append(result.getTitle() != null ? result.getTitle() : result.getDocumentId())
                       .append("]\n");
                context.append(content);
                
                currentTokens += estimatedTokens;
            }
            
            return context.toString();
            
        } catch (Exception e) {
            log.error("Error getting RAG context", e);
            return "";
        }
    }
}