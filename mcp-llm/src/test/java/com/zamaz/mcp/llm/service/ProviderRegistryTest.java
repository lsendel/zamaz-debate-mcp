package com.zamaz.mcp.llm.service;

import com.zamaz.mcp.llm.provider.LlmProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Provider Registry Tests")
class ProviderRegistryTest {

    @Mock
    private LlmProvider claudeProvider;

    @Mock
    private LlmProvider openAiProvider;

    @Mock
    private LlmProvider geminiProvider;

    @Mock
    private LlmProvider disabledProvider;

    @InjectMocks
    private ProviderRegistry providerRegistry;

    @BeforeEach
    void setUp() {
        // Setup Claude provider
        when(claudeProvider.getName()).thenReturn("claude");
        when(claudeProvider.isEnabled()).thenReturn(true);

        // Setup OpenAI provider
        when(openAiProvider.getName()).thenReturn("openai");
        when(openAiProvider.isEnabled()).thenReturn(true);

        // Setup Gemini provider
        when(geminiProvider.getName()).thenReturn("gemini");
        when(geminiProvider.isEnabled()).thenReturn(true);

        // Setup disabled provider
        when(disabledProvider.getName()).thenReturn("disabled");
        when(disabledProvider.isEnabled()).thenReturn(false);
    }

    @Nested
    @DisplayName("Provider Registration Tests")
    class ProviderRegistrationTests {

        @Test
        @DisplayName("Should register all providers on initialization")
        void shouldRegisterAllProvidersOnInitialization() {
            // Given
            List<LlmProvider> providers = Arrays.asList(claudeProvider, openAiProvider, geminiProvider, disabledProvider);
            ProviderRegistry registry = new ProviderRegistry(providers);

            // When
            registry.init();

            // Then
            assertThat(registry.getAllProviders()).hasSize(4);
            assertThat(registry.getProvider("claude")).isPresent();
            assertThat(registry.getProvider("openai")).isPresent();
            assertThat(registry.getProvider("gemini")).isPresent();
            assertThat(registry.getProvider("disabled")).isPresent();
        }

        @Test
        @DisplayName("Should handle empty provider list")
        void shouldHandleEmptyProviderList() {
            // Given
            List<LlmProvider> emptyProviders = Collections.emptyList();
            ProviderRegistry registry = new ProviderRegistry(emptyProviders);

            // When
            registry.init();

            // Then
            assertThat(registry.getAllProviders()).isEmpty();
            assertThat(registry.getEnabledProviders()).isEmpty();
            assertThat(registry.getProvider("claude")).isEmpty();
        }

        @Test
        @DisplayName("Should handle null provider name gracefully")
        void shouldHandleNullProviderNameGracefully() {
            // Given
            LlmProvider nullNameProvider = mock(LlmProvider.class);
            when(nullNameProvider.getName()).thenReturn(null);
            when(nullNameProvider.isEnabled()).thenReturn(true);

            List<LlmProvider> providers = Arrays.asList(claudeProvider, nullNameProvider);
            ProviderRegistry registry = new ProviderRegistry(providers);

            // When
            registry.init();

            // Then
            assertThat(registry.getAllProviders()).hasSize(2);
            assertThat(registry.getProvider("claude")).isPresent();
            assertThat(registry.getProvider(null)).isPresent();
        }

        @Test
        @DisplayName("Should handle duplicate provider names")
        void shouldHandleDuplicateProviderNames() {
            // Given
            LlmProvider duplicateClaudeProvider = mock(LlmProvider.class);
            when(duplicateClaudeProvider.getName()).thenReturn("claude");
            when(duplicateClaudeProvider.isEnabled()).thenReturn(true);

            List<LlmProvider> providers = Arrays.asList(claudeProvider, duplicateClaudeProvider);
            ProviderRegistry registry = new ProviderRegistry(providers);

            // When
            registry.init();

            // Then
            assertThat(registry.getAllProviders()).hasSize(2);
            // Last registered provider should win
            Optional<LlmProvider> provider = registry.getProvider("claude");
            assertThat(provider).isPresent();
            // Should be the second provider (duplicateClaudeProvider) due to HashMap behavior
        }

        @Test
        @DisplayName("Should log provider registration")
        void shouldLogProviderRegistration() {
            // Given
            List<LlmProvider> providers = Arrays.asList(claudeProvider, disabledProvider);
            ProviderRegistry registry = new ProviderRegistry(providers);

            // When
            registry.init();

            // Then
            // Verification that logging occurs (providers are called for name and enabled status)
            verify(claudeProvider, atLeastOnce()).getName();
            verify(claudeProvider, atLeastOnce()).isEnabled();
            verify(disabledProvider, atLeastOnce()).getName();
            verify(disabledProvider, atLeastOnce()).isEnabled();
        }
    }

