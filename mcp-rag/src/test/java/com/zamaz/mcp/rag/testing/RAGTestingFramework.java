package com.zamaz.mcp.rag.testing;

import com.zamaz.mcp.rag.domain.model.Document;
import com.zamaz.mcp.rag.domain.model.Embedding;
import com.zamaz.mcp.rag.domain.model.SearchResult;
import com.zamaz.mcp.rag.service.RAGService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive testing framework for RAG (Retrieval-Augmented Generation) functionality.
 * Provides tools for testing retrieval quality, embedding consistency, and end-to-end RAG flows.
 */
public class RAGTestingFramework {

    private final InMemoryVectorStore vectorStore;
    private final MockEmbeddingProvider embeddingProvider;
    private final RAGService ragService;
    
    // Test configuration
    private double relevanceThreshold = 0.7;
    private int defaultTopK = 5;
    private String defaultCollection = "test-collection";
    
    // Test data
    private final List<TestDocument> testDocuments = new ArrayList<>();
    private final List<TestQuery> testQueries = new ArrayList<>();
    private final Map<String, Set<String>> relevanceJudgments = new HashMap<>();

    public RAGTestingFramework(RAGService ragService) {
        this.ragService = ragService;
        this.vectorStore = new InMemoryVectorStore();
        this.embeddingProvider = MockEmbeddingProvider.forSemanticTesting();
        initializeTestData();
    }

    public RAGTestingFramework(InMemoryVectorStore vectorStore, MockEmbeddingProvider embeddingProvider, RAGService ragService) {
        this.vectorStore = vectorStore;
        this.embeddingProvider = embeddingProvider;
        this.ragService = ragService;
        initializeTestData();
    }

    /**
     * Tests retrieval quality using precision and recall metrics.
     */
    public RetrievalQualityReport testRetrievalQuality() {
        RetrievalQualityReport report = new RetrievalQualityReport();
        
        for (TestQuery query : testQueries) {
            List<SearchResult> results = vectorStore.search(
                defaultCollection, 
                embeddingProvider.generateEmbedding(query.text), 
                defaultTopK
            );
            
            Set<String> relevantDocs = relevanceJudgments.getOrDefault(query.id, Collections.emptySet());
            Set<String> retrievedDocs = results.stream()
                .map(SearchResult::getId)
                .collect(Collectors.toSet());
            
            double precision = calculatePrecision(retrievedDocs, relevantDocs);
            double recall = calculateRecall(retrievedDocs, relevantDocs);
            double f1 = calculateF1(precision, recall);
            
            QueryResult queryResult = new QueryResult(query.id, query.text, precision, recall, f1, results.size());
            report.addQueryResult(queryResult);
        }
        
        return report;
    }

    /**
     * Tests embedding consistency and similarity patterns.
     */
    public EmbeddingQualityReport testEmbeddingQuality() {
        EmbeddingQualityReport report = new EmbeddingQualityReport();
        
        // Test semantic similarity
        List<SimilarityTest> similarityTests = createSimilarityTests();
        for (SimilarityTest test : similarityTests) {
            double similarity = embeddingProvider.calculateSimilarity(test.text1, test.text2);
            boolean passed = (similarity >= test.expectedMinSimilarity) && 
                           (similarity <= test.expectedMaxSimilarity);
            
            report.addSimilarityResult(new SimilarityResult(
                test.text1, test.text2, similarity, test.expectedMinSimilarity, 
                test.expectedMaxSimilarity, passed
            ));
        }
        
        // Test embedding stability
        for (TestDocument doc : testDocuments.subList(0, Math.min(10, testDocuments.size()))) {
            Embedding emb1 = embeddingProvider.generateEmbedding(doc.content);
            Embedding emb2 = embeddingProvider.generateEmbedding(doc.content);
            
            double stability = cosineSimilarity(emb1.getVector(), emb2.getVector());
            report.addStabilityResult(new StabilityResult(doc.id, doc.content, stability));
        }
        
        return report;
    }

