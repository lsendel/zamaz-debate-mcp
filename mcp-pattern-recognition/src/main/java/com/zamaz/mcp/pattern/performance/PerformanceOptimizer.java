package com.zamaz.mcp.pattern.performance;

import com.zamaz.mcp.pattern.core.PatternDetector;
import com.zamaz.mcp.pattern.core.PatternType;
import com.zamaz.mcp.pattern.model.CodeAnalysisContext;
import com.zamaz.mcp.pattern.model.PatternDetectionResult;
import com.zamaz.mcp.pattern.model.OptimizationStrategy;
import com.zamaz.mcp.pattern.model.PerformanceMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Service for optimizing pattern detection performance on large codebases.
 * 
 * This service provides:
 * - Parallel processing for large codebases
 * - Intelligent caching strategies
 * - Selective pattern detection
 * - Performance monitoring and metrics
 * - Adaptive optimization strategies
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceOptimizer {
    
    private final List<PatternDetector> patternDetectors;
    private final PatternDetectionCache cache;
    private final PerformanceMetricsCollector metricsCollector;
    private final ExecutorService executorService;
    
    // Configuration
    private static final int DEFAULT_THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
    private static final int MAX_BATCH_SIZE = 100;
    private static final long CACHE_TTL_MINUTES = 60;
    
    /**
     * Optimize pattern detection for large codebases.
     * 
     * @param contexts List of code analysis contexts
     * @param optimizationStrategy Strategy for optimization
     * @return Optimized pattern detection results
     */
    public List<PatternDetectionResult> optimizePatternDetection(List<CodeAnalysisContext> contexts,
                                                               OptimizationStrategy optimizationStrategy) {
        
        log.info("Starting optimized pattern detection for {} contexts", contexts.size());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Apply optimization strategy
            List<PatternDetectionResult> results = switch (optimizationStrategy.getType()) {
                case PARALLEL_PROCESSING -> executeParallelProcessing(contexts, optimizationStrategy);
                case SELECTIVE_DETECTION -> executeSelectiveDetection(contexts, optimizationStrategy);
                case CACHED_DETECTION -> executeCachedDetection(contexts, optimizationStrategy);
                case BATCHED_PROCESSING -> executeBatchedProcessing(contexts, optimizationStrategy);
                case ADAPTIVE_OPTIMIZATION -> executeAdaptiveOptimization(contexts, optimizationStrategy);
                default -> executeStandardProcessing(contexts);
            };
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Record performance metrics
            PerformanceMetrics metrics = PerformanceMetrics.builder()
                    .executionTime(executionTime)
                    .contextCount(contexts.size())
                    .resultCount(results.size())
                    .optimizationStrategy(optimizationStrategy.getType())
                    .throughput(calculateThroughput(contexts.size(), executionTime))
                    .memoryUsage(getCurrentMemoryUsage())
                    .cacheHitRatio(cache.getHitRatio())
                    .build();
            
            metricsCollector.recordMetrics(metrics);
            
            log.info("Optimized pattern detection completed in {}ms with {} results", 
                     executionTime, results.size());
            
            return results;
            
        } catch (Exception e) {
            log.error("Error during optimized pattern detection", e);
            throw new RuntimeException("Pattern detection optimization failed", e);
        }
    }
    
    /**
     * Execute parallel processing optimization.
     */
    private List<PatternDetectionResult> executeParallelProcessing(List<CodeAnalysisContext> contexts,
                                                                 OptimizationStrategy strategy) {
        
        log.info("Executing parallel processing optimization");
        
        // Create thread pool based on strategy configuration
        int threadPoolSize = strategy.getThreadPoolSize().orElse(DEFAULT_THREAD_POOL_SIZE);
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        
        try {
            // Submit tasks for parallel execution
            List<Future<List<PatternDetectionResult>>> futures = contexts.stream()
                    .map(context -> executor.submit(() -> processContextInParallel(context, strategy)))
                    .collect(Collectors.toList());
            
            // Collect results
            List<PatternDetectionResult> allResults = new ArrayList<>();
            for (Future<List<PatternDetectionResult>> future : futures) {
                try {
                    allResults.addAll(future.get());
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error processing context in parallel", e);
                }
            }
            
            return allResults;
            
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
    }
    
    /**
     * Execute selective detection optimization.
     */
    private List<PatternDetectionResult> executeSelectiveDetection(List<CodeAnalysisContext> contexts,
                                                                  OptimizationStrategy strategy) {
        
        log.info("Executing selective detection optimization");
        
        // Filter detectors based on strategy
        List<PatternDetector> selectedDetectors = selectDetectors(strategy);
        
        // Filter contexts based on strategy
        List<CodeAnalysisContext> selectedContexts = selectContexts(contexts, strategy);
        
        List<PatternDetectionResult> results = new ArrayList<>();
        
        for (CodeAnalysisContext context : selectedContexts) {
            for (PatternDetector detector : selectedDetectors) {
                if (shouldRunDetector(detector, context, strategy)) {
                    results.addAll(detector.detectPatterns(context));
                }
            }
        }
        
        return results;
    }
    
    /**
     * Execute cached detection optimization.
     */
    private List<PatternDetectionResult> executeCachedDetection(List<CodeAnalysisContext> contexts,
                                                               OptimizationStrategy strategy) {
        
        log.info("Executing cached detection optimization");
        
        List<PatternDetectionResult> results = new ArrayList<>();
        List<CodeAnalysisContext> uncachedContexts = new ArrayList<>();
        
        // Check cache for existing results
        for (CodeAnalysisContext context : contexts) {
            String cacheKey = generateCacheKey(context);
            Optional<List<PatternDetectionResult>> cachedResults = cache.get(cacheKey);
            
            if (cachedResults.isPresent()) {
                results.addAll(cachedResults.get());
            } else {
                uncachedContexts.add(context);
            }
        }
        
        // Process uncached contexts
        for (CodeAnalysisContext context : uncachedContexts) {
            List<PatternDetectionResult> contextResults = processContext(context, strategy);
            results.addAll(contextResults);
            
            // Cache the results
            String cacheKey = generateCacheKey(context);
            cache.put(cacheKey, contextResults, CACHE_TTL_MINUTES);
        }
        
        return results;
    }
    
    /**
     * Execute batched processing optimization.
     */
    private List<PatternDetectionResult> executeBatchedProcessing(List<CodeAnalysisContext> contexts,
                                                                 OptimizationStrategy strategy) {
        
        log.info("Executing batched processing optimization");
        
        int batchSize = strategy.getBatchSize().orElse(MAX_BATCH_SIZE);
        List<PatternDetectionResult> results = new ArrayList<>();
        
        // Process contexts in batches
        for (int i = 0; i < contexts.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, contexts.size());
            List<CodeAnalysisContext> batch = contexts.subList(i, endIndex);
            
            log.debug("Processing batch {}-{} of {}", i, endIndex - 1, contexts.size());
            
            List<PatternDetectionResult> batchResults = processBatch(batch, strategy);
            results.addAll(batchResults);
            
            // Optional: Add delay between batches to prevent resource exhaustion
            if (strategy.getBatchDelay().isPresent()) {
                try {
                    Thread.sleep(strategy.getBatchDelay().get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        return results;
    }
    
    /**
     * Execute adaptive optimization.
     */
    private List<PatternDetectionResult> executeAdaptiveOptimization(List<CodeAnalysisContext> contexts,
                                                                    OptimizationStrategy strategy) {
        
        log.info("Executing adaptive optimization");
        
        // Analyze codebase characteristics
        CodebaseCharacteristics characteristics = analyzeCodebase(contexts);
        
        // Adapt strategy based on characteristics
        OptimizationStrategy adaptedStrategy = adaptStrategy(strategy, characteristics);
        
        // Execute with adapted strategy
        return optimizePatternDetection(contexts, adaptedStrategy);
    }
    
    /**
     * Execute standard processing without optimization.
     */
    private List<PatternDetectionResult> executeStandardProcessing(List<CodeAnalysisContext> contexts) {
        
        log.info("Executing standard processing");
        
        List<PatternDetectionResult> results = new ArrayList<>();
        
        for (CodeAnalysisContext context : contexts) {
            for (PatternDetector detector : patternDetectors) {
                results.addAll(detector.detectPatterns(context));
            }
        }
        
        return results;
    }
    
    /**
     * Process a single context in parallel.
     */
    private List<PatternDetectionResult> processContextInParallel(CodeAnalysisContext context,
                                                                 OptimizationStrategy strategy) {
        
        List<PatternDetectionResult> results = new ArrayList<>();
        
        // Get applicable detectors
        List<PatternDetector> applicableDetectors = getApplicableDetectors(context, strategy);
        
        // Process detectors in parallel
        List<Future<List<PatternDetectionResult>>> futures = applicableDetectors.stream()
                .map(detector -> CompletableFuture.supplyAsync(() -> detector.detectPatterns(context)))
                .collect(Collectors.toList());
        
        // Collect results
        for (Future<List<PatternDetectionResult>> future : futures) {
            try {
                results.addAll(future.get());
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error processing detector in parallel", e);
            }
        }
        
        return results;
    }
    
    /**
     * Select detectors based on optimization strategy.
     */
    private List<PatternDetector> selectDetectors(OptimizationStrategy strategy) {
        
        if (strategy.getSelectedPatternTypes().isEmpty()) {
            return patternDetectors;
        }
        
        Set<PatternType> selectedTypes = strategy.getSelectedPatternTypes();
        
        return patternDetectors.stream()
                .filter(detector -> selectedTypes.contains(detector.getPatternType()))
                .collect(Collectors.toList());
    }
    
    /**
     * Select contexts based on optimization strategy.
     */
    private List<CodeAnalysisContext> selectContexts(List<CodeAnalysisContext> contexts,
                                                    OptimizationStrategy strategy) {
        
        return contexts.stream()
                .filter(context -> shouldProcessContext(context, strategy))
                .collect(Collectors.toList());
    }
    
    /**
     * Check if a detector should run for a given context.
     */
    private boolean shouldRunDetector(PatternDetector detector, CodeAnalysisContext context,
                                     OptimizationStrategy strategy) {
        
        // Check if detector supports the file type
        if (!detector.supportsFileType(context.getFileExtension())) {
            return false;
        }
        
        // Check performance impact vs. strategy requirements
        if (strategy.getMaxPerformanceImpact().isPresent()) {
            PatternDetector.PerformanceImpact maxImpact = strategy.getMaxPerformanceImpact().get();
            if (detector.getPerformanceImpact().ordinal() > maxImpact.ordinal()) {
                return false;
            }
        }
        
        // Check if pattern type is in selected types
        if (!strategy.getSelectedPatternTypes().isEmpty()) {
            return strategy.getSelectedPatternTypes().contains(detector.getPatternType());
        }
        
        return true;
    }
    
    /**
     * Check if a context should be processed.
     */
    private boolean shouldProcessContext(CodeAnalysisContext context, OptimizationStrategy strategy) {
        
        // Skip test files if configured
        if (strategy.isSkipTestFiles() && context.isTestFile()) {
            return false;
        }
        
        // Skip generated files if configured
        if (strategy.isSkipGeneratedFiles() && context.getFileMetadata().isGenerated()) {
            return false;
        }
        
        // Skip files based on size limits
        if (strategy.getMaxFileSize().isPresent()) {
            long maxSize = strategy.getMaxFileSize().get();
            if (context.getFileMetadata().getFileSize() > maxSize) {
                return false;
            }
        }
        
        // Skip files based on line count limits
        if (strategy.getMaxLineCount().isPresent()) {
            int maxLines = strategy.getMaxLineCount().get();
            if (context.getLineCount() > maxLines) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Generate cache key for a context.
     */
    private String generateCacheKey(CodeAnalysisContext context) {
        return String.format("%s:%s:%d", 
                context.getFilePath().toString(),
                context.getFileMetadata().getContentHash(),
                context.getFileMetadata().getLastModified().hashCode());
    }
    
    /**
     * Process a single context with strategy.
     */
    private List<PatternDetectionResult> processContext(CodeAnalysisContext context,
                                                       OptimizationStrategy strategy) {
        
        List<PatternDetectionResult> results = new ArrayList<>();
        List<PatternDetector> applicableDetectors = getApplicableDetectors(context, strategy);
        
        for (PatternDetector detector : applicableDetectors) {
            if (shouldRunDetector(detector, context, strategy)) {
                results.addAll(detector.detectPatterns(context));
            }
        }
        
        return results;
    }
    
    /**
     * Process a batch of contexts.
     */
    private List<PatternDetectionResult> processBatch(List<CodeAnalysisContext> batch,
                                                     OptimizationStrategy strategy) {
        
        List<PatternDetectionResult> results = new ArrayList<>();
        
        for (CodeAnalysisContext context : batch) {
            results.addAll(processContext(context, strategy));
        }
        
        return results;
    }
    
    /**
     * Get applicable detectors for a context.
     */
    private List<PatternDetector> getApplicableDetectors(CodeAnalysisContext context,
                                                        OptimizationStrategy strategy) {
        
        return patternDetectors.stream()
                .filter(detector -> detector.supportsFileType(context.getFileExtension()))
                .filter(detector -> shouldRunDetector(detector, context, strategy))
                .collect(Collectors.toList());
    }
    
    /**
     * Analyze codebase characteristics for adaptive optimization.
     */
    private CodebaseCharacteristics analyzeCodebase(List<CodeAnalysisContext> contexts) {
        
        int totalFiles = contexts.size();
        long totalLines = contexts.stream()
                .mapToLong(CodeAnalysisContext::getLineCount)
                .sum();
        
        long totalSize = contexts.stream()
                .mapToLong(context -> context.getFileMetadata().getFileSize())
                .sum();
        
        double averageComplexity = contexts.stream()
                .mapToDouble(CodeAnalysisContext::getCyclomaticComplexity)
                .average()
                .orElse(0.0);
        
        Map<String, Long> fileTypeDistribution = contexts.stream()
                .collect(Collectors.groupingBy(CodeAnalysisContext::getFileExtension, 
                        Collectors.counting()));
        
        return CodebaseCharacteristics.builder()
                .totalFiles(totalFiles)
                .totalLines(totalLines)
                .totalSize(totalSize)
                .averageComplexity(averageComplexity)
                .fileTypeDistribution(fileTypeDistribution)
                .hasTestFiles(contexts.stream().anyMatch(CodeAnalysisContext::isTestFile))
                .hasGeneratedFiles(contexts.stream().anyMatch(c -> c.getFileMetadata().isGenerated()))
                .build();
    }
    
    /**
     * Adapt strategy based on codebase characteristics.
     */
    private OptimizationStrategy adaptStrategy(OptimizationStrategy baseStrategy,
                                             CodebaseCharacteristics characteristics) {
        
        OptimizationStrategy.Builder adaptedBuilder = baseStrategy.toBuilder();
        
        // Adapt based on codebase size
        if (characteristics.getTotalFiles() > 1000) {
            adaptedBuilder.type(OptimizationStrategy.Type.PARALLEL_PROCESSING);
            adaptedBuilder.threadPoolSize(Math.max(DEFAULT_THREAD_POOL_SIZE, 
                    characteristics.getTotalFiles() / 100));
        } else if (characteristics.getTotalFiles() > 100) {
            adaptedBuilder.type(OptimizationStrategy.Type.BATCHED_PROCESSING);
            adaptedBuilder.batchSize(50);
        }
        
        // Adapt based on complexity
        if (characteristics.getAverageComplexity() > 20) {
            adaptedBuilder.maxPerformanceImpact(PatternDetector.PerformanceImpact.MEDIUM);
        }
        
        // Adapt based on file types
        if (characteristics.getFileTypeDistribution().size() > 5) {
            adaptedBuilder.type(OptimizationStrategy.Type.SELECTIVE_DETECTION);
        }
        
        return adaptedBuilder.build();
    }
    
    /**
     * Calculate throughput (files per second).
     */
    private double calculateThroughput(int contextCount, long executionTimeMs) {
        if (executionTimeMs == 0) return 0.0;
        return (double) contextCount / (executionTimeMs / 1000.0);
    }
    
    /**
     * Get current memory usage.
     */
    private long getCurrentMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    /**
     * Get optimization recommendations for a codebase.
     */
    public OptimizationRecommendations getOptimizationRecommendations(CodebaseCharacteristics characteristics) {
        
        List<String> recommendations = new ArrayList<>();
        OptimizationStrategy.Type recommendedType = OptimizationStrategy.Type.STANDARD;
        
        if (characteristics.getTotalFiles() > 1000) {
            recommendations.add("Use parallel processing for large codebase");
            recommendedType = OptimizationStrategy.Type.PARALLEL_PROCESSING;
        }
        
        if (characteristics.getAverageComplexity() > 20) {
            recommendations.add("Consider selective detection for high complexity code");
        }
        
        if (characteristics.getTotalSize() > 100 * 1024 * 1024) { // 100MB
            recommendations.add("Enable caching for large files");
        }
        
        return OptimizationRecommendations.builder()
                .recommendedType(recommendedType)
                .recommendations(recommendations)
                .estimatedSpeedup(calculateEstimatedSpeedup(characteristics, recommendedType))
                .build();
    }
    
    private double calculateEstimatedSpeedup(CodebaseCharacteristics characteristics,
                                           OptimizationStrategy.Type optimizationType) {
        
        return switch (optimizationType) {
            case PARALLEL_PROCESSING -> Math.min(DEFAULT_THREAD_POOL_SIZE, characteristics.getTotalFiles() / 10.0);
            case CACHED_DETECTION -> 2.0; // Assume 2x speedup with caching
            case SELECTIVE_DETECTION -> 1.5; // Assume 1.5x speedup with selective detection
            case BATCHED_PROCESSING -> 1.3; // Assume 1.3x speedup with batching
            default -> 1.0;
        };
    }
}