package com.zamaz.mcp.common.linting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.zamaz.mcp.common.linting.impl.LintingEngineImpl;

/**
 * Unit tests for LintingEngine implementation.
 */
class LintingEngineTest {

    @TempDir
    Path tempDir;

    private LintingEngine lintingEngine;

    @Mock
    private LintingConfiguration mockConfiguration;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        lintingEngine = new LintingEngineImpl();

        // Setup mock configuration
        when(mockConfiguration.getThresholds()).thenReturn(new QualityThresholds());
        when(mockConfiguration.getExcludePatterns()).thenReturn(Collections.emptyList());
        when(mockConfiguration.isParallelExecution()).thenReturn(true);
        when(mockConfiguration.getMaxThreads()).thenReturn(4);
    }

    @Test
    void testLintProject_EmptyProject() throws Exception {
        // Given
        LintingContext context = createTestContext(tempDir);

        // When
        LintingResult result = lintingEngine.lintProject(context);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertEquals(0, result.getFilesProcessed());
        assertEquals(0, result.getIssues().size());
        assertNotNull(result.getTimestamp());
        assertTrue(result.getDurationMs() >= 0);
    }

    @Test
    void testLintProject_WithJavaFiles() throws Exception {
        // Given
        createTestJavaFile(tempDir, "TestClass.java", "public class TestClass { }");
        LintingContext context = createTestContext(tempDir);

        // When
        LintingResult result = lintingEngine.lintProject(context);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertTrue(result.getFilesProcessed() >= 0);
        assertNotNull(result.getIssues());
    }

    @Test
    void testLintService_ExistingService() throws Exception {
        // Given
        Path serviceDir = tempDir.resolve("mcp-test-service");
        Files.createDirectories(serviceDir);
        createTestJavaFile(serviceDir, "ServiceClass.java", "public class ServiceClass { }");

        LintingContext context = createTestContext(tempDir);

        // When
        LintingResult result = lintingEngine.lintService("mcp-test-service", context);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccessful());
    }

    @Test
    void testLintService_NonExistentService() throws Exception {
        // Given
        LintingContext context = createTestContext(tempDir);

        // When
        LintingResult result = lintingEngine.lintService("non-existent-service", context);

        // Then
        assertNotNull(result);
        // Should handle gracefully
    }

    @Test
    void testLintFiles_SpecificFiles() throws Exception {
        // Given
        Path javaFile = createTestJavaFile(tempDir, "TestFile.java", "public class TestFile { }");
        List<Path> files = Arrays.asList(javaFile);
        LintingContext context = createTestContext(tempDir);

        // When
        LintingResult result = lintingEngine.lintFiles(files, context);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertEquals(1, result.getFilesProcessed());
    }

    @Test
    void testLintFiles_NonExistentFiles() throws Exception {
        // Given
        Path nonExistentFile = tempDir.resolve("non-existent.java");
        List<Path> files = Arrays.asList(nonExistentFile);
        LintingContext context = createTestContext(tempDir);

        // When
        LintingResult result = lintingEngine.lintFiles(files, context);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertEquals(0, result.getFilesProcessed());
    }

    @Test
    void testGetAvailableLinters_JavaFiles() {
        // When
        List<String> linters = lintingEngine.getAvailableLinters("java");

        // Then
        assertNotNull(linters);
        assertFalse(linters.isEmpty());
        assertTrue(linters.contains("checkstyle"));
        assertTrue(linters.contains("spotbugs"));
        assertTrue(linters.contains("pmd"));
    }

    @Test
    void testGetAvailableLinters_TypeScriptFiles() {
        // When
        List<String> linters = lintingEngine.getAvailableLinters("ts");

        // Then
        assertNotNull(linters);
        assertFalse(linters.isEmpty());
        assertTrue(linters.contains("eslint"));
        assertTrue(linters.contains("prettier"));
    }

    @Test
    void testGetAvailableLinters_UnknownExtension() {
        // When
        List<String> linters = lintingEngine.getAvailableLinters("unknown");

        // Then
        assertNotNull(linters);
        assertTrue(linters.isEmpty());
    }

    @Test
    void testLintingResult_IssueFiltering() throws Exception {
        // Given
        LintingContext context = createTestContext(tempDir);

        // When
        LintingResult result = lintingEngine.lintProject(context);

        // Then
        assertNotNull(result.getIssuesBySeverity(LintingSeverity.ERROR));
        assertNotNull(result.getIssuesBySeverity(LintingSeverity.WARNING));
        assertNotNull(result.getIssuesBySeverity(LintingSeverity.INFO));
        assertNotNull(result.getIssuesBySeverity(LintingSeverity.SUGGESTION));
    }

    @Test
    void testLintingResult_ReportGeneration() throws Exception {
        // Given
        LintingContext context = createTestContext(tempDir);
        LintingResult result = lintingEngine.lintProject(context);

        // When & Then
        assertNotNull(result.generateReport(ReportFormat.CONSOLE));
        assertNotNull(result.generateReport(ReportFormat.JSON));
        assertNotNull(result.generateReport(ReportFormat.HTML));
        assertNotNull(result.generateReport(ReportFormat.XML));
        assertNotNull(result.generateReport(ReportFormat.MARKDOWN));
    }

    @Test
    void testParallelExecution() throws Exception {
        // Given
        for (int i = 0; i < 10; i++) {
            createTestJavaFile(tempDir, "TestClass" + i + ".java",
                    "public class TestClass" + i + " { }");
        }

        LintingContext parallelContext = LintingContext.builder()
                .projectRoot(tempDir)
                .configuration(mockConfiguration)
                .parallelExecution(true)
                .build();

        LintingContext sequentialContext = LintingContext.builder()
                .projectRoot(tempDir)
                .configuration(mockConfiguration)
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
        assertTrue(parallelResult.isSuccessful());
        assertTrue(sequentialResult.isSuccessful());

        // Parallel should generally be faster or at least not significantly slower
        // (This is a rough test and might be flaky in some environments)
        assertTrue(parallelTime <= sequentialTime * 2,
                "Parallel execution took too long compared to sequential");
    }

    private LintingContext createTestContext(Path projectRoot) {
        return LintingContext.builder()
                .projectRoot(projectRoot)
                .configuration(mockConfiguration)
                .excludePatterns(Collections.emptyList())
                .parallelExecution(true)
                .autoFix(false)
                .build();
    }

    private Path createTestJavaFile(Path directory, String fileName, String content) throws Exception {
        Path file = directory.resolve(fileName);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }
}
