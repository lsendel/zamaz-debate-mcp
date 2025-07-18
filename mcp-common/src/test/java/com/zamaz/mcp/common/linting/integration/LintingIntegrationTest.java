package com.zamaz.mcp.common.linting.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.zamaz.mcp.common.linting.LintingConfiguration;
import com.zamaz.mcp.common.linting.LintingContext;
import com.zamaz.mcp.common.linting.LintingEngine;
import com.zamaz.mcp.common.linting.LintingResult;
import com.zamaz.mcp.common.linting.ReportFormat;
import com.zamaz.mcp.common.linting.config.ConfigurationInheritance;
import com.zamaz.mcp.common.linting.impl.LintingEngineImpl;
import com.zamaz.mcp.common.linting.metrics.LintingMetricsCollector;
import com.zamaz.mcp.common.linting.report.ReportGenerator;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Integration tests for the complete linting system.
 */
class LintingIntegrationTest {

    @TempDir
    Path tempDir;

    private LintingEngine lintingEngine;
    private ReportGenerator reportGenerator;
    private LintingMetricsCollector metricsCollector;
    private ConfigurationInheritance configInheritance;

    @BeforeEach
    void setUp() {
        lintingEngine = new LintingEngineImpl();
        reportGenerator = new ReportGenerator();
        metricsCollector = new LintingMetricsCollector(new SimpleMeterRegistry());
        configInheritance = new ConfigurationInheritance();
    }

    @Test
    void testCompleteWorkflow_JavaProject() throws Exception {
        // Given - Create a sample Java project structure
        createSampleJavaProject();

        // Load configuration
        LintingConfiguration config = configInheritance.loadConfiguration(tempDir, null);
        LintingContext context = LintingContext.builder()
                .projectRoot(tempDir)
                .configuration(config)
                .parallelExecution(true)
                .autoFix(false)
                .build();

        // When - Run complete linting workflow
        LintingResult result = lintingEngine.lintProject(context);

        // Record metrics
        metricsCollector.recordLintingResult(result, "test-project");

        // Generate reports
        String consoleReport = reportGenerator.generateReport(result, ReportFormat.CONSOLE, "Integration Test Project");
        String jsonReport = reportGenerator.generateReport(result, ReportFormat.JSON, "Integration Test Project");
        String htmlReport = reportGenerator.generateReport(result, ReportFormat.HTML, "Integration Test Project");

        // Then - Verify complete workflow
        assertNotNull(result);
        assertTrue(result.getFilesProcessed() > 0);
        assertNotNull(result.getIssues());
        assertTrue(result.getDurationMs() > 0);

        // Verify reports
        assertNotNull(consoleReport);
        assertTrue(consoleReport.contains("Integration Test Project"));

        assertNotNull(jsonReport);
        assertTrue(jsonReport.startsWith("{"));

        assertNotNull(htmlReport);
        assertTrue(htmlReport.contains("<!DOCTYPE html>"));

        // Verify metrics
        LintingMetricsCollector.QualityMetrics metrics = metricsCollector.getCurrentQualityMetrics();
        assertEquals(1, metrics.getTotalRuns());
        assertTrue(metrics.getTotalFilesProcessed() > 0);
    }

