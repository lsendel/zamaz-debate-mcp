package com.zamaz.mcp.rag.task;

import com.zamaz.mcp.rag.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task for processing pending documents.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessingTask {
    
    private final DocumentService documentService;
    
    @Scheduled(fixedDelay = 30000, initialDelay = 10000) // Run every 30 seconds
    public void processPendingDocuments() {
        try {
            log.debug("Starting pending document processing");
            documentService.processPendingDocuments();
        } catch (Exception e) {
            log.error("Error in document processing task", e);
        }
    }
}