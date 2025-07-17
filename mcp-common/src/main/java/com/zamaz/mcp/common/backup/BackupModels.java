package com.zamaz.mcp.common.backup;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data models for backup and restore operations
 */
public class BackupModels {
    
    /**
     * Backup metadata
     */
    @Data
    @Builder(toBuilder = true)
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BackupMetadata {
        private String backupId;
        private BackupType backupType;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long durationMillis;
        private BackupStatus status;
        private long databaseSize;
        private long fileSystemSize;
        private long totalSize;
        private boolean compressionEnabled;
        private boolean encryptionEnabled;
        private boolean verificationPassed;
        private boolean cloudUploadCompleted;
        private String basedOnBackupId; // For incremental backups
        private String errorMessage;
        private List<String> warnings;
    }
    
    /**
     * Backup result
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BackupResult {
        private boolean success;
        private String backupId;
        private String backupPath;
        private long durationMillis;
        private long totalSize;
        private String message;
        private BackupMetadata metadata;
        private List<String> errors;
        private List<String> warnings;
    }
    
    /**
     * Restore metadata
     */
    @Data
    @Builder(toBuilder = true)
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RestoreMetadata {
        private String restoreId;
        private String backupId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long durationMillis;
        private RestoreStatus status;
        private RestoreOptions options;
        private String errorMessage;
        private List<String> warnings;
    }
    
    /**
     * Restore result
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RestoreResult {
        private boolean success;
        private String restoreId;
        private String backupId;
        private long durationMillis;
        private String message;
        private RestoreMetadata metadata;
        private List<String> errors;
        private List<String> warnings;
    }
    
    /**
     * Restore options
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RestoreOptions {
        private boolean restoreDatabase = true;
        private boolean restoreFileSystem = true;
        private boolean dropExistingDatabase = false;
        private boolean createDatabase = true;
        private boolean cleanBeforeRestore = true;
        private boolean overwriteExisting = false;
        private boolean exitOnError = true;
        private String targetDatabaseName;
        private String targetFileSystemPath;
        private List<String> includePatterns;
        private List<String> excludePatterns;
    }
    
    /**
     * Database backup result
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DatabaseBackupResult {
        private boolean success;
        private Path backupFile;
        private long backupSize;
        private long duration;
        private String message;
        private List<String> errors;
    }
    
    /**
     * File system backup result
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FileSystemBackupResult {
        private boolean success;
        private Path backupDirectory;
        private long backupSize;
        private int fileCount;
        private int directoryCount;
        private long duration;
        private String message;
        private List<String> errors;
    }
    
    /**
     * Cloud backup result
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CloudBackupResult {
        private boolean success;
        private String cloudPath;
        private long uploadSize;
        private long duration;
        private String message;
        private List<String> errors;
    }
    
    /**
     * Database backup info
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DatabaseBackupInfo {
        private String backupId;
        private BackupType backupType;
        private LocalDateTime timestamp;
        private String databaseName;
        private long backupSize;
        private String format;
        private int compressionLevel;
    }
    
    /**
     * Database backup stats
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DatabaseBackupStats {
        private Path backupFile;
        private long fileSize;
        private LocalDateTime lastModified;
    }
    
    /**
     * File system backup manifest
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FileSystemBackupManifest {
        private String backupId;
        private BackupType backupType;
        private LocalDateTime timestamp;
        private long totalSize;
        private int fileCount;
        private int directoryCount;
        private List<String> backupPaths;
        private List<String> excludePatterns;
        private boolean includeHidden;
        private boolean preservePermissions;
        private boolean followSymlinks;
    }
    
    /**
     * Process result
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProcessResult {
        private int exitCode;
        private String output;
        private String errorOutput;
        private long duration;
    }
    
    /**
     * Backup exception
     */
    public static class BackupException extends RuntimeException {
        public BackupException(String message) {
            super(message);
        }
        
        public BackupException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

/**
 * Backup type enum
 */
enum BackupType {
    FULL,
    INCREMENTAL,
    DIFFERENTIAL
}

/**
 * Backup status enum
 */
enum BackupStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED,
    DELETED
}

/**
 * Restore status enum
 */
enum RestoreStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED
}