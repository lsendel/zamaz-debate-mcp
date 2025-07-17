package com.zamaz.mcp.github.repository;

import com.zamaz.mcp.github.entity.ReviewComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for review comment entities
 */
@Repository
public interface ReviewCommentRepository extends JpaRepository<ReviewComment, Long> {

    /**
     * Find comments by review ID
     */
    List<ReviewComment> findByReviewId(Long reviewId);

    /**
     * Find comments by comment type
     */
    List<ReviewComment> findByCommentType(String commentType);

    /**
     * Find comments by GitHub comment ID
     */
    List<ReviewComment> findByGitHubCommentId(Long gitHubCommentId);

    /**
     * Find comments created after a specific date
     */
    List<ReviewComment> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find comments by review and type
     */
    List<ReviewComment> findByReviewIdAndCommentType(Long reviewId, String commentType);

    /**
     * Find comments containing specific text
     */
    @Query("SELECT c FROM ReviewComment c WHERE c.body LIKE %:text%")
    List<ReviewComment> findByBodyContaining(@Param("text") String text);

    /**
     * Count comments by review
     */
    @Query("SELECT COUNT(c) FROM ReviewComment c WHERE c.review.id = :reviewId")
    Long countByReviewId(@Param("reviewId") Long reviewId);

    /**
     * Find comments by review status
     */
    @Query("SELECT c FROM ReviewComment c WHERE c.review.status = :status")
    List<ReviewComment> findByReviewStatus(@Param("status") String status);

    /**
     * Find latest comments for a review
     */
    @Query("SELECT c FROM ReviewComment c WHERE c.review.id = :reviewId ORDER BY c.createdAt DESC")
    List<ReviewComment> findLatestCommentsForReview(@Param("reviewId") Long reviewId);
}