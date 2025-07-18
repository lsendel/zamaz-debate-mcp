package com.zamaz.mcp.common.linting.impl;

import com.zamaz.mcp.common.linting.*;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Default implementation of the linting engine.
 */
@Component
public class DefaultLintingEngine implements LintingEngine {
    
    private final Map<String, List<String>> lintersByExtension;
    
    public DefaultLintingEngine() {
        this.lintersByExtension = initializeLinterMappings();
    }
    
    @Override
    public LintingResult lintProject(LintingContext context) {
        long startTime = System.currentTimeMillis();
        List<LintingIssue> allIssues = new ArrayList<>();
        Map<String, Object> metrics = new ConcurrentHashMap<>();
        
        try {
            // Get all files to lint based on project structure
            List<Path> filesToLint = getProjectFiles(context);
            
            if (context.isParallelExecution()) {
                // Parallel execution
                List<CompletableFuture<List<LintingIssue>>> futures = filesToLint.stream()
                    .map(file -> CompletableFuture.supplyAsync(() -> lintFile(file, context)))
                    .collect(Collectors.toList());
                
                for (CompletableFuture<List<LintingIssue>> future : futures) {
                    allIssues.addAll(future.join());
                }
            } else {
                // Sequential execution
                for (Path file : filesToLint) {
                    allIssues.addAll(lintFile(file, context));
                }
            }
            
            // Calculate metrics
            metrics.put("totalFiles", filesToLint.size());
            metrics.put("totalIssues", allIssues.size());
            metrics.put("errorCount", allIssues.stream().mapToInt(i -> i.getSeverity() == LintingSeverity.ERROR ? 1 : 0).sum());
            metrics.put("warningCount", allIssues.stream().mapToInt(i -> i.getSeverity() == LintingSeverity.WARNING ? 1 : 0).sum());
            
            long duration = System.currentTimeMillis() - startTime;
            
            return new DefaultLintingResult(
                allIssues,
                metrics,
                LocalDateTime.now(),
                filesToLint.size(),
                duration,
                true
            );
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            metrics.put("error", e.getMessage());
            
            return new DefaultLintingResult(
                allIssues,
                metrics,
                LocalDateTime.now(),
                0,
                duration,
                false
            );
        }
    }
    
    @Override
    public LintingResult lintService(String serviceName, LintingContext context) {
        // Filter files to only include the specified service
        Path servicePath = context.getProjectRoot().resolve(serviceName);
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
        long startTime = System.currentTimeMillis();
        List<LintingIssue> allIssues = new ArrayList<>();
        Map<String, Object> metrics = new ConcurrentHashMap<>();
        
        try {
            for (Path file : files) {
                allIssues.addAll(lintFile(file, context));
            }
            
            metrics.put("totalFiles", files.size());
            metrics.put("totalIssues", allIssues.size());
            metrics.put("errorCount", allIssues.stream().mapToInt(i -> i.getSeverity() == LintingSeverity.ERROR ? 1 : 0).sum());
            metrics.put("warningCount", allIssues.stream().mapToInt(i -> i.getSeverity() == LintingSeverity.WARNING ? 1 : 0).sum());
            
            long duration = System.currentTimeMillis() - startTime;
            
            return new DefaultLintingResult(
                allIssues,
                metrics,
                LocalDateTime.now(),
                files.size(),
                duration,
                true
            );
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            metrics.put("error", e.getMessage());
            
            return new DefaultLintingResult(
                allIssues,
                metrics,
                LocalDateTime.now(),
                files.size(),
                duration,
                false
            );
        }
    }
    
    @Override
    public List<String> getAvailableLinters(String fileExtension) {
        return lintersByExtension.getOrDefault(fileExtension.toLowerCase(), Collections.emptyList());
    }
    
