package com.zamaz.mcp.common.patterns;

import com.zamaz.mcp.common.exception.ExceptionFactory;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utility class for common validation patterns across all services.
 * Provides standardized validation methods with consistent error messages.
 */
public final class ValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );

    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    private static final Pattern SLUG_PATTERN = Pattern.compile(
        "^[a-z0-9]+(?:-[a-z0-9]+)*$"
    );

    private static final Pattern ORGANIZATION_NAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9\\s\\-._]+$"
    );

    private static final Pattern USERNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_.-]+$"
    );

    private ValidationUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Validate that a string is not null or empty.
     */
    public static void requireNonEmpty(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw ExceptionFactory.validationFailed(
                "Validation failed",
                Map.of(fieldName, "Field is required and cannot be empty")
            );
        }
    }

    /**
     * Validate that an object is not null.
     */
    public static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw ExceptionFactory.validationFailed(
                "Validation failed",
                Map.of(fieldName, "Field is required and cannot be null")
            );
        }
    }

    /**
     * Validate string length.
     */
    public static void validateLength(String value, String fieldName, int minLength, int maxLength) {
        if (value == null) {
            return; // Null check should be done separately
        }
        
        if (value.length() < minLength) {
            throw ExceptionFactory.validationFailed(
                "Validation failed",
                Map.of(fieldName, String.format("Field must be at least %d characters long", minLength))
            );
        }
        
        if (value.length() > maxLength) {
            throw ExceptionFactory.validationFailed(
                "Validation failed",
                Map.of(fieldName, String.format("Field cannot be longer than %d characters", maxLength))
            );
        }
    }

    /**
     * Validate email format.
     */
    public static void validateEmail(String email, String fieldName) {
        if (email == null) {
            return; // Null check should be done separately
        }
        
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw ExceptionFactory.validationFailed(
                "Validation failed",
                Map.of(fieldName, "Invalid email format")
            );
        }
    }

    /**
     * Validate UUID format.
     */
    public static void validateUUID(String uuid, String fieldName) {
        if (uuid == null) {
            return; // Null check should be done separately
        }
        
        if (!UUID_PATTERN.matcher(uuid).matches()) {
            throw ExceptionFactory.validationFailed(
                "Validation failed",
                Map.of(fieldName, "Invalid UUID format")
            );
        }
    }

    /**
     * Validate slug format (lowercase, alphanumeric, hyphens).
     */
    public static void validateSlug(String slug, String fieldName) {
        if (slug == null) {
            return; // Null check should be done separately
        }
        
        if (!SLUG_PATTERN.matcher(slug).matches()) {
            throw ExceptionFactory.validationFailed(
                "Validation failed",
                Map.of(fieldName, "Invalid slug format. Use lowercase letters, numbers, and hyphens only")
            );
        }
    }

    /**
     * Validate organization name format.
     */
    public static void validateOrganizationName(String name, String fieldName) {
        if (name == null) {
            return; // Null check should be done separately
        }
        
        if (!ORGANIZATION_NAME_PATTERN.matcher(name).matches()) {
            throw ExceptionFactory.validationFailed(
                "Validation failed",
                Map.of(fieldName, "Organization name contains invalid characters")
            );
        }
    }

    /**
     * Validate username format.
     */
    public static void validateUsername(String username, String fieldName) {
        if (username == null) {
            return; // Null check should be done separately
        }
        
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw ExceptionFactory.validationFailed(
                "Validation failed",
                Map.of(fieldName, "Username can only contain letters, numbers, dots, hyphens, and underscores")
            );
        }
    }

    /**
     * Validate numeric range.
     */
    public static void validateRange(int value, String fieldName, int min, int max) {
        if (value < min || value > max) {
            throw ExceptionFactory.validationFailed(
                "Validation failed",
                Map.of(fieldName, String.format("Value must be between %d and %d", min, max))
            );
        }
    }

    /**
     * Validate numeric range (double).
     */
    public static void validateRange(double value, String fieldName, double min, double max) {
        if (value < min || value > max) {
            throw ExceptionFactory.validationFailed(
                "Validation failed",
                Map.of(fieldName, String.format("Value must be between %.2f and %.2f", min, max))
            );
        }
    }

    /**
     * Validate that a value is positive.
     */
    public static void validatePositive(int value, String fieldName) {
        if (value <= 0) {
            throw ExceptionFactory.validationFailed(
                "Validation failed",
                Map.of(fieldName, "Value must be positive")
            );
        }
    }

    /**
     * Validate that a value is positive (double).
     */
    public static void validatePositive(double value, String fieldName) {
        if (value <= 0) {
            throw ExceptionFactory.validationFailed(
                "Validation failed",
                Map.of(fieldName, "Value must be positive")
            );
        }
    }

    /**
     * Validate that a value is non-negative.
     */
    public static void validateNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw ExceptionFactory.validationFailed(
                "Validation failed",
                Map.of(fieldName, "Value cannot be negative")
            );
        }
    }

    /**
     * Validate that a value is non-negative (double).
     */
    public static void validateNonNegative(double value, String fieldName) {
        if (value < 0) {
            throw ExceptionFactory.validationFailed(
                "Validation failed",
                Map.of(fieldName, "Value cannot be negative")
            );
        }
    }

    /**
     * Validate URL format.
     */
    public static void validateUrl(String url, String fieldName) {
        if (url == null) {
            return; // Null check should be done separately
        }
        
        try {
            new java.net.URL(url);
        } catch (java.net.MalformedURLException e) {
            throw ExceptionFactory.validationFailed(
                "Validation failed",
                Map.of(fieldName, "Invalid URL format")
            );
        }
    }

    /**
     * Validate that a string matches a custom pattern.
     */
    public static void validatePattern(String value, String fieldName, Pattern pattern, String errorMessage) {
        if (value == null) {
            return; // Null check should be done separately
        }
        
        if (!pattern.matcher(value).matches()) {
            throw ExceptionFactory.validationFailed(
                "Validation failed",
                Map.of(fieldName, errorMessage)
            );
        }
    }

    /**
     * Validate multiple fields at once.
     */
    public static void validateMultiple(ValidationBuilder builder) {
        Map<String, String> errors = builder.getErrors();
        if (!errors.isEmpty()) {
            throw ExceptionFactory.validationFailed("Validation failed", errors);
        }
    }

    /**
     * Builder class for multiple field validation.
     */
    public static class ValidationBuilder {
        private final Map<String, String> errors = new HashMap<>();

        public ValidationBuilder requireNonEmpty(String value, String fieldName) {
            if (!StringUtils.hasText(value)) {
                errors.put(fieldName, "Field is required and cannot be empty");
            }
            return this;
        }

        public ValidationBuilder requireNonNull(Object value, String fieldName) {
            if (value == null) {
                errors.put(fieldName, "Field is required and cannot be null");
            }
            return this;
        }

        public ValidationBuilder validateLength(String value, String fieldName, int minLength, int maxLength) {
            if (value != null) {
                if (value.length() < minLength) {
                    errors.put(fieldName, String.format("Field must be at least %d characters long", minLength));
                } else if (value.length() > maxLength) {
                    errors.put(fieldName, String.format("Field cannot be longer than %d characters", maxLength));
                }
            }
            return this;
        }

        public ValidationBuilder validateEmail(String email, String fieldName) {
            if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
                errors.put(fieldName, "Invalid email format");
            }
            return this;
        }

        public ValidationBuilder validateUUID(String uuid, String fieldName) {
            if (uuid != null && !UUID_PATTERN.matcher(uuid).matches()) {
                errors.put(fieldName, "Invalid UUID format");
            }
            return this;
        }

        public ValidationBuilder validateRange(int value, String fieldName, int min, int max) {
            if (value < min || value > max) {
                errors.put(fieldName, String.format("Value must be between %d and %d", min, max));
            }
            return this;
        }

        public ValidationBuilder validatePositive(int value, String fieldName) {
            if (value <= 0) {
                errors.put(fieldName, "Value must be positive");
            }
            return this;
        }

        public ValidationBuilder custom(String fieldName, String errorMessage, boolean condition) {
            if (!condition) {
                errors.put(fieldName, errorMessage);
            }
            return this;
        }

        public Map<String, String> getErrors() {
            return errors;
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }
}