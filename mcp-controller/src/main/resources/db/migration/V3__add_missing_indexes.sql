-- Constants and Common Patterns
-- VARCHAR_DEFAULT: VARCHAR(255)
-- TIMESTAMP_DEFAULT: TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
-- UUID_DEFAULT: UUID PRIMARY KEY DEFAULT gen_random_uuid()
-- AUDIT_COLUMNS: created_at, updated_at, created_by, updated_by

-- Critical and performance indexes for controller module (debates)
-- Adds essential indexes for debate management, participants, and response tracking

-- ============================================================================
-- CRITICAL: Foreign key indexes (PostgreSQL doesn't auto-create these)
-- ============================================================================

-- Rounds to debate relationship
CREATE INDEX IF NOT EXISTS idx_rounds_debate_id 
ON rounds(debate_id);

-- Responses to round relationship
CREATE INDEX IF NOT EXISTS idx_responses_round_id 
ON responses(round_id);

-- Responses to participant relationship
CREATE INDEX IF NOT EXISTS idx_responses_participant_id 
ON responses(participant_id);

-- Participants to user relationship (nullable, so include NULL handling)
CREATE INDEX IF NOT EXISTS idx_participants_user_id 
ON participants(user_id) 
WHERE user_id IS NOT NULL;

-- Participants to debate relationship
CREATE INDEX IF NOT EXISTS idx_participants_debate_id 
ON participants(debate_id);

-- ============================================================================
-- CRITICAL: Multi-tenant debate queries
-- ============================================================================

-- Debates by organization and status (primary query pattern)
CREATE INDEX IF NOT EXISTS idx_debates_org_status 
ON debates(organization_id, status);

-- Debates by organization, status, and creation time for dashboards
CREATE INDEX IF NOT EXISTS idx_debates_org_status_created 
ON debates(organization_id, status, created_at DESC);

-- ============================================================================
-- HIGH: Debate workflow and state management
-- ============================================================================

-- Rounds by debate and status for workflow management
CREATE INDEX IF NOT EXISTS idx_rounds_debate_status 
ON rounds(debate_id, status);

-- Responses in order within rounds for display
CREATE INDEX IF NOT EXISTS idx_responses_round_order 
ON responses(round_id, response_order);

-- Round progression tracking
CREATE INDEX IF NOT EXISTS idx_rounds_number 
ON rounds(debate_id, round_number);

-- ============================================================================
-- HIGH: Participant management and AI model tracking
-- ============================================================================

-- Participants by debate and type for rendering
CREATE INDEX IF NOT EXISTS idx_participants_debate_type 
ON participants(debate_id, participant_type);

-- AI model participants for model usage analytics (partial index)
CREATE INDEX IF NOT EXISTS idx_participants_ai_model 
ON participants(model_provider, model_name) 
WHERE participant_type = 'AI';

-- Human participants for user engagement tracking
CREATE INDEX IF NOT EXISTS idx_participants_human_user 
ON participants(user_id, created_at DESC) 
WHERE participant_type = 'HUMAN' AND user_id IS NOT NULL;

-- ============================================================================
-- HIGH: Performance and analytics queries
-- ============================================================================

-- Completed debates by organization for analytics
CREATE INDEX IF NOT EXISTS idx_debates_completed_org 
ON debates(organization_id, completed_at DESC) 
WHERE status = 'COMPLETED';

-- Response times by participant for performance analysis
CREATE INDEX IF NOT EXISTS idx_responses_participant_created 
ON responses(participant_id, created_at DESC);

-- Debate duration analysis
CREATE INDEX IF NOT EXISTS idx_debates_duration 
ON debates(created_at, completed_at) 
WHERE completed_at IS NOT NULL;

-- ============================================================================
-- HIGH: Topic and content searches
-- ============================================================================

-- Debate topic searches (case-insensitive)
CREATE INDEX IF NOT EXISTS idx_debates_topic_lower 
ON debates(LOWER(topic));

-- Response content for full-text search (if needed for analytics)
CREATE INDEX IF NOT EXISTS idx_responses_content_gin 
ON responses USING gin(to_tsvector('english', content)) 
WHERE content IS NOT NULL;

-- ============================================================================
-- MEDIUM: Time-based queries for pagination and cleanup
-- ============================================================================

-- Debate creation time for pagination
CREATE INDEX IF NOT EXISTS idx_debates_created_at 
ON debates(created_at DESC);

-- Response creation time for chronological display
CREATE INDEX IF NOT EXISTS idx_responses_created_at 
ON responses(created_at DESC);

-- Participant join time for engagement metrics
CREATE INDEX IF NOT EXISTS idx_participants_created_at 
ON participants(created_at DESC);

-- Round completion time for workflow analytics
CREATE INDEX IF NOT EXISTS idx_rounds_completed_at 
ON rounds(completed_at DESC) 
WHERE completed_at IS NOT NULL;

-- ============================================================================
-- MEDIUM: Debate settings and configuration
-- ============================================================================

-- Settings JSONB queries for debate configuration
CREATE INDEX IF NOT EXISTS idx_debates_settings_gin 
ON debates USING gin(settings) 
WHERE settings IS NOT NULL;

-- Debate visibility for public/private filtering
CREATE INDEX IF NOT EXISTS idx_debates_visibility 
ON debates(visibility, organization_id) 
WHERE visibility = 'PUBLIC';

-- ============================================================================
-- MEDIUM: Advanced analytics and reporting
-- ============================================================================

-- Active debates for real-time monitoring
CREATE INDEX IF NOT EXISTS idx_debates_active 
ON debates(status, created_at DESC) 
WHERE status IN ('ACTIVE', 'WAITING_FOR_RESPONSES');

-- Response statistics for performance metrics
CREATE INDEX IF NOT EXISTS idx_responses_stats 
ON responses(round_id, response_time_ms) 
WHERE response_time_ms IS NOT NULL;

-- Participant performance tracking
CREATE INDEX IF NOT EXISTS idx_participants_debate_performance 
ON participants(debate_id, total_responses, avg_response_time);

-- ============================================================================
-- Update table statistics for query planner optimization
-- ============================================================================

ANALYZE debates;
ANALYZE participants;
ANALYZE rounds;
ANALYZE responses;