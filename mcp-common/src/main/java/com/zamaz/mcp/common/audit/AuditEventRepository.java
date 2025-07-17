package com.zamaz.mcp.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for audit events with custom queries
 */
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, String> {
    
    /**
     * Find audit events by organization and time range
     */
    Page<AuditEvent> findByOrganizationIdAndTimestampBetweenOrderByTimestampDesc(
        String organizationId, LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    /**
     * Find audit events by resource
     */
    Page<AuditEvent> findByResourceTypeAndResourceIdOrderByTimestampDesc(
        String resourceType, String resourceId, Pageable pageable);
    
    /**
     * Find audit events by user
     */
    Page<AuditEvent> findByUserIdAndOrganizationIdOrderByTimestampDesc(
        String userId, String organizationId, Pageable pageable);
    
    /**
     * Find audit events by event type
     */
    Page<AuditEvent> findByEventTypeAndOrganizationIdOrderByTimestampDesc(
        AuditEvent.AuditEventType eventType, String organizationId, Pageable pageable);
    
    /**
     * Find failed audit events
     */
    Page<AuditEvent> findByResultAndOrganizationIdOrderByTimestampDesc(
        AuditEvent.AuditResult result, String organizationId, Pageable pageable);
    
    /**
     * Find high-risk audit events
     */
    Page<AuditEvent> findByRiskLevelInAndOrganizationIdOrderByTimestampDesc(
        List<AuditEvent.RiskLevel> riskLevels, String organizationId, Pageable pageable);
    
    /**
     * Find audit events by IP address
     */
    List<AuditEvent> findBySourceIpAndTimestampAfterOrderByTimestampDesc(
        String sourceIp, LocalDateTime after);
    
    /**
     * Find suspicious activities
     */
    @Query("SELECT ae FROM AuditEvent ae WHERE ae.organizationId = :orgId " +
           "AND ae.timestamp >= :since " +
           "AND (ae.riskLevel IN ('HIGH', 'CRITICAL') " +
           "OR ae.result = 'FAILURE' " +
           "OR ae.action IN ('LOGIN_FAILED', 'ACCESS_DENIED', 'SUSPICIOUS_ACTIVITY')) " +
           "ORDER BY ae.timestamp DESC")
    List<AuditEvent> findSuspiciousActivities(@Param("orgId") String organizationId, 
                                            @Param("since") LocalDateTime since);
    
    /**
     * Get audit statistics
     */
    @Query("SELECT new com.zamaz.mcp.common.audit.AuditStatistics(" +
           "COUNT(ae), " +
           "SUM(CASE WHEN ae.result = 'SUCCESS' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN ae.result = 'FAILURE' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN ae.riskLevel IN ('HIGH', 'CRITICAL') THEN 1 ELSE 0 END), " +
           "COUNT(DISTINCT ae.userId), " +
           "COUNT(DISTINCT ae.sourceIp)) " +
           "FROM AuditEvent ae " +
           "WHERE ae.organizationId = :orgId " +
           "AND ae.timestamp BETWEEN :from AND :to")
    AuditStatistics getStatistics(@Param("orgId") String organizationId, 
                                @Param("from") LocalDateTime from, 
                                @Param("to") LocalDateTime to);
    
    /**
     * Complex search using criteria
     */
    @Query("SELECT ae FROM AuditEvent ae WHERE " +
           "(:#{#criteria.organizationId} IS NULL OR ae.organizationId = :#{#criteria.organizationId}) " +
           "AND (:#{#criteria.userId} IS NULL OR ae.userId = :#{#criteria.userId}) " +
           "AND (:#{#criteria.eventType} IS NULL OR ae.eventType = :#{#criteria.eventType}) " +
           "AND (:#{#criteria.action} IS NULL OR ae.action = :#{#criteria.action}) " +
           "AND (:#{#criteria.resourceType} IS NULL OR ae.resourceType = :#{#criteria.resourceType}) " +
           "AND (:#{#criteria.resourceId} IS NULL OR ae.resourceId = :#{#criteria.resourceId}) " +
           "AND (:#{#criteria.result} IS NULL OR ae.result = :#{#criteria.result}) " +
           "AND (:#{#criteria.riskLevel} IS NULL OR ae.riskLevel = :#{#criteria.riskLevel}) " +
           "AND (:#{#criteria.sourceIp} IS NULL OR ae.sourceIp = :#{#criteria.sourceIp}) " +
           "AND (:#{#criteria.fromDate} IS NULL OR ae.timestamp >= :#{#criteria.fromDate}) " +
           "AND (:#{#criteria.toDate} IS NULL OR ae.timestamp <= :#{#criteria.toDate}) " +
           "AND (:#{#criteria.searchTerm} IS NULL OR " +
           "     LOWER(ae.description) LIKE LOWER(CONCAT('%', :#{#criteria.searchTerm}, '%')) OR " +
           "     LOWER(ae.resourceName) LIKE LOWER(CONCAT('%', :#{#criteria.searchTerm}, '%')) OR " +
           "     LOWER(ae.errorMessage) LIKE LOWER(CONCAT('%', :#{#criteria.searchTerm}, '%'))) " +
           "ORDER BY ae.timestamp DESC")
    Page<AuditEvent> findByCriteria(@Param("criteria") AuditSearchCriteria criteria, Pageable pageable);
    
    /**
     * Delete old audit events
     */
    @Modifying
    @Query("DELETE FROM AuditEvent ae WHERE ae.timestamp < :before")
    int deleteByTimestampBefore(@Param("before") LocalDateTime before);
    
    /**
     * Count events by organization in time range
     */
    @Query("SELECT COUNT(ae) FROM AuditEvent ae " +
           "WHERE ae.organizationId = :orgId " +
           "AND ae.timestamp >= :since")
    long countByOrganizationSince(@Param("orgId") String organizationId, 
                                 @Param("since") LocalDateTime since);
    
    /**
     * Find recent failed login attempts
     */
    @Query("SELECT ae FROM AuditEvent ae " +
           "WHERE ae.action = 'LOGIN_FAILED' " +
           "AND ae.sourceIp = :ip " +
           "AND ae.timestamp >= :since " +
           "ORDER BY ae.timestamp DESC")
    List<AuditEvent> findRecentFailedLogins(@Param("ip") String sourceIp, 
                                          @Param("since") LocalDateTime since);
    
    /**
     * Find events with compliance tags
     */
    @Query("SELECT ae FROM AuditEvent ae " +
           "JOIN ae.complianceTags ct " +
           "WHERE ct IN :tags " +
           "AND ae.organizationId = :orgId " +
           "AND ae.timestamp BETWEEN :from AND :to " +
           "ORDER BY ae.timestamp DESC")
    Page<AuditEvent> findByComplianceTags(@Param("tags") List<String> tags,
                                        @Param("orgId") String organizationId,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to,
                                        Pageable pageable);
}