    @Test
    void testServiceSpecificLinting() throws Exception {
        // Given - Create service structure
        createSampleServiceProject("mcp-test-service");

        LintingConfiguration config = configInheritance.loadConfiguration(tempDir, "mcp-test-service");
        LintingContext context = LintingContext.builder()
                .projectRoot(tempDir)
                .configuration(config)
                .parallelExecution(false)
                .autoFix(false)
                .build();

        // When
        LintingResult result = lintingEngine.lintService("mcp-test-service", context);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccessful());
    }

    @Test
    void testMultipleReportFormats() throws Exception {
        // Given
        createSampleJavaProject();
        LintingConfiguration config = configInheritance.loadConfiguration(tempDir, null);
        LintingContext context = LintingContext.builder()
                .projectRoot(tempDir)
                .configuration(config)
                .build();

        LintingResult result = lintingEngine.lintProject(context);

        // When - Generate all report formats
        ReportFormat[] formats = {
                ReportFormat.CONSOLE,
                ReportFormat.JSON,
                ReportFormat.HTML,
                ReportFormat.XML,
                ReportFormat.MARKDOWN
        };

        // Then - Verify all formats work
        for (ReportFormat format : formats) {
            String report = reportGenerator.generateReport(result, format, "Multi-Format Test");
            assertNotNull(report, "Report should not be null for format: " + format);
            assertFalse(report.trim().isEmpty(), "Report should not be empty for format: " + format);
        }
    }

    @Test
    void testConfigurationInheritance() throws Exception {
        // Given - Create global and service-specific configurations
        createGlobalConfiguration();
        createServiceConfiguration("mcp-test-service");

        // When
        LintingConfiguration globalConfig = configInheritance.loadConfiguration(tempDir, null);
        LintingConfiguration serviceConfig = configInheritance.loadConfiguration(tempDir, "mcp-test-service");

        // Then
        assertNotNull(globalConfig);
        assertNotNull(serviceConfig);

        // Service config should have merged settings
        assertNotNull(serviceConfig.getLinters());
        assertNotNull(serviceConfig.getExcludePatterns());
        assertNotNull(serviceConfig.getThresholds());
    }

    @Test
    void testMetricsCollection() throws Exception {
        // Given
        createSampleJavaProject();
        LintingConfiguration config = configInheritance.loadConfiguration(tempDir, null);
        LintingContext context = LintingContext.builder()
                .projectRoot(tempDir)
                .configuration(config)
                .build();

        // When - Run multiple linting operations
        for (int i = 0; i < 3; i++) {
            LintingResult result = lintingEngine.lintProject(context);
            metricsCollector.recordLintingResult(result, "test-service-" + i);
        }

        // Then
        LintingMetricsCollector.QualityMetrics metrics = metricsCollector.getCurrentQualityMetrics();
        assertEquals(3, metrics.getTotalRuns());
        assertTrue(metrics.getTotalFilesProcessed() > 0);

        // Verify service-specific metrics
        assertTrue(metrics.getIssueCountsByService().size() >= 1);
    }

    @Test
    void testErrorHandling() throws Exception {
        // Given - Invalid project structure
        Path invalidPath = tempDir.resolve("non-existent");

        LintingConfiguration config = configInheritance.loadConfiguration(tempDir, null);
        LintingContext context = LintingContext.builder()
                .projectRoot(invalidPath)
                .configuration(config)
                .build();

        // When
        LintingResult result = lintingEngine.lintProject(context);

        // Then - Should handle gracefully
        assertNotNull(result);
        // Result might be unsuccessful, but should not throw exception
    }

    @Test
    void testParallelVsSequentialExecution() throws Exception {
        // Given
        createLargeJavaProject(20); // Create 20 Java files

        LintingConfiguration config = configInheritance.loadConfiguration(tempDir, null);

        LintingContext parallelContext = LintingContext.builder()
                .projectRoot(tempDir)
                .configuration(config)
                .parallelExecution(true)
                .build();

        LintingContext sequentialContext = LintingContext.builder()
                .projectRoot(tempDir)
                .configuration(config)
                .parallelExecution(false)
                .build();

        // When
        long startParallel = System.currentTimeMillis();
        LintingResult parallelResult = lintingEngine.lintProject(parallelContext);
        long parallelTime = System.currentTimeMillis() - startParallel;

        long startSequential = System.currentTimeMillis();
        LintingResult sequentialResult = lintingEngine.lintProject(sequentialContext);
        long sequentialTime = System.currentTimeMillis() - startSequential;

        // Then
        assertNotNull(parallelResult);
        assertNotNull(sequentialResult);

        // Both should process the same number of files
        assertEquals(parallelResult.getFilesProcessed(), sequentialResult.getFilesProcessed());

        // Parallel should not be significantly slower
        assertTrue(parallelTime <= sequentialTime * 2,
                "Parallel execution took too long: " + parallelTime + "ms vs " + sequentialTime + "ms");
    }

    // Helper methods for creating test projects

    private void createSampleJavaProject() throws Exception {
        // Create basic Java project structure
        Path srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);

        // Create sample Java files
        Files.writeString(srcDir.resolve("Application.java"),
                "package com.example;\n\npublic class Application {\n    public static void main(String[] args) {\n        System.out.println(\"Hello World\");\n    }\n}");

        Files.writeString(srcDir.resolve("Service.java"),
                "package com.example;\n\npublic class Service {\n    public void doSomething() {\n        // TODO: implement\n    }\n}");

        Files.writeString(srcDir.resolve("Controller.java"),
                "package com.example;\n\npublic class Controller {\n    private Service service;\n    \n    public void handleRequest() {\n        service.doSomething();\n    }\n}");
    }

    private void createSampleServiceProject(String serviceName) throws Exception {
        Path serviceDir = tempDir.resolve(serviceName);
        Files.createDirectories(serviceDir.resolve("src/main/java/com/zamaz/mcp/" + serviceName.replace("-", "")));

        Files.writeString(
                serviceDir.resolve("src/main/java/com/zamaz/mcp/" + serviceName.replace("-", "") + "/Application.java"),
                "package com.zamaz.mcp." + serviceName.replace("-", "")
                        + ";\n\n@SpringBootApplication\npublic class Application {\n    public static void main(String[] args) {\n        SpringApplication.run(Application.class, args);\n    }\n}");
    }

    private void createLargeJavaProject(int fileCount) throws Exception {
        Path srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);

        for (int i = 0; i < fileCount; i++) {
            Files.writeString(srcDir.resolve("Class" + i + ".java"),
                    "package com.example;\n\npublic class Class" + i + " {\n    public void method" + i
                            + "() {\n        // Method implementation\n    }\n}");
        }
    }

    private void createGlobalConfiguration() throws Exception {
        Path configDir = tempDir.resolve(".linting");
        Files.createDirectories(configDir);

        String globalConfig = """
                global:
                  parallel_execution: true
                  max_threads: 4
                  thresholds:
                    max_errors: 0
                    max_warnings: 10
                    min_coverage: 0.80
                    max_complexity: 10
                  exclude_patterns:
                    - "**/target/**"
                    - "**/build/**"
                    - "**/node_modules/**"

                linters:
                  checkstyle:
                    enabled: true
                    config_file: ".linting/java/checkstyle.xml"
                  spotbugs:
                    enabled: true
                    config_file: ".linting/java/spotbugs-exclude.xml"
                """;

        Files.writeString(configDir.resolve("global.yml"), globalConfig);
    }

    private void createServiceConfiguration(String serviceName) throws Exception {
        Path serviceConfigDir = tempDir.resolve(".linting/services/" + serviceName);
        Files.createDirectories(serviceConfigDir);

        String serviceConfig = """
                # Service-specific overrides
                global:
                  thresholds:
                    max_warnings: 15  # Allow more warnings for this service

                linters:
                  checkstyle:
                    properties:
                      max_line_length: 150
                """;

        Files.writeString(serviceConfigDir.resolve("config.yml"), serviceConfig);
    }
}
