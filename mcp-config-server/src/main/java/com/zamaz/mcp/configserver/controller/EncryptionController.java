package com.zamaz.mcp.configserver.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.zamaz.mcp.configserver.util.ConfigEncryptionUtil;
import com.zamaz.mcp.configserver.service.KeyRotationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;

/**
 * REST controller for encryption operations.
 * Provides endpoints for encrypting/decrypting values and managing encryption keys.
 */
@RestController
@RequestMapping("/encryption")
@PreAuthorize("hasRole('ADMIN')")
public class EncryptionController {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionController.class);
    
    @Autowired
    private ConfigEncryptionUtil encryptionUtil;
    
    @Autowired
    private KeyRotationService keyRotationService;

    /**
     * Encrypts a plain text value.
     */
    @PostMapping("/encrypt")
    public ResponseEntity<EncryptResponse> encrypt(@RequestBody EncryptRequest request) {
        try {
            if (request.getValue() == null || request.getValue().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new EncryptResponse(null, "Value cannot be empty"));
            }
            
            String encrypted = encryptionUtil.encrypt(request.getValue());
            logger.info("Successfully encrypted value");
            
            return ResponseEntity.ok(new EncryptResponse(encrypted, null));
        } catch (Exception e) {
            logger.error("Failed to encrypt value", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new EncryptResponse(null, "Encryption failed: " + e.getMessage()));
        }
    }

    /**
     * Decrypts an encrypted value.
     */
    @PostMapping("/decrypt")
    public ResponseEntity<DecryptResponse> decrypt(@RequestBody DecryptRequest request) {
        try {
            if (request.getValue() == null || request.getValue().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new DecryptResponse(null, "Value cannot be empty"));
            }
            
            if (!encryptionUtil.isEncrypted(request.getValue())) {
                return ResponseEntity.badRequest()
                    .body(new DecryptResponse(null, "Value is not encrypted"));
            }
            
            String decrypted = encryptionUtil.decrypt(request.getValue());
            logger.info("Successfully decrypted value");
            
            return ResponseEntity.ok(new DecryptResponse(decrypted, null));
        } catch (Exception e) {
            logger.error("Failed to decrypt value", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new DecryptResponse(null, "Decryption failed: " + e.getMessage()));
        }
    }

    /**
     * Validates an encrypted value.
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidateResponse> validate(@RequestBody ValidateRequest request) {
        try {
            boolean isValid = encryptionUtil.validateEncryptedValue(request.getValue());
            
            return ResponseEntity.ok(new ValidateResponse(
                isValid,
                isValid ? "Valid encrypted value" : "Invalid encrypted value"
            ));
        } catch (Exception e) {
            logger.error("Failed to validate value", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ValidateResponse(false, "Validation failed: " + e.getMessage()));
        }
    }

    /**
     * Encrypts multiple values in batch.
     */
    @PostMapping("/encrypt-batch")
    public ResponseEntity<BatchEncryptResponse> encryptBatch(@RequestBody BatchEncryptRequest request) {
        try {
            Map<String, String> encrypted = new HashMap<>();
            Map<String, String> errors = new HashMap<>();
            
            for (Map.Entry<String, String> entry : request.getValues().entrySet()) {
                try {
                    encrypted.put(entry.getKey(), encryptionUtil.encrypt(entry.getValue()));
                } catch (Exception e) {
                    errors.put(entry.getKey(), e.getMessage());
                }
            }
            
            logger.info("Batch encryption completed. Success: {}, Errors: {}", 
                encrypted.size(), errors.size());
            
            return ResponseEntity.ok(new BatchEncryptResponse(encrypted, errors));
        } catch (Exception e) {
            logger.error("Failed to perform batch encryption", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new BatchEncryptResponse(null, Map.of("error", e.getMessage())));
        }
    }

    /**
     * Rotates the encryption key.
     */
    @PostMapping("/rotate-key")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<KeyRotationResponse> rotateKey() {
        try {
            keyRotationService.rotateKey();
            logger.info("Key rotation initiated successfully");
            
            return ResponseEntity.ok(new KeyRotationResponse(
                true,
                "Key rotation completed successfully"
            ));
        } catch (Exception e) {
            logger.error("Failed to rotate key", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new KeyRotationResponse(false, "Key rotation failed: " + e.getMessage()));
        }
    }

    /**
     * Gets the key rotation history.
     */
    @GetMapping("/key-history")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getKeyHistory() {
        try {
            Map<String, KeyRotationService.KeyInfo> history = keyRotationService.getKeyHistory();
            Map<String, Object> response = new HashMap<>();
            
            history.forEach((keyId, keyInfo) -> {
                Map<String, Object> info = new HashMap<>();
                info.put("createdAt", keyInfo.getCreatedAt());
                info.put("keyId", keyInfo.getKeyId());
                response.put(keyId, info);
            });
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get key history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Request/Response DTOs
    
    public static class EncryptRequest {
        private String value;
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
    }
    
    public static class EncryptResponse {
        private String encryptedValue;
        private String error;
        
        public EncryptResponse(String encryptedValue, String error) {
            this.encryptedValue = encryptedValue;
            this.error = error;
        }
        
        public String getEncryptedValue() {
            return encryptedValue;
        }
        
        public String getError() {
            return error;
        }
    }
    
    public static class DecryptRequest {
        private String value;
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
    }
    
    public static class DecryptResponse {
        private String decryptedValue;
        private String error;
        
        public DecryptResponse(String decryptedValue, String error) {
            this.decryptedValue = decryptedValue;
            this.error = error;
        }
        
        public String getDecryptedValue() {
            return decryptedValue;
        }
        
        public String getError() {
            return error;
        }
    }
    
    public static class ValidateRequest {
        private String value;
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
    }
    
    public static class ValidateResponse {
        private boolean valid;
        private String message;
        
        public ValidateResponse(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    public static class BatchEncryptRequest {
        private Map<String, String> values;
        
        public Map<String, String> getValues() {
            return values;
        }
        
        public void setValues(Map<String, String> values) {
            this.values = values;
        }
    }
    
    public static class BatchEncryptResponse {
        private Map<String, String> encryptedValues;
        private Map<String, String> errors;
        
        public BatchEncryptResponse(Map<String, String> encryptedValues, Map<String, String> errors) {
            this.encryptedValues = encryptedValues;
            this.errors = errors;
        }
        
        public Map<String, String> getEncryptedValues() {
            return encryptedValues;
        }
        
        public Map<String, String> getErrors() {
            return errors;
        }
    }
    
    public static class KeyRotationResponse {
        private boolean success;
        private String message;
        
        public KeyRotationResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
    }
}