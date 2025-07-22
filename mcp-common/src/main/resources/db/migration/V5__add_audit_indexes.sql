-- Constants and Common Patterns
-- VARCHAR_DEFAULT: VARCHAR(255)
-- TIMESTAMP_DEFAULT: TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
-- UUID_DEFAULT: UUID PRIMARY KEY DEFAULT gen_random_uuid()
-- AUDIT_COLUMNS: created_at, updated_at, created_by, updated_by

-- Critical indexes for audit events and common infrastructure
-- Adds essential indexes for security audit trails and system monitoring

-- ============================================================================
-- CRITICAL: Audit trail and security monitoring indexes
-- ============================================================================

-- Audit events by organization and timestamp for security monitoring
CREATE INDEX IF NOT EXISTS idx_audit_org_timestamp 
ON audit_events(organization_id, timestamp DESC);

-- Audit events by user and action for user activity tracking
CREATE INDEX IF NOT EXISTS idx_audit_user_action 
ON audit_events(user_id, action, timestamp DESC);

-- Audit events by action type for security analysis
CREATE INDEX IF NOT EXISTS idx_audit_action_timestamp 
ON audit_events(action, timestamp DESC);

-- ============================================================================
-- HIGH: Security and compliance queries
-- ============================================================================

-- Failed authentication attempts for security monitoring
CREATE INDEX IF NOT EXISTS idx_audit_failed_auth 
ON audit_events(action, timestamp DESC) 
WHERE action IN ('LOGIN_FAILED', 'AUTH_FAILED', 'ACCESS_DENIED');

-- Administrative actions for compliance auditing
CREATE INDEX IF NOT EXISTS idx_audit_admin_actions 
ON audit_events(action, user_id, timestamp DESC) 
WHERE action IN ('USER_CREATED', 'USER_DELETED', 'ROLE_CHANGED', 'ORG_CREATED', 'ORG_DELETED');

-- Resource access events for data governance
CREATE INDEX IF NOT EXISTS idx_audit_resource_access 
ON audit_events(resource_type, resource_id, timestamp DESC) 
WHERE resource_type IS NOT NULL;

-- ============================================================================
-- HIGH: Performance monitoring and system health
-- ============================================================================

-- Error events for system monitoring
CREATE INDEX IF NOT EXISTS idx_audit_errors 
ON audit_events(severity, timestamp DESC) 
WHERE severity IN ('ERROR', 'CRITICAL');

-- IP address tracking for security analysis
CREATE INDEX IF NOT EXISTS idx_audit_ip_timestamp 
ON audit_events(ip_address, timestamp DESC) 
WHERE ip_address IS NOT NULL;

-- Session tracking for user activity analysis
CREATE INDEX IF NOT EXISTS idx_audit_session_id 
ON audit_events(session_id, timestamp ASC) 
WHERE session_id IS NOT NULL;

-- ============================================================================
-- MEDIUM: Detailed audit analysis
-- ============================================================================

-- User agent tracking for device/browser analytics
CREATE INDEX IF NOT EXISTS idx_audit_user_agent 
ON audit_events(user_agent) 
WHERE user_agent IS NOT NULL;

-- Composite index for detailed security queries
CREATE INDEX IF NOT EXISTS idx_audit_security_composite 
ON audit_events(user_id, ip_address, action, timestamp DESC) 
WHERE action LIKE '%_FAILED' OR action LIKE '%_DENIED';

-- Request path tracking for API usage analytics
CREATE INDEX IF NOT EXISTS idx_audit_request_path 
ON audit_events(request_path, timestamp DESC) 
WHERE request_path IS NOT NULL;

-- ============================================================================
-- MEDIUM: Data retention and cleanup support
-- ============================================================================

-- Timestamp index for data retention jobs
CREATE INDEX IF NOT EXISTS idx_audit_timestamp_retention 
ON audit_events(timestamp ASC);

-- Organization-based cleanup for tenant data deletion
CREATE INDEX IF NOT EXISTS idx_audit_org_cleanup 
ON audit_events(organization_id, timestamp ASC) 
WHERE organization_id IS NOT NULL;

-- ============================================================================
-- Update table statistics for query planner optimization
-- ============================================================================

ANALYZE audit_events;