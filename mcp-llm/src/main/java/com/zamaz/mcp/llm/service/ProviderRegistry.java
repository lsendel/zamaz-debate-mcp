package com.zamaz.mcp.llm.service;

import com.zamaz.mcp.llm.provider.LlmProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderRegistry {
    
    private final List<LlmProvider> providers;
    private final Map<String, LlmProvider> providerMap = new HashMap<>();
    
    @PostConstruct
    public void init() {
        providers.forEach(provider -> {
            providerMap.put(provider.getName(), provider);
            log.info("Registered LLM provider: {} (enabled: {})", provider.getName(), provider.isEnabled());
        });
    }
    
    public Optional<LlmProvider> getProvider(String name) {
        return Optional.ofNullable(providerMap.get(name));
    }
    
    public List<LlmProvider> getAllProviders() {
        return providers;
    }
    
    public List<LlmProvider> getEnabledProviders() {
        return providers.stream()
                .filter(LlmProvider::isEnabled)
                .toList();
    }
}