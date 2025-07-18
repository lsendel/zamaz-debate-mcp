package com.zamaz.mcp.organization.adapter.external;

import com.zamaz.mcp.common.architecture.adapter.external.ExternalServiceAdapter;
import com.zamaz.mcp.common.infrastructure.logging.DomainLogger;
import com.zamaz.mcp.common.infrastructure.logging.DomainLoggerFactory;
import com.zamaz.mcp.organization.adapter.external.email.EmailTemplate;
import com.zamaz.mcp.organization.adapter.external.email.SecureEmailService;
import com.zamaz.mcp.organization.application.port.outbound.NotificationService;
import com.zamaz.mcp.organization.domain.model.Organization;
import com.zamaz.mcp.organization.domain.model.Role;
import com.zamaz.mcp.organization.domain.model.User;
import lombok.RequiredArgsConstructor;
import com.zamaz.mcp.common.resilience.annotation.Retry;
import org.springframework.stereotype.Component;

/**
 * Implementation of NotificationService that sends notifications via external services.
 * This is an external service adapter in hexagonal architecture.
 */
@Component
@RequiredArgsConstructor
public class NotificationServiceAdapter implements NotificationService, ExternalServiceAdapter {
    
    private final SecureEmailService emailService;
    private final DomainLogger logger;
    
    public NotificationServiceAdapter(SecureEmailService emailService, DomainLoggerFactory loggerFactory) {
        this.emailService = emailService;
        this.logger = loggerFactory.getLogger(NotificationServiceAdapter.class);
    }
    
    @Override
    @Retry(name = "notification-send", maxAttempts = 3, waitDurationMs = 1000)
    public void sendOrganizationCreatedNotification(Organization organization, User owner) {
        logger.info("Sending organization created notification",
            "organizationId", organization.getId().value(),
            "organizationName", organization.getName().value(),
            "ownerId", owner.getId().value(),
            "ownerEmail", owner.getEmail().value()
        );
        
        try {
            EmailTemplate template = EmailTemplate.organizationCreated(
                organization.getName().value(),
                owner.getFullName(),
                owner.getEmail().value()
            );
            
            emailService.sendEmail(owner.getEmail().value(), template);
            
            logger.info("Organization created notification sent successfully",
                "organizationId", organization.getId().value(),
                "ownerEmail", owner.getEmail().value());
                
        } catch (Exception e) {
            logger.error("Failed to send organization created notification", e,
                "organizationId", organization.getId().value(),
                "ownerEmail", owner.getEmail().value());
            throw e;
        }
    }
    
    @Override
    @Retry(name = "notification-send", maxAttempts = 3, waitDurationMs = 1000)
    public void sendUserAddedToOrganizationNotification(Organization organization, User user, Role role) {
        logger.info("Sending user added to organization notification",
            "organizationId", organization.getId().value(),
            "organizationName", organization.getName().value(),
            "userId", user.getId().value(),
            "userEmail", user.getEmail().value(),
            "role", role.name()
        );
        
        try {
            // Get inviter from security context or default to system
            String inviterName = getCurrentInviterName().orElse("System Administrator");
            
            EmailTemplate template = EmailTemplate.userAddedToOrganization(
                organization.getName().value(),
                user.getFullName(),
                role.name(),
                inviterName
            );
            
            emailService.sendEmail(user.getEmail().value(), template);
            
            logger.info("User added notification sent successfully",
                "organizationId", organization.getId().value(),
                "userEmail", user.getEmail().value(),
                "role", role.name());
                
        } catch (Exception e) {
            logger.error("Failed to send user added notification", e,
                "organizationId", organization.getId().value(),
                "userEmail", user.getEmail().value());
            throw e;
        }
    }
    
    @Override
    @Retry(name = "notification-send", maxAttempts = 3, waitDurationMs = 1000)
    public void sendUserRemovedFromOrganizationNotification(Organization organization, User user) {
        logger.info("Sending user removed from organization notification",
            "organizationId", organization.getId().value(),
            "organizationName", organization.getName().value(),
            "userId", user.getId().value(),
            "userEmail", user.getEmail().value()
        );
        
        try {
            EmailTemplate template = EmailTemplate.userRemovedFromOrganization(
                organization.getName().value(),
                user.getFullName()
            );
            
            emailService.sendEmail(user.getEmail().value(), template);
            
            logger.info("User removed notification sent successfully",
                "organizationId", organization.getId().value(),
                "userEmail", user.getEmail().value());
                
        } catch (Exception e) {
            logger.error("Failed to send user removed notification", e,
                "organizationId", organization.getId().value(),
                "userEmail", user.getEmail().value());
            throw e;
        }
    }
    
    @Override
    @Retry(name = "notification-send", maxAttempts = 3, waitDurationMs = 1000)
    public void sendRoleChangedNotification(Organization organization, User user, Role oldRole, Role newRole) {
        logger.info("Sending role changed notification",
            "organizationId", organization.getId().value(),
            "organizationName", organization.getName().value(),
            "userId", user.getId().value(),
            "userEmail", user.getEmail().value(),
            "oldRole", oldRole.name(),
            "newRole", newRole.name()
        );
        
        try {
            EmailTemplate template = EmailTemplate.roleChanged(
                organization.getName().value(),
                user.getFullName(),
                oldRole.name(),
                newRole.name()
            );
            
            emailService.sendEmail(user.getEmail().value(), template);
            
            logger.info("Role changed notification sent successfully",
                "organizationId", organization.getId().value(),
                "userEmail", user.getEmail().value(),
                "oldRole", oldRole.name(),
                "newRole", newRole.name());
                
        } catch (Exception e) {
            logger.error("Failed to send role changed notification", e,
                "organizationId", organization.getId().value(),
                "userEmail", user.getEmail().value(),
                "oldRole", oldRole.name(),
                "newRole", newRole.name());
            throw e;
        }
    }
    
    @Override
    @Retry(name = "notification-send", maxAttempts = 3, waitDurationMs = 1000)
    public void sendEmailVerificationNotification(User user, String verificationToken) {
        logger.info("Sending email verification notification",
            "userId", user.getId().value(),
            "userEmail", user.getEmail().value(),
            "tokenLength", verificationToken.length()
        );
        
        try {
            // Generate verification link with token
            String verificationLink = buildVerificationLink(verificationToken);
            
            EmailTemplate template = EmailTemplate.emailVerification(
                user.getFullName(),
                verificationLink
            );
            
            emailService.sendEmail(user.getEmail().value(), template);
            
            logger.info("Email verification notification sent successfully",
                "userId", user.getId().value(),
                "userEmail", user.getEmail().value());
                
        } catch (Exception e) {
            logger.error("Failed to send email verification notification", e,
                "userId", user.getId().value(),
                "userEmail", user.getEmail().value());
            throw e;
        }
    }
    
    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;
    
    private String buildVerificationLink(String verificationToken) {
        return baseUrl + "/verify-email?token=" + verificationToken;
    }
    
    /**
     * Gets the current inviter name from security context.
     * In a production system, this would extract the authenticated user's name.
     */
    private java.util.Optional<String> getCurrentInviterName() {
        try {
            // In a real implementation, extract from SecurityContextHolder
            // For now, return empty to use default
            return java.util.Optional.empty();
        } catch (Exception e) {
            logger.warn("Failed to get current inviter name from security context", e);
            return java.util.Optional.empty();
        }
    }
}