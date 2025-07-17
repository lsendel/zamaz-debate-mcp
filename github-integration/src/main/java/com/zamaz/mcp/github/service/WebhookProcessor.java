package com.zamaz.mcp.github.service;

import com.zamaz.mcp.github.entity.GitHubInstallation;
import com.zamaz.mcp.github.entity.PullRequestReview;
import com.zamaz.mcp.github.entity.ReviewComment;
import com.zamaz.mcp.github.entity.RepositoryConfig;
import com.zamaz.mcp.github.repository.GitHubInstallationRepository;
import com.zamaz.mcp.github.repository.PullRequestReviewRepository;
import com.zamaz.mcp.github.repository.ReviewCommentRepository;
import com.zamaz.mcp.github.repository.RepositoryConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

/**
 * Processes GitHub webhook events and triggers appropriate actions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookProcessor {

    private final GitHubInstallationRepository installationRepository;
    private final PullRequestReviewRepository reviewRepository;
    private final ReviewCommentRepository commentRepository;
    private final RepositoryConfigRepository configRepository;
    private final GitHubApiClient apiClient;
    private final PullRequestAnalyzer analyzer;
    private final NotificationService notificationService;

    /**
     * Main webhook processing method
     */
    @Transactional
    public Map<String, Object> processWebhook(String eventType, Map<String, Object> payload, String deliveryId) {
        log.info("Processing webhook: event={}, delivery={}", eventType, deliveryId);

        Map<String, Object> result = new HashMap<>();
        result.put("eventType", eventType);
        result.put("deliveryId", deliveryId);
        result.put("processed", false);

        try {
            switch (eventType) {
                case "pull_request":
                    result.putAll(processPullRequestEvent(payload));
                    break;
                case "pull_request_review":
                    result.putAll(processPullRequestReviewEvent(payload));
                    break;
                case "pull_request_review_comment":
                    result.putAll(processPullRequestReviewCommentEvent(payload));
                    break;
                case "installation":
                    result.putAll(processInstallationEvent(payload));
                    break;
                case "installation_repositories":
                    result.putAll(processInstallationRepositoriesEvent(payload));
                    break;
                case "push":
                    result.putAll(processPushEvent(payload));
                    break;
                default:
                    log.debug("Unsupported event type: {}", eventType);
                    result.put("message", "Event type not supported");
                    return result;
            }

            result.put("processed", true);
            log.info("Successfully processed webhook: event={}, delivery={}", eventType, deliveryId);

        } catch (Exception e) {
            log.error("Error processing webhook: event={}, delivery={}, error={}",
                eventType, deliveryId, e.getMessage(), e);
            result.put("error", e.getMessage());
            result.put("processed", false);
        }

        return result;
    }

    /**
     * Process pull request events (opened, synchronize, closed)
     */
    private Map<String, Object> processPullRequestEvent(Map<String, Object> payload) {
        Map<String, Object> result = new HashMap<>();
        String action = (String) payload.get("action");
        Map<String, Object> prData = (Map<String, Object>) payload.get("pull_request");
        Map<String, Object> repoData = (Map<String, Object>) payload.get("repository");

        log.info("Processing pull request event: action={}, pr={}, repo={}",
            action, prData.get("number"), repoData.get("full_name"));

        try {
            // Get installation and repository config
            Long installationId = ((Number) payload.get("installation")).longValue();
            Optional<GitHubInstallation> installation = installationRepository.findById(installationId);
            
            if (installation.isEmpty()) {
                log.warn("Installation not found: {}", installationId);
                result.put("error", "Installation not found");
                return result;
            }

            String repoFullName = (String) repoData.get("full_name");
            Optional<RepositoryConfig> config = configRepository.findByRepositoryFullName(repoFullName);
            
            if (config.isEmpty() || !config.get().isAutoReviewEnabled()) {
                log.debug("Auto-review not enabled for repository: {}", repoFullName);
                result.put("message", "Auto-review not enabled");
                return result;
            }

            // Process based on action
            switch (action) {
                case "opened":
                case "synchronize":
                    result.putAll(triggerPullRequestReview(installation.get(), prData, repoData));
                    break;
                case "closed":
                    result.putAll(handlePullRequestClosed(installation.get(), prData, repoData));
                    break;
                default:
                    log.debug("Unsupported PR action: {}", action);
                    result.put("message", "Action not supported");
            }

        } catch (Exception e) {
            log.error("Error processing pull request event: {}", e.getMessage(), e);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Trigger automated pull request review
     */
    private Map<String, Object> triggerPullRequestReview(GitHubInstallation installation,
                                                        Map<String, Object> prData,
                                                        Map<String, Object> repoData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Extract PR details
            Integer prNumber = (Integer) prData.get("number");
            String repoFullName = (String) repoData.get("full_name");
            String[] repoParts = repoFullName.split("/");
            String owner = repoParts[0];
            String repo = repoParts[1];

            // Create or update review record
            PullRequestReview review = reviewRepository.findByInstallationIdAndRepositoryFullNameAndPrNumber(
                installation.getId(), repoFullName, prNumber)
                .orElse(new PullRequestReview());

            review.setInstallationId(installation.getId());
            review.setRepositoryFullName(repoFullName);
            review.setPrNumber(prNumber);
            review.setPrTitle((String) prData.get("title"));
            review.setPrAuthor((String) ((Map<String, Object>) prData.get("user")).get("login"));
            review.setStatus("PENDING");
            review.setCreatedAt(LocalDateTime.now());
            review.setUpdatedAt(LocalDateTime.now());

            review = reviewRepository.save(review);

            // Start asynchronous analysis
            analyzer.analyzePullRequestAsync(installation.getAccessToken(), owner, repo, prNumber, review.getId());

            result.put("reviewId", review.getId());
            result.put("status", "REVIEW_STARTED");
            result.put("message", "Pull request review initiated");

        } catch (Exception e) {
            log.error("Error triggering PR review: {}", e.getMessage(), e);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Handle pull request closed event
     */
    private Map<String, Object> handlePullRequestClosed(GitHubInstallation installation,
                                                       Map<String, Object> prData,
                                                       Map<String, Object> repoData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Integer prNumber = (Integer) prData.get("number");
            String repoFullName = (String) repoData.get("full_name");
            Boolean merged = (Boolean) prData.get("merged");

            // Update review record
            Optional<PullRequestReview> reviewOpt = reviewRepository.findByInstallationIdAndRepositoryFullNameAndPrNumber(
                installation.getId(), repoFullName, prNumber);

            if (reviewOpt.isPresent()) {
                PullRequestReview review = reviewOpt.get();
                review.setStatus(merged ? "MERGED" : "CLOSED");
                review.setUpdatedAt(LocalDateTime.now());
                reviewRepository.save(review);

                result.put("reviewId", review.getId());
                result.put("status", review.getStatus());
                result.put("message", "Pull request review updated");
            } else {
                result.put("message", "No review found for this PR");
            }

        } catch (Exception e) {
            log.error("Error handling PR closed: {}", e.getMessage(), e);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Process pull request review events
     */
    private Map<String, Object> processPullRequestReviewEvent(Map<String, Object> payload) {
        Map<String, Object> result = new HashMap<>();
        String action = (String) payload.get("action");
        
        log.info("Processing pull request review event: action={}", action);
        
        // Handle review submitted/edited/dismissed
        result.put("message", "Pull request review event processed");
        return result;
    }

    /**
     * Process pull request review comment events
     */
    private Map<String, Object> processPullRequestReviewCommentEvent(Map<String, Object> payload) {
        Map<String, Object> result = new HashMap<>();
        String action = (String) payload.get("action");
        
        log.info("Processing pull request review comment event: action={}", action);
        
        // Handle comment created/edited/deleted
        result.put("message", "Pull request review comment event processed");
        return result;
    }

    /**
     * Process installation events (created, deleted, suspend, unsuspend)
     */
    private Map<String, Object> processInstallationEvent(Map<String, Object> payload) {
        Map<String, Object> result = new HashMap<>();
        String action = (String) payload.get("action");
        Map<String, Object> installationData = (Map<String, Object>) payload.get("installation");

        log.info("Processing installation event: action={}, installation={}",
            action, installationData.get("id"));

        try {
            Long installationId = ((Number) installationData.get("id")).longValue();
            String account = (String) ((Map<String, Object>) installationData.get("account")).get("login");

            switch (action) {
                case "created":
                    GitHubInstallation installation = new GitHubInstallation();
                    installation.setId(installationId);
                    installation.setAccountLogin(account);
                    installation.setAccountType((String) ((Map<String, Object>) installationData.get("account")).get("type"));
                    installation.setStatus("ACTIVE");
                    installation.setCreatedAt(LocalDateTime.now());
                    installation.setUpdatedAt(LocalDateTime.now());
                    
                    installationRepository.save(installation);
                    result.put("message", "Installation created");
                    break;

                case "deleted":
                    installationRepository.deleteById(installationId);
                    result.put("message", "Installation deleted");
                    break;

                case "suspend":
                    installationRepository.findById(installationId).ifPresent(inst -> {
                        inst.setStatus("SUSPENDED");
                        inst.setUpdatedAt(LocalDateTime.now());
                        installationRepository.save(inst);
                    });
                    result.put("message", "Installation suspended");
                    break;

                case "unsuspend":
                    installationRepository.findById(installationId).ifPresent(inst -> {
                        inst.setStatus("ACTIVE");
                        inst.setUpdatedAt(LocalDateTime.now());
                        installationRepository.save(inst);
                    });
                    result.put("message", "Installation unsuspended");
                    break;

                default:
                    log.debug("Unsupported installation action: {}", action);
                    result.put("message", "Action not supported");
            }

        } catch (Exception e) {
            log.error("Error processing installation event: {}", e.getMessage(), e);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Process installation repositories events
     */
    private Map<String, Object> processInstallationRepositoriesEvent(Map<String, Object> payload) {
        Map<String, Object> result = new HashMap<>();
        String action = (String) payload.get("action");
        
        log.info("Processing installation repositories event: action={}", action);
        
        // Handle repositories added/removed
        result.put("message", "Installation repositories event processed");
        return result;
    }

    /**
     * Process push events
     */
    private Map<String, Object> processPushEvent(Map<String, Object> payload) {
        Map<String, Object> result = new HashMap<>();
        String ref = (String) payload.get("ref");
        
        log.info("Processing push event: ref={}", ref);
        
        // Handle push to monitored branches
        result.put("message", "Push event processed");
        return result;
    }
}