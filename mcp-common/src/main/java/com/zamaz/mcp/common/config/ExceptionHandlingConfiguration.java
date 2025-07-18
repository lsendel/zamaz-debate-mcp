package com.zamaz.mcp.common.config;

import com.zamaz.mcp.common.exception.StandardGlobalExceptionHandler;
import com.zamaz.mcp.common.infrastructure.logging.StructuredLogger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for centralized exception handling.
 * This configuration can be imported by all microservices to get consistent error handling.
 */
@Configuration
public class ExceptionHandlingConfiguration implements WebMvcConfigurer {

    /**
     * Provide the standard global exception handler if no custom one is defined.
     */
    @Bean
    @ConditionalOnMissingBean(StandardGlobalExceptionHandler.class)
    public StandardGlobalExceptionHandler standardGlobalExceptionHandler(
            StructuredLogger structuredLogger) {
        return new StandardGlobalExceptionHandler(structuredLogger);
    }
}