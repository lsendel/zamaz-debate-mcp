package com.zamaz.mcp.common.diagnostics;

import com.zamaz.mcp.common.logging.LogContext;
import com.zamaz.mcp.common.logging.StructuredLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Comprehensive debugging utilities for troubleshooting system issues
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DebuggingUtilities {
    
    private final StructuredLogger structuredLogger;
    private final ConcurrentMap<String, DebugSession> activeSessions = new ConcurrentHashMap<>();
    
    /**
     * Start a debug session for tracking operations
     */
    public String startDebugSession(String sessionName, String component) {
        String sessionId = UUID.randomUUID().toString();
        
        DebugSession session = new DebugSession(
            sessionId,
            sessionName,
            component,
            Instant.now(),
            Thread.currentThread().getName(),
            new ArrayList<>(),
            new HashMap<>()
        );
        
        activeSessions.put(sessionId, session);
        
        LogContext context = LogContext.builder()
            .operation("debug_session_start")
            .component(component)
            .requestId(sessionId)
            .build()
            .addMetadata("sessionName", sessionName);
        
        structuredLogger.info(DebuggingUtilities.class.getName(), 
            "Debug session started: " + sessionName, context);
        
        return sessionId;
    }
    
    /**
     * End a debug session and generate summary
     */
    public Map<String, Object> endDebugSession(String sessionId) {
        DebugSession session = activeSessions.remove(sessionId);
        
        if (session == null) {
            log.warn("Debug session not found: {}", sessionId);
            return Collections.emptyMap();
        }
        
        Instant endTime = Instant.now();
        Duration totalDuration = Duration.between(session.getStartTime(), endTime);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("sessionId", sessionId);
        summary.put("sessionName", session.getSessionName());
        summary.put("component", session.getComponent());
        summary.put("startTime", session.getStartTime());
        summary.put("endTime", endTime);
        summary.put("totalDuration", totalDuration.toMillis());
        summary.put("threadName", session.getThreadName());
        summary.put("eventCount", session.getEvents().size());
        summary.put("metadata", session.getMetadata());
        
        // Analyze events
        Map<String, Object> eventAnalysis = analyzeDebugEvents(session.getEvents());
        summary.put("eventAnalysis", eventAnalysis);
        
        LogContext context = LogContext.builder()
            .operation("debug_session_end")
            .component(session.getComponent())
            .requestId(sessionId)
            .duration(totalDuration.toMillis())
            .build()
            .addMetadata("sessionName", session.getSessionName())
            .addMetadata("eventCount", session.getEvents().size());
        
        structuredLogger.info(DebuggingUtilities.class.getName(), 
            "Debug session ended: " + session.getSessionName(), context);
        
        return summary;
    }
    
    /**
     * Add a debug event to a session
     */
    public void addDebugEvent(String sessionId, String eventType, String message, 
                             Map<String, Object> eventData) {
        DebugSession session = activeSessions.get(sessionId);
        
        if (session == null) {
            log.warn("Debug session not found: {}", sessionId);
            return;
        }
        
        DebugEvent event = new DebugEvent(
            eventType,
            message,
            Instant.now(),
            Thread.currentThread().getName(),
            eventData != null ? eventData : new HashMap<>()
        );
        
        session.getEvents().add(event);
        
        LogContext context = LogContext.builder()
            .operation("debug_event")
            .component(session.getComponent())
            .requestId(sessionId)
            .build()
            .addMetadata("eventType", eventType)
            .addMetadata("message", message);
        
        if (eventData != null) {
            eventData.forEach(context::addMetadata);
        }
        
        structuredLogger.debug(DebuggingUtilities.class.getName(), 
            "Debug event: " + eventType + " - " + message, context);
    }
    
    /**
     * Add metadata to a debug session
     */
    public void addSessionMetadata(String sessionId, String key, Object value) {
        DebugSession session = activeSessions.get(sessionId);
        
        if (session != null) {
            session.getMetadata().put(key, value);
        }
    }
    
    /**
     * Generate thread dump for debugging
     */
    public Map<String, Object> generateThreadDump() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        
        Map<String, Object> threadDump = new HashMap<>();
        threadDump.put("timestamp", Instant.now());
        threadDump.put("threadCount", threadBean.getThreadCount());
        threadDump.put("daemonThreadCount", threadBean.getDaemonThreadCount());
        threadDump.put("peakThreadCount", threadBean.getPeakThreadCount());
        
        // Get detailed thread information
        long[] threadIds = threadBean.getAllThreadIds();
        ThreadInfo[] threadInfos = threadBean.getThreadInfo(threadIds, true, true);
        
        List<Map<String, Object>> threads = new ArrayList<>();
        Map<String, Integer> threadStates = new HashMap<>();
        Map<String, Integer> threadsByName = new HashMap<>();
        
        for (ThreadInfo threadInfo : threadInfos) {
            if (threadInfo != null) {
                Map<String, Object> threadData = new HashMap<>();
                threadData.put("id", threadInfo.getThreadId());
                threadData.put("name", threadInfo.getThreadName());
                threadData.put("state", threadInfo.getThreadState().toString());
                threadData.put("blockedTime", threadInfo.getBlockedTime());
                threadData.put("blockedCount", threadInfo.getBlockedCount());
                threadData.put("waitedTime", threadInfo.getWaitedTime());
                threadData.put("waitedCount", threadInfo.getWaitedCount());
                threadData.put("lockName", threadInfo.getLockName());
                threadData.put("lockOwnerId", threadInfo.getLockOwnerId());
                threadData.put("lockOwnerName", threadInfo.getLockOwnerName());
                threadData.put("suspended", threadInfo.isSuspended());
                threadData.put("inNative", threadInfo.isInNative());
                
                // Stack trace
                StackTraceElement[] stackTrace = threadInfo.getStackTrace();
                if (stackTrace != null && stackTrace.length > 0) {
                    List<String> stackElements = Arrays.stream(stackTrace)
                        .limit(10) // Limit to first 10 frames
                        .map(StackTraceElement::toString)
                        .collect(Collectors.toList());
                    threadData.put("stackTrace", stackElements);
                }
                
                threads.add(threadData);
                
                // Count thread states
                String state = threadInfo.getThreadState().toString();
                threadStates.put(state, threadStates.getOrDefault(state, 0) + 1);
                
                // Count threads by name pattern
                String threadName = threadInfo.getThreadName();
                String namePattern = extractThreadNamePattern(threadName);
                threadsByName.put(namePattern, threadsByName.getOrDefault(namePattern, 0) + 1);
            }
        }
        
        threadDump.put("threads", threads);
        threadDump.put("threadStates", threadStates);
        threadDump.put("threadsByName", threadsByName);
        
        // Identify potential issues
        List<String> issues = identifyThreadIssues(threadInfos, threadStates);
        threadDump.put("potentialIssues", issues);
        
        LogContext context = LogContext.builder()
            .operation("thread_dump")
            .component("debugging")
            .build()
            .addMetadata("threadCount", threadBean.getThreadCount())
            .addMetadata("issueCount", issues.size());
        
        structuredLogger.info(DebuggingUtilities.class.getName(), 
            "Thread dump generated", context);
        
        return threadDump;
    }
    
    /**
     * Analyze memory usage patterns
     */
    public Map<String, Object> analyzeMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        
        Map<String, Object> memoryAnalysis = new HashMap<>();
        memoryAnalysis.put("timestamp", Instant.now());
        
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        memoryAnalysis.put("totalMemory", totalMemory);
        memoryAnalysis.put("freeMemory", freeMemory);
        memoryAnalysis.put("usedMemory", usedMemory);
        memoryAnalysis.put("maxMemory", maxMemory);
        memoryAnalysis.put("usagePercent", (double) usedMemory / maxMemory * 100);
        
        // Memory recommendations
        List<String> recommendations = new ArrayList<>();
        double usagePercent = (double) usedMemory / maxMemory * 100;
        
        if (usagePercent > 90) {
            recommendations.add("Critical: Memory usage above 90%. Consider increasing heap size or identifying memory leaks.");
        } else if (usagePercent > 75) {
            recommendations.add("Warning: Memory usage above 75%. Monitor for potential memory pressure.");
        }
        
        if (totalMemory < maxMemory * 0.5) {
            recommendations.add("Info: JVM is using less than 50% of max heap. Consider adjusting initial heap size.");
        }
        
        memoryAnalysis.put("recommendations", recommendations);
        
        LogContext context = LogContext.builder()
            .operation("memory_analysis")
            .component("debugging")
            .build()
            .addMetadata("usagePercent", usagePercent)
            .addMetadata("recommendationCount", recommendations.size());
        
        structuredLogger.info(DebuggingUtilities.class.getName(), 
            "Memory analysis completed", context);
        
        return memoryAnalysis;
    }
    
    /**
     * Generate system health report
     */
    public Map<String, Object> generateSystemHealthReport() {
        Map<String, Object> healthReport = new HashMap<>();
        healthReport.put("timestamp", Instant.now());
        
        // Memory analysis
        healthReport.put("memory", analyzeMemoryUsage());
        
        // Thread analysis
        Map<String, Object> threadInfo = generateThreadDump();
        healthReport.put("threads", Map.of(
            "count", threadInfo.get("threadCount"),
            "states", threadInfo.get("threadStates"),
            "issues", threadInfo.get("potentialIssues")
        ));
        
        // System properties
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("javaVersion", System.getProperty("java.version"));
        systemInfo.put("javaVendor", System.getProperty("java.vendor"));
        systemInfo.put("osName", System.getProperty("os.name"));
        systemInfo.put("osVersion", System.getProperty("os.version"));
        systemInfo.put("osArch", System.getProperty("os.arch"));
        systemInfo.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        systemInfo.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime());
        
        healthReport.put("system", systemInfo);
        
        // Active debug sessions
        healthReport.put("debugSessions", activeSessions.size());
        
        // Overall health score
        int healthScore = calculateHealthScore(healthReport);
        healthReport.put("healthScore", healthScore);
        healthReport.put("healthStatus", getHealthStatus(healthScore));
        
        LogContext context = LogContext.builder()
            .operation("system_health_report")
            .component("debugging")
            .build()
            .addMetadata("healthScore", healthScore)
            .addMetadata("healthStatus", getHealthStatus(healthScore));
        
        structuredLogger.info(DebuggingUtilities.class.getName(), 
            "System health report generated", context);
        
        return healthReport;
    }
    
    /**
     * Get all active debug sessions
     */
    public Map<String, Object> getActiveDebugSessions() {
        Map<String, Object> sessions = new HashMap<>();
        
        for (Map.Entry<String, DebugSession> entry : activeSessions.entrySet()) {
            DebugSession session = entry.getValue();
            Map<String, Object> sessionInfo = new HashMap<>();
            sessionInfo.put("sessionName", session.getSessionName());
            sessionInfo.put("component", session.getComponent());
            sessionInfo.put("startTime", session.getStartTime());
            sessionInfo.put("duration", Duration.between(session.getStartTime(), Instant.now()).toMillis());
            sessionInfo.put("eventCount", session.getEvents().size());
            sessionInfo.put("threadName", session.getThreadName());
            
            sessions.put(entry.getKey(), sessionInfo);
        }
        
        return sessions;
    }
    
    /**
     * Analyze debug events for patterns
     */
    private Map<String, Object> analyzeDebugEvents(List<DebugEvent> events) {
        Map<String, Object> analysis = new HashMap<>();
        
        if (events.isEmpty()) {
            return analysis;
        }
        
        // Event type distribution
        Map<String, Long> eventTypes = events.stream()
            .collect(Collectors.groupingBy(DebugEvent::getEventType, Collectors.counting()));
        analysis.put("eventTypes", eventTypes);
        
        // Timeline analysis
        Instant firstEvent = events.get(0).getTimestamp();
        Instant lastEvent = events.get(events.size() - 1).getTimestamp();
        analysis.put("timespan", Duration.between(firstEvent, lastEvent).toMillis());
        
        // Thread analysis
        Map<String, Long> threadDistribution = events.stream()
            .collect(Collectors.groupingBy(DebugEvent::getThreadName, Collectors.counting()));
        analysis.put("threadDistribution", threadDistribution);
        
        // Identify patterns
        List<String> patterns = new ArrayList<>();
        if (eventTypes.size() == 1) {
            patterns.add("Single event type pattern detected");
        }
        if (threadDistribution.size() > 1) {
            patterns.add("Multi-threaded execution detected");
        }
        
        analysis.put("patterns", patterns);
        
        return analysis;
    }
    
    /**
     * Extract thread name pattern
     */
    private String extractThreadNamePattern(String threadName) {
        // Extract pattern from thread names like "http-nio-8080-exec-1"
        if (threadName.contains("exec-")) {
            return threadName.substring(0, threadName.lastIndexOf("-"));
        }
        if (threadName.contains("-")) {
            return threadName.substring(0, threadName.indexOf("-"));
        }
        return threadName;
    }
    
    /**
     * Identify potential thread issues
     */
    private List<String> identifyThreadIssues(ThreadInfo[] threadInfos, Map<String, Integer> threadStates) {
        List<String> issues = new ArrayList<>();
        
        int blockedCount = threadStates.getOrDefault("BLOCKED", 0);
        int waitingCount = threadStates.getOrDefault("WAITING", 0);
        int timedWaitingCount = threadStates.getOrDefault("TIMED_WAITING", 0);
        
        if (blockedCount > 10) {
            issues.add("High number of blocked threads (" + blockedCount + ")");
        }
        
        if (waitingCount > 20) {
            issues.add("High number of waiting threads (" + waitingCount + ")");
        }
        
        if (threadInfos.length > 200) {
            issues.add("High thread count (" + threadInfos.length + ") - possible thread leak");
        }
        
        // Check for deadlocks
        for (ThreadInfo threadInfo : threadInfos) {
            if (threadInfo != null && threadInfo.getLockName() != null && 
                threadInfo.getThreadState() == Thread.State.BLOCKED) {
                issues.add("Potential deadlock detected in thread: " + threadInfo.getThreadName());
            }
        }
        
        return issues;
    }
    
    /**
     * Calculate overall health score
     */
    private int calculateHealthScore(Map<String, Object> healthReport) {
        int score = 100;
        
        // Memory score
        @SuppressWarnings("unchecked")
        Map<String, Object> memory = (Map<String, Object>) healthReport.get("memory");
        Double memoryUsage = (Double) memory.get("usagePercent");
        if (memoryUsage > 90) {
            score -= 30;
        } else if (memoryUsage > 75) {
            score -= 15;
        }
        
        // Thread issues score
        @SuppressWarnings("unchecked")
        Map<String, Object> threads = (Map<String, Object>) healthReport.get("threads");
        @SuppressWarnings("unchecked")
        List<String> threadIssues = (List<String>) threads.get("issues");
        score -= threadIssues.size() * 10;
        
        return Math.max(0, score);
    }
    
    /**
     * Get health status from score
     */
    private String getHealthStatus(int score) {
        if (score >= 90) return "EXCELLENT";
        if (score >= 75) return "GOOD";
        if (score >= 50) return "FAIR";
        if (score >= 25) return "POOR";
        return "CRITICAL";
    }
    
    /**
     * Debug session data structure
     */
    private static class DebugSession {
        private final String sessionId;
        private final String sessionName;
        private final String component;
        private final Instant startTime;
        private final String threadName;
        private final List<DebugEvent> events;
        private final Map<String, Object> metadata;
        
        public DebugSession(String sessionId, String sessionName, String component, 
                           Instant startTime, String threadName, List<DebugEvent> events, 
                           Map<String, Object> metadata) {
            this.sessionId = sessionId;
            this.sessionName = sessionName;
            this.component = component;
            this.startTime = startTime;
            this.threadName = threadName;
            this.events = events;
            this.metadata = metadata;
        }
        
        public String getSessionId() { return sessionId; }
        public String getSessionName() { return sessionName; }
        public String getComponent() { return component; }
        public Instant getStartTime() { return startTime; }
        public String getThreadName() { return threadName; }
        public List<DebugEvent> getEvents() { return events; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
    
    /**
     * Debug event data structure
     */
    private static class DebugEvent {
        private final String eventType;
        private final String message;
        private final Instant timestamp;
        private final String threadName;
        private final Map<String, Object> data;
        
        public DebugEvent(String eventType, String message, Instant timestamp, 
                         String threadName, Map<String, Object> data) {
            this.eventType = eventType;
            this.message = message;
            this.timestamp = timestamp;
            this.threadName = threadName;
            this.data = data;
        }
        
        public String getEventType() { return eventType; }
        public String getMessage() { return message; }
        public Instant getTimestamp() { return timestamp; }
        public String getThreadName() { return threadName; }
        public Map<String, Object> getData() { return data; }
    }
}