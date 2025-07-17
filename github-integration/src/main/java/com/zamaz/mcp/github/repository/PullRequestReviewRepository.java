package com.zamaz.mcp.github.repository;

import com.zamaz.mcp.github.entity.PullRequestReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for pull request review entities
 */
@Repository
public interface PullRequestReviewRepository extends JpaRepository<PullRequestReview, Long> {

    /**
     * Find review by installation, repository, and PR number
     */
    Optional<PullRequestReview> findByInstallationIdAndRepositoryFullNameAndPrNumber(
        Long installationId, String repositoryFullName, Integer prNumber);

    /**
     * Find reviews by installation ID
     */
    List<PullRequestReview> findByInstallationId(Long installationId);

    /**
     * Find reviews by repository full name
     */
    List<PullRequestReview> findByRepositoryFullName(String repositoryFullName);

    /**
     * Find reviews by status
     */
    List<PullRequestReview> findByStatus(String status);

    /**
     * Find reviews by PR author
     */
    List<PullRequestReview> findByPrAuthor(String prAuthor);

    /**
     * Find reviews created after a specific date
     */
    List<PullRequestReview> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find reviews by status and installation
     */
    List<PullRequestReview> findByStatusAndInstallationId(String status, Long installationId);

    /**
     * Find pending reviews
     */
    @Query("SELECT r FROM PullRequestReview r WHERE r.status IN ('PENDING', 'ANALYZING')")
    List<PullRequestReview> findPendingReviews();

    /**
     * Find completed reviews in date range
     */
    @Query("SELECT r FROM PullRequestReview r WHERE r.status = 'COMPLETED' AND r.createdAt BETWEEN :startDate AND :endDate")
    List<PullRequestReview> findCompletedReviewsInDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);

    /**
     * Count reviews by status
     */
    @Query("SELECT COUNT(r) FROM PullRequestReview r WHERE r.status = :status")
    Long countByStatus(@Param("status") String status);

    /**
     * Find reviews by repository and status
     */
    List<PullRequestReview> findByRepositoryFullNameAndStatus(String repositoryFullName, String status);

    /**
     * Find latest review for a PR
     */
    @Query("SELECT r FROM PullRequestReview r WHERE r.repositoryFullName = :repo AND r.prNumber = :prNumber ORDER BY r.createdAt DESC")
    Optional<PullRequestReview> findLatestReviewForPr(@Param("repo") String repositoryFullName, @Param("prNumber") Integer prNumber);
}