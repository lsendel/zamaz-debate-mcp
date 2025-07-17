package com.zamaz.mcp.organization.adapter.external.email;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Email template definition for notification system.
 */
@Value
@Builder
public class EmailTemplate {
    String templateName;
    String subject;
    String textContent;
    String htmlContent;
    Map<String, Object> variables;
    
    public static EmailTemplate organizationCreated(String organizationName, String ownerName, String ownerEmail) {
        return EmailTemplate.builder()
            .templateName("organization-created")
            .subject("Welcome to " + organizationName + " - Organization Created Successfully")
            .textContent("""
                Dear %s,
                
                Congratulations! Your organization "%s" has been created successfully.
                
                You can now start inviting team members and managing your organization settings.
                
                Login to your dashboard to get started: ${dashboardUrl}
                
                Best regards,
                The MCP Team
                """.formatted(ownerName, organizationName))
            .htmlContent("""
                <html>
                <body>
                    <h2>Welcome to %s!</h2>
                    <p>Dear %s,</p>
                    <p>Congratulations! Your organization "<strong>%s</strong>" has been created successfully.</p>
                    <p>You can now start inviting team members and managing your organization settings.</p>
                    <p><a href="${dashboardUrl}" style="background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Access Dashboard</a></p>
                    <p>Best regards,<br/>The MCP Team</p>
                </body>
                </html>
                """.formatted(organizationName, ownerName, organizationName))
            .variables(Map.of(
                "organizationName", organizationName,
                "ownerName", ownerName,
                "ownerEmail", ownerEmail,
                "dashboardUrl", "${app.base-url}/dashboard"
            ))
            .build();
    }
    
    public static EmailTemplate userAddedToOrganization(String organizationName, String userName, String role, String inviterName) {
        return EmailTemplate.builder()
            .templateName("user-added")
            .subject("You've been added to " + organizationName)
            .textContent("""
                Dear %s,
                
                You have been added to the organization "%s" with the role of %s by %s.
                
                Login to your dashboard to start collaborating: ${dashboardUrl}
                
                Best regards,
                The MCP Team
                """.formatted(userName, organizationName, role, inviterName))
            .htmlContent("""
                <html>
                <body>
                    <h2>Welcome to %s!</h2>
                    <p>Dear %s,</p>
                    <p>You have been added to the organization "<strong>%s</strong>" with the role of <strong>%s</strong> by %s.</p>
                    <p><a href="${dashboardUrl}" style="background-color: #28a745; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Start Collaborating</a></p>
                    <p>Best regards,<br/>The MCP Team</p>
                </body>
                </html>
                """.formatted(organizationName, userName, organizationName, role, inviterName))
            .variables(Map.of(
                "organizationName", organizationName,
                "userName", userName,
                "role", role,
                "inviterName", inviterName,
                "dashboardUrl", "${app.base-url}/dashboard"
            ))
            .build();
    }
    
    public static EmailTemplate userRemovedFromOrganization(String organizationName, String userName) {
        return EmailTemplate.builder()
            .templateName("user-removed")
            .subject("You have been removed from " + organizationName)
            .textContent("""
                Dear %s,
                
                You have been removed from the organization "%s".
                
                If you believe this was done in error, please contact your organization administrator.
                
                Best regards,
                The MCP Team
                """.formatted(userName, organizationName))
            .htmlContent("""
                <html>
                <body>
                    <h2>Organization Access Removed</h2>
                    <p>Dear %s,</p>
                    <p>You have been removed from the organization "<strong>%s</strong>".</p>
                    <p>If you believe this was done in error, please contact your organization administrator.</p>
                    <p>Best regards,<br/>The MCP Team</p>
                </body>
                </html>
                """.formatted(userName, organizationName))
            .variables(Map.of(
                "organizationName", organizationName,
                "userName", userName
            ))
            .build();
    }

    public static EmailTemplate roleChanged(String organizationName, String userName, String oldRole, String newRole) {
        return EmailTemplate.builder()
            .templateName("role-changed")
            .subject("Your role has been updated in " + organizationName)
            .textContent("""
                Dear %s,
                
                Your role in the organization "%s" has been updated from %s to %s.
                
                Login to your dashboard to see your updated permissions: ${dashboardUrl}
                
                Best regards,
                The MCP Team
                """.formatted(userName, organizationName, oldRole, newRole))
            .htmlContent("""
                <html>
                <body>
                    <h2>Role Updated</h2>
                    <p>Dear %s,</p>
                    <p>Your role in the organization "<strong>%s</strong>" has been updated from <strong>%s</strong> to <strong>%s</strong>.</p>
                    <p><a href="${dashboardUrl}" style="background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">View Dashboard</a></p>
                    <p>Best regards,<br/>The MCP Team</p>
                </body>
                </html>
                """.formatted(userName, organizationName, oldRole, newRole))
            .variables(Map.of(
                "userName", userName,
                "organizationName", organizationName,
                "oldRole", oldRole,
                "newRole", newRole,
                "dashboardUrl", "${app.base-url}/dashboard"
            ))
            .build();
    }
    
    public static EmailTemplate emailVerification(String userName, String verificationToken) {
        return EmailTemplate.builder()
            .templateName("email-verification")
            .subject("Verify Your Email Address")
            .textContent("""
                Dear %s,
                
                Please verify your email address by clicking the link below:
                
                ${verificationUrl}
                
                This link will expire in 1 hour for security reasons.
                
                If you didn't request this verification, please ignore this email.
                
                Best regards,
                The MCP Team
                """.formatted(userName))
            .htmlContent("""
                <html>
                <body>
                    <h2>Verify Your Email Address</h2>
                    <p>Dear %s,</p>
                    <p>Please verify your email address by clicking the button below:</p>
                    <p><a href="${verificationUrl}" style="background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Verify Email</a></p>
                    <p><small>This link will expire in 1 hour for security reasons.</small></p>
                    <p>If you didn't request this verification, please ignore this email.</p>
                    <p>Best regards,<br/>The MCP Team</p>
                </body>
                </html>
                """.formatted(userName))
            .variables(Map.of(
                "userName", userName,
                "verificationToken", verificationToken,
                "verificationUrl", "${app.base-url}/verify-email?token=" + verificationToken
            ))
            .build();
    }
}