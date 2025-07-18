package com.zamaz.mcp.llm.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for WebClient instances used by external provider adapters.
 */
@Slf4j
@Configuration
public class WebClientConfig {

    private final LlmProperties llmProperties;

    public WebClientConfig(LlmProperties llmProperties) {
        this.llmProperties = llmProperties;
    }

    @Bean
    @Qualifier("claudeWebClient")
    public WebClient claudeWebClient() {
        var providerConfig = llmProperties.getProviders().get("claude");
        if (providerConfig == null) {
            throw new IllegalStateException("Claude provider configuration not found");
        }

        String baseUrl = providerConfig.getBaseUrl() != null ? 
                providerConfig.getBaseUrl() : "https://api.anthropic.com";
        
        Duration timeout = providerConfig.getTimeout() != null ? 
                providerConfig.getTimeout() : Duration.ofSeconds(30);

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) timeout.toMillis())
                .doOnConnected(conn -> 
                    conn.addHandlerLast(new ReadTimeoutHandler(timeout.toSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeout.toSeconds(), TimeUnit.SECONDS)));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest("Claude"))
                .filter(logResponse("Claude"))
                .filter(errorHandling("Claude"))
                .build();
    }

    @Bean
    @Qualifier("openaiWebClient")
    public WebClient openaiWebClient() {
        var providerConfig = llmProperties.getProviders().get("openai");
        if (providerConfig == null) {
            throw new IllegalStateException("OpenAI provider configuration not found");
        }

        String baseUrl = providerConfig.getBaseUrl() != null ? 
                providerConfig.getBaseUrl() : "https://api.openai.com";
        
        Duration timeout = providerConfig.getTimeout() != null ? 
                providerConfig.getTimeout() : Duration.ofSeconds(30);

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) timeout.toMillis())
                .doOnConnected(conn -> 
                    conn.addHandlerLast(new ReadTimeoutHandler(timeout.toSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeout.toSeconds(), TimeUnit.SECONDS)));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest("OpenAI"))
                .filter(logResponse("OpenAI"))
                .filter(errorHandling("OpenAI"))
                .build();
    }

    @Bean
    @Qualifier("geminiWebClient")
    public WebClient geminiWebClient() {
        var providerConfig = llmProperties.getProviders().get("gemini");
        if (providerConfig == null) {
            log.warn("Gemini provider configuration not found, creating default WebClient");
            providerConfig = createDefaultProviderConfig();
        }

        String baseUrl = providerConfig.getBaseUrl() != null ? 
                providerConfig.getBaseUrl() : "https://generativelanguage.googleapis.com";
        
        Duration timeout = providerConfig.getTimeout() != null ? 
                providerConfig.getTimeout() : Duration.ofSeconds(30);

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) timeout.toMillis())
                .doOnConnected(conn -> 
                    conn.addHandlerLast(new ReadTimeoutHandler(timeout.toSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeout.toSeconds(), TimeUnit.SECONDS)));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest("Gemini"))
                .filter(logResponse("Gemini"))
                .filter(errorHandling("Gemini"))
                .build();
    }

    @Bean
    @Qualifier("ollamaWebClient")
    public WebClient ollamaWebClient() {
        var providerConfig = llmProperties.getProviders().get("ollama");
        if (providerConfig == null) {
            log.warn("Ollama provider configuration not found, creating default WebClient");
            providerConfig = createDefaultProviderConfig();
        }

        String baseUrl = providerConfig.getBaseUrl() != null ? 
                providerConfig.getBaseUrl() : System.getenv("OLLAMA_ENDPOINT");
        
        Duration timeout = providerConfig.getTimeout() != null ? 
                providerConfig.getTimeout() : Duration.ofMinutes(2); // Ollama can be slower

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) timeout.toMillis())
                .doOnConnected(conn -> 
                    conn.addHandlerLast(new ReadTimeoutHandler(timeout.toSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeout.toSeconds(), TimeUnit.SECONDS)));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest("Ollama"))
                .filter(logResponse("Ollama"))
                .filter(errorHandling("Ollama"))
                .build();
    }

    /**
     * Generic WebClient for any provider that doesn't have specific configuration.
     */
    @Bean
    @Qualifier("genericWebClient")
    public WebClient genericWebClient() {
        Duration timeout = Duration.ofSeconds(30);

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) timeout.toMillis())
                .doOnConnected(conn -> 
                    conn.addHandlerLast(new ReadTimeoutHandler(timeout.toSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeout.toSeconds(), TimeUnit.SECONDS)));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest("Generic"))
                .filter(logResponse("Generic"))
                .filter(errorHandling("Generic"))
                .build();
    }

    private ExchangeFilterFunction logRequest(String providerName) {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (log.isDebugEnabled()) {
                log.debug("[{}] Request: {} {}", 
                        providerName, 
                        clientRequest.method(), 
                        clientRequest.url());
                
                clientRequest.headers().forEach((name, values) -> {
                    if (!name.toLowerCase().contains("authorization") && 
                        !name.toLowerCase().contains("key")) {
                        log.debug("[{}] Header: {}={}", providerName, name, values);
                    }
                });
            }
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse(String providerName) {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (log.isDebugEnabled()) {
                log.debug("[{}] Response: {} {}", 
                        providerName, 
                        clientResponse.statusCode(), 
                        clientResponse.headers().asHttpHeaders().getContentType());
            }
            return Mono.just(clientResponse);
        });
    }

    private ExchangeFilterFunction errorHandling(String providerName) {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().isError()) {
                log.error("[{}] HTTP Error: {} {}", 
                        providerName, 
                        clientResponse.statusCode().value(),
                        clientResponse.statusCode().getReasonPhrase());
            }
            return Mono.just(clientResponse);
        });
    }

    private LlmProperties.ProviderConfig createDefaultProviderConfig() {
        var config = new LlmProperties.ProviderConfig();
        config.setEnabled(false);
        config.setTimeout(Duration.ofSeconds(30));
        return config;
    }
}