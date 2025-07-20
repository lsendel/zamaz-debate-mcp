package com.zamaz.workflow.domain.service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleConditionEvaluator implements WorkflowDomainServiceImpl.ConditionEvaluator {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    
    @Override
    public boolean evaluate(Object conditions, Object data) {
        if (conditions == null || data == null) {
            return false;
        }
        
        if (conditions instanceof Map) {
            Map<String, Object> conditionMap = (Map<String, Object>) conditions;
            String operator = (String) conditionMap.get("operator");
            
            if ("AND".equals(operator)) {
                return evaluateAnd(conditionMap, data);
            } else if ("OR".equals(operator)) {
                return evaluateOr(conditionMap, data);
            } else if ("NOT".equals(operator)) {
                return evaluateNot(conditionMap, data);
            } else {
                return evaluateSimpleCondition(conditionMap, data);
            }
        } else if (conditions instanceof String) {
            return evaluateExpression((String) conditions, data);
        }
        
        return false;
    }
    
    private boolean evaluateAnd(Map<String, Object> conditionMap, Object data) {
        Object conditions = conditionMap.get("conditions");
        if (conditions instanceof Iterable) {
            for (Object condition : (Iterable<?>) conditions) {
                if (!evaluate(condition, data)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    private boolean evaluateOr(Map<String, Object> conditionMap, Object data) {
        Object conditions = conditionMap.get("conditions");
        if (conditions instanceof Iterable) {
            for (Object condition : (Iterable<?>) conditions) {
                if (evaluate(condition, data)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean evaluateNot(Map<String, Object> conditionMap, Object data) {
        Object condition = conditionMap.get("condition");
        return !evaluate(condition, data);
    }
    
    private boolean evaluateSimpleCondition(Map<String, Object> conditionMap, Object data) {
        String field = (String) conditionMap.get("field");
        String operator = (String) conditionMap.get("operator");
        Object value = conditionMap.get("value");
        
        Object fieldValue = extractFieldValue(field, data);
        
        if (fieldValue == null && value == null) {
            return "==".equals(operator) || "equals".equals(operator);
        }
        
        if (fieldValue == null || value == null) {
            return "!=".equals(operator) || "notEquals".equals(operator);
        }
        
        return compareValues(fieldValue, operator, value);
    }
    
    private boolean evaluateExpression(String expression, Object data) {
        // Simple expression evaluation (e.g., "temperature > 25")
        String[] parts = expression.split("\\s+");
        if (parts.length == 3) {
            String field = parts[0];
            String operator = parts[1];
            String valueStr = parts[2];
            
            Object fieldValue = extractFieldValue(field, data);
            Object value = parseValue(valueStr);
            
            return compareValues(fieldValue, operator, value);
        }
        
        return false;
    }
    
    private Object extractFieldValue(String field, Object data) {
        if (data instanceof Map) {
            Map<String, Object> dataMap = (Map<String, Object>) data;
            
            if (field.contains(".")) {
                String[] parts = field.split("\\.");
                Object current = dataMap;
                
                for (String part : parts) {
                    if (current instanceof Map) {
                        current = ((Map<String, Object>) current).get(part);
                    } else {
                        return null;
                    }
                }
                
                return current;
            } else {
                return dataMap.get(field);
            }
        }
        
        return null;
    }
    
    private Object parseValue(String valueStr) {
        // Try to parse as number
        try {
            if (valueStr.contains(".")) {
                return Double.parseDouble(valueStr);
            } else {
                return Long.parseLong(valueStr);
            }
        } catch (NumberFormatException e) {
            // Not a number
        }
        
        // Try to parse as boolean
        if ("true".equalsIgnoreCase(valueStr) || "false".equalsIgnoreCase(valueStr)) {
            return Boolean.parseBoolean(valueStr);
        }
        
        // Return as string
        return valueStr;
    }
    
    private boolean compareValues(Object fieldValue, String operator, Object value) {
        switch (operator) {
            case "==":
            case "equals":
                return fieldValue.equals(value);
                
            case "!=":
            case "notEquals":
                return !fieldValue.equals(value);
                
            case ">":
            case "greaterThan":
                return compareNumeric(fieldValue, value) > 0;
                
            case ">=":
            case "greaterThanOrEquals":
                return compareNumeric(fieldValue, value) >= 0;
                
            case "<":
            case "lessThan":
                return compareNumeric(fieldValue, value) < 0;
                
            case "<=":
            case "lessThanOrEquals":
                return compareNumeric(fieldValue, value) <= 0;
                
            case "contains":
                return fieldValue.toString().contains(value.toString());
                
            case "startsWith":
                return fieldValue.toString().startsWith(value.toString());
                
            case "endsWith":
                return fieldValue.toString().endsWith(value.toString());
                
            default:
                return false;
        }
    }
    
    private int compareNumeric(Object a, Object b) {
        if (a instanceof Number && b instanceof Number) {
            double aDouble = ((Number) a).doubleValue();
            double bDouble = ((Number) b).doubleValue();
            return Double.compare(aDouble, bDouble);
        }
        
        throw new IllegalArgumentException("Cannot compare non-numeric values");
    }
}