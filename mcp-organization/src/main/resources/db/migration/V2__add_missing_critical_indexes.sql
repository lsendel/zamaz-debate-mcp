-- Constants and Common Patterns
-- VARCHAR_DEFAULT: VARCHAR(255)
-- TIMESTAMP_DEFAULT: TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
-- UUID_DEFAULT: UUID PRIMARY KEY DEFAULT gen_random_uuid()
-- AUDIT_COLUMNS: created_at, updated_at, created_by, updated_by

-- Critical missing indexes for organization module
-- Adds essential indexes for performance optimization

-- ============================================================================
-- CRITICAL: Foreign key indexes (PostgreSQL doesn't auto-create these)
-- ============================================================================

-- Organization users foreign keys
CREATE INDEX IF NOT EXISTS idx_org_users_organization_id 
ON organization_users(organization_id);

CREATE INDEX IF NOT EXISTS idx_org_users_user_id 
ON organization_users(user_id);

-- ============================================================================
-- CRITICAL: Authentication and status queries
-- ============================================================================

-- User status for active/inactive filtering
CREATE INDEX IF NOT EXISTS idx_users_status 
ON users(status);

-- Email verification for unverified users (partial index for efficiency)
CREATE INDEX IF NOT EXISTS idx_users_email_verified 
ON users(email_verified) 
WHERE email_verified = false;

-- ============================================================================
-- CRITICAL: Composite indexes for common multi-column queries
-- ============================================================================

-- User-organization membership with role (most common query pattern)
CREATE INDEX IF NOT EXISTS idx_org_users_composite 
ON organization_users(user_id, organization_id, role);

-- Non-member roles for permission queries (partial index)
CREATE INDEX IF NOT EXISTS idx_org_users_role 
ON organization_users(role) 
WHERE role != 'MEMBER';

-- ============================================================================
-- HIGH: Time-based queries for pagination and sorting
-- ============================================================================

-- User creation time for pagination
CREATE INDEX IF NOT EXISTS idx_users_created_at 
ON users(created_at DESC);

-- Organization creation time for admin dashboards
CREATE INDEX IF NOT EXISTS idx_organizations_created_at 
ON organizations(created_at DESC);

-- Organization membership join time
CREATE INDEX IF NOT EXISTS idx_org_users_joined_at 
ON organization_users(joined_at DESC);

-- ============================================================================
-- HIGH: Performance optimizations for name searches
-- ============================================================================

-- Case-insensitive name searches for users
CREATE INDEX IF NOT EXISTS idx_users_name_lower 
ON users(LOWER(first_name), LOWER(last_name));

-- Case-insensitive organization name searches
CREATE INDEX IF NOT EXISTS idx_organizations_name_lower 
ON organizations(LOWER(name));

-- ============================================================================
-- HIGH: Active record filtering with compound conditions
-- ============================================================================

-- Active organizations with creation time for dashboard queries
CREATE INDEX IF NOT EXISTS idx_organizations_active 
ON organizations(is_active, created_at DESC) 
WHERE is_active = true;

-- ============================================================================
-- HIGH: JSONB settings queries using GIN index
-- ============================================================================

-- Settings JSONB queries (partial index to exclude null values)
CREATE INDEX IF NOT EXISTS idx_organizations_settings_gin 
ON organizations USING gin(settings) 
WHERE settings IS NOT NULL;

-- ============================================================================
-- Update table statistics for query planner optimization
-- ============================================================================

ANALYZE users;
ANALYZE organizations;  
ANALYZE organization_users;