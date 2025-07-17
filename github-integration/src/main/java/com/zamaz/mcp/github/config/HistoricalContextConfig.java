package com.zamaz.mcp.github.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for Historical Context Awareness System
 */
@Configuration
@EnableAsync
@EnableScheduling
@ConfigurationProperties(prefix = "historical-context")
public class HistoricalContextConfig {

    private Analytics analytics = new Analytics();
    private MachineLearning machineLearning = new MachineLearning();
    private KnowledgeBase knowledgeBase = new KnowledgeBase();
    private Suggestions suggestions = new Suggestions();
    private Trends trends = new Trends();

    @Bean(name = "historicalContextTaskExecutor")
    public Executor historicalContextTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("HistoricalContext-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "mlAnalysisTaskExecutor")
    public Executor mlAnalysisTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("MLAnalysis-");
        executor.initialize();
        return executor;
    }

    // Getters and setters
    public Analytics getAnalytics() { return analytics; }
    public void setAnalytics(Analytics analytics) { this.analytics = analytics; }

    public MachineLearning getMachineLearning() { return machineLearning; }
    public void setMachineLearning(MachineLearning machineLearning) { this.machineLearning = machineLearning; }

    public KnowledgeBase getKnowledgeBase() { return knowledgeBase; }
    public void setKnowledgeBase(KnowledgeBase knowledgeBase) { this.knowledgeBase = knowledgeBase; }

    public Suggestions getSuggestions() { return suggestions; }
    public void setSuggestions(Suggestions suggestions) { this.suggestions = suggestions; }

    public Trends getTrends() { return trends; }
    public void setTrends(Trends trends) { this.trends = trends; }

    // Nested configuration classes
    public static class Analytics {
        private int defaultAnalysisMonths = 6;
        private int maxAnalysisMonths = 24;
        private boolean enableRealTimeAnalysis = true;
        private int batchSize = 100;

        // Getters and setters
        public int getDefaultAnalysisMonths() { return defaultAnalysisMonths; }
        public void setDefaultAnalysisMonths(int defaultAnalysisMonths) { this.defaultAnalysisMonths = defaultAnalysisMonths; }

        public int getMaxAnalysisMonths() { return maxAnalysisMonths; }
        public void setMaxAnalysisMonths(int maxAnalysisMonths) { this.maxAnalysisMonths = maxAnalysisMonths; }

        public boolean isEnableRealTimeAnalysis() { return enableRealTimeAnalysis; }
        public void setEnableRealTimeAnalysis(boolean enableRealTimeAnalysis) { this.enableRealTimeAnalysis = enableRealTimeAnalysis; }

        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    }

    public static class MachineLearning {
        private boolean enableMLAnalysis = true;
        private double confidenceThreshold = 0.7;
        private int trainingDataRetentionDays = 365;
        private int modelUpdateIntervalHours = 24;
        private int maxTrainingBatchSize = 1000;

        // Getters and setters
        public boolean isEnableMLAnalysis() { return enableMLAnalysis; }
        public void setEnableMLAnalysis(boolean enableMLAnalysis) { this.enableMLAnalysis = enableMLAnalysis; }

        public double getConfidenceThreshold() { return confidenceThreshold; }
        public void setConfidenceThreshold(double confidenceThreshold) { this.confidenceThreshold = confidenceThreshold; }

        public int getTrainingDataRetentionDays() { return trainingDataRetentionDays; }
        public void setTrainingDataRetentionDays(int trainingDataRetentionDays) { this.trainingDataRetentionDays = trainingDataRetentionDays; }

        public int getModelUpdateIntervalHours() { return modelUpdateIntervalHours; }
        public void setModelUpdateIntervalHours(int modelUpdateIntervalHours) { this.modelUpdateIntervalHours = modelUpdateIntervalHours; }

        public int getMaxTrainingBatchSize() { return maxTrainingBatchSize; }
        public void setMaxTrainingBatchSize(int maxTrainingBatchSize) { this.maxTrainingBatchSize = maxTrainingBatchSize; }
    }

