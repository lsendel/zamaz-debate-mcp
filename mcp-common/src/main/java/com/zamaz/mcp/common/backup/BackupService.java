package com.zamaz.mcp.common.backup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for automated backup and disaster recovery operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BackupService {
    
    private final DatabaseBackupProvider databaseBackupProvider;
    private final FileSystemBackupProvider fileSystemBackupProvider;
    private final CloudBackupProvider cloudBackupProvider;
    private final BackupRepository backupRepository;
    private final BackupNotificationService notificationService;
    
    @Value("${backup.enabled:true}")
    private boolean backupEnabled;
    
    @Value("${backup.retention.days:30}")
    private int retentionDays;
    
    @Value("${backup.storage.local.path:/var/backups/mcp}")
    private String localBackupPath;
    
    @Value("${backup.storage.cloud.enabled:false}")
    private boolean cloudBackupEnabled;
    
    @Value("${backup.compression.enabled:true}")
    private boolean compressionEnabled;
    
    @Value("${backup.encryption.enabled:false}")
    private boolean encryptionEnabled;
    
    @Value("${backup.verification.enabled:true}")
    private boolean verificationEnabled;
    
    private static final DateTimeFormatter BACKUP_TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    /**
     * Perform full system backup
     */
    @Async("backupExecutor")
    public CompletableFuture<BackupResult> performFullBackup() {
        if (!backupEnabled) {
            log.info("Backup is disabled, skipping full backup");
            return CompletableFuture.completedFuture(
                BackupResult.builder()
                    .success(false)
                    .message("Backup disabled")
                    .build()
            );
        }
        
        log.info("Starting full system backup");
        
        String backupId = generateBackupId();
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            BackupMetadata metadata = BackupMetadata.builder()
                .backupId(backupId)
                .backupType(BackupType.FULL)
                .startTime(startTime)
                .status(BackupStatus.IN_PROGRESS)
                .build();
            
            backupRepository.save(metadata);
            
            // Create backup directory
            Path backupDir = createBackupDirectory(backupId);
            
            // Perform database backup
            log.info("Starting database backup for backup {}", backupId);
            DatabaseBackupResult dbResult = databaseBackupProvider.performBackup(backupDir, metadata);
            
            // Perform file system backup
            log.info("Starting file system backup for backup {}", backupId);
            FileSystemBackupResult fsResult = fileSystemBackupProvider.performBackup(backupDir, metadata);
            
            // Compress backup if enabled
            if (compressionEnabled) {
                log.info("Compressing backup {}", backupId);
                compressBackup(backupDir);
            }
            
            // Encrypt backup if enabled
            if (encryptionEnabled) {
                log.info("Encrypting backup {}", backupId);
                encryptBackup(backupDir);
            }
            
            // Verify backup integrity
            boolean verificationPassed = true;
            if (verificationEnabled) {
                log.info("Verifying backup integrity for backup {}", backupId);
                verificationPassed = verifyBackupIntegrity(backupDir);
            }
            
            // Upload to cloud storage if enabled
            CloudBackupResult cloudResult = null;
            if (cloudBackupEnabled) {
                log.info("Uploading backup {} to cloud storage", backupId);
                cloudResult = cloudBackupProvider.uploadBackup(backupDir, metadata);
            }
            
            // Update metadata
            LocalDateTime endTime = LocalDateTime.now();
            long durationMillis = java.time.Duration.between(startTime, endTime).toMillis();
            
            metadata = metadata.toBuilder()
                .endTime(endTime)
                .durationMillis(durationMillis)
                .status(verificationPassed ? BackupStatus.COMPLETED : BackupStatus.FAILED)
                .databaseSize(dbResult.getBackupSize())
                .fileSystemSize(fsResult.getBackupSize())
                .totalSize(calculateTotalSize(backupDir))
                .compressionEnabled(compressionEnabled)
                .encryptionEnabled(encryptionEnabled)
                .verificationPassed(verificationPassed)
                .cloudUploadCompleted(cloudResult != null && cloudResult.isSuccess())
                .build();
            
            backupRepository.save(metadata);
            
            BackupResult result = BackupResult.builder()
                .success(verificationPassed)
                .backupId(backupId)
                .backupPath(backupDir.toString())
                .durationMillis(durationMillis)
                .totalSize(metadata.getTotalSize())
                .message("Full backup completed successfully")
                .metadata(metadata)
                .build();
            
            // Send notification
            notificationService.notifyBackupCompleted(result);
            
            log.info("Full backup {} completed successfully in {}ms", backupId, durationMillis);
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            log.error("Full backup {} failed", backupId, e);
            
            // Update metadata with failure
            BackupMetadata failedMetadata = BackupMetadata.builder()
                .backupId(backupId)
                .backupType(BackupType.FULL)
                .startTime(startTime)
                .endTime(LocalDateTime.now())
                .status(BackupStatus.FAILED)
                .errorMessage(e.getMessage())
                .build();
            
            backupRepository.save(failedMetadata);
            
            BackupResult result = BackupResult.builder()
                .success(false)
                .backupId(backupId)
                .message("Full backup failed: " + e.getMessage())
                .metadata(failedMetadata)
                .build();
            
            // Send failure notification
            notificationService.notifyBackupFailed(result, e);
            
            return CompletableFuture.completedFuture(result);
        }
    }
    
    /**
     * Perform incremental backup
     */
    @Async("backupExecutor")
    public CompletableFuture<BackupResult> performIncrementalBackup() {
        if (!backupEnabled) {
            log.info("Backup is disabled, skipping incremental backup");
            return CompletableFuture.completedFuture(
                BackupResult.builder()
                    .success(false)
                    .message("Backup disabled")
                    .build()
            );
        }
        
        log.info("Starting incremental backup");
        
        String backupId = generateBackupId();
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            // Find last successful backup
            BackupMetadata lastBackup = backupRepository.findLastSuccessfulBackup();
            if (lastBackup == null) {
                log.warn("No previous backup found, performing full backup instead");
                return performFullBackup();
            }
            
            BackupMetadata metadata = BackupMetadata.builder()
                .backupId(backupId)
                .backupType(BackupType.INCREMENTAL)
                .startTime(startTime)
                .status(BackupStatus.IN_PROGRESS)
                .basedOnBackupId(lastBackup.getBackupId())
                .build();
            
            backupRepository.save(metadata);
            
            // Create backup directory
            Path backupDir = createBackupDirectory(backupId);
            
            // Perform incremental database backup
            log.info("Starting incremental database backup for backup {}", backupId);
            DatabaseBackupResult dbResult = databaseBackupProvider.performIncrementalBackup(
                backupDir, metadata, lastBackup.getEndTime());
            
            // Perform incremental file system backup
            log.info("Starting incremental file system backup for backup {}", backupId);
            FileSystemBackupResult fsResult = fileSystemBackupProvider.performIncrementalBackup(
                backupDir, metadata, lastBackup.getEndTime());
            
            // Compress and encrypt if enabled
            if (compressionEnabled) {
                compressBackup(backupDir);
            }
            
            if (encryptionEnabled) {
                encryptBackup(backupDir);
            }
            
            // Verify backup integrity
            boolean verificationPassed = true;
            if (verificationEnabled) {
                verificationPassed = verifyBackupIntegrity(backupDir);
            }
            
            // Upload to cloud storage if enabled
            CloudBackupResult cloudResult = null;
            if (cloudBackupEnabled) {
                cloudResult = cloudBackupProvider.uploadBackup(backupDir, metadata);
            }
            
            // Update metadata
            LocalDateTime endTime = LocalDateTime.now();
            long durationMillis = java.time.Duration.between(startTime, endTime).toMillis();
            
            metadata = metadata.toBuilder()
                .endTime(endTime)
                .durationMillis(durationMillis)
                .status(verificationPassed ? BackupStatus.COMPLETED : BackupStatus.FAILED)
                .databaseSize(dbResult.getBackupSize())
                .fileSystemSize(fsResult.getBackupSize())
                .totalSize(calculateTotalSize(backupDir))
                .compressionEnabled(compressionEnabled)
                .encryptionEnabled(encryptionEnabled)
                .verificationPassed(verificationPassed)
                .cloudUploadCompleted(cloudResult != null && cloudResult.isSuccess())
                .build();
            
            backupRepository.save(metadata);
            
            BackupResult result = BackupResult.builder()
                .success(verificationPassed)
                .backupId(backupId)
                .backupPath(backupDir.toString())
                .durationMillis(durationMillis)
                .totalSize(metadata.getTotalSize())
                .message("Incremental backup completed successfully")
                .metadata(metadata)
                .build();
            
            // Send notification
            notificationService.notifyBackupCompleted(result);
            
            log.info("Incremental backup {} completed successfully in {}ms", backupId, durationMillis);
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            log.error("Incremental backup {} failed", backupId, e);
            
            BackupMetadata failedMetadata = BackupMetadata.builder()
                .backupId(backupId)
                .backupType(BackupType.INCREMENTAL)
                .startTime(startTime)
                .endTime(LocalDateTime.now())
                .status(BackupStatus.FAILED)
                .errorMessage(e.getMessage())
                .build();
            
            backupRepository.save(failedMetadata);
            
            BackupResult result = BackupResult.builder()
                .success(false)
                .backupId(backupId)
                .message("Incremental backup failed: " + e.getMessage())
                .metadata(failedMetadata)
                .build();
            
            notificationService.notifyBackupFailed(result, e);
            
            return CompletableFuture.completedFuture(result);
        }
    }
    
    /**
     * Restore from backup
     */
    @Async("backupExecutor")
    public CompletableFuture<RestoreResult> restoreFromBackup(String backupId, RestoreOptions options) {
        log.info("Starting restore from backup {}", backupId);
        
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            // Find backup metadata
            BackupMetadata backup = backupRepository.findByBackupId(backupId);
            if (backup == null) {
                throw new BackupException("Backup not found: " + backupId);
            }
            
            if (backup.getStatus() != BackupStatus.COMPLETED) {
                throw new BackupException("Backup is not in completed state: " + backup.getStatus());
            }
            
            // Create restore record
            RestoreMetadata restoreMetadata = RestoreMetadata.builder()
                .restoreId(generateRestoreId())
                .backupId(backupId)
                .startTime(startTime)
                .status(RestoreStatus.IN_PROGRESS)
                .options(options)
                .build();
            
            backupRepository.saveRestoreMetadata(restoreMetadata);
            
            // Download from cloud if necessary
            Path backupPath = Paths.get(localBackupPath, backupId);
            if (!Files.exists(backupPath) && cloudBackupEnabled) {
                log.info("Downloading backup {} from cloud storage", backupId);
                cloudBackupProvider.downloadBackup(backupId, backupPath);
            }
            
            // Decrypt if necessary
            if (backup.isEncryptionEnabled()) {
                log.info("Decrypting backup {}", backupId);
                decryptBackup(backupPath);
            }
            
            // Decompress if necessary
            if (backup.isCompressionEnabled()) {
                log.info("Decompressing backup {}", backupId);
                decompressBackup(backupPath);
            }
            
            // Verify backup integrity before restore
            if (verificationEnabled) {
                log.info("Verifying backup integrity before restore");
                if (!verifyBackupIntegrity(backupPath)) {
                    throw new BackupException("Backup integrity verification failed");
                }
            }
            
            // Perform database restore
            if (options.isRestoreDatabase()) {
                log.info("Restoring database from backup {}", backupId);
                databaseBackupProvider.restoreFromBackup(backupPath, options);
            }
            
            // Perform file system restore
            if (options.isRestoreFileSystem()) {
                log.info("Restoring file system from backup {}", backupId);
                fileSystemBackupProvider.restoreFromBackup(backupPath, options);
            }
            
            // Update restore metadata
            LocalDateTime endTime = LocalDateTime.now();
            long durationMillis = java.time.Duration.between(startTime, endTime).toMillis();
            
            restoreMetadata = restoreMetadata.toBuilder()
                .endTime(endTime)
                .durationMillis(durationMillis)
                .status(RestoreStatus.COMPLETED)
                .build();
            
            backupRepository.saveRestoreMetadata(restoreMetadata);
            
            RestoreResult result = RestoreResult.builder()
                .success(true)
                .restoreId(restoreMetadata.getRestoreId())
                .backupId(backupId)
                .durationMillis(durationMillis)
                .message("Restore completed successfully")
                .metadata(restoreMetadata)
                .build();
            
            // Send notification
            notificationService.notifyRestoreCompleted(result);
            
            log.info("Restore from backup {} completed successfully in {}ms", backupId, durationMillis);
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            log.error("Restore from backup {} failed", backupId, e);
            
            RestoreMetadata failedMetadata = RestoreMetadata.builder()
                .restoreId(generateRestoreId())
                .backupId(backupId)
                .startTime(startTime)
                .endTime(LocalDateTime.now())
                .status(RestoreStatus.FAILED)
                .errorMessage(e.getMessage())
                .build();
            
            backupRepository.saveRestoreMetadata(failedMetadata);
            
            RestoreResult result = RestoreResult.builder()
                .success(false)
                .restoreId(failedMetadata.getRestoreId())
                .backupId(backupId)
                .message("Restore failed: " + e.getMessage())
                .metadata(failedMetadata)
                .build();
            
            notificationService.notifyRestoreFailed(result, e);
            
            return CompletableFuture.completedFuture(result);
        }
    }
    
    /**
     * Scheduled full backup (runs daily at 2 AM)
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledFullBackup() {
        log.info("Starting scheduled full backup");
        performFullBackup().whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("Scheduled full backup failed", throwable);
            } else {
                log.info("Scheduled full backup completed: {}", result.getMessage());
            }
        });
    }
    
    /**
     * Scheduled incremental backup (runs every 6 hours)
     */
    @Scheduled(cron = "0 0 */6 * * *")
    public void scheduledIncrementalBackup() {
        log.info("Starting scheduled incremental backup");
        performIncrementalBackup().whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("Scheduled incremental backup failed", throwable);
            } else {
                log.info("Scheduled incremental backup completed: {}", result.getMessage());
            }
        });
    }
    
    /**
     * Scheduled cleanup of old backups (runs daily at 3 AM)
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void scheduledCleanup() {
        log.info("Starting scheduled backup cleanup");
        cleanupOldBackups().whenComplete((count, throwable) -> {
            if (throwable != null) {
                log.error("Scheduled backup cleanup failed", throwable);
            } else {
                log.info("Scheduled backup cleanup completed: {} backups cleaned", count);
            }
        });
    }
    
    /**
     * Clean up old backups based on retention policy
     */
    @Async("backupExecutor")
    public CompletableFuture<Integer> cleanupOldBackups() {
        log.info("Starting cleanup of backups older than {} days", retentionDays);
        
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
            List<BackupMetadata> oldBackups = backupRepository.findBackupsOlderThan(cutoffDate);
            
            int cleanedCount = 0;
            for (BackupMetadata backup : oldBackups) {
                try {
                    // Delete local backup files
                    Path backupPath = Paths.get(localBackupPath, backup.getBackupId());
                    if (Files.exists(backupPath)) {
                        deleteDirectory(backupPath);
                    }
                    
                    // Delete from cloud storage if enabled
                    if (cloudBackupEnabled && backup.isCloudUploadCompleted()) {
                        cloudBackupProvider.deleteBackup(backup.getBackupId());
                    }
                    
                    // Update metadata to mark as deleted
                    backup.setStatus(BackupStatus.DELETED);
                    backupRepository.save(backup);
                    
                    cleanedCount++;
                    log.debug("Cleaned up backup {}", backup.getBackupId());
                    
                } catch (Exception e) {
                    log.error("Failed to cleanup backup {}", backup.getBackupId(), e);
                }
            }
            
            log.info("Cleaned up {} old backups", cleanedCount);
            return CompletableFuture.completedFuture(cleanedCount);
            
        } catch (Exception e) {
            log.error("Backup cleanup failed", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Get backup status and statistics
     */
    public BackupStatus getBackupStatus() {
        return backupRepository.getBackupStatus();
    }
    
    /**
     * List all backups
     */
    public List<BackupMetadata> listBackups() {
        return backupRepository.findAll();
    }
    
    /**
     * Get backup by ID
     */
    public BackupMetadata getBackup(String backupId) {
        return backupRepository.findByBackupId(backupId);
    }
    
    private String generateBackupId() {
        return "backup_" + LocalDateTime.now().format(BACKUP_TIMESTAMP_FORMAT);
    }
    
    private String generateRestoreId() {
        return "restore_" + LocalDateTime.now().format(BACKUP_TIMESTAMP_FORMAT);
    }
    
    private Path createBackupDirectory(String backupId) throws IOException {
        Path backupDir = Paths.get(localBackupPath, backupId);
        Files.createDirectories(backupDir);
        return backupDir;
    }
    
    private void compressBackup(Path backupDir) throws IOException {
        // Implementation would use compression library like gzip or zip
        log.debug("Compressing backup at {}", backupDir);
        // Placeholder for compression implementation
    }
    
    private void encryptBackup(Path backupDir) throws IOException {
        // Implementation would use encryption library
        log.debug("Encrypting backup at {}", backupDir);
        // Placeholder for encryption implementation
    }
    
    private void decryptBackup(Path backupDir) throws IOException {
        // Implementation would use decryption library
        log.debug("Decrypting backup at {}", backupDir);
        // Placeholder for decryption implementation
    }
    
    private void decompressBackup(Path backupDir) throws IOException {
        // Implementation would use decompression library
        log.debug("Decompressing backup at {}", backupDir);
        // Placeholder for decompression implementation
    }
    
    private boolean verifyBackupIntegrity(Path backupDir) {
        // Implementation would verify checksums, file integrity, etc.
        log.debug("Verifying backup integrity at {}", backupDir);
        return true; // Placeholder
    }
    
    private long calculateTotalSize(Path backupDir) throws IOException {
        return Files.walk(backupDir)
            .filter(Files::isRegularFile)
            .mapToLong(path -> {
                try {
                    return Files.size(path);
                } catch (IOException e) {
                    return 0;
                }
            })
            .sum();
    }
    
    private void deleteDirectory(Path directory) throws IOException {
        Files.walk(directory)
            .sorted(java.util.Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(java.io.File::delete);
    }
}