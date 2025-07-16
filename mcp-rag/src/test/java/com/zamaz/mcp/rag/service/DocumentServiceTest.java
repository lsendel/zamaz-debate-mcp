package com.zamaz.mcp.rag.service;

import com.zamaz.mcp.rag.entity.Document;
import com.zamaz.mcp.rag.repository.DocumentRepository;
import com.zamaz.mcp.rag.service.impl.DocumentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private DocumentServiceImpl documentService;

    private Document testDocument;

    @BeforeEach
    void setUp() {
        testDocument = new Document();
        testDocument.setId("test-doc-id");
        testDocument.setOrganizationId("test-org-id");
        testDocument.setTitle("Test Document");
        testDocument.setContent("Test content");
        testDocument.setContentType("text/plain");
    }

    @Test
    void testFindById_WhenDocumentExists() {
        when(documentRepository.findById(anyString())).thenReturn(Optional.of(testDocument));

        Optional<Document> result = documentService.findById("test-doc-id");

        assertTrue(result.isPresent());
        assertEquals("Test Document", result.get().getTitle());
        verify(documentRepository).findById("test-doc-id");
    }

    @Test
    void testFindById_WhenDocumentNotExists() {
        when(documentRepository.findById(anyString())).thenReturn(Optional.empty());

        Optional<Document> result = documentService.findById("non-existent-id");

        assertFalse(result.isPresent());
        verify(documentRepository).findById("non-existent-id");
    }

    @Test
    void testFindByOrganizationId() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Document> page = new PageImpl<>(List.of(testDocument));
        
        when(documentRepository.findByOrganizationId(anyString(), any(Pageable.class)))
                .thenReturn(page);

        Page<Document> result = documentService.findByOrganizationId("test-org-id", pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Test Document", result.getContent().get(0).getTitle());
        verify(documentRepository).findByOrganizationId("test-org-id", pageable);
    }

    @Test
    void testSave() {
        when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

        Document result = documentService.save(testDocument);

        assertNotNull(result);
        assertEquals("Test Document", result.getTitle());
        verify(documentRepository).save(testDocument);
    }

    @Test
    void testDeleteById() {
        doNothing().when(documentRepository).deleteById(anyString());

        documentService.deleteById("test-doc-id");

        verify(documentRepository).deleteById("test-doc-id");
    }
}