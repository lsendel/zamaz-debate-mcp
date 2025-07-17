package com.zamaz.mcp.organization.adapter.persistence.mapper;

import com.zamaz.mcp.common.architecture.mapper.DomainMapper;
import com.zamaz.mcp.common.domain.model.valueobject.Email;
import com.zamaz.mcp.common.domain.model.valueobject.Name;
import com.zamaz.mcp.organization.adapter.persistence.entity.UserEntity;
import com.zamaz.mcp.organization.domain.model.User;
import com.zamaz.mcp.organization.domain.model.UserId;
import com.zamaz.mcp.organization.domain.model.UserStatus;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between User domain objects and persistence entities.
 * This is part of the persistence adapter layer.
 */
@Component
public class UserPersistenceMapper implements DomainMapper<User, UserEntity> {
    
    @Override
    public User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new User(
            UserId.from(entity.getId().toString()),
            Email.from(entity.getEmail()),
            Name.from(entity.getFirstName()),
            Name.from(entity.getLastName()),
            UserStatus.valueOf(entity.getStatus()),
            entity.getEmailVerified(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
    
    @Override
    public UserEntity fromDomain(User domain) {
        if (domain == null) {
            return null;
        }
        
        return UserEntity.builder()
            .id(domain.getId().value())
            .email(domain.getEmail().value())
            .firstName(domain.getFirstName().value())
            .lastName(domain.getLastName().value())
            .status(domain.getStatus().name())
            .emailVerified(domain.isEmailVerified())
            .createdAt(domain.getCreatedAt())
            .updatedAt(domain.getUpdatedAt())
            .build();
    }
}