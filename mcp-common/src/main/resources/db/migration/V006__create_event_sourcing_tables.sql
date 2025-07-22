-- Constants and Common Patterns
-- VARCHAR_DEFAULT: VARCHAR(255)
-- TIMESTAMP_DEFAULT: TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
-- UUID_DEFAULT: UUID PRIMARY KEY DEFAULT gen_random_uuid()
-- AUDIT_COLUMNS: created_at, updated_at, created_by, updated_by

-- Event Sourcing Tables for MCP System

-- Events table for storing all domain events
CREATE TABLE IF NOT EXISTS events (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    user_id VARCHAR(100),
    organization_id VARCHAR(100),
    correlation_id VARCHAR(100),
    payload JSONB,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT events_aggregate_version_unique UNIQUE (aggregate_id, version)
);

-- Snapshots table for storing aggregate snapshots
CREATE TABLE IF NOT EXISTS snapshots (
    id BIGSERIAL PRIMARY KEY,
    aggregate_id VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    data JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT snapshots_aggregate_version_unique UNIQUE (aggregate_id, version)
);

-- Event subscriptions table for managing event subscriptions
CREATE TABLE IF NOT EXISTS event_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    subscription_id VARCHAR(100) NOT NULL UNIQUE,
    event_types TEXT[], -- Array of event types to subscribe to
    from_timestamp TIMESTAMP NOT NULL,
    handler_class VARCHAR(255) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance optimization

-- Events indexes
CREATE INDEX IF NOT EXISTS idx_events_aggregate_id ON events (aggregate_id);
CREATE INDEX IF NOT EXISTS idx_events_aggregate_type ON events (aggregate_type);
CREATE INDEX IF NOT EXISTS idx_events_event_type ON events (event_type);
CREATE INDEX IF NOT EXISTS idx_events_timestamp ON events (timestamp);
CREATE INDEX IF NOT EXISTS idx_events_user_id ON events (user_id);
CREATE INDEX IF NOT EXISTS idx_events_organization_id ON events (organization_id);
CREATE INDEX IF NOT EXISTS idx_events_correlation_id ON events (correlation_id);
CREATE INDEX IF NOT EXISTS idx_events_aggregate_version ON events (aggregate_id, version);

-- Composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_events_org_timestamp ON events (organization_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_events_user_timestamp ON events (user_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_events_type_timestamp ON events (event_type, timestamp);

-- Snapshots indexes
CREATE INDEX IF NOT EXISTS idx_snapshots_aggregate_id ON snapshots (aggregate_id);
CREATE INDEX IF NOT EXISTS idx_snapshots_aggregate_version ON snapshots (aggregate_id, version DESC);

-- Event subscriptions indexes
CREATE INDEX IF NOT EXISTS idx_event_subscriptions_status ON event_subscriptions (status);

-- Partitioning for large event tables (by month)
-- This is commented out as it requires manual setup based on data volume
-- CREATE TABLE events_y2024m01 PARTITION OF events FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
-- CREATE TABLE events_y2024m02 PARTITION OF events FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');
-- ... continue for other months

-- Functions for event store operations

-- Function to get current version of an aggregate
CREATE OR REPLACE FUNCTION get_current_version(p_aggregate_id VARCHAR(100))
RETURNS BIGINT AS $$
BEGIN
    RETURN COALESCE(
        (SELECT MAX(version) FROM events WHERE aggregate_id = p_aggregate_id),
        0
    );
END;
$$ LANGUAGE plpgsql;

-- Function to append events with concurrency control
CREATE OR REPLACE FUNCTION append_events(
    p_aggregate_id VARCHAR(100),
    p_expected_version BIGINT,
    p_events JSONB
) RETURNS VOID AS $$
DECLARE
    current_version BIGINT;
    event_record JSONB;
BEGIN
    -- Get current version
    current_version := get_current_version(p_aggregate_id);
    
    -- Check optimistic concurrency
    IF current_version != p_expected_version THEN
        RAISE EXCEPTION 'Concurrency conflict: Expected version % but current version is %', 
            p_expected_version, current_version;
    END IF;
    
    -- Insert all events
    FOR event_record IN SELECT * FROM jsonb_array_elements(p_events) LOOP
        INSERT INTO events (
            event_id, event_type, aggregate_id, aggregate_type, version,
            timestamp, user_id, organization_id, correlation_id, payload, metadata
        ) VALUES (
            (event_record->>'event_id')::UUID,
            event_record->>'event_type',
            event_record->>'aggregate_id',
            event_record->>'aggregate_type',
            (event_record->>'version')::BIGINT,
            (event_record->>'timestamp')::TIMESTAMP,
            event_record->>'user_id',
            event_record->>'organization_id',
            event_record->>'correlation_id',
            event_record->'payload',
            event_record->'metadata'
        );
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Function to clean up old events (for data retention)
CREATE OR REPLACE FUNCTION cleanup_old_events(
    p_retention_days INTEGER DEFAULT 365
) RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM events 
    WHERE timestamp < NOW() - INTERVAL '1 day' * p_retention_days;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Function to get events for aggregate with pagination
CREATE OR REPLACE FUNCTION get_events_for_aggregate(
    p_aggregate_id VARCHAR(100),
    p_from_version BIGINT DEFAULT 1,
    p_to_version BIGINT DEFAULT NULL,
    p_limit INTEGER DEFAULT 1000
) RETURNS TABLE (
    event_id UUID,
    event_type VARCHAR(100),
    aggregate_id VARCHAR(100),
    aggregate_type VARCHAR(50),
    version BIGINT,
    timestamp TIMESTAMP,
    user_id VARCHAR(100),
    organization_id VARCHAR(100),
    correlation_id VARCHAR(100),
    payload JSONB,
    metadata JSONB
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        e.event_id, e.event_type, e.aggregate_id, e.aggregate_type, e.version,
        e.timestamp, e.user_id, e.organization_id, e.correlation_id, e.payload, e.metadata
    FROM events e
    WHERE e.aggregate_id = p_aggregate_id
      AND e.version >= p_from_version
      AND (p_to_version IS NULL OR e.version <= p_to_version)
    ORDER BY e.version ASC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

-- Trigger to update snapshots table updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_event_subscriptions_updated_at
    BEFORE UPDATE ON event_subscriptions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Grant permissions (adjust based on your user setup)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON events TO mcp_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON snapshots TO mcp_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON event_subscriptions TO mcp_user;
-- GRANT USAGE ON SEQUENCE events_id_seq TO mcp_user;
-- GRANT USAGE ON SEQUENCE snapshots_id_seq TO mcp_user;
-- GRANT USAGE ON SEQUENCE event_subscriptions_id_seq TO mcp_user;

-- Comments for documentation
COMMENT ON TABLE events IS 'Stores all domain events for event sourcing';
COMMENT ON TABLE snapshots IS 'Stores aggregate snapshots for performance optimization';
COMMENT ON TABLE event_subscriptions IS 'Manages event subscriptions for event handlers';

COMMENT ON COLUMN events.event_id IS 'Unique identifier for the event';
COMMENT ON COLUMN events.event_type IS 'Type of the event (e.g., user.created, debate.started)';
COMMENT ON COLUMN events.aggregate_id IS 'Identifier of the aggregate root';
COMMENT ON COLUMN events.aggregate_type IS 'Type of the aggregate (e.g., user, debate, organization)';
COMMENT ON COLUMN events.version IS 'Version of the aggregate after this event';
COMMENT ON COLUMN events.timestamp IS 'When the event occurred';
COMMENT ON COLUMN events.user_id IS 'User who triggered the event';
COMMENT ON COLUMN events.organization_id IS 'Organization context';
COMMENT ON COLUMN events.correlation_id IS 'Correlation ID for tracing related events';
COMMENT ON COLUMN events.payload IS 'Event data as JSON';
COMMENT ON COLUMN events.metadata IS 'Additional metadata as JSON';

COMMENT ON COLUMN snapshots.aggregate_id IS 'Identifier of the aggregate';
COMMENT ON COLUMN snapshots.version IS 'Version of the aggregate at snapshot time';
COMMENT ON COLUMN snapshots.timestamp IS 'When the snapshot was created';
COMMENT ON COLUMN snapshots.data IS 'Serialized aggregate state as JSON';

COMMENT ON COLUMN event_subscriptions.subscription_id IS 'Unique identifier for the subscription';
COMMENT ON COLUMN event_subscriptions.event_types IS 'Array of event types to subscribe to';
COMMENT ON COLUMN event_subscriptions.from_timestamp IS 'Starting timestamp for events';
COMMENT ON COLUMN event_subscriptions.handler_class IS 'Class name of the event handler';
COMMENT ON COLUMN event_subscriptions.status IS 'Status of the subscription (ACTIVE, INACTIVE)';