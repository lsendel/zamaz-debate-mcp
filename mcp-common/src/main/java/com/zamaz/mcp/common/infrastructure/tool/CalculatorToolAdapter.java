package com.zamaz.mcp.common.infrastructure.tool;

import com.zamaz.mcp.common.domain.tool.ExternalToolPort;
import com.zamaz.mcp.common.domain.tool.ToolCall;
import com.zamaz.mcp.common.domain.tool.ToolResponse;
import com.zamaz.mcp.common.exception.ToolCallException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Adapter for the calculator tool.
 * Evaluates mathematical expressions using the JavaScript engine.
 */
@Service
public class CalculatorToolAdapter implements ExternalToolPort {
    private static final Logger logger = LoggerFactory.getLogger(CalculatorToolAdapter.class);
    private final ScriptEngine scriptEngine;

    public CalculatorToolAdapter() {
        this.scriptEngine = new ScriptEngineManager().getEngineByName("JavaScript");
    }

    @Override
    public String getToolName() {
        return "calculator";
    }

    @Override
    public ToolResponse executeToolCall(ToolCall toolCall) {
        if (!canHandle(toolCall)) {
            throw new ToolCallException("Unsupported tool: " + toolCall.getTool());
        }

        String expression = toolCall.getParameters().get("query").toString();
        logger.info("Evaluating expression: {}", expression);

        try {
            // Sanitize the expression to prevent code execution
            String sanitizedExpression = sanitizeExpression(expression);

            // Evaluate the expression
            Object result = scriptEngine.eval(sanitizedExpression);

            return ToolResponse.builder()
                    .toolName(getToolName())
                    .result(result)
                    .success(true)
                    .timestamp(Instant.now())
                    .addMetadata("expression", expression)
                    .addMetadata("sanitized_expression", sanitizedExpression)
                    .build();
        } catch (ScriptException e) {
            logger.error("Error evaluating expression", e);
            return ToolResponse.builder()
                    .toolName(getToolName())
                    .success(false)
                    .errorMessage("Error evaluating expression: " + e.getMessage())
                    .timestamp(Instant.now())
                    .addMetadata("expression", expression)
                    .build();
        }
    }

    /**
     * Sanitizes the expression to prevent code execution.
     * Only allows basic mathematical operations.
     *
     * @param expression The expression to sanitize
     * @return The sanitized expression
     * @throws ToolCallException if the expression contains disallowed characters
     */
    private String sanitizeExpression(String expression) {
        // Remove all whitespace
        String sanitized = expression.replaceAll("\\s+", "");

        // Check for disallowed characters
        if (!sanitized.matches("^[0-9+\\-*/().\\s]*$")) {
            throw new ToolCallException("Expression contains disallowed characters: " + expression);
        }

        return sanitized;
    }
}