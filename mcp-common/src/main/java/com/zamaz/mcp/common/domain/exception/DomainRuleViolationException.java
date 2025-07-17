package com.zamaz.mcp.common.domain.exception;

/**
 * Thrown when a business rule or domain invariant is violated.
 * This is a pure domain class with no framework dependencies.
 */
public class DomainRuleViolationException extends DomainException {
    
    private final String rule;
    
    public DomainRuleViolationException(String rule, String message) {
        super(message);
        this.rule = rule;
    }
    
    public String getRule() {
        return rule;
    }
}