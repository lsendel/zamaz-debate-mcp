package com.zamaz.mcp.controller.controller;

import com.zamaz.mcp.controller.service.PushNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Push Notifications", description = "Manage push notifications for real-time updates")
public class PushNotificationController {
    
    private final PushNotificationService pushNotificationService;
    
    /**
     * Register device for push notifications
     */
    @PostMapping("/register")
    @Operation(summary = "Register device token for push notifications")
    public ResponseEntity<Map<String, String>> registerDevice(
            Authentication authentication,
            @Valid @RequestBody DeviceRegistrationRequest request) {
        
        String userId = authentication.getName();
        String organizationId = authentication.getDetails() instanceof Map ? 
                                ((Map<String, String>) authentication.getDetails()).get("organizationId") : null;

        log.debug("Registering device for user {} in organization {}", userId, organizationId);
        
        pushNotificationService.registerDeviceToken(
            organizationId, 
            userId, 
            request.getDeviceToken()
        );
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Device registered successfully"
        ));
    }
    
    /**
     * Unregister device from push notifications
     */
    @PostMapping("/unregister")
    @Operation(summary = "Unregister device token from push notifications")
    public ResponseEntity<Map<String, String>> unregisterDevice(
            @RequestHeader("X-Organization-ID") String organizationId,
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody DeviceRegistrationRequest request) {
        
        log.info("Unregistering device for user {} in organization {}", userId, organizationId);
        
        pushNotificationService.unregisterDeviceToken(
            organizationId, 
            userId, 
            request.getDeviceToken()
        );
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Device unregistered successfully"
        ));
    }
    
    /**
     * Send test notification
     */
    @PostMapping("/test")
    @Operation(summary = "Send test notification for debugging")
    public ResponseEntity<Map<String, String>> sendTestNotification(
            @RequestHeader("X-Organization-ID") String organizationId,
            @RequestHeader("X-User-ID") String userId) {
        
        log.info("Sending test notification to user {} in organization {}", userId, organizationId);
        
        pushNotificationService.sendTestNotification(organizationId, userId);
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Test notification sent"
        ));
    }
    
    /**
     * Get notification statistics
     */
    @GetMapping("/stats")
    @Operation(summary = "Get push notification statistics")
    public ResponseEntity<Map<String, Object>> getNotificationStats() {
        Map<String, Object> stats = pushNotificationService.getNotificationStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Send custom notification to user
     */
    @PostMapping("/send")
    @Operation(summary = "Send custom notification to specific user")
    public ResponseEntity<Map<String, String>> sendCustomNotification(
            Authentication authentication,
            @Valid @RequestBody CustomNotificationRequest request) {
        
        String userId = authentication.getName();
        String organizationId = authentication.getDetails() instanceof Map ? 
                                ((Map<String, String>) authentication.getDetails()).get("organizationId") : null;

        log.debug("Sending custom notification to user {} in organization {}", userId, organizationId);
        
        pushNotificationService.sendNotificationToUser(
            organizationId,
            userId,
            request.getTitle(),
            request.getBody(),
            request.getData()
        );
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Custom notification sent"
        ));
    }
    
    /**
     * Device registration request DTO
     */
    public static class DeviceRegistrationRequest {
        @NotBlank(message = "Device token is required")
        private String deviceToken;
        
        private String deviceType; // ios, android, web
        private String appVersion;
        
        public String getDeviceToken() { return deviceToken; }
        public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }
        public String getDeviceType() { return deviceType; }
        public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
        public String getAppVersion() { return appVersion; }
        public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
    }
    
    /**
     * Custom notification request DTO
     */
    public static class CustomNotificationRequest {
        @NotBlank(message = "Title is required")
        private String title;
        
        @NotBlank(message = "Body is required")
        private String body;
        
        private Map<String, Object> data;
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }
}