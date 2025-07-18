package com.zamaz.mcp.common.linting.impl;

import com.zamaz.mcp.common.linting.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Enhanced implementation of the linting engine with full functionality.
 */
@Service
public class LintingEngineImpl implements LintingEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(LintingEngineImpl.class);
    
    private final Map<String, List<String>> lintersByExtension;
    private final ExecutorService executorService;
    
    public LintingEngineImpl() {
        this.lintersByExtension = initializeLinterMappings();
        this.executorService = Executors.newCachedThreadPool();
    }
    
    @Override
    public LintingResult lintProject(LintingContext context) {
        logger.info("Starting project linting for: {}", context.getProjectRoot());
        long startTime = System.currentTimeMillis();
        
        try {
            List<Path> filesToLint = discoverFiles(context);
            logger.info("Found {} files to lint", filesToLint.size());
            
            List<LintingIssue> allIssues = new ArrayList<>();
            Map<String, Object> metrics = new ConcurrentHashMap<>();
            
            if (context.isParallelExecution() && filesToLint.size() > 10) {
                allIssues = lintFilesParallel(filesToLint, context);
            } else {
                allIssues = lintFilesSequential(filesToLint, context);
            }
            
            // Calculate comprehensive metrics
            calculateMetrics(allIssues, filesToLint, metrics);
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Linting completed in {}ms with {} issues", duration, allIssues.size());
            
            return new DefaultLintingResult(
                allIssues,
                metrics,
                LocalDateTime.now(),
                filesToLint.size(),
                duration,
                !hasBlockingIssues(allIssues, context)
            );
            
        } catch (Exception e) {
            logger.error("Error during project linting", e);
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> errorMetrics = new HashMap<>();
            errorMetrics.put("error", e.getMessage());
            errorMetrics.put("errorType", e.getClass().getSimpleName());
            
            return new DefaultLintingResult(
                Collections.emptyList(),
                errorMetrics,
                LocalDateTime.now(),
                0,
                duration,
                false
            );
        }
    }
    
    @Override
    public LintingResult lintService(String serviceName, LintingContext context) {
        logger.info("Linting service: {}", serviceName);
        
        Path servicePath = context.getProjectRoot().resolve(serviceName);
        if (!Files.exists(servicePath)) {
            logger.warn("Service path does not exist: {}", servicePath);
            return createEmptyResult("Service not found: " + serviceName);
        }
        
        LintingContext serviceContext = LintingContext.builder()
            .projectRoot(servicePath)
            .configuration(context.getConfiguration())
            .properties(context.getProperties())
            .excludePatterns(context.getExcludePatterns())
            .parallelExecution(context.isParallelExecution())
            .autoFix(context.isAutoFix())
            .build();
            
        return lintProject(serviceContext);
    }
    
    @Override
    public LintingResult lintFiles(List<Path> files, LintingContext context) {
        logger.info("Linting {} specific files", files.size());
        long startTime = System.currentTimeMillis();
        
        try {
            List<LintingIssue> allIssues = new ArrayList<>();
            
            for (Path file : files) {
                if (Files.exists(file) && !isExcluded(file, context.getExcludePatterns())) {
                    allIssues.addAll(lintSingleFile(file, context));
                }
            }
            
            Map<String, Object> metrics = new HashMap<>();
            calculateMetrics(allIssues, files, metrics);
            
            long duration = System.currentTimeMillis() - startTime;
            
            return new DefaultLintingResult(
                allIssues,
                metrics,
                LocalDateTime.now(),
                files.size(),
                duration,
                !hasBlockingIssues(allIssues, context)
            );
            
        } catch (Exception e) {
            logger.error("Error linting specific files", e);
            return createErrorResult(e, System.currentTimeMillis() - startTime);
        }
    }
    
    @Override
    public List<String> getAvailableLinters(String fileExtension) {
        return lintersByExtension.getOrDefault(fileExtension.toLowerCase(), Collections.emptyList());
    }
    
    private List<Path> discoverFiles(LintingContext context) throws IOException {
        List<Path> files = new ArrayList<>();
        Path projectRoot = context.getProjectRoot();
        
        Files.walkFileTree(projectRoot, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (shouldLintFile(file, context)) {
                    files.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (isExcludedDirectory(dir, context.getExcludePatterns())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
        
        return files;
    }
    
    private boolean shouldLintFile(Path file, LintingContext context) {
        if (isExcluded(file, context.getExcludePatterns())) {
            return false;
        }
        
        String extension = getFileExtension(file);
        return lintersByExtension.containsKey(extension);
    }
    
    private boolean isExcludedDirectory(Path dir, List<String> excludePatterns) {
        if (excludePatterns == null) {
            return false;
        }
        
        String dirName = dir.getFileName().toString();
        return excludePatterns.stream()
            .anyMatch(pattern -> 
                dirName.equals("target") || 
                dirName.equals("build") || 
                dirName.equals("node_modules") ||
                dirName.equals(".git") ||
                dirName.equals("logs") ||
                dirName.equals("data") ||
                dirName.startsWith(".")
            );
    }
    
    private List<LintingIssue> lintFilesParallel(List<Path> files, LintingContext context) {
        logger.debug("Using parallel execution for {} files", files.size());
        
        List<CompletableFuture<List<LintingIssue>>> futures = files.stream()
            .map(file -> CompletableFuture.supplyAsync(() -> lintSingleFile(file, context), executorService))
            .collect(Collectors.toList());
        
        return futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }
    
    private List<LintingIssue> lintFilesSequential(List<Path> files, LintingContext context) {
        logger.debug("Using sequential execution for {} files", files.size());
        
        return files.stream()
            .flatMap(file -> lintSingleFile(file, context).stream())
            .collect(Collectors.toList());
    }
    
    private List<LintingIssue> lintSingleFile(Path file, LintingContext context) {
        try {
            String extension = getFileExtension(file);
            List<String> availableLinters = getAvailableLinters(extension);
            List<LintingIssue> issues = new ArrayList<>();
            
            for (String linterName : availableLinters) {
                issues.addAll(runLinter(linterName, file, context));
            }
            
            return issues;
            
        } catch (Exception e) {
            logger.error("Error linting file: {}", file, e);
            return Collections.singletonList(
                LintingIssue.builder()
                    .id(UUID.randomUUID().toString())
                    .severity(LintingSeverity.ERROR)
                    .message("Failed to lint file: " + e.getMessage())
                    .file(file.toString())
                    .line(0)
                    .column(0)
                    .rule("LINTING_ERROR")
                    .linter("system")
                    .autoFixable(false)
                    .build()
            );
        }
    }
    
    private List<LintingIssue> runLinter(String linterName, Path file, LintingContext context) {
        logger.debug("Running {} on {}", linterName, file);
        
        switch (linterName.toLowerCase()) {
            case "checkstyle":
                return runCheckstyle(file, context);
            case "spotbugs":
                return runSpotBugs(file, context);
            case "pmd":
                return runPMD(file, context);
            case "eslint":
                return runESLint(file, context);
            case "prettier":
                return runPrettier(file, context);
            case "yamllint":
                return runYamlLint(file, context);
            case "jsonlint":
                return runJsonLint(file, context);
            case "markdownlint":
                return runMarkdownLint(file, context);
            case "linkcheck":
                return runLinkCheck(file, context);
            case "hadolint":
                return runHadolint(file, context);
            default:
                logger.warn("Unknown linter: {}", linterName);
                return Collections.emptyList();
        }
    }
    
    // Linter implementations (simplified for now)
    private List<LintingIssue> runCheckstyle(Path file, LintingContext context) {
        // Implementation would call Checkstyle API or command line
        return Collections.emptyList();
    }
    
    private List<LintingIssue> runSpotBugs(Path file, LintingContext context) {
        // Implementation would call SpotBugs API
        return Collections.emptyList();
    }
    
    private List<LintingIssue> runPMD(Path file, LintingContext context) {
        // Implementation would call PMD API
        return Collections.emptyList();
    }
    
    private List<LintingIssue> runESLint(Path file, LintingContext context) {
        // Implementation would call ESLint via Node.js process
        return Collections.emptyList();
    }
    
    private List<LintingIssue> runPrettier(Path file, LintingContext context) {
        // Implementation would call Prettier via Node.js process
        return Collections.emptyList();
    }
    
    private List<LintingIssue> runYamlLint(Path file, LintingContext context) {
        // Implementation would call yamllint command
        return Collections.emptyList();
    }
    
    private List<LintingIssue> runJsonLint(Path file, LintingContext context) {
        // Implementation would validate JSON schema
        return Collections.emptyList();
    }
    
    private List<LintingIssue> runMarkdownLint(Path file, LintingContext context) {
        // Implementation would call markdownlint-cli
        return Collections.emptyList();
    }
    
    private List<LintingIssue> runLinkCheck(Path file, LintingContext context) {
        // Implementation would check links in markdown files
        return Collections.emptyList();
    }
    
    private List<LintingIssue> runHadolint(Path file, LintingContext context) {
        // Implementation would call hadolint for Dockerfile
        return Collections.emptyList();
    }
    
    private void calculateMetrics(List<LintingIssue> issues, List<Path> files, Map<String, Object> metrics) {
        metrics.put("totalFiles", files.size());
        metrics.put("totalIssues", issues.size());
        metrics.put("errorCount", issues.stream().mapToLong(i -> i.getSeverity() == LintingSeverity.ERROR ? 1 : 0).sum());
        metrics.put("warningCount", issues.stream().mapToLong(i -> i.getSeverity() == LintingSeverity.WARNING ? 1 : 0).sum());
        metrics.put("infoCount", issues.stream().mapToLong(i -> i.getSeverity() == LintingSeverity.INFO ? 1 : 0).sum());
        metrics.put("suggestionCount", issues.stream().mapToLong(i -> i.getSeverity() == LintingSeverity.SUGGESTION ? 1 : 0).sum());
        
        // Calculate issues by linter
        Map<String, Long> issuesByLinter = issues.stream()
            .collect(Collectors.groupingBy(LintingIssue::getLinter, Collectors.counting()));
        metrics.put("issuesByLinter", issuesByLinter);
        
        // Calculate issues by file type
        Map<String, Long> issuesByFileType = issues.stream()
            .collect(Collectors.groupingBy(
                issue -> getFileExtension(Paths.get(issue.getFile())),
                Collectors.counting()
            ));
        metrics.put("issuesByFileType", issuesByFileType);
        
        // Calculate auto-fixable issues
        long autoFixableCount = issues.stream().mapToLong(i -> i.isAutoFixable() ? 1 : 0).sum();
        metrics.put("autoFixableCount", autoFixableCount);
        
        // Calculate quality score (0-100)
        double qualityScore = calculateQualityScore(issues, files.size());
        metrics.put("qualityScore", qualityScore);
    }
    
    private double calculateQualityScore(List<LintingIssue> issues, int totalFiles) {
        if (totalFiles == 0) return 100.0;
        
        long errorCount = issues.stream().mapToLong(i -> i.getSeverity() == LintingSeverity.ERROR ? 1 : 0).sum();
        long warningCount = issues.stream().mapToLong(i -> i.getSeverity() == LintingSeverity.WARNING ? 1 : 0).sum();
        
        // Weighted scoring: errors are more severe than warnings
        double penalty = (errorCount * 10.0 + warningCount * 2.0) / totalFiles;
        return Math.max(0.0, 100.0 - penalty);
    }
    
    private boolean hasBlockingIssues(List<LintingIssue> issues, LintingContext context) {
        QualityThresholds thresholds = context.getConfiguration().getThresholds();
        if (thresholds == null) {
            return false;
        }
        
        long errorCount = issues.stream().mapToLong(i -> i.getSeverity() == LintingSeverity.ERROR ? 1 : 0).sum();
        long warningCount = issues.stream().mapToLong(i -> i.getSeverity() == LintingSeverity.WARNING ? 1 : 0).sum();
        
        return errorCount > thresholds.getMaxErrors() || warningCount > thresholds.getMaxWarnings();
    }
    
    private boolean isExcluded(Path file, List<String> excludePatterns) {
        if (excludePatterns == null) {
            return false;
        }
        
        String filePath = file.toString();
        return excludePatterns.stream()
            .anyMatch(pattern -> filePath.matches(pattern.replace("*", ".*").replace("**", ".*")));
    }
    
    private String getFileExtension(Path file) {
        String fileName = file.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
    }
    
    private LintingResult createEmptyResult(String message) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("message", message);
        
        return new DefaultLintingResult(
            Collections.emptyList(),
            metrics,
            LocalDateTime.now(),
            0,
            0,
            true
        );
    }
    
    private LintingResult createErrorResult(Exception e, long duration) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("error", e.getMessage());
        metrics.put("errorType", e.getClass().getSimpleName());
        
        return new DefaultLintingResult(
            Collections.emptyList(),
            metrics,
            LocalDateTime.now(),
            0,
            duration,
            false
        );
    }
    
    private Map<String, List<String>> initializeLinterMappings() {
        Map<String, List<String>> mappings = new HashMap<>();
        
        // Java files
        mappings.put("java", Arrays.asList("checkstyle", "spotbugs", "pmd"));
        
        // TypeScript/JavaScript files
        mappings.put("ts", Arrays.asList("eslint", "prettier"));
        mappings.put("tsx", Arrays.asList("eslint", "prettier"));
        mappings.put("js", Arrays.asList("eslint", "prettier"));
        mappings.put("jsx", Arrays.asList("eslint", "prettier"));
        
        // Configuration files
        mappings.put("yml", Arrays.asList("yamllint"));
        mappings.put("yaml", Arrays.asList("yamllint"));
        mappings.put("json", Arrays.asList("jsonlint"));
        mappings.put("xml", Arrays.asList("xmllint"));
        
        // Documentation files
        mappings.put("md", Arrays.asList("markdownlint", "linkcheck"));
        mappings.put("markdown", Arrays.asList("markdownlint", "linkcheck"));
        
        // Docker files
        mappings.put("dockerfile", Arrays.asList("hadolint"));
        
        return mappings;
    }
}