package com.zamaz.mcp.context.repository;

import com.zamaz.mcp.context.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Message entity operations.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    
    /**
     * Find messages by context ID.
     */
    List<Message> findByContextIdAndIsHiddenFalseOrderByTimestampAsc(UUID contextId);
    
    /**
     * Find messages by context ID with pagination.
     */
    Page<Message> findByContextIdAndIsHiddenFalse(UUID contextId, Pageable pageable);
    
    /**
     * Find recent messages for a context.
     */
    @Query("SELECT m FROM Message m WHERE m.context.id = :contextId AND m.isHidden = false " +
           "ORDER BY m.timestamp DESC")
    List<Message> findRecentMessages(@Param("contextId") UUID contextId, Pageable pageable);
    
    /**
     * Count tokens in a context.
     */
    @Query("SELECT SUM(m.tokenCount) FROM Message m WHERE m.context.id = :contextId AND m.isHidden = false")
    Integer countTokensInContext(@Param("contextId") UUID contextId);
    
    /**
     * Find messages within a time range.
     */
    @Query("SELECT m FROM Message m WHERE m.context.id = :contextId AND " +
           "m.timestamp BETWEEN :startTime AND :endTime AND m.isHidden = false " +
           "ORDER BY m.timestamp ASC")
    List<Message> findMessagesInTimeRange(@Param("contextId") UUID contextId,
                                         @Param("startTime") Instant startTime,
                                         @Param("endTime") Instant endTime);
    
    /**
     * Find messages by role.
     */
    List<Message> findByContextIdAndRoleAndIsHiddenFalse(UUID contextId, Message.MessageRole role);
    
    /**
     * Delete messages older than a specific date for a context.
     */
    void deleteByContextIdAndTimestampBefore(UUID contextId, Instant cutoffTime);
}