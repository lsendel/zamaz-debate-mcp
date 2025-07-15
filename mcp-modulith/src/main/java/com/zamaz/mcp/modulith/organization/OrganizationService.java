package com.zamaz.mcp.modulith.organization;

import com.zamaz.mcp.modulith.shared.events.OrganizationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing organizations.
 * Publishes domain events for other modules to react to organization changes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationService {
    
    private final OrganizationRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * Creates a new organization and publishes an event.
     */
    public Organization createOrganization(String name, String description) {
        log.info("Creating organization: {}", name);
        
        Organization organization = Organization.builder()
            .name(name)
            .description(description)
            .status(Organization.OrganizationStatus.ACTIVE)
            .tier(Organization.SubscriptionTier.FREE)
            .build();
        
        Organization saved = repository.save(organization);
        
        // Publish event for other modules
        eventPublisher.publishEvent(new OrganizationCreatedEvent(
            saved.getId(),
            saved.getName(),
            saved.getTier()
        ));
        
        log.info("Organization created with ID: {}", saved.getId());
        return saved;
    }
    
    /**
     * Finds an organization by ID.
     */
    @Transactional(readOnly = true)
    public Optional<Organization> findById(UUID id) {
        return repository.findById(id);
    }
    
    /**
     * Lists all organizations.
     */
    @Transactional(readOnly = true)
    public List<Organization> listOrganizations() {
        return repository.findAll();
    }
    
    /**
     * Updates an organization's tier.
     */
    public Organization updateTier(UUID id, Organization.SubscriptionTier newTier) {
        Organization org = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + id));
        
        org.setTier(newTier);
        return repository.save(org);
    }
    
    /**
     * Activates or deactivates an organization.
     */
    public Organization updateStatus(UUID id, Organization.OrganizationStatus status) {
        Organization org = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + id));
        
        org.setStatus(status);
        return repository.save(org);
    }
}