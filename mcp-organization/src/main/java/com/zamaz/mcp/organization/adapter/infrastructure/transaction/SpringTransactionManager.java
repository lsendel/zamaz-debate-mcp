package com.zamaz.mcp.organization.adapter.infrastructure.transaction;

import com.zamaz.mcp.common.application.service.TransactionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

/**
 * Spring implementation of TransactionManager.
 * Uses Spring's @Transactional annotation for transaction management.
 */
@Component
@RequiredArgsConstructor
public class SpringTransactionManager implements TransactionManager {
    
    @Override
    @Transactional
    public <T> T executeInTransaction(Supplier<T> function) {
        return function.get();
    }
    
    @Override
    @Transactional
    public void executeInTransaction(Runnable runnable) {
        runnable.run();
    }
    
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> T executeInNewTransaction(Supplier<T> function) {
        return function.get();
    }
    
    @Override
    @Transactional(readOnly = true)
    public <T> T executeInReadOnlyTransaction(Supplier<T> function) {
        return function.get();
    }
}