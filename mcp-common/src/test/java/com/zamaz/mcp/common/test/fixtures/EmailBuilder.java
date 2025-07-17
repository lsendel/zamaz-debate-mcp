package com.zamaz.mcp.common.test.fixtures;

import com.zamaz.mcp.common.domain.model.valueobject.Email;

/**
 * Test data builder for Email value objects.
 */
public class EmailBuilder implements TestDataBuilder<Email> {
    
    private String value;
    
    private EmailBuilder() {
        this.value = "test@example.com";
    }
    
    public static EmailBuilder anEmail() {
        return new EmailBuilder();
    }
    
    public EmailBuilder withValue(String value) {
        this.value = value;
        return this;
    }
    
    public EmailBuilder withLocalPart(String localPart) {
        String domain = value.substring(value.indexOf('@'));
        this.value = localPart + domain;
        return this;
    }
    
    public EmailBuilder withDomain(String domain) {
        String localPart = value.substring(0, value.indexOf('@'));
        this.value = localPart + "@" + domain;
        return this;
    }
    
    @Override
    public Email build() {
        return Email.from(value);
    }
    
    /**
     * Creates common test emails.
     */
    public static EmailBuilder adminEmail() {
        return new EmailBuilder().withValue("admin@test.com");
    }
    
    public static EmailBuilder userEmail() {
        return new EmailBuilder().withValue("user@test.com");
    }
    
    /**
     * Creates an invalid email for testing validation.
     */
    public static EmailBuilder invalidEmail() {
        return new EmailBuilder().withValue("invalid-email");
    }
    
    /**
     * Creates multiple unique emails for testing.
     * 
     * @param count the number of emails to create
     * @return an array of emails
     */
    public static Email[] multipleEmails(int count) {
        Email[] emails = new Email[count];
        for (int i = 0; i < count; i++) {
            emails[i] = anEmail()
                .withLocalPart("user" + i)
                .build();
        }
        return emails;
    }
}