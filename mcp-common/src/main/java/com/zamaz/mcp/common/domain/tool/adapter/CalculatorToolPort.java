package com.zamaz.mcp.common.domain.tool.adapter;

import com.zamaz.mcp.common.domain.tool.ExternalToolPort;
import com.zamaz.mcp.common.domain.tool.ToolCall;
import com.zamaz.mcp.common.domain.tool.ToolResponse;
import com.zamaz.mcp.common.exception.ToolCallException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.time.Instant;
import java.util.regex.Pattern;

/**
 * External tool port implementation for mathematical calculations.
 * Provides safe mathematical expression evaluation using JavaScript engine.
 */
@Component
public class CalculatorToolPort implements ExternalToolPort {
    
    private static final Logger logger = LoggerFactory.getLogger(CalculatorToolPort.class);
    private static final String TOOL_NAME = "calculator";
    
    // Pattern to validate mathematical expressions
    private static final Pattern SAFE_MATH_PATTERN = Pattern.compile(
        "^[0-9+\\-*/().\\s,MathEPIabcdefghijklmnopqrstuvwxyz]*$"
    );
    
    // Forbidden keywords for security
    private static final String[] FORBIDDEN_KEYWORDS = {
        "function", "var", "let", "const", "return", "if", "else", "for", "while", 
        "do", "switch", "case", "break", "continue", "try", "catch", "throw",
        "new", "delete", "typeof", "instanceof", "void", "with", "eval",
        "document", "window", "global", "process", "require", "import", "export"
    };
    
    private final ScriptEngine scriptEngine;

