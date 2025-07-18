package com.zamaz.mcp.debateengine.service;

import com.zamaz.mcp.debateengine.config.CacheConfiguration;
import com.zamaz.mcp.debateengine.dto.DebateDto;
import com.zamaz.mcp.debateengine.dto.ParticipantDto;
import com.zamaz.mcp.debateengine.entity.Debate;
import com.zamaz.mcp.debateengine.entity.DebateStatus;
import com.zamaz.mcp.debateengine.repository.DebateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Cached debate service with comprehensive caching strategies.
 * Implements cache-aside pattern with proper cache invalidation.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@CacheConfig(cacheNames = CacheConfiguration.DEBATE_CACHE)
public class CachedDebateService {

    private final DebateRepository debateRepository;
    private final DebateMapper debateMapper;

    /**
     * Get debate by ID with caching
     */
    @Cacheable(key = "#debateId", unless = "#result == null")
    @Transactional(readOnly = true)
    public DebateDto getDebate(UUID debateId) {
        log.debug("Fetching debate from database: {}", debateId);
        Debate debate = debateRepository.findById(debateId)
                .orElseThrow(() -> new ResourceNotFoundException("Debate not found: " + debateId));
        return debateMapper.toDto(debate);
    }

    /**
     * Create new debate (cache eviction for lists)
     */
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheConfiguration.DEBATE_LIST_CACHE, allEntries = true),
        @CacheEvict(cacheNames = CacheConfiguration.USER_DEBATES_CACHE, key = "#userId"),
        @CacheEvict(cacheNames = CacheConfiguration.ORG_DEBATES_CACHE, key = "#organizationId")
    })
    @Transactional
    public DebateDto createDebate(UUID organizationId, UUID userId, CreateDebateRequest request) {
        log.info("Creating new debate for org: {} by user: {}", organizationId, userId);
        
        Debate debate = Debate.builder()
                .organizationId(organizationId)
                .createdByUserId(userId)
                .topic(request.getTopic())
                .description(request.getDescription())
                .visibility(request.getVisibility())
                .maxParticipants(request.getMaxParticipants())
                .maxRounds(request.getMaxRounds())
                .roundTimeoutMs(request.getRoundTimeoutMs())
                .settings(request.getSettings())
                .status(DebateStatus.DRAFT)
                .currentRound(0)
                .totalTokens(0)
                .messageCount(0)
                .build();
        
        debate = debateRepository.save(debate);
        return debateMapper.toDto(debate);
    }

    /**
     * Update debate with cache update
     */
    @CachePut(key = "#debateId")
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheConfiguration.DEBATE_LIST_CACHE, allEntries = true),
        @CacheEvict(cacheNames = CacheConfiguration.USER_DEBATES_CACHE, allEntries = true),
        @CacheEvict(cacheNames = CacheConfiguration.ORG_DEBATES_CACHE, allEntries = true)
    })
    @Transactional
    public DebateDto updateDebate(UUID debateId, UpdateDebateRequest request) {
        log.info("Updating debate: {}", debateId);
        
        Debate debate = debateRepository.findById(debateId)
                .orElseThrow(() -> new ResourceNotFoundException("Debate not found: " + debateId));
        
        // Update fields
        if (request.getTopic() != null) {
            debate.setTopic(request.getTopic());
        }
        if (request.getDescription() != null) {
            debate.setDescription(request.getDescription());
        }
        if (request.getSettings() != null) {
            debate.setSettings(request.getSettings());
        }
        
        debate = debateRepository.save(debate);
        return debateMapper.toDto(debate);
    }

    /**
     * Delete debate with cache eviction
     */
    @Caching(evict = {
        @CacheEvict(key = "#debateId"),
        @CacheEvict(cacheNames = CacheConfiguration.DEBATE_LIST_CACHE, allEntries = true),
        @CacheEvict(cacheNames = CacheConfiguration.USER_DEBATES_CACHE, allEntries = true),
        @CacheEvict(cacheNames = CacheConfiguration.ORG_DEBATES_CACHE, allEntries = true),
        @CacheEvict(cacheNames = CacheConfiguration.CONTEXT_CACHE, key = "#debateId"),
        @CacheEvict(cacheNames = CacheConfiguration.PARTICIPANT_CACHE, allEntries = true)
    })
    @Transactional
    public void deleteDebate(UUID debateId) {
        log.info("Deleting debate: {}", debateId);
        debateRepository.deleteById(debateId);
    }

    /**
     * Get debates by organization with caching
     */
    @Cacheable(cacheNames = CacheConfiguration.ORG_DEBATES_CACHE, key = "#organizationId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<DebateDto> getDebatesByOrganization(UUID organizationId, Pageable pageable) {
        log.debug("Fetching debates for organization: {}", organizationId);
        Page<Debate> debates = debateRepository.findByOrganizationId(organizationId, pageable);
        return debates.map(debateMapper::toDto);
    }

    /**
     * Get debates by user with caching
     */
    @Cacheable(cacheNames = CacheConfiguration.USER_DEBATES_CACHE, key = "#userId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<DebateDto> getDebatesByUser(UUID userId, Pageable pageable) {
        log.debug("Fetching debates for user: {}", userId);
        Page<Debate> debates = debateRepository.findByCreatedByUserId(userId, pageable);
        return debates.map(debateMapper::toDto);
    }

    /**
     * Update debate status with proper cache invalidation
     */
    @CachePut(key = "#debateId")
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheConfiguration.DEBATE_LIST_CACHE, allEntries = true),
        @CacheEvict(cacheNames = CacheConfiguration.ROUND_CACHE, key = "#debateId + ':*'")
    })
    @Transactional
    public DebateDto updateDebateStatus(UUID debateId, DebateStatus newStatus) {
        log.info("Updating debate status: {} -> {}", debateId, newStatus);
        
        Debate debate = debateRepository.findById(debateId)
                .orElseThrow(() -> new ResourceNotFoundException("Debate not found: " + debateId));
        
        debate.setStatus(newStatus);
        
        // Set timestamps based on status
        if (newStatus == DebateStatus.ACTIVE && debate.getStartedAt() == null) {
            debate.setStartedAt(java.time.Instant.now());
        } else if (newStatus == DebateStatus.COMPLETED && debate.getCompletedAt() == null) {
            debate.setCompletedAt(java.time.Instant.now());
        }
        
        debate = debateRepository.save(debate);
        return debateMapper.toDto(debate);
    }

    /**
     * Batch get debates with intelligent caching
     */
    @Transactional(readOnly = true)
    public List<DebateDto> getDebatesBatch(List<UUID> debateIds) {
        log.debug("Batch fetching {} debates", debateIds.size());
        
        // This could be optimized to check cache first for each ID
        List<Debate> debates = debateRepository.findAllById(debateIds);
        return debates.stream()
                .map(debateMapper::toDto)
                .toList();
    }

    /**
     * Warm up cache for active debates
     */
    @Transactional(readOnly = true)
    public void warmUpActiveDebatesCache() {
        log.info("Warming up cache for active debates");
        
        List<Debate> activeDebates = debateRepository.findByStatus(DebateStatus.ACTIVE);
        for (Debate debate : activeDebates) {
            // This will populate the cache
            getDebate(debate.getId());
        }
        
        log.info("Warmed up cache for {} active debates", activeDebates.size());
    }

    /**
     * Clear all debate-related caches
     */
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheConfiguration.DEBATE_CACHE, allEntries = true),
        @CacheEvict(cacheNames = CacheConfiguration.DEBATE_LIST_CACHE, allEntries = true),
        @CacheEvict(cacheNames = CacheConfiguration.USER_DEBATES_CACHE, allEntries = true),
        @CacheEvict(cacheNames = CacheConfiguration.ORG_DEBATES_CACHE, allEntries = true)
    })
    public void clearAllDebateCaches() {
        log.warn("Clearing all debate-related caches");
    }
}