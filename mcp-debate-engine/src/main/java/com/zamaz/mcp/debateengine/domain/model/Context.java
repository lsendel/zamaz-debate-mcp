package com.zamaz.mcp.debateengine.domain.model;

import com.zamaz.mcp.common.domain.Entity;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Entity representing a conversation context.
 */
public class Context implements Entity<ContextId> {
    
    private final ContextId id;
    private final DebateId debateId;
    private final OrganizationId organizationId;
    private final UUID userId;
    private String name;
    private String description;
    private ContextStatus status;
    private int totalTokens;
    private final int maxTokens;
    private final int windowSize;
    private int version;
    private final List<Message> messages;
    private LocalDateTime lastActivityAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Create a new context.
     */
    public static Context create(
            ContextId id,
            DebateId debateId,
            OrganizationId organizationId,
            UUID userId,
            String name,
            String description,
            int maxTokens,
            int windowSize) {
        return new Context(
            id,
            debateId,
            organizationId,
            userId,
            name,
            description,
            ContextStatus.ACTIVE,
            0,
            maxTokens,
            windowSize,
            1,
            new ArrayList<>(),
            LocalDateTime.now(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }
    
    private Context(
            ContextId id,
            DebateId debateId,
            OrganizationId organizationId,
            UUID userId,
            String name,
            String description,
            ContextStatus status,
            int totalTokens,
            int maxTokens,
            int windowSize,
            int version,
            List<Message> messages,
            LocalDateTime lastActivityAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.debateId = debateId; // Can be null for non-debate contexts
        this.organizationId = Objects.requireNonNull(organizationId);
        this.userId = Objects.requireNonNull(userId);
        this.name = Objects.requireNonNull(name);
        this.description = description;
        this.status = Objects.requireNonNull(status);
        this.totalTokens = totalTokens;
        this.maxTokens = maxTokens;
        this.windowSize = windowSize;
        this.version = version;
        this.messages = new ArrayList<>(messages);
        this.lastActivityAt = Objects.requireNonNull(lastActivityAt);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("Max tokens must be positive");
        }
        if (windowSize <= 0) {
            throw new IllegalArgumentException("Window size must be positive");
        }
        if (version < 1) {
            throw new IllegalArgumentException("Version must be positive");
        }
    }
    
    /**
     * Add a message to the context.
     */
    public Message addMessage(Message.MessageRole role, String content, int tokenCount) {
        return addMessage(role, content, tokenCount, null, null);
    }
    
    /**
     * Add a debate message to the context.
     */
    public Message addMessage(
            Message.MessageRole role,
            String content,
            int tokenCount,
            RoundId roundId,
            ParticipantId participantId) {
        if (status != ContextStatus.ACTIVE) {
            throw new IllegalStateException("Cannot add messages to inactive context");
        }
        
        Message message;
        if (roundId != null && participantId != null) {
            message = Message.createDebateMessage(
                role,
                content,
                messages.size() + 1,
                tokenCount,
                roundId,
                participantId
            );
        } else {
            message = Message.create(
                role,
                content,
                messages.size() + 1,
                tokenCount
            );
        }
        
        messages.add(message);
        totalTokens += tokenCount;
        lastActivityAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        // Apply windowing if needed
        applyWindowing();
        
        return message;
    }
    
    /**
     * Apply token windowing to maintain context size.
     */
    private void applyWindowing() {
        while (totalTokens > windowSize && messages.size() > 1) {
            // Remove oldest non-system messages
            for (int i = 0; i < messages.size(); i++) {
                Message msg = messages.get(i);
                if (msg.role() != Message.MessageRole.SYSTEM) {
                    messages.remove(i);
                    totalTokens -= msg.tokenCount();
                    break;
                }
            }
        }
    }
    
    /**
     * Create a version snapshot.
     */
    public ContextVersion createVersion(String changeSummary, UUID createdBy) {
        version++;
        updatedAt = LocalDateTime.now();
        
        return ContextVersion.create(
            id,
            version,
            createSnapshot(),
            changeSummary,
            createdBy
        );
    }
    
    /**
     * Create snapshot of current state.
     */
    private Map<String, Object> createSnapshot() {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("name", name);
        snapshot.put("description", description);
        snapshot.put("status", status.name());
        snapshot.put("totalTokens", totalTokens);
        snapshot.put("version", version);
        snapshot.put("messages", messages.stream()
            .map(msg -> Map.of(
                "role", msg.role().name(),
                "content", msg.content(),
                "tokenCount", msg.tokenCount(),
                "timestamp", msg.timestamp().toString()
            ))
            .toList());
        return snapshot;
    }
    
    /**
     * Archive the context.
     */
    public void archive() {
        if (status == ContextStatus.ARCHIVED) {
            throw new IllegalStateException("Context is already archived");
        }
        this.status = ContextStatus.ARCHIVED;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Delete the context.
     */
    public void delete() {
        if (status == ContextStatus.DELETED) {
            throw new IllegalStateException("Context is already deleted");
        }
        this.status = ContextStatus.DELETED;
        this.updatedAt = LocalDateTime.now();
    }
    
    @Override
    public ContextId getId() {
        return id;
    }
    
    public DebateId getDebateId() {
        return debateId;
    }
    
    public OrganizationId getOrganizationId() {
        return organizationId;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public ContextStatus getStatus() {
        return status;
    }
    
    public int getTotalTokens() {
        return totalTokens;
    }
    
    public int getMaxTokens() {
        return maxTokens;
    }
    
    public int getWindowSize() {
        return windowSize;
    }
    
    public int getVersion() {
        return version;
    }
    
    public List<Message> getMessages() {
        return Collections.unmodifiableList(messages);
    }
    
    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Context context = (Context) o;
        return Objects.equals(id, context.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    /**
     * Context status enumeration.
     */
    public enum ContextStatus {
        ACTIVE,
        ARCHIVED,
        DELETED
    }
}