-- PostGIS Spatial Tables Migration
-- Creates spatial telemetry tables with proper indexes and constraints

-- Enable PostGIS extension
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;

-- Create spatial telemetry table
CREATE TABLE IF NOT EXISTS spatial_telemetry (
    id SERIAL PRIMARY KEY,
    telemetry_id VARCHAR(255) NOT NULL,
    device_id VARCHAR(255) NOT NULL,
    organization_id VARCHAR(255) NOT NULL,
    location GEOMETRY(POINT, 4326) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    metrics JSONB NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Create spatial indexes for performance
CREATE INDEX IF NOT EXISTS idx_spatial_telemetry_location 
    ON spatial_telemetry USING GIST(location);
CREATE INDEX IF NOT EXISTS idx_spatial_telemetry_timestamp 
    ON spatial_telemetry(timestamp);
CREATE INDEX IF NOT EXISTS idx_spatial_telemetry_device 
    ON spatial_telemetry(device_id);
CREATE INDEX IF NOT EXISTS idx_spatial_telemetry_org 
    ON spatial_telemetry(organization_id);
CREATE INDEX IF NOT EXISTS idx_spatial_telemetry_metrics 
    ON spatial_telemetry USING GIN(metrics);

-- Composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_spatial_telemetry_org_timestamp 
    ON spatial_telemetry(organization_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_spatial_telemetry_device_timestamp 
    ON spatial_telemetry(device_id, timestamp);

-- Create Stamford addresses table for geospatial sample
CREATE TABLE IF NOT EXISTS stamford_addresses (
    id SERIAL PRIMARY KEY,
    address TEXT NOT NULL,
    location GEOMETRY(POINT, 4326) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_stamford_addresses_location 
    ON stamford_addresses USING GIST(location);

-- Create geographic features table
CREATE TABLE IF NOT EXISTS geographic_features (
    id SERIAL PRIMARY KEY,
    feature_id VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    feature_type VARCHAR(50) NOT NULL,
    location GEOMETRY(POINT, 4326),
    bounds GEOMETRY(POLYGON, 4326),
    properties JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_geographic_features_location 
    ON geographic_features USING GIST(location);
CREATE INDEX IF NOT EXISTS idx_geographic_features_bounds 
    ON geographic_features USING GIST(bounds);
CREATE INDEX IF NOT EXISTS idx_geographic_features_type 
    ON geographic_features(feature_type);
CREATE INDEX IF NOT EXISTS idx_geographic_features_name 
    ON geographic_features(name);

-- Create spatial analysis helper functions
CREATE OR REPLACE FUNCTION calculate_spatial_density(
    bbox_min_lng DOUBLE PRECISION,
    bbox_min_lat DOUBLE PRECISION,
    bbox_max_lng DOUBLE PRECISION,
    bbox_max_lat DOUBLE PRECISION,
    grid_size INTEGER DEFAULT 10
)
RETURNS TABLE(
    grid_x INTEGER,
    grid_y INTEGER,
    point_count BIGINT,
    density DOUBLE PRECISION
) AS $$
BEGIN
    RETURN QUERY
    WITH grid AS (
        SELECT 
            i as grid_x, 
            j as grid_y,
            ST_MakeEnvelope(
                bbox_min_lng + (bbox_max_lng - bbox_min_lng) * i / grid_size,
                bbox_min_lat + (bbox_max_lat - bbox_min_lat) * j / grid_size,
                bbox_min_lng + (bbox_max_lng - bbox_min_lng) * (i + 1) / grid_size,
                bbox_min_lat + (bbox_max_lat - bbox_min_lat) * (j + 1) / grid_size,
                4326
            ) as cell
        FROM generate_series(0, grid_size - 1) i,
             generate_series(0, grid_size - 1) j
    ),
    density_calc AS (
        SELECT 
            g.grid_x, 
            g.grid_y, 
            COUNT(st.location) as point_count,
            COUNT(st.location)::DOUBLE PRECISION / ST_Area(ST_Transform(g.cell, 3857)) * 1000000 as density
        FROM grid g
        LEFT JOIN spatial_telemetry st ON ST_Within(st.location, g.cell)
        GROUP BY g.grid_x, g.grid_y, g.cell
    )
    SELECT 
        dc.grid_x,
        dc.grid_y,
        dc.point_count,
        dc.density
    FROM density_calc dc
    ORDER BY dc.grid_x, dc.grid_y;
END;
$$ LANGUAGE plpgsql;

-- Create function for finding spatial clusters
CREATE OR REPLACE FUNCTION find_spatial_clusters(
    org_id VARCHAR(255),
    cluster_radius_meters DOUBLE PRECISION DEFAULT 1000,
    min_points INTEGER DEFAULT 3
)
RETURNS TABLE(
    cluster_id INTEGER,
    point_count BIGINT,
    center_lat DOUBLE PRECISION,
    center_lng DOUBLE PRECISION,
    radius_km DOUBLE PRECISION
) AS $$
BEGIN
    RETURN QUERY
    WITH clustered_points AS (
        SELECT 
            device_id, 
            location, 
            ST_ClusterDBSCAN(location, cluster_radius_meters, min_points) OVER() AS cluster_id
        FROM spatial_telemetry
        WHERE organization_id = org_id
    ),
    cluster_stats AS (
        SELECT 
            cp.cluster_id,
            COUNT(*) as point_count,
            ST_Y(ST_Centroid(ST_Collect(cp.location))) as center_lat,
            ST_X(ST_Centroid(ST_Collect(cp.location))) as center_lng,
            GREATEST(
                ST_Distance(
                    ST_Centroid(ST_Collect(cp.location)),
                    ST_Envelope(ST_Collect(cp.location))
                ),
                cluster_radius_meters
            ) / 1000 as radius_km
        FROM clustered_points cp
        WHERE cp.cluster_id IS NOT NULL
        GROUP BY cp.cluster_id
        HAVING COUNT(*) >= min_points
    )
    SELECT 
        cs.cluster_id,
        cs.point_count,
        cs.center_lat,
        cs.center_lng,
        cs.radius_km
    FROM cluster_stats cs
    ORDER BY cs.point_count DESC;
END;
$$ LANGUAGE plpgsql;

-- Create function for proximity analysis
CREATE OR REPLACE FUNCTION find_nearby_devices(
    target_device_id VARCHAR(255),
    radius_meters DOUBLE PRECISION DEFAULT 1000
)
RETURNS TABLE(
    nearby_device_id VARCHAR(255),
    distance_meters DOUBLE PRECISION,
    last_seen TIMESTAMPTZ,
    lat DOUBLE PRECISION,
    lng DOUBLE PRECISION
) AS $$
BEGIN
    RETURN QUERY
    WITH target_location AS (
        SELECT location, timestamp
        FROM spatial_telemetry
        WHERE device_id = target_device_id
        ORDER BY timestamp DESC
        LIMIT 1
    ),
    nearby_devices AS (
        SELECT DISTINCT ON (st.device_id)
            st.device_id,
            ST_Distance(st.location, tl.location) as distance_meters,
            st.timestamp as last_seen,
            ST_Y(st.location) as lat,
            ST_X(st.location) as lng
        FROM spatial_telemetry st, target_location tl
        WHERE st.device_id != target_device_id
          AND ST_DWithin(st.location, tl.location, radius_meters)
        ORDER BY st.device_id, st.timestamp DESC
    )
    SELECT 
        nd.device_id,
        nd.distance_meters,
        nd.last_seen,
        nd.lat,
        nd.lng
    FROM nearby_devices nd
    ORDER BY nd.distance_meters;
END;
$$ LANGUAGE plpgsql;

-- Create triggers for automatic timestamp updates
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_spatial_telemetry_updated_at
    BEFORE UPDATE ON spatial_telemetry
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_geographic_features_updated_at
    BEFORE UPDATE ON geographic_features
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add constraints
ALTER TABLE spatial_telemetry 
    ADD CONSTRAINT chk_spatial_telemetry_valid_location 
    CHECK (ST_IsValid(location));

ALTER TABLE geographic_features 
    ADD CONSTRAINT chk_geographic_features_valid_location 
    CHECK (location IS NULL OR ST_IsValid(location));

ALTER TABLE geographic_features 
    ADD CONSTRAINT chk_geographic_features_valid_bounds 
    CHECK (bounds IS NULL OR ST_IsValid(bounds));

-- Add comments for documentation
COMMENT ON TABLE spatial_telemetry IS 'Stores telemetry data with spatial coordinates';
COMMENT ON COLUMN spatial_telemetry.location IS 'Point geometry in WGS84 (SRID 4326)';
COMMENT ON COLUMN spatial_telemetry.metrics IS 'JSON object containing telemetry metrics';

COMMENT ON TABLE stamford_addresses IS 'Sample addresses in Stamford, CT for geospatial testing';
COMMENT ON TABLE geographic_features IS 'Geographic features like cities, landmarks, and regions';

COMMENT ON FUNCTION calculate_spatial_density IS 'Calculates point density within a bounding box using a grid';
COMMENT ON FUNCTION find_spatial_clusters IS 'Finds spatial clusters using DBSCAN algorithm';
COMMENT ON FUNCTION find_nearby_devices IS 'Finds devices within a specified radius of a target device';