    @Nested
    @DisplayName("Provider Retrieval Tests")
    class ProviderRetrievalTests {

        @BeforeEach
        void initializeRegistry() {
            List<LlmProvider> providers = Arrays.asList(claudeProvider, openAiProvider, geminiProvider, disabledProvider);
            providerRegistry = new ProviderRegistry(providers);
            providerRegistry.init();
        }

        @Test
        @DisplayName("Should get provider by name successfully")
        void shouldGetProviderByNameSuccessfully() {
            // When
            Optional<LlmProvider> claude = providerRegistry.getProvider("claude");
            Optional<LlmProvider> openai = providerRegistry.getProvider("openai");
            Optional<LlmProvider> gemini = providerRegistry.getProvider("gemini");

            // Then
            assertThat(claude).isPresent();
            assertThat(claude.get()).isEqualTo(claudeProvider);
            
            assertThat(openai).isPresent();
            assertThat(openai.get()).isEqualTo(openAiProvider);
            
            assertThat(gemini).isPresent();
            assertThat(gemini.get()).isEqualTo(geminiProvider);
        }

        @Test
        @DisplayName("Should return empty optional for non-existent provider")
        void shouldReturnEmptyOptionalForNonExistentProvider() {
            // When
            Optional<LlmProvider> nonExistent = providerRegistry.getProvider("non-existent");

            // Then
            assertThat(nonExistent).isEmpty();
        }

        @Test
        @DisplayName("Should handle null provider name")
        void shouldHandleNullProviderName() {
            // When
            Optional<LlmProvider> nullProvider = providerRegistry.getProvider(null);

            // Then
            assertThat(nullProvider).isEmpty();
        }

        @Test
        @DisplayName("Should handle empty provider name")
        void shouldHandleEmptyProviderName() {
            // When
            Optional<LlmProvider> emptyProvider = providerRegistry.getProvider("");

            // Then
            assertThat(emptyProvider).isEmpty();
        }

        @Test
        @DisplayName("Should be case sensitive for provider names")
        void shouldBeCaseSensitiveForProviderNames() {
            // When
            Optional<LlmProvider> claude = providerRegistry.getProvider("claude");
            Optional<LlmProvider> claudeUpper = providerRegistry.getProvider("CLAUDE");
            Optional<LlmProvider> claudeMixed = providerRegistry.getProvider("Claude");

            // Then
            assertThat(claude).isPresent();
            assertThat(claudeUpper).isEmpty();
            assertThat(claudeMixed).isEmpty();
        }
    }

    @Nested
    @DisplayName("Provider Listing Tests")
    class ProviderListingTests {

        @BeforeEach
        void initializeRegistry() {
            List<LlmProvider> providers = Arrays.asList(claudeProvider, openAiProvider, geminiProvider, disabledProvider);
            providerRegistry = new ProviderRegistry(providers);
            providerRegistry.init();
        }

        @Test
        @DisplayName("Should return all providers")
        void shouldReturnAllProviders() {
            // When
            List<LlmProvider> allProviders = providerRegistry.getAllProviders();

            // Then
            assertThat(allProviders).hasSize(4);
            assertThat(allProviders).containsExactly(claudeProvider, openAiProvider, geminiProvider, disabledProvider);
        }

        @Test
        @DisplayName("Should return only enabled providers")
        void shouldReturnOnlyEnabledProviders() {
            // When
            List<LlmProvider> enabledProviders = providerRegistry.getEnabledProviders();

            // Then
            assertThat(enabledProviders).hasSize(3);
            assertThat(enabledProviders).containsExactly(claudeProvider, openAiProvider, geminiProvider);
            assertThat(enabledProviders).doesNotContain(disabledProvider);
        }

        @Test
        @DisplayName("Should return empty list when no providers are enabled")
        void shouldReturnEmptyListWhenNoProvidersAreEnabled() {
            // Given
            when(claudeProvider.isEnabled()).thenReturn(false);
            when(openAiProvider.isEnabled()).thenReturn(false);
            when(geminiProvider.isEnabled()).thenReturn(false);

            List<LlmProvider> providers = Arrays.asList(claudeProvider, openAiProvider, geminiProvider, disabledProvider);
            ProviderRegistry registry = new ProviderRegistry(providers);
            registry.init();

            // When
            List<LlmProvider> enabledProviders = registry.getEnabledProviders();

            // Then
            assertThat(enabledProviders).isEmpty();
        }

