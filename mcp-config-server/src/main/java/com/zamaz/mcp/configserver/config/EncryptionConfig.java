package com.zamaz.mcp.configserver.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.rsa.crypto.KeyStoreKeyFactory;
import org.springframework.security.rsa.crypto.RsaSecretEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyPair;
import java.util.Base64;

/**
 * Configuration for property encryption in Spring Cloud Config Server.
 * Supports both symmetric (AES) and asymmetric (RSA) encryption.
 */
@Configuration
@RefreshScope
public class EncryptionConfig {

    @Value("${encrypt.key:}")
    private String encryptKey;

    @Value("${encrypt.key-store.location:}")
    private Resource keyStore;

    @Value("${encrypt.key-store.password:}")
    private String keyStorePassword;

    @Value("${encrypt.key-store.alias:}")
    private String keyAlias;

    @Value("${encrypt.key-store.secret:}")
    private String keySecret;

    @Value("${encrypt.salt:deadbeef}")
    private String salt;

    @Value("${encrypt.algorithm:AES}")
    private String algorithm;

    @Value("${encrypt.strong:false}")
    private boolean strongEncryption;

    /**
     * Creates a TextEncryptor bean for symmetric encryption using a shared secret key.
     * This is used when RSA encryption is not configured.
     */
    @Bean
    @ConditionalOnProperty(name = "encrypt.key-store.location", matchIfMissing = true, havingValue = "false")
    public TextEncryptor textEncryptor() {
        if (encryptKey == null || encryptKey.isEmpty()) {
            throw new IllegalStateException(
                "Encryption key not configured. Set 'encrypt.key' environment variable or use RSA key store."
            );
        }

        // Use strong encryption if enabled (requires JCE Unlimited Strength)
        if (strongEncryption) {
            return Encryptors.delux(encryptKey, salt);
        } else {
            return Encryptors.text(encryptKey, salt);
        }
    }

    /**
     * Creates an RsaSecretEncryptor bean for asymmetric encryption using RSA key pairs.
     * This provides better security than symmetric encryption.
     */
    @Bean
    @ConditionalOnProperty(name = "encrypt.key-store.location")
    public RsaSecretEncryptor rsaSecretEncryptor() {
        KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(
            keyStore, 
            keyStorePassword.toCharArray()
        );
        
        KeyPair keyPair = keyStoreKeyFactory.getKeyPair(
            keyAlias, 
            keySecret != null ? keySecret.toCharArray() : keyStorePassword.toCharArray()
        );
        
        return new RsaSecretEncryptor(keyPair);
    }

    /**
     * Creates a SecretKey for AES encryption from the configured key.
     * This can be used by services that need to perform their own encryption.
     */
    @Bean
    @ConditionalOnProperty(name = "encrypt.provide-secret-key", havingValue = "true")
    public SecretKey secretKey() {
        if (encryptKey == null || encryptKey.isEmpty()) {
            throw new IllegalStateException("Encryption key not configured");
        }

        byte[] decodedKey = Base64.getDecoder().decode(encryptKey);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, algorithm);
    }

    /**
     * Bean to provide encryption configuration properties to other beans.
     */
    @Bean
    public EncryptionProperties encryptionProperties() {
        EncryptionProperties props = new EncryptionProperties();
        props.setAlgorithm(algorithm);
        props.setStrongEncryption(strongEncryption);
        props.setSalt(salt);
        props.setKeyStoreLocation(keyStore != null ? keyStore.getFilename() : null);
        props.setKeyAlias(keyAlias);
        return props;
    }

    /**
     * Inner class to hold encryption properties.
     */
    public static class EncryptionProperties {
        private String algorithm;
        private boolean strongEncryption;
        private String salt;
        private String keyStoreLocation;
        private String keyAlias;

        // Getters and setters
        public String getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }

        public boolean isStrongEncryption() {
            return strongEncryption;
        }

        public void setStrongEncryption(boolean strongEncryption) {
            this.strongEncryption = strongEncryption;
        }

        public String getSalt() {
            return salt;
        }

        public void setSalt(String salt) {
            this.salt = salt;
        }

        public String getKeyStoreLocation() {
            return keyStoreLocation;
        }

        public void setKeyStoreLocation(String keyStoreLocation) {
            this.keyStoreLocation = keyStoreLocation;
        }

        public String getKeyAlias() {
            return keyAlias;
        }

        public void setKeyAlias(String keyAlias) {
            this.keyAlias = keyAlias;
        }
    }
}