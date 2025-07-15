package com.zamaz.mcp.common.util;

import com.zamaz.mcp.common.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Common validation utilities for MCP services.
 * Provides consistent validation logic across all services.
 */
public final class ValidationUtils {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    private static final Pattern SLUG_PATTERN = Pattern.compile(
        "^[a-z0-9]+(?:-[a-z0-9]+)*$"
    );
    
    private ValidationUtils() {
        // Prevent instantiation
    }
    
    /**
     * Validates that a value is not null
     */
    public static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw BusinessException.validationFailed(fieldName, "must not be null");
        }
        return value;
    }
    
    /**
     * Validates that a string is not empty
     */
    public static String requireNonEmpty(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw BusinessException.validationFailed(fieldName, "must not be empty");
        }
        return value.trim();
    }
    
    /**
     * Validates that a collection is not empty
     */
    public static <T extends Collection<?>> T requireNonEmpty(T collection, String fieldName) {
        if (collection == null || collection.isEmpty()) {
            throw BusinessException.validationFailed(fieldName, "must not be empty");
        }
        return collection;
    }
    
    /**
     * Validates string length
     */
    public static String requireLength(String value, String fieldName, int minLength, int maxLength) {
        requireNonEmpty(value, fieldName);
        if (value.length() < minLength || value.length() > maxLength) {
            throw BusinessException.validationFailed(fieldName, 
                String.format("must be between %d and %d characters", minLength, maxLength));
        }
        return value;
    }
    
    /**
     * Validates that a value is within a range
     */
    public static <T extends Comparable<T>> T requireInRange(T value, String fieldName, T min, T max) {
        requireNonNull(value, fieldName);
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw BusinessException.validationFailed(fieldName,
                String.format("must be between %s and %s", min, max));
        }
        return value;
    }
    
    /**
     * Validates that a string is a valid UUID
     */
    public static UUID requireValidUuid(String value, String fieldName) {
        requireNonEmpty(value, fieldName);
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw BusinessException.validationFailed(fieldName, "must be a valid UUID");
        }
    }
    
    /**
     * Validates that a string is a valid email
     */
    public static String requireValidEmail(String value, String fieldName) {
        requireNonEmpty(value, fieldName);
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw BusinessException.validationFailed(fieldName, "must be a valid email address");
        }
        return value.toLowerCase();
    }
    
    /**
     * Validates that a string is a valid slug
     */
    public static String requireValidSlug(String value, String fieldName) {
        requireNonEmpty(value, fieldName);
        if (!SLUG_PATTERN.matcher(value).matches()) {
            throw BusinessException.validationFailed(fieldName, 
                "must contain only lowercase letters, numbers, and hyphens");
        }
        return value;
    }
    
    /**
     * Validates that a value is one of the allowed values
     */
    @SafeVarargs
    public static <T> T requireOneOf(T value, String fieldName, T... allowedValues) {
        requireNonNull(value, fieldName);
        for (T allowed : allowedValues) {
            if (value.equals(allowed)) {
                return value;
            }
        }
        throw BusinessException.validationFailed(fieldName,
            String.format("must be one of: %s", String.join(", ", 
                java.util.Arrays.stream(allowedValues).map(Object::toString).toArray(String[]::new))));
    }
}