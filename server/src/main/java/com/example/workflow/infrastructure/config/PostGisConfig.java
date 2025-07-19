package com.example.workflow.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * PostGIS configuration for spatial data operations
 * Configures PostgreSQL with PostGIS extension for geographic queries
 */
@Configuration
public class PostGisConfig {
    
    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/workflow_db}")
    private String databaseUrl;
    
    @Value("${spring.datasource.username:postgres}")
    private String username;
    
    @Value("${spring.datasource.password:password}")
    private String password;
    
    @Bean
    public DataSource spatialDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(databaseUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        
        return dataSource;
    }
    
    @Bean
    public JdbcTemplate spatialJdbcTemplate(DataSource spatialDataSource) {
        return new JdbcTemplate(spatialDataSource);
    }
    
    /**
     * PostGIS initialization component
     */
    @Bean
    public PostGisInitializer postGisInitializer(JdbcTemplate jdbcTemplate) {
        return new PostGisInitializer(jdbcTemplate);
    }
    
    /**
     * PostGIS database initialization
     */
    public static class PostGisInitializer {
        private final JdbcTemplate jdbcTemplate;
        
        public PostGisInitializer(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
            initializePostGis();
        }
        
        private void initializePostGis() {
            try {
                // Enable PostGIS extension
                jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS postgis");
                jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS postgis_topology");
                
                // Create spatial reference systems if needed
                createSpatialReferenceSystems();
                
                // Initialize sample geographic data
                initializeSampleData();
                
            } catch (Exception e) {
                System.err.println("Failed to initialize PostGIS: " + e.getMessage());
            }
        }
        
        private void createSpatialReferenceSystems() {
            // Most common spatial reference systems are already included in PostGIS
            // This method can be used to add custom SRS if needed
        }
        
        private void initializeSampleData() {
            try {
                // Insert sample Stamford, CT addresses for geospatial sample
                String checkStamfordData = "SELECT COUNT(*) FROM stamford_addresses";
                Long count = jdbcTemplate.queryForObject(checkStamfordData, Long.class);
                
                if (count == null || count == 0) {
                    insertStamfordAddresses();
                }
                
                // Insert sample geographic features
                insertGeographicFeatures();
                
            } catch (Exception e) {
                System.err.println("Failed to initialize sample data: " + e.getMessage());
            }
        }
        
        private void insertStamfordAddresses() {
            String[] addresses = {
                "100 Main Street, Stamford, CT",
                "250 Bedford Street, Stamford, CT", 
                "75 Broad Street, Stamford, CT",
                "300 Atlantic Street, Stamford, CT",
                "150 Summer Street, Stamford, CT",
                "200 Elm Street, Stamford, CT",
                "50 Forest Street, Stamford, CT",
                "175 Park Avenue, Stamford, CT",
                "125 Washington Boulevard, Stamford, CT",
                "225 High Ridge Road, Stamford, CT"
            };
            
            // Stamford, CT approximate bounds: 41.0200 to 41.0900 lat, -73.5800 to -73.5000 lng
            double[][] coordinates = {
                {41.0534, -73.5387}, // Downtown Stamford
                {41.0456, -73.5312}, // Bedford Street area
                {41.0523, -73.5401}, // Broad Street area
                {41.0489, -73.5298}, // Atlantic Street area
                {41.0567, -73.5345}, // Summer Street area
                {41.0445, -73.5423}, // Elm Street area
                {41.0612, -73.5456}, // Forest Street area
                {41.0501, -73.5367}, // Park Avenue area
                {41.0478, -73.5334}, // Washington Boulevard area
                {41.0634, -73.5234}  // High Ridge Road area
            };
            
            String sql = """
                INSERT INTO stamford_addresses (address, location)
                VALUES (?, ST_GeomFromText(?, 4326))
                """;
            
            for (int i = 0; i < addresses.length; i++) {
                String wkt = String.format("POINT(%f %f)", coordinates[i][1], coordinates[i][0]);
                jdbcTemplate.update(sql, addresses[i], wkt);
            }
        }
        
        private void insertGeographicFeatures() {
            // Insert sample geographic features for testing
            String sql = """
                INSERT INTO geographic_features (feature_id, name, feature_type, location, properties)
                VALUES (?, ?, ?, ST_GeomFromText(?, 4326), ?::jsonb)
                ON CONFLICT (feature_id) DO NOTHING
                """;
            
            // Sample cities
            Object[][] cities = {
                {"stamford_ct", "Stamford", "CITY", "POINT(-73.5387 41.0534)", "{\"population\": 135470, \"state\": \"CT\"}"},
                {"new_york_ny", "New York", "CITY", "POINT(-74.0060 40.7128)", "{\"population\": 8336817, \"state\": \"NY\"}"},
                {"boston_ma", "Boston", "CITY", "POINT(-71.0589 42.3601)", "{\"population\": 695506, \"state\": \"MA\"}"},
                {"philadelphia_pa", "Philadelphia", "CITY", "POINT(-75.1652 39.9526)", "{\"population\": 1584064, \"state\": \"PA\"}"}
            };
            
            for (Object[] city : cities) {
                jdbcTemplate.update(sql, city[0], city[1], city[2], city[3], city[4]);
            }
            
            // Sample landmarks
            Object[][] landmarks = {
                {"stamford_town_center", "Stamford Town Center", "LANDMARK", "POINT(-73.5387 41.0534)", "{\"type\": \"shopping_mall\"}"},
                {"cove_island_park", "Cove Island Park", "LANDMARK", "POINT(-73.5234 41.0456)", "{\"type\": \"park\"}"},
                {"stamford_harbor", "Stamford Harbor", "LANDMARK", "POINT(-73.5298 41.0445)", "{\"type\": \"harbor\"}"}
            };
            
            for (Object[] landmark : landmarks) {
                jdbcTemplate.update(sql, landmark[0], landmark[1], landmark[2], landmark[3], landmark[4]);
            }
        }
    }
}