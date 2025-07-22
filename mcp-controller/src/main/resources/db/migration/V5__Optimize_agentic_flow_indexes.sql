-- Constants and Common Patterns
-- VARCHAR_DEFAULT: VARCHAR(255)
-- TIMESTAMP_DEFAULT: TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
-- UUID_DEFAULT: UUID PRIMARY KEY DEFAULT gen_random_uuid()
-- AUDIT_COLUMNS: created_at, updated_at, created_by, updated_by

-- Additional indexes for agentic flow performance optimization
-- This migration adds specialized indexes for common query patterns

-- Add composite indexes for execution history queries
CREATE INDEX IF NOT EXISTS idx_flow_executions_flow_created ON agentic_flow_executions(flow_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_flow_executions_debate_created ON agentic_flow_executions(debate_id, created_at DESC) WHERE debate_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_flow_executions_participant_created ON agentic_flow_executions(participant_id, created_at DESC) WHERE participant_id IS NOT NULL;

-- Add index for error analysis
CREATE INDEX IF NOT EXISTS idx_flow_executions_errors ON agentic_flow_executions(flow_id, error_message) WHERE error_message IS NOT NULL;

-- Add index for performance analysis
CREATE INDEX IF NOT EXISTS idx_flow_executions_processing_time ON agentic_flow_executions(flow_id, processing_time_ms DESC);

-- Add index for response change analysis
CREATE INDEX IF NOT EXISTS idx_flow_executions_response_changed_flow ON agentic_flow_executions(flow_id, response_changed) WHERE response_changed = true;

-- Add partial index for active flows only
CREATE INDEX IF NOT EXISTS idx_agentic_flows_active ON agentic_flows(organization_id, flow_type, created_at DESC) WHERE status = 'ACTIVE';

-- Add index for configuration searches (using GIN for JSONB)
CREATE INDEX IF NOT EXISTS idx_agentic_flows_config_gin ON agentic_flows USING GIN (configuration jsonb_path_ops);

-- Add index for flow name searches
CREATE INDEX IF NOT EXISTS idx_agentic_flows_name_text ON agentic_flows USING GIN (to_tsvector('english', name));

-- Add statistics view for better query performance
CREATE OR REPLACE VIEW agentic_flow_statistics AS
SELECT 
    af.id as flow_id,
    af.flow_type,
    af.name,
    af.organization_id,
    af.status,
    COUNT(afe.id) as total_executions,
    COUNT(CASE WHEN afe.response_changed = true THEN 1 END) as response_changes,
    COUNT(CASE WHEN afe.error_message IS NOT NULL THEN 1 END) as error_count,
    AVG(afe.processing_time_ms) as avg_processing_time,
    MIN(afe.processing_time_ms) as min_processing_time,
    MAX(afe.processing_time_ms) as max_processing_time,
    PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY afe.processing_time_ms) as median_processing_time,
    PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY afe.processing_time_ms) as p95_processing_time,
    COUNT(CASE WHEN afe.response_changed = true THEN 1 END)::float / NULLIF(COUNT(afe.id), 0) as change_rate,
    COUNT(CASE WHEN afe.error_message IS NOT NULL THEN 1 END)::float / NULLIF(COUNT(afe.id), 0) as error_rate,
    MIN(afe.created_at) as first_execution,
    MAX(afe.created_at) as last_execution
FROM agentic_flows af
LEFT JOIN agentic_flow_executions afe ON af.id = afe.flow_id
GROUP BY af.id, af.flow_type, af.name, af.organization_id, af.status;

-- Add comment to the view
COMMENT ON VIEW agentic_flow_statistics IS 'Aggregated statistics for agentic flows including execution metrics and performance data';

-- Create function to clean up old executions
CREATE OR REPLACE FUNCTION cleanup_old_agentic_flow_executions(retention_days INTEGER DEFAULT 90)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
    cutoff_date TIMESTAMP WITH TIME ZONE;
BEGIN
    cutoff_date := CURRENT_TIMESTAMP - (retention_days || ' days')::INTERVAL;
    
    DELETE FROM agentic_flow_executions 
    WHERE created_at < cutoff_date;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    -- Log the cleanup operation
    INSERT INTO agentic_flow_executions (flow_id, prompt, result, processing_time_ms, response_changed, created_at)
    SELECT 
        (SELECT id FROM agentic_flows WHERE flow_type = 'INTERNAL_MONOLOGUE' LIMIT 1),
        'Cleanup operation',
        jsonb_build_object('operation', 'cleanup', 'deleted_count', deleted_count, 'retention_days', retention_days),
        0,
        false,
        CURRENT_TIMESTAMP
    WHERE EXISTS (SELECT 1 FROM agentic_flows WHERE flow_type = 'INTERNAL_MONOLOGUE' LIMIT 1);
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Add comment to the function
COMMENT ON FUNCTION cleanup_old_agentic_flow_executions(INTEGER) IS 'Cleans up old agentic flow execution records older than specified retention period';

-- Create index on updated_at for better performance on recent updates
CREATE INDEX IF NOT EXISTS idx_agentic_flows_updated_at ON agentic_flows(updated_at DESC);

-- Add constraint to ensure processing time is non-negative
ALTER TABLE agentic_flow_executions 
ADD CONSTRAINT IF NOT EXISTS chk_processing_time_non_negative 
CHECK (processing_time_ms >= 0);

-- Add constraint to ensure flow configuration is not empty
ALTER TABLE agentic_flows 
ADD CONSTRAINT IF NOT EXISTS chk_configuration_not_empty 
CHECK (jsonb_typeof(configuration) = 'object' AND configuration != '{}'::jsonb);

-- Update table statistics for better query planning
ANALYZE agentic_flows;
ANALYZE agentic_flow_executions;