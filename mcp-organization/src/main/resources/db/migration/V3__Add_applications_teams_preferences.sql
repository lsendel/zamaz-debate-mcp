-- Constants and Common Patterns
-- VARCHAR_DEFAULT: VARCHAR(255)
-- TIMESTAMP_DEFAULT: TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
-- UUID_DEFAULT: UUID PRIMARY KEY DEFAULT gen_random_uuid()
-- AUDIT_COLUMNS: created_at, updated_at, created_by, updated_by

-- Constants
DECLARE
  C_DEFAULT_SCHEMA CONSTANT VARCHAR2(30) := 'PUBLIC';
  C_ERROR_MSG CONSTANT VARCHAR2(100) := 'An error occurred';
END;
/

-- Add applications, teams, and user preferences to organization service
-- This migration extends the organization service with application and team management capabilities

-- Create enums for scope and sharing (if not already created)
DO $$ BEGIN
    CREATE TYPE scope_type AS ENUM ('ORGANIZATION', 'APPLICATION', 'BOTH');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE sharing_level AS ENUM ('ME_ONLY', 'ORGANIZATION', 'APPLICATION_ALL', 'APPLICATION_TEAM', 'APPLICATION_ME');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Create team role enum
CREATE TYPE team_role AS ENUM ('MEMBER', 'LEAD', 'ADMIN');

-- Create applications table
CREATE TABLE applications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    settings JSONB DEFAULT '{}',
    max_teams INTEGER,
    max_members_per_team INTEGER,
    UNIQUE(organization_id, name)
);

-- Create teams table  
CREATE TABLE teams (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    application_id UUID REFERENCES applications(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    settings JSONB DEFAULT '{}',
    max_members INTEGER,
    UNIQUE(application_id, name),
    CONSTRAINT teams_application_or_organization CHECK (
        (application_id IS NOT NULL AND organization_id IS NOT NULL) OR
        (application_id IS NULL AND organization_id IS NOT NULL)
    )
);

-- Create team membership table
CREATE TABLE team_members (
    team_id UUID REFERENCES teams(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    role team_role NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    permissions JSONB DEFAULT '{}',
    PRIMARY KEY (team_id, user_id)
);

-- Create user preferences table
CREATE TABLE user_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    default_scope_type scope_type DEFAULT 'ORGANIZATION',
    default_sharing_level sharing_level DEFAULT 'ORGANIZATION',
    default_organization_id UUID REFERENCES organizations(id),
    default_application_id UUID REFERENCES applications(id),
    default_team_id UUID REFERENCES teams(id),
    preferences JSONB DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Performance indexes for applications
CREATE INDEX idx_applications_organization ON applications(organization_id);
CREATE INDEX idx_applications_active ON applications(is_active);
CREATE INDEX idx_applications_name ON applications(name);
CREATE INDEX idx_applications_created_at ON applications(created_at);

-- Performance indexes for teams
CREATE INDEX idx_teams_organization ON teams(organization_id);
CREATE INDEX idx_teams_application ON teams(application_id);
CREATE INDEX idx_teams_active ON teams(is_active);
CREATE INDEX idx_teams_name ON teams(name);
CREATE INDEX idx_teams_created_at ON teams(created_at);

-- Performance indexes for team members
CREATE INDEX idx_team_members_user ON team_members(user_id);
CREATE INDEX idx_team_members_role ON team_members(role);
CREATE INDEX idx_team_members_active ON team_members(is_active);
CREATE INDEX idx_team_members_joined_at ON team_members(joined_at);

-- Performance indexes for user preferences
CREATE INDEX idx_user_preferences_default_org ON user_preferences(default_organization_id);
CREATE INDEX idx_user_preferences_default_app ON user_preferences(default_application_id);
CREATE INDEX idx_user_preferences_default_team ON user_preferences(default_team_id);

-- Composite indexes for common queries
CREATE INDEX idx_applications_org_active ON applications(organization_id, is_active);
CREATE INDEX idx_teams_org_active ON teams(organization_id, is_active);
CREATE INDEX idx_teams_app_active ON teams(application_id, is_active);
CREATE INDEX idx_team_members_team_active ON team_members(team_id, is_active);

-- Create update triggers for updated_at columns
CREATE TRIGGER update_applications_updated_at BEFORE UPDATE ON applications
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_teams_updated_at BEFORE UPDATE ON teams
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_team_members_updated_at BEFORE UPDATE ON team_members
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_preferences_updated_at BEFORE UPDATE ON user_preferences
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Comments for documentation
COMMENT ON TABLE applications IS 'Applications within organizations that can contain teams and debates';
COMMENT ON COLUMN applications.name IS 'Unique name within the organization';
COMMENT ON COLUMN applications.max_teams IS 'Maximum number of teams allowed in this application';
COMMENT ON COLUMN applications.max_members_per_team IS 'Maximum number of members allowed per team';

COMMENT ON TABLE teams IS 'Teams within applications or organizations that can collaborate on debates';
COMMENT ON COLUMN teams.application_id IS 'Application this team belongs to (can be null for org-level teams)';
COMMENT ON COLUMN teams.organization_id IS 'Organization this team belongs to (always required)';
COMMENT ON COLUMN teams.max_members IS 'Maximum number of members allowed in this team';

COMMENT ON TABLE team_members IS 'Membership relationships between users and teams';
COMMENT ON COLUMN team_members.role IS 'Role of the user within the team (MEMBER, LEAD, ADMIN)';
COMMENT ON COLUMN team_members.permissions IS 'Additional permissions specific to this team membership';

COMMENT ON TABLE user_preferences IS 'User default preferences for debate scope and sharing';
COMMENT ON COLUMN user_preferences.preferences IS 'Additional user preferences stored as JSON';