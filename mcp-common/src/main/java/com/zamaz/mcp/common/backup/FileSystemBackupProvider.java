package com.zamaz.mcp.common.backup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Provider for file system backup and restore operations
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FileSystemBackupProvider {
    
    @Value("${backup.filesystem.paths:/var/lib/mcp,/opt/mcp/uploads,/tmp/mcp/cache}")
    private List<String> backupPaths;
    
    @Value("${backup.filesystem.exclude.patterns:.tmp,.cache,.log}")
    private List<String> excludePatterns;
    
    @Value("${backup.filesystem.include.hidden:false}")
    private boolean includeHidden;
    
    @Value("${backup.filesystem.preserve.permissions:true}")
    private boolean preservePermissions;
    
    @Value("${backup.filesystem.follow.symlinks:false}")
    private boolean followSymlinks;
    
    @Value("${backup.filesystem.max.file.size:100MB}")
    private String maxFileSize;
    
    private static final long MAX_FILE_SIZE_BYTES = 100 * 1024 * 1024; // 100MB
    
    /**
     * Perform full file system backup
     */
    public FileSystemBackupResult performBackup(Path backupDir, BackupMetadata metadata) {
        log.info("Starting full file system backup for backup {}", metadata.getBackupId());
        
        try {
            Path fsBackupDir = backupDir.resolve("filesystem");
            Files.createDirectories(fsBackupDir);
            
            long totalSize = 0;
            int totalFiles = 0;
            int totalDirectories = 0;
            List<String> errors = new ArrayList<>();
            
            // Backup each configured path
            for (String pathStr : backupPaths) {
                Path sourcePath = Paths.get(pathStr);
                if (!Files.exists(sourcePath)) {
                    log.warn("Backup path does not exist: {}", pathStr);
                    continue;
                }
                
                log.info("Backing up path: {}", pathStr);
                
                // Create directory structure in backup
                Path targetDir = fsBackupDir.resolve(sourcePath.getFileName());
                
                BackupStats stats = copyDirectoryRecursively(sourcePath, targetDir, null, errors);
                totalSize += stats.getTotalSize();
                totalFiles += stats.getFileCount();
                totalDirectories += stats.getDirectoryCount();
            }
            
            // Create backup manifest
            createBackupManifest(fsBackupDir, metadata, totalSize, totalFiles, totalDirectories);
            
            log.info("File system backup completed. Files: {}, Directories: {}, Size: {} bytes", 
                totalFiles, totalDirectories, totalSize);
            
            return FileSystemBackupResult.builder()
                .success(true)
                .backupDirectory(fsBackupDir)
                .backupSize(totalSize)
                .fileCount(totalFiles)
                .directoryCount(totalDirectories)
                .errors(errors)
                .message("File system backup completed successfully")
                .build();
            
        } catch (Exception e) {
            log.error("File system backup failed for backup {}", metadata.getBackupId(), e);
            return FileSystemBackupResult.builder()
                .success(false)
                .message("File system backup failed: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * Perform incremental file system backup
     */
    public FileSystemBackupResult performIncrementalBackup(Path backupDir, BackupMetadata metadata, 
                                                          LocalDateTime lastBackupTime) {
        log.info("Starting incremental file system backup for backup {} since {}", 
            metadata.getBackupId(), lastBackupTime);
        
        try {
            Path fsBackupDir = backupDir.resolve("filesystem");
            Files.createDirectories(fsBackupDir);
            
            long totalSize = 0;
            int totalFiles = 0;
            int totalDirectories = 0;
            List<String> errors = new ArrayList<>();
            
            // Backup each configured path (only modified files)
            for (String pathStr : backupPaths) {
                Path sourcePath = Paths.get(pathStr);
                if (!Files.exists(sourcePath)) {
                    log.warn("Backup path does not exist: {}", pathStr);
                    continue;
                }
                
                log.info("Backing up modified files from path: {}", pathStr);
                
                // Create directory structure in backup
                Path targetDir = fsBackupDir.resolve(sourcePath.getFileName());
                
                BackupStats stats = copyDirectoryRecursively(sourcePath, targetDir, lastBackupTime, errors);
                totalSize += stats.getTotalSize();
                totalFiles += stats.getFileCount();
                totalDirectories += stats.getDirectoryCount();
            }
            
            // Create backup manifest
            createBackupManifest(fsBackupDir, metadata, totalSize, totalFiles, totalDirectories);
            
            log.info("Incremental file system backup completed. Files: {}, Directories: {}, Size: {} bytes", 
                totalFiles, totalDirectories, totalSize);
            
            return FileSystemBackupResult.builder()
                .success(true)
                .backupDirectory(fsBackupDir)
                .backupSize(totalSize)
                .fileCount(totalFiles)
                .directoryCount(totalDirectories)
                .errors(errors)
                .message("Incremental file system backup completed successfully")
                .build();
            
        } catch (Exception e) {
            log.error("Incremental file system backup failed for backup {}", metadata.getBackupId(), e);
            return FileSystemBackupResult.builder()
                .success(false)
                .message("Incremental file system backup failed: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * Restore file system from backup
     */
    public void restoreFromBackup(Path backupPath, RestoreOptions options) {
        log.info("Starting file system restore from backup at {}", backupPath);
        
        try {
            Path fsBackupDir = backupPath.resolve("filesystem");
            if (!Files.exists(fsBackupDir)) {
                throw new BackupException("File system backup directory not found: " + fsBackupDir);
            }
            
            // Read backup manifest
            FileSystemBackupManifest manifest = readBackupManifest(fsBackupDir);
            
            // Restore each backed up path
            Files.walkFileTree(fsBackupDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (dir.equals(fsBackupDir)) {
                        return FileVisitResult.CONTINUE;
                    }
                    
                    // Determine target path
                    Path relativePath = fsBackupDir.relativize(dir);
                    Path targetPath = determineRestoreTarget(relativePath, options);
                    
                    if (targetPath != null) {
                        // Create directory if it doesn't exist
                        if (!Files.exists(targetPath)) {
                            Files.createDirectories(targetPath);
                            log.debug("Created directory: {}", targetPath);
                        }
                        
                        // Restore permissions if enabled
                        if (preservePermissions) {
                            // Implementation would restore file permissions
                            log.debug("Restoring permissions for directory: {}", targetPath);
                        }
                    }
                    
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // Skip manifest file
                    if (file.getFileName().toString().equals("backup_manifest.json")) {
                        return FileVisitResult.CONTINUE;
                    }
                    
                    // Determine target path
                    Path relativePath = fsBackupDir.relativize(file);
                    Path targetPath = determineRestoreTarget(relativePath, options);
                    
                    if (targetPath != null) {
                        // Create parent directories if they don't exist
                        Files.createDirectories(targetPath.getParent());
                        
                        // Copy file
                        if (options.isOverwriteExisting() || !Files.exists(targetPath)) {
                            Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
                            log.debug("Restored file: {}", targetPath);
                        } else {
                            log.debug("Skipped existing file: {}", targetPath);
                        }
                        
                        // Restore permissions if enabled
                        if (preservePermissions) {
                            // Implementation would restore file permissions
                            log.debug("Restoring permissions for file: {}", targetPath);
                        }
                    }
                    
                    return FileVisitResult.CONTINUE;
                }
            });
            
            log.info("File system restore completed successfully");
            
        } catch (Exception e) {
            log.error("File system restore failed", e);
            throw new BackupException("File system restore failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Verify file system backup integrity
     */
    public boolean verifyBackupIntegrity(Path backupPath) {
        log.info("Verifying file system backup integrity at {}", backupPath);
        
        try {
            Path fsBackupDir = backupPath.resolve("filesystem");
            if (!Files.exists(fsBackupDir)) {
                log.error("File system backup directory not found");
                return false;
            }
            
            // Read backup manifest
            FileSystemBackupManifest manifest = readBackupManifest(fsBackupDir);
            
            // Verify file count and total size
            BackupStats actualStats = calculateBackupStats(fsBackupDir);
            
            if (actualStats.getFileCount() != manifest.getFileCount()) {
                log.error("File count mismatch. Expected: {}, Actual: {}", 
                    manifest.getFileCount(), actualStats.getFileCount());
                return false;
            }
            
            if (actualStats.getTotalSize() != manifest.getTotalSize()) {
                log.error("Total size mismatch. Expected: {}, Actual: {}", 
                    manifest.getTotalSize(), actualStats.getTotalSize());
                return false;
            }
            
            log.info("File system backup integrity verification passed");
            return true;
            
        } catch (Exception e) {
            log.error("File system backup verification failed", e);
            return false;
        }
    }
    
    private BackupStats copyDirectoryRecursively(Path source, Path target, 
                                                LocalDateTime sinceTime, List<String> errors) throws IOException {
        BackupStats stats = new BackupStats();
        
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                // Skip hidden directories if not included
                if (!includeHidden && dir.getFileName().toString().startsWith(".")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                
                // Skip excluded patterns
                if (shouldExclude(dir)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                
                // Create corresponding directory in target
                Path targetDir = target.resolve(source.relativize(dir));
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                    stats.incrementDirectoryCount();
                }
                
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                try {
                    // Skip hidden files if not included
                    if (!includeHidden && file.getFileName().toString().startsWith(".")) {
                        return FileVisitResult.CONTINUE;
                    }
                    
                    // Skip excluded patterns
                    if (shouldExclude(file)) {
                        return FileVisitResult.CONTINUE;
                    }
                    
                    // Skip files larger than max size
                    if (attrs.size() > MAX_FILE_SIZE_BYTES) {
                        log.warn("Skipping large file: {} (size: {} bytes)", file, attrs.size());
                        return FileVisitResult.CONTINUE;
                    }
                    
                    // For incremental backup, skip files not modified since last backup
                    if (sinceTime != null) {
                        LocalDateTime fileModTime = LocalDateTime.ofInstant(
                            attrs.lastModifiedTime().toInstant(), 
                            ZoneId.systemDefault()
                        );
                        
                        if (fileModTime.isBefore(sinceTime)) {
                            return FileVisitResult.CONTINUE;
                        }
                    }
                    
                    // Copy file to target
                    Path targetFile = target.resolve(source.relativize(file));
                    Files.createDirectories(targetFile.getParent());
                    
                    if (followSymlinks && Files.isSymbolicLink(file)) {
                        // Copy symlink target
                        Path linkTarget = Files.readSymbolicLink(file);
                        Files.copy(linkTarget, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        // Copy regular file
                        Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    }
                    
                    stats.incrementFileCount();
                    stats.addSize(attrs.size());
                    
                } catch (IOException e) {
                    String error = "Failed to copy file " + file + ": " + e.getMessage();
                    log.error(error, e);
                    errors.add(error);
                }
                
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                String error = "Failed to visit file " + file + ": " + exc.getMessage();
                log.error(error, exc);
                errors.add(error);
                return FileVisitResult.CONTINUE;
            }
        });
        
        return stats;
    }
    
    private boolean shouldExclude(Path path) {
        String fileName = path.getFileName().toString();
        
        for (String pattern : excludePatterns) {
            if (fileName.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    private Path determineRestoreTarget(Path relativePath, RestoreOptions options) {
        // This is a simplified implementation
        // In a real implementation, you would have mapping configuration
        // for where to restore different types of files
        
        String firstElement = relativePath.getName(0).toString();
        
        // Map backup paths to restore paths
        for (String backupPath : backupPaths) {
            Path backupPathObj = Paths.get(backupPath);
            if (backupPathObj.getFileName().toString().equals(firstElement)) {
                return backupPathObj.resolve(relativePath.subpath(1, relativePath.getNameCount()));
            }
        }
        
        return null;
    }
    
    private void createBackupManifest(Path backupDir, BackupMetadata metadata, 
                                     long totalSize, int fileCount, int directoryCount) throws IOException {
        Path manifestFile = backupDir.resolve("backup_manifest.json");
        
        FileSystemBackupManifest manifest = FileSystemBackupManifest.builder()
            .backupId(metadata.getBackupId())
            .backupType(metadata.getBackupType())
            .timestamp(metadata.getStartTime())
            .totalSize(totalSize)
            .fileCount(fileCount)
            .directoryCount(directoryCount)
            .backupPaths(backupPaths)
            .excludePatterns(excludePatterns)
            .includeHidden(includeHidden)
            .preservePermissions(preservePermissions)
            .followSymlinks(followSymlinks)
            .build();
        
        // Write manifest (simplified JSON)
        String json = String.format(
            "{\n" +
            "  \"backupId\": \"%s\",\n" +
            "  \"backupType\": \"%s\",\n" +
            "  \"timestamp\": \"%s\",\n" +
            "  \"totalSize\": %d,\n" +
            "  \"fileCount\": %d,\n" +
            "  \"directoryCount\": %d,\n" +
            "  \"backupPaths\": %s,\n" +
            "  \"excludePatterns\": %s,\n" +
            "  \"includeHidden\": %s,\n" +
            "  \"preservePermissions\": %s,\n" +
            "  \"followSymlinks\": %s\n" +
            "}",
            manifest.getBackupId(),
            manifest.getBackupType(),
            manifest.getTimestamp(),
            manifest.getTotalSize(),
            manifest.getFileCount(),
            manifest.getDirectoryCount(),
            listToJsonArray(manifest.getBackupPaths()),
            listToJsonArray(manifest.getExcludePatterns()),
            manifest.isIncludeHidden(),
            manifest.isPreservePermissions(),
            manifest.isFollowSymlinks()
        );
        
        Files.write(manifestFile, json.getBytes());
    }
    
    private FileSystemBackupManifest readBackupManifest(Path backupDir) throws IOException {
        Path manifestFile = backupDir.resolve("backup_manifest.json");
        if (!Files.exists(manifestFile)) {
            throw new BackupException("Backup manifest not found: " + manifestFile);
        }
        
        // In a real implementation, use JSON parsing library
        // This is a simplified version
        String content = Files.readString(manifestFile);
        
        return FileSystemBackupManifest.builder()
            .backupId(extractJsonValue(content, "backupId"))
            .backupType(BackupType.valueOf(extractJsonValue(content, "backupType")))
            .timestamp(LocalDateTime.parse(extractJsonValue(content, "timestamp")))
            .totalSize(Long.parseLong(extractJsonValue(content, "totalSize")))
            .fileCount(Integer.parseInt(extractJsonValue(content, "fileCount")))
            .directoryCount(Integer.parseInt(extractJsonValue(content, "directoryCount")))
            .build();
    }
    
    private BackupStats calculateBackupStats(Path backupDir) throws IOException {
        BackupStats stats = new BackupStats();
        
        Files.walkFileTree(backupDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!dir.equals(backupDir)) {
                    stats.incrementDirectoryCount();
                }
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!file.getFileName().toString().equals("backup_manifest.json")) {
                    stats.incrementFileCount();
                    stats.addSize(attrs.size());
                }
                return FileVisitResult.CONTINUE;
            }
        });
        
        return stats;
    }
    
    private String listToJsonArray(List<String> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(list.get(i)).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }
    
    private String extractJsonValue(String json, String key) {
        // Simplified JSON value extraction
        String pattern = "\"" + key + "\": \"";
        int start = json.indexOf(pattern);
        if (start == -1) {
            pattern = "\"" + key + "\": ";
            start = json.indexOf(pattern);
            if (start == -1) return "";
            start += pattern.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            return json.substring(start, end).trim();
        }
        start += pattern.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
    
    /**
     * Helper class for backup statistics
     */
    private static class BackupStats {
        private int fileCount = 0;
        private int directoryCount = 0;
        private long totalSize = 0;
        
        public void incrementFileCount() { fileCount++; }
        public void incrementDirectoryCount() { directoryCount++; }
        public void addSize(long size) { totalSize += size; }
        
        public int getFileCount() { return fileCount; }
        public int getDirectoryCount() { return directoryCount; }
        public long getTotalSize() { return totalSize; }
    }
}