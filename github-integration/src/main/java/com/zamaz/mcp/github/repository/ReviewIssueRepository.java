package com.zamaz.mcp.github.repository;

import com.zamaz.mcp.github.entity.ReviewIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for review issue entities
 */
@Repository
public interface ReviewIssueRepository extends JpaRepository<ReviewIssue, Long> {

    /**
     * Find issues by review ID
     */
    List<ReviewIssue> findByReviewId(Long reviewId);

    /**
     * Find issues by issue type
     */
    List<ReviewIssue> findByIssueType(String issueType);

    /**
     * Find issues by severity
     */
    List<ReviewIssue> findBySeverity(String severity);

    /**
     * Find issues by status
     */
    List<ReviewIssue> findByStatus(String status);

    /**
     * Find issues by review and severity
     */
    List<ReviewIssue> findByReviewIdAndSeverity(Long reviewId, String severity);

    /**
     * Find issues by review and status
     */
    List<ReviewIssue> findByReviewIdAndStatus(Long reviewId, String status);

    /**
     * Find open issues
     */
    @Query("SELECT i FROM ReviewIssue i WHERE i.status = 'OPEN'")
    List<ReviewIssue> findOpenIssues();

    /**
     * Find high severity issues
     */
    @Query("SELECT i FROM ReviewIssue i WHERE i.severity = 'HIGH'")
    List<ReviewIssue> findHighSeverityIssues();

    /**
     * Find issues created after a specific date
     */
    List<ReviewIssue> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Count issues by severity
     */
    @Query("SELECT COUNT(i) FROM ReviewIssue i WHERE i.severity = :severity")
    Long countBySeverity(@Param("severity") String severity);

    /**
     * Count issues by status
     */
    @Query("SELECT COUNT(i) FROM ReviewIssue i WHERE i.status = :status")
    Long countByStatus(@Param("status") String status);

    /**
     * Find issues by review status
     */
    @Query("SELECT i FROM ReviewIssue i WHERE i.review.status = :status")
    List<ReviewIssue> findByReviewStatus(@Param("status") String status);

    /**
     * Find issues by repository
     */
    @Query("SELECT i FROM ReviewIssue i WHERE i.review.repositoryFullName = :repo")
    List<ReviewIssue> findByRepository(@Param("repo") String repositoryFullName);

    /**
     * Find issues by type and severity
     */
    List<ReviewIssue> findByIssueTypeAndSeverity(String issueType, String severity);
}