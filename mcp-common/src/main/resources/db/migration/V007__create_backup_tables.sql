-- Backup and Disaster Recovery Tables for MCP System

-- Backup metadata table
CREATE TABLE IF NOT EXISTS backup_metadata (
    id BIGSERIAL PRIMARY KEY,
    backup_id VARCHAR(100) NOT NULL UNIQUE,
    backup_type VARCHAR(20) NOT NULL, -- FULL, INCREMENTAL, DIFFERENTIAL
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    duration_millis BIGINT DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED, DELETED
    database_size BIGINT DEFAULT 0,
    filesystem_size BIGINT DEFAULT 0,
    total_size BIGINT DEFAULT 0,
    compression_enabled BOOLEAN DEFAULT FALSE,
    encryption_enabled BOOLEAN DEFAULT FALSE,
    verification_passed BOOLEAN DEFAULT FALSE,
    cloud_upload_completed BOOLEAN DEFAULT FALSE,
    based_on_backup_id VARCHAR(100), -- For incremental backups
    error_message TEXT,
    warnings TEXT[],
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key reference for incremental backups
    CONSTRAINT fk_backup_based_on FOREIGN KEY (based_on_backup_id) REFERENCES backup_metadata(backup_id)
);

-- Restore metadata table
CREATE TABLE IF NOT EXISTS restore_metadata (
    id BIGSERIAL PRIMARY KEY,
    restore_id VARCHAR(100) NOT NULL UNIQUE,
    backup_id VARCHAR(100) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    duration_millis BIGINT DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
    restore_options JSONB,
    error_message TEXT,
    warnings TEXT[],
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key reference to backup
    CONSTRAINT fk_restore_backup FOREIGN KEY (backup_id) REFERENCES backup_metadata(backup_id)
);

-- Backup files table (for tracking individual backup files)
CREATE TABLE IF NOT EXISTS backup_files (
    id BIGSERIAL PRIMARY KEY,
    backup_id VARCHAR(100) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_type VARCHAR(50) NOT NULL, -- database, filesystem, manifest, etc.
    file_size BIGINT NOT NULL,
    checksum VARCHAR(128),
    compression_ratio DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key reference to backup
    CONSTRAINT fk_backup_file FOREIGN KEY (backup_id) REFERENCES backup_metadata(backup_id) ON DELETE CASCADE
);

