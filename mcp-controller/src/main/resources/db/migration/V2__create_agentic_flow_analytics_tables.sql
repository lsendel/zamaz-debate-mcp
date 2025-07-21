-- Create agentic flow executions table for analytics
CREATE TABLE IF NOT EXISTS agentic_flow_executions (
    id UUID PRIMARY KEY,
    flow_id UUID NOT NULL,
    debate_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    flow_type VARCHAR(50) NOT NULL,
    execution_time_ms BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    confidence DECIMAL(5,2),
    metadata JSONB,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    improvement_score DECIMAL(5,2),
    improvement_reason TEXT,
    CONSTRAINT fk_flow_id FOREIGN KEY (flow_id) REFERENCES agentic_flows(id) ON DELETE CASCADE
);

-- Create performance metrics table
CREATE TABLE IF NOT EXISTS agentic_flow_performance_metrics (
    id UUID PRIMARY KEY,
    execution_id UUID NOT NULL,
    llm_response_time_ms BIGINT,
    tool_call_time_ms BIGINT,
    total_tokens INTEGER,
    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    memory_usage_bytes BIGINT,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_execution_id FOREIGN KEY (execution_id) REFERENCES agentic_flow_executions(id) ON DELETE CASCADE
);

-- Create indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_executions_organization_flow_type_timestamp 
    ON agentic_flow_executions(organization_id, flow_type, timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_executions_debate_id 
    ON agentic_flow_executions(debate_id);

CREATE INDEX IF NOT EXISTS idx_executions_flow_id_timestamp 
    ON agentic_flow_executions(flow_id, timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_executions_timestamp 
    ON agentic_flow_executions(timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_executions_status 
    ON agentic_flow_executions(status);

CREATE INDEX IF NOT EXISTS idx_performance_metrics_execution_id 
    ON agentic_flow_performance_metrics(execution_id);

-- Create materialized view for flow type statistics (refresh periodically)
CREATE MATERIALIZED VIEW IF NOT EXISTS flow_type_statistics AS
SELECT 
    organization_id,
    flow_type,
    COUNT(*) as execution_count,
    AVG(confidence) as avg_confidence,
    SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END)::DECIMAL / COUNT(*) as success_rate,
    AVG(execution_time_ms) as avg_execution_time_ms,
    PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY execution_time_ms) as median_execution_time_ms,
    PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY execution_time_ms) as p95_execution_time_ms,
    MAX(timestamp) as last_execution_time
FROM agentic_flow_executions
GROUP BY organization_id, flow_type;

-- Create index on materialized view
CREATE INDEX IF NOT EXISTS idx_flow_type_stats_org_type 
    ON flow_type_statistics(organization_id, flow_type);

-- Function to refresh materialized view (can be called periodically)
CREATE OR REPLACE FUNCTION refresh_flow_type_statistics()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY flow_type_statistics;
END;
$$ LANGUAGE plpgsql;

-- Comment on tables
COMMENT ON TABLE agentic_flow_executions IS 'Stores execution history and analytics for agentic flows';
COMMENT ON TABLE agentic_flow_performance_metrics IS 'Detailed performance metrics for each agentic flow execution';
COMMENT ON MATERIALIZED VIEW flow_type_statistics IS 'Pre-computed statistics by flow type for fast analytics queries';