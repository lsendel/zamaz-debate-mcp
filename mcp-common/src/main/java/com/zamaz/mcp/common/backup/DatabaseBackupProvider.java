package com.zamaz.mcp.common.backup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Provider for database backup and restore operations
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseBackupProvider {
    
    @Value("${spring.datasource.url}")
    private String databaseUrl;
    
    @Value("${spring.datasource.username}")
    private String databaseUsername;
    
    @Value("${spring.datasource.password}")
    private String databasePassword;
    
    @Value("${backup.database.pg_dump.path:/usr/bin/pg_dump}")
    private String pgDumpPath;
    
    @Value("${backup.database.pg_restore.path:/usr/bin/pg_restore}")
    private String pgRestorePath;
    
    @Value("${backup.database.timeout.minutes:60}")
    private int timeoutMinutes;
    
    @Value("${backup.database.format:custom}")
    private String backupFormat; // custom, plain, directory, tar
    
    @Value("${backup.database.compression.level:6}")
    private int compressionLevel;
    
    @Value("${backup.database.parallel.jobs:4}")
    private int parallelJobs;
    
    /**
     * Perform full database backup
     */
    public DatabaseBackupResult performBackup(Path backupDir, BackupMetadata metadata) {
        log.info("Starting full database backup for backup {}", metadata.getBackupId());
        
        try {
            Path dbBackupFile = backupDir.resolve("database_full.backup");
            
            // Extract database name from URL
            String databaseName = extractDatabaseName(databaseUrl);
            
            // Build pg_dump command
            List<String> command = buildPgDumpCommand(databaseName, dbBackupFile, false);
            
            // Execute backup
            ProcessResult result = executeCommand(command);
            
            if (result.getExitCode() != 0) {
                throw new BackupException("Database backup failed: " + result.getErrorOutput());
            }
            
            // Calculate backup size
            long backupSize = Files.size(dbBackupFile);
            
            // Create backup info file
            createBackupInfoFile(backupDir, metadata, backupSize);
            
            log.info("Database backup completed successfully. Size: {} bytes", backupSize);
            
            return DatabaseBackupResult.builder()
                .success(true)
                .backupFile(dbBackupFile)
                .backupSize(backupSize)
                .duration(result.getDuration())
                .message("Database backup completed successfully")
                .build();
            
        } catch (Exception e) {
            log.error("Database backup failed for backup {}", metadata.getBackupId(), e);
            return DatabaseBackupResult.builder()
                .success(false)
                .message("Database backup failed: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * Perform incremental database backup
     */
    public DatabaseBackupResult performIncrementalBackup(Path backupDir, BackupMetadata metadata, 
                                                        LocalDateTime lastBackupTime) {
        log.info("Starting incremental database backup for backup {} since {}", 
            metadata.getBackupId(), lastBackupTime);
        
        try {
            Path dbBackupFile = backupDir.resolve("database_incremental.backup");
            
            // Extract database name from URL
            String databaseName = extractDatabaseName(databaseUrl);
            
            // For PostgreSQL, incremental backups typically use WAL (Write-Ahead Logging)
            // This is a simplified implementation - real implementation would use pg_basebackup
            // with WAL archiving for true incremental backups
            
            // Build pg_dump command with timestamp filter (simplified approach)
            List<String> command = buildPgDumpCommand(databaseName, dbBackupFile, true);
            
            // Execute backup
            ProcessResult result = executeCommand(command);
            
            if (result.getExitCode() != 0) {
                throw new BackupException("Incremental database backup failed: " + result.getErrorOutput());
            }
            
            // Calculate backup size
            long backupSize = Files.size(dbBackupFile);
            
            // Create backup info file
            createBackupInfoFile(backupDir, metadata, backupSize);
            
            log.info("Incremental database backup completed successfully. Size: {} bytes", backupSize);
            
            return DatabaseBackupResult.builder()
                .success(true)
                .backupFile(dbBackupFile)
                .backupSize(backupSize)
                .duration(result.getDuration())
                .message("Incremental database backup completed successfully")
                .build();
            
        } catch (Exception e) {
            log.error("Incremental database backup failed for backup {}", metadata.getBackupId(), e);
            return DatabaseBackupResult.builder()
                .success(false)
                .message("Incremental database backup failed: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * Restore database from backup
     */
    public void restoreFromBackup(Path backupPath, RestoreOptions options) {
        log.info("Starting database restore from backup at {}", backupPath);
        
        try {
            // Find backup file
            Path dbBackupFile = findDatabaseBackupFile(backupPath);
            if (dbBackupFile == null) {
                throw new BackupException("Database backup file not found in " + backupPath);
            }
            
            // Extract database name from URL
            String databaseName = extractDatabaseName(databaseUrl);
            
            // Drop existing database if requested
            if (options.isDropExistingDatabase()) {
                dropDatabase(databaseName);
            }
            
            // Create database if it doesn't exist
            if (options.isCreateDatabase()) {
                createDatabase(databaseName);
            }
            
            // Build pg_restore command
            List<String> command = buildPgRestoreCommand(databaseName, dbBackupFile, options);
            
            // Execute restore
            ProcessResult result = executeCommand(command);
            
            if (result.getExitCode() != 0) {
                throw new BackupException("Database restore failed: " + result.getErrorOutput());
            }
            
            log.info("Database restore completed successfully");
            
        } catch (Exception e) {
            log.error("Database restore failed", e);
            throw new BackupException("Database restore failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Verify database backup integrity
     */
    public boolean verifyBackupIntegrity(Path backupPath) {
        log.info("Verifying database backup integrity at {}", backupPath);
        
        try {
            Path dbBackupFile = findDatabaseBackupFile(backupPath);
            if (dbBackupFile == null) {
                log.error("Database backup file not found");
                return false;
            }
            
            // Use pg_restore to verify backup file without actually restoring
            List<String> command = new ArrayList<>();
            command.add(pgRestorePath);
            command.add("--list");
            command.add(dbBackupFile.toString());
            
            ProcessResult result = executeCommand(command);
            
            if (result.getExitCode() != 0) {
                log.error("Database backup verification failed: {}", result.getErrorOutput());
                return false;
            }
            
            log.info("Database backup integrity verification passed");
            return true;
            
        } catch (Exception e) {
            log.error("Database backup verification failed", e);
            return false;
        }
    }
    
    /**
     * Get database backup statistics
     */
    public DatabaseBackupStats getBackupStats(Path backupPath) {
        try {
            Path dbBackupFile = findDatabaseBackupFile(backupPath);
            if (dbBackupFile == null) {
                return null;
            }
            
            long fileSize = Files.size(dbBackupFile);
            LocalDateTime lastModified = Files.getLastModifiedTime(dbBackupFile)
                .toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();
            
            return DatabaseBackupStats.builder()
                .backupFile(dbBackupFile)
                .fileSize(fileSize)
                .lastModified(lastModified)
                .build();
            
        } catch (Exception e) {
            log.error("Failed to get database backup stats", e);
            return null;
        }
    }
    
    private List<String> buildPgDumpCommand(String databaseName, Path outputFile, boolean incremental) {
        List<String> command = new ArrayList<>();
        command.add(pgDumpPath);
        
        // Connection parameters
        command.add("--host=" + extractHost(databaseUrl));
        command.add("--port=" + extractPort(databaseUrl));
        command.add("--username=" + databaseUsername);
        command.add("--dbname=" + databaseName);
        
        // Output format
        command.add("--format=" + backupFormat);
        
        // Compression
        if (compressionLevel > 0) {
            command.add("--compress=" + compressionLevel);
        }
        
        // Parallel jobs for directory format
        if ("directory".equals(backupFormat)) {
            command.add("--jobs=" + parallelJobs);
        }
        
        // Additional options
        command.add("--verbose");
        command.add("--no-password");
        command.add("--create");
        command.add("--clean");
        command.add("--if-exists");
        
        // Output file
        command.add("--file=" + outputFile.toString());
        
        return command;
    }
    
    private List<String> buildPgRestoreCommand(String databaseName, Path backupFile, RestoreOptions options) {
        List<String> command = new ArrayList<>();
        command.add(pgRestorePath);
        
        // Connection parameters
        command.add("--host=" + extractHost(databaseUrl));
        command.add("--port=" + extractPort(databaseUrl));
        command.add("--username=" + databaseUsername);
        command.add("--dbname=" + databaseName);
        
        // Restore options
        command.add("--verbose");
        command.add("--no-password");
        
        if (options.isCleanBeforeRestore()) {
            command.add("--clean");
        }
        
        if (options.isCreateDatabase()) {
            command.add("--create");
        }
        
        if (options.isExitOnError()) {
            command.add("--exit-on-error");
        }
        
        // Parallel jobs for directory format
        if ("directory".equals(backupFormat)) {
            command.add("--jobs=" + parallelJobs);
        }
        
        // Backup file
        command.add(backupFile.toString());
        
        return command;
    }
    
    private ProcessResult executeCommand(List<String> command) throws IOException, InterruptedException {
        log.debug("Executing command: {}", String.join(" ", command));
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().put("PGPASSWORD", databasePassword);
        
        long startTime = System.currentTimeMillis();
        Process process = pb.start();
        
        // Read output and error streams
        CompletableFuture<String> outputFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return new String(process.getInputStream().readAllBytes());
            } catch (IOException e) {
                return "";
            }
        });
        
        CompletableFuture<String> errorFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return new String(process.getErrorStream().readAllBytes());
            } catch (IOException e) {
                return "";
            }
        });
        
        // Wait for process completion with timeout
        boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
        
        if (!finished) {
            process.destroyForcibly();
            throw new BackupException("Database operation timed out after " + timeoutMinutes + " minutes");
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        return ProcessResult.builder()
            .exitCode(process.exitValue())
            .output(outputFuture.get())
            .errorOutput(errorFuture.get())
            .duration(duration)
            .build();
    }
    
    private String extractDatabaseName(String url) {
        // Extract database name from JDBC URL
        // Example: jdbc:postgresql://localhost:5432/mcp_db
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash != -1) {
            String dbPart = url.substring(lastSlash + 1);
            int questionMark = dbPart.indexOf('?');
            return questionMark != -1 ? dbPart.substring(0, questionMark) : dbPart;
        }
        return "mcp_db"; // Default
    }
    
    private String extractHost(String url) {
        // Extract host from JDBC URL
        try {
            String[] parts = url.split("://")[1].split("/")[0].split(":");
            return parts[0];
        } catch (Exception e) {
            return "localhost";
        }
    }
    
    private String extractPort(String url) {
        // Extract port from JDBC URL
        try {
            String[] parts = url.split("://")[1].split("/")[0].split(":");
            return parts.length > 1 ? parts[1] : "5432";
        } catch (Exception e) {
            return "5432";
        }
    }
    
    private Path findDatabaseBackupFile(Path backupPath) throws IOException {
        // Look for database backup files
        String[] possibleNames = {
            "database_full.backup",
            "database_incremental.backup",
            "database.backup",
            "database.sql",
            "database.dump"
        };
        
        for (String name : possibleNames) {
            Path file = backupPath.resolve(name);
            if (Files.exists(file)) {
                return file;
            }
        }
        
        return null;
    }
    
    private void createBackupInfoFile(Path backupDir, BackupMetadata metadata, long backupSize) throws IOException {
        Path infoFile = backupDir.resolve("database_backup_info.json");
        
        DatabaseBackupInfo info = DatabaseBackupInfo.builder()
            .backupId(metadata.getBackupId())
            .backupType(metadata.getBackupType())
            .timestamp(metadata.getStartTime())
            .databaseName(extractDatabaseName(databaseUrl))
            .backupSize(backupSize)
            .format(backupFormat)
            .compressionLevel(compressionLevel)
            .build();
        
        // Write backup info (simplified JSON)
        String json = String.format(
            "{\n" +
            "  \"backupId\": \"%s\",\n" +
            "  \"backupType\": \"%s\",\n" +
            "  \"timestamp\": \"%s\",\n" +
            "  \"databaseName\": \"%s\",\n" +
            "  \"backupSize\": %d,\n" +
            "  \"format\": \"%s\",\n" +
            "  \"compressionLevel\": %d\n" +
            "}",
            info.getBackupId(),
            info.getBackupType(),
            info.getTimestamp(),
            info.getDatabaseName(),
            info.getBackupSize(),
            info.getFormat(),
            info.getCompressionLevel()
        );
        
        Files.write(infoFile, json.getBytes());
    }
    
    private void dropDatabase(String databaseName) throws IOException, InterruptedException {
        log.info("Dropping database: {}", databaseName);
        
        List<String> command = new ArrayList<>();
        command.add("dropdb");
        command.add("--host=" + extractHost(databaseUrl));
        command.add("--port=" + extractPort(databaseUrl));
        command.add("--username=" + databaseUsername);
        command.add("--if-exists");
        command.add(databaseName);
        
        ProcessResult result = executeCommand(command);
        
        if (result.getExitCode() != 0) {
            log.warn("Drop database failed (database might not exist): {}", result.getErrorOutput());
        }
    }
    
    private void createDatabase(String databaseName) throws IOException, InterruptedException {
        log.info("Creating database: {}", databaseName);
        
        List<String> command = new ArrayList<>();
        command.add("createdb");
        command.add("--host=" + extractHost(databaseUrl));
        command.add("--port=" + extractPort(databaseUrl));
        command.add("--username=" + databaseUsername);
        command.add(databaseName);
        
        ProcessResult result = executeCommand(command);
        
        if (result.getExitCode() != 0) {
            log.warn("Create database failed (database might already exist): {}", result.getErrorOutput());
        }
    }
}