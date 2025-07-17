package com.zamaz.mcp.common.async;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

/**
 * Listener for RAG document ingestion messages.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RagDocumentIngestionListener implements StreamListener<String, ObjectRecord<String, String>> {

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Handle a message from the RAG document ingestion stream.
     *
     * @param message the message
     */
    @Override
    public void onMessage(ObjectRecord<String, String> message) {
        try {
            String payload = message.getValue();
            DocumentIngestionMessage ingestionMessage = objectMapper.readValue(payload, DocumentIngestionMessage.class);
            
            log.info("Processing document ingestion message: {}", ingestionMessage.getDocumentId());
            
            // Process the document ingestion
            processDocumentIngestion(ingestionMessage);
            
            // Acknowledge the message
            redisTemplate.opsForStream().acknowledge(
                    ingestionMessage.getStreamName(),
                    ingestionMessage.getConsumerGroup(),
                    message.getId());
            
            log.info("Document ingestion completed for document: {}", ingestionMessage.getDocumentId());
        } catch (JsonProcessingException e) {
            log.error("Error deserializing document ingestion message", e);
        } catch (Exception e) {
            log.error("Error processing document ingestion message", e);
        }
    }

    /**
     * Process a document ingestion message.
     *
     * @param message the message
     */
    private void processDocumentIngestion(DocumentIngestionMessage message) {
        // This would be implemented in the RAG service
        // Here we're just defining the interface
        log.info("Would process document ingestion for document: {} in knowledge base: {}",
                message.getDocumentId(), message.getKnowledgeBaseId());
    }
}
