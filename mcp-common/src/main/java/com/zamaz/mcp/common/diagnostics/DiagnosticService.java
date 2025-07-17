package com.zamaz.mcp.common.diagnostics;

import com.zamaz.mcp.common.logging.LogContext;
import com.zamaz.mcp.common.logging.StructuredLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Advanced diagnostic service for system inspection and troubleshooting
 */
@Service
@RequiredArgsConstructor
public class DiagnosticService {
    
    private final StructuredLogger logger;
    private final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    
    /**
     * Analyze memory usage patterns
     */
    public Map<String, Object> analyzeMemoryUsage() {
        LogContext context = LogContext.forRequest("memory-analysis", "analyze_memory_usage");
        
        try {
            Map<String, Object> analysis = new HashMap<>();
            
            // Memory pool analysis
            List<MemoryPoolMXBean> memoryPools = ManagementFactory.getMemoryPoolMXBeans();
            Map<String, Object> poolAnalysis = new HashMap<>();
            
            for (MemoryPoolMXBean pool : memoryPools) {
                MemoryUsage usage = pool.getUsage();
                MemoryUsage peakUsage = pool.getPeakUsage();
                
                Map<String, Object> poolInfo = new HashMap<>();
                poolInfo.put("type", pool.getType().toString());
                poolInfo.put("usage", usage);
                poolInfo.put("peakUsage", peakUsage);
                poolInfo.put("collectionUsage", pool.getCollectionUsage());
                poolInfo.put("usageThreshold", pool.getUsageThreshold());
                poolInfo.put("collectionUsageThreshold", pool.getCollectionUsageThreshold());
                
                // Calculate usage percentage
                if (usage != null && usage.getMax() > 0) {
                    double usagePercent = (double) usage.getUsed() / usage.getMax() * 100;
                    poolInfo.put("usagePercent", usagePercent);
                    
                    // Flag potential issues
                    if (usagePercent > 90) {
                        poolInfo.put("warning", "High memory usage detected");
                    } else if (usagePercent > 75) {
                        poolInfo.put("warning", "Elevated memory usage");
                    }
                }
                
                poolAnalysis.put(pool.getName(), poolInfo);
            }
            
            analysis.put("memoryPools", poolAnalysis);
            
            // Overall memory analysis
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            analysis.put("overall", Map.of(
                "totalMemory", totalMemory,
                "freeMemory", freeMemory,
                "usedMemory", usedMemory,
                "maxMemory", maxMemory,
                "usagePercent", (double) usedMemory / maxMemory * 100
            ));
            
            logger.info(DiagnosticService.class.getName(), "Memory analysis completed", context);
            return analysis;
            
        } catch (Exception e) {
            logger.error(DiagnosticService.class.getName(), "Memory analysis failed", 
                context.exception(e));
            throw new RuntimeException("Memory analysis failed", e);
        }
    }
    
    /**
     * Detect performance bottlenecks
     */
    public Map<String, Object> detectBottlenecks() {
        LogContext context = LogContext.forRequest("bottleneck-detection", "detect_bottlenecks");
        
        try {
            Map<String, Object> bottlenecks = new HashMap<>();
            
            // CPU analysis
            bottlenecks.put("cpu", analyzeCpuUsage());
            
            // Memory analysis
            bottlenecks.put("memory", analyzeMemoryBottlenecks());
            
            // Thread analysis
            bottlenecks.put("threads", analyzeThreadBottlenecks());
            
            // GC analysis
            bottlenecks.put("gc", analyzeGarbageCollection());
            
            logger.info(DiagnosticService.class.getName(), "Bottleneck detection completed", context);
            return bottlenecks;
            
        } catch (Exception e) {
            logger.error(DiagnosticService.class.getName(), "Bottleneck detection failed", 
                context.exception(e));
            throw new RuntimeException("Bottleneck detection failed", e);
        }
    }
    
