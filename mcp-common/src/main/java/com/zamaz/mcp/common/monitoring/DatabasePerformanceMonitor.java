package com.zamaz.mcp.common.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Database performance monitoring component
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabasePerformanceMonitor {
    
    private final MeterRegistry meterRegistry;
    
    @Autowired(required = false)
    private DataSource dataSource;
    
    // Performance tracking
    private final ConcurrentHashMap<String, DatabaseOperationStats> operationStats = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SlowQueryInfo> slowQueries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> connectionStats = new ConcurrentHashMap<>();
    
    // Database metrics
    private final ConcurrentHashMap<String, Double> databaseMetrics = new ConcurrentHashMap<>();
    
    // Thresholds
    private static final long SLOW_QUERY_THRESHOLD = 1000; // 1 second
    private static final long VERY_SLOW_QUERY_THRESHOLD = 5000; // 5 seconds
    private static final int MAX_SLOW_QUERIES = 100;
    
    /**
     * Record database operation performance
     */
    public void recordOperation(String operation, String table, String query, long duration, 
                              boolean success, int rowsAffected, String errorMessage) {
        String key = operation + ":" + table;
        
        // Update operation statistics
        operationStats.computeIfAbsent(key, k -> new DatabaseOperationStats(operation, table))
            .record(duration, success, rowsAffected, errorMessage);
        
        // Record slow queries
        if (duration > SLOW_QUERY_THRESHOLD) {
            recordSlowQuery(operation, table, query, duration, success, errorMessage);
        }
        
        // Update metrics
        meterRegistry.timer("database.operation.duration", 
            "operation", operation, "table", table, "success", String.valueOf(success))
            .record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        meterRegistry.counter("database.operation.count", 
            "operation", operation, "table", table, "success", String.valueOf(success))
            .increment();
        
        if (rowsAffected > 0) {
            meterRegistry.counter("database.rows.affected", 
                "operation", operation, "table", table)
                .increment(rowsAffected);
        }
        
        if (!success) {
            meterRegistry.counter("database.operation.errors", 
                "operation", operation, "table", table)
                .increment();
        }
    }
    
    /**
     * Record connection pool metrics
     */
    public void recordConnectionPool(String poolName, int active, int idle, int total, 
                                   int waiting, int max) {
        meterRegistry.gauge("database.connection.pool.active", 
            io.micrometer.core.instrument.Tags.of("pool", poolName), active);
        
        meterRegistry.gauge("database.connection.pool.idle", 
            io.micrometer.core.instrument.Tags.of("pool", poolName), idle);
        
        meterRegistry.gauge("database.connection.pool.total", 
            io.micrometer.core.instrument.Tags.of("pool", poolName), total);
        
        meterRegistry.gauge("database.connection.pool.waiting", 
            io.micrometer.core.instrument.Tags.of("pool", poolName), waiting);
        
        meterRegistry.gauge("database.connection.pool.max", 
            io.micrometer.core.instrument.Tags.of("pool", poolName), max);
        
        meterRegistry.gauge("database.connection.pool.utilization", 
            io.micrometer.core.instrument.Tags.of("pool", poolName), 
            max > 0 ? (double) active / max : 0.0);
    }
    
    /**
     * Record transaction metrics
     */
    public void recordTransaction(String transactionId, long duration, boolean success, 
                                int operationCount, String errorMessage) {
        meterRegistry.timer("database.transaction.duration", 
            "success", String.valueOf(success))
            .record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        meterRegistry.counter("database.transaction.count", 
            "success", String.valueOf(success))
            .increment();
        
        meterRegistry.counter("database.transaction.operations", 
            "success", String.valueOf(success))
            .increment(operationCount);
        
        if (!success) {
            meterRegistry.counter("database.transaction.errors")
                .increment();
        }
    }
    
    /**
     * Get database performance summary
     */
    public DatabasePerformanceSummary getPerformanceSummary() {
        Map<String, DatabaseOperationMetrics> metrics = new HashMap<>();
        
        operationStats.forEach((key, stats) -> {
            metrics.put(key, DatabaseOperationMetrics.builder()
                .operation(stats.getOperation())
                .table(stats.getTable())
                .totalOperations(stats.getTotalOperations())
                .successfulOperations(stats.getSuccessfulOperations())
                .errorOperations(stats.getErrorOperations())
                .averageDuration(stats.getAverageDuration())
                .minDuration(stats.getMinDuration())
                .maxDuration(stats.getMaxDuration())
                .p95Duration(stats.getP95Duration())
                .p99Duration(stats.getP99Duration())
                .totalRowsAffected(stats.getTotalRowsAffected())
                .errorRate(stats.getErrorRate())
                .throughput(stats.getThroughput())
                .build());
        });
        
        return DatabasePerformanceSummary.builder()
            .timestamp(LocalDateTime.now())
            .operationMetrics(metrics)
            .slowQueries(new ArrayList<>(slowQueries.values()))
            .topSlowQueries(getTopSlowQueries())
            .databaseMetrics(new HashMap<>(databaseMetrics))
            .build();
    }
    
    /**
     * Get top slow queries
     */
    public List<SlowQueryInfo> getTopSlowQueries() {
        return slowQueries.values().stream()
            .sorted(Comparator.comparingLong(SlowQueryInfo::getMaxDuration).reversed())
            .limit(10)
            .toList();
    }
    
    /**
     * Get operation statistics
     */
    public DatabaseOperationStats getOperationStats(String operation, String table) {
        return operationStats.get(operation + ":" + table);
    }
    
    /**
     * Get all monitored operations
     */
    public Set<String> getAllOperations() {
        return operationStats.keySet();
    }
    
    /**
     * Clear performance statistics
     */
    public void clearStatistics() {
        operationStats.clear();
        slowQueries.clear();
        connectionStats.clear();
        databaseMetrics.clear();
        
        log.info("Database performance statistics cleared");
    }
    
    /**
     * Monitor database metrics periodically
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void monitorDatabaseMetrics() {
        if (dataSource == null) {
            log.debug("No DataSource configured, skipping database metrics collection");
            return;
        }
        
        try {
            // Collect database-specific metrics
            collectDatabaseMetrics();
            
            // Update performance metrics
            updatePerformanceMetrics();
            
            // Check for performance issues
            checkPerformanceIssues();
            
            // Clean up old data
            cleanupOldData();
            
        } catch (Exception e) {
            log.error("Error monitoring database metrics", e);
        }
    }
    
    /**
     * Check database connectivity
     */
    public boolean checkDatabaseConnectivity() {
        if (dataSource == null) {
            return false;
        }
        
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5000); // 5 second timeout
        } catch (SQLException e) {
            log.error("Database connectivity check failed", e);
            return false;
        }
    }
    
    /**
     * Get database metadata
     */
    public DatabaseMetadata getDatabaseMetadata() {
        if (dataSource == null) {
            return null;
        }
        
        try (Connection connection = dataSource.getConnection()) {
            var metaData = connection.getMetaData();
            
            return DatabaseMetadata.builder()
                .databaseProductName(metaData.getDatabaseProductName())
                .databaseProductVersion(metaData.getDatabaseProductVersion())
                .driverName(metaData.getDriverName())
                .driverVersion(metaData.getDriverVersion())
                .url(metaData.getURL())
                .username(metaData.getUserName())
                .maxConnections(metaData.getMaxConnections())
                .supportsTransactions(metaData.supportsTransactions())
                .build();
            
        } catch (SQLException e) {
            log.error("Failed to get database metadata", e);
            return null;
        }
    }
    
    private void recordSlowQuery(String operation, String table, String query, long duration, 
                               boolean success, String errorMessage) {
        String key = operation + ":" + table + ":" + query.hashCode();
        
        slowQueries.compute(key, (k, existing) -> {
            if (existing == null) {
                return SlowQueryInfo.builder()
                    .operation(operation)
                    .table(table)
                    .query(truncateQuery(query))
                    .firstOccurrence(LocalDateTime.now())
                    .lastOccurrence(LocalDateTime.now())
                    .occurrenceCount(1)
                    .maxDuration(duration)
                    .averageDuration(duration)
                    .success(success)
                    .errorMessage(errorMessage)
                    .build();
            } else {
                existing.setLastOccurrence(LocalDateTime.now());
                existing.setOccurrenceCount(existing.getOccurrenceCount() + 1);
                existing.setMaxDuration(Math.max(existing.getMaxDuration(), duration));
                existing.setAverageDuration(
                    (existing.getAverageDuration() * (existing.getOccurrenceCount() - 1) + duration) / 
                    existing.getOccurrenceCount()
                );
                
                if (!success) {
                    existing.setSuccess(false);
                    existing.setErrorMessage(errorMessage);
                }
                
                return existing;
            }
        });
        
        // Limit slow queries collection size
        if (slowQueries.size() > MAX_SLOW_QUERIES) {
            // Remove oldest entries
            slowQueries.entrySet().stream()
                .sorted(Map.Entry.<String, SlowQueryInfo>comparingByValue(
                    Comparator.comparing(SlowQueryInfo::getLastOccurrence)))
                .limit(slowQueries.size() - MAX_SLOW_QUERIES + 10)
                .forEach(entry -> slowQueries.remove(entry.getKey()));
        }
        
        String level = duration > VERY_SLOW_QUERY_THRESHOLD ? "CRITICAL" : "WARNING";
        log.warn("Slow query detected [{}]: {} on {} took {}ms", level, operation, table, duration);
    }
    
    private void collectDatabaseMetrics() {
        try (Connection connection = dataSource.getConnection()) {
            // Connection validity
            boolean isValid = connection.isValid(5000);
            databaseMetrics.put("connectivity", isValid ? 1.0 : 0.0);
            
            // Database-specific metrics (PostgreSQL example)
            collectPostgreSQLMetrics(connection);
            
        } catch (SQLException e) {
            log.error("Failed to collect database metrics", e);
            databaseMetrics.put("connectivity", 0.0);
        }
    }
    
    private void collectPostgreSQLMetrics(Connection connection) {
        try {
            // Active connections
            try (PreparedStatement ps = connection.prepareStatement(
                "SELECT count(*) FROM pg_stat_activity WHERE state = 'active'")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    databaseMetrics.put("active_connections", rs.getDouble(1));
                }
            }
            
            // Database size
            try (PreparedStatement ps = connection.prepareStatement(
                "SELECT pg_database_size(current_database())")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    databaseMetrics.put("database_size_bytes", rs.getDouble(1));
                }
            }
            
            // Table statistics
            try (PreparedStatement ps = connection.prepareStatement(
                "SELECT sum(n_tup_ins) as inserts, sum(n_tup_upd) as updates, " +
                "sum(n_tup_del) as deletes, sum(seq_scan) as seq_scans, " +
                "sum(idx_scan) as idx_scans FROM pg_stat_user_tables")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    databaseMetrics.put("total_inserts", rs.getDouble("inserts"));
                    databaseMetrics.put("total_updates", rs.getDouble("updates"));
                    databaseMetrics.put("total_deletes", rs.getDouble("deletes"));
                    databaseMetrics.put("sequential_scans", rs.getDouble("seq_scans"));
                    databaseMetrics.put("index_scans", rs.getDouble("idx_scans"));
                }
            }
            
            // Lock information
            try (PreparedStatement ps = connection.prepareStatement(
                "SELECT mode, count(*) as count FROM pg_locks GROUP BY mode")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String mode = rs.getString("mode");
                    double count = rs.getDouble("count");
                    databaseMetrics.put("locks_" + mode.toLowerCase(), count);
                }
            }
            
        } catch (SQLException e) {
            log.debug("Failed to collect PostgreSQL-specific metrics", e);
        }
    }
    
    private void updatePerformanceMetrics() {
        // Update operation metrics
        operationStats.forEach((key, stats) -> {
            meterRegistry.gauge("database.operation.average_duration", 
                io.micrometer.core.instrument.Tags.of("operation", key), 
                stats.getAverageDuration());
            
            meterRegistry.gauge("database.operation.error_rate", 
                io.micrometer.core.instrument.Tags.of("operation", key), 
                stats.getErrorRate());
            
            meterRegistry.gauge("database.operation.throughput", 
                io.micrometer.core.instrument.Tags.of("operation", key), 
                stats.getThroughput());
        });
        
        // Update database metrics
        databaseMetrics.forEach((metric, value) -> {
            meterRegistry.gauge("database.metric." + metric, value);
        });
        
        // Update slow queries count
        meterRegistry.gauge("database.slow_queries.count", slowQueries.size());
    }
    
    private void checkPerformanceIssues() {
        // Check for high error rates
        operationStats.forEach((key, stats) -> {
            if (stats.getErrorRate() > 0.05) { // 5% error rate
                log.warn("High database error rate for operation {}: {:.2f}%", 
                    key, stats.getErrorRate() * 100);
            }
            
            if (stats.getAverageDuration() > SLOW_QUERY_THRESHOLD) {
                log.warn("High average duration for operation {}: {}ms", 
                    key, stats.getAverageDuration());
            }
        });
        
        // Check connectivity
        Double connectivity = databaseMetrics.get("connectivity");
        if (connectivity != null && connectivity < 1.0) {
            log.error("Database connectivity issues detected");
        }
    }
    
    private void cleanupOldData() {
        // Remove old slow queries
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        
        slowQueries.entrySet().removeIf(entry -> 
            entry.getValue().getLastOccurrence().isBefore(cutoff));
    }
    
    private String truncateQuery(String query) {
        if (query == null) return "N/A";
        return query.length() > 500 ? query.substring(0, 500) + "..." : query;
    }
    
    // Data classes
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class DatabaseOperationMetrics {
        private String operation;
        private String table;
        private long totalOperations;
        private long successfulOperations;
        private long errorOperations;
        private double averageDuration;
        private long minDuration;
        private long maxDuration;
        private double p95Duration;
        private double p99Duration;
        private long totalRowsAffected;
        private double errorRate;
        private double throughput;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class DatabasePerformanceSummary {
        private LocalDateTime timestamp;
        private Map<String, DatabaseOperationMetrics> operationMetrics;
        private List<SlowQueryInfo> slowQueries;
        private List<SlowQueryInfo> topSlowQueries;
        private Map<String, Double> databaseMetrics;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class SlowQueryInfo {
        private String operation;
        private String table;
        private String query;
        private LocalDateTime firstOccurrence;
        private LocalDateTime lastOccurrence;
        private int occurrenceCount;
        private long maxDuration;
        private long averageDuration;
        private boolean success;
        private String errorMessage;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class DatabaseMetadata {
        private String databaseProductName;
        private String databaseProductVersion;
        private String driverName;
        private String driverVersion;
        private String url;
        private String username;
        private int maxConnections;
        private boolean supportsTransactions;
    }
    
    public static class DatabaseOperationStats {
        private final String operation;
        private final String table;
        private long totalOperations = 0;
        private long successfulOperations = 0;
        private long errorOperations = 0;
        private long totalDuration = 0;
        private long minDuration = Long.MAX_VALUE;
        private long maxDuration = 0;
        private long totalRowsAffected = 0;
        private final List<Long> durations = new ArrayList<>();
        private long lastUpdateTime = System.currentTimeMillis();
        private long recentOperations = 0;
        
        public DatabaseOperationStats(String operation, String table) {
            this.operation = operation;
            this.table = table;
        }
        
        public synchronized void record(long duration, boolean success, int rowsAffected, String errorMessage) {
            totalOperations++;
            totalDuration += duration;
            
            if (success) {
                successfulOperations++;
            } else {
                errorOperations++;
            }
            
            minDuration = Math.min(minDuration, duration);
            maxDuration = Math.max(maxDuration, duration);
            totalRowsAffected += rowsAffected;
            
            durations.add(duration);
            if (durations.size() > 1000) {
                durations.removeFirst();
            }
            
            // Track recent operations
            long now = System.currentTimeMillis();
            if (now - lastUpdateTime < 60000) { // Within last minute
                recentOperations++;
            } else {
                recentOperations = 1;
                lastUpdateTime = now;
            }
        }
        
        public synchronized double getAverageDuration() {
            return totalOperations > 0 ? (double) totalDuration / totalOperations : 0.0;
        }
        
        public synchronized double getErrorRate() {
            return totalOperations > 0 ? (double) errorOperations / totalOperations : 0.0;
        }
        
        public synchronized double getThroughput() {
            return recentOperations / 60.0; // operations per second
        }
        
        public synchronized double getP95Duration() {
            if (durations.isEmpty()) return 0.0;
            
            List<Long> sorted = new ArrayList<>(durations);
            sorted.sort(Long::compareTo);
            
            int index = (int) (sorted.size() * 0.95);
            return sorted.get(Math.min(index, sorted.size() - 1));
        }
        
        public synchronized double getP99Duration() {
            if (durations.isEmpty()) return 0.0;
            
            List<Long> sorted = new ArrayList<>(durations);
            sorted.sort(Long::compareTo);
            
            int index = (int) (sorted.size() * 0.99);
            return sorted.get(Math.min(index, sorted.size() - 1));
        }
        
        // Getters
        public String getOperation() { return operation; }
        public String getTable() { return table; }
        public long getTotalOperations() { return totalOperations; }
        public long getSuccessfulOperations() { return successfulOperations; }
        public long getErrorOperations() { return errorOperations; }
        public long getMinDuration() { return minDuration == Long.MAX_VALUE ? 0 : minDuration; }
        public long getMaxDuration() { return maxDuration; }
        public long getTotalRowsAffected() { return totalRowsAffected; }
        public long getRecentOperations() { return recentOperations; }
    }
}