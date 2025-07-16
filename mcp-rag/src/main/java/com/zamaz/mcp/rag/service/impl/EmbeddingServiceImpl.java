package com.zamaz.mcp.rag.service.impl;

import com.theokanning.openai.embedding.Embedding;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.service.OpenAiService;
import com.zamaz.mcp.common.exception.TechnicalException;
import com.zamaz.mcp.rag.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of EmbeddingService using OpenAI.
 */
@Slf4j
@Service
public class EmbeddingServiceImpl implements EmbeddingService {
    
    private final OpenAiService openAiService;
    private final String embeddingModel;
    private final int embeddingDimension;
    
    public EmbeddingServiceImpl(@Value("${openai.api.key:}") String apiKey,
                               @Value("${openai.embedding.model:text-embedding-ada-002}") String embeddingModel,
                               @Value("${openai.embedding.dimension:1536}") int embeddingDimension) {
        this.embeddingModel = embeddingModel;
        this.embeddingDimension = embeddingDimension;
        
        if (apiKey != null && !apiKey.isEmpty() && !apiKey.equals("your-api-key-here")) {
            this.openAiService = new OpenAiService(apiKey, Duration.ofSeconds(30));
            log.info("OpenAI embedding service initialized with model: {}", embeddingModel);
        } else {
            this.openAiService = null;
            log.warn("OpenAI API key not configured. Using mock embeddings.");
        }
    }
    
    @Override
    public List<Float> generateEmbedding(String text) {
        if (openAiService == null) {
            return generateMockEmbedding(text);
        }
        
        try {
            EmbeddingRequest request = EmbeddingRequest.builder()
                    .model(embeddingModel)
                    .input(List.of(text))
                    .build();
            
            List<Embedding> embeddings = openAiService.createEmbeddings(request).getData();
            
            if (embeddings.isEmpty()) {
                throw new TechnicalException("No embedding returned from OpenAI", "EMBEDDING_GENERATION_FAILED");
            }
            
            return embeddings.get(0).getEmbedding().stream()
                    .map(Double::floatValue)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Error generating embedding: {}", e.getMessage());
            throw new TechnicalException("Failed to generate embedding: " + e.getMessage(), "EMBEDDING_GENERATION_FAILED");
        }
    }
    
    @Override
    public List<List<Float>> generateEmbeddings(List<String> texts) {
        if (openAiService == null) {
            return texts.stream()
                    .map(this::generateMockEmbedding)
                    .collect(Collectors.toList());
        }
        
        try {
            // OpenAI supports batch embedding
            EmbeddingRequest request = EmbeddingRequest.builder()
                    .model(embeddingModel)
                    .input(texts)
                    .build();
            
            List<Embedding> embeddings = openAiService.createEmbeddings(request).getData();
            
            return embeddings.stream()
                    .map(embedding -> embedding.getEmbedding().stream()
                            .map(Double::floatValue)
                            .collect(Collectors.toList()))
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Error generating embeddings: {}", e.getMessage());
            throw new TechnicalException("Failed to generate embeddings: " + e.getMessage(), "EMBEDDING_GENERATION_FAILED");
        }
    }
    
    @Override
    public int getEmbeddingDimension() {
        return embeddingDimension;
    }
    
    /**
     * Generate a mock embedding for testing when OpenAI is not configured.
     */
    private List<Float> generateMockEmbedding(String text) {
        List<Float> embedding = new ArrayList<>(embeddingDimension);
        
        // Generate deterministic mock embedding based on text hash
        int hash = text.hashCode();
        for (int i = 0; i < embeddingDimension; i++) {
            // Create pseudo-random values based on hash and position
            float value = (float) Math.sin(hash * (i + 1) * 0.01) * 0.5f;
            embedding.add(value);
        }
        
        return embedding;
    }
}