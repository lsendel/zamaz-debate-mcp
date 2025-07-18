-- Unified Debate Engine Database Schema
-- Consolidates schemas from controller, debate, and context services
-- Optimized for reduced inter-service communication and improved performance

-- ============================================================================
-- DEBATE MANAGEMENT TABLES
-- ============================================================================

-- Debates table - core debate entity
CREATE TABLE debates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    
    -- Basic debate information
    topic VARCHAR(500) NOT NULL,
    description TEXT,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE', -- PUBLIC, PRIVATE, ORGANIZATION
    
    -- Debate configuration
    max_participants INTEGER NOT NULL DEFAULT 2,
    max_rounds INTEGER NOT NULL DEFAULT 5,
    round_timeout_ms INTEGER NOT NULL DEFAULT 300000, -- 5 minutes
    settings JSONB,
    
    -- State and lifecycle
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, ACTIVE, COMPLETED, CANCELLED
    current_round INTEGER DEFAULT 0,
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    
    -- Context integration (embedded to reduce service calls)
    context_id UUID,
    total_tokens INTEGER DEFAULT 0,
    message_count INTEGER DEFAULT 0
);

-- Participants table - debate participants (human and AI)
CREATE TABLE participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    debate_id UUID NOT NULL REFERENCES debates(id) ON DELETE CASCADE,
    
    -- Participant identification
    user_id UUID, -- NULL for AI participants
    participant_type VARCHAR(20) NOT NULL, -- HUMAN, AI
    position VARCHAR(50) NOT NULL, -- PRO, CON, MODERATOR, JUDGE
    
    -- AI-specific fields
    model_provider VARCHAR(50), -- openai, anthropic, google, etc.
    model_name VARCHAR(100),
    model_config JSONB,
    
    -- Performance tracking
    total_responses INTEGER DEFAULT 0,
    avg_response_time BIGINT, -- milliseconds
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP WITH TIME ZONE
);

-- Rounds table - debate rounds
CREATE TABLE rounds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    debate_id UUID NOT NULL REFERENCES debates(id) ON DELETE CASCADE,
    
    round_number INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, ACTIVE, COMPLETED, TIMEOUT
    
    -- Round configuration
    timeout_ms INTEGER NOT NULL DEFAULT 300000,
    prompt_template TEXT,
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    
    UNIQUE(debate_id, round_number)
);

-- Responses table - participant responses in rounds
CREATE TABLE responses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    round_id UUID NOT NULL REFERENCES rounds(id) ON DELETE CASCADE,
    participant_id UUID NOT NULL REFERENCES participants(id) ON DELETE CASCADE,
    
    -- Response content
    content TEXT NOT NULL,
    response_order INTEGER NOT NULL, -- Order within the round
    
    -- Performance metrics
    response_time_ms BIGINT, -- Time taken to generate response
    token_count INTEGER,
    
    -- Quality metrics (computed by AI analysis)
    quality_score DECIMAL(3,2), -- 0.00 to 1.00
    sentiment_score DECIMAL(3,2), -- -1.00 to 1.00
    coherence_score DECIMAL(3,2), -- 0.00 to 1.00
    factuality_score DECIMAL(3,2), -- 0.00 to 1.00
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(round_id, participant_id)
);

-- ============================================================================
-- CONTEXT MANAGEMENT TABLES (Embedded for Performance)
-- ============================================================================

-- Contexts table - conversation contexts for debates
CREATE TABLE contexts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    debate_id UUID UNIQUE REFERENCES debates(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL,
    user_id UUID NOT NULL,
    
    -- Context metadata
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, ARCHIVED, DELETED
    
    -- Token and size management
    total_tokens INTEGER NOT NULL DEFAULT 0,
    max_tokens INTEGER NOT NULL DEFAULT 4096,
    message_count INTEGER NOT NULL DEFAULT 0,
    window_size INTEGER NOT NULL DEFAULT 4096,
    
    -- Context versioning
    version INTEGER NOT NULL DEFAULT 1,
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Messages table - conversation messages within contexts
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    context_id UUID NOT NULL REFERENCES contexts(id) ON DELETE CASCADE,
    
    -- Message content
    role VARCHAR(50) NOT NULL, -- system, user, assistant, moderator
    content TEXT NOT NULL,
    
    -- Message metadata
    sequence_number INTEGER NOT NULL,
    token_count INTEGER,
    is_hidden BOOLEAN NOT NULL DEFAULT false,
    
    -- Debate-specific fields
    round_id UUID REFERENCES rounds(id),
    participant_id UUID REFERENCES participants(id),
    
    -- Timestamps
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Context versions table - for context history and rollback
CREATE TABLE context_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    context_id UUID NOT NULL REFERENCES contexts(id) ON DELETE CASCADE,
    
    version INTEGER NOT NULL,
    snapshot JSONB NOT NULL, -- Complete context state
    change_summary TEXT,
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL,
    
    UNIQUE(context_id, version)
);

