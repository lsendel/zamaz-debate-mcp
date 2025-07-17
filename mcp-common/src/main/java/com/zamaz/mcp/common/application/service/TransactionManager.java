package com.zamaz.mcp.common.application.service;

import java.util.function.Supplier;

/**
 * Application service for managing transactions.
 * This interface abstracts transaction management from the application layer.
 * The implementation belongs in the infrastructure layer.
 */
public interface TransactionManager {
    
    /**
     * Executes a function within a transaction.
     * If the function throws an exception, the transaction is rolled back.
     * 
     * @param <T> the return type
     * @param function the function to execute
     * @return the result of the function
     */
    <T> T executeInTransaction(Supplier<T> function);
    
    /**
     * Executes a runnable within a transaction.
     * If the runnable throws an exception, the transaction is rolled back.
     * 
     * @param runnable the runnable to execute
     */
    void executeInTransaction(Runnable runnable);
    
    /**
     * Executes a function within a new transaction.
     * This creates a new transaction even if one already exists.
     * 
     * @param <T> the return type
     * @param function the function to execute
     * @return the result of the function
     */
    <T> T executeInNewTransaction(Supplier<T> function);
    
    /**
     * Executes a function within a read-only transaction.
     * This is optimized for read operations.
     * 
     * @param <T> the return type
     * @param function the function to execute
     * @return the result of the function
     */
    <T> T executeInReadOnlyTransaction(Supplier<T> function);
}