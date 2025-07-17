package com.zamaz.mcp.common.backup;

import com.zamaz.mcp.common.backup.BackupModels.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for backup metadata persistence
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class BackupRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * Save backup metadata
     */
    public void save(BackupMetadata metadata) {
        String sql = """
            INSERT INTO backup_metadata (
                backup_id, backup_type, start_time, end_time, duration_millis, status,
                database_size, filesystem_size, total_size, compression_enabled,
                encryption_enabled, verification_passed, cloud_upload_completed,
                based_on_backup_id, error_message
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (backup_id) DO UPDATE SET
                backup_type = EXCLUDED.backup_type,
                start_time = EXCLUDED.start_time,
                end_time = EXCLUDED.end_time,
                duration_millis = EXCLUDED.duration_millis,
                status = EXCLUDED.status,
                database_size = EXCLUDED.database_size,
                filesystem_size = EXCLUDED.filesystem_size,
                total_size = EXCLUDED.total_size,
                compression_enabled = EXCLUDED.compression_enabled,
                encryption_enabled = EXCLUDED.encryption_enabled,
                verification_passed = EXCLUDED.verification_passed,
                cloud_upload_completed = EXCLUDED.cloud_upload_completed,
                based_on_backup_id = EXCLUDED.based_on_backup_id,
                error_message = EXCLUDED.error_message,
                updated_at = CURRENT_TIMESTAMP
            """;
        
        jdbcTemplate.update(sql,
            metadata.getBackupId(),
            metadata.getBackupType().name(),
            metadata.getStartTime(),
            metadata.getEndTime(),
            metadata.getDurationMillis(),
            metadata.getStatus().name(),
            metadata.getDatabaseSize(),
            metadata.getFileSystemSize(),
            metadata.getTotalSize(),
            metadata.isCompressionEnabled(),
            metadata.isEncryptionEnabled(),
            metadata.isVerificationPassed(),
            metadata.isCloudUploadCompleted(),
            metadata.getBasedOnBackupId(),
            metadata.getErrorMessage()
        );
        
        log.debug("Saved backup metadata: {}", metadata.getBackupId());
    }
    
    /**
     * Save restore metadata
     */
    public void saveRestoreMetadata(RestoreMetadata metadata) {
        String sql = """
            INSERT INTO restore_metadata (
                restore_id, backup_id, start_time, end_time, duration_millis, status,
                restore_options, error_message
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (restore_id) DO UPDATE SET
                backup_id = EXCLUDED.backup_id,
                start_time = EXCLUDED.start_time,
                end_time = EXCLUDED.end_time,
                duration_millis = EXCLUDED.duration_millis,
                status = EXCLUDED.status,
                restore_options = EXCLUDED.restore_options,
                error_message = EXCLUDED.error_message,
                updated_at = CURRENT_TIMESTAMP
            """;
        
        jdbcTemplate.update(sql,
            metadata.getRestoreId(),
            metadata.getBackupId(),
            metadata.getStartTime(),
            metadata.getEndTime(),
            metadata.getDurationMillis(),
            metadata.getStatus().name(),
            metadata.getOptions() != null ? metadata.getOptions().toString() : null,
            metadata.getErrorMessage()
        );
        
        log.debug("Saved restore metadata: {}", metadata.getRestoreId());
    }
    
    /**
     * Find backup by ID
     */
    public BackupMetadata findByBackupId(String backupId) {
        String sql = """
            SELECT backup_id, backup_type, start_time, end_time, duration_millis, status,
                   database_size, filesystem_size, total_size, compression_enabled,
                   encryption_enabled, verification_passed, cloud_upload_completed,
                   based_on_backup_id, error_message
            FROM backup_metadata
            WHERE backup_id = ?
            """;
        
        List<BackupMetadata> results = jdbcTemplate.query(sql, new BackupMetadataRowMapper(), backupId);
        return results.isEmpty() ? null : results.get(0);
    }
    
    /**
     * Find last successful backup
     */
    public BackupMetadata findLastSuccessfulBackup() {
        String sql = """
            SELECT backup_id, backup_type, start_time, end_time, duration_millis, status,
                   database_size, filesystem_size, total_size, compression_enabled,
                   encryption_enabled, verification_passed, cloud_upload_completed,
                   based_on_backup_id, error_message
            FROM backup_metadata
            WHERE status = 'COMPLETED'
            ORDER BY end_time DESC
            LIMIT 1
            """;
        
        List<BackupMetadata> results = jdbcTemplate.query(sql, new BackupMetadataRowMapper());
        return results.isEmpty() ? null : results.get(0);
    }
    
    /**
     * Find backups older than specified date
     */
    public List<BackupMetadata> findBackupsOlderThan(LocalDateTime cutoffDate) {
        String sql = """
            SELECT backup_id, backup_type, start_time, end_time, duration_millis, status,
                   database_size, filesystem_size, total_size, compression_enabled,
                   encryption_enabled, verification_passed, cloud_upload_completed,
                   based_on_backup_id, error_message
            FROM backup_metadata
            WHERE start_time < ?
            AND status IN ('COMPLETED', 'FAILED')
            ORDER BY start_time ASC
            """;
        
        return jdbcTemplate.query(sql, new BackupMetadataRowMapper(), cutoffDate);
    }
    
    /**
     * Find all backups
     */
    public List<BackupMetadata> findAll() {
        String sql = """
            SELECT backup_id, backup_type, start_time, end_time, duration_millis, status,
                   database_size, filesystem_size, total_size, compression_enabled,
                   encryption_enabled, verification_passed, cloud_upload_completed,
                   based_on_backup_id, error_message
            FROM backup_metadata
            ORDER BY start_time DESC
            """;
        
        return jdbcTemplate.query(sql, new BackupMetadataRowMapper());
    }
    
    /**
     * Get backup status summary
     */
    public BackupStatusSummary getBackupStatus() {
        String sql = """
            SELECT 
                COUNT(*) as total_backups,
                COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as successful_backups,
                COUNT(CASE WHEN status = 'FAILED' THEN 1 END) as failed_backups,
                COUNT(CASE WHEN status = 'IN_PROGRESS' THEN 1 END) as in_progress_backups,
                MAX(CASE WHEN status = 'COMPLETED' THEN end_time END) as last_successful_backup,
                SUM(CASE WHEN status = 'COMPLETED' THEN total_size ELSE 0 END) as total_backup_size
            FROM backup_metadata
            WHERE start_time > ?
            """;
        
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        
        return jdbcTemplate.queryForObject(sql, new BackupStatusSummaryRowMapper(), oneMonthAgo);
    }
    
    /**
     * Get backup statistics
     */
    public BackupStatistics getBackupStatistics(LocalDateTime from, LocalDateTime to) {
        String sql = """
            SELECT 
                COUNT(*) as total_backups,
                COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as successful_backups,
                COUNT(CASE WHEN status = 'FAILED' THEN 1 END) as failed_backups,
                AVG(CASE WHEN status = 'COMPLETED' THEN duration_millis END) as avg_duration,
                SUM(CASE WHEN status = 'COMPLETED' THEN total_size ELSE 0 END) as total_size,
                AVG(CASE WHEN status = 'COMPLETED' THEN total_size END) as avg_size
            FROM backup_metadata
            WHERE start_time BETWEEN ? AND ?
            """;
        
        return jdbcTemplate.queryForObject(sql, new BackupStatisticsRowMapper(), from, to);
    }
    
    /**
     * Delete old backup records
     */
    public int deleteOldBackupRecords(LocalDateTime cutoffDate) {
        String sql = """
            DELETE FROM backup_metadata
            WHERE start_time < ?
            AND status IN ('COMPLETED', 'FAILED', 'DELETED')
            """;
        
        return jdbcTemplate.update(sql, cutoffDate);
    }
    
    private static class BackupMetadataRowMapper implements RowMapper<BackupMetadata> {
        @Override
        public BackupMetadata mapRow(ResultSet rs, int rowNum) throws SQLException {
            return BackupMetadata.builder()
                .backupId(rs.getString("backup_id"))
                .backupType(BackupType.valueOf(rs.getString("backup_type")))
                .startTime(rs.getTimestamp("start_time") != null ? 
                    rs.getTimestamp("start_time").toLocalDateTime() : null)
                .endTime(rs.getTimestamp("end_time") != null ? 
                    rs.getTimestamp("end_time").toLocalDateTime() : null)
                .durationMillis(rs.getLong("duration_millis"))
                .status(BackupStatus.valueOf(rs.getString("status")))
                .databaseSize(rs.getLong("database_size"))
                .fileSystemSize(rs.getLong("filesystem_size"))
                .totalSize(rs.getLong("total_size"))
                .compressionEnabled(rs.getBoolean("compression_enabled"))
                .encryptionEnabled(rs.getBoolean("encryption_enabled"))
                .verificationPassed(rs.getBoolean("verification_passed"))
                .cloudUploadCompleted(rs.getBoolean("cloud_upload_completed"))
                .basedOnBackupId(rs.getString("based_on_backup_id"))
                .errorMessage(rs.getString("error_message"))
                .build();
        }
    }
    
    private static class BackupStatusSummaryRowMapper implements RowMapper<BackupStatusSummary> {
        @Override
        public BackupStatusSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
            return BackupStatusSummary.builder()
                .totalBackups(rs.getInt("total_backups"))
                .successfulBackups(rs.getInt("successful_backups"))
                .failedBackups(rs.getInt("failed_backups"))
                .inProgressBackups(rs.getInt("in_progress_backups"))
                .lastSuccessfulBackup(rs.getTimestamp("last_successful_backup") != null ? 
                    rs.getTimestamp("last_successful_backup").toLocalDateTime() : null)
                .totalBackupSize(rs.getLong("total_backup_size"))
                .build();
        }
    }
    
    private static class BackupStatisticsRowMapper implements RowMapper<BackupStatistics> {
        @Override
        public BackupStatistics mapRow(ResultSet rs, int rowNum) throws SQLException {
            return BackupStatistics.builder()
                .totalBackups(rs.getInt("total_backups"))
                .successfulBackups(rs.getInt("successful_backups"))
                .failedBackups(rs.getInt("failed_backups"))
                .averageDuration(rs.getDouble("avg_duration"))
                .totalSize(rs.getLong("total_size"))
                .averageSize(rs.getDouble("avg_size"))
                .build();
        }
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class BackupStatusSummary {
        private int totalBackups;
        private int successfulBackups;
        private int failedBackups;
        private int inProgressBackups;
        private LocalDateTime lastSuccessfulBackup;
        private long totalBackupSize;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class BackupStatistics {
        private int totalBackups;
        private int successfulBackups;
        private int failedBackups;
        private double averageDuration;
        private long totalSize;
        private double averageSize;
    }
}