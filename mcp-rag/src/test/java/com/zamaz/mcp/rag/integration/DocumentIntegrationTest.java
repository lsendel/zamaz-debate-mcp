package com.zamaz.mcp.rag.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.rag.adapter.web.dto.UploadDocumentResponse;
import com.zamaz.mcp.rag.domain.model.Document;
import com.zamaz.mcp.rag.domain.model.DocumentId;
import com.zamaz.mcp.rag.domain.model.OrganizationId;
import com.zamaz.mcp.rag.domain.port.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for document operations.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
public class DocumentIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private OrganizationId organizationId;
    
    @BeforeEach
    void setUp() {
        organizationId = OrganizationId.from("550e8400-e29b-41d4-a716-446655440000");
    }
    
    @Test
    void shouldUploadDocument() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "This is a test document for RAG processing.".getBytes()
        );
        
        // When & Then
        String responseContent = mockMvc.perform(multipart("/api/documents/upload")
                .file(file)
                .param("title", "Test Document")
                .param("metadata", "{\"source\":\"test\", \"category\":\"demo\"}")
                .header("X-Organization-Id", organizationId.toString()))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.documentId").exists())
                .andExpect(jsonPath("$.message").value("Document uploaded successfully"))
                .andExpect(jsonPath("$.status").value("pending"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        // Verify document was saved
        UploadDocumentResponse response = objectMapper.readValue(responseContent, UploadDocumentResponse.class);
        DocumentId documentId = DocumentId.from(response.documentId());
        
        Optional<Document> savedDocument = documentRepository.findById(documentId);
        assertThat(savedDocument).isPresent();
        assertThat(savedDocument.get().getMetadata().title()).isEqualTo("Test Document");
    }
    
    @Test
    void shouldGetDocumentById() throws Exception {
        // Given - create a document first
        DocumentId documentId = createTestDocument();
        
        // When & Then
        mockMvc.perform(get("/api/documents/{documentId}", documentId.toString())
                .header("X-Organization-Id", organizationId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(documentId.toString()))
                .andExpect(jsonPath("$.title").value("Integration Test Document"))
                .andExpect(jsonPath("$.status").value("uploaded"));
    }
    
    @Test
    void shouldListDocuments() throws Exception {
        // Given - create a test document
        createTestDocument();
        
        // When & Then
        mockMvc.perform(get("/api/documents")
                .header("X-Organization-Id", organizationId.toString())
                .param("limit", "10")
                .param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpected(jsonPath("$[0].title").value("Integration Test Document"));
    }
    
    @Test
    void shouldProcessDocument() throws Exception {
        // Given
        DocumentId documentId = createTestDocument();
        
        // When & Then
        mockMvc.perform(post("/api/documents/{documentId}/process", documentId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.documentId").value(documentId.toString()))
                .andExpect(jsonPath("$.status").value("processing"))
                .andExpect(jsonPath("$.message").value("Document processing started"));
    }
    
    @Test
    void shouldSearchDocuments() throws Exception {
        // Given
        String searchRequest = """
            {
                "query": "test document",
                "maxResults": 5,
                "minSimilarity": 0.0
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/documents/search")
                .header("X-Organization-Id", organizationId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(searchRequest))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.query").value("test document"))
                .andExpect(jsonPath("$.totalResults").isNumber())
                .andExpect(jsonPath("$.results").isArray());
    }
    
    @Test
    void shouldReturnNotFoundForNonExistentDocument() throws Exception {
        // Given
        String nonExistentId = "550e8400-e29b-41d4-a716-999999999999";
        
        // When & Then
        mockMvc.perform(get("/api/documents/{documentId}", nonExistentId)
                .header("X-Organization-Id", organizationId.toString()))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void shouldValidateUploadRequest() throws Exception {
        // Given - empty file
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file",
            "empty.txt",
            "text/plain",
            "".getBytes()
        );
        
        // When & Then
        mockMvc.perform(multipart("/api/documents/upload")
                .file(emptyFile)
                .param("title", "") // Empty title
                .header("X-Organization-Id", organizationId.toString()))
                .andExpect(status().isBadRequest());
    }
    
    /**
     * Helper method to create a test document.
     */
    private DocumentId createTestDocument() {
        // This would typically use the test container or embedded database
        // For now, return a mock document ID
        return DocumentId.generate();
    }
}