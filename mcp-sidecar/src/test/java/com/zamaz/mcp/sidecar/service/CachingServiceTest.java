package com.zamaz.mcp.sidecar.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveServerCommands;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CachingService
 */
@ExtendWith(MockitoExtension.class)
class CachingServiceTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    @Mock
    private ReactiveSetOperations<String, String> setOperations;

    @Mock
    private ReactiveRedisConnectionFactory connectionFactory;

    @Mock
    private ReactiveRedisConnection connection;

    @Mock
    private ReactiveServerCommands serverCommands;

    private CachingService cachingService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getReactiveConnection()).thenReturn(connection);
        when(connection.serverCommands()).thenReturn(serverCommands);
        
        cachingService = new CachingService(redisTemplate);
    }

    @Test
    void testCacheSetAndGet() {
        String key = "test-key";
        String value = "test-value";
        
        when(valueOperations.set(anyString(), anyString(), any(Duration.class)))
            .thenReturn(Mono.just(true));
        when(valueOperations.get(anyString()))
            .thenReturn(Mono.just(value));

        StepVerifier.create(
            cachingService.set(CachingService.CacheCategory.API_RESPONSE, key, value)
                .then(cachingService.get(CachingService.CacheCategory.API_RESPONSE, key))
        )
        .expectNext(value)
        .verifyComplete();
    }

    @Test
    void testCacheGetMiss() {
        String key = "non-existent-key";
        
        when(valueOperations.get(anyString()))
            .thenReturn(Mono.empty());

        StepVerifier.create(
            cachingService.get(CachingService.CacheCategory.API_RESPONSE, key)
        )
        .expectComplete()
        .verify();
    }

    @Test
    void testCacheDelete() {
        String key = "test-key";
        
        when(redisTemplate.delete(anyString()))
            .thenReturn(Mono.just(1L));

        StepVerifier.create(
            cachingService.delete(CachingService.CacheCategory.API_RESPONSE, key)
        )
        .expectComplete()
        .verify();
    }

    @Test
    void testCacheExists() {
        String key = "test-key";
        
        when(redisTemplate.hasKey(anyString()))
            .thenReturn(Mono.just(true));

        StepVerifier.create(
            cachingService.exists(CachingService.CacheCategory.API_RESPONSE, key)
        )
        .expectNext(true)
        .verifyComplete();
    }

    @Test
    void testCacheGetOrSetWithFallback() {
        String key = "test-key";
        String value = "fallback-value";
        
        when(valueOperations.get(anyString()))
            .thenReturn(Mono.empty()); // Cache miss
        when(valueOperations.set(anyString(), anyString(), any(Duration.class)))
            .thenReturn(Mono.just(true));

        Mono<String> fallback = Mono.just(value);

        StepVerifier.create(
            cachingService.getOrSet(CachingService.CacheCategory.API_RESPONSE, key, fallback)
        )
        .expectNext(value)
        .verifyComplete();
    }

    @Test
    void testCacheGetOrSetWithCacheHit() {
        String key = "test-key";
        String cachedValue = "cached-value";
        String fallbackValue = "fallback-value";
        
        when(valueOperations.get(anyString()))
            .thenReturn(Mono.just(cachedValue)); // Cache hit

        Mono<String> fallback = Mono.just(fallbackValue);

        StepVerifier.create(
            cachingService.getOrSet(CachingService.CacheCategory.API_RESPONSE, key, fallback)
        )
        .expectNext(cachedValue)
        .verifyComplete();
    }

    @Test
    void testCacheAIResponse() {
        String prompt = "Test prompt";
        String model = "gpt-4";
        Map<String, Object> parameters = Map.of("temperature", 0.7);
        String response = "Test response";
        
        when(valueOperations.set(anyString(), anyString(), any(Duration.class)))
            .thenReturn(Mono.just(true));

        StepVerifier.create(
            cachingService.cacheAIResponse(prompt, model, parameters, response)
        )
        .expectNextMatches(cacheKey -> cacheKey != null && !cacheKey.isEmpty())
        .verifyComplete();
    }

    @Test
    void testGetCachedAIResponse() {
        String prompt = "Test prompt";
        String model = "gpt-4";
        Map<String, Object> parameters = Map.of("temperature", 0.7);
        String response = "Test response";
        
        when(valueOperations.get(anyString()))
            .thenReturn(Mono.just(response));

        StepVerifier.create(
            cachingService.getCachedAIResponse(prompt, model, parameters)
        )
        .expectNext(response)
        .verifyComplete();
    }

    @Test
    void testCacheUserSession() {
        String userId = "user123";
        String sessionData = "session-data";
        
        when(valueOperations.set(anyString(), anyString(), any(Duration.class)))
            .thenReturn(Mono.just(true));

        StepVerifier.create(
            cachingService.cacheUserSession(userId, sessionData)
        )
        .expectComplete()
        .verify();
    }

    @Test
    void testGetCachedUserSession() {
        String userId = "user123";
        String sessionData = "session-data";
        
        when(valueOperations.get(anyString()))
            .thenReturn(Mono.just(sessionData));

        StepVerifier.create(
            cachingService.getCachedUserSession(userId)
        )
        .expectNext(sessionData)
        .verifyComplete();
    }

    @Test
    void testCacheOrganizationData() {
        String orgId = "org123";
        String orgData = "organization-data";
        
        when(valueOperations.set(anyString(), anyString(), any(Duration.class)))
            .thenReturn(Mono.just(true));

        StepVerifier.create(
            cachingService.cacheOrganizationData(orgId, orgData)
        )
        .expectComplete()
        .verify();
    }

    @Test
    void testCachePermissionData() {
        String userId = "user123";
        String orgId = "org123";
        String permissions = "read,write";
        
        when(valueOperations.set(anyString(), anyString(), any(Duration.class)))
            .thenReturn(Mono.just(true));

        StepVerifier.create(
            cachingService.cachePermissionData(userId, orgId, permissions)
        )
        .expectComplete()
        .verify();
    }

    @Test
    void testGetCachedPermissionData() {
        String userId = "user123";
        String orgId = "org123";
        String permissions = "read,write";
        
        when(valueOperations.get(anyString()))
            .thenReturn(Mono.just(permissions));

        StepVerifier.create(
            cachingService.getCachedPermissionData(userId, orgId)
        )
        .expectNext(permissions)
        .verifyComplete();
    }

    @Test
    void testInvalidateByPattern() {
        String pattern = "*";
        
        when(redisTemplate.keys(anyString()))
            .thenReturn(Flux.just("key1", "key2", "key3"));
        when(redisTemplate.delete(anyString()))
            .thenReturn(Mono.just(1L));

        StepVerifier.create(
            cachingService.invalidateByPattern(CachingService.CacheCategory.API_RESPONSE, pattern)
        )
        .expectComplete()
        .verify();
    }

    @Test
    void testClearCategory() {
        when(redisTemplate.keys(anyString()))
            .thenReturn(Flux.just("key1", "key2"));
        when(redisTemplate.delete(anyString()))
            .thenReturn(Mono.just(1L));

        StepVerifier.create(
            cachingService.clearCategory(CachingService.CacheCategory.API_RESPONSE)
        )
        .expectComplete()
        .verify();
    }

    @Test
    void testGetCacheStatistics() {
        Map<CachingService.CacheCategory, CachingService.CacheStats> stats = 
            cachingService.getCacheStatistics();
        
        assertThat(stats).isNotNull();
        // Stats will be empty initially in tests
    }

    @Test
    void testGetCacheInfo() {
        when(serverCommands.info("memory"))
            .thenReturn(Mono.just("used_memory:1000"));

        StepVerifier.create(
            cachingService.getCacheInfo()
        )
        .expectNextMatches(info -> {
            assertThat(info).containsKey("enabled");
            assertThat(info).containsKey("defaultTtl");
            assertThat(info).containsKey("categories");
            assertThat(info).containsKey("statistics");
            return true;
        })
        .verifyComplete();
    }

    @Test
    void testWarmUpCache() {
        StepVerifier.create(
            cachingService.warmUpCache()
        )
        .expectComplete()
        .verify();
    }

    @Test
    void testCacheDisabled() {
        // Test with caching disabled
        CachingService disabledCachingService = new CachingService(redisTemplate);
        
        StepVerifier.create(
            disabledCachingService.get(CachingService.CacheCategory.API_RESPONSE, "test-key")
        )
        .expectComplete()
        .verify();
        
        StepVerifier.create(
            disabledCachingService.set(CachingService.CacheCategory.API_RESPONSE, "test-key", "test-value")
        )
        .expectComplete()
        .verify();
    }

    @Test
    void testCacheErrorHandling() {
        String key = "test-key";
        
        when(valueOperations.get(anyString()))
            .thenReturn(Mono.error(new RuntimeException("Redis error")));

        StepVerifier.create(
            cachingService.get(CachingService.CacheCategory.API_RESPONSE, key)
        )
        .expectComplete()
        .verify();
    }
}