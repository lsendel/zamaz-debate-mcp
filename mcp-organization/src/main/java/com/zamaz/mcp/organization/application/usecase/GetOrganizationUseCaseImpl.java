package com.zamaz.mcp.organization.application.usecase;

import com.zamaz.mcp.common.application.exception.ResourceNotFoundException;
import com.zamaz.mcp.common.application.exception.UseCaseException;
import com.zamaz.mcp.common.application.service.TransactionManager;
import com.zamaz.mcp.organization.application.port.inbound.GetOrganizationUseCase;
import com.zamaz.mcp.organization.application.port.outbound.OrganizationRepository;
import com.zamaz.mcp.organization.application.port.outbound.UserRepository;
import com.zamaz.mcp.organization.application.query.GetOrganizationQuery;
import com.zamaz.mcp.organization.application.query.OrganizationView;
import com.zamaz.mcp.organization.domain.model.Organization;
import com.zamaz.mcp.organization.domain.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Implementation of the get organization use case.
 * Retrieves organization details for authorized users.
 */
public class GetOrganizationUseCaseImpl implements GetOrganizationUseCase {
    
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final TransactionManager transactionManager;
    
    public GetOrganizationUseCaseImpl(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            TransactionManager transactionManager) {
        this.organizationRepository = Objects.requireNonNull(organizationRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
        this.transactionManager = Objects.requireNonNull(transactionManager);
    }
    
    @Override
    public OrganizationView execute(GetOrganizationQuery query) {
        return transactionManager.executeInReadOnlyTransaction(() -> {
            // Find organization
            var organization = organizationRepository.findById(query.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Organization not found: " + query.organizationId()
                ));
            
            // Check if requesting user has access
            if (!organization.isMember(query.requestingUserId())) {
                throw new UseCaseException(
                    "organization.access.denied",
                    "User does not have access to this organization"
                );
            }
            
            // Load member details
            var memberUserIds = organization.getMembers().stream()
                .map(member -> member.getUserId())
                .collect(Collectors.toList());
            
            var users = userRepository.findByIds(memberUserIds);
            var userMap = users.stream()
                .collect(Collectors.toMap(User::getId, user -> user));
            
            // Build member views
            var memberViews = organization.getMembers().stream()
                .map(member -> {
                    var user = userMap.get(member.getUserId());
                    if (user == null) {
                        return null; // Skip if user not found
                    }
                    return new OrganizationView.MemberView(
                        member.getUserId().value(),
                        user.getEmail().value(),
                        user.getFirstName().value(),
                        user.getLastName().value(),
                        member.getRole().name(),
                        member.getJoinedAt()
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            // Build organization view
            return new OrganizationView(
                organization.getId().value(),
                organization.getName().value(),
                organization.getDescription().value(),
                organization.getSettings().toMap(),
                organization.isActive(),
                organization.getMemberCount(),
                memberViews,
                organization.getCreatedAt(),
                organization.getUpdatedAt()
            );
        });
    }
}