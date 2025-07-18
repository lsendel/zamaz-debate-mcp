package com.zamaz.mcp.context.integration;

import com.zamaz.mcp.context.dto.ContextDto;
import com.zamaz.mcp.context.entity.Context;
import com.zamaz.mcp.context.entity.Message;
import com.zamaz.mcp.context.repository.ContextRepository;
import com.zamaz.mcp.context.service.ContextService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Context Service Integration Tests with Testcontainers")
class ContextServiceTestcontainersIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private ContextService contextService;

    @Autowired
    private ContextRepository contextRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String TENANT_ID = "test-tenant";
    private static final String USER_ID = "test-user";

    @BeforeEach
    void setUp() {
        // Clear Redis cache
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        // Clear database
        contextRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("Should create context and persist to PostgreSQL")
    @Transactional
    void shouldCreateContextAndPersistToPostgreSQL() {
        // Given
        ContextDto.CreateContextRequest request = ContextDto.CreateContextRequest.builder()
                .name("Integration Test Context")
                .description("Testing with real PostgreSQL")
                .maxTokens(4096)
                .tenantId(TENANT_ID)
                .userId(USER_ID)
                .metadata(Map.of("test", "true"))
                .build();

        // When
        ContextDto created = contextService.createContext(request);

        // Then
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Integration Test Context");

        // Verify in database
        Optional<Context> saved = contextRepository.findById(UUID.fromString(created.getId()));
        assertThat(saved).isPresent();
        assertThat(saved.get().getName()).isEqualTo("Integration Test Context");
        assertThat(saved.get().getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    @Order(2)
    @DisplayName("Should cache context in Redis after retrieval")
    void shouldCacheContextInRedisAfterRetrieval() {
        // Given - create context
        Context context = createTestContext();
        String contextId = context.getId().toString();

        // When - first retrieval (from database)
        long startTime = System.currentTimeMillis();
        ContextDto firstRetrieval = contextService.getContext(context.getId());
        long dbTime = System.currentTimeMillis() - startTime;

        // Then - verify it's now in cache
        String cacheKey = "context:" + TENANT_ID + ":" + contextId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        assertThat(cached).isNotNull();

        // When - second retrieval (from cache)
        startTime = System.currentTimeMillis();
        ContextDto secondRetrieval = contextService.getContext(context.getId());
        long cacheTime = System.currentTimeMillis() - startTime;

        // Then - cache should be faster
        assertThat(secondRetrieval).isEqualTo(firstRetrieval);
        assertThat(cacheTime).isLessThan(dbTime);
    }

    @Test
    @Order(3)
    @DisplayName("Should handle message appending with proper ordering")
    @Transactional
    void shouldHandleMessageAppendingWithProperOrdering() {
        // Given
        Context context = createTestContext();
        
        // When - append multiple messages
        List<ContextDto.AppendMessageRequest> messages = List.of(
                createMessageRequest("user", "First message"),
                createMessageRequest("assistant", "First response"),
                createMessageRequest("user", "Second message"),
                createMessageRequest("assistant", "Second response")
        );

        messages.forEach(msg -> contextService.appendMessage(context.getId(), msg));

        // Then - verify order
        ContextDto retrieved = contextService.getContext(context.getId());
        assertThat(retrieved.getMessages()).hasSize(4);
        
        List<ContextDto.MessageDto> returnedMessages = retrieved.getMessages();
        assertThat(returnedMessages.get(0).getContent()).isEqualTo("First message");
        assertThat(returnedMessages.get(1).getContent()).isEqualTo("First response");
        assertThat(returnedMessages.get(2).getContent()).isEqualTo("Second message");
        assertThat(returnedMessages.get(3).getContent()).isEqualTo("Second response");
    }

    @Test
    @Order(4)
    @DisplayName("Should invalidate cache when context is updated")
    void shouldInvalidateCacheWhenContextIsUpdated() {
        // Given
        Context context = createTestContext();
        String contextId = context.getId().toString();
        String cacheKey = "context:" + TENANT_ID + ":" + contextId;

        // Cache the context
        contextService.getContext(context.getId());
        assertThat(redisTemplate.opsForValue().get(cacheKey)).isNotNull();

        // When - update context
        ContextDto.UpdateContextRequest updateRequest = ContextDto.UpdateContextRequest.builder()
                .name("Updated Context Name")
                .description("Updated description")
                .build();
        
        contextService.updateContext(context.getId(), updateRequest);

        // Then - cache should be invalidated
        assertThat(redisTemplate.opsForValue().get(cacheKey)).isNull();

        // And updated data should be returned
        ContextDto updated = contextService.getContext(context.getId());
        assertThat(updated.getName()).isEqualTo("Updated Context Name");
    }

    @Test
    @Order(5)
    @DisplayName("Should handle concurrent message appending safely")
    void shouldHandleConcurrentMessageAppendingSafely() throws Exception {
        // Given
        Context context = createTestContext();
        int threadCount = 10;
        int messagesPerThread = 5;
        
        // When - append messages concurrently
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            futures.add(executor.submit(() -> {
                for (int j = 0; j < messagesPerThread; j++) {
                    ContextDto.AppendMessageRequest message = createMessageRequest(
                            "user",
                            String.format("Thread %d - Message %d", threadId, j)
                    );
                    contextService.appendMessage(context.getId(), message);
                }
            }));
        }

        // Wait for completion
        for (Future<?> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // Then - all messages should be saved
        ContextDto retrieved = contextService.getContext(context.getId());
        assertThat(retrieved.getMessages()).hasSize(threadCount * messagesPerThread);
        
        // Verify no messages were lost
        Set<String> messageContents = retrieved.getMessages().stream()
                .map(ContextDto.MessageDto::getContent)
                .collect(Collectors.toSet());
        assertThat(messageContents).hasSize(threadCount * messagesPerThread);
    }

    @Test
    @Order(6)
    @DisplayName("Should handle large contexts efficiently")
    @Transactional
    void shouldHandleLargeContextsEfficiently() {
        // Given - create context with many messages
        Context context = createTestContext();
        int messageCount = 1000;
        
        // When - add many messages
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < messageCount; i++) {
            ContextDto.AppendMessageRequest message = createMessageRequest(
                    i % 2 == 0 ? "user" : "assistant",
                    "Message " + i + " with some content to make it realistic in size"
            );
            contextService.appendMessage(context.getId(), message);
        }
        long appendTime = System.currentTimeMillis() - startTime;

        // Then - should complete in reasonable time
        assertThat(appendTime).isLessThan(30000); // 30 seconds for 1000 messages

        // Verify retrieval is still performant
        startTime = System.currentTimeMillis();
        ContextDto retrieved = contextService.getContext(context.getId());
        long retrievalTime = System.currentTimeMillis() - startTime;

        assertThat(retrieved.getMessages()).hasSize(messageCount);
        assertThat(retrievalTime).isLessThan(1000); // Should retrieve in < 1 second
    }

    @Test
    @Order(7)
    @DisplayName("Should list contexts with pagination from database")
    void shouldListContextsWithPaginationFromDatabase() {
        // Given - create multiple contexts
        int totalContexts = 25;
        for (int i = 0; i < totalContexts; i++) {
            createTestContext("Context " + i);
        }

        // When - get first page
        List<ContextDto> firstPage = contextService.listContexts(TENANT_ID, 0, 10);

        // Then
        assertThat(firstPage).hasSize(10);
        assertThat(firstPage.get(0).getName()).startsWith("Context");

        // When - get second page
        List<ContextDto> secondPage = contextService.listContexts(TENANT_ID, 10, 10);

        // Then
        assertThat(secondPage).hasSize(10);
        assertThat(secondPage).doesNotContainAnyElementsOf(firstPage);

        // When - get last page
        List<ContextDto> lastPage = contextService.listContexts(TENANT_ID, 20, 10);

        // Then
        assertThat(lastPage).hasSize(5);
    }

    @Test
    @Order(8)
    @DisplayName("Should handle Redis connection failure gracefully")
    void shouldHandleRedisConnectionFailureGracefully() {
        // Given
        Context context = createTestContext();
        
        // When - stop Redis container to simulate failure
        redis.stop();

        // Then - service should still work (fallback to database)
        assertThatCode(() -> {
            ContextDto retrieved = contextService.getContext(context.getId());
            assertThat(retrieved).isNotNull();
        }).doesNotThrowAnyException();

        // Restart Redis for other tests
        redis.start();
    }

    @Test
    @Order(9)
    @DisplayName("Should maintain data consistency between cache and database")
    @Transactional
    void shouldMaintainDataConsistencyBetweenCacheAndDatabase() {
        // Given
        Context context = createTestContext();
        
        // Append some messages
        for (int i = 0; i < 5; i++) {
            contextService.appendMessage(context.getId(), 
                    createMessageRequest("user", "Message " + i));
        }

        // Cache the context
        ContextDto cached = contextService.getContext(context.getId());

        // When - directly modify database (simulating external change)
        Context dbContext = contextRepository.findById(context.getId()).orElseThrow();
        Message newMessage = new Message();
        newMessage.setRole("system");
        newMessage.setContent("Directly added message");
        newMessage.setTokens(10);
        newMessage.setContext(dbContext);
        dbContext.getMessages().add(newMessage);
        contextRepository.save(dbContext);

        // Clear cache to force reload
        String cacheKey = "context:" + TENANT_ID + ":" + context.getId();
        redisTemplate.delete(cacheKey);

        // Then - should get updated data
        ContextDto updated = contextService.getContext(context.getId());
        assertThat(updated.getMessages()).hasSize(6);
        assertThat(updated.getMessages().get(5).getContent()).isEqualTo("Directly added message");
    }

    @Test
    @Order(10)
    @DisplayName("Should delete context and remove from cache")
    void shouldDeleteContextAndRemoveFromCache() {
        // Given
        Context context = createTestContext();
        String cacheKey = "context:" + TENANT_ID + ":" + context.getId();
        
        // Cache the context
        contextService.getContext(context.getId());
        assertThat(redisTemplate.opsForValue().get(cacheKey)).isNotNull();

        // When
        contextService.deleteContext(context.getId());

        // Then - should be removed from database
        assertThat(contextRepository.findById(context.getId())).isEmpty();
        
        // And removed from cache
        assertThat(redisTemplate.opsForValue().get(cacheKey)).isNull();

        // And throw error when trying to retrieve
        assertThatThrownBy(() -> contextService.getContext(context.getId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    // Helper methods
    private Context createTestContext() {
        return createTestContext("Test Context");
    }

    private Context createTestContext(String name) {
        Context context = new Context();
        context.setName(name);
        context.setDescription("Test context for integration tests");
        context.setTenantId(TENANT_ID);
        context.setUserId(USER_ID);
        context.setMaxTokens(4096);
        context.setCreatedAt(LocalDateTime.now());
        context.setMessages(new ArrayList<>());
        return contextRepository.save(context);
    }

    private ContextDto.AppendMessageRequest createMessageRequest(String role, String content) {
        return ContextDto.AppendMessageRequest.builder()
                .role(role)
                .content(content)
                .tokens(content.split(" ").length * 2) // Simple token estimation
                .metadata(Map.of("timestamp", System.currentTimeMillis()))
                .build();
    }
}