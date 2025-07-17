package com.zamaz.mcp.common.cache;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.StringJoiner;

/**
 * Custom cache key generator that creates meaningful cache keys
 */
@Component
public class CustomCacheKeyGenerator implements KeyGenerator {
    
    @Override
    public Object generate(Object target, Method method, Object... params) {
        return generateKey(target.getClass().getSimpleName(), method.getName(), params);
    }
    
    /**
     * Generate a cache key with organization context
     */
    public String generateWithOrganization(String organizationId, String prefix, Object... params) {
        StringJoiner joiner = new StringJoiner(":");
        
        // Add organization context
        if (StringUtils.hasText(organizationId)) {
            joiner.add("org").add(organizationId);
        }
        
        // Add prefix
        if (StringUtils.hasText(prefix)) {
            joiner.add(prefix);
        }
        
        // Add parameters
        for (Object param : params) {
            if (param != null) {
                joiner.add(param.toString());
            }
        }
        
        return joiner.toString();
    }
    
    /**
     * Generate a cache key for user-specific data
     */
    public String generateUserKey(String userId, String prefix, Object... params) {
        StringJoiner joiner = new StringJoiner(":");
        
        // Add user context
        joiner.add("user").add(userId);
        
        // Add prefix
        if (StringUtils.hasText(prefix)) {
            joiner.add(prefix);
        }
        
        // Add parameters
        for (Object param : params) {
            if (param != null) {
                joiner.add(param.toString());
            }
        }
        
        return joiner.toString();
    }
    
    private String generateKey(String className, String methodName, Object... params) {
        StringJoiner joiner = new StringJoiner(":");
        
        // Add class and method name
        joiner.add(className);
        joiner.add(methodName);
        
        // Add parameters
        for (Object param : params) {
            if (param != null) {
                // Handle complex objects
                if (param.getClass().isArray()) {
                    joiner.add(arrayToString((Object[]) param));
                } else {
                    joiner.add(param.toString());
                }
            } else {
                joiner.add("null");
            }
        }
        
        return joiner.toString();
    }
    
    private String arrayToString(Object[] array) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (Object obj : array) {
            joiner.add(obj != null ? obj.toString() : "null");
        }
        return joiner.toString();
    }
}