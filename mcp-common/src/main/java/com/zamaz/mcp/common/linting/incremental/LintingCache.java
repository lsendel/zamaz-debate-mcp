package com.zamaz.mcp.common.linting.incremental;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zamaz.mcp.common.linting.LintingIssue;

/**
 * Caches linting results to improve performance on subsequent runs.
 */
public class LintingCache {

    private static final Logger logger = LoggerFactory.getLogger(LintingCache.class);

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final CacheStatistics statistics = new CacheStatistics();

    /**
     * Get cached linting result for a file if it exists and is still valid.
     */
    public Optional<List<LintingIssue>> getCachedResult(Path file) {
        try {
            if (!Files.exists(file)) {
                return Optional.empty();
            }

            String fileHash = calculateFileHash(file);
            String cacheKey = file.toString();

            CacheEntry entry = cache.get(cacheKey);
            if (entry == null) {
                statistics.recordMiss();
                return Optional.empty();
            }

            // Check if file has been modified since cached
            if (!entry.getFileHash().equals(fileHash)) {
                cache.remove(cacheKey);
                statistics.recordMiss();
                logger.debug("Cache miss for {} - file modified", file);
                return Optional.empty();
            }

            statistics.recordHit();
            logger.debug("Cache hit for {}", file);
            return Optional.of(entry.getIssues());

        } catch (Exception e) {
            logger.warn("Error checking cache for file {}: {}", file, e.getMessage());
            statistics.recordMiss();
            return Optional.empty();
        }
    }

    /**
     * Cache linting results for a file.
     */
    public void cacheResult(Path file, List<LintingIssue> issues) {
        try {
            if (!Files.exists(file)) {
                return;
            }

            String fileHash = calculateFileHash(file);
            String cacheKey = file.toString();

            CacheEntry entry = new CacheEntry(fileHash, issues, LocalDateTime.now());
            cache.put(cacheKey, entry);

            logger.debug("Cached result for {} with {} issues", file, issues.size());

        } catch (Exception e) {
            logger.warn("Error caching result for file {}: {}", file, e.getMessage());
        }
    }

    /**
     * Clear all cached results.
     */
    public void clear() {
        cache.clear();
        statistics.reset();
        logger.info("Cache cleared");
    }

    /**
     * Remove cached result for a specific file.
     */
    public void invalidate(Path file) {
        String cacheKey = file.toString();
        cache.remove(cacheKey);
        logger.debug("Invalidated cache for {}", file);
    }

    /**
     * Get cache statistics.
     */
    public CacheStatistics getStatistics() {
        return statistics;
    }

    /**
     * Get the number of cached entries.
     */
    public int size() {
        return cache.size();
    }

    /**
     * Calculate hash of file content for cache validation.
     */
    private String calculateFileHash(Path file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        // Include file modification time in hash for additional validation
        FileTime lastModified = Files.getLastModifiedTime(file);
        digest.update(lastModified.toString().getBytes());

        // For small files, hash the content directly
        if (Files.size(file) < 1024 * 1024) { // 1MB
            byte[] content = Files.readAllBytes(file);
            digest.update(content);
        } else {
            // For large files, just use size and modification time
            digest.update(String.valueOf(Files.size(file)).getBytes());
        }

        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Cache entry containing file hash, issues, and timestamp.
     */
    private static class CacheEntry {
        private final String fileHash;
        private final List<LintingIssue> issues;
        private final LocalDateTime timestamp;

        public CacheEntry(String fileHash, List<LintingIssue> issues, LocalDateTime timestamp) {
            this.fileHash = fileHash;
            this.issues = issues;
            this.timestamp = timestamp;
        }

        public String getFileHash() {
            return fileHash;
        }

        public List<LintingIssue> getIssues() {
            return issues;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }
}
