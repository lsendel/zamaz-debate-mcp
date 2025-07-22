-- Constants and Common Patterns
-- VARCHAR_DEFAULT: VARCHAR(255)
-- TIMESTAMP_DEFAULT: TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
-- UUID_DEFAULT: UUID PRIMARY KEY DEFAULT gen_random_uuid()
-- AUDIT_COLUMNS: created_at, updated_at, created_by, updated_by

-- Create agentic flows table
CREATE TABLE agentic_flows (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type VARCHAR(50) NOT NULL,
    configuration JSONB NOT NULL DEFAULT '{}',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    organization_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for efficient querying
CREATE INDEX idx_agentic_flows_organization_id ON agentic_flows(organization_id);
CREATE INDEX idx_agentic_flows_type ON agentic_flows(type);
CREATE INDEX idx_agentic_flows_status ON agentic_flows(status);
CREATE INDEX idx_agentic_flows_org_type ON agentic_flows(organization_id, type);
CREATE INDEX idx_agentic_flows_org_status ON agentic_flows(organization_id, status);
CREATE INDEX idx_agentic_flows_created_at ON agentic_flows(created_at);
CREATE INDEX idx_agentic_flows_updated_at ON agentic_flows(updated_at);

-- Create GIN index for JSONB configuration for efficient JSON queries
CREATE INDEX idx_agentic_flows_configuration_gin ON agentic_flows USING GIN (configuration);

-- Create update trigger for updated_at
CREATE TRIGGER update_agentic_flows_updated_at BEFORE UPDATE ON agentic_flows
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Add constraints
ALTER TABLE agentic_flows ADD CONSTRAINT chk_agentic_flows_type 
    CHECK (type IN (
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
    ));

ALTER TABLE agentic_flows ADD CONSTRAINT chk_agentic_flows_status 
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'DRAFT'));

-- Add comments for documentation
COMMENT ON TABLE agentic_flows IS 'Stores agentic flow configurations for AI reasoning patterns';
COMMENT ON COLUMN agentic_flows.id IS 'Unique identifier for the agentic flow';
COMMENT ON COLUMN agentic_flows.type IS 'Type of agentic flow (reasoning pattern)';
COMMENT ON COLUMN agentic_flows.configuration IS 'JSON configuration parameters for the flow';
COMMENT ON COLUMN agentic_flows.status IS 'Current status of the flow (ACTIVE, INACTIVE, DRAFT)';
COMMENT ON COLUMN agentic_flows.organization_id IS 'ID of the organization that owns this flow';
COMMENT ON COLUMN agentic_flows.created_at IS 'Timestamp when the flow was created';
COMMENT ON COLUMN agentic_flows.updated_at IS 'Timestamp when the flow was last updated';