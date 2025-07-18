package com.zamaz.mcp.controller.adapter.persistence;

import com.zamaz.mcp.controller.adapter.persistence.entity.DebateEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Spring Data JPA repository for DebateEntity.
 */
@Repository
public interface SpringDataDebateRepository extends JpaRepository<DebateEntity, UUID> {
    
    /**
     * Find debates by status.
     */
    List<DebateEntity> findByStatusIn(Set<String> statuses);
    
    /**
     * Find debates by status with pagination.
     */
    Page<DebateEntity> findByStatusIn(Set<String> statuses, Pageable pageable);
    
    /**
     * Find debates by topic containing text (case insensitive).
     */
    List<DebateEntity> findByTopicContainingIgnoreCase(String topicFilter);
    
    /**
     * Find debates by status and topic containing text.
     */
    List<DebateEntity> findByStatusInAndTopicContainingIgnoreCase(Set<String> statuses, String topicFilter);
    
    /**
     * Find debates by status and topic containing text with pagination.
     */
    Page<DebateEntity> findByStatusInAndTopicContainingIgnoreCase(
        Set<String> statuses, String topicFilter, Pageable pageable
    );
    
    /**
     * Count debates by status.
     */
    long countByStatusIn(Set<String> statuses);
    
    /**
     * Find debates ordered by creation date (most recent first).
     */
    @Query("SELECT d FROM DebateEntity d ORDER BY d.createdAt DESC")
    List<DebateEntity> findAllOrderByCreatedAtDesc();
    
    /**
     * Find active debates (in progress or initialized).
     */
    @Query("SELECT d FROM DebateEntity d WHERE d.status IN ('in_progress', 'initialized') ORDER BY d.createdAt DESC")
    List<DebateEntity> findActiveDebates();
    
    /**
     * Find completed debates (completed or archived).
     */
    @Query("SELECT d FROM DebateEntity d WHERE d.status IN ('completed', 'archived') ORDER BY d.completedAt DESC")
    List<DebateEntity> findCompletedDebates();
    
    /**
     * Find debates by participant name.
     */
    @Query("SELECT DISTINCT d FROM DebateEntity d JOIN d.participants p WHERE p.name LIKE %:participantName%")
    List<DebateEntity> findByParticipantNameContaining(@Param("participantName") String participantName);
    
    /**
     * Find debates with minimum number of participants.
     */
    @Query("SELECT d FROM DebateEntity d WHERE SIZE(d.participants) >= :minParticipants")
    List<DebateEntity> findWithMinParticipants(@Param("minParticipants") int minParticipants);
    
    /**
     * Find debates created in the last N days.
     */
    @Query("SELECT d FROM DebateEntity d WHERE d.createdAt >= CURRENT_DATE - :days")
    List<DebateEntity> findRecentDebates(@Param("days") int days);
}