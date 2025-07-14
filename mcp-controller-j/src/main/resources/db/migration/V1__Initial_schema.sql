-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Debates table
CREATE TABLE debates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    topic TEXT NOT NULL,
    format VARCHAR(50) NOT NULL,
    max_rounds INTEGER DEFAULT 3,
    current_round INTEGER DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    settings JSONB DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP
);

-- Participants table
CREATE TABLE participants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    debate_id UUID REFERENCES debates(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL, -- 'human' or 'ai'
    provider VARCHAR(50), -- for AI participants
    model VARCHAR(100), -- for AI participants
    position VARCHAR(50), -- 'for' or 'against'
    settings JSONB DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Rounds table
CREATE TABLE rounds (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    debate_id UUID REFERENCES debates(id) ON DELETE CASCADE,
    round_number INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    UNIQUE(debate_id, round_number)
);

-- Responses table
CREATE TABLE responses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    round_id UUID REFERENCES rounds(id) ON DELETE CASCADE,
    participant_id UUID REFERENCES participants(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    token_count INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Debate results table
CREATE TABLE debate_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    debate_id UUID REFERENCES debates(id) ON DELETE CASCADE,
    winner_id UUID REFERENCES participants(id),
    summary TEXT,
    scores JSONB DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_debates_organization ON debates(organization_id);
CREATE INDEX idx_debates_status ON debates(status);
CREATE INDEX idx_debates_created ON debates(created_at DESC);
CREATE INDEX idx_participants_debate ON participants(debate_id);
CREATE INDEX idx_rounds_debate ON rounds(debate_id);
CREATE INDEX idx_responses_round ON responses(round_id);
CREATE INDEX idx_responses_participant ON responses(participant_id);

-- Create update trigger for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_debates_updated_at BEFORE UPDATE ON debates
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();