    public static class KnowledgeBase {
        private boolean enableAutoExtraction = true;
        private int extractionBatchSize = 50;
        private double effectivenessThreshold = 0.6;
        private int approvalTimeoutDays = 30;
        private int maxSimilarEntries = 10;

        // Getters and setters
        public boolean isEnableAutoExtraction() { return enableAutoExtraction; }
        public void setEnableAutoExtraction(boolean enableAutoExtraction) { this.enableAutoExtraction = enableAutoExtraction; }

        public int getExtractionBatchSize() { return extractionBatchSize; }
        public void setExtractionBatchSize(int extractionBatchSize) { this.extractionBatchSize = extractionBatchSize; }

        public double getEffectivenessThreshold() { return effectivenessThreshold; }
        public void setEffectivenessThreshold(double effectivenessThreshold) { this.effectivenessThreshold = effectivenessThreshold; }

        public int getApprovalTimeoutDays() { return approvalTimeoutDays; }
        public void setApprovalTimeoutDays(int approvalTimeoutDays) { this.approvalTimeoutDays = approvalTimeoutDays; }

        public int getMaxSimilarEntries() { return maxSimilarEntries; }
        public void setMaxSimilarEntries(int maxSimilarEntries) { this.maxSimilarEntries = maxSimilarEntries; }
    }

    public static class Suggestions {
        private boolean enablePersonalization = true;
        private int maxSuggestionsPerRequest = 10;
        private double relevanceThreshold = 0.3;
        private int suggestionExpirationDays = 30;
        private boolean enableContextualSuggestions = true;

        // Getters and setters
        public boolean isEnablePersonalization() { return enablePersonalization; }
        public void setEnablePersonalization(boolean enablePersonalization) { this.enablePersonalization = enablePersonalization; }

        public int getMaxSuggestionsPerRequest() { return maxSuggestionsPerRequest; }
        public void setMaxSuggestionsPerRequest(int maxSuggestionsPerRequest) { this.maxSuggestionsPerRequest = maxSuggestionsPerRequest; }

        public double getRelevanceThreshold() { return relevanceThreshold; }
        public void setRelevanceThreshold(double relevanceThreshold) { this.relevanceThreshold = relevanceThreshold; }

        public int getSuggestionExpirationDays() { return suggestionExpirationDays; }
        public void setSuggestionExpirationDays(int suggestionExpirationDays) { this.suggestionExpirationDays = suggestionExpirationDays; }

        public boolean isEnableContextualSuggestions() { return enableContextualSuggestions; }
        public void setEnableContextualSuggestions(boolean enableContextualSuggestions) { this.enableContextualSuggestions = enableContextualSuggestions; }
    }

    public static class Trends {
        private boolean enableTrendAnalysis = true;
        private int trendCalculationIntervalHours = 24;
        private int trendDataRetentionDays = 730;
        private double significanceThreshold = 0.05;
        private int predictionAccuracyThreshold = 80;

        // Getters and setters
        public boolean isEnableTrendAnalysis() { return enableTrendAnalysis; }
        public void setEnableTrendAnalysis(boolean enableTrendAnalysis) { this.enableTrendAnalysis = enableTrendAnalysis; }

        public int getTrendCalculationIntervalHours() { return trendCalculationIntervalHours; }
        public void setTrendCalculationIntervalHours(int trendCalculationIntervalHours) { this.trendCalculationIntervalHours = trendCalculationIntervalHours; }

        public int getTrendDataRetentionDays() { return trendDataRetentionDays; }
        public void setTrendDataRetentionDays(int trendDataRetentionDays) { this.trendDataRetentionDays = trendDataRetentionDays; }

        public double getSignificanceThreshold() { return significanceThreshold; }
        public void setSignificanceThreshold(double significanceThreshold) { this.significanceThreshold = significanceThreshold; }

        public int getPredictionAccuracyThreshold() { return predictionAccuracyThreshold; }
        public void setPredictionAccuracyThreshold(int predictionAccuracyThreshold) { this.predictionAccuracyThreshold = predictionAccuracyThreshold; }
    }
}