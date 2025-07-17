package com.zamaz.mcp.github.service;

import com.zamaz.mcp.github.entity.KnowledgeBaseEntry;
import com.zamaz.mcp.github.entity.DeveloperProfile;
import com.zamaz.mcp.github.entity.PullRequestReview;
import com.zamaz.mcp.github.entity.ReviewIssue;
import com.zamaz.mcp.github.entity.ReviewComment;
import com.zamaz.mcp.github.repository.KnowledgeBaseEntryRepository;
import com.zamaz.mcp.github.repository.DeveloperProfileRepository;
import com.zamaz.mcp.github.repository.PullRequestReviewRepository;
import com.zamaz.mcp.github.repository.ReviewIssueRepository;
import com.zamaz.mcp.github.repository.ReviewCommentRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for building and managing team knowledge base from past reviews and feedback
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TeamKnowledgeBaseService {

    private final KnowledgeBaseEntryRepository knowledgeBaseRepository;
    private final DeveloperProfileRepository developerProfileRepository;
    private final PullRequestReviewRepository pullRequestReviewRepository;
    private final ReviewIssueRepository reviewIssueRepository;
    private final ReviewCommentRepository reviewCommentRepository;

    /**
     * Extract knowledge from completed reviews and add to knowledge base
     */
    @Transactional
    public void extractKnowledgeFromReviews(Long repositoryId, LocalDateTime fromDate) {
        log.info("Extracting knowledge from reviews for repository {} since {}", repositoryId, fromDate);

        List<PullRequestReview> reviews = pullRequestReviewRepository
            .findByRepositoryIdAndCompletedAtAfter(repositoryId, fromDate);

        for (PullRequestReview review : reviews) {
            try {
                extractKnowledgeFromReview(review);
            } catch (Exception e) {
                log.error("Error extracting knowledge from review {}: {}", review.getId(), e.getMessage(), e);
            }
        }

        log.info("Completed knowledge extraction for repository {}", repositoryId);
    }

    /**
     * Extract knowledge from a specific review
     */
    @Transactional
    public void extractKnowledgeFromReview(PullRequestReview review) {
        log.info("Extracting knowledge from review {}", review.getId());

        // Extract knowledge from review issues
        List<ReviewIssue> issues = reviewIssueRepository.findByReviewId(review.getId());
        for (ReviewIssue issue : issues) {
            extractKnowledgeFromIssue(issue, review);
        }

        // Extract knowledge from review comments
        List<ReviewComment> comments = reviewCommentRepository.findByReviewId(review.getId());
        for (ReviewComment comment : comments) {
            extractKnowledgeFromComment(comment, review);
        }

        // Extract patterns from review as a whole
        extractReviewPatterns(review);
    }

    /**
     * Search knowledge base entries
     */
    @Transactional(readOnly = true)
    public Page<KnowledgeBaseEntry> searchKnowledgeBase(KnowledgeSearchCriteria criteria, Pageable pageable) {
        log.info("Searching knowledge base with criteria: {}", criteria);

        if (criteria.getRepositoryId() != null) {
            if (criteria.getCategory() != null) {
                return knowledgeBaseRepository.findByRepositoryIdAndCategory(
                    criteria.getRepositoryId(), criteria.getCategory(), pageable);
            } else {
                return knowledgeBaseRepository.findByRepositoryId(criteria.getRepositoryId(), pageable);
            }
        } else if (criteria.getCategory() != null) {
            return knowledgeBaseRepository.findByCategory(criteria.getCategory(), pageable);
        } else if (criteria.getSearchTerm() != null) {
            return knowledgeBaseRepository.searchByTitleOrContent(criteria.getSearchTerm(), pageable);
        }

        return knowledgeBaseRepository.findAll(pageable);
    }

    /**
     * Get knowledge base recommendations for a specific context
     */
    @Transactional(readOnly = true)
    public List<KnowledgeRecommendation> getRecommendations(Long repositoryId, String context, 
                                                           List<String> technologies, Long developerId) {
        log.info("Getting knowledge recommendations for repository {} with context: {}", repositoryId, context);

        List<KnowledgeRecommendation> recommendations = new ArrayList<>();

        // Get relevant knowledge entries
        List<KnowledgeBaseEntry> entries = knowledgeBaseRepository.findByRepositoryIdAndIsApproved(repositoryId, true);

        // Filter and score entries based on context
        for (KnowledgeBaseEntry entry : entries) {
            BigDecimal relevanceScore = calculateRelevanceScore(entry, context, technologies);
            
            if (relevanceScore.compareTo(BigDecimal.valueOf(0.3)) > 0) {
                recommendations.add(KnowledgeRecommendation.builder()
                    .entry(entry)
                    .relevanceScore(relevanceScore)
                    .context(context)
                    .reasoning(generateReasoningForRecommendation(entry, context))
                    .build());
            }
        }

        // Sort by relevance score
        recommendations.sort(Comparator.comparing(KnowledgeRecommendation::getRelevanceScore).reversed());

        // Add personalized recommendations based on developer profile
        if (developerId != null) {
            recommendations.addAll(getPersonalizedRecommendations(developerId, context, technologies));
        }

        return recommendations.stream().limit(10).collect(Collectors.toList());
    }

    /**
     * Create a new knowledge base entry
     */
    @Transactional
    public KnowledgeBaseEntry createKnowledgeEntry(KnowledgeEntryRequest request) {
        log.info("Creating new knowledge base entry: {}", request.getTitle());

        KnowledgeBaseEntry entry = KnowledgeBaseEntry.builder()
            .repositoryId(request.getRepositoryId())
            .category(request.getCategory())
            .title(request.getTitle())
            .description(request.getDescription())
            .content(request.getContent())
            .tags(request.getTags())
            .severity(request.getSeverity())
            .frequencyCount(1)
            .effectivenessScore(BigDecimal.valueOf(50)) // Default score
            .sourceReviewIds(request.getSourceReviewIds())
            .createdByUserId(request.getCreatedByUserId())
            .language(request.getLanguage())
            .framework(request.getFramework())
            .isApproved(false)
            .build();

        return knowledgeBaseRepository.save(entry);
    }

    /**
     * Update effectiveness score based on usage feedback
     */
    @Transactional
    public void updateEffectivenessScore(Long entryId, BigDecimal newScore, String feedback) {
        log.info("Updating effectiveness score for entry {}: {}", entryId, newScore);

        Optional<KnowledgeBaseEntry> entryOpt = knowledgeBaseRepository.findById(entryId);
        if (entryOpt.isPresent()) {
            KnowledgeBaseEntry entry = entryOpt.get();
            
            // Calculate weighted average of existing and new score
            BigDecimal currentScore = entry.getEffectivenessScore() != null ? 
                entry.getEffectivenessScore() : BigDecimal.valueOf(50);
            
            BigDecimal weightedScore = currentScore.multiply(BigDecimal.valueOf(0.7))
                .add(newScore.multiply(BigDecimal.valueOf(0.3)));
            
            entry.setEffectivenessScore(weightedScore);
            knowledgeBaseRepository.save(entry);
        }
    }

    /**
     * Approve a knowledge base entry
     */
    @Transactional
    public void approveKnowledgeEntry(Long entryId, Long approvedByUserId) {
        log.info("Approving knowledge base entry {} by user {}", entryId, approvedByUserId);

        Optional<KnowledgeBaseEntry> entryOpt = knowledgeBaseRepository.findById(entryId);
        if (entryOpt.isPresent()) {
            KnowledgeBaseEntry entry = entryOpt.get();
            entry.setIsApproved(true);
            entry.setApprovalDate(LocalDateTime.now());
            entry.setApprovedByUserId(approvedByUserId);
            knowledgeBaseRepository.save(entry);
        }
    }

    /**
     * Get knowledge base analytics
     */
    @Transactional(readOnly = true)
    public KnowledgeBaseAnalytics getKnowledgeBaseAnalytics(Long repositoryId) {
        log.info("Generating knowledge base analytics for repository {}", repositoryId);

        List<KnowledgeBaseEntry> entries = knowledgeBaseRepository.findByRepositoryId(repositoryId);
        
        Map<String, Long> categoryDistribution = entries.stream()
            .collect(Collectors.groupingBy(KnowledgeBaseEntry::getCategory, Collectors.counting()));
        
        Map<String, Long> severityDistribution = entries.stream()
            .filter(entry -> entry.getSeverity() != null)
            .collect(Collectors.groupingBy(entry -> entry.getSeverity().getValue(), Collectors.counting()));
        
        Map<String, Long> languageDistribution = entries.stream()
            .filter(entry -> entry.getLanguage() != null)
            .collect(Collectors.groupingBy(KnowledgeBaseEntry::getLanguage, Collectors.counting()));

        BigDecimal averageEffectiveness = entries.stream()
            .filter(entry -> entry.getEffectivenessScore() != null)
            .map(KnowledgeBaseEntry::getEffectivenessScore)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(entries.size()), 2, RoundingMode.HALF_UP);

        return KnowledgeBaseAnalytics.builder()
            .repositoryId(repositoryId)
            .totalEntries(entries.size())
            .approvedEntries((int) entries.stream().filter(e -> Boolean.TRUE.equals(e.getIsApproved())).count())
            .pendingEntries((int) entries.stream().filter(e -> Boolean.FALSE.equals(e.getIsApproved())).count())
            .categoryDistribution(categoryDistribution)
            .severityDistribution(severityDistribution)
            .languageDistribution(languageDistribution)
            .averageEffectivenessScore(averageEffectiveness)
            .mostFrequentPatterns(getMostFrequentPatterns(entries))
            .topContributors(getTopContributors(entries))
            .recentActivity(getRecentActivity(entries))
            .build();
    }

    /**
     * Get similar knowledge entries
     */
    @Transactional(readOnly = true)
    public List<KnowledgeBaseEntry> getSimilarEntries(Long entryId, int limit) {
        log.info("Finding similar entries to entry {}", entryId);

        Optional<KnowledgeBaseEntry> entryOpt = knowledgeBaseRepository.findById(entryId);
        if (entryOpt.isEmpty()) {
            return Collections.emptyList();
        }

        KnowledgeBaseEntry entry = entryOpt.get();
        
        // Find entries with similar tags, category, or language
        List<KnowledgeBaseEntry> similarEntries = new ArrayList<>();
        
        // Find by same category
        similarEntries.addAll(knowledgeBaseRepository.findByRepositoryIdAndCategory(
            entry.getRepositoryId(), entry.getCategory()));
        
        // Find by similar tags
        if (entry.getTags() != null && !entry.getTags().isEmpty()) {
            for (String tag : entry.getTags()) {
                similarEntries.addAll(knowledgeBaseRepository.findByTag(tag));
            }
        }
        
        // Find by same language
        if (entry.getLanguage() != null) {
            similarEntries.addAll(knowledgeBaseRepository.findByLanguage(entry.getLanguage()));
        }

        // Remove duplicates and the original entry
        return similarEntries.stream()
            .filter(e -> !e.getId().equals(entryId))
            .distinct()
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Scheduled task to extract knowledge from recent reviews
     */
    @Scheduled(cron = "0 0 1 * * ?") // Run daily at 1 AM
    @Async
    public void scheduleKnowledgeExtraction() {
        log.info("Starting scheduled knowledge extraction");

        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        
        // Get all repositories with recent reviews
        List<Long> repositoryIds = pullRequestReviewRepository.findDistinctRepositoryIdsWithRecentReviews(yesterday);
        
        for (Long repositoryId : repositoryIds) {
            try {
                extractKnowledgeFromReviews(repositoryId, yesterday);
            } catch (Exception e) {
                log.error("Error in scheduled knowledge extraction for repository {}: {}", 
                         repositoryId, e.getMessage(), e);
            }
        }
        
        log.info("Completed scheduled knowledge extraction for {} repositories", repositoryIds.size());
    }

    // Private helper methods

    private void extractKnowledgeFromIssue(ReviewIssue issue, PullRequestReview review) {
        // Extract patterns from common issues
        if (isCommonIssue(issue)) {
            createKnowledgeEntryFromIssue(issue, review);
        }
    }

    private void extractKnowledgeFromComment(ReviewComment comment, PullRequestReview review) {
        // Extract knowledge from helpful comments
        if (isKnowledgeWorthyComment(comment)) {
            createKnowledgeEntryFromComment(comment, review);
        }
    }

    private void extractReviewPatterns(PullRequestReview review) {
        // Extract patterns from review as a whole
        if (hasImportantPatterns(review)) {
            createKnowledgeEntryFromReview(review);
        }
    }

    private boolean isCommonIssue(ReviewIssue issue) {
        // Check if this type of issue appears frequently
        long count = reviewIssueRepository.countByIssueTypeAndSeverity(issue.getIssueType(), issue.getSeverity());
        return count > 5; // Threshold for common issues
    }

    private boolean isKnowledgeWorthyComment(ReviewComment comment) {
        // Analyze comment for knowledge value
        String content = comment.getContent();
        return content.length() > 50 && // Substantial content
               (content.contains("best practice") || 
                content.contains("pattern") || 
                content.contains("recommendation") ||
                content.contains("consider") ||
                content.contains("should"));
    }

    private boolean hasImportantPatterns(PullRequestReview review) {
        // Check if review reveals important patterns
        return review.getCriticalIssues() > 0 || 
               review.getMajorIssues() > 2 ||
               review.getSuggestions() > 5;
    }

    private void createKnowledgeEntryFromIssue(ReviewIssue issue, PullRequestReview review) {
        KnowledgeBaseEntry entry = KnowledgeBaseEntry.builder()
            .repositoryId(review.getRepositoryId())
            .category("common_issue")
            .title(String.format("Common Issue: %s", issue.getIssueType()))
            .description(issue.getDescription())
            .content(buildIssueKnowledgeContent(issue))
            .tags(Arrays.asList(issue.getIssueType(), issue.getSeverity()))
            .severity(mapIssueSeverityToKnowledgeSeverity(issue.getSeverity()))
            .frequencyCount(1)
            .effectivenessScore(BigDecimal.valueOf(70))
            .sourceReviewIds(Arrays.asList(review.getId()))
            .language(detectLanguageFromFilePath(issue.getFilePath()))
            .isApproved(false)
            .build();

        knowledgeBaseRepository.save(entry);
    }

    private void createKnowledgeEntryFromComment(ReviewComment comment, PullRequestReview review) {
        KnowledgeBaseEntry entry = KnowledgeBaseEntry.builder()
            .repositoryId(review.getRepositoryId())
            .category("best_practice")
            .title(String.format("Best Practice: %s", extractTitleFromComment(comment)))
            .description(comment.getContent())
            .content(buildCommentKnowledgeContent(comment))
            .tags(extractTagsFromComment(comment))
            .severity(KnowledgeBaseEntry.Severity.MEDIUM)
            .frequencyCount(1)
            .effectivenessScore(BigDecimal.valueOf(60))
            .sourceReviewIds(Arrays.asList(review.getId()))
            .language(detectLanguageFromFilePath(comment.getFilePath()))
            .isApproved(false)
            .build();

        knowledgeBaseRepository.save(entry);
    }

    private void createKnowledgeEntryFromReview(PullRequestReview review) {
        KnowledgeBaseEntry entry = KnowledgeBaseEntry.builder()
            .repositoryId(review.getRepositoryId())
            .category("review_pattern")
            .title(String.format("Review Pattern: %s", review.getPrTitle()))
            .description(String.format("Pattern extracted from PR #%d", review.getPrNumber()))
            .content(buildReviewKnowledgeContent(review))
            .tags(Arrays.asList("review", "pattern"))
            .severity(KnowledgeBaseEntry.Severity.LOW)
            .frequencyCount(1)
            .effectivenessScore(BigDecimal.valueOf(50))
            .sourceReviewIds(Arrays.asList(review.getId()))
            .isApproved(false)
            .build();

        knowledgeBaseRepository.save(entry);
    }

    // Additional helper methods would be implemented here for:
    // - calculateRelevanceScore
    // - generateReasoningForRecommendation
    // - getPersonalizedRecommendations
    // - getMostFrequentPatterns
    // - getTopContributors
    // - getRecentActivity
    // - Various utility methods

    // Data classes for the service
    @Data
    @Builder
    public static class KnowledgeSearchCriteria {
        private Long repositoryId;
        private String category;
        private String searchTerm;
        private List<String> tags;
        private String language;
        private String framework;
        private KnowledgeBaseEntry.Severity severity;
        private Boolean isApproved;
    }

    @Data
    @Builder
    public static class KnowledgeRecommendation {
        private KnowledgeBaseEntry entry;
        private BigDecimal relevanceScore;
        private String context;
        private String reasoning;
    }

    @Data
    @Builder
    public static class KnowledgeEntryRequest {
        private Long repositoryId;
        private String category;
        private String title;
        private String description;
        private String content;
        private List<String> tags;
        private KnowledgeBaseEntry.Severity severity;
        private List<Long> sourceReviewIds;
        private Long createdByUserId;
        private String language;
        private String framework;
    }

    @Data
    @Builder
    public static class KnowledgeBaseAnalytics {
        private Long repositoryId;
        private Integer totalEntries;
        private Integer approvedEntries;
        private Integer pendingEntries;
        private Map<String, Long> categoryDistribution;
        private Map<String, Long> severityDistribution;
        private Map<String, Long> languageDistribution;
        private BigDecimal averageEffectivenessScore;
        private List<String> mostFrequentPatterns;
        private List<String> topContributors;
        private List<String> recentActivity;
    }
}