    public CalculatorToolPort() {
        this.scriptEngine = new ScriptEngineManager().getEngineByName("JavaScript");
        if (scriptEngine == null) {
            logger.warn("JavaScript engine not available, calculator functionality will be limited");
        }
    }

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Override
    public ToolResponse executeToolCall(ToolCall toolCall) {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("Executing calculator tool call: {}", toolCall);
            
            // Validate parameters
            if (!toolCall.getParameters().containsKey("query")) {
                throw new ToolCallException("Query parameter is required for calculator");
            }
            
            String expression = (String) toolCall.getParameters().get("query");
            if (expression == null || expression.trim().isEmpty()) {
                throw new ToolCallException("Expression cannot be empty");
            }
            
            // Validate expression safety
            if (!isValidMathExpression(expression)) {
                throw new ToolCallException("Invalid mathematical expression. Only mathematical operations are allowed.");
            }
            
            // Evaluate the expression
            Object result = evaluateExpression(expression);
            long executionTime = System.currentTimeMillis() - startTime;
            
            return ToolResponse.builder()
                    .toolName(TOOL_NAME)
                    .result(formatResult(result))
                    .success(true)
                    .timestamp(Instant.now())
                    .addMetadata("expression", expression)
                    .addMetadata("result_value", result)
                    .addMetadata("result_type", result.getClass().getSimpleName())
                    .addMetadata("execution_time_ms", executionTime)
                    .build();
                    
        } catch (ToolCallException e) {
            logger.error("Calculator tool call failed: {}", e.getMessage());
            long executionTime = System.currentTimeMillis() - startTime;
            
            return ToolResponse.builder()
                    .toolName(TOOL_NAME)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .timestamp(Instant.now())
                    .addMetadata("execution_time_ms", executionTime)
                    .addMetadata("expression", toolCall.getParameters().get("query"))
                    .build();
        } catch (Exception e) {
            logger.error("Unexpected error in calculator tool: {}", e.getMessage(), e);
            long executionTime = System.currentTimeMillis() - startTime;
            
            return ToolResponse.builder()
                    .toolName(TOOL_NAME)
                    .success(false)
                    .errorMessage("Calculation failed: " + e.getMessage())
                    .timestamp(Instant.now())
                    .addMetadata("execution_time_ms", executionTime)
                    .addMetadata("expression", toolCall.getParameters().get("query"))
                    .build();
        }
    }

    @Override
    public boolean canHandle(ToolCall toolCall) {
        return TOOL_NAME.equals(toolCall.getTool()) && 
               toolCall.getParameters().containsKey("query") &&
               toolCall.getParameters().get("query") instanceof String;
    }

    /**
     * Validates that the expression contains only allowed mathematical operations.
     * This is a security check to prevent execution of arbitrary JavaScript code.
     *
     * @param expression The expression to validate
     * @return True if the expression is safe, false otherwise
     */
    private boolean isValidMathExpression(String expression) {
        String cleanExpression = expression.replaceAll("\\s+", "");
        
        // Check for potentially dangerous keywords
        String lowerExpression = cleanExpression.toLowerCase();
        for (String keyword : FORBIDDEN_KEYWORDS) {
            if (lowerExpression.contains(keyword)) {
                logger.warn("Forbidden keyword '{}' found in expression: {}", keyword, expression);
                return false;
            }
        }
        
        // Only allow mathematical expressions with numbers, operators, and Math functions
        if (!SAFE_MATH_PATTERN.matcher(cleanExpression).matches()) {
            logger.warn("Expression contains invalid characters: {}", expression);
            return false;
        }
        
        return true;
    }

    /**
     * Evaluates a mathematical expression using the JavaScript engine.
     *
     * @param expression The expression to evaluate
     * @return The result of the evaluation
     * @throws ToolCallException if evaluation fails
     */
    private Object evaluateExpression(String expression) throws ToolCallException {
        if (scriptEngine == null) {
            throw new ToolCallException("JavaScript engine not available");
        }
        
        try {
            // Pre-process common mathematical functions
            String processedExpression = preprocessExpression(expression);
            logger.debug("Evaluating expression: {} -> {}", expression, processedExpression);
            
            Object result = scriptEngine.eval(processedExpression);
            
            // Validate result
            if (result instanceof Number) {
                Number numResult = (Number) result;
                if (Double.isNaN(numResult.doubleValue())) {
                    throw new ToolCallException("Result is not a number (NaN)");
                }
                if (Double.isInfinite(numResult.doubleValue())) {
                    throw new ToolCallException("Result is infinite");
                }
            }
            
            return result;
            
        } catch (ScriptException e) {
            throw new ToolCallException("Mathematical expression evaluation failed: " + e.getMessage());
        }
    }

    /**
     * Preprocesses the expression to handle common mathematical functions.
     *
     * @param expression The original expression
     * @return The preprocessed expression
     */
    private String preprocessExpression(String expression) {
        // Handle common mathematical constants and functions
        return expression
            .replaceAll("\\bpi\\b", "Math.PI")
            .replaceAll("\\be\\b", "Math.E")
            .replaceAll("\\bsin\\(", "Math.sin(")
            .replaceAll("\\bcos\\(", "Math.cos(")
            .replaceAll("\\btan\\(", "Math.tan(")
            .replaceAll("\\blog\\(", "Math.log(")
            .replaceAll("\\bln\\(", "Math.log(")
            .replaceAll("\\bsqrt\\(", "Math.sqrt(")
            .replaceAll("\\babs\\(", "Math.abs(")
            .replaceAll("\\bpow\\(", "Math.pow(")
            .replaceAll("\\bfloor\\(", "Math.floor(")
            .replaceAll("\\bceil\\(", "Math.ceil(")
            .replaceAll("\\bround\\(", "Math.round(")
            .replaceAll("\\bmax\\(", "Math.max(")
            .replaceAll("\\bmin\\(", "Math.min(")
            .replaceAll("\\bexp\\(", "Math.exp(")
            .replaceAll("\\^", "**"); // Handle exponentiation
    }

    /**
     * Formats the calculation result for display.
     *
     * @param result The calculation result
     * @return Formatted result string
     */
    private String formatResult(Object result) {
        if (result instanceof Number) {
            Number numResult = (Number) result;
            double doubleValue = numResult.doubleValue();
            
            // Format based on the value
            if (doubleValue == Math.floor(doubleValue) && !Double.isInfinite(doubleValue)) {
                // Integer result
                return String.valueOf(Math.round(doubleValue));
            } else {
                // Decimal result - limit to reasonable precision
                return String.format("%.10g", doubleValue);
            }
        }
        
        return String.valueOf(result);
    }
}