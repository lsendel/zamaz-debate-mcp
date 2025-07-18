package com.zamaz.mcp.organization.adapter.external.email;

import com.zamaz.mcp.common.architecture.exception.ExternalServiceException;
import com.zamaz.mcp.common.infrastructure.logging.DomainLogger;
import com.zamaz.mcp.common.infrastructure.logging.DomainLoggerFactory;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Secure email service with rate limiting and validation.
 */
@Service
@RequiredArgsConstructor
public class SecureEmailService {
    
    private final JavaMailSender mailSender;
    private final DomainLogger logger;
    private final Map<String, Bucket> userRateLimitBuckets = new ConcurrentHashMap<>();
    
    @Value("${app.email.from:noreply@mcp-debate.com}")
    private String fromEmail;
    
    @Value("${app.email.rate-limit.per-hour:10}")
    private int emailsPerHour;
    
    @Value("${APP_EMAIL_BASE_URL}")
    private String baseUrl;
    
    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );
    
    public SecureEmailService(JavaMailSender mailSender, DomainLoggerFactory loggerFactory) {
        this.mailSender = mailSender;
        this.logger = loggerFactory.getLogger(SecureEmailService.class);
    }
    
    /**
     * Sends an email using a template with security validation and rate limiting.
     */
    public void sendEmail(String toEmail, EmailTemplate template) {
        try {
            // Validate inputs
            validateEmailAddress(toEmail);
            validateTemplate(template);
            
            // Check rate limiting
            if (!checkRateLimit(toEmail)) {
                logger.warn("Rate limit exceeded for email", "email", toEmail);
                throw new ExternalServiceException("EmailService", "Rate limit exceeded for email address");
            }
            
            if (!emailEnabled) {
                logger.info("Email sending disabled, logging email", 
                    "to", toEmail, 
                    "template", template.getTemplateName());
                return;
            }
            
            // Process template variables
            String processedSubject = processTemplate(template.getSubject(), template.getVariables());
            String processedTextContent = processTemplate(template.getTextContent(), template.getVariables());
            String processedHtmlContent = processTemplate(template.getHtmlContent(), template.getVariables());
            
            // Create and send email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(processedSubject);
            helper.setText(processedTextContent, processedHtmlContent);
            
            // Add security headers
            message.setHeader("X-MCP-Email-Type", template.getTemplateName());
            message.setHeader("X-MCP-Timestamp", String.valueOf(System.currentTimeMillis()));
            
            mailSender.send(message);
            
            logger.info("Email sent successfully",
                "to", toEmail,
                "template", template.getTemplateName(),
                "subject", processedSubject);
                
        } catch (Exception e) {
            logger.error("Failed to send email", e,
                "to", toEmail,
                "template", template.getTemplateName());
            throw new ExternalServiceException("EmailService", "Failed to send email", e);
        }
    }
    
    /**
     * Validates email address format and security.
     */
    private void validateEmailAddress(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email address cannot be null or empty");
        }
        
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email address format: " + email);
        }
        
        // Security check: prevent email injection
        if (email.contains("\n") || email.contains("\r") || email.contains("\t")) {
            throw new IllegalArgumentException("Email address contains invalid characters");
        }
        
        // Length validation
        if (email.length() > 254) {
            throw new IllegalArgumentException("Email address too long");
        }
    }
    
    /**
     * Validates email template.
     */
    private void validateTemplate(EmailTemplate template) {
        if (template == null) {
            throw new IllegalArgumentException("Email template cannot be null");
        }
        
        if (template.getSubject() == null || template.getSubject().trim().isEmpty()) {
            throw new IllegalArgumentException("Email subject cannot be null or empty");
        }
        
        if (template.getTextContent() == null || template.getTextContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Email text content cannot be null or empty");
        }
        
        // Security: prevent header injection
        if (template.getSubject().contains("\n") || template.getSubject().contains("\r")) {
            throw new IllegalArgumentException("Email subject contains invalid characters");
        }
    }
    
    /**
     * Checks rate limiting for email address.
     */
    private boolean checkRateLimit(String email) {
        Bucket bucket = userRateLimitBuckets.computeIfAbsent(email, this::createRateLimitBucket);
        return bucket.tryConsume(1);
    }
    
    /**
     * Creates a rate limit bucket for an email address.
     */
    private Bucket createRateLimitBucket(String email) {
        Bandwidth limit = Bandwidth.classic(emailsPerHour, Refill.intervally(emailsPerHour, Duration.ofHours(1)));
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }
    
    /**
     * Processes template variables in content.
     */
    private String processTemplate(String content, Map<String, Object> variables) {
        if (content == null || variables == null) {
            return content;
        }
        
        String processed = content;
        
        // Replace base URL
        processed = processed.replace("${app.base-url}", baseUrl);
        
        // Replace other variables
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            processed = processed.replace(placeholder, value);
        }
        
        return processed;
    }
    
    /**
     * Cleans up old rate limit buckets to prevent memory leaks.
     */
    public void cleanupRateLimitBuckets() {
        // In a real application, you'd implement a scheduled task to clean up old buckets
        // For now, we'll clean up when the map gets too large
        if (userRateLimitBuckets.size() > 10000) {
            logger.info("Cleaning up rate limit buckets", "currentSize", userRateLimitBuckets.size());
            userRateLimitBuckets.clear();
        }
    }
}