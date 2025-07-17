package com.zamaz.mcp.pattern.ml;

import com.zamaz.mcp.pattern.model.CodeAnalysisContext;
import com.zamaz.mcp.pattern.model.PatternDetectionResult;
import com.zamaz.mcp.pattern.model.TeamCodingPattern;
import com.zamaz.mcp.pattern.model.PatternLearningModel;
import com.zamaz.mcp.pattern.repository.TeamPatternRepository;
import com.zamaz.mcp.pattern.repository.PatternLearningRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for learning team-specific coding patterns using machine learning techniques.
 * 
 * This service analyzes historical code changes, pattern detections, and team feedback
 * to learn team-specific patterns and improve pattern recognition accuracy.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TeamPatternLearningService {
    
    private final TeamPatternRepository teamPatternRepository;
    private final PatternLearningRepository patternLearningRepository;
    private final PatternFeatureExtractor featureExtractor;
    private final PatternClassifier patternClassifier;
    private final PatternFeedbackProcessor feedbackProcessor;
    
    /**
     * Learn patterns from a codebase analysis session.
     * 
     * @param organizationId Organization identifier
     * @param analysisResults Results from pattern detection
     * @param codeContexts Code contexts that were analyzed
     */
    @Transactional
    public void learnFromAnalysis(String organizationId, 
                                 List<PatternDetectionResult> analysisResults,
                                 List<CodeAnalysisContext> codeContexts) {
        
        log.info("Starting pattern learning for organization: {}", organizationId);
        
        try {
            // Extract features from code contexts
            List<PatternFeatureVector> featureVectors = extractFeatures(codeContexts, analysisResults);
            
            // Update learning model with new data
            PatternLearningModel model = getOrCreateLearningModel(organizationId);
            model = updateModelWithNewData(model, featureVectors);
            
            // Identify new team-specific patterns
            List<TeamCodingPattern> newPatterns = identifyNewPatterns(model, featureVectors);
            
            // Save discovered patterns
            saveTeamPatterns(organizationId, newPatterns);
            
            // Update model accuracy metrics
            updateModelAccuracy(model, analysisResults);
            
            // Save updated model
            patternLearningRepository.save(model);
            
            log.info("Pattern learning completed. Discovered {} new patterns", newPatterns.size());
            
        } catch (Exception e) {
            log.error("Error during pattern learning for organization: {}", organizationId, e);
            throw new RuntimeException("Pattern learning failed", e);
        }
    }
    
    /**
     * Process feedback on pattern detection results to improve accuracy.
     * 
     * @param organizationId Organization identifier
     * @param feedback Feedback from developers on pattern detection results
     */
    @Transactional
    public void processFeedback(String organizationId, List<PatternFeedback> feedback) {
        log.info("Processing pattern feedback for organization: {}", organizationId);
        
        try {
            PatternLearningModel model = getOrCreateLearningModel(organizationId);
            
            // Process feedback and update model
            PatternFeedbackAnalysis analysis = feedbackProcessor.processFeedback(feedback);
            model = adjustModelBasedOnFeedback(model, analysis);
            
            // Update pattern confidence scores
            updatePatternConfidenceScores(organizationId, analysis);
            
            // Save updated model
            patternLearningRepository.save(model);
            
            log.info("Feedback processing completed. Processed {} feedback items", feedback.size());
            
        } catch (Exception e) {
            log.error("Error processing feedback for organization: {}", organizationId, e);
            throw new RuntimeException("Feedback processing failed", e);
        }
    }
    
    /**
     * Get personalized pattern recommendations for a team.
     * 
     * @param organizationId Organization identifier
     * @param codeContext Code context to analyze
     * @return List of recommended patterns
     */
    public List<PatternRecommendation> getPersonalizedRecommendations(String organizationId, 
                                                                     CodeAnalysisContext codeContext) {
        try {
            PatternLearningModel model = getOrCreateLearningModel(organizationId);
            List<TeamCodingPattern> teamPatterns = getTeamPatterns(organizationId);
            
            // Extract features from the code context
            PatternFeatureVector features = featureExtractor.extractFeatures(codeContext);
            
            // Get recommendations from the model
            List<PatternPrediction> predictions = patternClassifier.predict(model, features);
            
            // Convert predictions to recommendations
            return predictions.stream()
                    .map(prediction -> createRecommendation(prediction, teamPatterns))
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(PatternRecommendation::getConfidence).reversed())
                    .limit(10)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Error getting personalized recommendations for organization: {}", organizationId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Adapt pattern detection thresholds based on team preferences.
     * 
     * @param organizationId Organization identifier
     * @return Adapted detection configuration
     */
    public PatternDetectionConfig getAdaptedDetectionConfig(String organizationId) {
        try {
            PatternLearningModel model = getOrCreateLearningModel(organizationId);
            List<TeamCodingPattern> teamPatterns = getTeamPatterns(organizationId);
            
            return PatternDetectionConfig.builder()
                    .organizationId(organizationId)
                    .adaptedThresholds(calculateAdaptedThresholds(model, teamPatterns))
                    .prioritizedPatterns(getPrioritizedPatterns(teamPatterns))
                    .suppressedPatterns(getSuppressedPatterns(teamPatterns))
                    .customRules(getCustomRules(teamPatterns))
                    .confidenceAdjustments(getConfidenceAdjustments(model))
                    .build();
                    
        } catch (Exception e) {
            log.error("Error getting adapted detection config for organization: {}", organizationId, e);
            return PatternDetectionConfig.defaultConfig();
        }
    }
    
    /**
     * Train the model with historical pattern data.
     * 
     * @param organizationId Organization identifier
     * @param historicalData Historical pattern detection data
     */
    @Transactional
    public void trainModel(String organizationId, List<HistoricalPatternData> historicalData) {
        log.info("Training pattern model for organization: {} with {} data points", 
                 organizationId, historicalData.size());
        
        try {
            PatternLearningModel model = getOrCreateLearningModel(organizationId);
            
            // Prepare training data
            List<PatternTrainingExample> trainingExamples = prepareTrainingData(historicalData);
            
            // Train the model
            PatternModelTrainer trainer = new PatternModelTrainer();
            model = trainer.train(model, trainingExamples);
            
            // Validate model performance
            ModelValidationResult validation = validateModel(model, trainingExamples);
            model.setValidationResult(validation);
            
            // Save trained model
            patternLearningRepository.save(model);
            
            log.info("Model training completed. Accuracy: {:.2f}%", validation.getAccuracy() * 100);
            
        } catch (Exception e) {
            log.error("Error training model for organization: {}", organizationId, e);
            throw new RuntimeException("Model training failed", e);
        }
    }
    
    /**
     * Get model performance metrics for a team.
     * 
     * @param organizationId Organization identifier
     * @return Model performance metrics
     */
    public ModelPerformanceMetrics getModelPerformance(String organizationId) {
        try {
            PatternLearningModel model = getOrCreateLearningModel(organizationId);
            
            return ModelPerformanceMetrics.builder()
                    .organizationId(organizationId)
                    .accuracy(model.getAccuracy())
                    .precision(model.getPrecision())
                    .recall(model.getRecall())
                    .f1Score(model.getF1Score())
                    .trainingDataSize(model.getTrainingDataSize())
                    .lastTrainingDate(model.getLastTrainingDate())
                    .patternCoverage(calculatePatternCoverage(model))
                    .improvementSuggestions(generateImprovementSuggestions(model))
                    .build();
                    
        } catch (Exception e) {
            log.error("Error getting model performance for organization: {}", organizationId, e);
            return ModelPerformanceMetrics.defaultMetrics(organizationId);
        }
    }
    
    // Private helper methods
    
    private List<PatternFeatureVector> extractFeatures(List<CodeAnalysisContext> contexts, 
                                                      List<PatternDetectionResult> results) {
        return contexts.stream()
                .map(context -> {
                    List<PatternDetectionResult> contextResults = results.stream()
                            .filter(result -> result.getFilePath().equals(context.getFilePath()))
                            .collect(Collectors.toList());
                    return featureExtractor.extractFeatures(context, contextResults);
                })
                .collect(Collectors.toList());
    }
    
    private PatternLearningModel getOrCreateLearningModel(String organizationId) {
        return patternLearningRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> {
                    PatternLearningModel newModel = PatternLearningModel.builder()
                            .organizationId(organizationId)
                            .createdAt(LocalDateTime.now())
                            .version(1)
                            .modelType("ENSEMBLE")
                            .accuracy(0.0)
                            .precision(0.0)
                            .recall(0.0)
                            .f1Score(0.0)
                            .trainingDataSize(0)
                            .build();
                    return patternLearningRepository.save(newModel);
                });
    }
    
    private PatternLearningModel updateModelWithNewData(PatternLearningModel model, 
                                                       List<PatternFeatureVector> featureVectors) {
        // Implement incremental learning logic
        PatternIncrementalLearner learner = new PatternIncrementalLearner();
        return learner.updateModel(model, featureVectors);
    }
    
    private List<TeamCodingPattern> identifyNewPatterns(PatternLearningModel model, 
                                                       List<PatternFeatureVector> featureVectors) {
        // Implement pattern discovery logic using clustering and anomaly detection
        PatternDiscoveryEngine discoveryEngine = new PatternDiscoveryEngine();
        return discoveryEngine.discoverPatterns(model, featureVectors);
    }
    
    private void saveTeamPatterns(String organizationId, List<TeamCodingPattern> patterns) {
        patterns.forEach(pattern -> {
            pattern.setOrganizationId(organizationId);
            pattern.setCreatedAt(LocalDateTime.now());
            teamPatternRepository.save(pattern);
        });
    }
    
    private void updateModelAccuracy(PatternLearningModel model, List<PatternDetectionResult> results) {
        // Calculate accuracy metrics based on validation results
        ModelAccuracyCalculator calculator = new ModelAccuracyCalculator();
        ModelAccuracyMetrics metrics = calculator.calculateAccuracy(model, results);
        
        model.setAccuracy(metrics.getAccuracy());
        model.setPrecision(metrics.getPrecision());
        model.setRecall(metrics.getRecall());
        model.setF1Score(metrics.getF1Score());
        model.setLastUpdated(LocalDateTime.now());
    }
    
    private PatternLearningModel adjustModelBasedOnFeedback(PatternLearningModel model, 
                                                          PatternFeedbackAnalysis analysis) {
        // Implement model adjustment based on feedback
        FeedbackBasedModelAdjuster adjuster = new FeedbackBasedModelAdjuster();
        return adjuster.adjustModel(model, analysis);
    }
    
    private void updatePatternConfidenceScores(String organizationId, PatternFeedbackAnalysis analysis) {
        List<TeamCodingPattern> patterns = getTeamPatterns(organizationId);
        
        analysis.getPatternAdjustments().forEach((patternType, adjustment) -> {
            patterns.stream()
                    .filter(pattern -> pattern.getPatternType().equals(patternType))
                    .forEach(pattern -> {
                        double newConfidence = pattern.getConfidenceScore() + adjustment;
                        pattern.setConfidenceScore(Math.max(0.0, Math.min(1.0, newConfidence)));
                        teamPatternRepository.save(pattern);
                    });
        });
    }
    
    private List<TeamCodingPattern> getTeamPatterns(String organizationId) {
        return teamPatternRepository.findByOrganizationId(organizationId);
    }
    
    private PatternRecommendation createRecommendation(PatternPrediction prediction, 
                                                      List<TeamCodingPattern> teamPatterns) {
        // Find matching team pattern
        TeamCodingPattern matchingPattern = teamPatterns.stream()
                .filter(pattern -> pattern.getPatternType().equals(prediction.getPatternType()))
                .findFirst()
                .orElse(null);
        
        if (matchingPattern == null) {
            return null;
        }
        
        return PatternRecommendation.builder()
                .patternType(prediction.getPatternType())
                .confidence(prediction.getConfidence())
                .reasoning(prediction.getReasoning())
                .teamPattern(matchingPattern)
                .priority(calculatePriority(prediction, matchingPattern))
                .applicability(calculateApplicability(prediction, matchingPattern))
                .build();
    }
    
    private Map<String, Double> calculateAdaptedThresholds(PatternLearningModel model, 
                                                          List<TeamCodingPattern> teamPatterns) {
        // Calculate adapted thresholds based on team preferences and model performance
        Map<String, Double> thresholds = new HashMap<>();
        
        teamPatterns.forEach(pattern -> {
            double baseThreshold = 0.7; // Default threshold
            double teamPreference = pattern.getTeamPreferenceScore();
            double modelAccuracy = model.getAccuracy();
            
            // Adjust threshold based on team preference and model accuracy
            double adjustedThreshold = baseThreshold * (1 - teamPreference * 0.2) * (1 + modelAccuracy * 0.1);
            thresholds.put(pattern.getPatternType().name(), adjustedThreshold);
        });
        
        return thresholds;
    }
    
    private List<String> getPrioritizedPatterns(List<TeamCodingPattern> teamPatterns) {
        return teamPatterns.stream()
                .filter(pattern -> pattern.getPriority() > 0.7)
                .sorted(Comparator.comparing(TeamCodingPattern::getPriority).reversed())
                .map(pattern -> pattern.getPatternType().name())
                .collect(Collectors.toList());
    }
    
    private List<String> getSuppressedPatterns(List<TeamCodingPattern> teamPatterns) {
        return teamPatterns.stream()
                .filter(pattern -> pattern.isSuppressed())
                .map(pattern -> pattern.getPatternType().name())
                .collect(Collectors.toList());
    }
    
    private List<String> getCustomRules(List<TeamCodingPattern> teamPatterns) {
        return teamPatterns.stream()
                .filter(pattern -> pattern.hasCustomRules())
                .flatMap(pattern -> pattern.getCustomRules().stream())
                .collect(Collectors.toList());
    }
    
    private Map<String, Double> getConfidenceAdjustments(PatternLearningModel model) {
        // Calculate confidence adjustments based on model performance
        Map<String, Double> adjustments = new HashMap<>();
        
        // Adjust confidence based on model accuracy
        double accuracyAdjustment = (model.getAccuracy() - 0.5) * 0.2;
        
        // Apply adjustment to all patterns
        Arrays.stream(PatternType.values())
                .forEach(patternType -> {
                    adjustments.put(patternType.name(), accuracyAdjustment);
                });
        
        return adjustments;
    }
    
    private List<PatternTrainingExample> prepareTrainingData(List<HistoricalPatternData> historicalData) {
        return historicalData.stream()
                .map(data -> PatternTrainingExample.builder()
                        .features(data.getFeatures())
                        .label(data.getLabel())
                        .weight(data.getWeight())
                        .timestamp(data.getTimestamp())
                        .build())
                .collect(Collectors.toList());
    }
    
    private ModelValidationResult validateModel(PatternLearningModel model, 
                                               List<PatternTrainingExample> examples) {
        // Implement cross-validation
        ModelValidator validator = new ModelValidator();
        return validator.validate(model, examples);
    }
    
    private double calculatePatternCoverage(PatternLearningModel model) {
        // Calculate what percentage of known patterns the model can detect
        int totalPatterns = PatternType.values().length;
        int coveredPatterns = model.getCoveredPatterns().size();
        return (double) coveredPatterns / totalPatterns;
    }
    
    private List<String> generateImprovementSuggestions(PatternLearningModel model) {
        List<String> suggestions = new ArrayList<>();
        
        if (model.getAccuracy() < 0.8) {
            suggestions.add("Increase training data size to improve accuracy");
        }
        
        if (model.getPrecision() < 0.7) {
            suggestions.add("Adjust detection thresholds to reduce false positives");
        }
        
        if (model.getRecall() < 0.7) {
            suggestions.add("Review pattern definitions to capture more instances");
        }
        
        if (model.getTrainingDataSize() < 1000) {
            suggestions.add("Collect more training examples for better model performance");
        }
        
        return suggestions;
    }
    
    private PatternPriority calculatePriority(PatternPrediction prediction, TeamCodingPattern teamPattern) {
        double priorityScore = (prediction.getConfidence() + teamPattern.getTeamPreferenceScore()) / 2;
        
        if (priorityScore > 0.8) return PatternPriority.HIGH;
        if (priorityScore > 0.6) return PatternPriority.MEDIUM;
        return PatternPriority.LOW;
    }
    
    private double calculateApplicability(PatternPrediction prediction, TeamCodingPattern teamPattern) {
        // Calculate how applicable this pattern is to the current context
        return (prediction.getConfidence() * 0.6) + (teamPattern.getUsageFrequency() * 0.4);
    }
}