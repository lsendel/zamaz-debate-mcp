package com.zamaz.mcp.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration properties for RAG service.
 */
@Configuration
@ConfigurationProperties(prefix = "rag")
public class RagProperties {
    
    private Embedding embedding = new Embedding();
    private Chunking chunking = new Chunking();
    private Processing processing = new Processing();
    private Search search = new Search();
    
    // Getters and setters
    public Embedding getEmbedding() {
        return embedding;
    }
    
    public void setEmbedding(Embedding embedding) {
        this.embedding = embedding;
    }
    
    public Chunking getChunking() {
        return chunking;
    }
    
    public void setChunking(Chunking chunking) {
        this.chunking = chunking;
    }
    
    public Processing getProcessing() {
        return processing;
    }
    
    public void setProcessing(Processing processing) {
        this.processing = processing;
    }
    
    public Search getSearch() {
        return search;
    }
    
    public void setSearch(Search search) {
        this.search = search;
    }
    
    /**
     * Embedding configuration.
     */
    public static class Embedding {
        private String apiKey;
        private String apiUrl = "https://api.openai.com/v1/embeddings";
        private String model = "text-embedding-ada-002";
        private int batchSize = 100;
        
        // Getters and setters
        public String getApiKey() {
            return apiKey;
        }
        
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
        
        public String getApiUrl() {
            return apiUrl;
        }
        
        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }
        
        public String getModel() {
            return model;
        }
        
        public void setModel(String model) {
            this.model = model;
        }
        
        public int getBatchSize() {
            return batchSize;
        }
        
        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }
    
    /**
     * Chunking configuration.
     */
    public static class Chunking {
        private String strategy = "sliding-window";
        private int size = 512;
        private int overlap = 128;
        private int minSize = 50;
        
        // Getters and setters
        public String getStrategy() {
            return strategy;
        }
        
        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }
        
        public int getSize() {
            return size;
        }
        
        public void setSize(int size) {
            this.size = size;
        }
        
        public int getOverlap() {
            return overlap;
        }
        
        public void setOverlap(int overlap) {
            this.overlap = overlap;
        }
        
        public int getMinSize() {
            return minSize;
        }
        
        public void setMinSize(int minSize) {
            this.minSize = minSize;
        }
    }
    
    /**
     * Processing configuration.
     */
    public static class Processing {
        private long maxFileSize = 52428800; // 50MB
        private List<String> supportedTypes = List.of(
            "text/plain",
            "text/markdown",
            "text/csv",
            "application/json",
            "application/xml"
        );
        
        // Getters and setters
        public long getMaxFileSize() {
            return maxFileSize;
        }
        
        public void setMaxFileSize(long maxFileSize) {
            this.maxFileSize = maxFileSize;
        }
        
        public List<String> getSupportedTypes() {
            return supportedTypes;
        }
        
        public void setSupportedTypes(List<String> supportedTypes) {
            this.supportedTypes = supportedTypes;
        }
    }
    
    /**
     * Search configuration.
     */
    public static class Search {
        private int defaultLimit = 10;
        private int maxLimit = 100;
        private double minSimilarity = 0.0;
        
        // Getters and setters
        public int getDefaultLimit() {
            return defaultLimit;
        }
        
        public void setDefaultLimit(int defaultLimit) {
            this.defaultLimit = defaultLimit;
        }
        
        public int getMaxLimit() {
            return maxLimit;
        }
        
        public void setMaxLimit(int maxLimit) {
            this.maxLimit = maxLimit;
        }
        
        public double getMinSimilarity() {
            return minSimilarity;
        }
        
        public void setMinSimilarity(double minSimilarity) {
            this.minSimilarity = minSimilarity;
        }
    }
}