package com.example.mcpcontextclient.service;

import com.example.mcpcontextclient.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class McpContextService {

    private final WebClient webClient;

    public McpContextService(WebClient.Builder webClientBuilder, @Value("${mcp.context.client.baseUrl}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    public Mono<Context> createContext(CreateContextRequest request) {
        return webClient.post()
                .uri("/contexts")
                .body(Mono.just(request), CreateContextRequest.class)
                .retrieve()
                .bodyToMono(Context.class);
    }

    public Mono<Void> appendToContext(AppendMessagesRequest request) {
        return webClient.post()
                .uri("/contexts/append")
                .body(Mono.just(request), AppendMessagesRequest.class)
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<ContextWindow> getContextWindow(GetContextWindowRequest request) {
        return webClient.post()
                .uri("/contexts/window")
                .body(Mono.just(request), GetContextWindowRequest.class)
                .retrieve()
                .bodyToMono(ContextWindow.class);
    }

    public Mono<Void> shareContext(ShareContextRequest request) {
        return webClient.post()
                .uri("/contexts/share")
                .body(Mono.just(request), ShareContextRequest.class)
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Flux<Context> searchContexts(SearchContextsRequest request) {
        return webClient.post()
                .uri("/contexts/search")
                .body(Mono.just(request), SearchContextsRequest.class)
                .retrieve()
                .bodyToFlux(Context.class);
    }
}
