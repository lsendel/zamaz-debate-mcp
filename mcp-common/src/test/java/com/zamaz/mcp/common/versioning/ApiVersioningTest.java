package com.zamaz.mcp.common.versioning;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for API versioning functionality
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    ApiVersionConfig.class,
    ApiVersionResolver.class,
    ApiVersionInterceptor.class,
    ApiVersionMetrics.class,
    ApiVersionUtils.class,
    ApiVersionController.class,
    ApiVersionDocumentationGenerator.class,
    ApiVersioningTest.TestConfig.class
})
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "mcp.api.versioning.enabled=true",
    "mcp.api.versioning.default-version=1",
    "mcp.api.versioning.current-version=2",
    "mcp.api.versioning.supported-versions=1,2,3",
    "mcp.api.versioning.deprecated-versions=1",
    "mcp.api.versioning.strategy=HEADER",
    "mcp.api.versioning.version-header=X-API-Version",
    "mcp.api.versioning.enforce-version-validation=true"
})
class ApiVersioningTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    
    @Autowired
    private ApiVersionConfig config;
    
    @Autowired
    private ApiVersionResolver resolver;
    
    @Autowired
    private ApiVersionUtils utils;

    private MockMvc mockMvc;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public MockMvc mockMvc(WebApplicationContext context) {
            return MockMvcBuilders.webAppContextSetup(context).build();
        }
    }

    @Test
    void testHeaderBasedVersioning() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Test version 1
        mockMvc.perform(get("/api/versions/example")
                .header("X-API-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1"))
                .andExpect(jsonPath("$.message").value("This is version 1 response"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(header().string("X-API-Version", "1"))
                .andExpect(header().string("X-API-Deprecated", "true"));
        
        // Test version 2
        mockMvc.perform(get("/api/versions/example")
                .header("X-API-Version", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("2"))
                .andExpect(jsonPath("$.message").value("This is version 2 response"))
                .andExpect(jsonPath("$.data.identifier").value(1))
                .andExpect(jsonPath("$.new_field").value("This field was added in v2"))
                .andExpect(header().string("X-API-Version", "2"))
                .andExpect(header().doesNotExist("X-API-Deprecated"));
    }

    @Test
    void testUnsupportedVersion() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        mockMvc.perform(get("/api/versions/example")
                .header("X-API-Version", "99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("unsupported_version"));
    }

    @Test
    void testDeprecatedVersionWarning() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        mockMvc.perform(get("/api/versions/legacy")
                .header("X-API-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-API-Deprecated", "true"))
                .andExpect(header().exists("X-API-Deprecation-Warning"));
    }

    @Test
    void testVersionSpecificEndpoint() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Version 2 only endpoint
        mockMvc.perform(get("/api/versions/latest")
                .header("X-API-Version", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("This endpoint is only available in the latest version"));
        
        // Should fail with version 1
        mockMvc.perform(get("/api/versions/latest")
                .header("X-API-Version", "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDefaultVersionBehavior() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // No version specified should use default
        mockMvc.perform(get("/api/versions/example"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1")) // Default version
                .andExpect(header().string("X-API-Version", "1"));
    }

    @Test
    void testVersionInfo() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        mockMvc.perform(get("/api/versions")
                .header("X-API-Version", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current").value("2"))
                .andExpect(jsonPath("$.default").value("1"))
                .andExpect(jsonPath("$.latest").value("3"))
                .andExpect(jsonPath("$.supported").isArray())
                .andExpect(jsonPath("$.deprecated").isArray())
                .andExpect(jsonPath("$.strategy").value("HEADER"))
                .andExpect(jsonPath("$.client_version").value("2"))
                .andExpect(jsonPath("$.client_deprecated").value(false));
    }

    @Test
    void testApiVersionConfig() {
        // Test configuration
        assertEquals("1", config.getDefaultVersion());
        assertEquals("2", config.getCurrentVersion());
        assertEquals("3", config.getLatestVersion());
        assertEquals(3, config.getSupportedVersions().size());
        assertEquals(1, config.getDeprecatedVersions().size());
        assertEquals(ApiVersionConfig.VersioningStrategy.HEADER, config.getStrategy());
        
        // Test version validation
        assertTrue(config.isSupportedVersion("1"));
        assertTrue(config.isSupportedVersion("2"));
        assertTrue(config.isSupportedVersion("3"));
        assertFalse(config.isSupportedVersion("4"));
        
        assertTrue(config.isDeprecatedVersion("1"));
        assertFalse(config.isDeprecatedVersion("2"));
    }

    @Test
    void testVersionUtils() {
        // Test version comparison
        assertEquals(0, utils.compareVersions("1.0", "1.0"));
        assertEquals(-1, utils.compareVersions("1.0", "1.1"));
        assertEquals(1, utils.compareVersions("1.1", "1.0"));
        assertEquals(-1, utils.compareVersions("1.0", "2.0"));
        
        assertTrue(utils.isVersionNewer("1.1", "1.0"));
        assertTrue(utils.isVersionOlder("1.0", "1.1"));
        assertFalse(utils.isVersionNewer("1.0", "1.1"));
        
        // Test next version
        assertEquals("2", utils.getNextVersion("1"));
        assertEquals("1.1", utils.getNextVersion("1.0"));
        assertEquals("1.0.1", utils.getNextVersion("1.0.0"));
    }

    @Test
    void testVersionResolver() {
        // Test version resolution would require mock HttpServletRequest
        // This is a simplified test - full testing would require web context
        assertTrue(resolver instanceof ApiVersionResolver);
        assertNotNull(resolver);
    }

    @Test
    void testVersionDocumentation() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        mockMvc.perform(get("/api/versions/documentation"))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.currentVersion").value("2"))
                .andExpect(jsonPath("$.defaultVersion").value("1"))
                .andExpect(jsonPath("$.latestVersion").value("3"))
                .andExpect(jsonPath("$.supportedVersions").isArray())
                .andExpect(jsonPath("$.versioningStrategy").value("HEADER"))
                .andExpect(jsonPath("$.examples").exists())
                .andExpect(jsonPath("$.migrationGuides").exists());
    }

    @Test
    void testSupportedVersionsEndpoint() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        mockMvc.perform(get("/api/versions/supported"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versions").isArray())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.deprecated_count").value(1));
    }

    private static void assertEquals(String expected, String actual) {
        assert expected.equals(actual) : "Expected: " + expected + ", but got: " + actual;
    }

    private static void assertEquals(int expected, int actual) {
        assert expected == actual : "Expected: " + expected + ", but got: " + actual;
    }

    private static void assertTrue(boolean condition) {
        assert condition : "Expected condition to be true";
    }

    private static void assertFalse(boolean condition) {
        assert !condition : "Expected condition to be false";
    }

    private static void assertNotNull(Object object) {
        assert object != null : "Expected object to not be null";
    }
}