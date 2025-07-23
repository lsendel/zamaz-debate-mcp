package com.zamaz.mcp.organization.service;

import com.zamaz.mcp.organization.dto.OrganizationDto;
import com.zamaz.mcp.organization.entity.Organization;
import com.zamaz.mcp.organization.entity.OrganizationUser;
import com.zamaz.mcp.organization.entity.User;
import com.zamaz.mcp.organization.exception.ResourceNotFoundException;
import com.zamaz.mcp.organization.exception.DuplicateResourceException;
import com.zamaz.mcp.organization.repository.OrganizationRepository;
import com.zamaz.mcp.organization.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    public OrganizationDto createOrganization(OrganizationDto.CreateOrganizationRequest request) {
        log.debug("Creating organization with name: {}", request.getName());

        validateOrganizationNameUniqueness(request.getName());

        Organization organization = mapCreateRequestToEntity(request);
        organization = organizationRepository.save(organization);

        log.info("Created organization with ID: {}", organization.getId());
        return mapEntityToDto(organization);
    }

    @Cacheable(value = "organizations", key = "#id")
    public OrganizationDto getOrganization(UUID id) {
        log.debug("Getting organization with ID: {}", id);
        Organization organization = findOrganizationById(id);
        return mapEntityToDto(organization);
    }

    public Page<OrganizationDto> listOrganizations(Pageable pageable) {
        log.debug("Listing organizations with pageable: {}", pageable);
        return organizationRepository.findAll(pageable).map(this::mapEntityToDto);
    }

    public List<OrganizationDto> listUserOrganizations(UUID userId) {
        log.debug("Listing organizations for user: {}", userId);
        return organizationRepository.findActiveOrganizationsByUserId(userId)
                .stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "organizations", key = "#id")
    public OrganizationDto updateOrganization(UUID id, OrganizationDto.UpdateOrganizationRequest request) {
        log.debug("Updating organization with ID: {}", id);

        Organization organization = findOrganizationById(id);
        updateOrganizationFromRequest(organization, request);

        organization = organizationRepository.save(organization);
        log.info("Updated organization with ID: {}", organization.getId());

        return mapEntityToDto(organization);
    }

    @CacheEvict(value = "organizations", key = "#id")
    public void deleteOrganization(UUID id) {
        log.debug("Deleting organization with ID: {}", id);

        if (!organizationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Organization not found with ID: " + id);
        }

        organizationRepository.deleteById(id);
        log.info("Deleted organization with ID: {}", id);
    }

    public void addUserToOrganization(UUID organizationId, UUID userId, String role) {
        log.debug("Adding user {} to organization {} with role {}", userId, organizationId, role);

        Organization organization = findOrganizationById(organizationId);
        User user = findUserById(userId);

        OrganizationUser organizationUser = OrganizationUser.builder()
                .organization(organization)
                .user(user)
                .role(role)
                .build();

        organization.getOrganizationUsers().add(organizationUser);
        organizationRepository.save(organization);

        log.info("Added user {} to organization {} with role {}", userId, organizationId, role);
    }

    public void removeUserFromOrganization(UUID organizationId, UUID userId) {
        log.debug("Removing user {} from organization {}", userId, organizationId);

        Organization organization = findOrganizationById(organizationId);

        organization.getOrganizationUsers().removeIf(ou -> ou.getUser().getId().equals(userId));
        organizationRepository.save(organization);

        log.info("Removed user {} from organization {}", userId, organizationId);
    }

    private OrganizationDto mapEntityToDto(Organization organization) {
        Long userCount = organizationRepository.countUsersByOrganizationId(organization.getId());

        return OrganizationDto.builder()
                .id(organization.getId())
                .name(organization.getName())
                .description(organization.getDescription())
                .settings(organization.getSettings())
                .createdAt(organization.getCreatedAt())
                .updatedAt(organization.getUpdatedAt())
                .isActive(organization.getIsActive())
                .userCount(userCount.intValue())
                .build();
    }

    private void validateOrganizationNameUniqueness(String organizationName) {
        if (organizationRepository.existsByNameIgnoreCase(organizationName)) {
            throw new DuplicateResourceException("Organization with name '" + organizationName + "' already exists");
        }
    }

    private boolean isOrganizationNameAvailable(String organizationName) {
        return !organizationRepository.existsByNameIgnoreCase(organizationName);
    }

    private boolean hasUserAccessToOrganization(UUID userId, UUID organizationId) {
        return organizationRepository.findActiveOrganizationsByUserId(userId)
                .stream()
                .anyMatch(org -> org.getId().equals(organizationId));
    }

    private Organization mapCreateRequestToEntity(OrganizationDto.CreateOrganizationRequest request) {
        return Organization.builder()
                .name(request.getName())
                .description(request.getDescription())
                .settings(request.getSettings())
                .build();
    }

    private Organization findOrganizationById(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + organizationId));
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }

    private void updateOrganizationFromRequest(Organization organization,
            OrganizationDto.UpdateOrganizationRequest request) {
        if (request.getName() != null && !request.getName().equals(organization.getName())) {
            validateOrganizationNameUniqueness(request.getName());
            organization.setName(request.getName());
        }

        if (request.getDescription() != null) {
            organization.setDescription(request.getDescription());
        }

        if (request.getSettings() != null) {
            organization.setSettings(request.getSettings());
        }

        if (request.getIsActive() != null) {
            organization.setIsActive(request.getIsActive());
        }
    }
}