    private List<Path> getProjectFiles(LintingContext context) {
        // This is a simplified implementation
        // In a real implementation, this would scan the project directory
        // and filter files based on extension and exclude patterns
        List<Path> files = new ArrayList<>();
        
        // Add Java files
        files.addAll(findFilesByExtension(context.getProjectRoot(), "java"));
        
        // Add Python files - NEW for 2025
        files.addAll(findFilesByExtension(context.getProjectRoot(), "py"));
        files.addAll(findFilesByExtension(context.getProjectRoot(), "pyi"));
        
        // Add Shell scripts - NEW for 2025
        files.addAll(findFilesByExtension(context.getProjectRoot(), "sh"));
        files.addAll(findFilesByExtension(context.getProjectRoot(), "bash"));
        
        // Add TypeScript files
        files.addAll(findFilesByExtension(context.getProjectRoot(), "ts"));
        files.addAll(findFilesByExtension(context.getProjectRoot(), "tsx"));
        
        // Add JavaScript files
        files.addAll(findFilesByExtension(context.getProjectRoot(), "js"));
        files.addAll(findFilesByExtension(context.getProjectRoot(), "jsx"));
        
        // Add configuration files
        files.addAll(findFilesByExtension(context.getProjectRoot(), "yml"));
        files.addAll(findFilesByExtension(context.getProjectRoot(), "yaml"));
        files.addAll(findFilesByExtension(context.getProjectRoot(), "json"));
        
        // Add documentation files
        files.addAll(findFilesByExtension(context.getProjectRoot(), "md"));
        
        return files.stream()
            .filter(file -> !isExcluded(file, context.getExcludePatterns()))
            .collect(Collectors.toList());
    }
    
    private List<Path> findFilesByExtension(Path root, String extension) {
        // Simplified implementation - in reality would use Files.walk()
        return Collections.emptyList();
    }
    
    private boolean isExcluded(Path file, List<String> excludePatterns) {
        if (excludePatterns == null) {
            return false;
        }
        
        String filePath = file.toString();
        return excludePatterns.stream()
            .anyMatch(pattern -> filePath.matches(pattern.replace("*", ".*")));
    }
    
    private List<LintingIssue> lintFile(Path file, LintingContext context) {
        String extension = getFileExtension(file);
        List<String> availableLinters = getAvailableLinters(extension);
        List<LintingIssue> issues = new ArrayList<>();
        
        for (String linterName : availableLinters) {
            // This would delegate to specific linter implementations
            // For now, return empty list
        }
        
        return issues;
    }
    
    private String getFileExtension(Path file) {
        String fileName = file.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }
    
    private Map<String, List<String>> initializeLinterMappings() {
        Map<String, List<String>> mappings = new HashMap<>();
        
        // Java files
        mappings.put("java", Arrays.asList("checkstyle", "spotbugs", "pmd"));
        
        // Python files - NEW for 2025
        mappings.put("py", Arrays.asList("ruff", "mypy", "bandit"));
        mappings.put("pyi", Arrays.asList("ruff", "mypy"));
        
        // Shell scripts - NEW for 2025
        mappings.put("sh", Arrays.asList("shellcheck"));
        mappings.put("bash", Arrays.asList("shellcheck"));
        mappings.put("ksh", Arrays.asList("shellcheck"));
        mappings.put("zsh", Arrays.asList("shellcheck"));
        
        // TypeScript/JavaScript files
        mappings.put("ts", Arrays.asList("eslint", "prettier"));
        mappings.put("tsx", Arrays.asList("eslint", "prettier"));
        mappings.put("js", Arrays.asList("eslint", "prettier"));
        mappings.put("jsx", Arrays.asList("eslint", "prettier"));
        
        // Configuration files
        mappings.put("yml", Arrays.asList("yamllint"));
        mappings.put("yaml", Arrays.asList("yamllint"));
        mappings.put("json", Arrays.asList("jsonlint"));
        
        // Documentation files
        mappings.put("md", Arrays.asList("markdownlint", "linkcheck"));
        
        // Docker files
        mappings.put("dockerfile", Arrays.asList("hadolint"));
        
        return mappings;
    }
}