package com.zamaz.mcp.configserver.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zamaz.mcp.configserver.util.ConfigEncryptionUtil;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service for managing encryption key rotation.
 * Handles periodic key rotation and maintains key history for decryption of older values.
 */
@Service
public class KeyRotationService {

    private static final Logger logger = LoggerFactory.getLogger(KeyRotationService.class);
    
    @Autowired(required = false)
    private ContextRefresher contextRefresher;
    
    @Autowired
    private ConfigEncryptionUtil encryptionUtil;
    
    @Value("${encrypt.key-rotation.enabled:false}")
    private boolean rotationEnabled;
    
    @Value("${encrypt.key-rotation.interval-days:90}")
    private int rotationIntervalDays;
    
    @Value("${encrypt.key-rotation.algorithm:AES}")
    private String keyAlgorithm;
    
    @Value("${encrypt.key-rotation.key-size:256}")
    private int keySize;
    
    @Value("${encrypt.key-rotation.max-key-history:5}")
    private int maxKeyHistory;
    
    private final Map<String, KeyInfo> keyHistory = new ConcurrentHashMap<>();
    private final AtomicReference<String> currentKeyId = new AtomicReference<>();
    
    /**
     * Scheduled task to check if key rotation is needed.
     * Runs daily at 2 AM.
     */
    @Scheduled(cron = "${encrypt.key-rotation.cron:0 0 2 * * ?}")
    public void checkKeyRotation() {
        if (!rotationEnabled) {
            logger.debug("Key rotation is disabled");
            return;
        }
        
        try {
            KeyInfo currentKey = getCurrentKeyInfo();
            if (currentKey == null || isRotationNeeded(currentKey)) {
                logger.info("Initiating key rotation");
                rotateKey();
            } else {
                logger.debug("Key rotation not needed. Current key age: {} days", 
                    getKeyAgeDays(currentKey));
            }
        } catch (Exception e) {
            logger.error("Error during key rotation check", e);
        }
    }
    
    /**
     * Performs key rotation by generating a new encryption key.
     */
    public void rotateKey() {
        if (!rotationEnabled) {
            throw new IllegalStateException("Key rotation is not enabled");
        }
        
        try {
            // Generate new key
            SecretKey newKey = generateNewKey();
            String newKeyId = generateKeyId();
            String encodedKey = Base64.getEncoder().encodeToString(newKey.getEncoded());
            
            // Store in history
            KeyInfo keyInfo = new KeyInfo(newKeyId, encodedKey, LocalDateTime.now());
            keyHistory.put(newKeyId, keyInfo);
            
            // Update current key
            String previousKeyId = currentKeyId.getAndSet(newKeyId);
            
            // Clean up old keys
            cleanupOldKeys();
            
            // Update configuration
            updateEncryptionConfiguration(encodedKey);
            
            logger.info("Successfully rotated encryption key. New key ID: {}, Previous key ID: {}", 
                newKeyId, previousKeyId);
            
            // Trigger configuration refresh
            if (contextRefresher != null) {
                contextRefresher.refresh();
            }
            
        } catch (Exception e) {
            logger.error("Failed to rotate encryption key", e);
            throw new KeyRotationException("Failed to rotate encryption key", e);
        }
    }
    
    /**
     * Generates a new encryption key.
     */
    private SecretKey generateNewKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(keyAlgorithm);
        keyGenerator.init(keySize, new SecureRandom());
        return keyGenerator.generateKey();
    }
    
    /**
     * Generates a unique key ID.
     */
    private String generateKeyId() {
        return "key-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString(new SecureRandom().nextInt());
    }
    
    /**
     * Updates the encryption configuration with the new key.
     * This would typically update an external key management service or vault.
     */
    private void updateEncryptionConfiguration(String newKey) {
        // In a real implementation, this would update:
        // - Environment variables
        // - HashiCorp Vault
        // - AWS Secrets Manager
        // - Azure Key Vault
        // etc.
        
        logger.info("Updating encryption configuration with new key");
        // Implementation depends on the key storage mechanism
    }
    
    /**
     * Checks if key rotation is needed based on key age.
     */
    private boolean isRotationNeeded(KeyInfo keyInfo) {
        long keyAgeDays = getKeyAgeDays(keyInfo);
        return keyAgeDays >= rotationIntervalDays;
    }
    
    /**
     * Gets the age of a key in days.
     */
    private long getKeyAgeDays(KeyInfo keyInfo) {
        return ChronoUnit.DAYS.between(keyInfo.getCreatedAt(), LocalDateTime.now());
    }
    
    /**
     * Gets the current key information.
     */
    private KeyInfo getCurrentKeyInfo() {
        String keyId = currentKeyId.get();
        return keyId != null ? keyHistory.get(keyId) : null;
    }
    
    /**
     * Removes old keys from history, keeping only the configured maximum.
     */
    private void cleanupOldKeys() {
        if (keyHistory.size() <= maxKeyHistory) {
            return;
        }
        
        keyHistory.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().getCreatedAt()
                .compareTo(e1.getValue().getCreatedAt()))
            .skip(maxKeyHistory)
            .forEach(entry -> {
                keyHistory.remove(entry.getKey());
                logger.info("Removed old key from history: {}", entry.getKey());
            });
    }
    
    /**
     * Gets the key history for audit purposes.
     */
    public Map<String, KeyInfo> getKeyHistory() {
        return new ConcurrentHashMap<>(keyHistory);
    }
    
    /**
     * Information about an encryption key.
     */
    public static class KeyInfo {
        private final String keyId;
        private final String encodedKey;
        private final LocalDateTime createdAt;
        
        public KeyInfo(String keyId, String encodedKey, LocalDateTime createdAt) {
            this.keyId = keyId;
            this.encodedKey = encodedKey;
            this.createdAt = createdAt;
        }
        
        public String getKeyId() {
            return keyId;
        }
        
        public String getEncodedKey() {
            return encodedKey;
        }
        
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }
    
    /**
     * Exception for key rotation operations.
     */
    public static class KeyRotationException extends RuntimeException {
        public KeyRotationException(String message) {
            super(message);
        }
        
        public KeyRotationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}