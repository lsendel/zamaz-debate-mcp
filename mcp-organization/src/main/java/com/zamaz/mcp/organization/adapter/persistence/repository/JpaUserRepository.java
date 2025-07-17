package com.zamaz.mcp.organization.adapter.persistence.repository;

import com.zamaz.mcp.common.architecture.adapter.persistence.PersistenceAdapter;
import com.zamaz.mcp.common.architecture.exception.PersistenceException;
import com.zamaz.mcp.common.domain.model.valueobject.Email;
import com.zamaz.mcp.organization.adapter.persistence.mapper.UserPersistenceMapper;
import com.zamaz.mcp.organization.application.port.outbound.UserRepository;
import com.zamaz.mcp.organization.domain.model.User;
import com.zamaz.mcp.organization.domain.model.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA implementation of the UserRepository port.
 * This adapter translates between the domain model and JPA entities.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JpaUserRepository implements UserRepository, PersistenceAdapter {
    
    private final SpringDataUserRepository jpaRepository;
    private final UserPersistenceMapper mapper;
    
    @Override
    @Transactional
    public User save(User user) {
        try {
            var entity = mapper.fromDomain(user);
            var saved = jpaRepository.save(entity);
            return mapper.toDomain(saved);
        } catch (Exception e) {
            throw new PersistenceException("Failed to save user", e);
        }
    }
    
    @Override
    public Optional<User> findById(UserId id) {
        try {
            return jpaRepository.findById(id.value())
                .map(mapper::toDomain);
        } catch (Exception e) {
            throw new PersistenceException("Failed to find user by ID", e);
        }
    }
    
    @Override
    public void delete(User user) {
        try {
            jpaRepository.deleteById(user.getId().value());
        } catch (Exception e) {
            throw new PersistenceException("Failed to delete user", e);
        }
    }
    
    @Override
    public List<User> findAll() {
        try {
            return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new PersistenceException("Failed to find all users", e);
        }
    }
    
    @Override
    public Optional<User> findByEmail(Email email) {
        try {
            return jpaRepository.findByEmail(email.value())
                .map(mapper::toDomain);
        } catch (Exception e) {
            throw new PersistenceException("Failed to find user by email", e);
        }
    }
    
    @Override
    public boolean existsByEmail(Email email) {
        try {
            return jpaRepository.existsByEmail(email.value());
        } catch (Exception e) {
            throw new PersistenceException("Failed to check user existence by email", e);
        }
    }
    
    @Override
    public List<User> findByIds(List<UserId> userIds) {
        try {
            var uuids = userIds.stream()
                .map(UserId::value)
                .collect(Collectors.toList());
            
            return jpaRepository.findByIdIn(uuids).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new PersistenceException("Failed to find users by IDs", e);
        }
    }
    
    @Override
    public List<User> findAllActive() {
        try {
            return jpaRepository.findByStatus("ACTIVE").stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new PersistenceException("Failed to find active users", e);
        }
    }
    
    @Override
    public List<User> searchByName(String namePattern) {
        try {
            return jpaRepository.searchByName(namePattern).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new PersistenceException("Failed to search users by name", e);
        }
    }
}