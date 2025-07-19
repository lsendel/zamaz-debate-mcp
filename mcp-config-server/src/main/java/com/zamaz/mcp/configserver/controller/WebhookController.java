package com.zamaz.mcp.configserver.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.bus.endpoint.RefreshBusEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

/**
 * Webhook controller for triggering configuration refresh from Git repositories.
 * Supports GitHub, GitLab, and Bitbucket webhooks.
 */
@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    @Autowired(required = false)
    private RefreshBusEndpoint refreshBusEndpoint;

    /**
     * GitHub webhook endpoint.
     */
    @PostMapping("/github")
    public ResponseEntity<WebhookResponse> handleGitHubWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event", required = false) String event,
            @RequestBody Map<String, Object> payload) {
        
        logger.info("Received GitHub webhook event: {}", event);
        
        // Validate webhook signature if configured
        if (!validateGitHubSignature(signature, payload)) {
            logger.warn("Invalid GitHub webhook signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new WebhookResponse(false, "Invalid signature"));
        }
        
        // Check if it's a push event
        if (!"push".equals(event)) {
            logger.info("Ignoring non-push event: {}", event);
            return ResponseEntity.ok(new WebhookResponse(true, "Event ignored"));
        }
        
        // Extract branch information
        String ref = (String) payload.get("ref");
        if (ref != null && shouldRefreshForBranch(ref)) {
            return triggerRefresh("GitHub push to " + ref);
        }
        
        return ResponseEntity.ok(new WebhookResponse(true, "No refresh needed"));
    }

    /**
     * GitLab webhook endpoint.
     */
    @PostMapping("/gitlab")
    public ResponseEntity<WebhookResponse> handleGitLabWebhook(
            @RequestHeader(value = "X-Gitlab-Token", required = false) String token,
            @RequestHeader(value = "X-Gitlab-Event", required = false) String event,
            @RequestBody Map<String, Object> payload) {
        
        logger.info("Received GitLab webhook event: {}", event);
        
        // Validate webhook token if configured
        if (!validateGitLabToken(token)) {
            logger.warn("Invalid GitLab webhook token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new WebhookResponse(false, "Invalid token"));
        }
        
        // Check if it's a push event
        if (!"Push Hook".equals(event)) {
            logger.info("Ignoring non-push event: {}", event);
            return ResponseEntity.ok(new WebhookResponse(true, "Event ignored"));
        }
        
        // Extract branch information
        String ref = (String) payload.get("ref");
        if (ref != null && shouldRefreshForBranch(ref)) {
            return triggerRefresh("GitLab push to " + ref);
        }
        
        return ResponseEntity.ok(new WebhookResponse(true, "No refresh needed"));
    }

    /**
     * Bitbucket webhook endpoint.
     */
    @PostMapping("/bitbucket")
    public ResponseEntity<WebhookResponse> handleBitbucketWebhook(
            @RequestHeader(value = "X-Event-Key", required = false) String eventKey,
            @RequestBody Map<String, Object> payload) {
        
        logger.info("Received Bitbucket webhook event: {}", eventKey);
        
        // Check if it's a push event
        if (!"repo:push".equals(eventKey)) {
            logger.info("Ignoring non-push event: {}", eventKey);
            return ResponseEntity.ok(new WebhookResponse(true, "Event ignored"));
        }
        
        // Extract push information
        @SuppressWarnings("unchecked")
        Map<String, Object> push = (Map<String, Object>) payload.get("push");
        if (push != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> changes = (Map<String, Object>) push.get("changes");
            if (changes != null && shouldRefreshForBitbucketChanges(changes)) {
                return triggerRefresh("Bitbucket push");
            }
        }
        
        return ResponseEntity.ok(new WebhookResponse(true, "No refresh needed"));
    }

    /**
     * Generic webhook endpoint.
     */
    @PostMapping("/generic")
    public ResponseEntity<WebhookResponse> handleGenericWebhook(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret,
            @RequestBody Map<String, Object> payload) {
        
        logger.info("Received generic webhook");
        
        // Validate webhook secret if configured
        if (!validateGenericSecret(secret)) {
            logger.warn("Invalid webhook secret");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new WebhookResponse(false, "Invalid secret"));
        }
        
        return triggerRefresh("Generic webhook");
    }

    /**
     * Triggers a configuration refresh.
     */
    private ResponseEntity<WebhookResponse> triggerRefresh(String source) {
        if (refreshBusEndpoint == null) {
            logger.error("RefreshBusEndpoint not available. Cannot trigger refresh.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new WebhookResponse(false, "Refresh endpoint not available"));
        }
        
        try {
            logger.info("Triggering configuration refresh from: {}", source);
            refreshBusEndpoint.busRefreshWithDestination("**"); // Refresh all services
            
            return ResponseEntity.ok(new WebhookResponse(
                true, 
                "Configuration refresh triggered successfully"
            ));
        } catch (Exception e) {
            logger.error("Failed to trigger configuration refresh", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new WebhookResponse(false, "Failed to trigger refresh: " + e.getMessage()));
        }
    }

    /**
     * Validates GitHub webhook signature.
     */
    private boolean validateGitHubSignature(String signature, Map<String, Object> payload) {
        // In production, implement proper signature validation
        // using the webhook secret configured in GitHub
        return true; // Placeholder
    }

    /**
     * Validates GitLab webhook token.
     */
    private boolean validateGitLabToken(String token) {
        // In production, compare with configured token
        return true; // Placeholder
    }

    /**
     * Validates generic webhook secret.
     */
    private boolean validateGenericSecret(String secret) {
        // In production, compare with configured secret
        return true; // Placeholder
    }

    /**
     * Determines if refresh should be triggered for a branch.
     */
    private boolean shouldRefreshForBranch(String ref) {
        // Extract branch name from ref (e.g., "refs/heads/main" -> "main")
        String branch = ref.replaceFirst("^refs/heads/", "");
        
        // In production, configure which branches trigger refresh
        return "main".equals(branch) || "master".equals(branch) || 
               "develop".equals(branch) || branch.startsWith("release/");
    }

    /**
     * Determines if refresh should be triggered for Bitbucket changes.
     */
    private boolean shouldRefreshForBitbucketChanges(Map<String, Object> changes) {
        // In production, implement logic to check if configuration files were changed
        return true; // Placeholder
    }

    /**
     * Response class for webhook operations.
     */
    public static class WebhookResponse {
        private boolean success;
        private String message;
        private long timestamp;

        public WebhookResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters and setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}