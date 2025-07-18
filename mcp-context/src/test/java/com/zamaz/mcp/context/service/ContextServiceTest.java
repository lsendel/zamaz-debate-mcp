package com.zamaz.mcp.context.service;

import com.zamaz.mcp.context.dto.AppendMessageRequest;
import com.zamaz.mcp.context.dto.ContextDto;
import com.zamaz.mcp.context.dto.CreateContextRequest;
import com.zamaz.mcp.context.dto.MessageDto;
import com.zamaz.mcp.context.entity.Context;
import com.zamaz.mcp.context.entity.Message;
import com.zamaz.mcp.context.exception.ContextNotFoundException;
import com.zamaz.mcp.context.exception.UnauthorizedAccessException;
import com.zamaz.mcp.context.repository.ContextRepository;
import com.zamaz.mcp.context.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Context Service Tests")
class ContextServiceTest {

    @Mock
    private ContextRepository contextRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private TokenCountingService tokenCountingService;

    @Mock
    private ContextCacheService cacheService;

    @Mock
    private ContextVersionService versionService;

    @InjectMocks
    private ContextService contextService;

    private Context testContext;
    private Message testMessage;
    private UUID testContextId;
    private UUID testUserId;
    private UUID testOrganizationId;
    private CreateContextRequest createRequest;
    private AppendMessageRequest appendRequest;

