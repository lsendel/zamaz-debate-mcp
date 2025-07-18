package com.zamaz.mcp.context.domain.model;

import com.zamaz.mcp.common.domain.model.AggregateRoot;
import com.zamaz.mcp.common.domain.model.OrganizationId;
import com.zamaz.mcp.common.domain.model.UserId;
import com.zamaz.mcp.common.domain.exception.DomainRuleViolationException;
import com.zamaz.mcp.context.domain.event.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregate root representing a conversation context.
 * Manages messages, enforces business rules, and maintains consistency.
 */
public class Context extends AggregateRoot<ContextId> {
    
    private static final int MAX_MESSAGES = 10000;
    private static final TokenCount MAX_TOTAL_TOKENS = TokenCount.of(1_000_000);
    
    private final OrganizationId organizationId;
    private final UserId userId;
    private final String name;
    private ContextMetadata metadata;
    private ContextStatus status;
    private final List<Message> messages;
    private TokenCount totalTokens;
    private final Instant createdAt;
    private Instant updatedAt;
    
    private Context(
            ContextId id,
            OrganizationId organizationId,
            UserId userId,
            String name,
            ContextMetadata metadata,
            ContextStatus status,
            List<Message> messages,
            TokenCount totalTokens,
            Instant createdAt,
            Instant updatedAt
    ) {
        super(id);
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.metadata = Objects.requireNonNull(metadata, "Metadata cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.messages = new ArrayList<>(messages);
        this.totalTokens = Objects.requireNonNull(totalTokens, "Total tokens cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created at cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated at cannot be null");
    }
    
    public static Context create(
            OrganizationId organizationId,
            UserId userId,
            String name,
            ContextMetadata metadata
    ) {
        ContextId contextId = ContextId.generate();
        Instant now = Instant.now();
        
        Context context = new Context(
            contextId,
            organizationId,
            userId,
            name,
            metadata,
            ContextStatus.ACTIVE,
            new ArrayList<>(),
            TokenCount.zero(),
            now,
            now
        );
        
        context.registerEvent(new ContextCreatedEvent(
            contextId.asString(),
            organizationId.value(),
            userId.value(),
            name,
            now
        ));
        
        return context;
    }
    
    public static Context restore(
            ContextId id,
            OrganizationId organizationId,
            UserId userId,
            String name,
            ContextMetadata metadata,
            ContextStatus status,
            List<Message> messages,
            TokenCount totalTokens,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Context(
            id,
            organizationId,
            userId,
            name,
            metadata,
            status,
            messages,
            totalTokens,
            createdAt,
            updatedAt
        );
    }
    
    public void appendMessage(MessageRole role, MessageContent content, TokenCount tokenCount) {
        validateCanModify();
        validateMessageLimit();
        validateTokenLimit(tokenCount);
        
        Message message = Message.create(role, content, tokenCount);
        messages.add(message);
        totalTokens = totalTokens.add(tokenCount);
        updatedAt = Instant.now();
        
        registerEvent(new MessageAppendedEvent(
            getId().asString(),
            message.getId().asString(),
            role.getValue(),
            content.value(),
            tokenCount.value(),
            updatedAt
        ));
    }
    
    public void hideMessage(MessageId messageId) {
        validateCanModify();
        
        Message message = findMessage(messageId);
        message.hide();
        updatedAt = Instant.now();
        
        registerEvent(new MessageHiddenEvent(
            getId().asString(),
            messageId.asString(),
            updatedAt
        ));
    }
    
    public void updateMetadata(ContextMetadata newMetadata) {
        validateCanModify();
        
        this.metadata = Objects.requireNonNull(newMetadata, "Metadata cannot be null");
        this.updatedAt = Instant.now();
        
        registerEvent(new ContextMetadataUpdatedEvent(
            getId().asString(),
            metadata.asMap(),
            updatedAt
        ));
    }
    
    public void archive() {
        if (status != ContextStatus.ACTIVE) {
            throw new DomainRuleViolationException(
                "Context.not.active",
                "Only active contexts can be archived"
            );
        }
        
        this.status = ContextStatus.ARCHIVED;
        this.updatedAt = Instant.now();
        
        registerEvent(new ContextArchivedEvent(
            getId().asString(),
            updatedAt
        ));
    }
    
    public void delete() {
        if (status == ContextStatus.DELETED) {
            throw new DomainRuleViolationException(
                "Context.already.deleted",
                "Context is already deleted"
            );
        }
        
        this.status = ContextStatus.DELETED;
        this.updatedAt = Instant.now();
        
        registerEvent(new ContextDeletedEvent(
            getId().asString(),
            updatedAt
        ));
    }
    
    public ContextWindow createWindow(TokenCount maxTokens, Optional<Integer> maxMessages) {
        List<Message> visibleMessages = messages.stream()
            .filter(Message::isVisible)
            .collect(Collectors.toList());
        
        if (visibleMessages.isEmpty()) {
            return ContextWindow.empty(getId());
        }
        
        List<Message> windowMessages = new ArrayList<>();
        TokenCount windowTokens = TokenCount.zero();
        int messageLimit = maxMessages.orElse(Integer.MAX_VALUE);
        
        // Iterate from most recent to oldest
        for (int i = visibleMessages.size() - 1; i >= 0 && windowMessages.size() < messageLimit; i--) {
            Message message = visibleMessages.get(i);
            TokenCount newTotal = windowTokens.add(message.getTokenCount());
            
            if (newTotal.isGreaterThan(maxTokens)) {
                break;
            }
            
            windowMessages.add(0, message); // Add to beginning to maintain order
            windowTokens = newTotal;
        }
        
        return ContextWindow.of(getId(), windowMessages, windowTokens);
    }
    
    public List<MessageSnapshot> createMessageSnapshots() {
        return messages.stream()
            .map(Message::toSnapshot)
            .collect(Collectors.toList());
    }
    
    private void validateCanModify() {
        if (status != ContextStatus.ACTIVE) {
            throw new DomainRuleViolationException(
                "Context.not.active",
                "Cannot modify inactive context"
            );
        }
    }
    
    private void validateMessageLimit() {
        if (messages.size() >= MAX_MESSAGES) {
            throw new DomainRuleViolationException(
                "Context.message.limit.exceeded",
                "Context has reached maximum message limit of " + MAX_MESSAGES
            );
        }
    }
    
    private void validateTokenLimit(TokenCount additionalTokens) {
        TokenCount newTotal = totalTokens.add(additionalTokens);
        if (newTotal.isGreaterThan(MAX_TOTAL_TOKENS)) {
            throw new DomainRuleViolationException(
                "Context.token.limit.exceeded",
                "Adding message would exceed token limit of " + MAX_TOTAL_TOKENS
            );
        }
    }
    
    private Message findMessage(MessageId messageId) {
        return messages.stream()
            .filter(m -> m.getId().equals(messageId))
            .findFirst()
            .orElseThrow(() -> new DomainRuleViolationException(
                "Message.not.found",
                "Message not found in context: " + messageId
            ));
    }
    
    @Override
    public void validateInvariants() {
        if (organizationId == null) {
            throw new DomainRuleViolationException(
                "Context.organizationId.required",
                "Context must belong to an organization"
            );
        }
        if (userId == null) {
            throw new DomainRuleViolationException(
                "Context.userId.required",
                "Context must have a user"
            );
        }
        if (name == null || name.trim().isEmpty()) {
            throw new DomainRuleViolationException(
                "Context.name.required",
                "Context must have a name"
            );
        }
        if (totalTokens == null || totalTokens.value() < 0) {
            throw new DomainRuleViolationException(
                "Context.totalTokens.invalid",
                "Total tokens must be non-negative"
            );
        }
    }
    
    // Getters
    public OrganizationId getOrganizationId() {
        return organizationId;
    }
    
    public UserId getUserId() {
        return userId;
    }
    
    public String getName() {
        return name;
    }
    
    public ContextMetadata getMetadata() {
        return metadata;
    }
    
    public ContextStatus getStatus() {
        return status;
    }
    
    public List<Message> getMessages() {
        return Collections.unmodifiableList(messages);
    }
    
    public List<Message> getVisibleMessages() {
        return messages.stream()
            .filter(Message::isVisible)
            .collect(Collectors.toList());
    }
    
    public TokenCount getTotalTokens() {
        return totalTokens;
    }
    
    public int getMessageCount() {
        return messages.size();
    }
    
    public int getVisibleMessageCount() {
        return (int) messages.stream().filter(Message::isVisible).count();
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public boolean isActive() {
        return status == ContextStatus.ACTIVE;
    }
    
    public boolean isArchived() {
        return status == ContextStatus.ARCHIVED;
    }
    
    public boolean isDeleted() {
        return status == ContextStatus.DELETED;
    }
}