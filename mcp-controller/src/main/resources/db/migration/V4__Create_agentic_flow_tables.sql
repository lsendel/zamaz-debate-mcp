-- Constants and Common Patterns
-- VARCHAR_DEFAULT: VARCHAR(255)
-- TIMESTAMP_DEFAULT: TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
-- UUID_DEFAULT: UUID PRIMARY KEY DEFAULT gen_random_uuid()
-- AUDIT_COLUMNS: created_at, updated_at, created_by, updated_by

-- Create agentic flows table
CREATE TABLE IF NOT EXISTS agentic_flows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flow_type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    configuration JSONB NOT NULL DEFAULT '{}',
    organization_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT agentic_flows_flow_type_check CHECK (flow_type IN (
        'INTERNAL_MONOLOGUE',
        'SELF_CRITIQUE_LOOP',
        'MULTI_AGENT_RED_TEAM',
        'TOOL_CALLING_VERIFICATION',
        'RAG_WITH_RERANKING',
        'CONFIDENCE_SCORING',
        'CONSTITUTIONAL_PROMPTING',
        'ENSEMBLE_VOTING',
        'POST_PROCESSING_RULES',
        'TREE_OF_THOUGHTS',
        'STEP_BACK_PROMPTING',
        'PROMPT_CHAINING'
    )),
    
    CONSTRAINT agentic_flows_status_check CHECK (status IN (
        'CREATED',
        'ACTIVE',
        'INACTIVE',
        'ERROR',
        'ARCHIVED'
    ))
);

-- Create indexes for performance
CREATE INDEX idx_agentic_flow_org_id ON agentic_flows(organization_id);
CREATE INDEX idx_agentic_flow_type ON agentic_flows(flow_type);
CREATE INDEX idx_agentic_flow_status ON agentic_flows(status);
CREATE INDEX idx_agentic_flow_org_type ON agentic_flows(organization_id, flow_type);
CREATE INDEX idx_agentic_flow_org_status ON agentic_flows(organization_id, status);
CREATE INDEX idx_agentic_flow_created_at ON agentic_flows(created_at DESC);

-- Add GIN index for JSONB configuration field for efficient querying
CREATE INDEX idx_agentic_flow_configuration ON agentic_flows USING GIN (configuration);

-- Add trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_agentic_flows_updated_at 
    BEFORE UPDATE ON agentic_flows 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Add metadata column to responses table for storing agentic flow results
ALTER TABLE responses 
ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}';

CREATE INDEX IF NOT EXISTS idx_responses_metadata ON responses USING GIN (metadata);

-- Create agentic flow execution history table for analytics
CREATE TABLE IF NOT EXISTS agentic_flow_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flow_id UUID NOT NULL REFERENCES agentic_flows(id) ON DELETE CASCADE,
    debate_id UUID,
    participant_id UUID,
    prompt TEXT NOT NULL,
    result JSONB NOT NULL,
    processing_time_ms BIGINT NOT NULL,
    response_changed BOOLEAN NOT NULL DEFAULT false,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT agentic_flow_executions_processing_time_check CHECK (processing_time_ms >= 0)
);

-- Create indexes for execution history
CREATE INDEX idx_flow_executions_flow_id ON agentic_flow_executions(flow_id);
CREATE INDEX idx_flow_executions_debate_id ON agentic_flow_executions(debate_id);
CREATE INDEX idx_flow_executions_participant_id ON agentic_flow_executions(participant_id);
CREATE INDEX idx_flow_executions_created_at ON agentic_flow_executions(created_at DESC);
CREATE INDEX idx_flow_executions_response_changed ON agentic_flow_executions(response_changed);

-- Add comment to tables
COMMENT ON TABLE agentic_flows IS 'Stores agentic flow configurations for enhancing AI debate responses';
COMMENT ON TABLE agentic_flow_executions IS 'Stores execution history of agentic flows for analytics and monitoring';

-- Add column comments
COMMENT ON COLUMN agentic_flows.flow_type IS 'Type of agentic flow processor';
COMMENT ON COLUMN agentic_flows.configuration IS 'JSON configuration parameters for the flow';
COMMENT ON COLUMN agentic_flows.status IS 'Current status of the flow';
COMMENT ON COLUMN agentic_flow_executions.processing_time_ms IS 'Total processing time in milliseconds';
COMMENT ON COLUMN agentic_flow_executions.response_changed IS 'Whether the flow changed the original response';