/**
 * Logger utility for production-safe logging
 * Replaces console.log statements to prevent information leakage
 */

/* eslint-disable no-console */

type LogLevel = 'debug' | 'info' | 'warn' | 'error';

interface LogContext {
  [key: string]: any;
}

class Logger {
  private isDevelopment = process.env.NODE_ENV === 'development';
  private logLevel: LogLevel = (process.env.NEXT_PUBLIC_LOG_LEVEL as LogLevel) || 'info';
  
  private logLevels: Record<LogLevel, number> = {
    debug: 0,
    info: 1,
    warn: 2,
    error: 3
  };

  private shouldLog(level: LogLevel): boolean {
    // Validate level exists in logLevels to prevent object injection
    if (!Object.hasOwn(this.logLevels, level)) {
      return false;
    }
    // eslint-disable-next-line security/detect-object-injection
    return this.logLevels[level] >= this.logLevels[this.logLevel];
  }

  private formatMessage(level: LogLevel, message: string, context?: LogContext): string {
    const timestamp = new Date().toISOString();
    const contextStr = context ? ` ${JSON.stringify(context)}` : '';
    return `[${timestamp}] [${level.toUpperCase()}] ${message}${contextStr}`;
  }

  debug(message: string, context?: LogContext): void {
    if (this.isDevelopment && this.shouldLog('debug')) {
      console.log(this.formatMessage('debug', message, context));
    }
  }

  info(message: string, context?: LogContext): void {
    if (this.shouldLog('info')) {
      if (this.isDevelopment) {
        console.log(this.formatMessage('info', message, context));
      } else {
        // In production, could send to logging service
        // Example: sendToLoggingService('info', message, context);
      }
    }
  }

  warn(message: string, context?: LogContext): void {
    if (this.shouldLog('warn')) {
      console.warn(this.formatMessage('warn', message, context));
    }
  }

  error(message: string, error?: Error | any, context?: LogContext): void {
    if (this.shouldLog('error')) {
      const errorContext = {
        ...context,
        ...(error && {
          errorMessage: error.message || String(error),
          errorStack: error.stack
        })
      };
      console.error(this.formatMessage('error', message, errorContext));
      
      // In production, send to error tracking service
      if (!this.isDevelopment && typeof window !== 'undefined') {
        // Example: Sentry.captureException(error);
      }
    }
  }

  // Group related logs
  group(label: string): void {
    if (this.isDevelopment) {
      console.group(label);
    }
  }

  groupEnd(): void {
    if (this.isDevelopment) {
      console.groupEnd();
    }
  }

  // Performance logging
  time(label: string): void {
    if (this.isDevelopment) {
      console.time(label);
    }
  }

  timeEnd(label: string): void {
    if (this.isDevelopment) {
      console.timeEnd(label);
    }
  }

  // Table logging for debugging
  table(data: any): void {
    if (this.isDevelopment) {
      console.table(data);
    }
  }
}

// Export singleton instance
export const logger = new Logger();

// Export for testing purposes
export { Logger };

// Convenience exports
export const { debug, info, warn, error, group, groupEnd, time, timeEnd, table } = logger;

/* eslint-enable no-console */