-- Backup schedule table
CREATE TABLE IF NOT EXISTS backup_schedule (
    id BIGSERIAL PRIMARY KEY,
    schedule_name VARCHAR(100) NOT NULL UNIQUE,
    backup_type VARCHAR(20) NOT NULL,
    cron_expression VARCHAR(100) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    retention_days INTEGER DEFAULT 30,
    compression_enabled BOOLEAN DEFAULT TRUE,
    encryption_enabled BOOLEAN DEFAULT FALSE,
    cloud_upload_enabled BOOLEAN DEFAULT FALSE,
    last_run TIMESTAMP,
    next_run TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Backup notifications table
CREATE TABLE IF NOT EXISTS backup_notifications (
    id BIGSERIAL PRIMARY KEY,
    backup_id VARCHAR(100),
    restore_id VARCHAR(100),
    notification_type VARCHAR(50) NOT NULL, -- success, failure, warning
    channel VARCHAR(50) NOT NULL, -- email, slack, webhook
    recipient VARCHAR(200) NOT NULL,
    subject VARCHAR(500),
    message TEXT,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, SENT, FAILED
    retry_count INTEGER DEFAULT 0,
    
    -- Foreign key references
    CONSTRAINT fk_notification_backup FOREIGN KEY (backup_id) REFERENCES backup_metadata(backup_id),
    CONSTRAINT fk_notification_restore FOREIGN KEY (restore_id) REFERENCES restore_metadata(restore_id)
);

-- Backup storage locations table
CREATE TABLE IF NOT EXISTS backup_storage_locations (
    id BIGSERIAL PRIMARY KEY,
    location_name VARCHAR(100) NOT NULL UNIQUE,
    storage_type VARCHAR(50) NOT NULL, -- local, aws_s3, gcp_storage, azure_blob
    configuration JSONB NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    priority INTEGER DEFAULT 1, -- 1 is highest priority
    capacity_bytes BIGINT,
    used_bytes BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Backup verification results table
CREATE TABLE IF NOT EXISTS backup_verification_results (
    id BIGSERIAL PRIMARY KEY,
    backup_id VARCHAR(100) NOT NULL,
    verification_type VARCHAR(50) NOT NULL, -- integrity, restore_test, checksum
    verification_time TIMESTAMP NOT NULL,
    result VARCHAR(20) NOT NULL, -- PASSED, FAILED, WARNING
    details JSONB,
    error_message TEXT,
    duration_millis BIGINT,
    
    -- Foreign key reference
    CONSTRAINT fk_verification_backup FOREIGN KEY (backup_id) REFERENCES backup_metadata(backup_id)
);

-- Indexes for performance optimization

-- Backup metadata indexes
CREATE INDEX IF NOT EXISTS idx_backup_metadata_backup_id ON backup_metadata (backup_id);
CREATE INDEX IF NOT EXISTS idx_backup_metadata_type ON backup_metadata (backup_type);
CREATE INDEX IF NOT EXISTS idx_backup_metadata_status ON backup_metadata (status);
CREATE INDEX IF NOT EXISTS idx_backup_metadata_start_time ON backup_metadata (start_time);
CREATE INDEX IF NOT EXISTS idx_backup_metadata_end_time ON backup_metadata (end_time);
CREATE INDEX IF NOT EXISTS idx_backup_metadata_based_on ON backup_metadata (based_on_backup_id);

-- Composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_backup_metadata_status_time ON backup_metadata (status, start_time DESC);
CREATE INDEX IF NOT EXISTS idx_backup_metadata_type_time ON backup_metadata (backup_type, start_time DESC);

-- Restore metadata indexes
CREATE INDEX IF NOT EXISTS idx_restore_metadata_restore_id ON restore_metadata (restore_id);
CREATE INDEX IF NOT EXISTS idx_restore_metadata_backup_id ON restore_metadata (backup_id);
CREATE INDEX IF NOT EXISTS idx_restore_metadata_status ON restore_metadata (status);
CREATE INDEX IF NOT EXISTS idx_restore_metadata_start_time ON restore_metadata (start_time);

-- Backup files indexes
CREATE INDEX IF NOT EXISTS idx_backup_files_backup_id ON backup_files (backup_id);
CREATE INDEX IF NOT EXISTS idx_backup_files_type ON backup_files (file_type);

-- Backup schedule indexes
CREATE INDEX IF NOT EXISTS idx_backup_schedule_enabled ON backup_schedule (enabled);
CREATE INDEX IF NOT EXISTS idx_backup_schedule_next_run ON backup_schedule (next_run);

-- Backup notifications indexes
CREATE INDEX IF NOT EXISTS idx_backup_notifications_backup_id ON backup_notifications (backup_id);
CREATE INDEX IF NOT EXISTS idx_backup_notifications_restore_id ON backup_notifications (restore_id);
CREATE INDEX IF NOT EXISTS idx_backup_notifications_status ON backup_notifications (status);
CREATE INDEX IF NOT EXISTS idx_backup_notifications_sent_at ON backup_notifications (sent_at);

-- Backup storage locations indexes
CREATE INDEX IF NOT EXISTS idx_backup_storage_enabled ON backup_storage_locations (enabled);
CREATE INDEX IF NOT EXISTS idx_backup_storage_priority ON backup_storage_locations (priority);

-- Backup verification results indexes
CREATE INDEX IF NOT EXISTS idx_backup_verification_backup_id ON backup_verification_results (backup_id);
CREATE INDEX IF NOT EXISTS idx_backup_verification_type ON backup_verification_results (verification_type);
CREATE INDEX IF NOT EXISTS idx_backup_verification_result ON backup_verification_results (result);
CREATE INDEX IF NOT EXISTS idx_backup_verification_time ON backup_verification_results (verification_time);

-- Functions for backup operations

-- Function to get backup statistics
CREATE OR REPLACE FUNCTION get_backup_statistics(
    p_from_date TIMESTAMP DEFAULT NULL,
    p_to_date TIMESTAMP DEFAULT NULL
) RETURNS TABLE (
    total_backups BIGINT,
    successful_backups BIGINT,
    failed_backups BIGINT,
    total_size BIGINT,
    avg_duration DECIMAL,
    success_rate DECIMAL
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*) as total_backups,
        COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as successful_backups,
        COUNT(CASE WHEN status = 'FAILED' THEN 1 END) as failed_backups,
        SUM(CASE WHEN status = 'COMPLETED' THEN total_size ELSE 0 END) as total_size,
        AVG(CASE WHEN status = 'COMPLETED' THEN duration_millis END) as avg_duration,
        ROUND(
            COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) * 100.0 / 
            NULLIF(COUNT(*), 0), 2
        ) as success_rate
    FROM backup_metadata
    WHERE (p_from_date IS NULL OR start_time >= p_from_date)
      AND (p_to_date IS NULL OR start_time <= p_to_date);
END;
$$ LANGUAGE plpgsql;

-- Function to cleanup old backups
CREATE OR REPLACE FUNCTION cleanup_old_backups(
    p_retention_days INTEGER DEFAULT 30
) RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM backup_metadata
    WHERE start_time < NOW() - INTERVAL '1 day' * p_retention_days
      AND status IN ('COMPLETED', 'FAILED', 'DELETED');
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Function to get backup size trends
CREATE OR REPLACE FUNCTION get_backup_size_trends(
    p_days INTEGER DEFAULT 30
) RETURNS TABLE (
    backup_date DATE,
    total_size BIGINT,
    avg_size BIGINT,
    backup_count BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        DATE(start_time) as backup_date,
        SUM(total_size) as total_size,
        AVG(total_size) as avg_size,
        COUNT(*) as backup_count
    FROM backup_metadata
    WHERE start_time >= NOW() - INTERVAL '1 day' * p_days
      AND status = 'COMPLETED'
    GROUP BY DATE(start_time)
    ORDER BY backup_date DESC;
END;
$$ LANGUAGE plpgsql;

-- Function to get next backup schedule
CREATE OR REPLACE FUNCTION get_next_backup_schedules(
    p_limit INTEGER DEFAULT 10
) RETURNS TABLE (
    schedule_name VARCHAR(100),
    backup_type VARCHAR(20),
    next_run TIMESTAMP,
    last_run TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        bs.schedule_name,
        bs.backup_type,
        bs.next_run,
        bs.last_run
    FROM backup_schedule bs
    WHERE bs.enabled = TRUE
      AND bs.next_run IS NOT NULL
    ORDER BY bs.next_run ASC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

-- Function to update backup storage usage
CREATE OR REPLACE FUNCTION update_storage_usage(
    p_location_name VARCHAR(100),
    p_size_change BIGINT
) RETURNS BOOLEAN AS $$
BEGIN
    UPDATE backup_storage_locations 
    SET used_bytes = used_bytes + p_size_change,
        updated_at = CURRENT_TIMESTAMP
    WHERE location_name = p_location_name;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply triggers
CREATE TRIGGER update_backup_metadata_updated_at
    BEFORE UPDATE ON backup_metadata
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_restore_metadata_updated_at
    BEFORE UPDATE ON restore_metadata
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_backup_schedule_updated_at
    BEFORE UPDATE ON backup_schedule
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_backup_storage_locations_updated_at
    BEFORE UPDATE ON backup_storage_locations
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Insert default backup schedules
INSERT INTO backup_schedule (schedule_name, backup_type, cron_expression, retention_days, compression_enabled, encryption_enabled, cloud_upload_enabled)
VALUES 
    ('Daily Full Backup', 'FULL', '0 0 2 * * *', 30, TRUE, FALSE, FALSE),
    ('Hourly Incremental Backup', 'INCREMENTAL', '0 0 */4 * * *', 7, TRUE, FALSE, FALSE)
ON CONFLICT (schedule_name) DO NOTHING;

-- Insert default storage location
INSERT INTO backup_storage_locations (location_name, storage_type, configuration, capacity_bytes)
VALUES 
    ('Local Storage', 'local', '{"path": "/var/backups/mcp", "max_size": "100GB"}', 107374182400) -- 100GB
ON CONFLICT (location_name) DO NOTHING;

-- Grant permissions (adjust based on your user setup)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON backup_metadata TO mcp_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON restore_metadata TO mcp_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON backup_files TO mcp_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON backup_schedule TO mcp_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON backup_notifications TO mcp_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON backup_storage_locations TO mcp_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON backup_verification_results TO mcp_user;
-- GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO mcp_user;

-- Comments for documentation
COMMENT ON TABLE backup_metadata IS 'Stores metadata for all backup operations';
COMMENT ON TABLE restore_metadata IS 'Stores metadata for all restore operations';
COMMENT ON TABLE backup_files IS 'Tracks individual files within each backup';
COMMENT ON TABLE backup_schedule IS 'Defines automated backup schedules';
COMMENT ON TABLE backup_notifications IS 'Tracks backup and restore notifications';
COMMENT ON TABLE backup_storage_locations IS 'Defines available backup storage locations';
COMMENT ON TABLE backup_verification_results IS 'Stores results of backup verification tests';

COMMENT ON COLUMN backup_metadata.backup_id IS 'Unique identifier for the backup';
COMMENT ON COLUMN backup_metadata.backup_type IS 'Type of backup (FULL, INCREMENTAL, DIFFERENTIAL)';
COMMENT ON COLUMN backup_metadata.based_on_backup_id IS 'Reference to the base backup for incremental backups';
COMMENT ON COLUMN backup_metadata.total_size IS 'Total size of the backup in bytes';
COMMENT ON COLUMN backup_metadata.verification_passed IS 'Whether backup integrity verification passed';
COMMENT ON COLUMN backup_metadata.cloud_upload_completed IS 'Whether backup was successfully uploaded to cloud storage';

COMMENT ON COLUMN restore_metadata.restore_id IS 'Unique identifier for the restore operation';
COMMENT ON COLUMN restore_metadata.restore_options IS 'JSON configuration for restore options';

COMMENT ON COLUMN backup_schedule.cron_expression IS 'Cron expression defining when backup should run';
COMMENT ON COLUMN backup_schedule.retention_days IS 'Number of days to retain backups';
COMMENT ON COLUMN backup_schedule.next_run IS 'Next scheduled execution time';

COMMENT ON COLUMN backup_storage_locations.storage_type IS 'Type of storage (local, aws_s3, gcp_storage, azure_blob)';
COMMENT ON COLUMN backup_storage_locations.configuration IS 'JSON configuration for storage location';
COMMENT ON COLUMN backup_storage_locations.priority IS 'Priority order for storage location selection (1 is highest)';

COMMENT ON COLUMN backup_verification_results.verification_type IS 'Type of verification performed (integrity, restore_test, checksum)';
COMMENT ON COLUMN backup_verification_results.result IS 'Result of verification (PASSED, FAILED, WARNING)';
COMMENT ON COLUMN backup_verification_results.details IS 'JSON details of verification results';