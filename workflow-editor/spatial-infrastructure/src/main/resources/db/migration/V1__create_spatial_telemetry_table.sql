-- Constants and Common Patterns
-- VARCHAR_DEFAULT: VARCHAR(255)
-- TIMESTAMP_DEFAULT: TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
-- UUID_DEFAULT: UUID PRIMARY KEY DEFAULT gen_random_uuid()
-- AUDIT_COLUMNS: created_at, updated_at, created_by, updated_by

-- Enable PostGIS extension
CREATE EXTENSION IF NOT EXISTS postgis;

-- Create spatial telemetry table
CREATE TABLE spatial_telemetry (
    id BIGSERIAL PRIMARY KEY,
    telemetry_id VARCHAR(255) NOT NULL UNIQUE,
    device_id VARCHAR(255) NOT NULL,
    location GEOMETRY(POINT, 4326) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    data JSONB NOT NULL DEFAULT '{}',
    quality_score INTEGER,
    data_source VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Create spatial index
CREATE INDEX idx_spatial_telemetry_location ON spatial_telemetry USING GIST(location);

-- Create other indexes
CREATE INDEX idx_spatial_telemetry_device ON spatial_telemetry(device_id);
CREATE INDEX idx_spatial_telemetry_timestamp ON spatial_telemetry(timestamp);
CREATE INDEX idx_spatial_telemetry_device_timestamp ON spatial_telemetry(device_id, timestamp DESC);

-- Create index for JSONB data
CREATE INDEX idx_spatial_telemetry_data ON spatial_telemetry USING GIN(data);

-- Create Stamford Connecticut addresses table for geospatial sample
CREATE TABLE stamford_addresses (
    id SERIAL PRIMARY KEY,
    address TEXT NOT NULL,
    location GEOMETRY(POINT, 4326) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Create spatial index for addresses
CREATE INDEX idx_stamford_addresses_location ON stamford_addresses USING GIST(location);

-- Insert sample Stamford addresses
INSERT INTO stamford_addresses (address, location) VALUES
    ('123 Main St, Stamford, CT 06901', ST_SetSRID(ST_MakePoint(-73.5387, 41.0534), 4326)),
    ('456 Atlantic St, Stamford, CT 06901', ST_SetSRID(ST_MakePoint(-73.5398, 41.0520), 4326)),
    ('789 Washington Blvd, Stamford, CT 06901', ST_SetSRID(ST_MakePoint(-73.5420, 41.0550), 4326)),
    ('321 Bedford St, Stamford, CT 06901', ST_SetSRID(ST_MakePoint(-73.5410, 41.0560), 4326)),
    ('654 Summer St, Stamford, CT 06901', ST_SetSRID(ST_MakePoint(-73.5430, 41.0540), 4326)),
    ('987 Broad St, Stamford, CT 06901', ST_SetSRID(ST_MakePoint(-73.5425, 41.0525), 4326)),
    ('147 Tresser Blvd, Stamford, CT 06901', ST_SetSRID(ST_MakePoint(-73.5415, 41.0515), 4326)),
    ('258 Grove St, Stamford, CT 06901', ST_SetSRID(ST_MakePoint(-73.5405, 41.0545), 4326)),
    ('369 Franklin St, Stamford, CT 06901', ST_SetSRID(ST_MakePoint(-73.5395, 41.0555), 4326)),
    ('741 Canal St, Stamford, CT 06902', ST_SetSRID(ST_MakePoint(-73.5440, 41.0510), 4326));

-- Create function for finding nearby telemetry
CREATE OR REPLACE FUNCTION find_nearby_telemetry(
    center_lat DOUBLE PRECISION,
    center_lon DOUBLE PRECISION,
    radius_meters DOUBLE PRECISION
)
RETURNS TABLE(
    telemetry_id VARCHAR,
    device_id VARCHAR,
    distance_meters DOUBLE PRECISION
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        st.telemetry_id,
        st.device_id,
        ST_Distance(
            st.location::geography,
            ST_SetSRID(ST_MakePoint(center_lon, center_lat), 4326)::geography
        ) as distance_meters
    FROM spatial_telemetry st
    WHERE ST_DWithin(
        st.location::geography,
        ST_SetSRID(ST_MakePoint(center_lon, center_lat), 4326)::geography,
        radius_meters
    )
    ORDER BY distance_meters;
END;
$$ LANGUAGE plpgsql;