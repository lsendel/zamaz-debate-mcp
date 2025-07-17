-- Performance optimization indexes for context service

-- Contexts table indexes
CREATE INDEX IF NOT EXISTS idx_contexts_organization_id ON contexts(organization_id);
CREATE INDEX IF NOT EXISTS idx_contexts_created_at ON contexts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_contexts_updated_at ON contexts(updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_contexts_debate_id ON contexts(debate_id);
CREATE INDEX IF NOT EXISTS idx_contexts_composite ON contexts(organization_id, debate_id, is_active);

-- Context entries indexes
CREATE INDEX IF NOT EXISTS idx_context_entries_context_id ON context_entries(context_id);
CREATE INDEX IF NOT EXISTS idx_context_entries_created_at ON context_entries(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_context_entries_participant_id ON context_entries(participant_id);
CREATE INDEX IF NOT EXISTS idx_context_entries_composite ON context_entries(context_id, created_at DESC);

-- Context permissions indexes
CREATE INDEX IF NOT EXISTS idx_context_permissions_context_id ON context_permissions(context_id);
CREATE INDEX IF NOT EXISTS idx_context_permissions_user_id ON context_permissions(user_id);
CREATE INDEX IF NOT EXISTS idx_context_permissions_composite ON context_permissions(context_id, user_id, permission);

-- Context shares indexes
CREATE INDEX IF NOT EXISTS idx_context_shares_context_id ON context_shares(context_id);
CREATE INDEX IF NOT EXISTS idx_context_shares_shared_with ON context_shares(shared_with_org_id, shared_with_user_id);
CREATE INDEX IF NOT EXISTS idx_context_shares_expires_at ON context_shares(expires_at) WHERE expires_at IS NOT NULL;

-- Full-text search index for content
CREATE INDEX IF NOT EXISTS idx_context_entries_content_gin ON context_entries USING gin(to_tsvector('english', content));

-- Partial indexes for common queries
CREATE INDEX IF NOT EXISTS idx_contexts_active ON contexts(id) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_context_shares_active ON context_shares(id) WHERE expires_at IS NULL OR expires_at > NOW();

-- Update table statistics
ANALYZE contexts;
ANALYZE context_entries;
ANALYZE context_permissions;
ANALYZE context_shares;