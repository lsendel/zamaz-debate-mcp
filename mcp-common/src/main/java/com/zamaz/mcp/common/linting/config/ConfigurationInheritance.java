package com.zamaz.mcp.common.linting.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import com.zamaz.mcp.common.linting.LinterConfig;
import com.zamaz.mcp.common.linting.LintingConfiguration;
import com.zamaz.mcp.common.linting.QualityThresholds;

/**
 * Handles configuration inheritance and overrides for service-specific linting
 * configurations.
 */
@Component
public class ConfigurationInheritance {

    private final Yaml yaml = new Yaml();

    /**
     * Load and merge configurations with inheritance.
     *
     * @param projectRoot the project root directory
     * @param serviceName the service name (optional, for service-specific config)
     * @return merged linting configuration
     */
    public LintingConfiguration loadConfiguration(Path projectRoot, String serviceName) {
        try {
            // Load global configuration
            LintingConfiguration globalConfig = loadGlobalConfiguration(projectRoot);

            if (serviceName == null) {
                return globalConfig;
            }

            // Load service-specific configuration if it exists
            LintingConfiguration serviceConfig = loadServiceConfiguration(projectRoot, serviceName);

            // Merge configurations (service overrides global)
            return mergeConfigurations(globalConfig, serviceConfig);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load linting configuration", e);
        }
    }

    /**
     * Load global linting configuration.
     */
    private LintingConfiguration loadGlobalConfiguration(Path projectRoot) throws IOException {
        Path globalConfigPath = projectRoot.resolve(".linting/global.yml");

        if (!Files.exists(globalConfigPath)) {
            return createDefaultConfiguration();
        }

        String content = Files.readString(globalConfigPath);
        Map<String, Object> configMap = yaml.load(content);

        return parseConfiguration(configMap);
    }

    /**
     * Load service-specific linting configuration.
     */
    private LintingConfiguration loadServiceConfiguration(Path projectRoot, String serviceName) throws IOException {
        Path serviceConfigPath = projectRoot.resolve(".linting/services/" + serviceName + "/config.yml");

        if (!Files.exists(serviceConfigPath)) {
            return null; // No service-specific configuration
        }

        String content = Files.readString(serviceConfigPath);
        Map<String, Object> configMap = yaml.load(content);

        return parseConfiguration(configMap);
    }

    /**
     * Merge two configurations, with override taking precedence.
     */
    private LintingConfiguration mergeConfigurations(LintingConfiguration base, LintingConfiguration override) {
        if (override == null) {
            return base;
        }

        // Merge linters
        Map<String, LinterConfig> mergedLinters = new HashMap<>(base.getLinters());
        if (override.getLinters() != null) {
            mergedLinters.putAll(override.getLinters());
        }

        // Merge exclude patterns
        List<String> mergedExcludePatterns = new ArrayList<>(base.getExcludePatterns());
        if (override.getExcludePatterns() != null) {
            mergedExcludePatterns.addAll(override.getExcludePatterns());
        }

        // Merge global settings
        Map<String, Object> mergedGlobalSettings = new HashMap<>(base.getGlobalSettings());
        if (override.getGlobalSettings() != null) {
            mergedGlobalSettings.putAll(override.getGlobalSettings());
        }

        // Use override thresholds if present, otherwise base
        QualityThresholds mergedThresholds = override.getThresholds() != null ? override.getThresholds()
                : base.getThresholds();

        return LintingConfiguration.builder()
                .linters(mergedLinters)
                .excludePatterns(mergedExcludePatterns)
                .globalSettings(mergedGlobalSettings)
                .thresholds(mergedThresholds)
                .parallelExecution(override.isParallelExecution())
                .maxThreads(override.getMaxThreads() > 0 ? override.getMaxThreads() : base.getMaxThreads())
                .build();
    }

    /**
     * Parse configuration from YAML map.
     */
    private LintingConfiguration parseConfiguration(Map<String, Object> configMap) {
        if (configMap == null) {
            return createDefaultConfiguration();
        }

        // Parse linters
        Map<String, LinterConfig> linters = parseLinters(configMap);

        // Parse exclude patterns
        List<String> excludePatterns = parseExcludePatterns(configMap);

        // Parse global settings
        Map<String, Object> globalSettings = parseGlobalSettings(configMap);

        // Parse quality thresholds
        QualityThresholds thresholds = parseQualityThresholds(configMap);

        // Parse execution settings
        boolean parallelExecution = parseBoolean(configMap, "global.parallel_execution", true);
        int maxThreads = parseInt(configMap, "global.max_threads", 4);

        return LintingConfiguration.builder()
                .linters(linters)
                .excludePatterns(excludePatterns)
                .globalSettings(globalSettings)
                .thresholds(thresholds)
                .parallelExecution(parallelExecution)
                .maxThreads(maxThreads)
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, LinterConfig> parseLinters(Map<String, Object> configMap) {
        Map<String, LinterConfig> linters = new HashMap<>();

        // Default linters
        linters.put("checkstyle", LinterConfig.builder()
                .name("checkstyle")
                .enabled(true)
                .configFile(".linting/java/checkstyle.xml")
                .build());

        linters.put("spotbugs", LinterConfig.builder()
                .name("spotbugs")
                .enabled(true)
                .configFile(".linting/java/spotbugs-exclude.xml")
                .build());

        linters.put("pmd", LinterConfig.builder()
                .name("pmd")
                .enabled(true)
                .configFile(".linting/java/pmd.xml")
                .build());

        linters.put("eslint", LinterConfig.builder()
                .name("eslint")
                .enabled(true)
                .configFile(".linting/frontend/.eslintrc.js")
                .build());

        linters.put("prettier", LinterConfig.builder()
                .name("prettier")
                .enabled(true)
                .configFile(".linting/frontend/.prettierrc")
                .build());

        // Override with configuration from file
        Object lintersConfig = getNestedValue(configMap, "linters");
        if (lintersConfig instanceof Map) {
            Map<String, Object> lintersMap = (Map<String, Object>) lintersConfig;
            for (Map.Entry<String, Object> entry : lintersMap.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    Map<String, Object> linterMap = (Map<String, Object>) entry.getValue();
                    LinterConfig linterConfig = parseLinterConfig(entry.getKey(), linterMap);
                    linters.put(entry.getKey(), linterConfig);
                }
            }
        }

        return linters;
    }

