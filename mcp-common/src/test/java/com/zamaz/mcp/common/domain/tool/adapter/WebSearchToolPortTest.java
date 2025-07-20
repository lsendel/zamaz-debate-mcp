package com.zamaz.mcp.common.domain.tool.adapter;

import com.zamaz.mcp.common.domain.tool.ToolCall;
import com.zamaz.mcp.common.domain.tool.ToolResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WebSearchToolPortTest {

    private WebSearchToolPort webSearchTool;

    @BeforeEach
    void setUp() {
        webSearchTool = new WebSearchToolPort();
    }

    @Test
    void testGetToolName() {
        assertEquals("web_search", webSearchTool.getToolName());
    }

    @Test
    void testSuccessfulSearch() {
        ToolCall toolCall = new ToolCall("web_search", Map.of("query", "artificial intelligence"));
        
        ToolResponse response = webSearchTool.executeToolCall(toolCall);
        
        assertTrue(response.isSuccess());
        assertNotNull(response.getResult());
        assertTrue(response.getResult().toString().contains("artificial intelligence"));
        assertTrue(response.getResult().toString().contains("Web Search Results"));
        assertEquals("web_search", response.getToolName());
    }

    @Test
    void testWeatherSearch() {
        ToolCall toolCall = new ToolCall("web_search", Map.of("query", "weather today"));
        
        ToolResponse response = webSearchTool.executeToolCall(toolCall);
        
        assertTrue(response.isSuccess());
        assertTrue(response.getResult().toString().contains("weather"));
        assertTrue(response.getResult().toString().contains("Temperature"));
        assertTrue(response.getResult().toString().contains("°F"));
    }

    @Test
    void testStockSearch() {
        ToolCall toolCall = new ToolCall("web_search", Map.of("query", "stock market prices"));
        
        ToolResponse response = webSearchTool.executeToolCall(toolCall);
        
        assertTrue(response.isSuccess());
        assertTrue(response.getResult().toString().contains("Stock"));
        assertTrue(response.getResult().toString().contains("Market"));
    }

    @Test
    void testNewsSearch() {
        ToolCall toolCall = new ToolCall("web_search", Map.of("query", "latest news technology"));
        
        ToolResponse response = webSearchTool.executeToolCall(toolCall);
        
        assertTrue(response.isSuccess());
        assertTrue(response.getResult().toString().contains("News"));
        assertTrue(response.getResult().toString().contains("technology"));
    }

    @Test
    void testGenericSearch() {
        ToolCall toolCall = new ToolCall("web_search", Map.of("query", "machine learning algorithms"));
        
        ToolResponse response = webSearchTool.executeToolCall(toolCall);
        
        assertTrue(response.isSuccess());
        assertTrue(response.getResult().toString().contains("machine learning algorithms"));
        assertTrue(response.getResult().toString().contains("Wikipedia"));
    }

    @Test
    void testMissingQueryParameter() {
        ToolCall toolCall = new ToolCall("web_search", Map.of());
        
        ToolResponse response = webSearchTool.executeToolCall(toolCall);
        
        assertFalse(response.isSuccess());
        assertNotNull(response.getErrorMessage());
        assertTrue(response.getErrorMessage().contains("Query parameter is required"));
    }

    @Test
    void testEmptyQuery() {
        ToolCall toolCall = new ToolCall("web_search", Map.of("query", ""));
        
        ToolResponse response = webSearchTool.executeToolCall(toolCall);
        
        assertFalse(response.isSuccess());
        assertTrue(response.getErrorMessage().contains("Query parameter cannot be empty"));
    }

    @Test
    void testNullQuery() {
        ToolCall toolCall = new ToolCall("web_search", Map.of("query", null));
        
        ToolResponse response = webSearchTool.executeToolCall(toolCall);
        
        assertFalse(response.isSuccess());
        assertTrue(response.getErrorMessage().contains("Query parameter cannot be empty"));
    }

    @Test
    void testCanHandle() {
        ToolCall validCall = new ToolCall("web_search", Map.of("query", "test query"));
        ToolCall invalidTool = new ToolCall("calculator", Map.of("query", "2 + 2"));
        ToolCall missingQuery = new ToolCall("web_search", Map.of("other", "value"));
        
        assertTrue(webSearchTool.canHandle(validCall));
        assertFalse(webSearchTool.canHandle(invalidTool));
        assertFalse(webSearchTool.canHandle(missingQuery));
    }

    @Test
    void testResponseMetadata() {
        ToolCall toolCall = new ToolCall("web_search", Map.of("query", "test search"));
        
        ToolResponse response = webSearchTool.executeToolCall(toolCall);
        
        assertTrue(response.isSuccess());
        
        // Check metadata
        Map<String, Object> metadata = response.getMetadata();
        assertEquals("test search", metadata.get("query"));
        assertEquals("simulated", metadata.get("search_type"));
        assertNotNull(metadata.get("execution_time_ms"));
        assertTrue((Long) metadata.get("execution_time_ms") >= 0);
    }

    @Test
    void testSimulatedResults() {
        ToolCall toolCall = new ToolCall("web_search", Map.of("query", "quantum computing"));
        
        ToolResponse response = webSearchTool.executeToolCall(toolCall);
        
        assertTrue(response.isSuccess());
        String result = response.getResult().toString();
        
        // Should contain disclaimer about simulated results
        assertTrue(result.contains("simulated search results"));
        assertTrue(result.contains("demonstration purposes"));
        
        // Should contain the search query
        assertTrue(result.contains("quantum computing"));
        
        // Should contain structured results
        assertTrue(result.contains("1."));
        assertTrue(result.contains("URL:"));
    }

    @Test
    void testExecutionTime() {
        ToolCall toolCall = new ToolCall("web_search", Map.of("query", "performance test"));
        
        long startTime = System.currentTimeMillis();
        ToolResponse response = webSearchTool.executeToolCall(toolCall);
        long endTime = System.currentTimeMillis();
        
        assertTrue(response.isSuccess());
        
        // Execution should take some time due to simulated delay
        long actualDuration = endTime - startTime;
        Long metadataDuration = (Long) response.getMetadata().get("execution_time_ms");
        
        assertNotNull(metadataDuration);
        assertTrue(metadataDuration > 0);
        assertTrue(actualDuration >= metadataDuration);
    }
}