    /**
     * Tests end-to-end RAG pipeline with real queries.
     */
    public RAGPipelineReport testRAGPipeline() {
        RAGPipelineReport report = new RAGPipelineReport();
        
        for (TestQuery query : testQueries) {
            long startTime = System.currentTimeMillis();
            
            try {
                // Test the complete RAG pipeline
                String response = ragService.generateResponse(query.text, defaultCollection);
                
                long endTime = System.currentTimeMillis();
                long responseTime = endTime - startTime;
                
                // Evaluate response quality
                double relevanceScore = evaluateResponseRelevance(query.text, response);
                double coherenceScore = evaluateResponseCoherence(response);
                double factualityScore = evaluateResponseFactuality(response, query.text);
                
                RAGResult ragResult = new RAGResult(
                    query.id, query.text, response, responseTime,
                    relevanceScore, coherenceScore, factualityScore
                );
                
                report.addResult(ragResult);
                
            } catch (Exception e) {
                report.addError(query.id, e);
            }
        }
        
        return report;
    }

    /**
     * Tests retrieval performance under various conditions.
     */
    public PerformanceReport testRetrievalPerformance() {
        PerformanceReport report = new PerformanceReport();
        
        // Test with different collection sizes
        int[] collectionSizes = {100, 1000, 5000, 10000};
        
        for (int size : collectionSizes) {
            String collectionName = "perf-test-" + size;
            setupPerformanceCollection(collectionName, size);
            
            // Measure search performance
            long totalTime = 0;
            int iterations = 10;
            
            for (int i = 0; i < iterations; i++) {
                String query = "test query " + i;
                Embedding queryEmbedding = embeddingProvider.generateEmbedding(query);
                
                long start = System.nanoTime();
                vectorStore.search(collectionName, queryEmbedding, defaultTopK);
                long end = System.nanoTime();
                
                totalTime += (end - start);
            }
            
            double avgTimeMs = (totalTime / iterations) / 1_000_000.0;
            report.addPerformanceResult(size, avgTimeMs);
            
            // Cleanup
            vectorStore.deleteCollection(collectionName);
        }
        
        return report;
    }

    /**
     * Creates a benchmark dataset for testing.
     */
    public void createBenchmarkDataset(String collectionName, int documentCount) {
        vectorStore.createCollection(collectionName);
        
        for (int i = 0; i < documentCount; i++) {
            Document doc = createBenchmarkDocument(i);
            Embedding embedding = embeddingProvider.generateEmbedding(doc);
            vectorStore.addDocument(collectionName, doc, embedding);
        }
    }

    /**
     * Validates the correctness of retrieval results.
     */
    public ValidationReport validateRetrievalResults() {
        ValidationReport report = new ValidationReport();
        
        // Test edge cases
        report.addValidation("Empty query", validateEmptyQuery());
        report.addValidation("Very long query", validateLongQuery());
        report.addValidation("Special characters", validateSpecialCharacters());
        report.addValidation("Duplicate documents", validateDuplicateHandling());
        report.addValidation("Collection boundaries", validateCollectionBoundaries());
        
        return report;
    }

    // Helper methods for quality evaluation

    private double calculatePrecision(Set<String> retrieved, Set<String> relevant) {
        if (retrieved.isEmpty()) return 0.0;
        
        Set<String> intersection = new HashSet<>(retrieved);
        intersection.retainAll(relevant);
        
        return (double) intersection.size() / retrieved.size();
    }

    private double calculateRecall(Set<String> retrieved, Set<String> relevant) {
        if (relevant.isEmpty()) return 1.0;
        
        Set<String> intersection = new HashSet<>(retrieved);
        intersection.retainAll(relevant);
        
        return (double) intersection.size() / relevant.size();
    }

    private double calculateF1(double precision, double recall) {
        if (precision + recall == 0) return 0.0;
        return 2 * (precision * recall) / (precision + recall);
    }

