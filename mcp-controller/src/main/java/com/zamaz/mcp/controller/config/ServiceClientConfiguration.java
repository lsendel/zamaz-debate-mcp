package com.zamaz.mcp.controller.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for service clients used by the controller service.
 * Sets up RestTemplate and other HTTP clients for inter-service communication.
 */
@Configuration
public class ServiceClientConfiguration {
    
    /**
     * Configure RestTemplate for template service communication.
     * 
     * @return Configured RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // Configure timeout settings
        restTemplate.setRequestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory());
        org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory = 
            (org.springframework.http.client.SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
        
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
        
        return restTemplate;
    }
}