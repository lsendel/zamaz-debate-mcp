package com.zamaz.mcp.llm.adapter.infrastructure;

import com.zamaz.mcp.llm.domain.service.ProviderSelectionService;
import com.zamaz.mcp.llm.domain.service.ProviderSelectionServiceImpl;
import org.springframework.stereotype.Component;

/**
 * Spring adapter for the provider selection domain service.
 */
@Component
public class ProviderSelectionServiceAdapter extends ProviderSelectionServiceImpl implements ProviderSelectionService {
}