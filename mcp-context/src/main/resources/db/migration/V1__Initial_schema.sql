-- Create contexts table
CREATE TABLE contexts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    metadata JSONB DEFAULT '{}',
    total_tokens INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED'))
);

-- Create indexes for contexts
CREATE INDEX idx_context_org_id ON contexts(organization_id);
CREATE INDEX idx_context_user_id ON contexts(user_id);
CREATE INDEX idx_context_created_at ON contexts(created_at);
CREATE INDEX idx_context_last_accessed ON contexts(last_accessed_at);
CREATE INDEX idx_context_status ON contexts(status);
CREATE INDEX idx_context_metadata ON contexts USING GIN(metadata);

-- Create messages table
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    context_id UUID NOT NULL REFERENCES contexts(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    token_count INTEGER,
    metadata JSONB DEFAULT '{}',
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_hidden BOOLEAN DEFAULT FALSE,
    version BIGINT DEFAULT 0,
    CONSTRAINT chk_role CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM', 'FUNCTION'))
);

-- Create indexes for messages
CREATE INDEX idx_message_context_id ON messages(context_id);
CREATE INDEX idx_message_timestamp ON messages(timestamp);
CREATE INDEX idx_message_role ON messages(role);
CREATE INDEX idx_message_hidden ON messages(is_hidden);
CREATE INDEX idx_message_metadata ON messages USING GIN(metadata);

-- Create context versions table
CREATE TABLE context_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    context_id UUID NOT NULL REFERENCES contexts(id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    messages JSONB NOT NULL,
    total_tokens INTEGER,
    metadata JSONB DEFAULT '{}',
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL,
    UNIQUE(context_id, version)
);

-- Create indexes for context versions
CREATE INDEX idx_version_context_id ON context_versions(context_id);
CREATE INDEX idx_version_created_at ON context_versions(created_at);

-- Create shared contexts table
CREATE TABLE shared_contexts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    context_id UUID NOT NULL REFERENCES contexts(id) ON DELETE CASCADE,
    source_organization_id UUID NOT NULL,
    target_organization_id UUID,
    target_user_id UUID,
    permission VARCHAR(50) NOT NULL DEFAULT 'READ',
    shared_by UUID NOT NULL,
    shared_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT TRUE,
    version BIGINT DEFAULT 0,
    CONSTRAINT chk_permission CHECK (permission IN ('READ', 'WRITE', 'ADMIN')),
    CONSTRAINT chk_target CHECK (target_organization_id IS NOT NULL OR target_user_id IS NOT NULL)
);

-- Create indexes for shared contexts
CREATE INDEX idx_shared_context_id ON shared_contexts(context_id);
CREATE INDEX idx_shared_target_org ON shared_contexts(target_organization_id);
CREATE INDEX idx_shared_target_user ON shared_contexts(target_user_id);
CREATE INDEX idx_shared_expires ON shared_contexts(expires_at);
CREATE INDEX idx_shared_active ON shared_contexts(is_active);

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at
CREATE TRIGGER update_contexts_updated_at BEFORE UPDATE ON contexts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Add comments
COMMENT ON TABLE contexts IS 'Stores conversation contexts for multi-tenant system';
COMMENT ON TABLE messages IS 'Stores messages within contexts';
COMMENT ON TABLE context_versions IS 'Stores versioned snapshots of contexts';
COMMENT ON TABLE shared_contexts IS 'Manages context sharing between organizations and users';

-- Grant permissions (assuming app user exists)
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO postgres;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO postgres;