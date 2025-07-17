package com.zamaz.mcp.pattern.recommendation;

import com.zamaz.mcp.pattern.core.PatternType;
import com.zamaz.mcp.pattern.core.PatternCategory;
import com.zamaz.mcp.pattern.core.PatternSeverity;
import com.zamaz.mcp.pattern.model.CodeAnalysisContext;
import com.zamaz.mcp.pattern.model.PatternDetectionResult;
import com.zamaz.mcp.pattern.model.PatternRecommendation;
import com.zamaz.mcp.pattern.model.RefactoringPlan;
import com.zamaz.mcp.pattern.model.ImprovementSuggestion;
import com.zamaz.mcp.pattern.ml.TeamPatternLearningService;
import com.zamaz.mcp.pattern.service.PatternAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Engine for generating pattern-based recommendations and suggestions.
 * 
 * This service analyzes detected patterns and provides:
 * - Refactoring suggestions
 * - Code improvement recommendations
 * - Best practice guidance
 * - Team-specific recommendations
 * - Priority-based action plans
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatternRecommendationEngine {
    
    private final TeamPatternLearningService teamLearningService;
    private final PatternAnalysisService patternAnalysisService;
    private final RefactoringPlanGenerator refactoringPlanGenerator;
    private final BestPracticeAdvisor bestPracticeAdvisor;
    private final PriorityCalculator priorityCalculator;
    
    /**
     * Generate comprehensive recommendations based on pattern analysis results.
     * 
     * @param organizationId Organization identifier
     * @param analysisResults Pattern detection results
     * @param codeContexts Code contexts that were analyzed
     * @return Comprehensive recommendations
     */
    public ComprehensiveRecommendations generateRecommendations(String organizationId,
                                                               List<PatternDetectionResult> analysisResults,
                                                               List<CodeAnalysisContext> codeContexts) {
        
        log.info("Generating recommendations for organization: {} with {} results", 
                 organizationId, analysisResults.size());
        
        try {
            // Group results by severity and category
            Map<PatternSeverity, List<PatternDetectionResult>> resultsBySeverity = 
                    groupResultsBySeverity(analysisResults);
            
            Map<PatternCategory, List<PatternDetectionResult>> resultsByCategory = 
                    groupResultsByCategory(analysisResults);
            
            // Generate different types of recommendations
            List<RefactoringPlan> refactoringPlans = generateRefactoringPlans(resultsBySeverity);
            List<ImprovementSuggestion> improvementSuggestions = generateImprovementSuggestions(analysisResults);
            List<PatternRecommendation> teamRecommendations = generateTeamRecommendations(organizationId, codeContexts);
            List<BestPracticeRecommendation> bestPractices = generateBestPracticeRecommendations(resultsByCategory);
            
            // Calculate priorities and create action plan
            ActionPlan actionPlan = createActionPlan(refactoringPlans, improvementSuggestions, teamRecommendations);
            
            // Generate quality metrics and trends
            QualityMetrics qualityMetrics = calculateQualityMetrics(analysisResults);
            List<QualityTrend> qualityTrends = calculateQualityTrends(organizationId, qualityMetrics);
            
            // Create comprehensive recommendations
            ComprehensiveRecommendations recommendations = ComprehensiveRecommendations.builder()
                    .organizationId(organizationId)
                    .refactoringPlans(refactoringPlans)
                    .improvementSuggestions(improvementSuggestions)
                    .teamRecommendations(teamRecommendations)
                    .bestPractices(bestPractices)
                    .actionPlan(actionPlan)
                    .qualityMetrics(qualityMetrics)
                    .qualityTrends(qualityTrends)
                    .generatedAt(new Date())
                    .build();
            
            log.info("Generated {} refactoring plans, {} improvement suggestions, {} team recommendations", 
                     refactoringPlans.size(), improvementSuggestions.size(), teamRecommendations.size());
            
            return recommendations;
            
        } catch (Exception e) {
            log.error("Error generating recommendations for organization: {}", organizationId, e);
            return ComprehensiveRecommendations.empty(organizationId);
        }
    }
    
    /**
     * Generate quick fix suggestions for immediate issues.
     * 
     * @param analysisResults Pattern detection results
     * @return List of quick fix suggestions
     */
    public List<QuickFixSuggestion> generateQuickFixes(List<PatternDetectionResult> analysisResults) {
        return analysisResults.stream()
                .filter(result -> result.getSeverity().isAtLeast(PatternSeverity.MEDIUM))
                .filter(result -> result.getRefactoringEffort() == RefactoringEffort.LOW)
                .map(this::createQuickFix)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(QuickFixSuggestion::getImpact).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Generate architectural recommendations based on pattern analysis.
     * 
     * @param organizationId Organization identifier
     * @param analysisResults Pattern detection results
     * @return Architectural recommendations
     */
    public ArchitecturalRecommendations generateArchitecturalRecommendations(String organizationId,
                                                                             List<PatternDetectionResult> analysisResults) {
        
        // Filter architectural patterns and anti-patterns
        List<PatternDetectionResult> architecturalResults = analysisResults.stream()
                .filter(result -> result.getPatternCategory() == PatternCategory.ARCHITECTURAL_PATTERN ||
                                 result.getPatternCategory() == PatternCategory.ANTI_PATTERN)
                .collect(Collectors.toList());
        
        // Analyze architectural quality
        ArchitecturalQualityAnalysis qualityAnalysis = analyzeArchitecturalQuality(architecturalResults);
        
        // Generate recommendations
        List<ArchitecturalImprovement> improvements = generateArchitecturalImprovements(qualityAnalysis);
        List<DesignPrincipleViolation> violations = identifyDesignPrincipleViolations(architecturalResults);
        List<ArchitecturalPattern> suggestedPatterns = suggestArchitecturalPatterns(organizationId, qualityAnalysis);
        
        return ArchitecturalRecommendations.builder()
                .organizationId(organizationId)
                .qualityAnalysis(qualityAnalysis)
                .improvements(improvements)
                .violations(violations)
                .suggestedPatterns(suggestedPatterns)
                .build();
    }
    
    /**
     * Generate security-focused recommendations.
     * 
     * @param analysisResults Pattern detection results
     * @return Security recommendations
     */
    public SecurityRecommendations generateSecurityRecommendations(List<PatternDetectionResult> analysisResults) {
        List<PatternDetectionResult> securityResults = analysisResults.stream()
                .filter(result -> result.getPatternCategory() == PatternCategory.SECURITY_PATTERN ||
                                 isSecurityRelated(result))
                .collect(Collectors.toList());
        
        List<SecurityVulnerability> vulnerabilities = identifySecurityVulnerabilities(securityResults);
        List<SecurityBestPractice> bestPractices = generateSecurityBestPractices(securityResults);
        SecurityRiskAssessment riskAssessment = assessSecurityRisk(vulnerabilities);
        
        return SecurityRecommendations.builder()
                .vulnerabilities(vulnerabilities)
                .bestPractices(bestPractices)
                .riskAssessment(riskAssessment)
                .build();
    }
    
    /**
     * Generate performance optimization recommendations.
     * 
     * @param analysisResults Pattern detection results
     * @return Performance recommendations
     */
    public PerformanceRecommendations generatePerformanceRecommendations(List<PatternDetectionResult> analysisResults) {
        List<PatternDetectionResult> performanceResults = analysisResults.stream()
                .filter(result -> result.getPatternCategory() == PatternCategory.PERFORMANCE_PATTERN ||
                                 isPerformanceRelated(result))
                .collect(Collectors.toList());
        
        List<PerformanceIssue> issues = identifyPerformanceIssues(performanceResults);
        List<OptimizationOpportunity> opportunities = identifyOptimizationOpportunities(performanceResults);
        List<PerformancePattern> suggestedPatterns = suggestPerformancePatterns(performanceResults);
        
        return PerformanceRecommendations.builder()
                .issues(issues)
                .opportunities(opportunities)
                .suggestedPatterns(suggestedPatterns)
                .build();
    }
    
    // Private helper methods
    
    private Map<PatternSeverity, List<PatternDetectionResult>> groupResultsBySeverity(List<PatternDetectionResult> results) {
        return results.stream()
                .collect(Collectors.groupingBy(PatternDetectionResult::getSeverity));
    }
    
    private Map<PatternCategory, List<PatternDetectionResult>> groupResultsByCategory(List<PatternDetectionResult> results) {
        return results.stream()
                .collect(Collectors.groupingBy(PatternDetectionResult::getPatternCategory));
    }
    
    private List<RefactoringPlan> generateRefactoringPlans(Map<PatternSeverity, List<PatternDetectionResult>> resultsBySeverity) {
        List<RefactoringPlan> plans = new ArrayList<>();
        
        // Generate plans for high severity issues first
        List<PatternDetectionResult> highSeverityResults = resultsBySeverity.getOrDefault(PatternSeverity.HIGH, Collections.emptyList());
        List<PatternDetectionResult> criticalResults = resultsBySeverity.getOrDefault(PatternSeverity.CRITICAL, Collections.emptyList());
        
        List<PatternDetectionResult> priorityResults = new ArrayList<>();
        priorityResults.addAll(criticalResults);
        priorityResults.addAll(highSeverityResults);
        
        // Group by file and create file-specific refactoring plans
        Map<String, List<PatternDetectionResult>> resultsByFile = priorityResults.stream()
                .collect(Collectors.groupingBy(result -> result.getFilePath().toString()));
        
        resultsByFile.forEach((filePath, fileResults) -> {
            RefactoringPlan plan = refactoringPlanGenerator.generatePlan(filePath, fileResults);
            if (plan != null) {
                plans.add(plan);
            }
        });
        
        return plans;
    }
    
    private List<ImprovementSuggestion> generateImprovementSuggestions(List<PatternDetectionResult> results) {
        return results.stream()
                .flatMap(result -> result.getSuggestions().stream()
                        .map(suggestion -> ImprovementSuggestion.builder()
                                .patternType(result.getPatternType())
                                .filePath(result.getFilePath())
                                .suggestion(suggestion)
                                .priority(result.getPriority())
                                .effort(result.getRefactoringEffort())
                                .impact(result.getImpact())
                                .build()))
                .collect(Collectors.toList());
    }
    
    private List<PatternRecommendation> generateTeamRecommendations(String organizationId, List<CodeAnalysisContext> contexts) {
        return contexts.stream()
                .flatMap(context -> teamLearningService.getPersonalizedRecommendations(organizationId, context).stream())
                .collect(Collectors.toList());
    }
    
    private List<BestPracticeRecommendation> generateBestPracticeRecommendations(Map<PatternCategory, List<PatternDetectionResult>> resultsByCategory) {
        return resultsByCategory.entrySet().stream()
                .map(entry -> bestPracticeAdvisor.generateRecommendations(entry.getKey(), entry.getValue()))
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
    
    private ActionPlan createActionPlan(List<RefactoringPlan> refactoringPlans,
                                       List<ImprovementSuggestion> improvementSuggestions,
                                       List<PatternRecommendation> teamRecommendations) {
        
        // Calculate priorities for all recommendations
        List<PrioritizedAction> actions = new ArrayList<>();
        
        // Add refactoring plans
        refactoringPlans.forEach(plan -> {
            actions.add(PrioritizedAction.builder()
                    .type(ActionType.REFACTORING)
                    .priority(priorityCalculator.calculatePriority(plan))
                    .description(plan.getDescription())
                    .effort(plan.getEffort())
                    .impact(plan.getImpact())
                    .build());
        });
        
        // Add improvement suggestions
        improvementSuggestions.forEach(suggestion -> {
            actions.add(PrioritizedAction.builder()
                    .type(ActionType.IMPROVEMENT)
                    .priority(priorityCalculator.calculatePriority(suggestion))
                    .description(suggestion.getSuggestion())
                    .effort(suggestion.getEffort())
                    .impact(suggestion.getImpact())
                    .build());
        });
        
        // Add team recommendations
        teamRecommendations.forEach(recommendation -> {
            actions.add(PrioritizedAction.builder()
                    .type(ActionType.TEAM_RECOMMENDATION)
                    .priority(recommendation.getPriority())
                    .description(recommendation.getReasoning())
                    .effort(RefactoringEffort.MEDIUM)
                    .impact(PatternImpact.MEDIUM)
                    .build());
        });
        
        // Sort by priority
        actions.sort(Comparator.comparing(PrioritizedAction::getPriority).reversed());
        
        return ActionPlan.builder()
                .actions(actions)
                .estimatedDuration(calculateEstimatedDuration(actions))
                .phases(createPhases(actions))
                .build();
    }
    
    private QualityMetrics calculateQualityMetrics(List<PatternDetectionResult> results) {
        long totalIssues = results.stream()
                .filter(result -> result.getPatternCategory() == PatternCategory.CODE_SMELL ||
                                 result.getPatternCategory() == PatternCategory.ANTI_PATTERN)
                .count();
        
        long criticalIssues = results.stream()
                .filter(result -> result.getSeverity() == PatternSeverity.CRITICAL)
                .count();
        
        long highSeverityIssues = results.stream()
                .filter(result -> result.getSeverity() == PatternSeverity.HIGH)
                .count();
        
        double averageConfidence = results.stream()
                .mapToDouble(PatternDetectionResult::getConfidence)
                .average()
                .orElse(0.0);
        
        long positivePatterns = results.stream()
                .filter(PatternDetectionResult::isPositivePattern)
                .count();
        
        return QualityMetrics.builder()
                .totalIssues(totalIssues)
                .criticalIssues(criticalIssues)
                .highSeverityIssues(highSeverityIssues)
                .averageConfidence(averageConfidence)
                .positivePatterns(positivePatterns)
                .qualityScore(calculateQualityScore(results))
                .build();
    }
    
    private List<QualityTrend> calculateQualityTrends(String organizationId, QualityMetrics currentMetrics) {
        // This would typically compare with historical data
        // For now, return empty trends
        return new ArrayList<>();
    }
    
    private QuickFixSuggestion createQuickFix(PatternDetectionResult result) {
        if (result.getSuggestions().isEmpty()) {
            return null;
        }
        
        return QuickFixSuggestion.builder()
                .patternType(result.getPatternType())
                .filePath(result.getFilePath())
                .startLine(result.getStartLine())
                .endLine(result.getEndLine())
                .suggestion(result.getSuggestions().get(0))
                .impact(result.getImpact())
                .effort(result.getRefactoringEffort())
                .codeSnippet(result.getCodeSnippet())
                .build();
    }
    
    private ArchitecturalQualityAnalysis analyzeArchitecturalQuality(List<PatternDetectionResult> results) {
        // Analyze architectural quality based on detected patterns
        return ArchitecturalQualityAnalysis.builder()
                .couplingLevel(calculateCouplingLevel(results))
                .cohesionLevel(calculateCohesionLevel(results))
                .layeringViolations(countLayeringViolations(results))
                .circularDependencies(countCircularDependencies(results))
                .build();
    }
    
    private List<ArchitecturalImprovement> generateArchitecturalImprovements(ArchitecturalQualityAnalysis analysis) {
        List<ArchitecturalImprovement> improvements = new ArrayList<>();
        
        if (analysis.getCouplingLevel() > 0.7) {
            improvements.add(ArchitecturalImprovement.builder()
                    .type(ImprovementType.REDUCE_COUPLING)
                    .description("Reduce coupling between components")
                    .priority(PatternPriority.HIGH)
                    .build());
        }
        
        if (analysis.getCohesionLevel() < 0.5) {
            improvements.add(ArchitecturalImprovement.builder()
                    .type(ImprovementType.IMPROVE_COHESION)
                    .description("Improve cohesion within components")
                    .priority(PatternPriority.MEDIUM)
                    .build());
        }
        
        return improvements;
    }
    
    private List<DesignPrincipleViolation> identifyDesignPrincipleViolations(List<PatternDetectionResult> results) {
        return results.stream()
                .filter(result -> violatesDesignPrinciples(result))
                .map(this::createDesignPrincipleViolation)
                .collect(Collectors.toList());
    }
    
    private List<ArchitecturalPattern> suggestArchitecturalPatterns(String organizationId, ArchitecturalQualityAnalysis analysis) {
        List<ArchitecturalPattern> patterns = new ArrayList<>();
        
        if (analysis.getCouplingLevel() > 0.7) {
            patterns.add(ArchitecturalPattern.builder()
                    .type(PatternType.FACADE)
                    .description("Use Facade pattern to reduce coupling")
                    .applicability(0.8)
                    .build());
        }
        
        return patterns;
    }
    
    private boolean isSecurityRelated(PatternDetectionResult result) {
        return result.getPatternType().name().toLowerCase().contains("security") ||
               result.getDescription().toLowerCase().contains("security") ||
               result.getDescription().toLowerCase().contains("authentication") ||
               result.getDescription().toLowerCase().contains("authorization");
    }
    
    private boolean isPerformanceRelated(PatternDetectionResult result) {
        return result.getPatternType().name().toLowerCase().contains("performance") ||
               result.getDescription().toLowerCase().contains("performance") ||
               result.getDescription().toLowerCase().contains("optimization") ||
               result.getDescription().toLowerCase().contains("caching");
    }
    
    private List<SecurityVulnerability> identifySecurityVulnerabilities(List<PatternDetectionResult> results) {
        return results.stream()
                .filter(result -> result.getSeverity().isAtLeast(PatternSeverity.MEDIUM))
                .map(this::createSecurityVulnerability)
                .collect(Collectors.toList());
    }
    
    private SecurityVulnerability createSecurityVulnerability(PatternDetectionResult result) {
        return SecurityVulnerability.builder()
                .type(result.getPatternType())
                .description(result.getDescription())
                .severity(result.getSeverity())
                .filePath(result.getFilePath())
                .lineNumber(result.getStartLine())
                .build();
    }
    
    private List<SecurityBestPractice> generateSecurityBestPractices(List<PatternDetectionResult> results) {
        // Generate security best practices based on detected patterns
        return new ArrayList<>();
    }
    
    private SecurityRiskAssessment assessSecurityRisk(List<SecurityVulnerability> vulnerabilities) {
        int highRiskCount = (int) vulnerabilities.stream()
                .filter(v -> v.getSeverity() == PatternSeverity.HIGH || v.getSeverity() == PatternSeverity.CRITICAL)
                .count();
        
        return SecurityRiskAssessment.builder()
                .overallRisk(calculateOverallRisk(vulnerabilities))
                .highRiskIssues(highRiskCount)
                .recommendedActions(generateSecurityActions(vulnerabilities))
                .build();
    }
    
    private List<PerformanceIssue> identifyPerformanceIssues(List<PatternDetectionResult> results) {
        return results.stream()
                .filter(result -> result.getSeverity().isAtLeast(PatternSeverity.MEDIUM))
                .map(this::createPerformanceIssue)
                .collect(Collectors.toList());
    }
    
    private PerformanceIssue createPerformanceIssue(PatternDetectionResult result) {
        return PerformanceIssue.builder()
                .type(result.getPatternType())
                .description(result.getDescription())
                .impact(result.getImpact())
                .filePath(result.getFilePath())
                .lineNumber(result.getStartLine())
                .build();
    }
    
    private List<OptimizationOpportunity> identifyOptimizationOpportunities(List<PatternDetectionResult> results) {
        return results.stream()
                .filter(result -> result.isPositivePattern())
                .map(this::createOptimizationOpportunity)
                .collect(Collectors.toList());
    }
    
    private OptimizationOpportunity createOptimizationOpportunity(PatternDetectionResult result) {
        return OptimizationOpportunity.builder()
                .type(result.getPatternType())
                .description(result.getDescription())
                .benefit(result.getImpact())
                .effort(result.getRefactoringEffort())
                .build();
    }
    
    private List<PerformancePattern> suggestPerformancePatterns(List<PatternDetectionResult> results) {
        // Suggest performance patterns based on detected issues
        return new ArrayList<>();
    }
    
    private double calculateQualityScore(List<PatternDetectionResult> results) {
        // Calculate overall quality score based on detected patterns
        double score = 100.0;
        
        long criticalIssues = results.stream()
                .filter(result -> result.getSeverity() == PatternSeverity.CRITICAL)
                .count();
        
        long highSeverityIssues = results.stream()
                .filter(result -> result.getSeverity() == PatternSeverity.HIGH)
                .count();
        
        long mediumSeverityIssues = results.stream()
                .filter(result -> result.getSeverity() == PatternSeverity.MEDIUM)
                .count();
        
        score -= criticalIssues * 20;
        score -= highSeverityIssues * 10;
        score -= mediumSeverityIssues * 5;
        
        return Math.max(0, score);
    }
    
    private int calculateEstimatedDuration(List<PrioritizedAction> actions) {
        return actions.stream()
                .mapToInt(action -> action.getEffort().getDays())
                .sum();
    }
    
    private List<ActionPhase> createPhases(List<PrioritizedAction> actions) {
        // Group actions into phases based on priority and dependencies
        List<ActionPhase> phases = new ArrayList<>();
        
        // Phase 1: Critical and high priority items
        List<PrioritizedAction> phase1Actions = actions.stream()
                .filter(action -> action.getPriority() == PatternPriority.CRITICAL ||
                                 action.getPriority() == PatternPriority.HIGH)
                .collect(Collectors.toList());
        
        if (!phase1Actions.isEmpty()) {
            phases.add(ActionPhase.builder()
                    .name("Critical Issues")
                    .description("Address critical and high priority issues")
                    .actions(phase1Actions)
                    .estimatedDuration(calculateEstimatedDuration(phase1Actions))
                    .build());
        }
        
        // Phase 2: Medium priority items
        List<PrioritizedAction> phase2Actions = actions.stream()
                .filter(action -> action.getPriority() == PatternPriority.MEDIUM)
                .collect(Collectors.toList());
        
        if (!phase2Actions.isEmpty()) {
            phases.add(ActionPhase.builder()
                    .name("Medium Priority Improvements")
                    .description("Address medium priority issues and improvements")
                    .actions(phase2Actions)
                    .estimatedDuration(calculateEstimatedDuration(phase2Actions))
                    .build());
        }
        
        // Phase 3: Low priority items
        List<PrioritizedAction> phase3Actions = actions.stream()
                .filter(action -> action.getPriority() == PatternPriority.LOW)
                .collect(Collectors.toList());
        
        if (!phase3Actions.isEmpty()) {
            phases.add(ActionPhase.builder()
                    .name("Low Priority Enhancements")
                    .description("Address low priority enhancements")
                    .actions(phase3Actions)
                    .estimatedDuration(calculateEstimatedDuration(phase3Actions))
                    .build());
        }
        
        return phases;
    }
    
    // Additional helper methods for architectural analysis
    private double calculateCouplingLevel(List<PatternDetectionResult> results) {
        // Calculate coupling level based on detected patterns
        return 0.5; // Placeholder
    }
    
    private double calculateCohesionLevel(List<PatternDetectionResult> results) {
        // Calculate cohesion level based on detected patterns
        return 0.7; // Placeholder
    }
    
    private int countLayeringViolations(List<PatternDetectionResult> results) {
        return (int) results.stream()
                .filter(result -> result.getPatternType().name().contains("LAYER"))
                .count();
    }
    
    private int countCircularDependencies(List<PatternDetectionResult> results) {
        return (int) results.stream()
                .filter(result -> result.getDescription().toLowerCase().contains("circular"))
                .count();
    }
    
    private boolean violatesDesignPrinciples(PatternDetectionResult result) {
        return result.getPatternCategory() == PatternCategory.ANTI_PATTERN;
    }
    
    private DesignPrincipleViolation createDesignPrincipleViolation(PatternDetectionResult result) {
        return DesignPrincipleViolation.builder()
                .principle(determineViolatedPrinciple(result))
                .description(result.getDescription())
                .severity(result.getSeverity())
                .filePath(result.getFilePath())
                .lineNumber(result.getStartLine())
                .build();
    }
    
    private String determineViolatedPrinciple(PatternDetectionResult result) {
        if (result.getPatternType() == PatternType.GOD_CLASS) {
            return "Single Responsibility Principle";
        }
        return "General Design Principle";
    }
    
    private String calculateOverallRisk(List<SecurityVulnerability> vulnerabilities) {
        long criticalCount = vulnerabilities.stream()
                .filter(v -> v.getSeverity() == PatternSeverity.CRITICAL)
                .count();
        
        if (criticalCount > 0) return "HIGH";
        
        long highCount = vulnerabilities.stream()
                .filter(v -> v.getSeverity() == PatternSeverity.HIGH)
                .count();
        
        if (highCount > 3) return "HIGH";
        if (highCount > 0) return "MEDIUM";
        
        return "LOW";
    }
    
    private List<String> generateSecurityActions(List<SecurityVulnerability> vulnerabilities) {
        List<String> actions = new ArrayList<>();
        
        if (vulnerabilities.stream().anyMatch(v -> v.getSeverity() == PatternSeverity.CRITICAL)) {
            actions.add("Immediate security review required");
        }
        
        actions.add("Implement security best practices");
        actions.add("Add security testing to CI/CD pipeline");
        
        return actions;
    }
}