    private double cosineSimilarity(double[] vector1, double[] vector2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) return 0.0;
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private double evaluateResponseRelevance(String query, String response) {
        // Simple relevance scoring based on keyword overlap
        String[] queryWords = query.toLowerCase().split("\\s+");
        String[] responseWords = response.toLowerCase().split("\\s+");
        
        Set<String> querySet = Set.of(queryWords);
        Set<String> responseSet = Set.of(responseWords);
        
        Set<String> intersection = new HashSet<>(querySet);
        intersection.retainAll(responseSet);
        
        return querySet.isEmpty() ? 0.0 : (double) intersection.size() / querySet.size();
    }

    private double evaluateResponseCoherence(String response) {
        // Simple coherence scoring based on sentence structure
        String[] sentences = response.split("[.!?]+");
        if (sentences.length < 2) return 1.0;
        
        // Check for reasonable sentence lengths
        double avgLength = Arrays.stream(sentences)
            .mapToInt(String::length)
            .average()
            .orElse(0.0);
        
        return Math.min(1.0, avgLength / 50.0); // Reasonable sentence length
    }

    private double evaluateResponseFactuality(String response, String query) {
        // Placeholder for factuality evaluation
        // In a real implementation, this might check against known facts
        return 0.8; // Default moderate factuality score
    }

    // Test data initialization and management

    private void initializeTestData() {
        // Create sample test documents
        testDocuments.addAll(List.of(
            new TestDocument("doc1", "AI and Machine Learning", "Artificial intelligence and machine learning are transforming industries."),
            new TestDocument("doc2", "Climate Change", "Climate change poses significant challenges to global sustainability."),
            new TestDocument("doc3", "Quantum Computing", "Quantum computing promises to revolutionize computational capabilities."),
            new TestDocument("doc4", "Renewable Energy", "Solar and wind power are leading renewable energy sources."),
            new TestDocument("doc5", "Space Exploration", "Space exploration continues to push the boundaries of human knowledge.")
        ));
        
        // Create sample test queries
        testQueries.addAll(List.of(
            new TestQuery("q1", "What is artificial intelligence?"),
            new TestQuery("q2", "How does climate change affect the environment?"),
            new TestQuery("q3", "What are the applications of quantum computing?"),
            new TestQuery("q4", "What are the benefits of renewable energy?"),
            new TestQuery("q5", "Why is space exploration important?")
        ));
        
        // Define relevance judgments
        relevanceJudgments.put("q1", Set.of("doc1"));
        relevanceJudgments.put("q2", Set.of("doc2"));
        relevanceJudgments.put("q3", Set.of("doc3"));
        relevanceJudgments.put("q4", Set.of("doc4"));
        relevanceJudgments.put("q5", Set.of("doc5"));
        
        // Setup default collection
        setupDefaultCollection();
    }

    private void setupDefaultCollection() {
        vectorStore.createCollection(defaultCollection);
        
        for (TestDocument testDoc : testDocuments) {
            Document doc = Document.builder()
                .id(testDoc.id)
                .title(testDoc.title)
                .content(testDoc.content)
                .metadata(Map.of("source", "test"))
                .build();
            
            Embedding embedding = embeddingProvider.generateEmbedding(doc);
            vectorStore.addDocument(defaultCollection, doc, embedding);
        }
    }

    private List<SimilarityTest> createSimilarityTests() {
        return List.of(
            new SimilarityTest("machine learning", "artificial intelligence", 0.3, 0.8),
            new SimilarityTest("climate change", "global warming", 0.4, 0.9),
            new SimilarityTest("quantum computer", "quantum computing", 0.8, 1.0),
            new SimilarityTest("cat", "quantum physics", 0.0, 0.3),
            new SimilarityTest("renewable energy", "solar power", 0.3, 0.7)
        );
    }

    private void setupPerformanceCollection(String collectionName, int size) {
        vectorStore.createCollection(collectionName);
        
        for (int i = 0; i < size; i++) {
            Document doc = createBenchmarkDocument(i);
            Embedding embedding = embeddingProvider.generateEmbedding(doc);
            vectorStore.addDocument(collectionName, doc, embedding);
        }
    }

    private Document createBenchmarkDocument(int index) {
        return Document.builder()
            .id("bench-doc-" + index)
            .title("Benchmark Document " + index)
            .content("This is benchmark document number " + index + " with some content for testing.")
            .metadata(Map.of("index", index, "category", "benchmark"))
            .build();
    }

