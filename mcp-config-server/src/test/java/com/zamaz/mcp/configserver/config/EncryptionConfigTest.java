package com.zamaz.mcp.configserver.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionConfigTest {

    private EncryptionConfig encryptionConfig;
    private String testKey = "mysecretkey12345"; // Test key only

    @BeforeEach
    void setUp() {
        encryptionConfig = new EncryptionConfig();
        ReflectionTestUtils.setField(encryptionConfig, "encryptKey", testKey);
    }

    @Test
    void testTextEncryptorCreation() {
        TextEncryptor encryptor = encryptionConfig.textEncryptor();
        assertNotNull(encryptor);
    }

    @Test
    void testEncryptionDecryption() {
        TextEncryptor encryptor = encryptionConfig.textEncryptor();
        String plainText = "test-password";
        
        String encrypted = encryptor.encrypt(plainText);
        assertNotNull(encrypted);
        assertNotEquals(plainText, encrypted);
        
        String decrypted = encryptor.decrypt(encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    void testEncryptionWithDifferentInputs() {
        TextEncryptor encryptor = encryptionConfig.textEncryptor();
        
        String[] testInputs = {
            "password123",
            "database-secret",
            "api-key-value",
            "token@#$%^&*()"
        };
        
        for (String input : testInputs) {
            String encrypted = encryptor.encrypt(input);
            String decrypted = encryptor.decrypt(encrypted);
            assertEquals(input, decrypted, "Failed for input: " + input);
        }
    }

    @Test
    void testEncryptionProducesUniqueValues() {
        TextEncryptor encryptor = encryptionConfig.textEncryptor();
        String plainText = "test-value";
        
        String encrypted1 = encryptor.encrypt(plainText);
        String encrypted2 = encryptor.encrypt(plainText);
        
        // Encrypted values should be different due to salt
        assertNotEquals(encrypted1, encrypted2);
        
        // But both should decrypt to same value
        assertEquals(plainText, encryptor.decrypt(encrypted1));
        assertEquals(plainText, encryptor.decrypt(encrypted2));
    }

    @Test
    void testInvalidEncryptedValue() {
        TextEncryptor encryptor = encryptionConfig.textEncryptor();
        
        assertThrows(Exception.class, () -> {
            encryptor.decrypt("invalid-encrypted-value");
        });
    }
}