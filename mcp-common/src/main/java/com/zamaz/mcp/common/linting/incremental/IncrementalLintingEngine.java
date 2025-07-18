package com.zamaz.mcp.common.linting.incremental;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.zamaz.mcp.common.linting.LintingContext;
import com.zamaz.mcp.common.linting.LintingEngine;
import com.zamaz.mcp.common.linting.LintingIssue;
import com.zamaz.mcp.common.linting.LintingResult;
import com.zamaz.mcp.common.linting.LintingSeverity;
import com.zamaz.mcp.common.linting.QualityThresholds;
import com.zamaz.mcp.common.linting.ReportFormat;

/**
 * Incremental linting engine that only processes changed files for improved
 * performance.
 */
@Component
public class IncrementalLintingEngine {

    private static final Logger logger = LoggerFactory.getLogger(IncrementalLintingEngine.class);

    private final LintingEngine baseLintingEngine;
    private final GitDiffAnalyzer gitDiffAnalyzer;
    private final LintingCache lintingCache;

    public IncrementalLintingEngine(LintingEngine baseLintingEngine) {
        this.baseLintingEngine = baseLintingEngine;
        this.gitDiffAnalyzer = new GitDiffAnalyzer();
        this.lintingCache = new LintingCache();
    }

    /**
     * Lint only files that have changed since the last commit.
     */
    public LintingResult lintChangedFiles(LintingContext context) {
        return lintChangedFiles(context, "HEAD~1", "HEAD");
    }

    /**
     * Lint only files that have changed between two commits.
     */
    public LintingResult lintChangedFiles(LintingContext context, String fromCommit, String toCommit) {
        logger.info("Running incremental linting from {} to {}", fromCommit, toCommit);
        long startTime = System.currentTimeMillis();

        try {
            // Get changed files from git diff
            Set<Path> changedFiles = gitDiffAnalyzer.getChangedFiles(
                    context.getProjectRoot(), fromCommit, toCommit);

            if (changedFiles.isEmpty()) {
                logger.info("No files changed, returning empty result");
                return createEmptyResult(startTime);
            }

            logger.info("Found {} changed files", changedFiles.size());

            // Filter files that need linting
            List<Path> filesToLint = changedFiles.stream()
                    .filter(file -> shouldLintFile(file, context))
                    .collect(Collectors.toList());

            if (filesToLint.isEmpty()) {
                logger.info("No lintable files changed, returning empty result");
                return createEmptyResult(startTime);
            }

            logger.info("Linting {} changed files", filesToLint.size());

            // Check cache for unchanged results
            List<Path> uncachedFiles = new ArrayList<>();
            List<LintingIssue> cachedIssues = new ArrayList<>();

            for (Path file : filesToLint) {
                Optional<List<LintingIssue>> cached = lintingCache.getCachedResult(file);
                if (cached.isPresent()) {
                    cachedIssues.addAll(cached.get());
                    logger.debug("Using cached result for {}", file);
                } else {
                    uncachedFiles.add(file);
                }
            }

            // Lint uncached files
            List<LintingIssue> newIssues = new ArrayList<>();
            if (!uncachedFiles.isEmpty()) {
                logger.info("Linting {} uncached files", uncachedFiles.size());
                LintingResult uncachedResult = baseLintingEngine.lintFiles(uncachedFiles, context);
                newIssues.addAll(uncachedResult.getIssues());

                // Cache the new results
                cacheResults(uncachedFiles, uncachedResult.getIssues());
            }

            // Combine cached and new results
            List<LintingIssue> allIssues = new ArrayList<>();
            allIssues.addAll(cachedIssues);
            allIssues.addAll(newIssues);

            long duration = System.currentTimeMillis() - startTime;

            // Create incremental result
            Map<String, Object> metrics = createIncrementalMetrics(
                    filesToLint.size(), cachedIssues.size(), newIssues.size(), changedFiles.size());

            return new IncrementalLintingResult(
                    allIssues,
                    metrics,
                    LocalDateTime.now(),
                    filesToLint.size(),
                    duration,
                    !hasBlockingIssues(allIssues, context),
                    changedFiles,
                    uncachedFiles.size(),
                    cachedIssues.size());

        } catch (Exception e) {
            logger.error("Error during incremental linting", e);
            long duration = System.currentTimeMillis() - startTime;

            Map<String, Object> errorMetrics = new HashMap<>();
            errorMetrics.put("error", e.getMessage());
            errorMetrics.put("incremental", true);

            return new IncrementalLintingResult(
                    Collections.emptyList(),
                    errorMetrics,
                    LocalDateTime.now(),
                    0,
                    duration,
                    false,
                    Collections.emptySet(),
                    0,
                    0);
        }
    }

