package com.zamaz.mcp.common.backup;

import com.zamaz.mcp.common.backup.BackupModels.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for sending backup-related notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BackupNotificationService {
    
    private final JavaMailSender mailSender;
    
    @Value("${backup.notifications.enabled:true}")
    private boolean notificationsEnabled;
    
    @Value("${backup.notifications.email.from:backup@mcp.example.com}")
    private String fromEmail;
    
    @Value("${backup.notifications.email.to:admin@mcp.example.com}")
    private List<String> toEmails;
    
    @Value("${backup.notifications.slack.enabled:false}")
    private boolean slackEnabled;
    
    @Value("${backup.notifications.slack.webhook:}")
    private String slackWebhook;
    
    @Value("${backup.notifications.success.enabled:true}")
    private boolean successNotificationsEnabled;
    
    @Value("${backup.notifications.failure.enabled:true}")
    private boolean failureNotificationsEnabled;
    
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Notify backup completed successfully
     */
    public void notifyBackupCompleted(BackupResult result) {
        if (!notificationsEnabled || !successNotificationsEnabled) {
            return;
        }
        
        log.info("Sending backup completion notification for backup {}", result.getBackupId());
        
        try {
            String subject = String.format("✅ Backup Completed Successfully - %s", result.getBackupId());
            String body = buildBackupCompletionMessage(result);
            
            sendEmailNotification(subject, body);
            
            if (slackEnabled) {
                sendSlackNotification(subject, body, "good");
            }
            
        } catch (Exception e) {
            log.error("Failed to send backup completion notification", e);
        }
    }
    
    /**
     * Notify backup failed
     */
    public void notifyBackupFailed(BackupResult result, Exception exception) {
        if (!notificationsEnabled || !failureNotificationsEnabled) {
            return;
        }
        
        log.info("Sending backup failure notification for backup {}", result.getBackupId());
        
        try {
            String subject = String.format("❌ Backup Failed - %s", result.getBackupId());
            String body = buildBackupFailureMessage(result, exception);
            
            sendEmailNotification(subject, body);
            
            if (slackEnabled) {
                sendSlackNotification(subject, body, "danger");
            }
            
        } catch (Exception e) {
            log.error("Failed to send backup failure notification", e);
        }
    }
    
    /**
     * Notify restore completed successfully
     */
    public void notifyRestoreCompleted(RestoreResult result) {
        if (!notificationsEnabled || !successNotificationsEnabled) {
            return;
        }
        
        log.info("Sending restore completion notification for restore {}", result.getRestoreId());
        
        try {
            String subject = String.format("✅ Restore Completed Successfully - %s", result.getRestoreId());
            String body = buildRestoreCompletionMessage(result);
            
            sendEmailNotification(subject, body);
            
            if (slackEnabled) {
                sendSlackNotification(subject, body, "good");
            }
            
        } catch (Exception e) {
            log.error("Failed to send restore completion notification", e);
        }
    }
    
    /**
     * Notify restore failed
     */
    public void notifyRestoreFailed(RestoreResult result, Exception exception) {
        if (!notificationsEnabled || !failureNotificationsEnabled) {
            return;
        }
        
        log.info("Sending restore failure notification for restore {}", result.getRestoreId());
        
        try {
            String subject = String.format("❌ Restore Failed - %s", result.getRestoreId());
            String body = buildRestoreFailureMessage(result, exception);
            
            sendEmailNotification(subject, body);
            
            if (slackEnabled) {
                sendSlackNotification(subject, body, "danger");
            }
            
        } catch (Exception e) {
            log.error("Failed to send restore failure notification", e);
        }
    }
    
    /**
     * Notify backup storage issues
     */
    public void notifyStorageIssues(String message, Exception exception) {
        if (!notificationsEnabled) {
            return;
        }
        
        log.info("Sending storage issue notification");
        
        try {
            String subject = "⚠️ Backup Storage Issue";
            String body = buildStorageIssueMessage(message, exception);
            
            sendEmailNotification(subject, body);
            
            if (slackEnabled) {
                sendSlackNotification(subject, body, "warning");
            }
            
        } catch (Exception e) {
            log.error("Failed to send storage issue notification", e);
        }
    }
    
    private void sendEmailNotification(String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmails.toArray(new String[0]));
            message.setSubject(subject);
            message.setText(body);
            
            mailSender.send(message);
            
            log.debug("Email notification sent successfully");
            
        } catch (Exception e) {
            log.error("Failed to send email notification", e);
        }
    }
    
    private void sendSlackNotification(String title, String message, String color) {
        try {
            // In a real implementation, this would use Slack API or webhook
            // For now, this is a placeholder
            log.debug("Slack notification sent: {} - {}", title, message);
            
        } catch (Exception e) {
            log.error("Failed to send Slack notification", e);
        }
    }
    
    private String buildBackupCompletionMessage(BackupResult result) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Backup completed successfully!\n\n");
        sb.append("Details:\n");
        sb.append("- Backup ID: ").append(result.getBackupId()).append("\n");
        sb.append("- Type: ").append(result.getMetadata().getBackupType()).append("\n");
        sb.append("- Duration: ").append(formatDuration(result.getDurationMillis())).append("\n");
        sb.append("- Size: ").append(formatSize(result.getTotalSize())).append("\n");
        sb.append("- Started: ").append(result.getMetadata().getStartTime().format(DATETIME_FORMAT)).append("\n");
        sb.append("- Completed: ").append(result.getMetadata().getEndTime().format(DATETIME_FORMAT)).append("\n");
        sb.append("- Compression: ").append(result.getMetadata().isCompressionEnabled() ? "Enabled" : "Disabled").append("\n");
        sb.append("- Encryption: ").append(result.getMetadata().isEncryptionEnabled() ? "Enabled" : "Disabled").append("\n");
        sb.append("- Verification: ").append(result.getMetadata().isVerificationPassed() ? "Passed" : "Failed").append("\n");
        sb.append("- Cloud Upload: ").append(result.getMetadata().isCloudUploadCompleted() ? "Completed" : "Skipped").append("\n");
        
        if (result.getMetadata().getBasedOnBackupId() != null) {
            sb.append("- Based on Backup: ").append(result.getMetadata().getBasedOnBackupId()).append("\n");
        }
        
        sb.append("\nBreakdown:\n");
        sb.append("- Database Size: ").append(formatSize(result.getMetadata().getDatabaseSize())).append("\n");
        sb.append("- File System Size: ").append(formatSize(result.getMetadata().getFileSystemSize())).append("\n");
        
        if (result.getWarnings() != null && !result.getWarnings().isEmpty()) {
            sb.append("\nWarnings:\n");
            for (String warning : result.getWarnings()) {
                sb.append("- ").append(warning).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    private String buildBackupFailureMessage(BackupResult result, Exception exception) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Backup failed!\n\n");
        sb.append("Details:\n");
        sb.append("- Backup ID: ").append(result.getBackupId()).append("\n");
        sb.append("- Error: ").append(result.getMessage()).append("\n");
        sb.append("- Duration: ").append(formatDuration(result.getDurationMillis())).append("\n");
        
        if (result.getMetadata() != null) {
            sb.append("- Started: ").append(result.getMetadata().getStartTime().format(DATETIME_FORMAT)).append("\n");
            if (result.getMetadata().getEndTime() != null) {
                sb.append("- Failed at: ").append(result.getMetadata().getEndTime().format(DATETIME_FORMAT)).append("\n");
            }
            
            if (result.getMetadata().getErrorMessage() != null) {
                sb.append("- Error Details: ").append(result.getMetadata().getErrorMessage()).append("\n");
            }
        }
        
        if (exception != null) {
            sb.append("- Exception: ").append(exception.getMessage()).append("\n");
        }
        
        if (result.getErrors() != null && !result.getErrors().isEmpty()) {
            sb.append("\nErrors:\n");
            for (String error : result.getErrors()) {
                sb.append("- ").append(error).append("\n");
            }
        }
        
        sb.append("\nPlease check the logs for more details and take appropriate action.");
        
        return sb.toString();
    }
    
    private String buildRestoreCompletionMessage(RestoreResult result) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Restore completed successfully!\n\n");
        sb.append("Details:\n");
        sb.append("- Restore ID: ").append(result.getRestoreId()).append("\n");
        sb.append("- Source Backup: ").append(result.getBackupId()).append("\n");
        sb.append("- Duration: ").append(formatDuration(result.getDurationMillis())).append("\n");
        sb.append("- Started: ").append(result.getMetadata().getStartTime().format(DATETIME_FORMAT)).append("\n");
        sb.append("- Completed: ").append(result.getMetadata().getEndTime().format(DATETIME_FORMAT)).append("\n");
        
        if (result.getMetadata().getOptions() != null) {
            RestoreOptions options = result.getMetadata().getOptions();
            sb.append("\nRestore Options:\n");
            sb.append("- Database: ").append(options.isRestoreDatabase() ? "Restored" : "Skipped").append("\n");
            sb.append("- File System: ").append(options.isRestoreFileSystem() ? "Restored" : "Skipped").append("\n");
            sb.append("- Overwrite Existing: ").append(options.isOverwriteExisting() ? "Yes" : "No").append("\n");
        }
        
        if (result.getWarnings() != null && !result.getWarnings().isEmpty()) {
            sb.append("\nWarnings:\n");
            for (String warning : result.getWarnings()) {
                sb.append("- ").append(warning).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    private String buildRestoreFailureMessage(RestoreResult result, Exception exception) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Restore failed!\n\n");
        sb.append("Details:\n");
        sb.append("- Restore ID: ").append(result.getRestoreId()).append("\n");
        sb.append("- Source Backup: ").append(result.getBackupId()).append("\n");
        sb.append("- Error: ").append(result.getMessage()).append("\n");
        sb.append("- Duration: ").append(formatDuration(result.getDurationMillis())).append("\n");
        
        if (result.getMetadata() != null) {
            sb.append("- Started: ").append(result.getMetadata().getStartTime().format(DATETIME_FORMAT)).append("\n");
            if (result.getMetadata().getEndTime() != null) {
                sb.append("- Failed at: ").append(result.getMetadata().getEndTime().format(DATETIME_FORMAT)).append("\n");
            }
            
            if (result.getMetadata().getErrorMessage() != null) {
                sb.append("- Error Details: ").append(result.getMetadata().getErrorMessage()).append("\n");
            }
        }
        
        if (exception != null) {
            sb.append("- Exception: ").append(exception.getMessage()).append("\n");
        }
        
        if (result.getErrors() != null && !result.getErrors().isEmpty()) {
            sb.append("\nErrors:\n");
            for (String error : result.getErrors()) {
                sb.append("- ").append(error).append("\n");
            }
        }
        
        sb.append("\nPlease check the logs for more details and take appropriate action.");
        
        return sb.toString();
    }
    
    private String buildStorageIssueMessage(String message, Exception exception) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Backup storage issue detected!\n\n");
        sb.append("Issue: ").append(message).append("\n");
        
        if (exception != null) {
            sb.append("Exception: ").append(exception.getMessage()).append("\n");
        }
        
        sb.append("\nPlease check the backup storage configuration and ensure sufficient space is available.");
        
        return sb.toString();
    }
    
    private String formatDuration(long durationMillis) {
        if (durationMillis < 1000) {
            return durationMillis + "ms";
        } else if (durationMillis < 60000) {
            return String.format("%.1fs", durationMillis / 1000.0);
        } else if (durationMillis < 3600000) {
            return String.format("%.1fm", durationMillis / 60000.0);
        } else {
            return String.format("%.1fh", durationMillis / 3600000.0);
        }
    }
    
    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " bytes";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}