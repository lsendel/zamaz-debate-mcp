-- Add scope and sharing functionality to debates table
-- This migration extends the debates table with scope and sharing capabilities

-- Create enums for scope and sharing
CREATE TYPE scope_type AS ENUM ('ORGANIZATION', 'APPLICATION', 'BOTH');
CREATE TYPE sharing_level AS ENUM ('ME_ONLY', 'ORGANIZATION', 'APPLICATION_ALL', 'APPLICATION_TEAM', 'APPLICATION_ME');

-- Add scope and sharing columns to debates table
ALTER TABLE debates 
ADD COLUMN scope_type scope_type NOT NULL DEFAULT 'ORGANIZATION',
ADD COLUMN scope_organization_id UUID,
ADD COLUMN scope_application_id UUID,
ADD COLUMN sharing_level sharing_level NOT NULL DEFAULT 'ORGANIZATION',
ADD COLUMN team_id UUID,
ADD COLUMN created_by_user_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000';

-- Create applications table
CREATE TABLE applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    organization_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT true,
    settings JSONB DEFAULT '{}',
    UNIQUE(organization_id, name)
);

-- Create teams table  
CREATE TABLE teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    application_id UUID REFERENCES applications(id),
    organization_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT true,
    settings JSONB DEFAULT '{}',
    UNIQUE(application_id, name),
    UNIQUE(organization_id, name)
);

-- Create team membership table
CREATE TABLE team_members (
    team_id UUID REFERENCES teams(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    role VARCHAR(50) DEFAULT 'MEMBER',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT true,
    PRIMARY KEY (team_id, user_id)
);

-- Create user preferences table
CREATE TABLE user_preferences (
    user_id UUID PRIMARY KEY,
    default_scope_type scope_type DEFAULT 'ORGANIZATION',
    default_sharing_level sharing_level DEFAULT 'ORGANIZATION',
    default_organization_id UUID,
    default_application_id UUID REFERENCES applications(id),
    default_team_id UUID REFERENCES teams(id),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add foreign key constraints to debates table
ALTER TABLE debates ADD CONSTRAINT fk_debates_application 
    FOREIGN KEY (scope_application_id) REFERENCES applications(id);
ALTER TABLE debates ADD CONSTRAINT fk_debates_team 
    FOREIGN KEY (team_id) REFERENCES teams(id);

-- Add foreign key constraints to teams table
ALTER TABLE teams ADD CONSTRAINT fk_teams_application 
    FOREIGN KEY (application_id) REFERENCES applications(id);

-- Performance indexes for debates scope and sharing
CREATE INDEX idx_debates_scope_type ON debates(scope_type);
CREATE INDEX idx_debates_scope_organization ON debates(scope_organization_id);
CREATE INDEX idx_debates_scope_application ON debates(scope_application_id);
CREATE INDEX idx_debates_sharing_level ON debates(sharing_level);
CREATE INDEX idx_debates_team_id ON debates(team_id);
CREATE INDEX idx_debates_created_by_user ON debates(created_by_user_id);

-- Performance indexes for applications
CREATE INDEX idx_applications_organization ON applications(organization_id);
CREATE INDEX idx_applications_active ON applications(active);
CREATE INDEX idx_applications_name ON applications(name);

-- Performance indexes for teams
CREATE INDEX idx_teams_organization ON teams(organization_id);
CREATE INDEX idx_teams_application ON teams(application_id);
CREATE INDEX idx_teams_active ON teams(active);
CREATE INDEX idx_teams_name ON teams(name);

-- Performance indexes for team members
CREATE INDEX idx_team_members_user ON team_members(user_id);
CREATE INDEX idx_team_members_role ON team_members(role);
CREATE INDEX idx_team_members_active ON team_members(active);

-- Performance indexes for user preferences
CREATE INDEX idx_user_preferences_default_org ON user_preferences(default_organization_id);
CREATE INDEX idx_user_preferences_default_app ON user_preferences(default_application_id);
CREATE INDEX idx_user_preferences_default_team ON user_preferences(default_team_id);

-- Composite indexes for common queries
CREATE INDEX idx_debates_scope_sharing ON debates(scope_type, sharing_level);
CREATE INDEX idx_debates_org_sharing ON debates(scope_organization_id, sharing_level);
CREATE INDEX idx_debates_app_sharing ON debates(scope_application_id, sharing_level);
CREATE INDEX idx_debates_team_sharing ON debates(team_id, sharing_level);

-- Comments for documentation
COMMENT ON COLUMN debates.scope_type IS 'Determines whether debate is scoped to organization, application, or both';
COMMENT ON COLUMN debates.scope_organization_id IS 'Organization ID when scope includes organization';
COMMENT ON COLUMN debates.scope_application_id IS 'Application ID when scope includes application';
COMMENT ON COLUMN debates.sharing_level IS 'Determines who can view or access the debate';
COMMENT ON COLUMN debates.team_id IS 'Team ID when sharing level is APPLICATION_TEAM';
COMMENT ON COLUMN debates.created_by_user_id IS 'User who created the debate';

COMMENT ON TABLE applications IS 'Applications within organizations that can contain teams and debates';
COMMENT ON TABLE teams IS 'Teams within applications that can collaborate on debates';
COMMENT ON TABLE team_members IS 'Membership relationships between users and teams';
COMMENT ON TABLE user_preferences IS 'User default preferences for debate scope and sharing';