-- Constants and Common Patterns
-- VARCHAR_DEFAULT: VARCHAR(255)
-- TIMESTAMP_DEFAULT: TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
-- UUID_DEFAULT: UUID PRIMARY KEY DEFAULT gen_random_uuid()
-- AUDIT_COLUMNS: created_at, updated_at, created_by, updated_by

-- Performance optimization indexes for controller service

-- Debates table indexes
CREATE INDEX IF NOT EXISTS idx_debates_organization_id ON debates(organization_id);
CREATE INDEX IF NOT EXISTS idx_debates_status ON debates(status);
CREATE INDEX IF NOT EXISTS idx_debates_created_at ON debates(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_debates_started_at ON debates(started_at DESC) WHERE started_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_debates_composite ON debates(organization_id, status, created_at DESC);

-- Participants table indexes
CREATE INDEX IF NOT EXISTS idx_participants_debate_id ON participants(debate_id);
CREATE INDEX IF NOT EXISTS idx_participants_user_id ON participants(user_id);
CREATE INDEX IF NOT EXISTS idx_participants_model_provider ON participants(model_provider, model_name) WHERE participant_type = 'AI';
CREATE INDEX IF NOT EXISTS idx_participants_composite ON participants(debate_id, participant_type);

-- Rounds table indexes
CREATE INDEX IF NOT EXISTS idx_rounds_debate_id ON rounds(debate_id);
CREATE INDEX IF NOT EXISTS idx_rounds_started_at ON rounds(started_at DESC);
CREATE INDEX IF NOT EXISTS idx_rounds_composite ON rounds(debate_id, round_number);

-- Responses table indexes
CREATE INDEX IF NOT EXISTS idx_responses_round_id ON responses(round_id);
CREATE INDEX IF NOT EXISTS idx_responses_participant_id ON responses(participant_id);
CREATE INDEX IF NOT EXISTS idx_responses_created_at ON responses(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_responses_composite ON responses(round_id, participant_id, created_at DESC);

-- Votes table indexes (if implemented)
CREATE INDEX IF NOT EXISTS idx_votes_response_id ON votes(response_id) IF EXISTS;
CREATE INDEX IF NOT EXISTS idx_votes_voter_id ON votes(voter_id) IF EXISTS;
CREATE INDEX IF NOT EXISTS idx_votes_composite ON votes(response_id, voter_id) IF EXISTS;

-- Full-text search indexes
CREATE INDEX IF NOT EXISTS idx_debates_title_gin ON debates USING gin(to_tsvector('english', title));
CREATE INDEX IF NOT EXISTS idx_debates_topic_gin ON debates USING gin(to_tsvector('english', topic));
CREATE INDEX IF NOT EXISTS idx_responses_content_gin ON responses USING gin(to_tsvector('english', content));

-- Partial indexes for common queries
CREATE INDEX IF NOT EXISTS idx_debates_active ON debates(id) WHERE status IN ('CREATED', 'IN_PROGRESS', 'PAUSED');
CREATE INDEX IF NOT EXISTS idx_debates_completed ON debates(id) WHERE status = 'COMPLETED';

-- JSON indexes for settings (if using JSONB)
CREATE INDEX IF NOT EXISTS idx_debates_settings_gin ON debates USING gin(settings) WHERE settings IS NOT NULL;

-- Update table statistics
ANALYZE debates;
ANALYZE participants;
ANALYZE rounds;
ANALYZE responses;