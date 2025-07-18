package com.zamaz.mcp.debateengine.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

/**
 * Configuration for API pagination with sensible defaults and limits.
 */
@Configuration
public class PaginationConfig {

    @Value("${app.pagination.default-page-size:20}")
    private int defaultPageSize;
    
    @Value("${app.pagination.max-page-size:100}")
    private int maxPageSize;

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return resolver -> {
            // Set default page size
            resolver.setFallbackPageable(PageRequest.of(0, defaultPageSize));
            
            // Set maximum page size to prevent abuse
            resolver.setMaxPageSize(maxPageSize);
            
            // Configure parameter names
            resolver.setPageParameterName("page");
            resolver.setSizeParameterName("size");
            resolver.setOneIndexedParameters(false); // Use 0-based indexing
            
            // Configure sort parameter
            resolver.setSortParameterName("sort");
            resolver.setQualifierDelimiter("_");
        };
    }
}