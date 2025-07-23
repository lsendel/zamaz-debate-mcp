package com.zamaz.mcp.configserver.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.config.server.encryption.EncryptionController;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Utility class for encrypting and decrypting configuration values.
 * Provides methods to encrypt sensitive properties and validate encrypted values.
 */
@Component
public class ConfigEncryptionUtil {

    private static final Logger logger = LoggerFactory.getLogger(ConfigEncryptionUtil.class);
    
    private static final String CIPHER_PREFIX = "{cipher}";
    private static final Pattern ENCRYPTED_PROPERTY_PATTERN = Pattern.compile("\\{cipher\\}([A-Za-z0-9+/=]+)");
    
    @Autowired(required = false)
    private TextEncryptor textEncryptor;
    
    @Autowired(required = false)
    private EncryptionController encryptionController;

    @PostConstruct
    public void init() {
        if (textEncryptor == null && encryptionController == null) {
            logger.warn("No encryption configured. Sensitive properties will not be encrypted.");
        } else {
            logger.info("Encryption configured and ready for use.");
        }
    }

    /**
     * Encrypts a plain text value and returns it with the {cipher} prefix.
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        
        if (textEncryptor == null) {
            logger.warn("Encryption not available. Returning plain text.");
            return plainText;
        }
        
        try {
            String encrypted = textEncryptor.encrypt(plainText);
            return CIPHER_PREFIX + encrypted;
        } catch (Exception e) {
            logger.error("Failed to encrypt value", e);
            throw new EncryptionException("Failed to encrypt value", e);
        }
    }

    /**
     * Decrypts a value that has the {cipher} prefix.
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || !encryptedText.startsWith(CIPHER_PREFIX)) {
            return encryptedText;
        }
        
        if (textEncryptor == null) {
            logger.warn("Decryption not available. Returning encrypted text.");
            return encryptedText;
        }
        
        try {
            String cipherText = encryptedText.substring(CIPHER_PREFIX.length());
            return textEncryptor.decrypt(cipherText);
        } catch (Exception e) {
            logger.error("Failed to decrypt value", e);
            throw new EncryptionException("Failed to decrypt value", e);
        }
    }

    /**
     * Checks if a value is encrypted (has the {cipher} prefix).
     */
    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(CIPHER_PREFIX);
    }

    /**
     * Validates that an encrypted value can be successfully decrypted.
     */
    public boolean validateEncryptedValue(String encryptedValue) {
        if (!isEncrypted(encryptedValue)) {
            return false;
        }
        
        try {
            decrypt(encryptedValue);
            return true;
        } catch (Exception e) {
            logger.debug("Invalid encrypted value", e);
            return false;
        }
    }

    /**
     * Encrypts all values in a map that match certain patterns (passwords, secrets, keys).
     */
    public Map<String, String> encryptSensitiveProperties(Map<String, String> properties) {
        Map<String, String> result = new HashMap<>();
        
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            if (shouldEncrypt(key) && !isEncrypted(value)) {
                result.put(key, encrypt(value));
            } else {
                result.put(key, value);
            }
        }
        
        return result;
    }

    /**
     * Determines if a property key represents a sensitive value that should be encrypted.
     */
    private boolean shouldEncrypt(String propertyKey) {
        String lowerKey = propertyKey.toLowerCase();
        return lowerKey.contains("password") ||
               lowerKey.contains("secret") ||
               lowerKey.contains("key") ||
               lowerKey.contains("token") ||
               lowerKey.contains("credential") ||
               lowerKey.contains("private");
    }

    /**
     * Validates all encrypted properties in a configuration map.
     */
    public ValidationResult validateEncryptedProperties(Map<String, String> properties) {
        ValidationResult result = new ValidationResult();
        
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            if (isEncrypted(value)) {
                if (!validateEncryptedValue(value)) {
                    result.addError(key, "Invalid encrypted value");
                } else {
                    result.addValid(key);
                }
            }
        }
        
        return result;
    }

    /**
     * Custom exception for encryption operations.
     */
    public static class EncryptionException extends RuntimeException {
        public EncryptionException(String message) {
            super(message);
        }
        
        public EncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Result class for validation operations.
     */
    public static class ValidationResult {
        private final Map<String, String> errors = new HashMap<>();
        private final Map<String, Boolean> validProperties = new HashMap<>();
        
        public void addError(String property, String error) {
            errors.put(property, error);
        }
        
        public void addValid(String property) {
            validProperties.put(property, true);
        }
        
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
        
        public Map<String, String> getErrors() {
            return errors;
        }
        
        public Map<String, Boolean> getValidProperties() {
            return validProperties;
        }
        
        public int getErrorCount() {
            return errors.size();
        }
        
        public int getValidCount() {
            return validProperties.size();
        }
    }
}