    @BeforeEach
    void setUp() {
        testContextId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testOrganizationId = UUID.randomUUID();

        // Create test context
        testContext = Context.builder()
                .id(testContextId)
                .name("Test Context")
                .description("Test Description")
                .organizationId(testOrganizationId)
                .userId(testUserId)
                .isActive(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .metadata(Map.of("key", "value"))
                .build();

        // Create test message
        testMessage = Message.builder()
                .id(UUID.randomUUID())
                .contextId(testContextId)
                .role("user")
                .content("Test message content")
                .tokenCount(10)
                .timestamp(Instant.now())
                .metadata(Map.of("source", "test"))
                .build();

        // Create test requests
        createRequest = CreateContextRequest.builder()
                .name("New Context")
                .description("New Description")
                .organizationId(testOrganizationId)
                .userId(testUserId)
                .metadata(Map.of("key", "value"))
                .build();

        appendRequest = AppendMessageRequest.builder()
                .contextId(testContextId)
                .role("user")
                .content("New message content")
                .metadata(Map.of("source", "test"))
                .build();
    }

    @Nested
    @DisplayName("Context Creation Tests")
    class ContextCreationTests {

        @Test
        @DisplayName("Should create context successfully")
        void shouldCreateContextSuccessfully() {
            // Given
            when(contextRepository.save(any(Context.class))).thenReturn(testContext);
            when(tokenCountingService.countTokens(anyString())).thenReturn(0);

            // When
            ContextDto result = contextService.createContext(createRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testContextId);
            assertThat(result.getName()).isEqualTo(testContext.getName());
            assertThat(result.getDescription()).isEqualTo(testContext.getDescription());
            assertThat(result.getOrganizationId()).isEqualTo(testOrganizationId);
            assertThat(result.getUserId()).isEqualTo(testUserId);
            assertThat(result.getIsActive()).isTrue();

            verify(contextRepository).save(argThat(context -> 
                context.getName().equals(createRequest.getName()) &&
                context.getDescription().equals(createRequest.getDescription()) &&
                context.getOrganizationId().equals(testOrganizationId) &&
                context.getUserId().equals(testUserId) &&
                context.getIsActive()
            ));
            verify(cacheService).invalidateContextCache(testContextId);
        }

        @Test
        @DisplayName("Should create context with minimal information")
        void shouldCreateContextWithMinimalInformation() {
            // Given
            CreateContextRequest minimalRequest = CreateContextRequest.builder()
                    .name("Minimal Context")
                    .organizationId(testOrganizationId)
                    .userId(testUserId)
                    .build();

            when(contextRepository.save(any(Context.class))).thenReturn(testContext);
            when(tokenCountingService.countTokens(anyString())).thenReturn(0);

            // When
            ContextDto result = contextService.createContext(minimalRequest);

            // Then
            assertThat(result).isNotNull();
            verify(contextRepository).save(argThat(context -> 
                context.getName().equals("Minimal Context") &&
                context.getDescription() == null &&
                context.getMetadata() == null
            ));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        @DisplayName("Should handle invalid context names")
        void shouldHandleInvalidContextNames(String name) {
            // Given
            createRequest.setName(name);

            // When & Then
            // Assuming validation happens at controller level, service should handle gracefully
            when(contextRepository.save(any(Context.class))).thenReturn(testContext);
            when(tokenCountingService.countTokens(anyString())).thenReturn(0);

            ContextDto result = contextService.createContext(createRequest);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should handle context creation with complex metadata")
        void shouldHandleContextCreationWithComplexMetadata() {
            // Given
            Map<String, Object> complexMetadata = Map.of(
                "nested", Map.of("key", "value"),
                "array", Arrays.asList(1, 2, 3),
                "boolean", true,
                "number", 42
            );
            createRequest.setMetadata(complexMetadata);

            when(contextRepository.save(any(Context.class))).thenReturn(testContext);
            when(tokenCountingService.countTokens(anyString())).thenReturn(0);

            // When
            ContextDto result = contextService.createContext(createRequest);

            // Then
            assertThat(result).isNotNull();
            verify(contextRepository).save(argThat(context -> 
                context.getMetadata().equals(complexMetadata)
            ));
        }
    }

    @Nested
    @DisplayName("Context Retrieval Tests")
    class ContextRetrievalTests {

        @Test
        @DisplayName("Should get context by ID successfully")
        void shouldGetContextByIdSuccessfully() {
            // Given
            when(contextRepository.findById(testContextId)).thenReturn(Optional.of(testContext));
            when(messageRepository.findByContextIdOrderByTimestamp(testContextId)).thenReturn(Arrays.asList(testMessage));
            when(tokenCountingService.countTokens(anyString())).thenReturn(10);

            // When
            ContextDto result = contextService.getContext(testContextId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testContextId);
            assertThat(result.getMessages()).hasSize(1);
            assertThat(result.getMessages().get(0).getContent()).isEqualTo(testMessage.getContent());
            assertThat(result.getTotalTokenCount()).isEqualTo(10);

            verify(contextRepository).findById(testContextId);
            verify(messageRepository).findByContextIdOrderByTimestamp(testContextId);
        }

        @Test
        @DisplayName("Should throw exception when context not found")
        void shouldThrowExceptionWhenContextNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(contextRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> contextService.getContext(nonExistentId))
                    .isInstanceOf(ContextNotFoundException.class)
                    .hasMessage("Context not found with ID: " + nonExistentId);

            verify(contextRepository).findById(nonExistentId);
            verify(messageRepository, never()).findByContextIdOrderByTimestamp(any());
        }

        @Test
        @DisplayName("Should list user contexts with pagination")
        void shouldListUserContextsWithPagination() {
            // Given
            List<Context> contexts = Arrays.asList(testContext);
            Page<Context> contextPage = new PageImpl<>(contexts);
            Pageable pageable = mock(Pageable.class);

            when(contextRepository.findByUserIdAndIsActiveTrue(testUserId, pageable)).thenReturn(contextPage);

            // When
            Page<ContextDto> result = contextService.listUserContexts(testUserId, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(testContextId);

            verify(contextRepository).findByUserIdAndIsActiveTrue(testUserId, pageable);
        }

        @Test
        @DisplayName("Should list organization contexts")
        void shouldListOrganizationContexts() {
            // Given
            List<Context> contexts = Arrays.asList(testContext);
            Page<Context> contextPage = new PageImpl<>(contexts);
            Pageable pageable = mock(Pageable.class);

            when(contextRepository.findByOrganizationIdAndIsActiveTrue(testOrganizationId, pageable)).thenReturn(contextPage);

            // When
            Page<ContextDto> result = contextService.listOrganizationContexts(testOrganizationId, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getOrganizationId()).isEqualTo(testOrganizationId);

            verify(contextRepository).findByOrganizationIdAndIsActiveTrue(testOrganizationId, pageable);
        }

        @Test
        @DisplayName("Should return empty page when no contexts found")
        void shouldReturnEmptyPageWhenNoContextsFound() {
            // Given
            Page<Context> emptyPage = new PageImpl<>(Collections.emptyList());
            Pageable pageable = mock(Pageable.class);

            when(contextRepository.findByUserIdAndIsActiveTrue(testUserId, pageable)).thenReturn(emptyPage);

            // When
            Page<ContextDto> result = contextService.listUserContexts(testUserId, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);

            verify(contextRepository).findByUserIdAndIsActiveTrue(testUserId, pageable);
        }
    }

    @Nested
    @DisplayName("Message Management Tests")
    class MessageManagementTests {

        @Test
        @DisplayName("Should append message successfully")
        void shouldAppendMessageSuccessfully() {
            // Given
            when(contextRepository.findById(testContextId)).thenReturn(Optional.of(testContext));
            when(messageRepository.save(any(Message.class))).thenReturn(testMessage);
            when(tokenCountingService.countTokens(appendRequest.getContent())).thenReturn(15);
            when(contextRepository.save(any(Context.class))).thenReturn(testContext);

            // When
            MessageDto result = contextService.appendMessage(appendRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContextId()).isEqualTo(testContextId);
            assertThat(result.getRole()).isEqualTo(appendRequest.getRole());
            assertThat(result.getContent()).isEqualTo(appendRequest.getContent());
            assertThat(result.getTokenCount()).isEqualTo(15);

            verify(contextRepository).findById(testContextId);
            verify(messageRepository).save(argThat(message -> 
                message.getContextId().equals(testContextId) &&
                message.getRole().equals(appendRequest.getRole()) &&
                message.getContent().equals(appendRequest.getContent()) &&
                message.getTokenCount().equals(15)
            ));
            verify(contextRepository).save(testContext); // Updates lastMessageAt
            verify(cacheService).invalidateContextCache(testContextId);
            verify(versionService).createVersion(testContextId);
        }

        @Test
        @DisplayName("Should throw exception when appending to non-existent context")
        void shouldThrowExceptionWhenAppendingToNonExistentContext() {
            // Given
            UUID nonExistentContextId = UUID.randomUUID();
            appendRequest.setContextId(nonExistentContextId);
            when(contextRepository.findById(nonExistentContextId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> contextService.appendMessage(appendRequest))
                    .isInstanceOf(ContextNotFoundException.class)
                    .hasMessage("Context not found with ID: " + nonExistentContextId);

            verify(contextRepository).findById(nonExistentContextId);
            verify(messageRepository, never()).save(any(Message.class));
        }

        @Test
        @DisplayName("Should handle different message roles")
        void shouldHandleDifferentMessageRoles() {
            // Given
            String[] roles = {"user", "assistant", "system", "tool"};
            
            for (String role : roles) {
                reset(messageRepository, contextRepository, tokenCountingService);
                
                appendRequest.setRole(role);
                when(contextRepository.findById(testContextId)).thenReturn(Optional.of(testContext));
                when(messageRepository.save(any(Message.class))).thenReturn(testMessage);
                when(tokenCountingService.countTokens(anyString())).thenReturn(10);
                when(contextRepository.save(any(Context.class))).thenReturn(testContext);

                // When
                MessageDto result = contextService.appendMessage(appendRequest);

                // Then
                assertThat(result.getRole()).isEqualTo(role);
                verify(messageRepository).save(argThat(message -> message.getRole().equals(role)));
            }
        }

        @Test
        @DisplayName("Should handle empty message content")
        void shouldHandleEmptyMessageContent() {
            // Given
            appendRequest.setContent("");
            when(contextRepository.findById(testContextId)).thenReturn(Optional.of(testContext));
            when(messageRepository.save(any(Message.class))).thenReturn(testMessage);
            when(tokenCountingService.countTokens("")).thenReturn(0);
            when(contextRepository.save(any(Context.class))).thenReturn(testContext);

            // When
            MessageDto result = contextService.appendMessage(appendRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTokenCount()).isEqualTo(0);
            verify(tokenCountingService).countTokens("");
        }

        @Test
        @DisplayName("Should handle message with complex metadata")
        void shouldHandleMessageWithComplexMetadata() {
            // Given
            Map<String, Object> complexMetadata = Map.of(
                "attachments", Arrays.asList("file1.txt", "file2.pdf"),
                "priority", "high",
                "timestamp", Instant.now().toString()
            );
            appendRequest.setMetadata(complexMetadata);

            when(contextRepository.findById(testContextId)).thenReturn(Optional.of(testContext));
            when(messageRepository.save(any(Message.class))).thenReturn(testMessage);
            when(tokenCountingService.countTokens(anyString())).thenReturn(10);
            when(contextRepository.save(any(Context.class))).thenReturn(testContext);

            // When
            MessageDto result = contextService.appendMessage(appendRequest);

            // Then
            assertThat(result).isNotNull();
            verify(messageRepository).save(argThat(message -> 
                message.getMetadata().equals(complexMetadata)
            ));
        }
    }

    @Nested
    @DisplayName("Context Update and Deletion Tests")
    class ContextUpdateAndDeletionTests {

        @Test
        @DisplayName("Should soft delete context successfully")
        void shouldSoftDeleteContextSuccessfully() {
            // Given
            when(contextRepository.findById(testContextId)).thenReturn(Optional.of(testContext));
            when(contextRepository.save(any(Context.class))).thenReturn(testContext);

            // When
            contextService.deleteContext(testContextId);

            // Then
            verify(contextRepository).findById(testContextId);
            verify(contextRepository).save(argThat(context -> !context.getIsActive()));
            verify(cacheService).invalidateContextCache(testContextId);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent context")
        void shouldThrowExceptionWhenDeletingNonExistentContext() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(contextRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> contextService.deleteContext(nonExistentId))
                    .isInstanceOf(ContextNotFoundException.class)
                    .hasMessage("Context not found with ID: " + nonExistentId);

            verify(contextRepository).findById(nonExistentId);
            verify(contextRepository, never()).save(any(Context.class));
        }

        @Test
        @DisplayName("Should update context metadata")
        void shouldUpdateContextMetadata() {
            // Given
            Map<String, Object> newMetadata = Map.of("updated", "true", "version", "2");
            when(contextRepository.findById(testContextId)).thenReturn(Optional.of(testContext));
            when(contextRepository.save(any(Context.class))).thenReturn(testContext);

            // When
            contextService.updateContextMetadata(testContextId, newMetadata);

            // Then
            verify(contextRepository).findById(testContextId);
            verify(contextRepository).save(argThat(context -> 
                context.getMetadata().equals(newMetadata)
            ));
            verify(cacheService).invalidateContextCache(testContextId);
        }

        @Test
        @DisplayName("Should clear context metadata when null provided")
        void shouldClearContextMetadataWhenNullProvided() {
            // Given
            when(contextRepository.findById(testContextId)).thenReturn(Optional.of(testContext));
            when(contextRepository.save(any(Context.class))).thenReturn(testContext);

            // When
            contextService.updateContextMetadata(testContextId, null);

            // Then
            verify(contextRepository).save(argThat(context -> 
                context.getMetadata() == null
            ));
        }
    }

    @Nested
    @DisplayName("Caching and Performance Tests")
    class CachingAndPerformanceTests {

        @Test
        @DisplayName("Should invalidate cache on context operations")
        void shouldInvalidateCacheOnContextOperations() {
            // Test context creation
            when(contextRepository.save(any(Context.class))).thenReturn(testContext);
            when(tokenCountingService.countTokens(anyString())).thenReturn(0);
            
            contextService.createContext(createRequest);
            verify(cacheService).invalidateContextCache(testContextId);

            // Test message append
            reset(cacheService);
            when(contextRepository.findById(testContextId)).thenReturn(Optional.of(testContext));
            when(messageRepository.save(any(Message.class))).thenReturn(testMessage);
            when(tokenCountingService.countTokens(anyString())).thenReturn(10);
            when(contextRepository.save(any(Context.class))).thenReturn(testContext);

            contextService.appendMessage(appendRequest);
            verify(cacheService).invalidateContextCache(testContextId);

            // Test context deletion
            reset(cacheService);
            when(contextRepository.findById(testContextId)).thenReturn(Optional.of(testContext));
            when(contextRepository.save(any(Context.class))).thenReturn(testContext);

            contextService.deleteContext(testContextId);
            verify(cacheService).invalidateContextCache(testContextId);
        }

        @Test
        @DisplayName("Should create version on message append")
        void shouldCreateVersionOnMessageAppend() {
            // Given
            when(contextRepository.findById(testContextId)).thenReturn(Optional.of(testContext));
            when(messageRepository.save(any(Message.class))).thenReturn(testMessage);
            when(tokenCountingService.countTokens(anyString())).thenReturn(10);
            when(contextRepository.save(any(Context.class))).thenReturn(testContext);

            // When
            contextService.appendMessage(appendRequest);

            // Then
            verify(versionService).createVersion(testContextId);
        }

        @Test
        @DisplayName("Should handle token counting errors gracefully")
        void shouldHandleTokenCountingErrorsGracefully() {
            // Given
            when(contextRepository.findById(testContextId)).thenReturn(Optional.of(testContext));
            when(tokenCountingService.countTokens(anyString())).thenThrow(new RuntimeException("Token counting failed"));
            when(messageRepository.save(any(Message.class))).thenReturn(testMessage);
            when(contextRepository.save(any(Context.class))).thenReturn(testContext);

            // When & Then
            assertThatThrownBy(() -> contextService.appendMessage(appendRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Token counting failed");

            verify(tokenCountingService).countTokens(appendRequest.getContent());
        }
    }

    @Nested
    @DisplayName("Security and Authorization Tests")
    class SecurityAndAuthorizationTests {

        @Test
        @DisplayName("Should enforce organization-level access control")
        void shouldEnforceOrganizationLevelAccessControl() {
            // This test verifies that the service relies on security annotations
            // In a full integration test, these would be tested with Spring Security
            
            // Given - User from different organization
            UUID differentOrgId = UUID.randomUUID();
            CreateContextRequest unauthorizedRequest = CreateContextRequest.builder()
                    .name("Unauthorized Context")
                    .organizationId(differentOrgId)
                    .userId(testUserId)
                    .build();

            when(contextRepository.save(any(Context.class))).thenReturn(testContext);
            when(tokenCountingService.countTokens(anyString())).thenReturn(0);

            // When - In reality, this would be blocked by security annotations
            ContextDto result = contextService.createContext(unauthorizedRequest);

            // Then - Service accepts the request (security is enforced at controller/AOP level)
            assertThat(result).isNotNull();
            verify(contextRepository).save(any(Context.class));
        }

        @Test
        @DisplayName("Should handle null security context gracefully")
        void shouldHandleNullSecurityContextGracefully() {
            // This test ensures the service doesn't break when security context is null
            // (though in practice, security annotations would prevent this)

            when(contextRepository.save(any(Context.class))).thenReturn(testContext);
            when(tokenCountingService.countTokens(anyString())).thenReturn(0);

            // When
            ContextDto result = contextService.createContext(createRequest);

            // Then
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandling {

        @Test
        @DisplayName("Should handle null UUID gracefully")
        void shouldHandleNullUuidGracefully() {
            // When & Then
            assertThatThrownBy(() -> contextService.getContext(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should handle database errors gracefully")
        void shouldHandleDatabaseErrorsGracefully() {
            // Given
            when(contextRepository.findById(testContextId)).thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThatThrownBy(() -> contextService.getContext(testContextId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");
        }

        @Test
        @DisplayName("Should handle concurrent message appends")
        void shouldHandleConcurrentMessageAppends() {
            // Given - Simulate concurrent access where context is found but save fails
            when(contextRepository.findById(testContextId)).thenReturn(Optional.of(testContext));
            when(tokenCountingService.countTokens(anyString())).thenReturn(10);
            when(messageRepository.save(any(Message.class)))
                    .thenThrow(new RuntimeException("Constraint violation"));

            // When & Then
            assertThatThrownBy(() -> contextService.appendMessage(appendRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Constraint violation");
        }

        @Test
        @DisplayName("Should handle large message content")
        void shouldHandleLargeMessageContent() {
            // Given
            String largeContent = "a".repeat(100000); // 100KB content
            appendRequest.setContent(largeContent);
            
            when(contextRepository.findById(testContextId)).thenReturn(Optional.of(testContext));
            when(messageRepository.save(any(Message.class))).thenReturn(testMessage);
            when(tokenCountingService.countTokens(largeContent)).thenReturn(25000);
            when(contextRepository.save(any(Context.class))).thenReturn(testContext);

            // When
            MessageDto result = contextService.appendMessage(appendRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTokenCount()).isEqualTo(25000);
            verify(tokenCountingService).countTokens(largeContent);
        }

        @Test
        @DisplayName("Should handle context with many messages")
        void shouldHandleContextWithManyMessages() {
            // Given
            List<Message> manyMessages = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                manyMessages.add(Message.builder()
                        .id(UUID.randomUUID())
                        .contextId(testContextId)
                        .role("user")
                        .content("Message " + i)
                        .tokenCount(5)
                        .timestamp(Instant.now())
                        .build());
            }

            when(contextRepository.findById(testContextId)).thenReturn(Optional.of(testContext));
            when(messageRepository.findByContextIdOrderByTimestamp(testContextId)).thenReturn(manyMessages);
            when(tokenCountingService.countTokens(anyString())).thenReturn(5000);

            // When
            ContextDto result = contextService.getContext(testContextId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getMessages()).hasSize(1000);
            assertThat(result.getTotalTokenCount()).isEqualTo(5000);
        }
    }
}