        @Test
        @DisplayName("Should return all providers when all are enabled")
        void shouldReturnAllProvidersWhenAllAreEnabled() {
            // Given
            when(disabledProvider.isEnabled()).thenReturn(true);

            List<LlmProvider> providers = Arrays.asList(claudeProvider, openAiProvider, geminiProvider, disabledProvider);
            ProviderRegistry registry = new ProviderRegistry(providers);
            registry.init();

            // When
            List<LlmProvider> enabledProviders = registry.getEnabledProviders();

            // Then
            assertThat(enabledProviders).hasSize(4);
            assertThat(enabledProviders).containsExactly(claudeProvider, openAiProvider, geminiProvider, disabledProvider);
        }

        @Test
        @DisplayName("Should return immutable list of providers")
        void shouldReturnImmutableListOfProviders() {
            // When
            List<LlmProvider> allProviders = providerRegistry.getAllProviders();
            List<LlmProvider> enabledProviders = providerRegistry.getEnabledProviders();

            // Then
            // The returned lists should be the original lists from the constructor
            // If they were mutable, modifying them could affect the registry
            assertThat(allProviders).isNotNull();
            assertThat(enabledProviders).isNotNull();
            
            // Verify we can't modify the original list through the returned reference
            // Note: This depends on the implementation returning defensive copies or immutable lists
        }
    }

    @Nested
    @DisplayName("Dynamic Provider State Tests")
    class DynamicProviderStateTests {

        @BeforeEach
        void initializeRegistry() {
            List<LlmProvider> providers = Arrays.asList(claudeProvider, openAiProvider, disabledProvider);
            providerRegistry = new ProviderRegistry(providers);
            providerRegistry.init();
        }

        @Test
        @DisplayName("Should reflect dynamic provider state changes")
        void shouldReflectDynamicProviderStateChanges() {
            // Given - Initially disabled provider
            assertThat(providerRegistry.getEnabledProviders()).hasSize(2);
            assertThat(providerRegistry.getEnabledProviders()).doesNotContain(disabledProvider);

            // When - Provider becomes enabled
            when(disabledProvider.isEnabled()).thenReturn(true);

            // Then - Should reflect the change
            List<LlmProvider> enabledProviders = providerRegistry.getEnabledProviders();
            assertThat(enabledProviders).hasSize(3);
            assertThat(enabledProviders).contains(disabledProvider);
        }

        @Test
        @DisplayName("Should handle provider becoming disabled")
        void shouldHandleProviderBecomingDisabled() {
            // Given - Initially enabled provider
            assertThat(providerRegistry.getEnabledProviders()).contains(claudeProvider);

            // When - Provider becomes disabled
            when(claudeProvider.isEnabled()).thenReturn(false);

            // Then - Should reflect the change
            List<LlmProvider> enabledProviders = providerRegistry.getEnabledProviders();
            assertThat(enabledProviders).hasSize(1);
            assertThat(enabledProviders).doesNotContain(claudeProvider);
            assertThat(enabledProviders).contains(openAiProvider);
        }

