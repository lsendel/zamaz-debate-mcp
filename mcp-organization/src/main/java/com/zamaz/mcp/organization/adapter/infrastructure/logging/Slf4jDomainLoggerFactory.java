package com.zamaz.mcp.organization.adapter.infrastructure.logging;

import com.zamaz.mcp.common.infrastructure.logging.DomainLogger;
import com.zamaz.mcp.common.infrastructure.logging.DomainLoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SLF4J implementation of DomainLoggerFactory.
 * Creates domain loggers backed by SLF4J.
 */
@Component
public class Slf4jDomainLoggerFactory implements DomainLoggerFactory {
    
    @Override
    public DomainLogger getLogger(Class<?> clazz) {
        return new Slf4jDomainLogger(org.slf4j.LoggerFactory.getLogger(clazz));
    }
    
    @Override
    public DomainLogger getLogger(String name) {
        return new Slf4jDomainLogger(org.slf4j.LoggerFactory.getLogger(name));
    }
}