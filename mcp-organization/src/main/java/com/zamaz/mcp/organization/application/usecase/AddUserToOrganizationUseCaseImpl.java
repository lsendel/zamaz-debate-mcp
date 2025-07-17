package com.zamaz.mcp.organization.application.usecase;

import com.zamaz.mcp.common.application.exception.ResourceNotFoundException;
import com.zamaz.mcp.common.application.exception.UseCaseException;
import com.zamaz.mcp.common.application.service.TransactionManager;
import com.zamaz.mcp.common.application.service.ValidationService;
import com.zamaz.mcp.common.domain.event.DomainEventPublisher;
import com.zamaz.mcp.organization.application.command.AddUserToOrganizationCommand;
import com.zamaz.mcp.organization.application.port.inbound.AddUserToOrganizationUseCase;
import com.zamaz.mcp.organization.application.port.outbound.NotificationService;
import com.zamaz.mcp.organization.application.port.outbound.OrganizationRepository;
import com.zamaz.mcp.organization.application.port.outbound.UserRepository;
import com.zamaz.mcp.organization.domain.model.Role;
import com.zamaz.mcp.organization.domain.service.OrganizationDomainService;

import java.util.Objects;

/**
 * Implementation of the add user to organization use case.
 * Adds users to organizations with proper authorization.
 */
public class AddUserToOrganizationUseCaseImpl implements AddUserToOrganizationUseCase {
    
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OrganizationDomainService domainService;
    private final NotificationService notificationService;
    private final DomainEventPublisher eventPublisher;
    private final TransactionManager transactionManager;
    private final ValidationService validationService;
    
    public AddUserToOrganizationUseCaseImpl(
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
    public void execute(AddUserToOrganizationCommand command) {
        // Validate command
        validationService.validateOrThrow(command);
        
        transactionManager.executeInTransaction(() -> {
            // Find organization
            var organization = organizationRepository.findById(command.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Organization not found: " + command.organizationId()
                ));
            
            // Check if adding user has permission (must be ADMIN or OWNER)
            if (!organization.hasRole(command.addedBy(), Role.ADMIN)) {
                throw new UseCaseException(
                    "organization.addUser.unauthorized",
                    "User does not have permission to add users to this organization"
                );
            }
            
            // Find user to add
            var userToAdd = userRepository.findById(command.userToAdd())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "User not found: " + command.userToAdd()
                ));
            
            // Validate user can join
            if (!userToAdd.canJoinOrganizations()) {
                throw new UseCaseException(
                    "user.cannotJoinOrganization",
                    "User cannot join organizations. Email verification may be required."
                );
            }
            
            // Additional domain validation
            domainService.validateUserCanJoinOrganization(
                command.userToAdd(),
                command.organizationId()
            );
            
            // Check if adding user can assign the requested role
            var addingMember = organization.getMembers().stream()
                .filter(m -> m.getUserId().equals(command.addedBy()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Adding user not found in organization"));
            
            if (!addingMember.getRole().canManage(command.role())) {
                throw new UseCaseException(
                    "organization.role.cannotAssign",
                    "User cannot assign role " + command.role() + " to other users"
                );
            }
            
            // Add user to organization
            organization.addUser(command.userToAdd(), command.role());
            
            // Save changes
            organizationRepository.save(organization);
            
            // Publish domain events
            eventPublisher.publishAll(organization.getUncommittedEvents());
            organization.markEventsAsCommitted();
            
            // Send notification
            notificationService.sendUserAddedToOrganizationNotification(
                organization,
                userToAdd,
                command.role()
            );
        });
    }
}