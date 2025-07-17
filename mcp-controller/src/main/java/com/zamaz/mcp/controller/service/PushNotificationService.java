package com.zamaz.mcp.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for handling push notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${push.notification.enabled:false}")
    private boolean pushNotificationEnabled;
    
    @Value("${push.notification.firebase.api-key:}")
    private String firebaseApiKey;
    
    @Value("${push.notification.firebase.url:https://fcm.googleapis.com/fcm/send}")
    private String firebaseUrl;
    
    // Store user device tokens by organization and user ID
    private final Map<String, Map<String, List<String>>> userTokens = new ConcurrentHashMap<>();
    
    /**
     * Register a device token for push notifications
     */
    public void registerDeviceToken(String organizationId, String userId, String deviceToken) {
        log.info("Registering device token for user {} in organization {}", userId, organizationId);
        
        userTokens.computeIfAbsent(organizationId, k -> new ConcurrentHashMap<>())
                  .computeIfAbsent(userId, k -> new java.util.ArrayList<>())
                  .add(deviceToken);
        
        log.debug("Device token registered successfully");
    }
    
    /**
     * Unregister a device token
     */
    public void unregisterDeviceToken(String organizationId, String userId, String deviceToken) {
        Map<String, List<String>> orgTokens = userTokens.get(organizationId);
        if (orgTokens != null) {
            List<String> tokens = orgTokens.get(userId);
            if (tokens != null) {
                tokens.remove(deviceToken);
                if (tokens.isEmpty()) {
                    orgTokens.remove(userId);
                }
            }
        }
        
        log.info("Device token unregistered for user {} in organization {}", userId, organizationId);
    }
    
    /**
     * Send push notification about new debate response
     */
    @Async
    public void sendNewResponseNotification(String debateId, String organizationId, 
                                          String participantName, String position) {
        if (!pushNotificationEnabled) {
            log.debug("Push notifications disabled, skipping notification");
            return;
        }
        
        String title = "New Debate Response";
        String body = String.format("%s (%s) has responded in the debate", participantName, position);
        
        Map<String, Object> data = Map.of(
            "type", "new_response",
            "debateId", debateId,
            "participantName", participantName,
            "position", position
        );
        
        sendNotificationToOrganization(organizationId, title, body, data);
    }
    
    /**
     * Send push notification about round completion
     */
    @Async
    public void sendRoundCompletedNotification(String debateId, String organizationId, int roundNumber) {
        if (!pushNotificationEnabled) {
            return;
        }
        
        String title = "Debate Round Completed";
        String body = String.format("Round %d has been completed", roundNumber);
        
        Map<String, Object> data = Map.of(
            "type", "round_completed",
            "debateId", debateId,
            "roundNumber", roundNumber
        );
        
        sendNotificationToOrganization(organizationId, title, body, data);
    }
    
    /**
     * Send push notification about debate completion
     */
    @Async
    public void sendDebateCompletedNotification(String debateId, String organizationId, String summary) {
        if (!pushNotificationEnabled) {
            return;
        }
        
        String title = "Debate Completed";
        String body = "The debate has finished. Check out the results!";
        
        Map<String, Object> data = Map.of(
            "type", "debate_completed",
            "debateId", debateId,
            "summary", summary
        );
        
        sendNotificationToOrganization(organizationId, title, body, data);
    }
    
    /**
     * Send notification to all users in an organization
     */
    private void sendNotificationToOrganization(String organizationId, String title, 
                                               String body, Map<String, Object> data) {
        Map<String, List<String>> orgTokens = userTokens.get(organizationId);
        if (orgTokens == null || orgTokens.isEmpty()) {
            log.debug("No device tokens found for organization {}", organizationId);
            return;
        }
        
        // Send to all users in the organization
        orgTokens.forEach((userId, tokens) -> {
            tokens.forEach(token -> sendPushNotification(token, title, body, data));
        });
    }
    
    /**
     * Send notification to specific user
     */
    public void sendNotificationToUser(String organizationId, String userId, 
                                     String title, String body, Map<String, Object> data) {
        if (!pushNotificationEnabled) {
            return;
        }
        
        Map<String, List<String>> orgTokens = userTokens.get(organizationId);
        if (orgTokens != null) {
            List<String> tokens = orgTokens.get(userId);
            if (tokens != null) {
                tokens.forEach(token -> sendPushNotification(token, title, body, data));
            }
        }
    }
    
    /**
     * Send Firebase Cloud Messaging push notification
     */
    private void sendPushNotification(String deviceToken, String title, String body, Map<String, Object> data) {
        if (firebaseApiKey.isEmpty()) {
            log.warn("Firebase API key not configured, cannot send push notification");
            return;
        }
        
        try {
            Map<String, Object> notification = Map.of(
                "title", title,
                "body", body,
                "icon", "/icons/debate-icon-192.png",
                "badge", "/icons/debate-badge.png",
                "click_action", "FLUTTER_NOTIFICATION_CLICK"
            );
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("to", deviceToken);
            payload.put("notification", notification);
            payload.put("data", data);
            
            // Add priority and TTL
            payload.put("priority", "high");
            payload.put("time_to_live", 3600); // 1 hour
            
            WebClient webClient = webClientBuilder
                .defaultHeader("Authorization", "key=" + firebaseApiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
            
            webClient.post()
                .uri(firebaseUrl)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> 
                    log.debug("Push notification sent successfully: {}", response))
                .doOnError(error -> 
                    log.error("Failed to send push notification: {}", error.getMessage()))
                .subscribe();
                
        } catch (Exception e) {
            log.error("Error sending push notification", e);
        }
    }
    
    /**
     * Send test notification for debugging
     */
    public void sendTestNotification(String organizationId, String userId) {
        String title = "Test Notification";
        String body = "This is a test notification from the MCP Debate System";
        
        Map<String, Object> data = Map.of(
            "type", "test",
            "timestamp", System.currentTimeMillis()
        );
        
        sendNotificationToUser(organizationId, userId, title, body, data);
        log.info("Test notification sent to user {} in organization {}", userId, organizationId);
    }
    
    /**
     * Get notification statistics
     */
    public Map<String, Object> getNotificationStats() {
        int totalOrganizations = userTokens.size();
        int totalUsers = userTokens.values().stream()
            .mapToInt(Map::size)
            .sum();
        int totalTokens = userTokens.values().stream()
            .flatMap(orgTokens -> orgTokens.values().stream())
            .mapToInt(List::size)
            .sum();
        
        return Map.of(
            "enabled", pushNotificationEnabled,
            "totalOrganizations", totalOrganizations,
            "totalUsers", totalUsers,
            "totalDeviceTokens", totalTokens,
            "timestamp", System.currentTimeMillis()
        );
    }
}