    /**
     * Lint files changed in a specific commit range with caching.
     */
    public LintingResult lintCommitRange(LintingContext context, String commitRange) {
        String[] commits = commitRange.split("\\.\\.");
        if (commits.length != 2) {
            throw new IllegalArgumentException("Invalid commit range format. Use 'commit1..commit2'");
        }

        return lintChangedFiles(context, commits[0], commits[1]);
    }

    /**
     * Lint files in the current working directory that differ from the index.
     */
    public LintingResult lintWorkingDirectory(LintingContext context) {
        logger.info("Running incremental linting on working directory changes");

        try {
            Set<Path> changedFiles = gitDiffAnalyzer.getWorkingDirectoryChanges(context.getProjectRoot());

            if (changedFiles.isEmpty()) {
                return createEmptyResult(System.currentTimeMillis());
            }

            List<Path> filesToLint = changedFiles.stream()
                    .filter(file -> shouldLintFile(file, context))
                    .collect(Collectors.toList());

            if (filesToLint.isEmpty()) {
                return createEmptyResult(System.currentTimeMillis());
            }

            // For working directory changes, don't use cache as files might be modified
            return baseLintingEngine.lintFiles(filesToLint, context);

        } catch (Exception e) {
            logger.error("Error linting working directory changes", e);
            throw new RuntimeException("Failed to lint working directory changes", e);
        }
    }

    /**
     * Clear the linting cache.
     */
    public void clearCache() {
        lintingCache.clear();
        logger.info("Linting cache cleared");
    }

    /**
     * Get cache statistics.
     */
    public CacheStatistics getCacheStatistics() {
        return lintingCache.getStatistics();
    }

    /**
     * Warm up the cache by linting all files in the project.
     */
    public void warmUpCache(LintingContext context) {
        logger.info("Warming up linting cache");

        try {
            LintingResult result = baseLintingEngine.lintProject(context);

            // Cache results for all files
            // This is a simplified approach - in reality, we'd need to map issues to files
            Map<Path, List<LintingIssue>> issuesByFile = groupIssuesByFile(result.getIssues());

            for (Map.Entry<Path, List<LintingIssue>> entry : issuesByFile.entrySet()) {
                lintingCache.cacheResult(entry.getKey(), entry.getValue());
            }

            logger.info("Cache warmed up with {} files", issuesByFile.size());

        } catch (Exception e) {
            logger.error("Error warming up cache", e);
        }
    }

    private boolean shouldLintFile(Path file, LintingContext context) {
        if (!Files.exists(file)) {
            return false;
        }

        String fileName = file.getFileName().toString();
        String extension = getFileExtension(fileName);

        // Check if we have linters for this file type
        List<String> availableLinters = baseLintingEngine.getAvailableLinters(extension);
        if (availableLinters.isEmpty()) {
            return false;
        }

        // Check exclude patterns
        if (context.getExcludePatterns() != null) {
            String filePath = file.toString();
            for (String pattern : context.getExcludePatterns()) {
                if (filePath.matches(pattern.replace("*", ".*"))) {
                    return false;
                }
            }
        }

        return true;
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
    }

    private void cacheResults(List<Path> files, List<LintingIssue> issues) {
        Map<Path, List<LintingIssue>> issuesByFile = groupIssuesByFile(issues);

        // Cache results for each file
        for (Path file : files) {
            List<LintingIssue> fileIssues = issuesByFile.getOrDefault(file, Collections.emptyList());
            lintingCache.cacheResult(file, fileIssues);
        }
    }

