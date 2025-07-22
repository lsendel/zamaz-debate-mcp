-- OAuth2 Authorization Server Schema
-- Based on Spring Authorization Server 1.2.x schema

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- OAuth2 Authorization table
CREATE TABLE oauth2_authorization (
    id VARCHAR2(100) NOT NULL,
    registered_client_id VARCHAR2(100) NOT NULL,
    principal_name VARCHAR2(200) NOT NULL,
    authorization_grant_type VARCHAR2(100) NOT NULL,
    authorized_scopes TEXT DEFAULT NULL,
    attributes TEXT DEFAULT NULL,
    state VARCHAR2(500) DEFAULT NULL,
    authorization_code_value TEXT DEFAULT NULL,
    authorization_code_issued_at TIMESTAMP DEFAULT NULL,
    authorization_code_expires_at TIMESTAMP DEFAULT NULL,
    authorization_code_metadata TEXT DEFAULT NULL,
    access_token_value TEXT DEFAULT NULL,
    access_token_issued_at TIMESTAMP DEFAULT NULL,
    access_token_expires_at TIMESTAMP DEFAULT NULL,
    access_token_metadata TEXT DEFAULT NULL,
    access_token_type VARCHAR2(100) DEFAULT NULL,
    access_token_scopes TEXT DEFAULT NULL,
    oidc_id_token_value TEXT DEFAULT NULL,
    oidc_id_token_issued_at TIMESTAMP DEFAULT NULL,
    oidc_id_token_expires_at TIMESTAMP DEFAULT NULL,
    oidc_id_token_metadata TEXT DEFAULT NULL,
    refresh_token_value TEXT DEFAULT NULL,
    refresh_token_issued_at TIMESTAMP DEFAULT NULL,
    refresh_token_expires_at TIMESTAMP DEFAULT NULL,
    refresh_token_metadata TEXT DEFAULT NULL,
    user_code_value TEXT DEFAULT NULL,
    user_code_issued_at TIMESTAMP DEFAULT NULL,
    user_code_expires_at TIMESTAMP DEFAULT NULL,
    user_code_metadata TEXT DEFAULT NULL,
    device_code_value TEXT DEFAULT NULL,
    device_code_issued_at TIMESTAMP DEFAULT NULL,
    device_code_expires_at TIMESTAMP DEFAULT NULL,
    device_code_metadata TEXT DEFAULT NULL,
    PRIMARY KEY (id)
);

-- OAuth2 Authorization Consent table
CREATE TABLE oauth2_authorization_consent (
    registered_client_id VARCHAR2(100) NOT NULL,
    principal_name VARCHAR2(200) NOT NULL,
    authorities TEXT NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);

-- OAuth2 Registered Client table
CREATE TABLE oauth2_registered_client (
    id VARCHAR2(100) NOT NULL,
    client_id VARCHAR2(100) NOT NULL,
    client_id_issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_secret VARCHAR2(200) DEFAULT NULL,
    client_secret_expires_at TIMESTAMP DEFAULT NULL,
    client_name VARCHAR2(200) NOT NULL,
    client_authentication_methods TEXT NOT NULL,
    authorization_grant_types TEXT NOT NULL,
    redirect_uris TEXT DEFAULT NULL,
    post_logout_redirect_uris TEXT DEFAULT NULL,
    scopes TEXT NOT NULL,
    client_settings TEXT NOT NULL,
    token_settings TEXT NOT NULL,
    PRIMARY KEY (id)
);

-- Users table (extends existing schema)
-- Note: This assumes the users table already exists from mcp-organization
-- We'll add additional columns for enhanced security features

-- Check if users table exists, if not create it
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'users') THEN
        CREATE TABLE users (
            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
            email VARCHAR2(255) UNIQUE NOT NULL,
            password_hash VARCHAR2(255) NOT NULL,
            first_name VARCHAR2(100),
            last_name VARCHAR2(100),
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            is_active BOOLEAN DEFAULT true
        );
    END IF;
END
$$;

-- Add new columns for enhanced security if they don't exist
DO $$
BEGIN
    -- Email verification
    IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'email_verified') THEN
        ALTER TABLE users ADD COLUMN email_verified BOOLEAN DEFAULT false;
    END IF;
    
    -- MFA support
    IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'mfa_enabled') THEN
        ALTER TABLE users ADD COLUMN mfa_enabled BOOLEAN DEFAULT false;
    END IF;
    
    IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'mfa_secret') THEN
        ALTER TABLE users ADD COLUMN mfa_secret VARCHAR2(255);
    END IF;
    
    -- Account security
    IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'account_locked') THEN
        ALTER TABLE users ADD COLUMN account_locked BOOLEAN DEFAULT false;
    END IF;
    
    IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'failed_login_attempts') THEN
        ALTER TABLE users ADD COLUMN failed_login_attempts INTEGER DEFAULT 0;
    END IF;
    
    -- Audit fields
    IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'last_login_at') THEN
        ALTER TABLE users ADD COLUMN last_login_at TIMESTAMP;
    END IF;
    
    IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'password_changed_at') THEN
        ALTER TABLE users ADD COLUMN password_changed_at TIMESTAMP;
    END IF;
END
$$;

-- Organizations table (if not exists)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'organizations') THEN
        CREATE TABLE organizations (
            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
            name VARCHAR2(255) NOT NULL,
            description TEXT,
            settings JSONB DEFAULT '{}',
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            is_active BOOLEAN DEFAULT true
        );
    END IF;
END
$$;

-- Organization users mapping (if not exists)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'organization_users') THEN
        CREATE TABLE organization_users (
            organization_id UUID REFERENCES organizations(id) ON DELETE CASCADE,
            user_id UUID REFERENCES users(id) ON DELETE CASCADE,
            role VARCHAR2(50) NOT NULL DEFAULT 'member',
            joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (organization_id, user_id)
        );
    END IF;
END
$$;

-- Refresh tokens table (if not exists)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'refresh_tokens') THEN
        CREATE TABLE refresh_tokens (
            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
            user_id UUID REFERENCES users(id) ON DELETE CASCADE,
            token VARCHAR2(500) UNIQUE NOT NULL,
            expires_at TIMESTAMP NOT NULL,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        );
    END IF;
END
$$;

-- Create indexes for OAuth2 tables
CREATE INDEX IF NOT EXISTS idx_oauth2_authorization_registered_client_id ON oauth2_authorization(registered_client_id);
CREATE INDEX IF NOT EXISTS idx_oauth2_authorization_principal_name ON oauth2_authorization(principal_name);
CREATE INDEX IF NOT EXISTS idx_oauth2_registered_client_client_id ON oauth2_registered_client(client_id);

-- Create indexes for user tables (if not exist)
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_active ON users(is_active);
CREATE INDEX IF NOT EXISTS idx_users_email_verified ON users(email_verified);
CREATE INDEX IF NOT EXISTS idx_users_account_locked ON users(account_locked);
CREATE INDEX IF NOT EXISTS idx_organizations_name ON organizations(name);
CREATE INDEX IF NOT EXISTS idx_organizations_active ON organizations(is_active);
CREATE INDEX IF NOT EXISTS idx_organization_users_user ON organization_users(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- Create update trigger for updated_at (if not exists)
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers if they don't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM information_schema.triggers WHERE trigger_name = 'update_users_updated_at') THEN
        CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
            FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
    END IF;
    
    IF NOT EXISTS (SELECT FROM information_schema.triggers WHERE trigger_name = 'update_organizations_updated_at') THEN
        CREATE TRIGGER update_organizations_updated_at BEFORE UPDATE ON organizations
            FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
    END IF;
END
$$;