    /**
     * Generate troubleshooting report
     */
    public Map<String, Object> generateTroubleshootingReport() {
        LogContext context = LogContext.forRequest("troubleshooting", "generate_report");
        
        try {
            Map<String, Object> report = new HashMap<>();
            
            // System overview
            report.put("system", getSystemOverview());
            
            // Performance metrics
            report.put("performance", getPerformanceMetrics());
            
            // Resource utilization
            report.put("resources", getResourceUtilization());
            
            // Potential issues
            report.put("issues", identifyPotentialIssues());
            
            // Recommendations
            report.put("recommendations", generateRecommendations());
            
            logger.info(DiagnosticService.class.getName(), "Troubleshooting report generated", context);
            return report;
            
        } catch (Exception e) {
            logger.error(DiagnosticService.class.getName(), "Troubleshooting report generation failed", 
                context.exception(e));
            throw new RuntimeException("Troubleshooting report generation failed", e);
        }
    }
    
    /**
     * Check system connectivity
     */
    public Map<String, Object> checkConnectivity() {
        LogContext context = LogContext.forRequest("connectivity-check", "check_connectivity");
        
        try {
            Map<String, Object> connectivity = new HashMap<>();
            
            // Database connectivity
            connectivity.put("database", checkDatabaseConnectivity());
            
            // Cache connectivity
            connectivity.put("cache", checkCacheConnectivity());
            
            // External services
            connectivity.put("external", checkExternalServices());
            
            logger.info(DiagnosticService.class.getName(), "Connectivity check completed", context);
            return connectivity;
            
        } catch (Exception e) {
            logger.error(DiagnosticService.class.getName(), "Connectivity check failed", 
                context.exception(e));
            throw new RuntimeException("Connectivity check failed", e);
        }
    }
    
    /**
     * Analyze CPU usage
     */
    private Map<String, Object> analyzeCpuUsage() {
        Map<String, Object> cpuAnalysis = new HashMap<>();
        
        try {
            // Get CPU load via MBean
            ObjectName osName = new ObjectName("java.lang:type=OperatingSystem");
            Double cpuLoad = (Double) mBeanServer.getAttribute(osName, "ProcessCpuLoad");
            Double systemCpuLoad = (Double) mBeanServer.getAttribute(osName, "SystemCpuLoad");
            
            cpuAnalysis.put("processCpuLoad", cpuLoad);
            cpuAnalysis.put("systemCpuLoad", systemCpuLoad);
            cpuAnalysis.put("availableProcessors", Runtime.getRuntime().availableProcessors());
            
            // Flag high CPU usage
            if (cpuLoad > 0.8) {
                cpuAnalysis.put("warning", "High CPU usage detected");
            }
            
        } catch (Exception e) {
            cpuAnalysis.put("error", "Unable to retrieve CPU metrics: " + e.getMessage());
        }
        
        return cpuAnalysis;
    }
    
    /**
     * Analyze memory bottlenecks
     */
    private Map<String, Object> analyzeMemoryBottlenecks() {
        Map<String, Object> memoryAnalysis = new HashMap<>();
        
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        double usagePercent = (double) usedMemory / maxMemory * 100;
        
        memoryAnalysis.put("usagePercent", usagePercent);
        memoryAnalysis.put("usedMemory", usedMemory);
        memoryAnalysis.put("maxMemory", maxMemory);
        
        if (usagePercent > 90) {
            memoryAnalysis.put("severity", "CRITICAL");
            memoryAnalysis.put("issue", "Memory usage is critically high");
        } else if (usagePercent > 75) {
            memoryAnalysis.put("severity", "WARNING");
            memoryAnalysis.put("issue", "Memory usage is elevated");
        }
        
        return memoryAnalysis;
    }
    
    /**
     * Analyze thread bottlenecks
     */
    private Map<String, Object> analyzeThreadBottlenecks() {
        Map<String, Object> threadAnalysis = new HashMap<>();
        
        var threadBean = ManagementFactory.getThreadMXBean();
        
        int threadCount = threadBean.getThreadCount();
        int peakThreadCount = threadBean.getPeakThreadCount();
        
        threadAnalysis.put("currentThreads", threadCount);
        threadAnalysis.put("peakThreads", peakThreadCount);
        
        // Check for thread leaks
        if (threadCount > 1000) {
            threadAnalysis.put("severity", "CRITICAL");
            threadAnalysis.put("issue", "Potential thread leak detected");
        } else if (threadCount > 500) {
            threadAnalysis.put("severity", "WARNING");
            threadAnalysis.put("issue", "High thread count");
        }
        
        // Analyze thread states
        long[] threadIds = threadBean.getAllThreadIds();
        var threadInfos = threadBean.getThreadInfo(threadIds);
        
        Map<String, Integer> threadStates = new HashMap<>();
        for (var info : threadInfos) {
            if (info != null) {
                String state = info.getThreadState().toString();
                threadStates.put(state, threadStates.getOrDefault(state, 0) + 1);
            }
        }
        
        threadAnalysis.put("threadStates", threadStates);
        
        return threadAnalysis;
    }
    
