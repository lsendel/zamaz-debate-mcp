package com.zamaz.mcp.docs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;

import java.time.Duration;

@Configuration
@EnableCaching
@EnableRetry
public class ApplicationConfig {

    @Bean
    @ConfigurationProperties(prefix = "app.rag")
    public RagServiceProperties ragServiceProperties() {
        return new RagServiceProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "app.documents")
    public DocumentProperties documentProperties() {
        return new DocumentProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "app.tenant")
    public TenantProperties tenantProperties() {
        return new TenantProperties();
    }

    @Bean
    public WebClient ragServiceWebClient(RagServiceProperties ragProperties) {
        return WebClient.builder()
                .baseUrl(ragProperties.getBaseUrl())
                .build();
    }

    public static class RagServiceProperties {
        private String baseUrl = "http://localhost:5004";
        private Duration timeout = Duration.ofSeconds(30);
        private RetryProperties retry = new RetryProperties();

        // Getters and setters
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public Duration getTimeout() { return timeout; }
        public void setTimeout(Duration timeout) { this.timeout = timeout; }
        public RetryProperties getRetry() { return retry; }
        public void setRetry(RetryProperties retry) { this.retry = retry; }

        public static class RetryProperties {
            private int maxAttempts = 3;
            private Duration delay = Duration.ofSeconds(1);

            public int getMaxAttempts() { return maxAttempts; }
            public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
            public Duration getDelay() { return delay; }
            public void setDelay(Duration delay) { this.delay = delay; }
        }
    }

    public static class DocumentProperties {
        private String maxSize = "10MB";
        private String allowedTypes = "md,txt,html,json,yaml,yml";
        private long cacheTtl = 3600;

        public String getMaxSize() { return maxSize; }
        public void setMaxSize(String maxSize) { this.maxSize = maxSize; }
        public String getAllowedTypes() { return allowedTypes; }
        public void setAllowedTypes(String allowedTypes) { this.allowedTypes = allowedTypes; }
        public long getCacheTtl() { return cacheTtl; }
        public void setCacheTtl(long cacheTtl) { this.cacheTtl = cacheTtl; }
    }

    public static class TenantProperties {
        private String isolationMode = "row-level-security";
        private String defaultAppName = "default";

        public String getIsolationMode() { return isolationMode; }
        public void setIsolationMode(String isolationMode) { this.isolationMode = isolationMode; }
        public String getDefaultAppName() { return defaultAppName; }
        public void setDefaultAppName(String defaultAppName) { this.defaultAppName = defaultAppName; }
    }
}