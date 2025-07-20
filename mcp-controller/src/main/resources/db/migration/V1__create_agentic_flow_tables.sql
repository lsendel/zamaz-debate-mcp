-- Create agentic flows table
CREATE TABLE IF NOT EXISTS agentic_flows (
    id UUID PRIMARY KEY,
    flow_type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    configuration JSONB NOT NULL,
    status VARCHAR(20) NOT NULL,
    organization_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_agentic_flows_org_name UNIQUE (organization_id, name)
);

-- Create indexes for agentic_flows
CREATE INDEX idx_agentic_flows_organization ON agentic_flows(organization_id);
CREATE INDEX idx_agentic_flows_type ON agentic_flows(flow_type);
CREATE INDEX idx_agentic_flows_status ON agentic_flows(status);
CREATE INDEX idx_agentic_flows_org_type ON agentic_flows(organization_id, flow_type);
CREATE INDEX idx_agentic_flows_org_name ON agentic_flows(organization_id, name);

-- Create debate agentic flow configuration table
CREATE TABLE IF NOT EXISTS debate_agentic_flows (
    id UUID PRIMARY KEY,
    debate_id UUID NOT NULL,
    flow_id UUID NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    configuration JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    FOREIGN KEY (debate_id) REFERENCES debates(id) ON DELETE CASCADE,
    FOREIGN KEY (flow_id) REFERENCES agentic_flows(id) ON DELETE CASCADE
);

-- Create indexes for debate_agentic_flows
CREATE INDEX idx_debate_agentic_flows_debate ON debate_agentic_flows(debate_id);
CREATE INDEX idx_debate_agentic_flows_flow ON debate_agentic_flows(flow_id);

-- Create participant agentic flow configuration table
CREATE TABLE IF NOT EXISTS participant_agentic_flows (
    id UUID PRIMARY KEY,
    participant_id UUID NOT NULL,
    flow_id UUID NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    configuration JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    FOREIGN KEY (participant_id) REFERENCES participants(id) ON DELETE CASCADE,
    FOREIGN KEY (flow_id) REFERENCES agentic_flows(id) ON DELETE CASCADE
);

-- Create indexes for participant_agentic_flows
CREATE INDEX idx_participant_agentic_flows_participant ON participant_agentic_flows(participant_id);
CREATE INDEX idx_participant_agentic_flows_flow ON participant_agentic_flows(flow_id);

-- Create agentic flow executions table (for analytics)
CREATE TABLE IF NOT EXISTS agentic_flow_executions (
    id UUID PRIMARY KEY,
    flow_id UUID NOT NULL,
    debate_id UUID,
    participant_id UUID,
    prompt TEXT NOT NULL,
    result JSONB NOT NULL,
    processing_time_ms BIGINT NOT NULL,
    response_changed BOOLEAN NOT NULL DEFAULT FALSE,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    FOREIGN KEY (flow_id) REFERENCES agentic_flows(id) ON DELETE CASCADE
);

-- Create indexes for agentic_flow_executions
CREATE INDEX idx_agentic_flow_exec_flow ON agentic_flow_executions(flow_id);
CREATE INDEX idx_agentic_flow_exec_debate ON agentic_flow_executions(debate_id);
CREATE INDEX idx_agentic_flow_exec_participant ON agentic_flow_executions(participant_id);
CREATE INDEX idx_agentic_flow_exec_created ON agentic_flow_executions(created_at);
CREATE INDEX idx_agentic_flow_exec_processing_time ON agentic_flow_executions(processing_time_ms DESC);
CREATE INDEX idx_agentic_flow_exec_errors ON agentic_flow_executions(flow_id) WHERE error_message IS NOT NULL;