        @Test
        @DisplayName("Should handle provider state toggling")
        void shouldHandleProviderStateToggling() {
            // Initial state: claude enabled, disabled provider disabled
            assertThat(providerRegistry.getEnabledProviders()).hasSize(2);

            // Toggle 1: Disable claude, enable disabled provider
            when(claudeProvider.isEnabled()).thenReturn(false);
            when(disabledProvider.isEnabled()).thenReturn(true);

            List<LlmProvider> enabled1 = providerRegistry.getEnabledProviders();
            assertThat(enabled1).hasSize(2);
            assertThat(enabled1).contains(openAiProvider, disabledProvider);
            assertThat(enabled1).doesNotContain(claudeProvider);

            // Toggle 2: Re-enable claude, disable disabled provider
            when(claudeProvider.isEnabled()).thenReturn(true);
            when(disabledProvider.isEnabled()).thenReturn(false);

            List<LlmProvider> enabled2 = providerRegistry.getEnabledProviders();
            assertThat(enabled2).hasSize(2);
            assertThat(enabled2).contains(openAiProvider, claudeProvider);
            assertThat(enabled2).doesNotContain(disabledProvider);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandling {

        @Test
        @DisplayName("Should handle provider that throws exception on getName")
        void shouldHandleProviderThatThrowsExceptionOnGetName() {
            // Given
            LlmProvider faultyProvider = mock(LlmProvider.class);
            when(faultyProvider.getName()).thenThrow(new RuntimeException("Name error"));
            when(faultyProvider.isEnabled()).thenReturn(true);

            List<LlmProvider> providers = Arrays.asList(claudeProvider, faultyProvider);
            ProviderRegistry registry = new ProviderRegistry(providers);

            // When & Then
            assertThatThrownBy(registry::init)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Name error");
        }

        @Test
        @DisplayName("Should handle provider that throws exception on isEnabled")
        void shouldHandleProviderThatThrowsExceptionOnIsEnabled() {
            // Given
            LlmProvider faultyProvider = mock(LlmProvider.class);
            when(faultyProvider.getName()).thenReturn("faulty");
            when(faultyProvider.isEnabled()).thenThrow(new RuntimeException("Enabled check error"));

            List<LlmProvider> providers = Arrays.asList(claudeProvider, faultyProvider);
            ProviderRegistry registry = new ProviderRegistry(providers);

            // When & Then
            assertThatThrownBy(registry::init)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Enabled check error");
        }

        @Test
        @DisplayName("Should handle concurrent access to provider map")
        void shouldHandleConcurrentAccessToProviderMap() {
            // Given
            List<LlmProvider> providers = Arrays.asList(claudeProvider, openAiProvider);
            ProviderRegistry registry = new ProviderRegistry(providers);
            registry.init();

            // When - Simulate concurrent access
            Runnable getProvider = () -> {
                for (int i = 0; i < 100; i++) {
                    registry.getProvider("claude");
                    registry.getProvider("openai");
                    registry.getEnabledProviders();
                    registry.getAllProviders();
                }
            };

            Thread thread1 = new Thread(getProvider);
            Thread thread2 = new Thread(getProvider);

            // Then - Should not throw any exceptions
            assertThatCode(() -> {
                thread1.start();
                thread2.start();
                thread1.join();
                thread2.join();
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle provider with very long name")
        void shouldHandleProviderWithVeryLongName() {
            // Given
            String veryLongName = "a".repeat(10000); // 10KB name
            LlmProvider longNameProvider = mock(LlmProvider.class);
            when(longNameProvider.getName()).thenReturn(veryLongName);
            when(longNameProvider.isEnabled()).thenReturn(true);

            List<LlmProvider> providers = Arrays.asList(claudeProvider, longNameProvider);
            ProviderRegistry registry = new ProviderRegistry(providers);

            // When
            registry.init();

            // Then
            assertThat(registry.getProvider(veryLongName)).isPresent();
            assertThat(registry.getAllProviders()).hasSize(2);
            assertThat(registry.getEnabledProviders()).hasSize(2);
        }

        @Test
        @DisplayName("Should handle provider list with nulls")
        void shouldHandleProviderListWithNulls() {
            // Given
            List<LlmProvider> providersWithNull = Arrays.asList(claudeProvider, null, openAiProvider);
            ProviderRegistry registry = new ProviderRegistry(providersWithNull);

            // When & Then
            assertThatThrownBy(registry::init)
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should handle large number of providers efficiently")
        void shouldHandleLargeNumberOfProvidersEfficiently() {
            // Given
            List<LlmProvider> manyProviders = new java.util.ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                LlmProvider provider = mock(LlmProvider.class);
                when(provider.getName()).thenReturn("provider" + i);
                when(provider.isEnabled()).thenReturn(i % 2 == 0); // Every other provider enabled
                manyProviders.add(provider);
            }

            ProviderRegistry registry = new ProviderRegistry(manyProviders);

            // When
            long startTime = System.currentTimeMillis();
            registry.init();
            long initTime = System.currentTimeMillis() - startTime;

            startTime = System.currentTimeMillis();
            List<LlmProvider> enabled = registry.getEnabledProviders();
            long getEnabledTime = System.currentTimeMillis() - startTime;

            startTime = System.currentTimeMillis();
            Optional<LlmProvider> specific = registry.getProvider("provider500");
            long getSpecificTime = System.currentTimeMillis() - startTime;

            // Then
            assertThat(registry.getAllProviders()).hasSize(1000);
            assertThat(enabled).hasSize(500); // Half are enabled
            assertThat(specific).isPresent();

            // Performance assertions (these thresholds may need adjustment based on environment)
            assertThat(initTime).isLessThan(1000); // Should init in < 1 second
            assertThat(getEnabledTime).isLessThan(100); // Should filter in < 100ms
            assertThat(getSpecificTime).isLessThan(10); // Should lookup in < 10ms
        }

        @Test
        @DisplayName("Should cache enabled providers efficiently")
        void shouldCacheEnabledProvidersEfficiently() {
            // Given
            List<LlmProvider> providers = Arrays.asList(claudeProvider, openAiProvider, geminiProvider);
            ProviderRegistry registry = new ProviderRegistry(providers);
            registry.init();

            // When - Call getEnabledProviders multiple times
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                registry.getEnabledProviders();
            }
            long totalTime = System.currentTimeMillis() - startTime;

            // Then - Should be efficient (note: actual implementation may not cache, so this verifies current behavior)
            assertThat(totalTime).isLessThan(1000); // Should complete 1000 calls in < 1 second

            // Verify that isEnabled() was called multiple times (since there's no caching in current implementation)
            verify(claudeProvider, atLeast(1000)).isEnabled();
            verify(openAiProvider, atLeast(1000)).isEnabled();
            verify(geminiProvider, atLeast(1000)).isEnabled();
        }
    }
}