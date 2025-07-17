package com.zamaz.mcp.github.analyzer.pattern;

import com.zamaz.mcp.github.analyzer.model.*;
import com.zamaz.mcp.github.analyzer.model.ModelEnums.ArchitecturePatternType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Detects architecture patterns in repository structure
 */
@Component
@Slf4j
public class ArchitecturePatternDetector {
    
    private final Map<ArchitecturePatternType, PatternDetector> patternDetectors;
    
    public ArchitecturePatternDetector() {
        this.patternDetectors = initializePatternDetectors();
    }
    
    /**
     * Detect architecture patterns in the repository
     */
    public List<ArchitecturePattern> detectPatterns(RepositoryStructure structure, 
                                                   CodeOrganizationResult organization, 
                                                   DependencyGraph dependencyGraph) {
        log.info("Detecting architecture patterns for repository {}/{}", 
                structure.getOwner(), structure.getRepository());
        
        List<ArchitecturePattern> detectedPatterns = new ArrayList<>();
        
        // Apply each pattern detector
        for (Map.Entry<ArchitecturePatternType, PatternDetector> entry : patternDetectors.entrySet()) {
            ArchitecturePatternType patternType = entry.getKey();
            PatternDetector detector = entry.getValue();
            
            try {
                ArchitecturePattern pattern = detector.detect(structure, organization, dependencyGraph);
                if (pattern != null) {
                    detectedPatterns.add(pattern);
                    log.info("Detected pattern: {} with confidence: {}", 
                            patternType.name(), pattern.getConfidence());
                }
            } catch (Exception e) {
                log.error("Error detecting pattern {}: {}", patternType, e.getMessage(), e);
            }
        }
        
        // Sort by confidence (highest first)
        detectedPatterns.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));
        
        // Post-process patterns to resolve conflicts and enhance detection
        enhancePatternDetection(detectedPatterns, structure, organization);
        
        log.info("Detected {} architecture patterns", detectedPatterns.size());
        return detectedPatterns;
    }
    
    /**
     * Initialize pattern detectors
     */
    private Map<ArchitecturePatternType, PatternDetector> initializePatternDetectors() {
        Map<ArchitecturePatternType, PatternDetector> detectors = new HashMap<>();
        
        detectors.put(ArchitecturePatternType.MVC, new MVCPatternDetector());
        detectors.put(ArchitecturePatternType.LAYERED, new LayeredPatternDetector());
        detectors.put(ArchitecturePatternType.MICROSERVICES, new MicroservicesPatternDetector());
        detectors.put(ArchitecturePatternType.MONOLITH, new MonolithPatternDetector());
        detectors.put(ArchitecturePatternType.HEXAGONAL, new HexagonalPatternDetector());
        detectors.put(ArchitecturePatternType.CLEAN_ARCHITECTURE, new CleanArchitecturePatternDetector());
        detectors.put(ArchitecturePatternType.REPOSITORY_PATTERN, new RepositoryPatternDetector());
        detectors.put(ArchitecturePatternType.FACTORY_PATTERN, new FactoryPatternDetector());
        detectors.put(ArchitecturePatternType.SINGLETON_PATTERN, new SingletonPatternDetector());
        detectors.put(ArchitecturePatternType.OBSERVER_PATTERN, new ObserverPatternDetector());
        detectors.put(ArchitecturePatternType.STRATEGY_PATTERN, new StrategyPatternDetector());
        
        return detectors;
    }
    
    /**
     * Enhance pattern detection results
     */
    private void enhancePatternDetection(List<ArchitecturePattern> patterns, 
                                       RepositoryStructure structure, 
                                       CodeOrganizationResult organization) {
        
        // Resolve conflicts between similar patterns
        resolvePatternConflicts(patterns);
        
        // Add pattern relationships
        addPatternRelationships(patterns);
        
        // Enhance pattern evidence
        enhancePatternEvidence(patterns, structure, organization);
    }
    
    /**
     * Resolve conflicts between similar patterns
     */
    private void resolvePatternConflicts(List<ArchitecturePattern> patterns) {
        // Remove lower confidence patterns that conflict with higher confidence ones
        Set<ArchitecturePattern> toRemove = new HashSet<>();
        
        for (int i = 0; i < patterns.size(); i++) {
            for (int j = i + 1; j < patterns.size(); j++) {
                ArchitecturePattern pattern1 = patterns.get(i);
                ArchitecturePattern pattern2 = patterns.get(j);
                
                if (patternsConflict(pattern1, pattern2)) {
                    if (pattern1.getConfidence() > pattern2.getConfidence()) {
                        toRemove.add(pattern2);
                    } else {
                        toRemove.add(pattern1);
                    }
                }
            }
        }
        
        patterns.removeAll(toRemove);
    }
    
    /**
     * Check if two patterns conflict
     */
    private boolean patternsConflict(ArchitecturePattern pattern1, ArchitecturePattern pattern2) {
        // Microservices and Monolith conflict
        if ((pattern1.getType() == ArchitecturePatternType.MICROSERVICES && 
             pattern2.getType() == ArchitecturePatternType.MONOLITH) ||
            (pattern1.getType() == ArchitecturePatternType.MONOLITH && 
             pattern2.getType() == ArchitecturePatternType.MICROSERVICES)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Add relationships between patterns
     */
    private void addPatternRelationships(List<ArchitecturePattern> patterns) {
        for (ArchitecturePattern pattern : patterns) {
            List<String> relatedPatterns = new ArrayList<>();
            
            for (ArchitecturePattern other : patterns) {
                if (pattern != other && areRelated(pattern, other)) {
                    relatedPatterns.add(other.getName());
                }
            }
            
            pattern.setRelatedPatterns(relatedPatterns);
        }
    }
    
    /**
     * Check if two patterns are related
     */
    private boolean areRelated(ArchitecturePattern pattern1, ArchitecturePattern pattern2) {
        // MVC and Layered are related
        if ((pattern1.getType() == ArchitecturePatternType.MVC && 
             pattern2.getType() == ArchitecturePatternType.LAYERED) ||
            (pattern1.getType() == ArchitecturePatternType.LAYERED && 
             pattern2.getType() == ArchitecturePatternType.MVC)) {
            return true;
        }
        
        // Repository pattern is related to Clean Architecture
        if ((pattern1.getType() == ArchitecturePatternType.REPOSITORY_PATTERN && 
             pattern2.getType() == ArchitecturePatternType.CLEAN_ARCHITECTURE) ||
            (pattern1.getType() == ArchitecturePatternType.CLEAN_ARCHITECTURE && 
             pattern2.getType() == ArchitecturePatternType.REPOSITORY_PATTERN)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Enhance pattern evidence
     */
    private void enhancePatternEvidence(List<ArchitecturePattern> patterns, 
                                      RepositoryStructure structure, 
                                      CodeOrganizationResult organization) {
        for (ArchitecturePattern pattern : patterns) {
            List<String> enhancedEvidence = new ArrayList<>(pattern.getEvidence());
            
            // Add organization-based evidence
            if (organization.getPackageStructure() != null) {
                enhancedEvidence.add("Package depth: " + organization.getPackageStructure().getMaxDepth());
                enhancedEvidence.add("Total packages: " + organization.getPackageStructure().getTotalPackages());
            }
            
            if (organization.getModuleStructure() != null) {
                enhancedEvidence.add("Total modules: " + organization.getModuleStructure().getTotalModules());
            }
            
            if (organization.getLayerStructure() != null) {
                enhancedEvidence.add("Total layers: " + organization.getLayerStructure().getTotalLayers());
            }
            
            pattern.setEvidence(enhancedEvidence);
        }
    }
    
    /**
     * Interface for pattern detectors
     */
    private interface PatternDetector {
        ArchitecturePattern detect(RepositoryStructure structure, 
                                 CodeOrganizationResult organization, 
                                 DependencyGraph dependencyGraph);
    }
    
    /**
     * MVC Pattern Detector
     */
    private static class MVCPatternDetector implements PatternDetector {
        @Override
        public ArchitecturePattern detect(RepositoryStructure structure, 
                                        CodeOrganizationResult organization, 
                                        DependencyGraph dependencyGraph) {
            double confidence = 0.0;
            List<String> evidence = new ArrayList<>();
            
            // Check for MVC-related directories
            boolean hasController = structure.getDirectories().stream()
                    .anyMatch(dir -> dir.getName().toLowerCase().contains("controller"));
            boolean hasModel = structure.getDirectories().stream()
                    .anyMatch(dir -> dir.getName().toLowerCase().contains("model") || 
                                    dir.getName().toLowerCase().contains("entity"));
            boolean hasView = structure.getDirectories().stream()
                    .anyMatch(dir -> dir.getName().toLowerCase().contains("view") || 
                                    dir.getName().toLowerCase().contains("template"));
            
            if (hasController) {
                confidence += 0.4;
                evidence.add("Found controller directory");
            }
            if (hasModel) {
                confidence += 0.3;
                evidence.add("Found model/entity directory");
            }
            if (hasView) {
                confidence += 0.3;
                evidence.add("Found view/template directory");
            }
            
            // Check for MVC-related files
            long controllerFiles = structure.getFiles().stream()
                    .filter(file -> file.getName().toLowerCase().contains("controller"))
                    .count();
            
            if (controllerFiles > 0) {
                confidence += Math.min(controllerFiles * 0.05, 0.2);
                evidence.add("Found " + controllerFiles + " controller files");
            }
            
            if (confidence < 0.5) {
                return null;
            }
            
            return ArchitecturePattern.builder()
                    .type(ArchitecturePatternType.MVC)
                    .name("Model-View-Controller")
                    .confidence(Math.min(confidence, 1.0))
                    .evidence(evidence)
                    .description("Web application following MVC architectural pattern")
                    .benefits(Arrays.asList("Separation of concerns", "Testability", "Maintainability"))
                    .drawbacks(Arrays.asList("Can become complex", "Tight coupling between components"))
                    .build();
        }
    }
    
    /**
     * Layered Pattern Detector
     */
    private static class LayeredPatternDetector implements PatternDetector {
        @Override
        public ArchitecturePattern detect(RepositoryStructure structure, 
                                        CodeOrganizationResult organization, 
                                        DependencyGraph dependencyGraph) {
            double confidence = 0.0;
            List<String> evidence = new ArrayList<>();
            
            // Check for layered structure
            String[] layers = {"presentation", "business", "data", "service", "repository", "controller"};
            int layerCount = 0;
            
            for (String layer : layers) {
                boolean hasLayer = structure.getDirectories().stream()
                        .anyMatch(dir -> dir.getName().toLowerCase().contains(layer));
                
                if (hasLayer) {
                    layerCount++;
                    evidence.add("Found " + layer + " layer");
                }
            }
            
            if (layerCount >= 3) {
                confidence = 0.3 + (layerCount * 0.1);
                evidence.add("Found " + layerCount + " distinct layers");
            }
            
            // Check organization structure
            if (organization.getLayerStructure() != null && 
                organization.getLayerStructure().getTotalLayers() >= 3) {
                confidence += 0.3;
                evidence.add("Layer structure detected in organization analysis");
            }
            
            if (confidence < 0.5) {
                return null;
            }
            
            return ArchitecturePattern.builder()
                    .type(ArchitecturePatternType.LAYERED)
                    .name("Layered Architecture")
                    .confidence(Math.min(confidence, 1.0))
                    .evidence(evidence)
                    .description("Application organized in distinct layers")
                    .benefits(Arrays.asList("Clear separation of concerns", "Scalability", "Testability"))
                    .drawbacks(Arrays.asList("Performance overhead", "Can become monolithic"))
                    .build();
        }
    }
    
    /**
     * Microservices Pattern Detector
     */
    private static class MicroservicesPatternDetector implements PatternDetector {
        @Override
        public ArchitecturePattern detect(RepositoryStructure structure, 
                                        CodeOrganizationResult organization, 
                                        DependencyGraph dependencyGraph) {
            double confidence = 0.0;
            List<String> evidence = new ArrayList<>();
            
            // Check for microservices indicators
            boolean hasDockerfile = structure.hasFile("Dockerfile");
            boolean hasDockerCompose = structure.hasFile("docker-compose.yml") || 
                                     structure.hasFile("docker-compose.yaml");
            boolean hasK8sConfig = structure.getFiles().stream()
                    .anyMatch(file -> file.getName().endsWith(".yaml") || file.getName().endsWith(".yml"));
            
            if (hasDockerfile) {
                confidence += 0.3;
                evidence.add("Found Dockerfile");
            }
            if (hasDockerCompose) {
                confidence += 0.2;
                evidence.add("Found docker-compose configuration");
            }
            if (hasK8sConfig) {
                confidence += 0.2;
                evidence.add("Found Kubernetes configuration");
            }
            
            // Check for multiple modules
            if (organization.getModuleStructure() != null && 
                organization.getModuleStructure().getTotalModules() > 3) {
                confidence += 0.3;
                evidence.add("Multiple modules detected: " + organization.getModuleStructure().getTotalModules());
            }
            
            if (confidence < 0.5) {
                return null;
            }
            
            return ArchitecturePattern.builder()
                    .type(ArchitecturePatternType.MICROSERVICES)
                    .name("Microservices Architecture")
                    .confidence(Math.min(confidence, 1.0))
                    .evidence(evidence)
                    .description("Application composed of multiple independently deployable services")
                    .benefits(Arrays.asList("Scalability", "Technology diversity", "Fault isolation"))
                    .drawbacks(Arrays.asList("Complexity", "Network overhead", "Data consistency challenges"))
                    .build();
        }
    }
    
    /**
     * Monolith Pattern Detector
     */
    private static class MonolithPatternDetector implements PatternDetector {
        @Override
        public ArchitecturePattern detect(RepositoryStructure structure, 
                                        CodeOrganizationResult organization, 
                                        DependencyGraph dependencyGraph) {
            double confidence = 0.0;
            List<String> evidence = new ArrayList<>();
            
            // Check for monolith indicators
            boolean hasSingleBuildFile = (structure.hasFile("pom.xml") && 
                                        structure.getFiles().stream()
                                               .filter(f -> f.getName().equals("pom.xml"))
                                               .count() == 1) ||
                                       (structure.hasFile("build.gradle") && 
                                        structure.getFiles().stream()
                                               .filter(f -> f.getName().equals("build.gradle"))
                                               .count() == 1);
            
            if (hasSingleBuildFile) {
                confidence += 0.4;
                evidence.add("Single build configuration file");
            }
            
            // Check for few modules
            if (organization.getModuleStructure() != null && 
                organization.getModuleStructure().getTotalModules() <= 2) {
                confidence += 0.3;
                evidence.add("Few modules detected: " + organization.getModuleStructure().getTotalModules());
            }
            
            // Check for large codebase
            if (structure.getFiles().size() > 100) {
                confidence += 0.2;
                evidence.add("Large codebase with " + structure.getFiles().size() + " files");
            }
            
            if (confidence < 0.5) {
                return null;
            }
            
            return ArchitecturePattern.builder()
                    .type(ArchitecturePatternType.MONOLITH)
                    .name("Monolithic Architecture")
                    .confidence(Math.min(confidence, 1.0))
                    .evidence(evidence)
                    .description("Application deployed as a single unit")
                    .benefits(Arrays.asList("Simplicity", "Easy testing", "Easy deployment"))
                    .drawbacks(Arrays.asList("Limited scalability", "Technology lock-in", "Large codebase"))
                    .build();
        }
    }
    
    /**
     * Repository Pattern Detector
     */
    private static class RepositoryPatternDetector implements PatternDetector {
        @Override
        public ArchitecturePattern detect(RepositoryStructure structure, 
                                        CodeOrganizationResult organization, 
                                        DependencyGraph dependencyGraph) {
            double confidence = 0.0;
            List<String> evidence = new ArrayList<>();
            
            // Check for repository-related files
            long repositoryFiles = structure.getFiles().stream()
                    .filter(file -> file.getName().toLowerCase().contains("repository"))
                    .count();
            
            if (repositoryFiles > 0) {
                confidence += Math.min(repositoryFiles * 0.2, 0.6);
                evidence.add("Found " + repositoryFiles + " repository files");
            }
            
            // Check for DAO pattern
            long daoFiles = structure.getFiles().stream()
                    .filter(file -> file.getName().toLowerCase().contains("dao"))
                    .count();
            
            if (daoFiles > 0) {
                confidence += Math.min(daoFiles * 0.1, 0.3);
                evidence.add("Found " + daoFiles + " DAO files");
            }
            
            if (confidence < 0.3) {
                return null;
            }
            
            return ArchitecturePattern.builder()
                    .type(ArchitecturePatternType.REPOSITORY_PATTERN)
                    .name("Repository Pattern")
                    .confidence(Math.min(confidence, 1.0))
                    .evidence(evidence)
                    .description("Data access abstraction using repository pattern")
                    .benefits(Arrays.asList("Data access abstraction", "Testability", "Maintainability"))
                    .drawbacks(Arrays.asList("Additional complexity", "Potential performance overhead"))
                    .build();
        }
    }
    
    // Additional pattern detectors would be implemented similarly...
    
    /**
     * Generic pattern detector for simple patterns
     */
    private static class GenericPatternDetector implements PatternDetector {
        private final ArchitecturePatternType type;
        private final String name;
        private final String[] keywords;
        private final double minConfidence;
        
        public GenericPatternDetector(ArchitecturePatternType type, String name, 
                                    String[] keywords, double minConfidence) {
            this.type = type;
            this.name = name;
            this.keywords = keywords;
            this.minConfidence = minConfidence;
        }
        
        @Override
        public ArchitecturePattern detect(RepositoryStructure structure, 
                                        CodeOrganizationResult organization, 
                                        DependencyGraph dependencyGraph) {
            double confidence = 0.0;
            List<String> evidence = new ArrayList<>();
            
            for (String keyword : keywords) {
                long matchingFiles = structure.getFiles().stream()
                        .filter(file -> file.getName().toLowerCase().contains(keyword.toLowerCase()))
                        .count();
                
                if (matchingFiles > 0) {
                    confidence += Math.min(matchingFiles * 0.1, 0.3);
                    evidence.add("Found " + matchingFiles + " files matching '" + keyword + "'");
                }
            }
            
            if (confidence < minConfidence) {
                return null;
            }
            
            return ArchitecturePattern.builder()
                    .type(type)
                    .name(name)
                    .confidence(Math.min(confidence, 1.0))
                    .evidence(evidence)
                    .description("Detected " + name + " pattern")
                    .build();
        }
    }
    
    // Implement remaining pattern detectors using GenericPatternDetector
    private static class HexagonalPatternDetector extends GenericPatternDetector {
        public HexagonalPatternDetector() {
            super(ArchitecturePatternType.HEXAGONAL, "Hexagonal Architecture", 
                  new String[]{"port", "adapter", "hexagonal"}, 0.3);
        }
    }
    
    private static class CleanArchitecturePatternDetector extends GenericPatternDetector {
        public CleanArchitecturePatternDetector() {
            super(ArchitecturePatternType.CLEAN_ARCHITECTURE, "Clean Architecture", 
                  new String[]{"usecase", "interactor", "presenter", "gateway"}, 0.4);
        }
    }
    
    private static class FactoryPatternDetector extends GenericPatternDetector {
        public FactoryPatternDetector() {
            super(ArchitecturePatternType.FACTORY_PATTERN, "Factory Pattern", 
                  new String[]{"factory", "builder", "creator"}, 0.3);
        }
    }
    
    private static class SingletonPatternDetector extends GenericPatternDetector {
        public SingletonPatternDetector() {
            super(ArchitecturePatternType.SINGLETON_PATTERN, "Singleton Pattern", 
                  new String[]{"singleton"}, 0.2);
        }
    }
    
    private static class ObserverPatternDetector extends GenericPatternDetector {
        public ObserverPatternDetector() {
            super(ArchitecturePatternType.OBSERVER_PATTERN, "Observer Pattern", 
                  new String[]{"observer", "listener", "event"}, 0.3);
        }
    }
    
    private static class StrategyPatternDetector extends GenericPatternDetector {
        public StrategyPatternDetector() {
            super(ArchitecturePatternType.STRATEGY_PATTERN, "Strategy Pattern", 
                  new String[]{"strategy", "algorithm"}, 0.2);
        }
    }
}