-- Constants and Common Patterns
-- VARCHAR_DEFAULT: VARCHAR(255)
-- TIMESTAMP_DEFAULT: TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
-- UUID_DEFAULT: UUID PRIMARY KEY DEFAULT gen_random_uuid()
-- AUDIT_COLUMNS: created_at, updated_at, created_by, updated_by

-- Create debate agentic flows configuration table
CREATE TABLE debate_agentic_flows (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    debate_id UUID NOT NULL,
    flow_id UUID NOT NULL REFERENCES agentic_flows(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    configuration JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(debate_id, flow_id)
);

-- Create participant agentic flows configuration table
CREATE TABLE participant_agentic_flows (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    participant_id UUID NOT NULL,
    flow_id UUID NOT NULL REFERENCES agentic_flows(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    configuration JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(participant_id, flow_id)
);

-- Create agentic flow analytics table
CREATE TABLE agentic_flow_analytics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    flow_id UUID NOT NULL REFERENCES agentic_flows(id) ON DELETE CASCADE,
    flow_type VARCHAR(50) NOT NULL,
    debate_id UUID NOT NULL,
    participant_id UUID NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processing_time_ms INTEGER NOT NULL,
    confidence_score INTEGER,
    flow_changed_response BOOLEAN NOT NULL DEFAULT FALSE,
    metrics JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for debate agentic flows
CREATE INDEX idx_debate_agentic_flows_debate_id ON debate_agentic_flows(debate_id);
CREATE INDEX idx_debate_agentic_flows_flow_id ON debate_agentic_flows(flow_id);
CREATE INDEX idx_debate_agentic_flows_enabled ON debate_agentic_flows(enabled);

-- Create indexes for participant agentic flows
CREATE INDEX idx_participant_agentic_flows_participant_id ON participant_agentic_flows(participant_id);
CREATE INDEX idx_participant_agentic_flows_flow_id ON participant_agentic_flows(flow_id);
CREATE INDEX idx_participant_agentic_flows_enabled ON participant_agentic_flows(enabled);

-- Create indexes for agentic flow analytics
CREATE INDEX idx_agentic_flow_analytics_flow_id ON agentic_flow_analytics(flow_id);
CREATE INDEX idx_agentic_flow_analytics_flow_type ON agentic_flow_analytics(flow_type);
CREATE INDEX idx_agentic_flow_analytics_debate_id ON agentic_flow_analytics(debate_id);
CREATE INDEX idx_agentic_flow_analytics_participant_id ON agentic_flow_analytics(participant_id);
CREATE INDEX idx_agentic_flow_analytics_timestamp ON agentic_flow_analytics(timestamp);
CREATE INDEX idx_agentic_flow_analytics_processing_time ON agentic_flow_analytics(processing_time_ms);
CREATE INDEX idx_agentic_flow_analytics_confidence_score ON agentic_flow_analytics(confidence_score);

-- Create GIN indexes for JSONB columns
CREATE INDEX idx_debate_agentic_flows_configuration_gin ON debate_agentic_flows USING GIN (configuration);
CREATE INDEX idx_participant_agentic_flows_configuration_gin ON participant_agentic_flows USING GIN (configuration);
CREATE INDEX idx_agentic_flow_analytics_metrics_gin ON agentic_flow_analytics USING GIN (metrics);

-- Create update triggers
CREATE TRIGGER update_debate_agentic_flows_updated_at BEFORE UPDATE ON debate_agentic_flows
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_participant_agentic_flows_updated_at BEFORE UPDATE ON participant_agentic_flows
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Add constraints
ALTER TABLE agentic_flow_analytics ADD CONSTRAINT chk_agentic_flow_analytics_processing_time 
    CHECK (processing_time_ms >= 0);

ALTER TABLE agentic_flow_analytics ADD CONSTRAINT chk_agentic_flow_analytics_confidence_score 
    CHECK (confidence_score IS NULL OR (confidence_score >= 0 AND confidence_score <= 100));

ALTER TABLE agentic_flow_analytics ADD CONSTRAINT chk_agentic_flow_analytics_flow_type 
    CHECK (flow_type IN (
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

-- Add comments for documentation
COMMENT ON TABLE debate_agentic_flows IS 'Configuration of agentic flows at the debate level';
COMMENT ON TABLE participant_agentic_flows IS 'Configuration of agentic flows at the participant level';
COMMENT ON TABLE agentic_flow_analytics IS 'Analytics data for agentic flow executions';

COMMENT ON COLUMN debate_agentic_flows.debate_id IS 'ID of the debate this configuration applies to';
COMMENT ON COLUMN debate_agentic_flows.flow_id IS 'ID of the agentic flow being configured';
COMMENT ON COLUMN debate_agentic_flows.enabled IS 'Whether this flow is enabled for the debate';
COMMENT ON COLUMN debate_agentic_flows.configuration IS 'Debate-specific configuration overrides';

COMMENT ON COLUMN participant_agentic_flows.participant_id IS 'ID of the participant this configuration applies to';
COMMENT ON COLUMN participant_agentic_flows.flow_id IS 'ID of the agentic flow being configured';
COMMENT ON COLUMN participant_agentic_flows.enabled IS 'Whether this flow is enabled for the participant';
COMMENT ON COLUMN participant_agentic_flows.configuration IS 'Participant-specific configuration overrides';

COMMENT ON COLUMN agentic_flow_analytics.flow_id IS 'ID of the agentic flow that was executed';
COMMENT ON COLUMN agentic_flow_analytics.flow_type IS 'Type of agentic flow for efficient querying';
COMMENT ON COLUMN agentic_flow_analytics.debate_id IS 'ID of the debate where the flow was executed';
COMMENT ON COLUMN agentic_flow_analytics.participant_id IS 'ID of the participant who used the flow';
COMMENT ON COLUMN agentic_flow_analytics.processing_time_ms IS 'Time taken to process the flow in milliseconds';
COMMENT ON COLUMN agentic_flow_analytics.confidence_score IS 'Confidence score of the response (0-100)';
COMMENT ON COLUMN agentic_flow_analytics.flow_changed_response IS 'Whether the flow changed the original response';
COMMENT ON COLUMN agentic_flow_analytics.metrics IS 'Additional metrics and metadata from flow execution';