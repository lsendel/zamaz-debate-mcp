package com.zamaz.mcp.rag.application.port.in;

import com.zamaz.mcp.rag.domain.model.document.DocumentId;

/**
 * Inbound port for processing documents (chunking and embedding).
 * This interface defines the contract for document processing use case.
 */
public interface ProcessDocumentUseCase {
    
    /**
     * Process a document by chunking it and generating embeddings
     * 
     * @param command The command containing processing details
     */
    void processDocument(ProcessDocumentCommand command);
    
    /**
     * Command object for document processing
     */
    record ProcessDocumentCommand(
        DocumentId documentId,
        ChunkingConfiguration chunkingConfig,
        EmbeddingConfiguration embeddingConfig
    ) {
        public ProcessDocumentCommand {
            if (documentId == null) {
                throw new IllegalArgumentException("Document ID cannot be null");
            }
            if (chunkingConfig == null) {
                chunkingConfig = ChunkingConfiguration.defaults();
            }
            if (embeddingConfig == null) {
                embeddingConfig = EmbeddingConfiguration.defaults();
            }
        }
    }
    
    /**
     * Configuration for chunking
     */
    record ChunkingConfiguration(
        String strategy,
        int maxChunkSize,
        int overlapSize,
        boolean preserveSentences
    ) {
        public static ChunkingConfiguration defaults() {
            return new ChunkingConfiguration("sliding_window", 512, 128, true);
        }
        
        public static ChunkingConfiguration forParagraphs() {
            return new ChunkingConfiguration("paragraph", 1000, 0, true);
        }
        
        public static ChunkingConfiguration forSemantic() {
            return new ChunkingConfiguration("semantic", 800, 200, true);
        }
    }
    
    /**
     * Configuration for embedding generation
     */
    record EmbeddingConfiguration(
        String model,
        int batchSize,
        boolean useCache
    ) {
        public static EmbeddingConfiguration defaults() {
            return new EmbeddingConfiguration("text-embedding-ada-002", 100, true);
        }
        
        public static EmbeddingConfiguration forLocalModel(String modelName) {
            return new EmbeddingConfiguration(modelName, 50, false);
        }
    }
}