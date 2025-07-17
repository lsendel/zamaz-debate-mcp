package com.zamaz.mcp.organization.application.usecase;

import com.zamaz.mcp.common.application.exception.ResourceNotFoundException;
import com.zamaz.mcp.common.application.exception.UseCaseException;
import com.zamaz.mcp.common.application.service.TransactionManager;
import com.zamaz.mcp.common.application.service.ValidationService;
import com.zamaz.mcp.common.domain.event.DomainEventPublisher;
import com.zamaz.mcp.organization.application.command.UpdateOrganizationCommand;
import com.zamaz.mcp.organization.application.port.inbound.UpdateOrganizationUseCase;
import com.zamaz.mcp.organization.application.port.outbound.OrganizationRepository;
import com.zamaz.mcp.organization.domain.model.OrganizationSettings;
import com.zamaz.mcp.organization.domain.model.Role;
import com.zamaz.mcp.organization.domain.service.OrganizationDomainService;

import java.util.Objects;

/**
 * Implementation of the update organization use case.
 * Updates organization details for authorized users.
 */
public class UpdateOrganizationUseCaseImpl implements UpdateOrganizationUseCase {
    
    private final OrganizationRepository organizationRepository;
    private final OrganizationDomainService domainService;
    private final DomainEventPublisher eventPublisher;
    private final TransactionManager transactionManager;
    private final ValidationService validationService;
    
    public UpdateOrganizationUseCaseImpl(
            OrganizationRepository organizationRepository,
            OrganizationDomainService domainService,
            DomainEventPublisher eventPublisher,
            TransactionManager transactionManager,
            ValidationService validationService) {
        this.organizationRepository = Objects.requireNonNull(organizationRepository);
        this.domainService = Objects.requireNonNull(domainService);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
        this.transactionManager = Objects.requireNonNull(transactionManager);
        this.validationService = Objects.requireNonNull(validationService);
    }
    
    @Override
    public void execute(UpdateOrganizationCommand command) {
        // Validate command
        validationService.validateOrThrow(command);
        
        transactionManager.executeInTransaction(() -> {
            // Find organization
            var organization = organizationRepository.findById(command.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Organization not found: " + command.organizationId()
                ));
            
            // Check permissions - only OWNER or ADMIN can update
            if (!organization.hasRole(command.updatingUserId(), Role.ADMIN)) {
                throw new UseCaseException(
                    "organization.update.unauthorized",
                    "User does not have permission to update this organization"
                );
            }
            
            // Update name and description if provided
            command.getName().ifPresent(newName -> {
                // Check if new name is available (if changed)
                if (!organization.getName().equals(newName) && 
                    !domainService.isOrganizationNameAvailable(newName)) {
                    throw new UseCaseException(
                        "organization.name.taken",
                        "Organization name '" + newName.value() + "' is already taken"
                    );
                }
            });
            
            if (command.getName().isPresent() || command.getDescription().isPresent()) {
                organization.update(
                    command.getName().orElse(organization.getName()),
                    command.getDescription().orElse(organization.getDescription())
                );
            }
            
            // Update settings if provided
            command.getSettings().ifPresent(settings -> {
                var newSettings = OrganizationSettings.from(settings);
                domainService.validateOrganizationSettings(newSettings, organization.getId());
                organization.updateSettings(newSettings);
            });
            
            // Save changes
            organizationRepository.save(organization);
            
            // Publish domain events
            eventPublisher.publishAll(organization.getUncommittedEvents());
            organization.markEventsAsCommitted();
        });
    }
}