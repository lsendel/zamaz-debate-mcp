package com.zamaz.mcp.common.domain.tool.adapter;

import com.zamaz.mcp.common.domain.tool.ExternalToolPort;
import com.zamaz.mcp.common.domain.tool.ToolCall;
import com.zamaz.mcp.common.domain.tool.ToolResponse;
import com.zamaz.mcp.common.exception.ToolCallException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * External tool port implementation for web search functionality.
 * This is a placeholder implementation that simulates web search.
 * In a production environment, this would integrate with a real search API.
 */
@Component
public class WebSearchToolPort implements ExternalToolPort {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSearchToolPort.class);
    private static final String TOOL_NAME = "web_search";

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Override
    public ToolResponse executeToolCall(ToolCall toolCall) {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("Executing web search tool call: {}", toolCall);
            
            // Validate parameters
            if (!toolCall.getParameters().containsKey("query")) {
                throw new ToolCallException("Query parameter is required for web search");
            }
            
            String query = (String) toolCall.getParameters().get("query");
            if (query == null || query.trim().isEmpty()) {
                throw new ToolCallException("Query parameter cannot be empty");
            }
            
            // Simulate web search - in production, this would call a real search API
            String searchResult = performWebSearch(query);
            long executionTime = System.currentTimeMillis() - startTime;
            
            return ToolResponse.builder()
                    .toolName(TOOL_NAME)
                    .result(searchResult)
                    .success(true)
                    .timestamp(Instant.now())
                    .addMetadata("query", query)
                    .addMetadata("execution_time_ms", executionTime)
                    .addMetadata("search_type", "simulated")
                    .build();
                    
        } catch (ToolCallException e) {
            logger.error("Web search tool call failed: {}", e.getMessage());
            long executionTime = System.currentTimeMillis() - startTime;
            
            return ToolResponse.builder()
                    .toolName(TOOL_NAME)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .timestamp(Instant.now())
                    .addMetadata("execution_time_ms", executionTime)
                    .addMetadata("query", toolCall.getParameters().get("query"))
                    .build();
        } catch (Exception e) {
            logger.error("Unexpected error in web search tool: {}", e.getMessage(), e);
            long executionTime = System.currentTimeMillis() - startTime;
            
            return ToolResponse.builder()
                    .toolName(TOOL_NAME)
                    .success(false)
                    .errorMessage("Web search failed: " + e.getMessage())
                    .timestamp(Instant.now())
                    .addMetadata("execution_time_ms", executionTime)
                    .addMetadata("query", toolCall.getParameters().get("query"))
                    .build();
        }
    }

    @Override
    public boolean canHandle(ToolCall toolCall) {
        return TOOL_NAME.equals(toolCall.getTool()) && 
               toolCall.getParameters().containsKey("query");
    }

    /**
     * Simulates a web search for the given query.
     * In a production environment, this would integrate with search APIs like:
     * - Google Search API
     * - Bing Search API
     * - DuckDuckGo API
     * - Custom search engines
     *
     * @param query The search query
     * @return Simulated search results
     */
    private String performWebSearch(String query) {
        // Simulate processing time
        try {
            Thread.sleep(100 + (long)(Math.random() * 500)); // 100-600ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        StringBuilder results = new StringBuilder();
        results.append("Web Search Results for: \"").append(query).append("\"\n\n");
        
        // Generate simulated search results based on query keywords
        if (query.toLowerCase().contains("weather")) {
            results.append(generateWeatherResults(query));
        } else if (query.toLowerCase().contains("stock") || query.toLowerCase().contains("price")) {
            results.append(generateStockResults(query));
        } else if (query.toLowerCase().contains("news")) {
            results.append(generateNewsResults(query));
        } else {
            results.append(generateGenericResults(query));
        }
        
        results.append("\n[Note: These are simulated search results for demonstration purposes. ");
        results.append("In a production environment, this would return real web search data.]");
        
        return results.toString();
    }

    private String generateWeatherResults(String query) {
        return """
            1. Current Weather Conditions
               URL: https://weather.com/current-conditions
               Temperature: 72°F (22°C), Partly Cloudy
               Wind: 8 mph SW, Humidity: 65%
               
            2. Weather Forecast - 7 Day
               URL: https://weather.gov/forecast
               Today: Partly cloudy, high 75°F
               Tomorrow: Sunny, high 78°F
               
            3. Weather.com - Hourly Forecast
               URL: https://weather.com/hourly
               Detailed hourly conditions and precipitation chances
            """;
    }

    private String generateStockResults(String query) {
        return """
            1. Stock Market Today - Live Updates
               URL: https://finance.yahoo.com/live
               Major indices mixed, tech stocks gaining
               
            2. Real-time Stock Quotes
               URL: https://bloomberg.com/markets
               Live market data and analysis
               
            3. Stock Analysis & Reports
               URL: https://seekingalpha.com
               Professional stock analysis and recommendations
            """;
    }

    private String generateNewsResults(String query) {
        return """
            1. Breaking News - Latest Updates
               URL: https://news.reuters.com/latest
               Current breaking news and developing stories
               
            2. World News Headlines
               URL: https://bbc.com/news/world
               International news coverage and analysis
               
            3. Technology News
               URL: https://techcrunch.com/latest
               Latest technology news and startup coverage
            """;
    }

    private String generateGenericResults(String query) {
        return String.format("""
            1. %s - Overview and Information
               URL: https://example.com/overview
               Comprehensive information about %s including
               definitions, explanations, and related topics.
               
            2. %s - Wikipedia Article
               URL: https://wikipedia.org/wiki/%s
               Detailed encyclopedia entry with references
               and historical information.
               
            3. %s - Expert Analysis
               URL: https://experts.com/analysis
               Professional insights and expert opinions
               on %s and related subjects.
               
            4. %s - Latest News and Updates
               URL: https://news.com/latest/%s
               Recent news articles and developments
               related to %s.
            """, 
            toTitleCase(query), query,
            toTitleCase(query), query.replace(" ", "_"),
            toTitleCase(query), query,
            toTitleCase(query), query.replace(" ", "-"),
            query);
    }

    private String toTitleCase(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String[] words = text.split("\\s+");
        StringBuilder titleCase = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                titleCase.append(Character.toUpperCase(word.charAt(0)))
                         .append(word.substring(1).toLowerCase())
                         .append(" ");
            }
        }
        
        return titleCase.toString().trim();
    }
}