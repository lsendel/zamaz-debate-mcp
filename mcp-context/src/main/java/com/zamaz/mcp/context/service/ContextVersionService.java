package com.zamaz.mcp.context.service;

import com.zamaz.mcp.context.entity.Context;
import com.zamaz.mcp.context.entity.ContextVersion;
import com.zamaz.mcp.context.entity.Message;
import com.zamaz.mcp.context.exception.ContextNotFoundException;
import com.zamaz.mcp.context.repository.ContextRepository;
import com.zamaz.mcp.context.repository.ContextVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing context versions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ContextVersionService {
    
    private final ContextRepository contextRepository;
    private final ContextVersionRepository versionRepository;
    
    @Value("${app.context.version.retention-days:30}")
    private int retentionDays;
    
    /**
     * Create a new version of a context.
     */
    public ContextVersion createVersion(UUID contextId, UUID createdBy, String description) {
        log.info("Creating version for context: {}", contextId);
        
        Context context = contextRepository.findById(contextId)
                .orElseThrow(() -> new ContextNotFoundException("Context not found: " + contextId));
        
        // Get next version number
        Integer versionNumber = versionRepository.getNextVersionNumber(contextId);
        
        // Create snapshots of all messages
        List<ContextVersion.MessageSnapshot> messageSnapshots = context.getMessages().stream()
                .filter(m -> !m.getIsHidden())
                .map(this::createMessageSnapshot)
                .collect(Collectors.toList());
        
        ContextVersion version = ContextVersion.builder()
                .context(context)
                .version(versionNumber)
                .messages(messageSnapshots)
                .totalTokens(context.getTotalTokens())
                .metadata(context.getMetadata())
                .description(description)
                .createdBy(createdBy)
                .build();
        
        version = versionRepository.save(version);
        log.info("Created version {} for context: {}", versionNumber, contextId);
        
        return version;
    }
    
    /**
     * Get all versions for a context.
     */
    @Transactional(readOnly = true)
    public List<ContextVersion> getVersions(UUID contextId) {
        return versionRepository.findByContextIdOrderByVersionDesc(contextId);
    }
    
    /**
     * Get a specific version of a context.
     */
    @Transactional(readOnly = true)
    public ContextVersion getVersion(UUID contextId, Integer versionNumber) {
        return versionRepository.findByContextIdAndVersion(contextId, versionNumber)
                .orElseThrow(() -> new ContextNotFoundException(
                        "Version " + versionNumber + " not found for context: " + contextId));
    }
    
    /**
     * Restore a context to a specific version.
     */
    public void restoreVersion(UUID contextId, Integer versionNumber, UUID restoredBy) {
        log.info("Restoring context {} to version {}", contextId, versionNumber);
        
        Context context = contextRepository.findById(contextId)
                .orElseThrow(() -> new ContextNotFoundException("Context not found: " + contextId));
        
        ContextVersion version = getVersion(contextId, versionNumber);
        
        // Create a new version to preserve current state before restoring
        createVersion(contextId, restoredBy, "Auto-save before restore to version " + versionNumber);
        
        // Clear current messages and restore from version
        context.getMessages().clear();
        
        // Recreate messages from snapshots
        for (ContextVersion.MessageSnapshot snapshot : version.getMessages()) {
            Message message = Message.builder()
                    .context(context)
                    .role(Message.MessageRole.valueOf(snapshot.getRole()))
                    .content(snapshot.getContent())
                    .tokenCount(snapshot.getTokenCount())
                    .metadata(snapshot.getMetadata())
                    .timestamp(snapshot.getTimestamp())
                    .build();
            context.addMessage(message);
        }
        
        context.setMetadata(version.getMetadata());
        contextRepository.save(context);
        
        log.info("Restored context {} to version {}", contextId, versionNumber);
    }
    
    /**
     * Clean up old versions based on retention policy.
     */
    @Async
    public void cleanupOldVersions(UUID contextId) {
        log.debug("Cleaning up old versions for context: {}", contextId);
        
        Instant cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        versionRepository.deleteByContextIdAndCreatedAtBefore(contextId, cutoffDate);
    }
    
    /**
     * Create a message snapshot.
     */
    private ContextVersion.MessageSnapshot createMessageSnapshot(Message message) {
        return ContextVersion.MessageSnapshot.builder()
                .id(message.getId())
                .role(message.getRole().name())
                .content(message.getContent())
                .tokenCount(message.getTokenCount())
                .timestamp(message.getTimestamp())
                .metadata(message.getMetadata())
                .build();
    }
}