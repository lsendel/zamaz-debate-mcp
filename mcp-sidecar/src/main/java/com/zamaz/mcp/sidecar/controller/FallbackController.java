package com.zamaz.mcp.sidecar.controller;

import com.zamaz.mcp.common.patterns.BaseController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * Fallback Controller for MCP Sidecar
 * 
 * Provides fallback responses when backend services are unavailable
 * or circuit breakers are open. This ensures graceful degradation
 * of service functionality.
 */
@RestController
@RequestMapping("/fallback")
@RequiredArgsConstructor
@Slf4j
public class FallbackController extends BaseController {

    /**
     * Fallback for organization service
     */
    @GetMapping("/organization")
    public Mono<ResponseEntity<FallbackResponse>> organizationFallback() {
        log.warn("Organization service fallback triggered");
        
        FallbackResponse response = FallbackResponse.builder()
                .error("SERVICE_UNAVAILABLE")
                .message("Organization service is temporarily unavailable. Please try again later.")
                .timestamp(Instant.now())
                .service("mcp-organization")
                .suggestion("Check service status or contact support if the issue persists.")
                .retryAfter(30)
                .build();
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    /**
     * Fallback for LLM service
     */
    @GetMapping("/llm")
    public Mono<ResponseEntity<FallbackResponse>> llmFallback() {
        log.warn("LLM service fallback triggered");
        
        FallbackResponse response = FallbackResponse.builder()
                .error("AI_SERVICE_UNAVAILABLE")
                .message("AI/LLM service is temporarily unavailable. Your request cannot be processed right now.")
                .timestamp(Instant.now())
                .service("mcp-llm")
                .suggestion("Try again in a few minutes or use cached responses if available.")
                .retryAfter(60)
                .build();
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    /**
     * Fallback for debate controller service
     */
    @GetMapping("/debate")
    public Mono<ResponseEntity<FallbackResponse>> debateFallback() {
        log.warn("Debate controller service fallback triggered");
        
        FallbackResponse response = FallbackResponse.builder()
                .error("DEBATE_SERVICE_UNAVAILABLE")
                .message("Debate management service is temporarily unavailable. Debates cannot be processed right now.")
                .timestamp(Instant.now())
                .service("mcp-controller")
                .suggestion("Check ongoing debates in read-only mode or try again later.")
                .retryAfter(45)
                .build();
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    /**
     * Fallback for RAG service
     */
    @GetMapping("/rag")
    public Mono<ResponseEntity<FallbackResponse>> ragFallback() {
        log.warn("RAG service fallback triggered");
        
        FallbackResponse response = FallbackResponse.builder()
                .error("RAG_SERVICE_UNAVAILABLE")
                .message("Document retrieval and RAG service is temporarily unavailable.")
                .timestamp(Instant.now())
                .service("mcp-rag")
                .suggestion("Document search and context retrieval are temporarily disabled.")
                .retryAfter(30)
                .build();
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    /**
     * Fallback for template service
     */
    @GetMapping("/template")
    public Mono<ResponseEntity<FallbackResponse>> templateFallback() {
        log.warn("Template service fallback triggered");
        
        FallbackResponse response = FallbackResponse.builder()
                .error("TEMPLATE_SERVICE_UNAVAILABLE")
                .message("Template service is temporarily unavailable. Template operations are disabled.")
                .timestamp(Instant.now())
                .service("mcp-template")
                .suggestion("Use default templates or try again later.")
                .retryAfter(30)
                .build();
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    /**
     * Fallback for context service
     */
    @GetMapping("/context")
    public Mono<ResponseEntity<FallbackResponse>> contextFallback() {
        log.warn("Context service fallback triggered");
        
        FallbackResponse response = FallbackResponse.builder()
                .error("CONTEXT_SERVICE_UNAVAILABLE")
                .message("Context management service is temporarily unavailable.")
                .timestamp(Instant.now())
                .service("mcp-context")
                .suggestion("Context operations are temporarily disabled.")
                .retryAfter(30)
                .build();
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    /**
     * Fallback for security service
     */
    @GetMapping("/security")
    public Mono<ResponseEntity<FallbackResponse>> securityFallback() {
        log.error("Security service fallback triggered - this is critical!");
        
        FallbackResponse response = FallbackResponse.builder()
                .error("SECURITY_SERVICE_UNAVAILABLE")
                .message("Security service is temporarily unavailable. Authentication and authorization may be affected.")
                .timestamp(Instant.now())
                .service("mcp-security")
                .suggestion("Contact system administrator immediately.")
                .retryAfter(15)
                .build();
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    /**
     * Generic fallback for any service
     */
    @GetMapping("/{service}")
    public Mono<ResponseEntity<FallbackResponse>> genericFallback(@PathVariable String service) {
        log.warn("Generic fallback triggered for service: {}", service);
        
        FallbackResponse response = FallbackResponse.builder()
                .error("SERVICE_UNAVAILABLE")
                .message(String.format("Service '%s' is temporarily unavailable.", service))
                .timestamp(Instant.now())
                .service(service)
                .suggestion("Please try again later or contact support.")
                .retryAfter(30)
                .build();
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    /**
     * Health check fallback
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> healthFallback() {
        log.info("Health check fallback triggered");
        
        Map<String, Object> health = Map.of(
                "status", "DEGRADED",
                "timestamp", Instant.now(),
                "message", "Running in fallback mode - some services may be unavailable",
                "services", Map.of(
                        "sidecar", "UP",
                        "downstream", "DEGRADED"
                )
        );
        
        return Mono.just(ResponseEntity.ok(health));
    }

    /**
     * Fallback response model
     */
    public static class FallbackResponse {
        private String error;
        private String message;
        private Instant timestamp;
        private String service;
        private String suggestion;
        private Integer retryAfter;

        public static FallbackResponseBuilder builder() {
            return new FallbackResponseBuilder();
        }

        // Getters and setters
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
        
        public String getService() { return service; }
        public void setService(String service) { this.service = service; }
        
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
        
        public Integer getRetryAfter() { return retryAfter; }
        public void setRetryAfter(Integer retryAfter) { this.retryAfter = retryAfter; }

        public static class FallbackResponseBuilder {
            private String error;
            private String message;
            private Instant timestamp;
            private String service;
            private String suggestion;
            private Integer retryAfter;

            public FallbackResponseBuilder error(String error) {
                this.error = error;
                return this;
            }

            public FallbackResponseBuilder message(String message) {
                this.message = message;
                return this;
            }

            public FallbackResponseBuilder timestamp(Instant timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public FallbackResponseBuilder service(String service) {
                this.service = service;
                return this;
            }

            public FallbackResponseBuilder suggestion(String suggestion) {
                this.suggestion = suggestion;
                return this;
            }

            public FallbackResponseBuilder retryAfter(Integer retryAfter) {
                this.retryAfter = retryAfter;
                return this;
            }

            public FallbackResponse build() {
                FallbackResponse response = new FallbackResponse();
                response.error = this.error;
                response.message = this.message;
                response.timestamp = this.timestamp;
                response.service = this.service;
                response.suggestion = this.suggestion;
                response.retryAfter = this.retryAfter;
                return response;
            }
        }
    }
}