    /**
     * Analyze garbage collection
     */
    private Map<String, Object> analyzeGarbageCollection() {
        Map<String, Object> gcAnalysis = new HashMap<>();
        
        var gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        
        long totalCollectionTime = 0;
        long totalCollectionCount = 0;
        
        for (var gcBean : gcBeans) {
            Map<String, Object> gcInfo = new HashMap<>();
            gcInfo.put("collectionCount", gcBean.getCollectionCount());
            gcInfo.put("collectionTime", gcBean.getCollectionTime());
            
            totalCollectionTime += gcBean.getCollectionTime();
            totalCollectionCount += gcBean.getCollectionCount();
            
            gcAnalysis.put(gcBean.getName(), gcInfo);
        }
        
        gcAnalysis.put("totalCollectionTime", totalCollectionTime);
        gcAnalysis.put("totalCollectionCount", totalCollectionCount);
        
        // Check for excessive GC
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        double gcTimePercent = (double) totalCollectionTime / uptime * 100;
        
        if (gcTimePercent > 10) {
            gcAnalysis.put("severity", "CRITICAL");
            gcAnalysis.put("issue", "Excessive garbage collection time");
        } else if (gcTimePercent > 5) {
            gcAnalysis.put("severity", "WARNING");
            gcAnalysis.put("issue", "High garbage collection time");
        }
        
        return gcAnalysis;
    }
    
    /**
     * Get system overview
     */
    private Map<String, Object> getSystemOverview() {
        Map<String, Object> overview = new HashMap<>();
        
        overview.put("javaVersion", System.getProperty("java.version"));
        overview.put("javaVendor", System.getProperty("java.vendor"));
        overview.put("osName", System.getProperty("os.name"));
        overview.put("osVersion", System.getProperty("os.version"));
        overview.put("osArch", System.getProperty("os.arch"));
        overview.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime());
        
        return overview;
    }
    
    /**
     * Get performance metrics
     */
    private Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Add CPU, memory, and thread metrics
        metrics.put("cpu", analyzeCpuUsage());
        metrics.put("memory", analyzeMemoryBottlenecks());
        metrics.put("threads", analyzeThreadBottlenecks());
        metrics.put("gc", analyzeGarbageCollection());
        
        return metrics;
    }
    
    /**
     * Get resource utilization
     */
    private Map<String, Object> getResourceUtilization() {
        Map<String, Object> utilization = new HashMap<>();
        
        Runtime runtime = Runtime.getRuntime();
        utilization.put("memoryUtilization", (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory() * 100);
        utilization.put("threadUtilization", ManagementFactory.getThreadMXBean().getThreadCount());
        
        return utilization;
    }
    
    /**
     * Identify potential issues
     */
    private List<String> identifyPotentialIssues() {
        return List.of(
            "Check for memory leaks if memory usage is consistently high",
            "Monitor thread creation if thread count is increasing",
            "Review GC configuration if GC time is excessive",
            "Check for connection leaks if database connections are high"
        );
    }
    
    /**
     * Generate recommendations
     */
    private List<String> generateRecommendations() {
        return List.of(
            "Increase heap size if memory usage is consistently high",
            "Tune GC settings for your workload",
            "Monitor application metrics regularly",
            "Set up alerts for critical thresholds",
            "Review thread pool configurations"
        );
    }
    
    /**
     * Check database connectivity
     */
    private Map<String, Object> checkDatabaseConnectivity() {
        // Implementation depends on database configuration
        return Map.of("status", "unknown", "message", "Database connectivity check not implemented");
    }
    
    /**
     * Check cache connectivity
     */
    private Map<String, Object> checkCacheConnectivity() {
        // Implementation depends on cache configuration
        return Map.of("status", "unknown", "message", "Cache connectivity check not implemented");
    }
    
    /**
     * Check external services
     */
    private Map<String, Object> checkExternalServices() {
        // Implementation depends on external services
        return Map.of("status", "unknown", "message", "External services check not implemented");
    }
}