package com.zamaz.mcp.common.async;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message for document ingestion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentIngestionMessage {

    /**
     * The document ID.
     */
    private String documentId;

    /**
     * The knowledge base ID.
     */
    private String knowledgeBaseId;

    /**
     * The organization ID.
     */
    private String organizationId;

    /**
     * The stream name.
     */
    private String streamName;

    /**
     * The consumer group.
     */
    private String consumerGroup;

    /**
     * The document content type.
     */
    private String contentType;

    /**
     * The document file path.
     */
    private String filePath;

    /**
     * The document metadata.
     */
    private String metadata;

    /**
     * The processing options.
     */
    private ProcessingOptions processingOptions;

    /**
     * Document processing options.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessingOptions {
        
        /**
         * The chunk size.
         */
        private Integer chunkSize;
        
        /**
         * The chunk overlap.
         */
        private Integer chunkOverlap;
        
        /**
         * Whether to skip sections.
         */
        private String[] skipSections;
        
        /**
         * The embedding model.
         */
        private String embeddingModel;
    }
}
