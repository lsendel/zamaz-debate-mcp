package com.zamaz.mcp.common.versioning;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Configuration for API versioning
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "mcp.api.versioning.enabled", havingValue = "true", matchIfMissing = true)
public class ApiVersioningConfiguration implements WebMvcConfigurer {

    private final ApiVersionInterceptor apiVersionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiVersionInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/api/health", "/api/info", "/api/metrics");
    }

    @Bean
    public RequestMappingHandlerMapping apiVersionRequestMappingHandlerMapping(
            ApiVersionResolver versionResolver,
            ApiVersionConfig config) {
        return new ApiVersionRequestMappingHandlerMapping(versionResolver, config);
    }

    @Bean
    public ApiVersionUtils apiVersionUtils(ApiVersionConfig config) {
        return new ApiVersionUtils(config);
    }

    @Bean
    public ApiVersionDocumentationGenerator apiVersionDocumentationGenerator(
            ApiVersionConfig config) {
        return new ApiVersionDocumentationGenerator(config);
    }
}