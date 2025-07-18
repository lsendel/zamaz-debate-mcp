package com.zamaz.mcp.common.linting.incremental;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Statistics for linting cache performance.
 */
public class CacheStatistics {

    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final LocalDateTime startTime = LocalDateTime.now();

    /**
     * Record a cache hit.
     */
    public void recordHit() {
        hits.incrementAndGet();
    }

    /**
     * Record a cache miss.
     */
    public void recordMiss() {
        misses.incrementAndGet();
    }

    /**
     * Get the number of cache hits.
     */
    public long getHits() {
        return hits.get();
    }

    /**
     * Get the number of cache misses.
     */
    public long getMisses() {
        return misses.get();
    }

    /**
     * Get the total number of cache requests.
     */
    public long getTotal() {
        return hits.get() + misses.get();
    }

    /**
     * Get the cache hit rate as a percentage.
     */
    public double getHitRate() {
        long total = getTotal();
        return total > 0 ? (double) hits.get() / total : 0.0;
    }

    /**
     * Get the cache miss rate as a percentage.
     */
    public double getMissRate() {
        return 1.0 - getHitRate();
    }

    /**
     * Get the time when statistics collection started.
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }

    /**
     * Reset all statistics.
     */
    public void reset() {
        hits.set(0);
        misses.set(0);
    }

    @Override
    public String toString() {
        return String.format("CacheStatistics{hits=%d, misses=%d, hitRate=%.2f%%, total=%d}",
                getHits(), getMisses(), getHitRate() * 100, getTotal());
    }
}
