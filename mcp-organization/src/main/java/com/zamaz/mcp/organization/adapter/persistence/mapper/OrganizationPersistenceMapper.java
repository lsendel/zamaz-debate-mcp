package com.zamaz.mcp.organization.adapter.persistence.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.common.architecture.mapper.DomainMapper;
import com.zamaz.mcp.common.architecture.adapter.persistence.PersistenceAdapter;
import com.zamaz.mcp.organization.adapter.persistence.entity.OrganizationEntity;
import com.zamaz.mcp.organization.adapter.persistence.entity.OrganizationUserEntity;
import com.zamaz.mcp.organization.adapter.persistence.entity.UserEntity;
import com.zamaz.mcp.organization.domain.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper for converting between domain objects and persistence entities.
 * This is part of the persistence adapter layer.
 */
@Component
@RequiredArgsConstructor
public class OrganizationPersistenceMapper implements DomainMapper<Organization, OrganizationEntity>, PersistenceAdapter {
    
    private final ObjectMapper objectMapper;
    private final UserPersistenceMapper userMapper;
    
    @Override
    public Organization toDomain(OrganizationEntity entity) {
        if (entity == null) {
            return null;
        }
        
        // Map organization members
        Map<UserId, OrganizationMember> members = entity.getOrganizationUsers().stream()
            .collect(Collectors.toMap(
                ou -> UserId.from(ou.getUser().getId().toString()),
                ou -> new OrganizationMember(
                    UserId.from(ou.getUser().getId().toString()),
                    Role.fromString(ou.getRole()),
                    ou.getJoinedAt()
                )
            ));
        
        // Map settings from JSON
        Map<String, Object> settings = jsonNodeToMap(entity.getSettings());
        
        // Reconstruct domain object
        return new Organization(
            OrganizationId.from(entity.getId().toString()),
            OrganizationName.from(entity.getName()),
            OrganizationDescription.from(entity.getDescription()),
            OrganizationSettings.from(settings),
            entity.getActive(),
            members,
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
    
    @Override
    public OrganizationEntity fromDomain(Organization domain) {
        if (domain == null) {
            return null;
        }
        
        // Convert settings to JSON
        JsonNode settingsJson = objectMapper.valueToTree(domain.getSettings().toMap());
        
        // Create entity
        OrganizationEntity entity = OrganizationEntity.builder()
            .id(domain.getId().value())
            .name(domain.getName().value())
            .description(domain.getDescription().value())
            .settings(settingsJson)
            .active(domain.isActive())
            .createdAt(domain.getCreatedAt())
            .updatedAt(domain.getUpdatedAt())
            .build();
        
        // Note: Organization members are managed separately through OrganizationUserEntity
        // This avoids circular dependencies and allows for lazy loading
        
        return entity;
    }
    
    /**
     * Maps organization member from domain to entity.
     */
    public OrganizationUserEntity fromDomainMember(
            OrganizationEntity organization,
            UserEntity user,
            OrganizationMember member) {
        
        return OrganizationUserEntity.builder()
            .organization(organization)
            .user(user)
            .role(member.getRole().name())
            .joinedAt(member.getJoinedAt())
            .build();
    }
    
    /**
     * Converts JsonNode to Map.
     */
    private Map<String, Object> jsonNodeToMap(JsonNode node) {
        if (node == null || node.isNull()) {
            return new HashMap<>();
        }
        
        try {
            return objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}