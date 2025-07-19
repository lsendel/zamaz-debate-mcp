package com.zamaz.mcp.configserver.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConfigEncryptionUtil.
 */
@ExtendWith(MockitoExtension.class)
class ConfigEncryptionUtilTest {

    @Mock
    private TextEncryptor textEncryptor;

    private ConfigEncryptionUtil encryptionUtil;

    @BeforeEach
    void setUp() {
        encryptionUtil = new ConfigEncryptionUtil();
        ReflectionTestUtils.setField(encryptionUtil, "textEncryptor", textEncryptor);
    }

    @Test
    void testEncrypt_Success() {
        // Given
        String plainText = "mySecretPassword";
        String encryptedText = "encryptedValue123";
        when(textEncryptor.encrypt(plainText)).thenReturn(encryptedText);

        // When
        String result = encryptionUtil.encrypt(plainText);

        // Then
        assertEquals("{cipher}encryptedValue123", result);
        verify(textEncryptor).encrypt(plainText);
    }

    @Test
    void testEncrypt_EmptyValue() {
        // When
        String result = encryptionUtil.encrypt("");

        // Then
        assertEquals("", result);
        verify(textEncryptor, never()).encrypt(anyString());
    }

    @Test
    void testEncrypt_NullValue() {
        // When
        String result = encryptionUtil.encrypt(null);

        // Then
        assertNull(result);
        verify(textEncryptor, never()).encrypt(anyString());
    }

    @Test
    void testDecrypt_Success() {
        // Given
        String encryptedText = "{cipher}encryptedValue123";
        String plainText = "mySecretPassword";
        when(textEncryptor.decrypt("encryptedValue123")).thenReturn(plainText);

        // When
        String result = encryptionUtil.decrypt(encryptedText);

        // Then
        assertEquals(plainText, result);
        verify(textEncryptor).decrypt("encryptedValue123");
    }

    @Test
    void testDecrypt_NotEncrypted() {
        // Given
        String plainText = "notEncrypted";

        // When
        String result = encryptionUtil.decrypt(plainText);

        // Then
        assertEquals(plainText, result);
        verify(textEncryptor, never()).decrypt(anyString());
    }

    @Test
    void testIsEncrypted() {
        assertTrue(encryptionUtil.isEncrypted("{cipher}someValue"));
        assertFalse(encryptionUtil.isEncrypted("plainValue"));
        assertFalse(encryptionUtil.isEncrypted(null));
    }

    @Test
    void testValidateEncryptedValue_Valid() {
        // Given
        String encryptedText = "{cipher}encryptedValue123";
        when(textEncryptor.decrypt("encryptedValue123")).thenReturn("decryptedValue");

        // When
        boolean result = encryptionUtil.validateEncryptedValue(encryptedText);

        // Then
        assertTrue(result);
    }

    @Test
    void testValidateEncryptedValue_Invalid() {
        // Given
        String encryptedText = "{cipher}invalidValue";
        when(textEncryptor.decrypt("invalidValue")).thenThrow(new RuntimeException("Decryption failed"));

        // When
        boolean result = encryptionUtil.validateEncryptedValue(encryptedText);

        // Then
        assertFalse(result);
    }

    @Test
    void testEncryptSensitiveProperties() {
        // Given
        Map<String, String> properties = new HashMap<>();
        properties.put("database.url", "jdbc:postgresql://localhost:5432/db");
        properties.put("database.password", "plainPassword");
        properties.put("api.secret", "plainSecret");
        properties.put("jwt.key", "plainKey");
        properties.put("normal.property", "normalValue");

        when(textEncryptor.encrypt("plainPassword")).thenReturn("encryptedPassword");
        when(textEncryptor.encrypt("plainSecret")).thenReturn("encryptedSecret");
        when(textEncryptor.encrypt("plainKey")).thenReturn("encryptedKey");

        // When
        Map<String, String> result = encryptionUtil.encryptSensitiveProperties(properties);

        // Then
        assertEquals("jdbc:postgresql://localhost:5432/db", result.get("database.url"));
        assertEquals("{cipher}encryptedPassword", result.get("database.password"));
        assertEquals("{cipher}encryptedSecret", result.get("api.secret"));
        assertEquals("{cipher}encryptedKey", result.get("jwt.key"));
        assertEquals("normalValue", result.get("normal.property"));
    }

    @Test
    void testValidateEncryptedProperties() {
        // Given
        Map<String, String> properties = new HashMap<>();
        properties.put("password", "{cipher}validEncrypted");
        properties.put("secret", "{cipher}invalidEncrypted");
        properties.put("normal", "plainValue");

        when(textEncryptor.decrypt("validEncrypted")).thenReturn("decryptedValue");
        when(textEncryptor.decrypt("invalidEncrypted")).thenThrow(new RuntimeException("Invalid"));

        // When
        ConfigEncryptionUtil.ValidationResult result = encryptionUtil.validateEncryptedProperties(properties);

        // Then
        assertTrue(result.hasErrors());
        assertEquals(1, result.getErrorCount());
        assertEquals(1, result.getValidCount());
        assertTrue(result.getErrors().containsKey("secret"));
        assertTrue(result.getValidProperties().containsKey("password"));
    }

    @Test
    void testEncryptionWithoutEncryptor() {
        // Given
        ConfigEncryptionUtil utilWithoutEncryptor = new ConfigEncryptionUtil();
        ReflectionTestUtils.setField(utilWithoutEncryptor, "textEncryptor", null);

        // When
        String encrypted = utilWithoutEncryptor.encrypt("test");
        String decrypted = utilWithoutEncryptor.decrypt("{cipher}test");

        // Then
        assertEquals("test", encrypted);
        assertEquals("{cipher}test", decrypted);
    }
}