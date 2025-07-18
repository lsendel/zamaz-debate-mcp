package com.zamaz.mcp.rag.unit;

import com.zamaz.mcp.rag.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Document domain model.
 */
public class DocumentTest {
    
    @Test
    void shouldCreateDocumentSuccessfully() {
        // Given
        DocumentId id = DocumentId.generate();
        OrganizationId orgId = OrganizationId.from("550e8400-e29b-41d4-a716-446655440000");
        DocumentContent content = DocumentContent.of("Test content", "text/plain");
        DocumentMetadata metadata = DocumentMetadata.of("Test Document", Map.of("source", "test"));
        FileInfo fileInfo = FileInfo.of("test.txt", "text/plain", 100L);
        
        // When
        Document document = Document.create(id, orgId, content, metadata, fileInfo);
        
        // Then
        assertThat(document.getId()).isEqualTo(id);
        assertThat(document.getOrganizationId()).isEqualTo(orgId);
        assertThat(document.getContent()).isEqualTo(content);
        assertThat(document.getMetadata()).isEqualTo(metadata);
        assertThat(document.getFileInfo()).isEqualTo(fileInfo);
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(document.getChunkCount()).isEqualTo(0);
        assertThat(document.getCreatedAt()).isNotNull();
        assertThat(document.getUpdatedAt()).isNotNull();
    }
    
    @Test
    void shouldAddChunkToDocument() {
        // Given
        Document document = createTestDocument();
        ChunkId chunkId = ChunkId.generate();
        ChunkContent chunkContent = ChunkContent.of("Test chunk content", 0, 18);
        
        // When
        DocumentChunk chunk = document.addChunk(
            DocumentChunk.create(chunkId, document.getId(), "Test Document", chunkContent, 1)
        );
        
        // Then
        assertThat(document.getChunkCount()).isEqualTo(1);
        assertThat(chunk.getDocumentId()).isEqualTo(document.getId());
        assertThat(chunk.getChunkNumber()).isEqualTo(1);
    }
    
    @Test
    void shouldUpdateDocumentStatus() {
        // Given
        Document document = createTestDocument();
        
        // When
        document.updateStatus(DocumentStatus.PROCESSING);
        
        // Then
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.PROCESSING);
    }
    
    @Test
    void shouldFailWhenAddingDuplicateChunkNumber() {
        // Given
        Document document = createTestDocument();
        ChunkContent content = ChunkContent.of("Test content", 0, 12);
        
        document.addChunk(DocumentChunk.create(
            ChunkId.generate(), document.getId(), "Test", content, 1
        ));
        
        // When & Then
        assertThatThrownBy(() -> 
            document.addChunk(DocumentChunk.create(
                ChunkId.generate(), document.getId(), "Test", content, 1
            ))
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Chunk number 1 already exists");
    }
    
    @Test
    void shouldValidateDocumentContent() {
        // Given
        DocumentId id = DocumentId.generate();
        OrganizationId orgId = OrganizationId.from("550e8400-e29b-41d4-a716-446655440000");
        DocumentMetadata metadata = DocumentMetadata.of("Test", Map.of());
        FileInfo fileInfo = FileInfo.of("test.txt", "text/plain", 100L);
        
        // When & Then
        assertThatThrownBy(() -> 
            Document.create(id, orgId, null, metadata, fileInfo)
        ).isInstanceOf(NullPointerException.class);
    }
    
    @Test
    void shouldValidateFileInfo() {
        // When & Then
        assertThatThrownBy(() -> 
            FileInfo.of("", "text/plain", 100L)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("File name cannot be empty");
        
        assertThatThrownBy(() -> 
            FileInfo.of("test.txt", "text/plain", -1L)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("File size cannot be negative");
    }
    
    @Test
    void shouldValidateDocumentMetadata() {
        // When & Then
        assertThatThrownBy(() -> 
            DocumentMetadata.of("", Map.of())
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Title cannot be empty");
        
        assertThatThrownBy(() -> 
            DocumentMetadata.of(null, Map.of())
        ).isInstanceOf(NullPointerException.class);
    }
    
    @Test
    void shouldValidateChunkContent() {
        // When & Then
        assertThatThrownBy(() -> 
            ChunkContent.of("", 0, 0)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Content cannot be empty");
        
        assertThatThrownBy(() -> 
            ChunkContent.of("test", -1, 4)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Start offset cannot be negative");
        
        assertThatThrownBy(() -> 
            ChunkContent.of("test", 10, 5)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("End offset must be greater than start offset");
    }
    
    @Test
    void shouldCalculateEmbeddingSimilarity() {
        // Given
        Embedding embedding1 = new Embedding(List.of(1.0, 0.0, 0.0));
        Embedding embedding2 = new Embedding(List.of(0.0, 1.0, 0.0));
        Embedding embedding3 = new Embedding(List.of(1.0, 0.0, 0.0));
        
        // When
        double similarity1 = embedding1.cosineSimilarity(embedding2);
        double similarity2 = embedding1.cosineSimilarity(embedding3);
        
        // Then
        assertThat(similarity1).isEqualTo(0.0); // Orthogonal vectors
        assertThat(similarity2).isEqualTo(1.0); // Identical vectors
    }
    
    private Document createTestDocument() {
        DocumentId id = DocumentId.generate();
        OrganizationId orgId = OrganizationId.from("550e8400-e29b-41d4-a716-446655440000");
        DocumentContent content = DocumentContent.of("Test content", "text/plain");
        DocumentMetadata metadata = DocumentMetadata.of("Test Document", Map.of());
        FileInfo fileInfo = FileInfo.of("test.txt", "text/plain", 100L);
        
        return Document.create(id, orgId, content, metadata, fileInfo);
    }
}