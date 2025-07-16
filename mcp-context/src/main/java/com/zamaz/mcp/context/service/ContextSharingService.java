package com.zamaz.mcp.context.service;

import com.zamaz.mcp.context.dto.ShareContextRequest;
import com.zamaz.mcp.context.entity.Context;
import com.zamaz.mcp.context.entity.SharedContext;
import com.zamaz.mcp.context.exception.ContextNotFoundException;
import com.zamaz.mcp.context.exception.DuplicateShareException;
import com.zamaz.mcp.context.exception.UnauthorizedAccessException;
import com.zamaz.mcp.context.repository.ContextRepository;
import com.zamaz.mcp.context.repository.SharedContextRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing context sharing between organizations and users.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ContextSharingService {
    
    private final ContextRepository contextRepository;
    private final SharedContextRepository sharedContextRepository;
    
    /**
     * Share a context with another organization or user.
     */
    public SharedContext shareContext(UUID contextId, UUID organizationId, UUID sharedBy, ShareContextRequest request) {
        log.info("Sharing context {} from org {} to target", contextId, organizationId);
        
        // Verify the context exists and belongs to the organization
        Context context = contextRepository.findByIdAndOrganizationId(contextId, organizationId)
                .orElseThrow(() -> new ContextNotFoundException("Context not found: " + contextId));
        
        // Check if already shared
        if (sharedContextRepository.isContextSharedWithTarget(contextId, 
                request.getTargetOrganizationId(), request.getTargetUserId())) {
            throw new DuplicateShareException("Context is already shared with this target");
        }
        
        // Validate that either organization or user is specified
        if (request.getTargetOrganizationId() == null && request.getTargetUserId() == null) {
            throw new IllegalArgumentException("Either target organization or user must be specified");
        }
        
        SharedContext sharedContext = SharedContext.builder()
                .context(context)
                .sourceOrganizationId(organizationId)
                .targetOrganizationId(request.getTargetOrganizationId())
                .targetUserId(request.getTargetUserId())
                .permission(SharedContext.Permission.valueOf(request.getPermission().toUpperCase()))
                .sharedBy(sharedBy)
                .expiresAt(request.getExpiresAt())
                .build();
        
        sharedContext = sharedContextRepository.save(sharedContext);
        log.info("Created share {} for context {}", sharedContext.getId(), contextId);
        
        return sharedContext;
    }
    
    /**
     * Get contexts shared with an organization.
     */
    @Transactional(readOnly = true)
    public List<SharedContext> getSharedWithOrganization(UUID organizationId) {
        return sharedContextRepository.findByTargetOrganizationIdAndIsActiveTrue(organizationId);
    }
    
    /**
     * Get contexts shared with a user.
     */
    @Transactional(readOnly = true)
    public List<SharedContext> getSharedWithUser(UUID userId) {
        return sharedContextRepository.findByTargetUserIdAndIsActiveTrue(userId);
    }
    
    /**
     * Get contexts shared by an organization.
     */
    @Transactional(readOnly = true)
    public List<SharedContext> getSharedByOrganization(UUID organizationId) {
        return sharedContextRepository.findBySourceOrganizationIdAndIsActiveTrue(organizationId);
    }
    
    /**
     * Revoke a context share.
     */
    public void revokeShare(UUID shareId, UUID organizationId) {
        log.info("Revoking share {} for organization {}", shareId, organizationId);
        
        SharedContext share = sharedContextRepository.findById(shareId)
                .orElseThrow(() -> new ContextNotFoundException("Share not found: " + shareId));
        
        // Verify the organization owns the shared context
        if (!share.getSourceOrganizationId().equals(organizationId)) {
            throw new UnauthorizedAccessException("Organization does not own this share");
        }
        
        sharedContextRepository.revokeShare(shareId);
        log.info("Revoked share {}", shareId);
    }
    
    /**
     * Check if a user has access to a shared context.
     */
    @Transactional(readOnly = true)
    public SharedContext.Permission checkAccess(UUID contextId, UUID organizationId, UUID userId) {
        // First check if the user owns the context
        if (contextRepository.findByIdAndOrganizationId(contextId, organizationId).isPresent()) {
            return SharedContext.Permission.ADMIN;
        }
        
        // Check shared access
        List<SharedContext> shares = sharedContextRepository.findByTargetOrganizationIdAndIsActiveTrue(organizationId);
        shares.addAll(sharedContextRepository.findByTargetUserIdAndIsActiveTrue(userId));
        
        return shares.stream()
                .filter(share -> share.getContext().getId().equals(contextId))
                .filter(share -> !share.isExpired())
                .map(SharedContext::getPermission)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Scheduled task to clean up expired shares.
     */
    @Scheduled(fixedDelay = 3600000) // Run every hour
    public void cleanupExpiredShares() {
        log.debug("Cleaning up expired context shares");
        int deactivated = sharedContextRepository.deactivateExpiredShares(Instant.now());
        if (deactivated > 0) {
            log.info("Deactivated {} expired context shares", deactivated);
        }
    }
}