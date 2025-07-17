package com.zamaz.mcp.organization.adapter.persistence.repository;

import com.zamaz.mcp.common.architecture.adapter.persistence.PersistenceAdapter;
import com.zamaz.mcp.common.architecture.exception.PersistenceException;
import com.zamaz.mcp.organization.adapter.persistence.mapper.OrganizationPersistenceMapper;
import com.zamaz.mcp.organization.application.port.outbound.OrganizationRepository;
import com.zamaz.mcp.organization.domain.model.Organization;
import com.zamaz.mcp.organization.domain.model.OrganizationId;
import com.zamaz.mcp.organization.domain.model.OrganizationName;
import com.zamaz.mcp.organization.domain.model.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JPA implementation of the OrganizationRepository port.
 * This adapter translates between the domain model and JPA entities.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JpaOrganizationRepository implements OrganizationRepository, PersistenceAdapter {
    
    private final SpringDataOrganizationRepository jpaRepository;
    private final SpringDataUserRepository userRepository;
    private final OrganizationPersistenceMapper mapper;
    
    @Override
    @Transactional
    public Organization save(Organization organization) {
        try {
            // Convert to entity
            var entity = mapper.fromDomain(organization);
            
            // Handle members separately
            if (entity.getId() != null) {
                // For updates, we need to sync members
                var existingEntity = jpaRepository.findByIdWithMembers(entity.getId())
                    .orElse(entity);
                
                // Update entity fields
                existingEntity.setName(entity.getName());
                existingEntity.setDescription(entity.getDescription());
                existingEntity.setSettings(entity.getSettings());
                existingEntity.setActive(entity.getActive());
                existingEntity.setUpdatedAt(entity.getUpdatedAt());
                
                // Sync members
                syncOrganizationMembers(existingEntity, organization);
                
                entity = existingEntity;
            }
            
            // Save entity
            var saved = jpaRepository.save(entity);
            
            // Convert back to domain
            return mapper.toDomain(saved);
            
        } catch (Exception e) {
            throw new PersistenceException("Failed to save organization", e);
        }
    }
    
    @Override
    public Optional<Organization> findById(OrganizationId id) {
        try {
            return jpaRepository.findByIdWithMembers(id.value())
                .map(mapper::toDomain);
        } catch (Exception e) {
            throw new PersistenceException("Failed to find organization by ID", e);
        }
    }
    
    @Override
    public void delete(Organization organization) {
        try {
            jpaRepository.deleteById(organization.getId().value());
        } catch (Exception e) {
            throw new PersistenceException("Failed to delete organization", e);
        }
    }
    
    @Override
    public List<Organization> findAll() {
        try {
            return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new PersistenceException("Failed to find all organizations", e);
        }
    }
    
    @Override
    public Optional<Organization> findByName(OrganizationName name) {
        try {
            return jpaRepository.findByName(name.value())
                .map(mapper::toDomain);
        } catch (Exception e) {
            throw new PersistenceException("Failed to find organization by name", e);
        }
    }
    
    @Override
    public boolean existsByName(OrganizationName name) {
        try {
            return jpaRepository.existsByName(name.value());
        } catch (Exception e) {
            throw new PersistenceException("Failed to check organization existence by name", e);
        }
    }
    
    @Override
    public List<Organization> findByMemberUserId(UserId userId) {
        try {
            return jpaRepository.findByMemberUserId(userId.value()).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new PersistenceException("Failed to find organizations by member", e);
        }
    }
    
    @Override
    public List<Organization> findAllActive() {
        try {
            return jpaRepository.findByActiveTrue().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new PersistenceException("Failed to find active organizations", e);
        }
    }
    
    @Override
    public long count() {
        try {
            return jpaRepository.count();
        } catch (Exception e) {
            throw new PersistenceException("Failed to count organizations", e);
        }
    }
    
    /**
     * Syncs organization members between domain and entity.
     */
    private void syncOrganizationMembers(
            com.zamaz.mcp.organization.adapter.persistence.entity.OrganizationEntity entity,
            Organization domain) {
        
        // Clear existing members
        entity.getOrganizationUsers().clear();
        
        // Add members from domain
        domain.getMembers().forEach(member -> {
            var userEntity = userRepository.findById(member.getUserId().value())
                .orElseThrow(() -> new PersistenceException(
                    "User not found: " + member.getUserId()
                ));
            
            var organizationUser = mapper.fromDomainMember(entity, userEntity, member);
            entity.addUser(organizationUser);
        });
    }
}