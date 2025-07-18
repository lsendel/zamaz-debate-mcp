package com.zamaz.mcp.controller.domain.port;

import com.zamaz.mcp.controller.domain.model.Debate;
import com.zamaz.mcp.controller.domain.model.DebateId;
import com.zamaz.mcp.controller.domain.model.DebateStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository port for debate persistence.
 */
public interface DebateRepository {
    
    /**
     * Save a debate.
     * 
     * @param debate the debate to save
     * @return the saved debate
     */
    Debate save(Debate debate);
    
    /**
     * Find a debate by ID.
     * 
     * @param debateId the debate ID
     * @return the debate if found
     */
    Optional<Debate> findById(DebateId debateId);
    
    /**
     * Find all debates.
     * 
     * @return all debates
     */
    List<Debate> findAll();
    
    /**
     * Find debates by status.
     * 
     * @param statuses the statuses to filter by
     * @return debates matching the statuses
     */
    List<Debate> findByStatuses(Set<DebateStatus> statuses);
    
    /**
     * Find debates by topic containing the given text.
     * 
     * @param topicFilter the text to search for in topics
     * @return debates with matching topics
     */
    List<Debate> findByTopicContaining(String topicFilter);
    
    /**
     * Find debates with pagination.
     * 
     * @param limit maximum number of results
     * @param offset offset for pagination
     * @return debates with pagination
     */
    List<Debate> findWithPagination(int limit, int offset);
    
    /**
     * Find debates by status and topic filter with pagination.
     * 
     * @param statuses the statuses to filter by
     * @param topicFilter the topic filter (optional)
     * @param limit maximum number of results (optional)
     * @param offset offset for pagination (optional)
     * @return debates matching the criteria
     */
    List<Debate> findByFilters(Set<DebateStatus> statuses, String topicFilter, Integer limit, Integer offset);
    
    /**
     * Count debates by status.
     * 
     * @param statuses the statuses to filter by
     * @return count of debates
     */
    long countByStatuses(Set<DebateStatus> statuses);
    
    /**
     * Delete a debate.
     * 
     * @param debateId the debate ID to delete
     * @return true if deleted, false if not found
     */
    boolean deleteById(DebateId debateId);
    
    /**
     * Check if a debate exists.
     * 
     * @param debateId the debate ID
     * @return true if exists, false otherwise
     */
    boolean existsById(DebateId debateId);
}