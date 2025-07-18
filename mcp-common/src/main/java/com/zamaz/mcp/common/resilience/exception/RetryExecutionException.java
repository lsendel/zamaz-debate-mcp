package com.zamaz.mcp.common.resilience.exception;

/**
 * Exception thrown when retry execution fails.
 */
public class RetryExecutionException extends RuntimeException {
    
    private final String retryName;
    private final int attemptsMade;
    private final Throwable originalException;
    
    public RetryExecutionException(String retryName, int attemptsMade, Throwable originalException) {
        super(String.format("Retry '%s' failed after %d attempts: %s", 
              retryName, attemptsMade, originalException.getMessage()), originalException);
        this.retryName = retryName;
        this.attemptsMade = attemptsMade;
        this.originalException = originalException;
    }
    
    public String getRetryName() {
        return retryName;
    }
    
    public int getAttemptsMade() {
        return attemptsMade;
    }
    
    public Throwable getOriginalException() {
        return originalException;
    }
}