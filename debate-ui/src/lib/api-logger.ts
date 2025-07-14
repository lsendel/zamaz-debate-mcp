/**
 * API Route Logger - Server-side logging for Next.js API routes
 * This logger is specifically for server-side API routes and should not be used in client components
 */

type LogLevel = 'debug' | 'info' | 'warn' | 'error';

interface ApiLogContext {
  method?: string;
  path?: string;
  statusCode?: number;
  duration?: number;
  error?: Error;
  [key: string]: any;
}

class ApiLogger {
  private isDevelopment = process.env.NODE_ENV === 'development';
  
  private formatMessage(level: LogLevel, message: string, context?: ApiLogContext): string {
    const timestamp = new Date().toISOString();
    const contextStr = context ? ` ${JSON.stringify(context)}` : '';
    return `[${timestamp}] [${level.toUpperCase()}] ${message}${contextStr}`;
  }

  private log(level: LogLevel, message: string, context?: ApiLogContext): void {
    const formattedMessage = this.formatMessage(level, message, context);
    
    // In production, you would send this to a logging service
    if (process.env.NODE_ENV === 'production') {
      // TODO: Send to logging service (e.g., Datadog, CloudWatch, etc.)
      // For now, we'll use console methods that are appropriate for server-side
      /* eslint-disable no-console */
      switch (level) {
        case 'error':
          console.error(formattedMessage);
          break;
        case 'warn':
          console.warn(formattedMessage);
          break;
        default:
          // In production, only log warnings and errors to reduce noise
          break;
      }
      /* eslint-enable no-console */
    } else {
      // In development, log everything to console
      /* eslint-disable no-console */
      switch (level) {
        case 'error':
          console.error(formattedMessage);
          break;
        case 'warn':
          console.warn(formattedMessage);
          break;
        case 'info':
          console.info(formattedMessage);
          break;
        case 'debug':
          console.log(formattedMessage);
          break;
      }
      /* eslint-enable no-console */
    }
  }

  debug(message: string, context?: ApiLogContext): void {
    this.log('debug', message, context);
  }

  info(message: string, context?: ApiLogContext): void {
    this.log('info', message, context);
  }

  warn(message: string, context?: ApiLogContext): void {
    this.log('warn', message, context);
  }

  error(message: string, error?: Error | unknown, context?: ApiLogContext): void {
    const errorContext: ApiLogContext = {
      ...context
    };
    
    if (error instanceof Error) {
      errorContext.error = error;
      errorContext.errorDetails = {
        message: error.message,
        stack: this.isDevelopment ? error.stack : undefined,
        name: error.name
      };
    } else if (error) {
      errorContext.errorDetails = error;
    }
    
    this.log('error', message, errorContext);
  }

  // Helper method for API route logging
  logApiRequest(method: string, path: string, startTime: number, statusCode: number, error?: Error): void {
    const duration = Date.now() - startTime;
    const level = error || statusCode >= 400 ? 'error' : 'info';
    
    this.log(level, `API Request`, {
      method,
      path,
      statusCode,
      duration,
      error
    });
  }
}

// Export singleton instance
export const apiLogger = new ApiLogger();