-- Shared contexts table - for context sharing between organizations
CREATE TABLE shared_contexts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    context_id UUID NOT NULL REFERENCES contexts(id) ON DELETE CASCADE,
    shared_with_org_id UUID NOT NULL,
    
    permission_level VARCHAR(50) NOT NULL DEFAULT 'READ', -- read, write, admin
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    
    UNIQUE(context_id, shared_with_org_id)
);

-- ============================================================================
-- ANALYSIS AND QUALITY TABLES
-- ============================================================================

-- Debate analysis table - AI-powered debate analysis results
CREATE TABLE debate_analysis (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    debate_id UUID NOT NULL REFERENCES debates(id) ON DELETE CASCADE,
    
    -- Analysis results
    overall_quality_score DECIMAL(3,2),
    winner_participant_id UUID REFERENCES participants(id),
    analysis_summary TEXT,
    detailed_metrics JSONB,
    
    -- Analysis metadata
    analysis_status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, COMPLETED, FAILED
    analysis_type VARCHAR(50) NOT NULL DEFAULT 'COMPREHENSIVE', -- QUICK, COMPREHENSIVE, DETAILED
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE
);

-- ============================================================================
-- INDEXES FOR PERFORMANCE OPTIMIZATION
-- ============================================================================

-- Primary query patterns
CREATE INDEX idx_debates_org_status ON debates(organization_id, status);
CREATE INDEX idx_debates_org_created ON debates(organization_id, created_at DESC);
CREATE INDEX idx_debates_status_started ON debates(status, started_at DESC);

-- Participant queries
CREATE INDEX idx_participants_debate_id ON participants(debate_id);
CREATE INDEX idx_participants_user_id ON participants(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_participants_type_position ON participants(participant_type, position);

-- Round and response queries
CREATE INDEX idx_rounds_debate_id ON rounds(debate_id);
CREATE INDEX idx_rounds_debate_number ON rounds(debate_id, round_number);
CREATE INDEX idx_responses_round_id ON responses(round_id);
CREATE INDEX idx_responses_participant_id ON responses(participant_id);
CREATE INDEX idx_responses_round_order ON responses(round_id, response_order);

-- Context queries
CREATE INDEX idx_contexts_org_user ON contexts(organization_id, user_id);
CREATE INDEX idx_contexts_org_status ON contexts(organization_id, status);
CREATE INDEX idx_contexts_debate_id ON contexts(debate_id);
CREATE INDEX idx_messages_context_id ON messages(context_id);
CREATE INDEX idx_messages_context_sequence ON messages(context_id, sequence_number);
CREATE INDEX idx_messages_context_timestamp ON messages(context_id, timestamp ASC);

-- Analysis queries
CREATE INDEX idx_debate_analysis_debate_id ON debate_analysis(debate_id);
CREATE INDEX idx_debate_analysis_status ON debate_analysis(analysis_status);

-- Time-based queries
CREATE INDEX idx_debates_created_at ON debates(created_at DESC);
CREATE INDEX idx_contexts_last_activity ON contexts(last_activity_at ASC);
CREATE INDEX idx_messages_created_at ON messages(created_at DESC);

-- JSONB queries
CREATE INDEX idx_debates_settings_gin ON debates USING gin(settings) WHERE settings IS NOT NULL;
CREATE INDEX idx_participants_model_config_gin ON participants USING gin(model_config) WHERE model_config IS NOT NULL;

-- ============================================================================
-- UPDATE STATISTICS
-- ============================================================================

ANALYZE debates;
ANALYZE participants;
ANALYZE rounds;
ANALYZE responses;
ANALYZE contexts;
ANALYZE messages;
ANALYZE context_versions;
ANALYZE shared_contexts;
ANALYZE debate_analysis;