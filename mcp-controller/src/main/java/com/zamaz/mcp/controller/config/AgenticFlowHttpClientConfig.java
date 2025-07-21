package com.zamaz.mcp.controller.config;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

/**
 * HTTP client configuration with connection pooling for external service calls.
 */
@Configuration
public class AgenticFlowHttpClientConfig {
    
    @Value("${agentic-flow.http.max-total-connections:200}")
    private int maxTotalConnections;
    
    @Value("${agentic-flow.http.max-connections-per-route:20}")
    private int maxConnectionsPerRoute;
    
    @Value("${agentic-flow.http.connection-timeout-ms:5000}")
    private int connectionTimeout;
    
    @Value("${agentic-flow.http.socket-timeout-ms:30000}")
    private int socketTimeout;
    
    @Value("${agentic-flow.http.request-timeout-ms:30000}")
    private int requestTimeout;
    
    @Bean
    public PoolingHttpClientConnectionManager connectionManager() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(maxTotalConnections);
        connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);
        
        // Configure specific routes if needed (e.g., for LLM services)
        // HttpHost llmHost = new HttpHost("llm-service", 5002);
        // connectionManager.setMaxPerRoute(new HttpRoute(llmHost), 50);
        
        // Validate connections after inactivity
        connectionManager.setValidateAfterInactivity(5000);
        
        return connectionManager;
    }
    
    @Bean
    public RequestConfig requestConfig() {
        return RequestConfig.custom()
            .setConnectTimeout(connectionTimeout)
            .setSocketTimeout(socketTimeout)
            .setConnectionRequestTimeout(requestTimeout)
            .build();
    }
    
    @Bean
    public CloseableHttpClient httpClient(
            PoolingHttpClientConnectionManager connectionManager,
            RequestConfig requestConfig) {
        
        return HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .setConnectionTimeToLive(30, TimeUnit.SECONDS)
            .evictExpiredConnections()
            .evictIdleConnections(30, TimeUnit.SECONDS)
            .build();
    }
    
    @Bean(name = "agenticFlowRestTemplate")
    public RestTemplate agenticFlowRestTemplate(CloseableHttpClient httpClient) {
        HttpComponentsClientHttpRequestFactory factory = 
            new HttpComponentsClientHttpRequestFactory(httpClient);
        
        RestTemplate restTemplate = new RestTemplate(factory);
        
        // Add interceptors for logging, retry, etc. if needed
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("X-Agentic-Flow", "true");
            return execution.execute(request, body);
        });
        
        return restTemplate;
    }
    
    @Bean(name = "llmServiceRestTemplate")
    public RestTemplate llmServiceRestTemplate(CloseableHttpClient httpClient) {
        HttpComponentsClientHttpRequestFactory factory = 
            new HttpComponentsClientHttpRequestFactory(httpClient);
        
        // Set specific timeouts for LLM calls (they might take longer)
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(60000);
        
        return new RestTemplate(factory);
    }
}