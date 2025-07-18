# Email Integration Guide

This guide explains how to configure and use the email service in the MCP Gateway for sending transactional emails.

## Overview

The MCP Gateway includes a flexible email service that supports:
- SMTP integration (Gmail, Outlook, custom SMTP servers)
- HTML and plain text email formats
- Email verification, password reset, and notification emails
- Extensible architecture for future providers (SendGrid, AWS SES, etc.)

## Configuration

### Basic SMTP Configuration

Add the following to your `application.yml`:

```yaml
# Email service configuration
app:
  email:
    enabled: true
    from: noreply@yourcompany.com
    from-name: Your Company Name
    base-url: https://yourapp.com
    provider: smtp  # Options: smtp, sendgrid, ses

# Spring Mail configuration
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password  # Use app-specific password for Gmail
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000
```

### Provider-Specific Configurations

#### Gmail

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password  # Generate from Google Account settings
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          ssl:
            protocols: TLSv1.2
            trust: smtp.gmail.com
```

**Important**: For Gmail, you must:
1. Enable 2-factor authentication
2. Generate an app-specific password at https://myaccount.google.com/apppasswords
3. Use the app password instead of your regular password

#### Outlook/Office 365

```yaml
spring:
  mail:
    host: smtp-mail.outlook.com
    port: 587
    username: your-email@outlook.com
    password: your-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
```

#### SendGrid (SMTP)

```yaml
spring:
  mail:
    host: smtp.sendgrid.net
    port: 587
    username: apikey  # Literally "apikey"
    password: SG.your-sendgrid-api-key  # Your actual SendGrid API key
```

#### AWS SES (SMTP)

```yaml
spring:
  mail:
    host: email-smtp.us-east-1.amazonaws.com  # Replace with your region
    port: 587
    username: your-ses-smtp-username
    password: your-ses-smtp-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
```

## Environment Variables

For production deployments, use environment variables:

```bash
# Email service settings
APP_EMAIL_ENABLED=true
APP_EMAIL_FROM=noreply@yourcompany.com
APP_EMAIL_FROM_NAME=Your Company Name
APP_EMAIL_BASE_URL=https://yourapp.com

# SMTP settings
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password
```

## Testing Email Configuration

### 1. Enable Debug Logging

Add to `application.yml`:

```yaml
logging:
  level:
    com.zamaz.mcp.gateway.service.EmailService: DEBUG
    org.springframework.mail: DEBUG
    com.sun.mail: DEBUG
```

### 2. Test Email Endpoint

Create a test endpoint or use the existing user registration flow:

```java
@RestController
@RequestMapping("/api/test")
public class EmailTestController {
    
    @Autowired
    private EmailService emailService;
    
    @PostMapping("/email")
    public ResponseEntity<String> testEmail(@RequestParam String email) {
        emailService.sendPasswordResetEmail(email, "Test User", "test-token");
        return ResponseEntity.ok("Test email sent");
    }
}
```

### 3. Verify Email Delivery

Check:
1. Application logs for successful send confirmation
2. Recipient's inbox (check spam folder)
3. SMTP server logs if available

## Email Templates

The service includes pre-built templates for:

### Email Verification
- Clean, responsive HTML design
- Clear call-to-action button
- Verification link with token
- Plain text fallback

### Password Reset
- Secure reset link
- Expiration time notice
- Instructions for users
- Support contact information

### Email Change Notification
- Security notification
- Sent to both old and new addresses
- Action steps if unauthorized

## Troubleshooting

### Common Issues

#### 1. Authentication Failed
```
javax.mail.AuthenticationFailedException: 535-5.7.8 Username and Password not accepted
```

**Solution**: 
- For Gmail: Use app-specific password
- Check username/password are correct
- Ensure 2FA is enabled for Gmail

#### 2. Connection Timeout
```
javax.mail.MessagingException: Could not connect to SMTP host
```

**Solution**:
- Check firewall rules
- Verify SMTP host and port
- Increase timeout values

#### 3. TLS/SSL Issues
```
javax.net.ssl.SSLException: Unrecognized SSL message
```

**Solution**:
- Ensure correct port (587 for TLS, 465 for SSL)
- Set starttls.enable=true for port 587
- Update Java security providers if needed

#### 4. Email Not Received
- Check spam/junk folders
- Verify 'from' address is not blacklisted
- Check SPF/DKIM records for your domain
- Review SMTP server logs

### Debug Mode

Enable detailed SMTP debugging:

```yaml
spring:
  mail:
    properties:
      mail:
        debug: true
```

## Production Considerations

### 1. Security
- Never commit credentials to version control
- Use environment variables or secrets management
- Rotate credentials regularly
- Monitor for unauthorized usage

### 2. Rate Limiting
- Implement rate limiting for email endpoints
- Track email sending metrics
- Set up alerts for unusual activity

### 3. Deliverability
- Configure SPF records
- Set up DKIM signing
- Monitor bounce rates
- Use dedicated IP for high volume

### 4. Monitoring
- Track email send success/failure rates
- Monitor queue sizes
- Set up alerts for failures
- Log all email activities

## Extending the Service

### Adding New Email Types

1. Add new method to `EmailService`:
```java
public void sendCustomEmail(String toEmail, String data) {
    String subject = "Custom Email Subject";
    String html = buildCustomTemplate(data);
    String text = buildCustomTextTemplate(data);
    sendEmail(toEmail, subject, html, text);
}
```

2. Create template builder methods
3. Add appropriate logging and error handling

### Adding New Providers

To add support for a new email provider:

1. Add provider case in `sendEmail()` method
2. Implement provider-specific sending method
3. Add configuration properties
4. Update documentation

## Best Practices

1. **Always provide both HTML and text versions**
2. **Keep emails concise and actionable**
3. **Test emails across different clients**
4. **Include unsubscribe links where appropriate**
5. **Monitor delivery rates and user engagement**
6. **Handle bounces and complaints properly**
7. **Use transactional email service for better deliverability**

## References

- [Spring Boot Mail Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.email)
- [Gmail SMTP Settings](https://support.google.com/mail/answer/7126229)
- [SendGrid SMTP Documentation](https://docs.sendgrid.com/for-developers/sending-email/integrating-with-the-smtp-api)
- [AWS SES SMTP Documentation](https://docs.aws.amazon.com/ses/latest/dg/send-email-smtp.html)