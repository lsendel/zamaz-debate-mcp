package com.zamaz.mcp.common.async;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.connection.stream.StringRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * Producer for Redis Stream messages.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisStreamMessageProducer {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisStreamConfig redisStreamConfig;

    /**
     * Send a document ingestion message.
     *
     * @param message the message
     * @return the record ID
     */
    public RecordId sendDocumentIngestionMessage(DocumentIngestionMessage message) {
        if (!redisStreamConfig.isEnabled()) {
            log.warn("Redis Streams is disabled, not sending document ingestion message");
            return null;
        }
        
        String streamName = redisStreamConfig.getStreamNames().getRagDocumentIngestion();
        String consumerGroup = redisStreamConfig.getConsumerGroupNames().getRagDocumentIngestion();
        
        // Set stream name and consumer group in the message
        message.setStreamName(streamName);
        message.setConsumerGroup(consumerGroup);
        
        try {
            String payload = objectMapper.writeValueAsString(message);
            
            // Create a record with the message as a field
            StringRecord record = StreamRecords.string(Collections.singletonMap("payload", payload))
                    .withStreamKey(streamName);
            
            // Send the message to the stream
            RecordId recordId = redisTemplate.opsForStream().add(record);
            
            log.debug("Sent document ingestion message to stream: {}, record ID: {}", streamName, recordId);
            
            // Create consumer group if it doesn't exist
            createConsumerGroupIfNotExists(streamName, consumerGroup);
            
            return recordId;
        } catch (JsonProcessingException e) {
            log.error("Error serializing document ingestion message", e);
            return null;
        }
    }

    /**
     * Create a consumer group if it doesn't exist.
     *
     * @param streamName the stream name
     * @param groupName the group name
     */
    private void createConsumerGroupIfNotExists(String streamName, String groupName) {
        try {
            // Check if the stream exists
            Boolean streamExists = redisTemplate.hasKey(streamName);
            
            if (Boolean.FALSE.equals(streamExists)) {
                // Create the stream with a dummy message that we'll delete
                StringRecord record = StreamRecords.string(Collections.singletonMap("init", "init"))
                        .withStreamKey(streamName);
                redisTemplate.opsForStream().add(record);
            }
            
            // Check if the consumer group exists
            boolean groupExists = false;
            try {
                redisTemplate.opsForStream().groups(streamName).forEach(group -> {
                    if (group.groupName().equals(groupName)) {
                        // Group exists
                    }
                });
            } catch (Exception e) {
                // Group doesn't exist or there was an error checking
            }
            
            if (!groupExists) {
                // Create the consumer group
                redisTemplate.opsForStream().createGroup(streamName, groupName);
                log.info("Created consumer group: {} for stream: {}", groupName, streamName);
            }
        } catch (Exception e) {
            log.error("Error creating consumer group: {} for stream: {}", groupName, streamName, e);
        }
    }
}
