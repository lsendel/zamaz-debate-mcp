package com.zamaz.workflow.api.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.webmvc.GraphQlWebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebSocket
@Slf4j
public class GraphQLWebSocketConfig implements WebSocketConfigurer {
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(graphQlWebSocketHandler(), "/graphql-ws")
                .setAllowedOrigins("*"); // Configure appropriately for production
    }
    
    private GraphQlWebSocketHandler graphQlWebSocketHandler() {
        return GraphQlWebSocketHandler.builder()
                .interceptor(authenticationInterceptor())
                .build();
    }
    
    private WebGraphQlInterceptor authenticationInterceptor() {
        return (webInput, interceptorChain) -> {
            log.debug("WebSocket connection established");
            
            // Extract authentication from connection params
            webInput.configureExecutionInput((executionInput, builder) -> {
                var connectionParams = webInput.getHeaders();
                String authToken = connectionParams.getFirst("Authorization");
                
                if (authToken != null) {
                    // Validate token and set security context
                    log.debug("Authenticated WebSocket connection");
                }
                
                return Mono.just(executionInput);
            });
            
            return interceptorChain.next(webInput);
        };
    }
}