    // Validation methods

    private boolean validateEmptyQuery() {
        try {
            List<SearchResult> results = vectorStore.search(
                defaultCollection, 
                embeddingProvider.generateEmbedding(""), 
                defaultTopK
            );
            return results != null; // Should handle gracefully
        } catch (Exception e) {
            return false;
        }
    }

    private boolean validateLongQuery() {
        String longQuery = "a".repeat(10000);
        try {
            List<SearchResult> results = vectorStore.search(
                defaultCollection, 
                embeddingProvider.generateEmbedding(longQuery), 
                defaultTopK
            );
            return results != null;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean validateSpecialCharacters() {
        String specialQuery = "!@#$%^&*(){}[]|\\:;\"'<>,.?/~`";
        try {
            List<SearchResult> results = vectorStore.search(
                defaultCollection, 
                embeddingProvider.generateEmbedding(specialQuery), 
                defaultTopK
            );
            return results != null;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean validateDuplicateHandling() {
        // Test with duplicate documents
        String testCollection = "duplicate-test";
        vectorStore.createCollection(testCollection);
        
        Document doc = Document.builder()
            .id("dup-doc")
            .content("duplicate content")
            .build();
        
        Embedding embedding = embeddingProvider.generateEmbedding(doc);
        vectorStore.addDocument(testCollection, doc, embedding);
        
        // Try to add the same document again
        String id2 = vectorStore.addDocument(testCollection, doc, embedding);
        
        vectorStore.deleteCollection(testCollection);
        return id2 != null; // Should handle gracefully
    }

    private boolean validateCollectionBoundaries() {
        // Test queries across different collections
        String otherCollection = "other-test";
        vectorStore.createCollection(otherCollection);
        
        Document doc = Document.builder()
            .id("other-doc")
            .content("content in other collection")
            .build();
        
        Embedding embedding = embeddingProvider.generateEmbedding(doc);
        vectorStore.addDocument(otherCollection, doc, embedding);
        
        // Search in default collection should not return documents from other collection
        List<SearchResult> results = vectorStore.search(
            defaultCollection, 
            embedding, 
            defaultTopK
        );
        
        boolean valid = results.stream().noneMatch(r -> r.getId().equals("other-doc"));
        
        vectorStore.deleteCollection(otherCollection);
        return valid;
    }

    // Data classes for test results

    public static class TestDocument {
        public final String id;
        public final String title;
        public final String content;

        public TestDocument(String id, String title, String content) {
            this.id = id;
            this.title = title;
            this.content = content;
        }
    }

    public static class TestQuery {
        public final String id;
        public final String text;

        public TestQuery(String id, String text) {
            this.id = id;
            this.text = text;
        }
    }

    public static class SimilarityTest {
        public final String text1;
        public final String text2;
        public final double expectedMinSimilarity;
        public final double expectedMaxSimilarity;

        public SimilarityTest(String text1, String text2, double expectedMinSimilarity, double expectedMaxSimilarity) {
            this.text1 = text1;
            this.text2 = text2;
            this.expectedMinSimilarity = expectedMinSimilarity;
            this.expectedMaxSimilarity = expectedMaxSimilarity;
        }
    }

    // Report classes for test results

    public static class RetrievalQualityReport {
        private final List<QueryResult> queryResults = new ArrayList<>();

        public void addQueryResult(QueryResult result) {
            queryResults.add(result);
        }

        public List<QueryResult> getQueryResults() {
            return queryResults;
        }

        public double getAveragePrecision() {
            return queryResults.stream().mapToDouble(r -> r.precision).average().orElse(0.0);
        }

        public double getAverageRecall() {
            return queryResults.stream().mapToDouble(r -> r.recall).average().orElse(0.0);
        }

        public double getAverageF1() {
            return queryResults.stream().mapToDouble(r -> r.f1).average().orElse(0.0);
        }
    }

    public static class QueryResult {
        public final String queryId;
        public final String queryText;
        public final double precision;
        public final double recall;
        public final double f1;
        public final int resultCount;

        public QueryResult(String queryId, String queryText, double precision, double recall, double f1, int resultCount) {
            this.queryId = queryId;
            this.queryText = queryText;
            this.precision = precision;
            this.recall = recall;
            this.f1 = f1;
            this.resultCount = resultCount;
        }
    }

    public static class EmbeddingQualityReport {
        private final List<SimilarityResult> similarityResults = new ArrayList<>();
        private final List<StabilityResult> stabilityResults = new ArrayList<>();

        public void addSimilarityResult(SimilarityResult result) {
            similarityResults.add(result);
        }

        public void addStabilityResult(StabilityResult result) {
            stabilityResults.add(result);
        }

        public List<SimilarityResult> getSimilarityResults() {
            return similarityResults;
        }

        public List<StabilityResult> getStabilityResults() {
            return stabilityResults;
        }

        public double getSimilarityTestPassRate() {
            if (similarityResults.isEmpty()) return 0.0;
            long passed = similarityResults.stream().mapToLong(r -> r.passed ? 1 : 0).sum();
            return (double) passed / similarityResults.size();
        }

        public double getAverageStability() {
            return stabilityResults.stream().mapToDouble(r -> r.stability).average().orElse(0.0);
        }
    }

    public static class SimilarityResult {
        public final String text1;
        public final String text2;
        public final double actualSimilarity;
        public final double expectedMin;
        public final double expectedMax;
        public final boolean passed;

        public SimilarityResult(String text1, String text2, double actualSimilarity, double expectedMin, double expectedMax, boolean passed) {
            this.text1 = text1;
            this.text2 = text2;
            this.actualSimilarity = actualSimilarity;
            this.expectedMin = expectedMin;
            this.expectedMax = expectedMax;
            this.passed = passed;
        }
    }

    public static class StabilityResult {
        public final String documentId;
        public final String content;
        public final double stability;

        public StabilityResult(String documentId, String content, double stability) {
            this.documentId = documentId;
            this.content = content;
            this.stability = stability;
        }
    }

    public static class RAGPipelineReport {
        private final List<RAGResult> results = new ArrayList<>();
        private final Map<String, Exception> errors = new HashMap<>();

        public void addResult(RAGResult result) {
            results.add(result);
        }

        public void addError(String queryId, Exception error) {
            errors.put(queryId, error);
        }

        public List<RAGResult> getResults() {
            return results;
        }

        public Map<String, Exception> getErrors() {
            return errors;
        }

        public double getAverageResponseTime() {
            return results.stream().mapToLong(r -> r.responseTimeMs).average().orElse(0.0);
        }

        public double getAverageRelevanceScore() {
            return results.stream().mapToDouble(r -> r.relevanceScore).average().orElse(0.0);
        }
    }

    public static class RAGResult {
        public final String queryId;
        public final String query;
        public final String response;
        public final long responseTimeMs;
        public final double relevanceScore;
        public final double coherenceScore;
        public final double factualityScore;

        public RAGResult(String queryId, String query, String response, long responseTimeMs, 
                        double relevanceScore, double coherenceScore, double factualityScore) {
            this.queryId = queryId;
            this.query = query;
            this.response = response;
            this.responseTimeMs = responseTimeMs;
            this.relevanceScore = relevanceScore;
            this.coherenceScore = coherenceScore;
            this.factualityScore = factualityScore;
        }
    }

    public static class PerformanceReport {
        private final Map<Integer, Double> performanceResults = new HashMap<>();

        public void addPerformanceResult(int collectionSize, double avgTimeMs) {
            performanceResults.put(collectionSize, avgTimeMs);
        }

        public Map<Integer, Double> getPerformanceResults() {
            return performanceResults;
        }
    }

    public static class ValidationReport {
        private final Map<String, Boolean> validations = new HashMap<>();

        public void addValidation(String test, boolean passed) {
            validations.put(test, passed);
        }

        public Map<String, Boolean> getValidations() {
            return validations;
        }

        public boolean allPassed() {
            return validations.values().stream().allMatch(Boolean::booleanValue);
        }
    }
}