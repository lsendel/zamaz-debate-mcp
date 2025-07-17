package com.zamaz.mcp.common.domain.model.valueobject;

import com.zamaz.mcp.common.domain.model.ValueObject;
import com.zamaz.mcp.common.domain.exception.DomainRuleViolationException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object representing an email address.
 * Ensures email format validation at the domain level.
 */
public record Email(String value) implements ValueObject {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );
    
    public Email {
        Objects.requireNonNull(value, "Email cannot be null");
        if (value.isBlank()) {
            throw new DomainRuleViolationException(
                "email.empty",
                "Email cannot be empty"
            );
        }
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new DomainRuleViolationException(
                "email.invalid",
                "Email format is invalid: " + value
            );
        }
    }
    
    /**
     * Creates an Email from a string, validating the format.
     * 
     * @param value the email string
     * @return a validated Email
     */
    public static Email from(String value) {
        return new Email(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}