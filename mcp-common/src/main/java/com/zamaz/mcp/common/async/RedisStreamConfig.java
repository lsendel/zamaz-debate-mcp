package com.zamaz.mcp.common.async;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Configuration for Redis Streams.
 */
@Configuration
@ConfigurationProperties(prefix = "mcp.async.redis-streams")
@Data
public class RedisStreamConfig {

    /**
     * Whether to enable Redis Streams.
     */
    private boolean enabled = true;

    /**
     * Stream names.
     */
    private StreamNames streamNames = new StreamNames();

    /**
     * Consumer group names.
     */
    private ConsumerGroupNames consumerGroupNames = new ConsumerGroupNames();

    /**
     * Consumer names.
     */
    private ConsumerNames consumerNames = new ConsumerNames();

    /**
     * Polling configuration.
     */
    private Polling polling = new Polling();

    /**
     * Creates a Redis template for String keys and values.
     *
     * @param connectionFactory the Redis connection factory
     * @return the Redis template
     */
    @Bean
    public RedisTemplate<String, String> redisStreamTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Creates a stream message listener container.
     *
     * @param connectionFactory the Redis connection factory
     * @return the stream message listener container
     */
    @Bean
    public StreamMessageListenerContainer<String, ObjectRecord<String, String>> streamMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {
        
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        .pollTimeout(polling.getPollTimeout())
                        .targetType(String.class)
                        .executor(asyncTaskExecutor())
                        .build();
        
        return StreamMessageListenerContainer.create(connectionFactory, options);
    }

    /**
     * Creates an async task executor.
     *
     * @return the executor
     */
    @Bean
    public Executor asyncTaskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Creates a subscription for the RAG document ingestion stream.
     *
     * @param container the stream message listener container
     * @param ragDocumentIngestionListener the RAG document ingestion listener
     * @return the subscription
     */
    @Bean
    public Subscription ragDocumentIngestionSubscription(
            StreamMessageListenerContainer<String, ObjectRecord<String, String>> container,
            RagDocumentIngestionListener ragDocumentIngestionListener) {
        
        if (!enabled) {
            return null;
        }
        
        String streamName = streamNames.getRagDocumentIngestion();
        String groupName = consumerGroupNames.getRagDocumentIngestion();
        String consumerName = consumerNames.getRagDocumentIngestion();
        
        return container.receive(
                Consumer.from(groupName, consumerName),
                StreamOffset.create(streamName, ReadOffset.lastConsumed()),
                ragDocumentIngestionListener);
    }

    /**
     * Stream names.
     */
    @Data
    public static class StreamNames {
        private String ragDocumentIngestion = "rag:document:ingestion";
        private String debateSummarization = "debate:summarization";
    }

    /**
     * Consumer group names.
     */
    @Data
    public static class ConsumerGroupNames {
        private String ragDocumentIngestion = "rag-document-ingestion-group";
        private String debateSummarization = "debate-summarization-group";
    }

    /**
     * Consumer names.
     */
    @Data
    public static class ConsumerNames {
        private String ragDocumentIngestion = "rag-document-ingestion-consumer";
        private String debateSummarization = "debate-summarization-consumer";
    }

    /**
     * Polling configuration.
     */
    @Data
    public static class Polling {
        private Duration pollTimeout = Duration.ofMillis(100);
    }
}
