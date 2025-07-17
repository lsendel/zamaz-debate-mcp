-- Critical missing indexes for context module
-- Adds essential indexes for multi-tenant context management and message retrieval

-- ============================================================================
-- CRITICAL: Foreign key indexes (PostgreSQL doesn't auto-create these)
-- ============================================================================

-- Messages to context relationship
CREATE INDEX IF NOT EXISTS idx_messages_context_id 
ON messages(context_id);

-- Context versions to context relationship  
CREATE INDEX IF NOT EXISTS idx_context_versions_context_id 
ON context_versions(context_id);

-- Shared contexts foreign keys
CREATE INDEX IF NOT EXISTS idx_shared_contexts_context_id 
ON shared_contexts(context_id);

CREATE INDEX IF NOT EXISTS idx_shared_contexts_shared_with_org 
ON shared_contexts(shared_with_org_id);

-- ============================================================================
-- CRITICAL: Multi-tenant composite indexes
-- ============================================================================

-- Context access by organization, user, and status (primary query pattern)
CREATE INDEX IF NOT EXISTS idx_contexts_org_user_status 
ON contexts(organization_id, user_id, status);

-- Active contexts by organization (partial index for efficiency)
CREATE INDEX IF NOT EXISTS idx_contexts_org_status 
ON contexts(organization_id, status) 
WHERE status = 'ACTIVE';

-- ============================================================================
-- CRITICAL: Message retrieval optimization
-- ============================================================================

-- Messages in chronological order for context loading
CREATE INDEX IF NOT EXISTS idx_messages_context_timestamp 
ON messages(context_id, timestamp ASC);

-- Non-hidden messages for display (partial index)
CREATE INDEX IF NOT EXISTS idx_messages_hidden 
ON messages(context_id, is_hidden) 
WHERE is_hidden = false;

-- Messages by role and time for conversation flow
CREATE INDEX IF NOT EXISTS idx_messages_role_timestamp 
ON messages(context_id, role, timestamp DESC);

-- ============================================================================
-- HIGH: Version tracking and context history
-- ============================================================================

-- Context versions in reverse chronological order
CREATE INDEX IF NOT EXISTS idx_context_versions_composite 
ON context_versions(context_id, version DESC);

-- Context versions by creation time for audit trails
CREATE INDEX IF NOT EXISTS idx_context_versions_created 
ON context_versions(created_at DESC);

-- ============================================================================
-- HIGH: Token counting and management queries
-- ============================================================================

-- Token usage tracking by organization for billing/limits
CREATE INDEX IF NOT EXISTS idx_contexts_org_tokens 
ON contexts(organization_id, total_tokens) 
WHERE status = 'ACTIVE';

-- Context size for memory management
CREATE INDEX IF NOT EXISTS idx_contexts_message_count 
ON contexts(message_count DESC) 
WHERE status = 'ACTIVE';

-- ============================================================================
-- HIGH: Sharing and collaboration features
-- ============================================================================

-- Shared context permissions
CREATE INDEX IF NOT EXISTS idx_shared_contexts_permissions 
ON shared_contexts(shared_with_org_id, permission_level);

-- Context sharing by creation time
CREATE INDEX IF NOT EXISTS idx_shared_contexts_created 
ON shared_contexts(created_at DESC);

-- ============================================================================
-- MEDIUM: Time-based queries for analytics
-- ============================================================================

-- Context creation time for analytics
CREATE INDEX IF NOT EXISTS idx_contexts_created_at 
ON contexts(created_at DESC);

-- Context last activity for cleanup jobs
CREATE INDEX IF NOT EXISTS idx_contexts_last_activity 
ON contexts(last_activity_at ASC) 
WHERE status = 'ACTIVE';

-- Message creation time for analytics
CREATE INDEX IF NOT EXISTS idx_messages_created_at 
ON messages(created_at DESC);

-- ============================================================================
-- Update table statistics for query planner optimization
-- ============================================================================

ANALYZE contexts;
ANALYZE messages;
ANALYZE context_versions;
ANALYZE shared_contexts;