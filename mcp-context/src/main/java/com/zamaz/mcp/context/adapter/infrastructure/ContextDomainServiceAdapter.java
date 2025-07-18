package com.zamaz.mcp.context.adapter.infrastructure;

import com.zamaz.mcp.context.domain.service.ContextDomainService;
import com.zamaz.mcp.context.domain.service.ContextDomainServiceImpl;
import org.springframework.stereotype.Component;

/**
 * Spring adapter for the context domain service.
 */
@Component
public class ContextDomainServiceAdapter extends ContextDomainServiceImpl implements ContextDomainService {
}