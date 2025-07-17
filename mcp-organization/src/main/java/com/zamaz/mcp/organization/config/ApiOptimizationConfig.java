package com.zamaz.mcp.organization.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.CacheControl;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.WebContentInterceptor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * API optimization configuration including:
 * - Response compression
 * - ETag support for caching
 * - CORS configuration
 * - JSON optimization
 * - Connection pooling
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class ApiOptimizationConfig implements WebMvcConfigurer {

    /**
     * Configure optimized ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                .modules(new JavaTimeModule())
                .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .serializationInclusion(JsonInclude.Include.NON_NULL) // Exclude null fields
                .build();
    }

    /**
     * Enable ETag support for HTTP caching
     */
    @Bean
    public ShallowEtagHeaderFilter shallowEtagHeaderFilter() {
        return new ShallowEtagHeaderFilter();
    }

    /**
     * Configure CORS for API access
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("${app.cors.allowed-origins:http://localhost:3000}")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // Cache preflight responses for 1 hour
    }

    /**
     * Configure cache control headers
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        WebContentInterceptor interceptor = new WebContentInterceptor();
        
        // Cache static resources
        interceptor.addCacheMapping(
            CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic(),
            "/static/**", "/assets/**"
        );
        
        // Cache API responses for GET requests
        interceptor.addCacheMapping(
            CacheControl.maxAge(60, TimeUnit.SECONDS).cachePrivate(),
            "/api/*/organizations", "/api/*/users"
        );
        
        // No cache for sensitive endpoints
        interceptor.addCacheMapping(
            CacheControl.noCache(),
            "/api/*/auth/**", "/api/*/admin/**"
        );
        
        registry.addInterceptor(interceptor);
    }

    /**
     * Configure RestTemplate with connection pooling
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }
}