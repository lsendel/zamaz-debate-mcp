package com.zamaz.mcp.github.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for handling GitHub webhook operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubWebhookService {

    @Value("${github.webhook.secret}")
    private String webhookSecret;

    private final Map<String, AtomicLong> webhookStats = new ConcurrentHashMap<>();

    /**
     * Verify GitHub webhook signature
     */
    public boolean verifySignature(Map<String, Object> payload, String signature) {
        if (signature == null || !signature.startsWith("sha256=")) {
            log.warn("Invalid signature format");
            return false;
        }

        try {
            String payloadString = convertPayloadToString(payload);
            String expectedSignature = "sha256=" + calculateHmacSha256(payloadString, webhookSecret);
            
            boolean valid = signature.equals(expectedSignature);
            if (!valid) {
                log.warn("Signature verification failed. Expected: {}, Got: {}", expectedSignature, signature);
            }
            
            return valid;
            
        } catch (Exception e) {
            log.error("Error verifying webhook signature: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get webhook statistics
     */
    public Map<String, Object> getWebhookStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        webhookStats.forEach((eventType, count) -> {
            stats.put(eventType, count.get());
        });
        
        stats.put("totalEvents", webhookStats.values().stream()
            .mapToLong(AtomicLong::get)
            .sum());
        
        return stats;
    }

    /**
     * Increment webhook event counter
     */
    public void incrementEventCounter(String eventType) {
        webhookStats.computeIfAbsent(eventType, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Calculate HMAC-SHA256 signature
     */
    private String calculateHmacSha256(String data, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    /**
     * Convert byte array to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Convert payload map to string for signature verification
     */
    private String convertPayloadToString(Map<String, Object> payload) {
        // In a real implementation, this would serialize the payload exactly as GitHub sends it
        // For now, we'll use a simple toString approach
        return payload.toString();
    }
}