    @SuppressWarnings("unchecked")
    private LinterConfig parseLinterConfig(String name, Map<String, Object> linterMap) {
        boolean enabled = parseBoolean(linterMap, "enabled", true);
        String configFile = parseString(linterMap, "config_file", null);
        Map<String, Object> properties = (Map<String, Object>) linterMap.getOrDefault("properties", new HashMap<>());

        return LinterConfig.builder()
                .name(name)
                .enabled(enabled)
                .configFile(configFile)
                .properties(properties)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<String> parseExcludePatterns(Map<String, Object> configMap) {
        Object excludePatterns = getNestedValue(configMap, "global.exclude_patterns");
        if (excludePatterns instanceof List) {
            return new ArrayList<>((List<String>) excludePatterns);
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseGlobalSettings(Map<String, Object> configMap) {
        Object globalSettings = getNestedValue(configMap, "global");
        if (globalSettings instanceof Map) {
            return new HashMap<>((Map<String, Object>) globalSettings);
        }
        return new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private QualityThresholds parseQualityThresholds(Map<String, Object> configMap) {
        Object thresholds = getNestedValue(configMap, "global.thresholds");
        if (thresholds instanceof Map) {
            Map<String, Object> thresholdsMap = (Map<String, Object>) thresholds;

            int maxErrors = parseInt(thresholdsMap, "max_errors", 0);
            int maxWarnings = parseInt(thresholdsMap, "max_warnings", 10);
            double minCoverage = parseDouble(thresholdsMap, "min_coverage", 0.80);
            int maxComplexity = parseInt(thresholdsMap, "max_complexity", 10);

            return new QualityThresholds(maxErrors, maxWarnings, minCoverage, maxComplexity);
        }

        return new QualityThresholds();
    }

    /**
     * Create default configuration when no configuration file is found.
     */
    private LintingConfiguration createDefaultConfiguration() {
        Map<String, LinterConfig> defaultLinters = new HashMap<>();

        defaultLinters.put("checkstyle", LinterConfig.builder()
                .name("checkstyle")
                .enabled(true)
                .configFile(".linting/java/checkstyle.xml")
                .build());

        return LintingConfiguration.builder()
                .linters(defaultLinters)
                .excludePatterns(Arrays.asList("**/target/**", "**/build/**", "**/node_modules/**"))
                .globalSettings(new HashMap<>())
                .thresholds(new QualityThresholds())
                .parallelExecution(true)
                .maxThreads(4)
                .build();
    }

    /**
     * Get service-specific configuration file paths.
     */
    public Map<String, String> getServiceConfigPaths(Path projectRoot, String serviceName) {
        Map<String, String> paths = new HashMap<>();

        Path serviceDir = projectRoot.resolve(".linting/services/" + serviceName);

        // Java configurations
        Path checkstyleOverride = serviceDir.resolve("checkstyle-overrides.xml");
        if (Files.exists(checkstyleOverride)) {
            paths.put("checkstyle", checkstyleOverride.toString());
        }

        Path spotbugsOverride = serviceDir.resolve("spotbugs-exclude.xml");
        if (Files.exists(spotbugsOverride)) {
            paths.put("spotbugs", spotbugsOverride.toString());
        }

        Path pmdOverride = serviceDir.resolve("pmd-overrides.xml");
        if (Files.exists(pmdOverride)) {
            paths.put("pmd", pmdOverride.toString());
        }

        // Frontend configurations
        Path eslintOverride = serviceDir.resolve("eslint-overrides.js");
        if (Files.exists(eslintOverride)) {
            paths.put("eslint", eslintOverride.toString());
        }

        Path prettierOverride = serviceDir.resolve("prettier-overrides.json");
        if (Files.exists(prettierOverride)) {
            paths.put("prettier", prettierOverride.toString());
        }

        return paths;
    }

    // Helper methods
    private Object getNestedValue(Map<String, Object> map, String path) {
        String[] parts = path.split("\\.");
        Object current = map;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }

        return current;
    }

    private boolean parseBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    private int parseInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private double parseDouble(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    private String parseString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }
}