    private Map<Path, List<LintingIssue>> groupIssuesByFile(List<LintingIssue> issues) {
        return issues.stream()
                .collect(Collectors.groupingBy(issue -> Path.of(issue.getFile())));
    }

    private boolean hasBlockingIssues(List<LintingIssue> issues, LintingContext context) {
        QualityThresholds thresholds = context.getConfiguration().getThresholds();
        if (thresholds == null) {
            return false;
        }

        long errorCount = issues.stream().filter(i -> i.getSeverity() == LintingSeverity.ERROR).count();
        long warningCount = issues.stream().filter(i -> i.getSeverity() == LintingSeverity.WARNING).count();

        return errorCount > thresholds.getMaxErrors() || warningCount > thresholds.getMaxWarnings();
    }

    private Map<String, Object> createIncrementalMetrics(int totalFiles, int cachedIssues,
            int newIssues, int totalChangedFiles) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("incremental", true);
        metrics.put("totalFiles", totalFiles);
        metrics.put("totalChangedFiles", totalChangedFiles);
        metrics.put("cachedIssues", cachedIssues);
        metrics.put("newIssues", newIssues);
        metrics.put("totalIssues", cachedIssues + newIssues);
        metrics.put("cacheHitRate", totalFiles > 0 ? (double) (totalFiles - totalChangedFiles) / totalFiles : 0.0);
        return metrics;
    }

    private LintingResult createEmptyResult(long startTime) {
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("incremental", true);
        metrics.put("totalFiles", 0);
        metrics.put("totalIssues", 0);

        return new IncrementalLintingResult(
                Collections.emptyList(),
                metrics,
                LocalDateTime.now(),
                0,
                duration,
                true,
                Collections.emptySet(),
                0,
                0);
    }

    /**
     * Extended LintingResult with incremental-specific information.
     */
    public static class IncrementalLintingResult implements LintingResult {
        private final LintingResult baseResult;
        private final Set<Path> changedFiles;
        private final int uncachedFileCount;
        private final int cachedIssueCount;

        public IncrementalLintingResult(List<LintingIssue> issues, Map<String, Object> metrics,
                LocalDateTime timestamp, int filesProcessed, long durationMs,
                boolean successful, Set<Path> changedFiles,
                int uncachedFileCount, int cachedIssueCount) {
            this.baseResult = new com.zamaz.mcp.common.linting.impl.DefaultLintingResult(
                    issues, metrics, timestamp, filesProcessed, durationMs, successful);
            this.changedFiles = changedFiles;
            this.uncachedFileCount = uncachedFileCount;
            this.cachedIssueCount = cachedIssueCount;
        }

        // Delegate to base result
        @Override
        public boolean hasErrors() {
            return baseResult.hasErrors();
        }

        @Override
        public boolean hasWarnings() {
            return baseResult.hasWarnings();
        }

        @Override
        public List<LintingIssue> getIssues() {
            return baseResult.getIssues();
        }

        @Override
        public List<LintingIssue> getIssuesBySeverity(LintingSeverity severity) {
            return baseResult.getIssuesBySeverity(severity);
        }

        @Override
        public Map<String, Object> getMetrics() {
            return baseResult.getMetrics();
        }

        @Override
        public String generateReport(ReportFormat format) {
            return baseResult.generateReport(format);
        }

        @Override
        public LocalDateTime getTimestamp() {
            return baseResult.getTimestamp();
        }

        @Override
        public int getFilesProcessed() {
            return baseResult.getFilesProcessed();
        }

        @Override
        public long getDurationMs() {
            return baseResult.getDurationMs();
        }

        @Override
        public boolean isSuccessful() {
            return baseResult.isSuccessful();
        }

        // Incremental-specific methods
        public Set<Path> getChangedFiles() {
            return changedFiles;
        }

        public int getUncachedFileCount() {
            return uncachedFileCount;
        }

        public int getCachedIssueCount() {
            return cachedIssueCount;
        }

        public boolean isIncremental() {
            return true;
        }
    }
}
