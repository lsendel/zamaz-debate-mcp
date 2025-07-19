package com.example.workflow.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Execution context for workflow execution
 * Stores runtime data and variables during workflow execution
 */
public class ExecutionContext {
    
    private final Map<String, Object> data;
    
    public ExecutionContext() {
        this.data = new HashMap<>();
    }
    
    /**
     * Set context data
     */
    public void setData(String key, Object value) {
        Objects.requireNonNull(key, "Context key cannot be null");
        if (value == null) {
            data.remove(key);
        } else {
            data.put(key, value);
        }
    }
    
    /**
     * Get context data
     */
    public Object getData(String key) {
        return data.get(key);
    }
    
    /**
     * Get context data with default value
     */
    public Object getData(String key, Object defaultValue) {
        return data.getOrDefault(key, defaultValue);
    }
    
    /**
     * Check if context has data for key
     */
    public boolean hasData(String key) {
        return data.containsKey(key);
    }
    
    /**
     * Remove context data
     */
    public void removeData(String key) {
        data.remove(key);
    }
    
    /**
     * Clear all context data
     */
    public void clear() {
        data.clear();
    }
    
    /**
     * Get all context keys
     */
    public java.util.Set<String> getKeys() {
        return data.keySet();
    }
    
    /**
     * Get copy of all context data
     */
    public Map<String, Object> getAllData() {
        return Map.copyOf(data);
    }
    
    /**
     * Get context data as string
     */
    public String getStringData(String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * Get context data as number
     */
    public Number getNumericData(String key) {
        Object value = data.get(key);
        return value instanceof Number ? (Number) value : null;
    }
    
    /**
     * Get context data as boolean
     */
    public Boolean getBooleanData(String key) {
        Object value = data.get(key);
        return value instanceof Boolean ? (Boolean) value : null;
    }
    
    @Override
    public String toString() {
        return "ExecutionContext{" +
                "dataCount=" + data.size() +
                ", keys=" + data.keySet() +
                '}';
    }
}