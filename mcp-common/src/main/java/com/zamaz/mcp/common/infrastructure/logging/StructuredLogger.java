package com.zamaz.mcp.common.infrastructure.logging;

/**
 * Structured logger interface for consistent logging across all layers.
 * Provides fluent API for building structured log messages with context.
 */
public interface StructuredLogger {

    /**
     * Create a debug log entry builder.
     */
    LogEntryBuilder debug(String message);

    /**
     * Create an info log entry builder.
     */
    LogEntryBuilder info(String message);

    /**
     * Create a warn log entry builder.
     */
    LogEntryBuilder warn(String message);

    /**
     * Create an error log entry builder.
     */
    LogEntryBuilder error(String message);

    /**
     * Create an error log entry builder with exception.
     */
    LogEntryBuilder error(String message, Throwable throwable);

    /**
     * Fluent builder for log entries.
     */
    interface LogEntryBuilder {

        /**
         * Add a field to the log entry.
         */
        LogEntryBuilder field(String key, Object value);

        /**
         * Add organization context.
         */
        LogEntryBuilder organizationId(String organizationId);

        /**
         * Add user context.
         */
        LogEntryBuilder userId(String userId);

        /**
         * Add request context.
         */
        LogEntryBuilder requestId(String requestId);

        /**
         * Add correlation context.
         */
        LogEntryBuilder correlationId(String correlationId);

        /**
         * Add operation context.
         */
        LogEntryBuilder operation(String operation);

        /**
         * Add resource context.
         */
        LogEntryBuilder resource(String resourceType, String resourceId);

        /**
         * Add duration context.
         */
        LogEntryBuilder duration(long durationMs);

        /**
         * Add exception context.
         */
        LogEntryBuilder exception(Throwable throwable);

        /**
         * Execute the log entry.
         */
        void log();
    }
}