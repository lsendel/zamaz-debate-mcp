-- Constants and Common Patterns
-- VARCHAR_DEFAULT: VARCHAR(255)
-- TIMESTAMP_DEFAULT: TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
-- UUID_DEFAULT: UUID PRIMARY KEY DEFAULT gen_random_uuid()
-- AUDIT_COLUMNS: created_at, updated_at, created_by, updated_by

-- Create debates table
CREATE TABLE debates (
    id UUID PRIMARY KEY,
    topic VARCHAR2(1000) NOT NULL,
    status VARCHAR2(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    result TEXT,
    
    -- Configuration fields
    min_participants INTEGER NOT NULL,
    max_participants INTEGER NOT NULL,
    max_rounds INTEGER NOT NULL,
    round_time_limit_minutes INTEGER,
    max_debate_duration_hours INTEGER,
    require_balanced_positions BOOLEAN NOT NULL,
    auto_advance_rounds BOOLEAN NOT NULL,
    allow_spectators BOOLEAN NOT NULL,
    max_response_length INTEGER NOT NULL,
    enable_quality_assessment BOOLEAN NOT NULL,
    
    version BIGINT NOT NULL DEFAULT 0
);

-- Create participants table
CREATE TABLE participants (
    id UUID PRIMARY KEY,
    debate_id UUID NOT NULL REFERENCES debates(id) ON DELETE CASCADE,
    name VARCHAR2(255) NOT NULL,
    type VARCHAR2(50) NOT NULL,
    position VARCHAR2(1000) NOT NULL,
    provider VARCHAR2(50),
    joined_at TIMESTAMP NOT NULL,
    active BOOLEAN NOT NULL,
    response_count INTEGER NOT NULL DEFAULT 0,
    
    -- Provider configuration fields
    provider_model VARCHAR2(255),
    provider_max_tokens INTEGER,
    provider_temperature DECIMAL(3,2),
    provider_top_p DECIMAL(3,2),
    provider_system_prompt TEXT,
    
    -- Quality metrics
    avg_logical_strength DECIMAL(4,2),
    avg_evidence_quality DECIMAL(4,2),
    avg_clarity DECIMAL(4,2),
    avg_relevance DECIMAL(4,2),
    avg_originality DECIMAL(4,2),
    
    version BIGINT NOT NULL DEFAULT 0
);

-- Create rounds table
CREATE TABLE rounds (
    id UUID PRIMARY KEY,
    debate_id UUID NOT NULL REFERENCES debates(id) ON DELETE CASCADE,
    round_number INTEGER NOT NULL,
    status VARCHAR2(50) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    time_limit_minutes INTEGER,
    
    version BIGINT NOT NULL DEFAULT 0
);

-- Create responses table
CREATE TABLE responses (
    id UUID PRIMARY KEY,
    round_id UUID NOT NULL REFERENCES rounds(id) ON DELETE CASCADE,
    participant_id UUID NOT NULL,
    position VARCHAR2(1000) NOT NULL,
    content TEXT NOT NULL,
    submitted_at TIMESTAMP NOT NULL,
    response_time_seconds BIGINT,
    flagged BOOLEAN NOT NULL DEFAULT FALSE,
    flag_reason VARCHAR2(1000),
    
    -- Quality metrics
    quality_logical_strength DECIMAL(4,2),
    quality_evidence_quality DECIMAL(4,2),
    quality_clarity DECIMAL(4,2),
    quality_relevance DECIMAL(4,2),
    quality_originality DECIMAL(4,2),
    
    version BIGINT NOT NULL DEFAULT 0
);

-- Create indexes for better query performance
CREATE INDEX idx_debates_status ON debates(status);
CREATE INDEX idx_debates_created_at ON debates(created_at);
CREATE INDEX idx_debates_topic ON debates USING gin(to_tsvector('english', topic));

CREATE INDEX idx_participants_debate_id ON participants(debate_id);
CREATE INDEX idx_participants_type ON participants(type);
CREATE INDEX idx_participants_active ON participants(active);

CREATE INDEX idx_rounds_debate_id ON rounds(debate_id);
CREATE INDEX idx_rounds_status ON rounds(status);
CREATE INDEX idx_rounds_round_number ON rounds(round_number);

CREATE INDEX idx_responses_round_id ON responses(round_id);
CREATE INDEX idx_responses_participant_id ON responses(participant_id);
CREATE INDEX idx_responses_submitted_at ON responses(submitted_at);
CREATE INDEX idx_responses_flagged ON responses(flagged);