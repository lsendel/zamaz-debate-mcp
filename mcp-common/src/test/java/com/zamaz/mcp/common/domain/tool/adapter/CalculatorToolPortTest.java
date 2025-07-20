package com.zamaz.mcp.common.domain.tool.adapter;

import com.zamaz.mcp.common.domain.tool.ToolCall;
import com.zamaz.mcp.common.domain.tool.ToolResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalculatorToolPortTest {

    private CalculatorToolPort calculatorTool;

    @BeforeEach
    void setUp() {
        calculatorTool = new CalculatorToolPort();
    }

    @Test
    void testGetToolName() {
        assertEquals("calculator", calculatorTool.getToolName());
    }

    @Test
    void testSimpleCalculation() {
        ToolCall toolCall = new ToolCall("calculator", Map.of("query", "2 + 2"));
        
        ToolResponse response = calculatorTool.executeToolCall(toolCall);
        
        assertTrue(response.isSuccess());
        assertEquals("4", response.getResult());
        assertEquals("calculator", response.getToolName());
    }

    @Test
    void testComplexCalculation() {
        ToolCall toolCall = new ToolCall("calculator", Map.of("query", "Math.sqrt(16) + Math.pow(2, 3)"));
        
        ToolResponse response = calculatorTool.executeToolCall(toolCall);
        
        assertTrue(response.isSuccess());
        assertEquals("12", response.getResult());
    }

    @Test
    void testFloatingPointCalculation() {
        ToolCall toolCall = new ToolCall("calculator", Map.of("query", "3.14159 * 2"));
        
        ToolResponse response = calculatorTool.executeToolCall(toolCall);
        
        assertTrue(response.isSuccess());
        assertTrue(response.getResult().toString().startsWith("6.28"));
    }

    @Test
    void testTrigonometricFunctions() {
        ToolCall toolCall = new ToolCall("calculator", Map.of("query", "sin(0) + cos(0)"));
        
        ToolResponse response = calculatorTool.executeToolCall(toolCall);
        
        assertTrue(response.isSuccess());
        assertEquals("1", response.getResult());
    }

    @Test
    void testInvalidExpression() {
        ToolCall toolCall = new ToolCall("calculator", Map.of("query", "2 + + 2"));
        
        ToolResponse response = calculatorTool.executeToolCall(toolCall);
        
        assertFalse(response.isSuccess());
        assertNotNull(response.getErrorMessage());
        assertTrue(response.getErrorMessage().contains("evaluation failed"));
    }

    @Test
    void testSecurityViolation() {
        ToolCall toolCall = new ToolCall("calculator", Map.of("query", "function() { return 42; }"));
        
        ToolResponse response = calculatorTool.executeToolCall(toolCall);
        
        assertFalse(response.isSuccess());
        assertNotNull(response.getErrorMessage());
        assertTrue(response.getErrorMessage().contains("Invalid mathematical expression"));
    }

    @Test
    void testMissingQueryParameter() {
        ToolCall toolCall = new ToolCall("calculator", Map.of());
        
        ToolResponse response = calculatorTool.executeToolCall(toolCall);
        
        assertFalse(response.isSuccess());
        assertTrue(response.getErrorMessage().contains("Query parameter is required"));
    }

    @Test
    void testEmptyQuery() {
        ToolCall toolCall = new ToolCall("calculator", Map.of("query", ""));
        
        ToolResponse response = calculatorTool.executeToolCall(toolCall);
        
        assertFalse(response.isSuccess());
        assertTrue(response.getErrorMessage().contains("Expression cannot be empty"));
    }

    @Test
    void testCanHandle() {
        ToolCall validCall = new ToolCall("calculator", Map.of("query", "2 + 2"));
        ToolCall invalidTool = new ToolCall("web_search", Map.of("query", "test"));
        ToolCall missingQuery = new ToolCall("calculator", Map.of("other", "value"));
        
        assertTrue(calculatorTool.canHandle(validCall));
        assertFalse(calculatorTool.canHandle(invalidTool));
        assertFalse(calculatorTool.canHandle(missingQuery));
    }

    @Test
    void testDivisionByZero() {
        ToolCall toolCall = new ToolCall("calculator", Map.of("query", "1 / 0"));
        
        ToolResponse response = calculatorTool.executeToolCall(toolCall);
        
        assertFalse(response.isSuccess());
        assertTrue(response.getErrorMessage().contains("infinite"));
    }

    @Test
    void testResponseMetadata() {
        ToolCall toolCall = new ToolCall("calculator", Map.of("query", "5 * 5"));
        
        ToolResponse response = calculatorTool.executeToolCall(toolCall);
        
        assertTrue(response.isSuccess());
        assertEquals("25", response.getResult());
        
        // Check metadata
        Map<String, Object> metadata = response.getMetadata();
        assertEquals("5 * 5", metadata.get("expression"));
        assertEquals(25, metadata.get("result_value"));
        assertNotNull(metadata.get("execution_time_ms"));
    }
}