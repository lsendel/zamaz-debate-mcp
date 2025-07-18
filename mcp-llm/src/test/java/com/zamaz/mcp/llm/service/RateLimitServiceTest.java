package com.zamaz.mcp.llm.service;

import com.zamaz.mcp.llm.config.LlmProperties;
import com.zamaz.mcp.llm.exception.RateLimitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Rate Limit Service Tests")
class RateLimitServiceTest {

    @Mock
    private LlmProperties llmProperties;

    @Mock
    private LlmProperties.RateLimiting rateLimiting;

    @InjectMocks
    private RateLimitService rateLimitService;

    private Map<String, Integer> providerLimits;

    @BeforeEach
    void setUp() {
        // Setup provider limits
        providerLimits = new HashMap<>();
        providerLimits.put("claude", 50);
        providerLimits.put("openai", 100);
        providerLimits.put("gemini", 60);

        // Setup mock behavior
        when(llmProperties.getRateLimiting()).thenReturn(rateLimiting);
        when(rateLimiting.getProviderLimits()).thenReturn(providerLimits);
        when(rateLimiting.getDefaultRequestsPerMinute()).thenReturn(30);
    }

    @Nested
    @DisplayName("Rate Limiting Enabled Tests")
    class RateLimitingEnabledTests {

        @BeforeEach
        void enableRateLimiting() {
            when(rateLimiting.isEnabled()).thenReturn(true);
        }

        @Test
        @DisplayName("Should allow requests within rate limit")
        void shouldAllowRequestsWithinRateLimit() {
            // When & Then - Make requests within Claude's limit (50 per minute)
            for (int i = 0; i < 10; i++) {
                StepVerifier.create(rateLimitService.checkRateLimit("claude"))
                        .verifyComplete();
            }
        }

        @Test
        @DisplayName("Should reject requests exceeding rate limit")
        void shouldRejectRequestsExceedingRateLimit() {
            // Given - Make requests up to the limit
            for (int i = 0; i < 50; i++) {
                StepVerifier.create(rateLimitService.checkRateLimit("claude"))
                        .verifyComplete();
            }

            // When & Then - Next request should be rejected
            StepVerifier.create(rateLimitService.checkRateLimit("claude"))
                    .expectError(RateLimitException.class)
                    .verify();
        }

        @Test
        @DisplayName("Should use provider-specific limits")
        void shouldUseProviderSpecificLimits() {
            // Test Claude limit (50)
            for (int i = 0; i < 50; i++) {
                StepVerifier.create(rateLimitService.checkRateLimit("claude"))
                        .verifyComplete();
            }
            StepVerifier.create(rateLimitService.checkRateLimit("claude"))
                    .expectError(RateLimitException.class)
                    .verify();

            // Test OpenAI limit (100) - should still allow requests
            for (int i = 0; i < 10; i++) {
                StepVerifier.create(rateLimitService.checkRateLimit("openai"))
                        .verifyComplete();
            }
        }

        @Test
        @DisplayName("Should use default limit for unknown providers")
        void shouldUseDefaultLimitForUnknownProviders() {
            // Given - Unknown provider should use default limit (30)
            String unknownProvider = "unknown-provider";

            // When & Then - Should allow up to default limit
            for (int i = 0; i < 30; i++) {
                StepVerifier.create(rateLimitService.checkRateLimit(unknownProvider))
                        .verifyComplete();
            }

            // Next request should be rejected
            StepVerifier.create(rateLimitService.checkRateLimit(unknownProvider))
                    .expectError(RateLimitException.class)
                    .verify();
        }

        @Test
        @DisplayName("Should maintain separate buckets per provider")
        void shouldMaintainSeparateBucketsPerProvider() {
            // Given - Exhaust Claude's bucket
            for (int i = 0; i < 50; i++) {
                StepVerifier.create(rateLimitService.checkRateLimit("claude"))
                        .verifyComplete();
            }

            // When & Then - Claude should be limited but OpenAI should work
            StepVerifier.create(rateLimitService.checkRateLimit("claude"))
                    .expectError(RateLimitException.class)
                    .verify();

            StepVerifier.create(rateLimitService.checkRateLimit("openai"))
                    .verifyComplete();
        }

