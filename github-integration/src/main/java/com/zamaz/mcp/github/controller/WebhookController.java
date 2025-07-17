package com.zamaz.mcp.github.controller;

import com.zamaz.mcp.github.service.WebhookProcessor;
import com.zamaz.mcp.github.service.GitHubWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * GitHub Webhook Controller
 * Handles incoming GitHub webhook events for pull requests, issues, and installations
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookProcessor webhookProcessor;
    private final GitHubWebhookService webhookService;

    /**
     * Main webhook endpoint for all GitHub events
     */
    @PostMapping("/github")
    public ResponseEntity<Map<String, Object>> handleGitHubWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader("X-GitHub-Event") String eventType,
            @RequestHeader("X-GitHub-Delivery") String deliveryId,
            @RequestHeader(value = "X-GitHub-Signature-256", required = false) String signature,
            HttpServletRequest request) {

        log.info("Received GitHub webhook: event={}, delivery={}", eventType, deliveryId);

        try {
            // Verify webhook signature
            if (!webhookService.verifySignature(payload, signature)) {
                log.warn("Invalid webhook signature for delivery: {}", deliveryId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid signature"));
            }

            // Process the webhook event
            Map<String, Object> result = webhookProcessor.processWebhook(eventType, payload, deliveryId);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error processing webhook: event={}, delivery={}, error={}",
                eventType, deliveryId, e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Internal server error",
                    "message", e.getMessage(),
                    "deliveryId", deliveryId
                ));
        }
    }

    /**
     * Health check endpoint for webhook service
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "github-webhook",
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Get webhook statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = webhookService.getWebhookStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Test webhook endpoint for development
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testWebhook(
            @RequestBody Map<String, Object> payload) {

        log.info("Test webhook received: {}", payload);

        return ResponseEntity.ok(Map.of(
            "status", "received",
            "payload", payload,
            "timestamp", System.currentTimeMillis()
        ));
    }
}