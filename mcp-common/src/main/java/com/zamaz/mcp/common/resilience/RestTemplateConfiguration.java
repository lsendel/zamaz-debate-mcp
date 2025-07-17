package com.zamaz.mcp.common.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Configuration for RestTemplate with resilience patterns.
 * Provides HTTP clients with circuit breaker, retry, and timeout capabilities.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class RestTemplateConfiguration {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    /**
     * Create a resilient RestTemplate with default configuration
     */
    @Bean(name = "resilientRestTemplate")
    public RestTemplate resilientRestTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(30))
            .additionalInterceptors(new ResilienceInterceptor("default"))
            .build();
    }

    /**
     * Create a RestTemplate for organization service
     */
    @Bean(name = "organizationRestTemplate")
    public RestTemplate organizationRestTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(3))
            .setReadTimeout(Duration.ofSeconds(10))
            .additionalInterceptors(new ResilienceInterceptor("organization-service"))
            .build();
    }

    /**
     * Create a RestTemplate for LLM service
     */
    @Bean(name = "llmRestTemplate")
    public RestTemplate llmRestTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(60)) // LLM calls can be slow
            .additionalInterceptors(new ResilienceInterceptor("llm-service"))
            .build();
    }

    /**
     * Create a RestTemplate for external APIs
     */
    @Bean(name = "externalApiRestTemplate")
    public RestTemplate externalApiRestTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(30))
            .additionalInterceptors(new ResilienceInterceptor("external-api"))
            .build();
    }

    /**
     * HTTP interceptor that applies resilience patterns
     */
    private class ResilienceInterceptor implements ClientHttpRequestInterceptor {
        
        private final String serviceName;
        private final CircuitBreaker circuitBreaker;
        private final Retry retry;

        public ResilienceInterceptor(String serviceName) {
            this.serviceName = serviceName;
            this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);
            this.retry = retryRegistry.retry(serviceName);
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, 
                                          ClientHttpRequestExecution execution) throws IOException {
            
            String endpoint = request.getURI().toString();
            log.debug("Executing HTTP request to {} with resilience patterns", endpoint);

            // Create supplier for the HTTP call
            Supplier<ClientHttpResponse> supplier = () -> {
                try {
                    return execution.execute(request, body);
                } catch (IOException e) {
                    throw new RuntimeException("HTTP request failed: " + e.getMessage(), e);
                }
            };

            // Decorate with retry
            supplier = Retry.decorateSupplier(retry, supplier);

            // Decorate with circuit breaker
            supplier = CircuitBreaker.decorateSupplier(circuitBreaker, supplier);

            try {
                ClientHttpResponse response = supplier.get();
                
                // Check response status and record failures for circuit breaker
                int statusCode = response.getStatusCode().value();
                if (statusCode >= 500) {
                    // Server errors should open circuit breaker
                    throw new RuntimeException("Server error: " + statusCode);
                }
                
                return response;
                
            } catch (Exception e) {
                if (e.getCause() instanceof IOException) {
                    throw (IOException) e.getCause();
                }
                throw new IOException("Request failed with resilience patterns: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Create a RestTemplate builder with common configuration
     */
    @Bean
    public RestTemplateBuilder restTemplateBuilder() {
        return new RestTemplateBuilder()
            .requestFactory(() -> {
                HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
                factory.setConnectTimeout(5000);
                factory.setReadTimeout(30000);
                return factory;
            })
            .errorHandler(new ResilientResponseErrorHandler());
    }

    /**
     * Custom error handler that works with resilience patterns
     */
    private static class ResilientResponseErrorHandler extends DefaultResponseErrorHandler {
        @Override
        public void handleError(ClientHttpResponse response) throws IOException {
            // Don't throw exceptions for 4xx errors (client errors)
            // Let circuit breaker handle 5xx errors (server errors)
            if (response.getStatusCode().is5xxServerError()) {
                super.handleError(response);
            }
            // Log 4xx errors but don't throw
            if (response.getStatusCode().is4xxClientError()) {
                log.warn("Client error: {} {}", response.getStatusCode(), response.getStatusText());
            }
        }
    }
}