        @ParameterizedTest
        @ValueSource(strings = {"claude", "openai", "gemini", "custom-provider"})
        @DisplayName("Should handle different provider names")
        void shouldHandleDifferentProviderNames(String provider) {
            // When & Then
            StepVerifier.create(rateLimitService.checkRateLimit(provider))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should handle null provider name gracefully")
        void shouldHandleNullProviderNameGracefully() {
            // When & Then - Should use default bucket
            StepVerifier.create(rateLimitService.checkRateLimit(null))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should handle empty provider name")
        void shouldHandleEmptyProviderName() {
            // When & Then - Should use default bucket
            StepVerifier.create(rateLimitService.checkRateLimit(""))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should return proper error message")
        void shouldReturnProperErrorMessage() {
            // Given - Exhaust bucket
            for (int i = 0; i < 50; i++) {
                StepVerifier.create(rateLimitService.checkRateLimit("claude"))
                        .verifyComplete();
            }

            // When & Then - Check error message
            StepVerifier.create(rateLimitService.checkRateLimit("claude"))
                    .expectErrorMatches(throwable -> 
                        throwable instanceof RateLimitException &&
                        throwable.getMessage().equals("Rate limit exceeded for provider: claude"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("Rate Limiting Disabled Tests")
    class RateLimitingDisabledTests {

        @BeforeEach
        void disableRateLimiting() {
            when(rateLimiting.isEnabled()).thenReturn(false);
        }

        @Test
        @DisplayName("Should allow unlimited requests when disabled")
        void shouldAllowUnlimitedRequestsWhenDisabled() {
            // When & Then - Should allow many more requests than any limit
            for (int i = 0; i < 1000; i++) {
                StepVerifier.create(rateLimitService.checkRateLimit("claude"))
                        .verifyComplete();
            }
        }

        @Test
        @DisplayName("Should not create buckets when disabled")
        void shouldNotCreateBucketsWhenDisabled() {
            // When
            StepVerifier.create(rateLimitService.checkRateLimit("claude"))
                    .verifyComplete();

            // Then - Should not call provider limits configuration
            verify(rateLimiting, never()).getProviderLimits();
            verify(rateLimiting, never()).getDefaultRequestsPerMinute();
        }

        @Test
        @DisplayName("Should handle any provider when disabled")
        void shouldHandleAnyProviderWhenDisabled() {
            // Test various providers
            String[] providers = {"claude", "openai", "gemini", "unknown", null, ""};

            for (String provider : providers) {
                StepVerifier.create(rateLimitService.checkRateLimit(provider))
                        .verifyComplete();
            }
        }
    }

    @Nested
    @DisplayName("Configuration Edge Cases")
    class ConfigurationEdgeCases {

        @BeforeEach
        void enableRateLimiting() {
            when(rateLimiting.isEnabled()).thenReturn(true);
        }

        @Test
        @DisplayName("Should handle zero default limit")
        void shouldHandleZeroDefaultLimit() {
            // Given
            when(rateLimiting.getDefaultRequestsPerMinute()).thenReturn(0);

            // When & Then - Should immediately reject requests for unknown providers
            StepVerifier.create(rateLimitService.checkRateLimit("unknown"))
                    .expectError(RateLimitException.class)
                    .verify();
        }

        @Test
        @DisplayName("Should handle negative provider limit")
        void shouldHandleNegativeProviderLimit() {
            // Given
            providerLimits.put("negative", -10);
            when(rateLimiting.getProviderLimits()).thenReturn(providerLimits);

            // When & Then - Should handle gracefully (bucket4j behavior)
            // This test verifies that the service doesn't crash with invalid config
            assertThatCode(() -> {
                StepVerifier.create(rateLimitService.checkRateLimit("negative"))
                        .expectError() // Might be RateLimitException or IllegalArgumentException
                        .verify();
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle null provider limits map")
        void shouldHandleNullProviderLimitsMap() {
            // Given
            when(rateLimiting.getProviderLimits()).thenReturn(null);

            // When & Then - Should fall back to default limit
            StepVerifier.create(rateLimitService.checkRateLimit("claude"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should handle empty provider limits map")
        void shouldHandleEmptyProviderLimitsMap() {
            // Given
            when(rateLimiting.getProviderLimits()).thenReturn(new HashMap<>());

            // When & Then - Should use default limit for all providers
            for (int i = 0; i < 30; i++) {
                StepVerifier.create(rateLimitService.checkRateLimit("claude"))
                        .verifyComplete();
            }

            StepVerifier.create(rateLimitService.checkRateLimit("claude"))
                    .expectError(RateLimitException.class)
                    .verify();
        }

        @Test
        @DisplayName("Should handle very high limits")
        void shouldHandleVeryHighLimits() {
            // Given
            providerLimits.put("unlimited", Integer.MAX_VALUE);
            when(rateLimiting.getProviderLimits()).thenReturn(providerLimits);

            // When & Then - Should allow many requests
            for (int i = 0; i < 10000; i++) {
                StepVerifier.create(rateLimitService.checkRateLimit("unlimited"))
                        .verifyComplete();
            }
        }
    }

    @Nested
    @DisplayName("Concurrency Tests")
    class ConcurrencyTests {

        @BeforeEach
        void enableRateLimiting() {
            when(rateLimiting.isEnabled()).thenReturn(true);
        }

        @Test
        @DisplayName("Should handle concurrent requests to same provider")
        void shouldHandleConcurrentRequestsToSameProvider() {
            // Given - Multiple threads making requests concurrently
            int threadCount = 10;
            int requestsPerThread = 5;
            Thread[] threads = new Thread[threadCount];

            // When
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < requestsPerThread; j++) {
                        rateLimitService.checkRateLimit("claude").block();
                    }
                });
            }

            // Start all threads
            for (Thread thread : threads) {
                thread.start();
            }

            // Wait for completion
            assertThatCode(() -> {
                for (Thread thread : threads) {
                    thread.join(5000); // 5 second timeout
                }
            }).doesNotThrowAnyException();

            // Then - Total requests (50) should be within Claude's limit, so all should succeed
        }

        @Test
        @DisplayName("Should handle concurrent requests to different providers")
        void shouldHandleConcurrentRequestsToDifferentProviders() {
            // Given
            String[] providers = {"claude", "openai", "gemini"};
            Thread[] threads = new Thread[providers.length];

            // When - Each thread hammers a different provider
            for (int i = 0; i < providers.length; i++) {
                final String provider = providers[i];
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < 20; j++) {
                        rateLimitService.checkRateLimit(provider).block();
                    }
                });
            }

            // Start and wait for completion
            assertThatCode(() -> {
                for (Thread thread : threads) {
                    thread.start();
                }
                for (Thread thread : threads) {
                    thread.join(5000);
                }
            }).doesNotThrowAnyException();

            // Then - All should succeed as each provider has its own bucket
        }

        @Test
        @DisplayName("Should maintain thread safety with bucket creation")
        void shouldMaintainThreadSafetyWithBucketCreation() {
            // Given - Multiple threads accessing same new provider simultaneously
            String newProvider = "concurrent-test-provider";
            int threadCount = 50;
            Thread[] threads = new Thread[threadCount];

            // When
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    rateLimitService.checkRateLimit(newProvider).block();
                });
            }

            // Start all threads simultaneously
            assertThatCode(() -> {
                for (Thread thread : threads) {
                    thread.start();
                }
                for (Thread thread : threads) {
                    thread.join(5000);
                }
            }).doesNotThrowAnyException();

            // Then - Should not crash or create inconsistent state
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @BeforeEach
        void enableRateLimiting() {
            when(rateLimiting.isEnabled()).thenReturn(true);
        }

        @Test
        @DisplayName("Should process rate limit checks efficiently")
        void shouldProcessRateLimitChecksEfficiently() {
            // Given
            int requestCount = 1000;

            // When
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < requestCount; i++) {
                if (i < 30) { // Within default limit
                    StepVerifier.create(rateLimitService.checkRateLimit("performance-test"))
                            .verifyComplete();
                } else { // Will be rate limited
                    StepVerifier.create(rateLimitService.checkRateLimit("performance-test"))
                            .expectError(RateLimitException.class)
                            .verify();
                }
            }
            long totalTime = System.currentTimeMillis() - startTime;

            // Then - Should complete quickly
            assertThat(totalTime).isLessThan(5000); // Should complete in < 5 seconds
        }

        @Test
        @DisplayName("Should handle many providers efficiently")
        void shouldHandleManyProvidersEfficiently() {
            // Given
            int providerCount = 100;

            // When
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < providerCount; i++) {
                StepVerifier.create(rateLimitService.checkRateLimit("provider" + i))
                        .verifyComplete();
            }
            long totalTime = System.currentTimeMillis() - startTime;

            // Then
            assertThat(totalTime).isLessThan(2000); // Should complete in < 2 seconds
        }
    }

    @Nested
    @DisplayName("Reactive Stream Tests")
    class ReactiveStreamTests {

        @BeforeEach
        void enableRateLimiting() {
            when(rateLimiting.isEnabled()).thenReturn(true);
        }

        @Test
        @DisplayName("Should handle timeout correctly")
        void shouldHandleTimeoutCorrectly() {
            // When & Then
            StepVerifier.create(rateLimitService.checkRateLimit("claude"))
                    .expectComplete()
                    .verify(Duration.ofSeconds(1));
        }

        @Test
        @DisplayName("Should work with reactive chains")
        void shouldWorkWithReactiveChains() {
            // When & Then
            Mono<String> result = rateLimitService.checkRateLimit("claude")
                    .then(Mono.just("Success"));

            StepVerifier.create(result)
                    .expectNext("Success")
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should work with retry logic")
        void shouldWorkWithRetryLogic() {
            // Given - Exhaust bucket first
            for (int i = 0; i < 50; i++) {
                rateLimitService.checkRateLimit("claude").block();
            }

            // When & Then - Should fail even with retry
            Mono<Void> rateLimitedMono = rateLimitService.checkRateLimit("claude")
                    .retry(3);

            StepVerifier.create(rateLimitedMono)
                    .expectError(RateLimitException.class)
                    .verify();
        }

        @Test
        @DisplayName("Should integrate with error handling")
        void shouldIntegrateWithErrorHandling() {
            // Given - Exhaust bucket
            for (int i = 0; i < 50; i++) {
                rateLimitService.checkRateLimit("claude").block();
            }

            // When & Then
            Mono<String> result = rateLimitService.checkRateLimit("claude")
                    .then(Mono.just("Success"))
                    .onErrorReturn("Rate Limited");

            StepVerifier.create(result)
                    .expectNext("Rate Limited")
                    .verifyComplete();
        }
    }
}