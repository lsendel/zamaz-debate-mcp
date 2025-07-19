package com.zamaz.mcp.configserver.config;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom configuration source for AWS Secrets Manager integration.
 * Allows Spring Cloud Config Server to fetch secrets from AWS Secrets Manager.
 */
@Configuration
@ConditionalOnProperty(name = "spring.profiles.active", havingValue = "aws")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AwsSecretsManagerConfigSource implements EnvironmentRepository {

    private static final Logger logger = LoggerFactory.getLogger(AwsSecretsManagerConfigSource.class);

    private final AWSSecretsManager secretsManager;
    private final ObjectMapper objectMapper;
    private final String region;
    private final String prefix;

    public AwsSecretsManagerConfigSource() {
        this.region = System.getenv("AWS_REGION") != null ? 
            System.getenv("AWS_REGION") : "us-east-1";
        this.prefix = System.getenv("AWS_SECRETS_PREFIX") != null ? 
            System.getenv("AWS_SECRETS_PREFIX") : "/secret/mcp";
        
        this.secretsManager = AWSSecretsManagerClientBuilder.standard()
            .withRegion(region)
            .build();
        
        this.objectMapper = new ObjectMapper();
        
        logger.info("AWS Secrets Manager configuration source initialized for region: {}", region);
    }

    @Override
    public Environment findOne(String application, String profile, String label) {
        logger.debug("Fetching secrets for application: {}, profile: {}, label: {}", 
            application, profile, label);
        
        Environment environment = new Environment(application, profile, label);
        
        try {
            // Fetch global secrets
            Map<String, Object> globalSecrets = fetchSecrets(prefix + "/application");
            if (!globalSecrets.isEmpty()) {
                environment.add(new PropertySource("aws-secrets-global", globalSecrets));
            }
            
            // Fetch application-specific secrets
            Map<String, Object> appSecrets = fetchSecrets(prefix + "/" + application);
            if (!appSecrets.isEmpty()) {
                environment.add(new PropertySource("aws-secrets-" + application, appSecrets));
            }
            
            // Fetch profile-specific secrets
            if (profile != null && !profile.isEmpty()) {
                Map<String, Object> profileSecrets = fetchSecrets(
                    prefix + "/" + application + "-" + profile);
                if (!profileSecrets.isEmpty()) {
                    environment.add(new PropertySource(
                        "aws-secrets-" + application + "-" + profile, profileSecrets));
                }
            }
            
            // Fetch security-specific secrets
            Map<String, Object> securitySecrets = fetchSecuritySecrets(profile);
            if (!securitySecrets.isEmpty()) {
                environment.add(new PropertySource("aws-secrets-security", securitySecrets));
            }
            
        } catch (Exception e) {
            logger.error("Failed to fetch secrets from AWS Secrets Manager", e);
            // Return empty environment on error to allow fallback to other sources
        }
        
        return environment;
    }

    /**
     * Fetches secrets from AWS Secrets Manager
     */
    private Map<String, Object> fetchSecrets(String secretName) {
        Map<String, Object> secrets = new HashMap<>();
        
        try {
            GetSecretValueRequest request = new GetSecretValueRequest()
                .withSecretId(secretName);
            
            GetSecretValueResult result = secretsManager.getSecretValue(request);
            
            if (result.getSecretString() != null) {
                // Parse JSON secret
                Map<String, Object> secretMap = objectMapper.readValue(
                    result.getSecretString(), Map.class);
                
                // Flatten nested properties
                flattenMap("", secretMap, secrets);
                
                logger.debug("Fetched {} properties from secret: {}", secrets.size(), secretName);
            }
            
        } catch (com.amazonaws.services.secretsmanager.model.ResourceNotFoundException e) {
            logger.debug("Secret not found: {}", secretName);
        } catch (Exception e) {
            logger.error("Failed to fetch secret: {}", secretName, e);
        }
        
        return secrets;
    }

    /**
     * Fetches security-specific secrets based on profile
     */
    private Map<String, Object> fetchSecuritySecrets(String profile) {
        Map<String, Object> secrets = new HashMap<>();
        
        // JWT signing keys
        secrets.putAll(fetchSecrets(prefix + "/security/jwt-keys"));
        
        // OAuth2 client secrets
        secrets.putAll(fetchSecrets(prefix + "/security/oauth2-clients"));
        
        // Encryption keys
        secrets.putAll(fetchSecrets(prefix + "/security/encryption-keys"));
        
        // API keys for external services
        if ("prod".equals(profile) || "staging".equals(profile)) {
            secrets.putAll(fetchSecrets(prefix + "/security/external-apis"));
        }
        
        // Database credentials
        secrets.putAll(fetchSecrets(prefix + "/security/database"));
        
        // Redis credentials
        secrets.putAll(fetchSecrets(prefix + "/security/redis"));
        
        return secrets;
    }

    /**
     * Flattens a nested map structure into dot-notation properties
     */
    private void flattenMap(String prefix, Map<String, Object> source, Map<String, Object> target) {
        source.forEach((key, value) -> {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            
            if (value instanceof Map) {
                flattenMap(fullKey, (Map<String, Object>) value, target);
            } else {
                target.put(fullKey, value);
            }
        });
    }

    @Override
    public Environment findOne(String application, String profile, String label, boolean includeOrigin) {
        // For compatibility with newer versions of Spring Cloud Config
        return findOne(application, profile, label);
    }
}