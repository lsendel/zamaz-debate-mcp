package com.zamaz.mcp.organization.application.usecase;

import com.zamaz.mcp.common.application.exception.ResourceNotFoundException;
import com.zamaz.mcp.common.application.exception.UseCaseException;
import com.zamaz.mcp.common.application.service.TransactionManager;
import com.zamaz.mcp.common.application.service.ValidationService;
import com.zamaz.mcp.common.domain.event.DomainEventPublisher;
import com.zamaz.mcp.organization.application.command.RemoveUserFromOrganizationCommand;
import com.zamaz.mcp.organization.application.port.inbound.RemoveUserFromOrganizationUseCase;
import com.zamaz.mcp.organization.application.port.outbound.NotificationService;
import com.zamaz.mcp.organization.application.port.outbound.OrganizationRepository;
import com.zamaz.mcp.organization.application.port.outbound.UserRepository;
import com.zamaz.mcp.organization.domain.model.Role;

import java.util.Objects;

/**
 * Implementation of the remove user from organization use case.
 * Removes users from organizations with proper authorization.
 */
public class RemoveUserFromOrganizationUseCaseImpl implements RemoveUserFromOrganizationUseCase {
    
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final DomainEventPublisher eventPublisher;
    private final TransactionManager transactionManager;
    private final ValidationService validationService;
    
    public RemoveUserFromOrganizationUseCaseImpl(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            DomainEventPublisher eventPublisher,
            TransactionManager transactionManager,
            ValidationService validationService) {
        this.organizationRepository = Objects.requireNonNull(organizationRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
        this.notificationService = Objects.requireNonNull(notificationService);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
        this.transactionManager = Objects.requireNonNull(transactionManager);
        this.validationService = Objects.requireNonNull(validationService);
    }
    
    @Override
    public void execute(RemoveUserFromOrganizationCommand command) {
        // Validate command
        validationService.validateOrThrow(command);
        
        transactionManager.executeInTransaction(() -> {
            // Find organization
            var organization = organizationRepository.findById(command.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Organization not found: " + command.organizationId()
                ));
            
            // Special case: users can remove themselves
            boolean isSelfRemoval = command.userToRemove().equals(command.removedBy());
            
            if (!isSelfRemoval) {
                // Check if removing user has permission
                var removingMember = organization.getMembers().stream()
                    .filter(m -> m.getUserId().equals(command.removedBy()))
                    .findFirst()
                    .orElseThrow(() -> new UseCaseException(
                        "organization.removeUser.notMember",
                        "Removing user is not a member of this organization"
                    ));
                
                var userToRemoveMember = organization.getMembers().stream()
                    .filter(m -> m.getUserId().equals(command.userToRemove()))
                    .findFirst()
                    .orElseThrow(() -> new UseCaseException(
                        "organization.removeUser.userNotFound",
                        "User to remove is not a member of this organization"
                    ));
                
                // Check if removing user can manage the user to be removed
                if (!removingMember.canManage(userToRemoveMember)) {
                    throw new UseCaseException(
                        "organization.removeUser.unauthorized",
                        "User does not have permission to remove this user"
                    );
                }
            }
            
            // Find user for notification
            var userToRemove = userRepository.findById(command.userToRemove())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "User not found: " + command.userToRemove()
                ));
            
            // Remove user from organization
            organization.removeUser(command.userToRemove());
            
            // Save changes
            organizationRepository.save(organization);
            
            // Publish domain events
            eventPublisher.publishAll(organization.getUncommittedEvents());
            organization.markEventsAsCommitted();
            
            // Send notification
            notificationService.sendUserRemovedFromOrganizationNotification(
                organization,
                userToRemove
            );
        });
    }
}