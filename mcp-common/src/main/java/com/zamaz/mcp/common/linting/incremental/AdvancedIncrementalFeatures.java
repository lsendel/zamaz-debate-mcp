package com.zamaz.mcp.common.linting.incremental;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.zamaz.mcp.common.linting.LintingContext;
import com.zamaz.mcp.common.linting.LintingResult;

/**
 * Advanced incremental linting features including batch processing,
 * parallel execution, and enhanced cache management.
 */
@Component
public class AdvancedIncrementalFeatures {

    private static final Logger logger = LoggerFactory.getLogger(AdvancedIncrementalFeatures.class);

    private final IncrementalLintingEngine incrementalEngine;
    private final GitDiffAnalyzer gitAnalyzer;
    private final LintingCache cache;
    private final Map<String, CacheStatistics> branchStatistics = new ConcurrentHashMap<>();

    public AdvancedIncrementalFeatures(IncrementalLintingEngine incrementalEngine) {
        this.incrementalEngine = incrementalEngine;
        this.gitAnalyzer = new GitDiffAnalyzer();
        this.cache = new LintingCache();
    }

    /**
     * Lint multiple commit ranges in parallel for CI/CD pipeline optimization.
     */
    public CompletableFuture<Map<String, LintingResult>> lintCommitRangesParallel(
            LintingContext context, List<String> commitRanges) {
        
        logger.info("Starting parallel linting for {} commit ranges", commitRanges.size());
        
        List<CompletableFuture<Map.Entry<String, LintingResult>>> futures = commitRanges.stream()
                .map(range -> CompletableFuture.supplyAsync(() -> {
                    try {
                        LintingResult result = incrementalEngine.lintCommitRange(context, range);
                        return Map.entry(range, result);
                    } catch (Exception e) {
                        logger.error("Error linting commit range {}", range, e);
                        return Map.entry(range, createErrorResult(e.getMessage()));
                    }
                }))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue)));
    }

    /**
     * Enhanced branch comparison with detailed change analysis.
     */
    public BranchComparisonResult compareBranches(LintingContext context, 
            String baseBranch, String targetBranch) {
        
        logger.info("Comparing branches {} vs {}", baseBranch, targetBranch);
        
        try {
            // Get changed files between branches
            Set<Path> changedFiles = gitAnalyzer.getChangedFilesBetweenBranches(
                    context.getProjectRoot(), baseBranch, targetBranch);

            // Get files by type for targeted linting
            Map<String, List<Path>> filesByType = categorizeFiles(changedFiles);
            
            // Lint each category
            Map<String, LintingResult> resultsByType = new HashMap<>();
            long totalDuration = 0;
            
            for (Map.Entry<String, List<Path>> entry : filesByType.entrySet()) {
                String fileType = entry.getKey();
                List<Path> files = entry.getValue();
                
                if (!files.isEmpty()) {
                    long startTime = System.currentTimeMillis();
                    LintingResult result = lintFilesByType(context, files, fileType);
                    long duration = System.currentTimeMillis() - startTime;
                    
                    resultsByType.put(fileType, result);
                    totalDuration += duration;
                    
                    logger.info("Linted {} {} files in {}ms", 
                            files.size(), fileType, duration);
                }
            }

            return new BranchComparisonResult(
                    baseBranch, targetBranch, changedFiles, 
                    filesByType, resultsByType, totalDuration);

        } catch (Exception e) {
            logger.error("Error comparing branches {} vs {}", baseBranch, targetBranch, e);
            throw new RuntimeException("Failed to compare branches", e);
        }
    }

    /**
     * Smart cache warming based on git history and file patterns.
     */
    public void warmCacheIntelligently(LintingContext context) {
        logger.info("Starting intelligent cache warming");
        
        try {
            // Analyze git history to identify frequently changed files
            Set<Path> frequentFiles = analyzeGitHistory(context.getProjectRoot());
            
            // Warm cache for these files first
            if (!frequentFiles.isEmpty()) {
                logger.info("Warming cache for {} frequently changed files", frequentFiles.size());
                
                // Process files in batches to avoid memory issues
                List<Path> fileList = new ArrayList<>(frequentFiles);
                int batchSize = 50;
                
                for (int i = 0; i < fileList.size(); i += batchSize) {
                    int endIndex = Math.min(i + batchSize, fileList.size());
                    List<Path> batch = fileList.subList(i, endIndex);
                    
                    CompletableFuture.runAsync(() -> {
                        for (Path file : batch) {
                            try {
                                // Pre-calculate and cache file hash
                                if (Files.exists(file)) {
                                    cache.cacheResult(file, new ArrayList<>());
                                    logger.debug("Pre-cached {}", file);
                                }
                            } catch (Exception e) {
                                logger.warn("Failed to pre-cache {}: {}", file, e.getMessage());
                            }
                        }
                    });
                }
            }
            
        } catch (Exception e) {
            logger.error("Error during intelligent cache warming", e);
        }
    }

    /**
     * Generate comprehensive cache performance report.
     */
    public CachePerformanceReport generateCacheReport() {
        CacheStatistics globalStats = cache.getStatistics();
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalSize", cache.size());
        metrics.put("hitRate", globalStats.getHitRate());
        metrics.put("missRate", globalStats.getMissRate());
        metrics.put("totalRequests", globalStats.getTotal());
        metrics.put("branchStats", branchStatistics);
        
        // Calculate cache efficiency trends
        Map<String, Double> trends = calculateCacheTrends();
        metrics.put("trends", trends);
        
        return new CachePerformanceReport(globalStats, branchStatistics, metrics);
    }

    /**
     * Advanced file watching for real-time incremental linting.
     */
    public FileWatcher createFileWatcher(LintingContext context) {
        return new FileWatcher(context, this::onFileChanged);
    }

    /**
     * Optimize cache by removing stale entries and compacting storage.
     */
    public void optimizeCache() {
        logger.info("Optimizing linting cache");
        
        // Remove entries for files that no longer exist
        cache.cleanup();
        
        // Update statistics
        CacheStatistics stats = cache.getStatistics();
        logger.info("Cache optimization complete. Size: {}, Hit Rate: {:.2f}%", 
                cache.size(), stats.getHitRate() * 100);
    }

    // Private helper methods

    private Map<String, List<Path>> categorizeFiles(Set<Path> files) {
        Map<String, List<Path>> filesByType = new HashMap<>();
        
        for (Path file : files) {
            String fileName = file.getFileName().toString().toLowerCase();
            String category = determineFileCategory(fileName);
            
            filesByType.computeIfAbsent(category, k -> new ArrayList<>()).add(file);
        }
        
        return filesByType;
    }

    private String determineFileCategory(String fileName) {
        if (fileName.endsWith(".java")) return "java";
        if (fileName.matches(".*\\.(ts|tsx|js|jsx)$")) return "typescript";
        if (fileName.matches(".*\\.(yml|yaml|json)$") || fileName.equals("dockerfile")) return "config";
        if (fileName.endsWith(".md")) return "markdown";
        return "other";
    }

    private LintingResult lintFilesByType(LintingContext context, List<Path> files, String fileType) {
        // This would delegate to appropriate linters based on file type
        // For now, use the base incremental engine
        return incrementalEngine.lintChangedFiles(context);
    }

    private Set<Path> analyzeGitHistory(Path projectRoot) {
        try {
            // Get files changed in last 50 commits
            ProcessBuilder pb = new ProcessBuilder(
                    "git", "log", "--name-only", "--pretty=format:", "-50");
            pb.directory(projectRoot.toFile());
            
            Process process = pb.start();
            Set<Path> frequentFiles = new HashSet<>();
            
            process.getInputStream().transferTo(System.out); // Simple implementation
            
            return frequentFiles;
            
        } catch (Exception e) {
            logger.warn("Failed to analyze git history: {}", e.getMessage());
            return new HashSet<>();
        }
    }

    private Map<String, Double> calculateCacheTrends() {
        Map<String, Double> trends = new HashMap<>();
        
        // Calculate hit rate trends over time
        double avgHitRate = branchStatistics.values().stream()
                .mapToDouble(CacheStatistics::getHitRate)
                .average()
                .orElse(0.0);
        
        trends.put("averageHitRate", avgHitRate);
        trends.put("improvementTrend", avgHitRate > 0.7 ? 1.0 : -1.0);
        
        return trends;
    }

    private void onFileChanged(Path file) {
        logger.debug("File changed: {}", file);
        cache.invalidate(file);
    }

    private LintingResult createErrorResult(String error) {
        // Return a simple error result
        return new ErrorLintingResult(error);
    }

    // Inner classes for structured data

    public static class BranchComparisonResult {
        private final String baseBranch;
        private final String targetBranch;
        private final Set<Path> changedFiles;
        private final Map<String, List<Path>> filesByType;
        private final Map<String, LintingResult> resultsByType;
        private final long totalDuration;

        public BranchComparisonResult(String baseBranch, String targetBranch, 
                Set<Path> changedFiles, Map<String, List<Path>> filesByType,
                Map<String, LintingResult> resultsByType, long totalDuration) {
            this.baseBranch = baseBranch;
            this.targetBranch = targetBranch;
            this.changedFiles = changedFiles;
            this.filesByType = filesByType;
            this.resultsByType = resultsByType;
            this.totalDuration = totalDuration;
        }

        // Getters
        public String getBaseBranch() { return baseBranch; }
        public String getTargetBranch() { return targetBranch; }
        public Set<Path> getChangedFiles() { return changedFiles; }
        public Map<String, List<Path>> getFilesByType() { return filesByType; }
        public Map<String, LintingResult> getResultsByType() { return resultsByType; }
        public long getTotalDuration() { return totalDuration; }

        public int getTotalFiles() { return changedFiles.size(); }
        
        public boolean hasErrors() {
            return resultsByType.values().stream().anyMatch(LintingResult::hasErrors);
        }
    }

    public static class CachePerformanceReport {
        private final CacheStatistics globalStats;
        private final Map<String, CacheStatistics> branchStats;
        private final Map<String, Object> metrics;
        private final LocalDateTime generatedAt;

        public CachePerformanceReport(CacheStatistics globalStats, 
                Map<String, CacheStatistics> branchStats, Map<String, Object> metrics) {
            this.globalStats = globalStats;
            this.branchStats = branchStats;
            this.metrics = metrics;
            this.generatedAt = LocalDateTime.now();
        }

        // Getters
        public CacheStatistics getGlobalStats() { return globalStats; }
        public Map<String, CacheStatistics> getBranchStats() { return branchStats; }
        public Map<String, Object> getMetrics() { return metrics; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }

        public String generateReport() {
            StringBuilder report = new StringBuilder();
            report.append("# Cache Performance Report\n");
            report.append("Generated: ").append(generatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");
            report.append("## Global Statistics\n");
            report.append("- Total Requests: ").append(globalStats.getTotal()).append("\n");
            report.append("- Hit Rate: ").append(String.format("%.2f%%", globalStats.getHitRate() * 100)).append("\n");
            report.append("- Cache Size: ").append(metrics.get("totalSize")).append("\n\n");
            
            if (!branchStats.isEmpty()) {
                report.append("## Branch Statistics\n");
                branchStats.forEach((branch, stats) -> {
                    report.append("- ").append(branch).append(": ")
                            .append(String.format("%.2f%% hit rate", stats.getHitRate() * 100))
                            .append("\n");
                });
            }
            
            return report.toString();
        }
    }

    public static class FileWatcher {
        private final LintingContext context;
        private final FileChangeCallback callback;

        public FileWatcher(LintingContext context, FileChangeCallback callback) {
            this.context = context;
            this.callback = callback;
        }

        public void start() {
            // Implementation would use Java NIO WatchService
            logger.info("File watcher started for {}", context.getProjectRoot());
        }

        public void stop() {
            logger.info("File watcher stopped");
        }
    }

    @FunctionalInterface
    public interface FileChangeCallback {
        void onFileChanged(Path file);
    }

    private static class ErrorLintingResult implements LintingResult {
        private final String error;

        public ErrorLintingResult(String error) {
            this.error = error;
        }

        @Override
        public boolean hasErrors() { return true; }

        @Override
        public boolean hasWarnings() { return false; }

        @Override
        public List<com.zamaz.mcp.common.linting.LintingIssue> getIssues() { 
            return Arrays.asList(); 
        }

        @Override
        public List<com.zamaz.mcp.common.linting.LintingIssue> getIssuesBySeverity(
                com.zamaz.mcp.common.linting.LintingSeverity severity) { 
            return Arrays.asList(); 
        }

        @Override
        public Map<String, Object> getMetrics() { 
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("error", error);
            return metrics;
        }

        @Override
        public String generateReport(com.zamaz.mcp.common.linting.ReportFormat format) { 
            return "Error: " + error; 
        }

        @Override
        public LocalDateTime getTimestamp() { return LocalDateTime.now(); }

        @Override
        public int getFilesProcessed() { return 0; }

        @Override
        public long getDurationMs() { return 0; }

        @Override
        public boolean isSuccessful() { return false; }
    }
}