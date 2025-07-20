package com.zamaz.mcp.common.infrastructure.tool;

import com.zamaz.mcp.common.domain.tool.ExternalToolPort;
import com.zamaz.mcp.common.domain.tool.ToolCall;
import com.zamaz.mcp.common.domain.tool.ToolResponse;
import com.zamaz.mcp.common.exception.ToolCallException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for the web search tool.
 * This is a simplified implementation that returns mock search results.
 */
@Service
public class WebSearchToolAdapter implements ExternalToolPort {
    private static final Logger logger = LoggerFactory.getLogger(WebSearchToolAdapter.class);

    @Override
    public String getToolName() {
        return "web_search";
    }

    @Override
    public ToolResponse executeToolCall(ToolCall toolCall) {
        if (!canHandle(toolCall)) {
            throw new ToolCallException("Unsupported tool: " + toolCall.getTool());
        }

        String query = toolCall.getParameters().get("query").toString();
        logger.info("Executing web search for query: {}", query);

        try {
            // In a real implementation, this would call an actual search API
            List<Map<String, String>> searchResults = performMockSearch(query);

            return ToolResponse.builder()
                    .toolName(getToolName())
                    .result(searchResults)
                    .success(true)
                    .timestamp(Instant.now())
                    .addMetadata("query", query)
                    .addMetadata("result_count", searchResults.size())
                    .build();
        } catch (Exception e) {
            logger.error("Error executing web search", e);
            return ToolResponse.builder()
                    .toolName(getToolName())
                    .success(false)
                    .errorMessage("Error executing web search: " + e.getMessage())
                    .timestamp(Instant.now())
                    .addMetadata("query", query)
                    .build();
        }
    }

    /**
     * Performs a mock search and returns fake results.
     * This is just for demonstration purposes.
     *
     * @param query The search query
     * @return A list of search results
     */
    private List<Map<String, String>> performMockSearch(String query) {
        List<Map<String, String>> results = new ArrayList<>();

        // Generate some mock results based on the query
        String normalizedQuery = query.toLowerCase();

        if (normalizedQuery.contains("ceo") && normalizedQuery.contains("twitter")) {
            Map<String, String> result1 = new HashMap<>();
            result1.put("title", "Linda Yaccarino is the CEO of Twitter (X)");
            result1.put("snippet",
                    "Linda Yaccarino is the current CEO of Twitter (now called X), appointed in June 2023, succeeding Elon Musk who remains as executive chairman and CTO.");
            result1.put("url", "https://en.wikipedia.org/wiki/Linda_Yaccarino");
            results.add(result1);

            Map<String, String> result2 = new HashMap<>();
            result2.put("title", "Twitter Leadership - X Corp");
            result2.put("snippet",
                    "Linda Yaccarino serves as the Chief Executive Officer of X (formerly Twitter), leading the company's business operations.");
            result2.put("url", "https://about.twitter.com/leadership");
            results.add(result2);
        } else if (normalizedQuery.contains("weather") && normalizedQuery.contains("new york")) {
            Map<String, String> result = new HashMap<>();
            result.put("title", "Current Weather in New York City");
            result.put("snippet", "New York City: 72°F, Partly Cloudy, Humidity: 65%, Wind: 8 mph");
            result.put("url", "https://weather.example.com/new-york");
            results.add(result);
        } else if (normalizedQuery.contains("population") && normalizedQuery.contains("japan")) {
            Map<String, String> result = new HashMap<>();
            result.put("title", "Japan Population 2024");
            result.put("snippet",
                    "The current population of Japan is 125.7 million as of 2024, showing a decline from previous years due to low birth rates and aging population.");
            result.put("url", "https://worldpopulationreview.com/countries/japan-population");
            results.add(result);
        } else {
            // Generic results for any other query
            Map<String, String> result = new HashMap<>();
            result.put("title", "Search results for: " + query);
            result.put("snippet", "This is a mock search result for the query: " + query);
            result.put("url", "https://example.com/search?q=" + query.replace(" ", "+"));
            results.add(result);
        }

        return results;
    }
}