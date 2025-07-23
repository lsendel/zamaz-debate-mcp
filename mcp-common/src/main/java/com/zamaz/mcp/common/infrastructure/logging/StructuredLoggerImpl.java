package com.zamaz.mcp.common.infrastructure.logging;

import com.zamaz.mcp.common.logging.LogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of StructuredLogger that provides fluent API for structured
 * logging.
 */
public class StructuredLoggerImpl implements StructuredLogger {

    private final Logger logger;

    public StructuredLoggerImpl(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    public StructuredLoggerImpl(String name) {
        this.logger = LoggerFactory.getLogger(name);
    }

    @Override
    public LogEntryBuilder debug(String message) {
        return new LogEntryBuilderImpl(logger, LogLevel.DEBUG, message);
    }

    @Override
    public LogEntryBuilder info(String message) {
        return new LogEntryBuilderImpl(logger, LogLevel.INFO, message);
    }

    @Override
    public LogEntryBuilder warn(String message) {
        return new LogEntryBuilderImpl(logger, LogLevel.WARN, message);
    }

    @Override
    public LogEntryBuilder error(String message) {
        return new LogEntryBuilderImpl(logger, LogLevel.ERROR, message);
    }

    @Override
    public LogEntryBuilder error(String message, Throwable throwable) {
        return new LogEntryBuilderImpl(logger, LogLevel.ERROR, message).exception(throwable);
    }

    private enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }

    private static class LogEntryBuilderImpl implements LogEntryBuilder {

        private final Logger logger;
        private final LogLevel level;
        private final String message;
        private final Map<String, Object> fields = new HashMap<>();
        private Throwable throwable;

        public LogEntryBuilderImpl(Logger logger, LogLevel level, String message) {
            this.logger = logger;
            this.level = level;
            this.message = message;
        }

        @Override
        public LogEntryBuilder field(String key, Object value) {
            if (key != null && value != null) {
                fields.put(key, value);
            }
            return this;
        }

        @Override
        public LogEntryBuilder organizationId(String organizationId) {
            return field("organizationId", organizationId);
        }

        @Override
        public LogEntryBuilder userId(String userId) {
            return field("userId", userId);
        }

        @Override
        public LogEntryBuilder requestId(String requestId) {
            return field("requestId", requestId);
        }

        @Override
        public LogEntryBuilder correlationId(String correlationId) {
            return field("correlationId", correlationId);
        }

        @Override
        public LogEntryBuilder operation(String operation) {
            return field("operation", operation);
        }

        @Override
        public LogEntryBuilder resource(String resourceType, String resourceId) {
            return field("resourceType", resourceType).field("resourceId", resourceId);
        }

        @Override
        public LogEntryBuilder duration(long durationMs) {
            return field("duration", durationMs);
        }

        @Override
        public LogEntryBuilder exception(Throwable throwable) {
            this.throwable = throwable;
            return field("exceptionType", throwable.getClass().getSimpleName())
                    .field("exceptionMessage", throwable.getMessage());
        }

        @Override
        public void log() {
            // Add fields to MDC for structured logging
            String originalMdc = MDC.getCopyOfContextMap() != null ? MDC.getCopyOfContextMap().toString() : null;

            try {
                // Add all fields to MDC
                fields.forEach((key, value) -> {
                    if (value != null) {
                        MDC.put(key, value.toString());
                    }
                });

                // Log the message
                switch (level) {
                    case DEBUG:
                        if (throwable != null) {
                            logger.debug(message, throwable);
                        } else {
                            logger.debug(message);
                        }
                        break;
                    case INFO:
                        if (throwable != null) {
                            logger.info(message, throwable);
                        } else {
                            logger.info(message);
                        }
                        break;
                    case WARN:
                        if (throwable != null) {
                            logger.warn(message, throwable);
                        } else {
                            logger.warn(message);
                        }
                        break;
                    case ERROR:
                        if (throwable != null) {
                            logger.error(message, throwable);
                        } else {
                            logger.error(message);
                        }
                        break;
                }
            } finally {
                // Clean up MDC
                fields.keySet().forEach(MDC::remove);
            }
        }
    }
}