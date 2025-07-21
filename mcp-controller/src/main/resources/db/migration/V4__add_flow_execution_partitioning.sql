-- Convert flow_executions to partitioned table for better performance at scale

-- Create new partitioned table
CREATE TABLE IF NOT EXISTS flow_executions_partitioned (
    id UUID NOT NULL,
    flow_id UUID NOT NULL,
    debate_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    participant_id UUID,
    flow_type VARCHAR(50) NOT NULL,
    prompt TEXT NOT NULL,
    final_answer TEXT,
    reasoning TEXT,
    metadata JSONB,
    status VARCHAR(20) NOT NULL,
    confidence DECIMAL(5,2),
    execution_time BIGINT,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_flow_executions_partitioned PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- Create monthly partitions for the next 12 months
DO $$
DECLARE
    start_date date := date_trunc('month', CURRENT_DATE);
    end_date date;
    partition_name text;
BEGIN
    FOR i IN 0..11 LOOP
        end_date := start_date + interval '1 month';
        partition_name := 'flow_executions_' || to_char(start_date, 'YYYY_MM');
        
        EXECUTE format(
            'CREATE TABLE IF NOT EXISTS %I PARTITION OF flow_executions_partitioned
            FOR VALUES FROM (%L) TO (%L)',
            partition_name,
            start_date,
            end_date
        );
        
        -- Create indexes on partition
        EXECUTE format(
            'CREATE INDEX IF NOT EXISTS idx_%s_flow_id ON %I(flow_id)',
            partition_name, partition_name
        );
        
        EXECUTE format(
            'CREATE INDEX IF NOT EXISTS idx_%s_debate_id ON %I(debate_id)',
            partition_name, partition_name
        );
        
        EXECUTE format(
            'CREATE INDEX IF NOT EXISTS idx_%s_org_type ON %I(organization_id, flow_type)',
            partition_name, partition_name
        );
        
        EXECUTE format(
            'CREATE INDEX IF NOT EXISTS idx_%s_created_at ON %I(created_at DESC)',
            partition_name, partition_name
        );
        
        start_date := end_date;
    END LOOP;
END $$;

-- Create default partition for data outside the defined ranges
CREATE TABLE IF NOT EXISTS flow_executions_default 
PARTITION OF flow_executions_partitioned DEFAULT;

-- Create function to automatically create new monthly partitions
CREATE OR REPLACE FUNCTION create_monthly_partition()
RETURNS void AS $$
DECLARE
    partition_date date;
    partition_name text;
    start_date date;
    end_date date;
BEGIN
    -- Get the date for next month
    partition_date := date_trunc('month', CURRENT_DATE + interval '1 month');
    partition_name := 'flow_executions_' || to_char(partition_date, 'YYYY_MM');
    start_date := partition_date;
    end_date := partition_date + interval '1 month';
    
    -- Check if partition already exists
    IF NOT EXISTS (
        SELECT 1 FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relname = partition_name
        AND n.nspname = 'public'
    ) THEN
        -- Create the partition
        EXECUTE format(
            'CREATE TABLE %I PARTITION OF flow_executions_partitioned
            FOR VALUES FROM (%L) TO (%L)',
            partition_name,
            start_date,
            end_date
        );
        
        -- Create indexes
        EXECUTE format(
            'CREATE INDEX idx_%s_flow_id ON %I(flow_id)',
            partition_name, partition_name
        );
        
        EXECUTE format(
            'CREATE INDEX idx_%s_debate_id ON %I(debate_id)',
            partition_name, partition_name
        );
        
        EXECUTE format(
            'CREATE INDEX idx_%s_org_type ON %I(organization_id, flow_type)',
            partition_name, partition_name
        );
        
        EXECUTE format(
            'CREATE INDEX idx_%s_created_at ON %I(created_at DESC)',
            partition_name, partition_name
        );
        
        RAISE NOTICE 'Created partition: %', partition_name;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Schedule monthly partition creation (requires pg_cron extension)
-- Uncomment if pg_cron is available:
-- SELECT cron.schedule('create-monthly-partition', '0 0 1 * *', 'SELECT create_monthly_partition()');

-- Create view for backward compatibility
CREATE OR REPLACE VIEW flow_executions AS
SELECT * FROM flow_executions_partitioned;

-- Add comments
COMMENT ON TABLE flow_executions_partitioned IS 'Partitioned table for agentic flow execution history';
COMMENT ON FUNCTION create_monthly_partition() IS 'Automatically creates monthly partitions for flow executions';