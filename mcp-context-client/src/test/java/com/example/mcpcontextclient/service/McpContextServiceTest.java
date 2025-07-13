package com.example.mcpcontextclient.service;

import com.example.mcpcontextclient.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.Collections;

public class McpContextServiceTest {

    private static MockWebServer mockWebServer;
    private McpContextService mcpContextService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @BeforeEach
    void initialize() {
        String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
        mcpContextService = new McpContextService(WebClient.builder(), baseUrl);
    }

    @Test
    void testCreateContext() throws JsonProcessingException {
        Context context = new Context();
        context.setId("123");
        context.setName("Test Context");

        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(context))
                .addHeader("Content-Type", "application/json"));

        CreateContextRequest request = new CreateContextRequest("ns1", "Test Context", "Test Description", Collections.emptyList());

        Mono<Context> contextMono = mcpContextService.createContext(request);

        StepVerifier.create(contextMono)
                .expectNextMatches(c -> c.getId().equals("123"))
                .verifyComplete();
    }

    @Test
    void testAppendToContext() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        AppendMessagesRequest request = new AppendMessagesRequest("123", Collections.singletonList(new Message("user", "Hello")));

        Mono<Void> result = mcpContextService.appendToContext(request);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void testGetContextWindow() throws JsonProcessingException {
        ContextWindow contextWindow = new ContextWindow();
        contextWindow.setTokenCount(10);
        contextWindow.setMessages(Collections.singletonList(new Message("user", "Hello")));

        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(contextWindow))
                .addHeader("Content-Type", "application/json"));

        GetContextWindowRequest request = new GetContextWindowRequest("123", 100, "full");

        Mono<ContextWindow> contextWindowMono = mcpContextService.getContextWindow(request);

        StepVerifier.create(contextWindowMono)
                .expectNextMatches(cw -> cw.getTokenCount() == 10)
                .verifyComplete();
    }

    @Test
    void testShareContext() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        ShareContextRequest request = new ShareContextRequest("123", "org2", "read", 24);

        Mono<Void> result = mcpContextService.shareContext(request);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void testSearchContexts() throws JsonProcessingException {
        Context context = new Context();
        context.setId("123");
        context.setName("Test Context");

        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(Collections.singletonList(context)))
                .addHeader("Content-Type", "application/json"));

        SearchContextsRequest request = new SearchContextsRequest("Test", "ns1", 10, true);

        StepVerifier.create(mcpContextService.searchContexts(request))
                .expectNextMatches(c -> c.getId().equals("123"))
                .verifyComplete();
    }
}
