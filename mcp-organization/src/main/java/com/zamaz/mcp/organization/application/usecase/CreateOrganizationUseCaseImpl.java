package com.zamaz.mcp.organization.application.usecase;

import com.zamaz.mcp.common.application.exception.ResourceNotFoundException;
import com.zamaz.mcp.common.application.exception.UseCaseException;
import com.zamaz.mcp.common.application.service.TransactionManager;
import com.zamaz.mcp.common.application.service.ValidationService;
import com.zamaz.mcp.common.domain.event.DomainEventPublisher;
import com.zamaz.mcp.organization.application.command.CreateOrganizationCommand;
import com.zamaz.mcp.organization.application.port.inbound.CreateOrganizationUseCase;
import com.zamaz.mcp.organization.application.port.outbound.NotificationService;
import com.zamaz.mcp.organization.application.port.outbound.OrganizationRepository;
import com.zamaz.mcp.organization.application.port.outbound.UserRepository;
import com.zamaz.mcp.organization.domain.model.Organization;
import com.zamaz.mcp.organization.domain.model.OrganizationId;
import com.zamaz.mcp.organization.domain.model.OrganizationSettings;
import com.zamaz.mcp.organization.domain.service.OrganizationDomainService;

import java.util.Objects;

/**
 * Implementation of the create organization use case.
 * Orchestrates the creation of a new organization.
 */
public class CreateOrganizationUseCaseImpl implements CreateOrganizationUseCase {
    
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OrganizationDomainService domainService;
    private final NotificationService notificationService;
    private final DomainEventPublisher eventPublisher;
    private final TransactionManager transactionManager;
    private final ValidationService validationService;
    
    public CreateOrganizationUseCaseImpl(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            OrganizationDomainService domainService,
            NotificationService notificationService,
            DomainEventPublisher eventPublisher,
            TransactionManager transactionManager,
            ValidationService validationService) {
        this.organizationRepository = Objects.requireNonNull(organizationRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
        this.domainService = Objects.requireNonNull(domainService);
        this.notificationService = Objects.requireNonNull(notificationService);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
        this.transactionManager = Objects.requireNonNull(transactionManager);
        this.validationService = Objects.requireNonNull(validationService);
    }
    
    @Override
    public OrganizationId execute(CreateOrganizationCommand command) {
        // Validate command
        validationService.validateOrThrow(command);
        
        return transactionManager.executeInTransaction(() -> {
            // Check if organization name is available
            if (!domainService.isOrganizationNameAvailable(command.name())) {
                throw new UseCaseException(
                    "organization.name.taken",
                    "Organization name '" + command.name().value() + "' is already taken"
                );
            }
            
            // Verify creator user exists and is active
            var creator = userRepository.findById(command.creatorUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "User not found: " + command.creatorUserId()
                ));
            
            if (!creator.canJoinOrganizations()) {
                throw new UseCaseException(
                    "user.cannotCreateOrganization",
                    "User cannot create organizations. Email verification may be required."
                );
            }
            
            // Create organization with settings
            var organizationId = OrganizationId.generate();
            var settings = command.initialSettings() != null
                ? OrganizationSettings.from(command.initialSettings())
                : OrganizationSettings.defaultSettings();
            
            var organization = new Organization(
                organizationId,
                command.name(),
                command.description(),
                command.creatorUserId()
            );
            
            if (command.initialSettings() != null) {
                organization.updateSettings(settings);
            }
            
            // Save organization
            organizationRepository.save(organization);
            
            // Publish domain events
            eventPublisher.publishAll(organization.getUncommittedEvents());
            organization.markEventsAsCommitted();
            
            // Send notifications
            notificationService.sendOrganizationCreatedNotification(organization, creator);
            
            return organizationId;
        });
    }
}