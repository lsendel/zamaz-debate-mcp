-- Performance indexes for agentic flows
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_agentic_flows_org_status 
ON agentic_flows(organization_id, status);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_agentic_flows_debate_type 
ON agentic_flows(debate_id, flow_type);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_agentic_flows_created_at 
ON agentic_flows(created_at DESC);

-- Partial index for active flows
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_active_flows 
ON agentic_flows(organization_id, flow_type) 
WHERE status = 'ACTIVE';

-- Composite index for flow lookups
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_agentic_flows_lookup 
ON agentic_flows(organization_id, debate_id, status, flow_type);

-- JSONB indexes for configuration queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_agentic_flows_config_tools 
ON agentic_flows USING gin ((configuration -> 'allowedTools'));

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_agentic_flows_config_params 
ON agentic_flows USING gin (configuration);

-- Add comments
COMMENT ON INDEX idx_agentic_flows_org_status IS 'Index for organization flow queries';
COMMENT ON INDEX idx_agentic_flows_debate_type IS 'Index for debate-specific flow type queries';
COMMENT ON INDEX idx_active_flows IS 'Partial index for active flows only';
COMMENT ON INDEX idx_agentic_flows_config_tools IS 'GIN index for tool configuration queries';