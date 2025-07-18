package com.zamaz.mcp.common.linting.impl;

import com.zamaz.mcp.common.linting.Linter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for managing all available linters.
 */
@Component
public class LinterRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(LinterRegistry.class);
    
    private final Map<String, Linter> lintersByName = new ConcurrentHashMap<>();
    private final Map<String, List<Linter>> lintersByExtension = new ConcurrentHashMap<>();
    
    @Autowired(required = false)
    private List<Linter> availableLinters = new ArrayList<>();
    
    @PostConstruct
    public void initialize() {
        // Register default linters if no Spring-managed linters are found
        if (availableLinters.isEmpty()) {
            registerDefaultLinters();
        }
        
        // Register all available linters
        for (Linter linter : availableLinters) {
            if (linter.isAvailable()) {
                registerLinter(linter);
                logger.info("Registered linter: {} for extensions: {}", 
                    linter.getName(), linter.getSupportedExtensions());
            } else {
                logger.warn("Linter {} is not available on this system", linter.getName());
            }
        }
        
        logger.info("Initialized linter registry with {} linters", lintersByName.size());
    }
    
    private void registerDefaultLinters() {
        // Add default linters
        availableLinters.add(new PythonLinter());
        availableLinters.add(new ShellLinter());
        // Note: Java linters (CheckStyle, SpotBugs, PMD) would be added here
        // TypeScript linters (ESLint, Prettier) would be added here
        // Other linters (YAMLLint, JSONLint, etc.) would be added here
    }
    
    private void registerLinter(Linter linter) {
        lintersByName.put(linter.getName(), linter);
        
        for (String extension : linter.getSupportedExtensions()) {
            lintersByExtension.computeIfAbsent(extension, k -> new ArrayList<>()).add(linter);
        }
    }
    
    /**
     * Get a linter by name.
     */
    public Optional<Linter> getLinter(String name) {
        return Optional.ofNullable(lintersByName.get(name));
    }
    
    /**
     * Get all linters that support a given file extension.
     */
    public List<Linter> getLintersForExtension(String extension) {
        return lintersByExtension.getOrDefault(extension.toLowerCase(), Collections.emptyList());
    }
    
    /**
     * Get all registered linter names.
     */
    public Set<String> getAllLinterNames() {
        return Collections.unmodifiableSet(lintersByName.keySet());
    }
    
    /**
     * Get all supported file extensions.
     */
    public Set<String> getAllSupportedExtensions() {
        return Collections.unmodifiableSet(lintersByExtension.keySet());
    }
    
    /**
     * Check if a linter is available by name.
     */
    public boolean isLinterAvailable(String name) {
        Linter linter = lintersByName.get(name);
        return linter != null && linter.isAvailable();
    }
    
    /**
     * Get linter availability report.
     */
    public Map<String, Boolean> getLinterAvailabilityReport() {
        return lintersByName.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().isAvailable()
            ));
    }
}