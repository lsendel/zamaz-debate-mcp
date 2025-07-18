package com.zamaz.mcp.controller.adapter.persistence;

import com.zamaz.mcp.controller.adapter.persistence.entity.DebateEntity;
import com.zamaz.mcp.controller.adapter.persistence.mapper.DebateEntityMapper;
import com.zamaz.mcp.controller.domain.model.Debate;
import com.zamaz.mcp.controller.domain.model.DebateId;
import com.zamaz.mcp.controller.domain.model.DebateStatus;
import com.zamaz.mcp.controller.domain.port.DebateRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * JPA implementation of DebateRepository.
 */
@Repository
@Transactional
public class JpaDebateRepository implements DebateRepository {
    
    private final SpringDataDebateRepository springRepository;
    private final DebateEntityMapper mapper;
    
    public JpaDebateRepository(SpringDataDebateRepository springRepository, DebateEntityMapper mapper) {
        this.springRepository = Objects.requireNonNull(springRepository);
        this.mapper = Objects.requireNonNull(mapper);
    }
    
    @Override
    public Debate save(Debate debate) {
        Objects.requireNonNull(debate, "Debate cannot be null");
        
        DebateEntity entity = mapper.toEntity(debate);
        DebateEntity saved = springRepository.save(entity);
        return mapper.toDomain(saved);
    }
    
    @Override
    public Optional<Debate> findById(DebateId debateId) {
        Objects.requireNonNull(debateId, "Debate ID cannot be null");
        
        return springRepository.findById(debateId.value())
            .map(mapper::toDomain);
    }
    
    @Override
    public List<Debate> findAll() {
        return springRepository.findAll().stream()
            .map(mapper::toDomain)
            .toList();
    }
    
    @Override
    public List<Debate> findByStatuses(Set<DebateStatus> statuses) {
        Objects.requireNonNull(statuses, "Statuses cannot be null");
        
        if (statuses.isEmpty()) {
            return List.of();
        }
        
        Set<String> statusValues = statuses.stream()
            .map(DebateStatus::getValue)
            .collect(java.util.stream.Collectors.toSet());
        
        return springRepository.findByStatusIn(statusValues).stream()
            .map(mapper::toDomain)
            .toList();
    }
    
    @Override
    public List<Debate> findByTopicContaining(String topicFilter) {
        Objects.requireNonNull(topicFilter, "Topic filter cannot be null");
        
        return springRepository.findByTopicContainingIgnoreCase(topicFilter).stream()
            .map(mapper::toDomain)
            .toList();
    }
    
    @Override
    public List<Debate> findWithPagination(int limit, int offset) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }
        
        Pageable pageable = PageRequest.of(offset / limit, limit);
        return springRepository.findAll(pageable).stream()
            .map(mapper::toDomain)
            .toList();
    }
    
    @Override
    public List<Debate> findByFilters(Set<DebateStatus> statuses, String topicFilter, Integer limit, Integer offset) {
        Objects.requireNonNull(statuses, "Statuses cannot be null");
        
        Set<String> statusValues = statuses.stream()
            .map(DebateStatus::getValue)
            .collect(java.util.stream.Collectors.toSet());
        
        // Build query based on provided filters
        List<DebateEntity> entities;
        
        if (topicFilter != null && !topicFilter.trim().isEmpty()) {
            if (limit != null && offset != null) {
                Pageable pageable = PageRequest.of(offset / limit, limit);
                entities = springRepository.findByStatusInAndTopicContainingIgnoreCase(
                    statusValues, topicFilter.trim(), pageable
                ).getContent();
            } else {
                entities = springRepository.findByStatusInAndTopicContainingIgnoreCase(
                    statusValues, topicFilter.trim()
                );
            }
        } else {
            if (limit != null && offset != null) {
                Pageable pageable = PageRequest.of(offset / limit, limit);
                entities = springRepository.findByStatusIn(statusValues, pageable).getContent();
            } else {
                entities = springRepository.findByStatusIn(statusValues);
            }
        }
        
        return entities.stream()
            .map(mapper::toDomain)
            .toList();
    }
    
    @Override
    public long countByStatuses(Set<DebateStatus> statuses) {
        Objects.requireNonNull(statuses, "Statuses cannot be null");
        
        if (statuses.isEmpty()) {
            return 0;
        }
        
        Set<String> statusValues = statuses.stream()
            .map(DebateStatus::getValue)
            .collect(java.util.stream.Collectors.toSet());
        
        return springRepository.countByStatusIn(statusValues);
    }
    
    @Override
    public boolean deleteById(DebateId debateId) {
        Objects.requireNonNull(debateId, "Debate ID cannot be null");
        
        if (springRepository.existsById(debateId.value())) {
            springRepository.deleteById(debateId.value());
            return true;
        }
        return false;
    }
    
    @Override
    public boolean existsById(DebateId debateId) {
        Objects.requireNonNull(debateId, "Debate ID cannot be null");
        
        return springRepository.existsById(debateId.value());
    }
}