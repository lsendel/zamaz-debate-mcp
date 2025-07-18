package com.zamaz.mcp.rag.testing;

import com.zamaz.mcp.rag.domain.model.Document;
import com.zamaz.mcp.rag.domain.model.Embedding;
import com.zamaz.mcp.rag.service.EmbeddingService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Mock embedding provider that generates deterministic embeddings for testing.
 * Supports various embedding strategies and provides consistent results.
 */
public class MockEmbeddingProvider implements EmbeddingService {

    private final int dimensions;
    private final EmbeddingStrategy strategy;
    private final Map<String, double[]> embeddingCache = new ConcurrentHashMap<>();
    private final Random random;
    private double baseValue = 0.1;
    private boolean normalize = true;
    
    // Usage tracking
    private int embeddingRequestCount = 0;
    private final List<String> processedTexts = new ArrayList<>();
    private final Map<String, Integer> textFrequency = new HashMap<>();

    public MockEmbeddingProvider() {
        this(384, EmbeddingStrategy.HASH_BASED);
    }

    public MockEmbeddingProvider(int dimensions) {
        this(dimensions, EmbeddingStrategy.HASH_BASED);
    }

    public MockEmbeddingProvider(int dimensions, EmbeddingStrategy strategy) {
        this.dimensions = dimensions;
        this.strategy = strategy;
        this.random = new Random(42); // Fixed seed for deterministic results
    }

    @Override
    public Embedding generateEmbedding(String text) {
        embeddingRequestCount++;
        processedTexts.add(text);
        textFrequency.merge(text, 1, Integer::sum);
        
        // Check cache first
        if (embeddingCache.containsKey(text)) {
            return new Embedding(embeddingCache.get(text));
        }
        
        double[] vector = strategy.generateVector(text, dimensions, random, baseValue);
        
        if (normalize) {
            vector = normalizeVector(vector);
        }
        
        embeddingCache.put(text, vector);
        return new Embedding(vector);
    }

    @Override
    public List<Embedding> generateEmbeddings(List<String> texts) {
        return texts.stream()
            .map(this::generateEmbedding)
            .toList();
    }

    @Override
    public Embedding generateEmbedding(Document document) {
        String text = document.getContent();
        
        // Include title and metadata in embedding if available
        StringBuilder fullText = new StringBuilder(text);
        
        if (document.getTitle() != null) {
            fullText.insert(0, document.getTitle() + " ");
        }
        
        if (document.getMetadata() != null) {
            document.getMetadata().forEach((key, value) -> {
                if (value instanceof String) {
                    fullText.append(" ").append(value);
                }
            });
        }
        
        return generateEmbedding(fullText.toString());
    }

    @Override
    public int getDimensions() {
        return dimensions;
    }

    @Override
    public String getModelName() {
        return "mock-embedding-model-" + dimensions + "d";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    // Configuration methods

    public MockEmbeddingProvider withBaseValue(double baseValue) {
        this.baseValue = baseValue;
        return this;
    }

    public MockEmbeddingProvider withNormalization(boolean normalize) {
        this.normalize = normalize;
        return this;
    }

    public MockEmbeddingProvider withSeed(long seed) {
        random.setSeed(seed);
        return this;
    }

    public MockEmbeddingProvider withPrecomputedEmbedding(String text, double[] embedding) {
        if (embedding.length != dimensions) {
            throw new IllegalArgumentException("Embedding dimension mismatch");
        }
        embeddingCache.put(text, embedding.clone());
        return this;
    }

    // Testing utilities

    public void clearCache() {
        embeddingCache.clear();
    }

    public void reset() {
        embeddingCache.clear();
        embeddingRequestCount = 0;
        processedTexts.clear();
        textFrequency.clear();
    }

    public int getRequestCount() {
        return embeddingRequestCount;
    }

    public List<String> getProcessedTexts() {
        return new ArrayList<>(processedTexts);
    }

    public Map<String, Integer> getTextFrequency() {
        return new HashMap<>(textFrequency);
    }

    public boolean hasCachedEmbedding(String text) {
        return embeddingCache.containsKey(text);
    }

    public double[] getCachedEmbedding(String text) {
        return embeddingCache.get(text);
    }

    public int getCacheSize() {
        return embeddingCache.size();
    }

    // Similarity testing utilities

    public double calculateSimilarity(String text1, String text2) {
        Embedding emb1 = generateEmbedding(text1);
        Embedding emb2 = generateEmbedding(text2);
        return cosineSimilarity(emb1.getVector(), emb2.getVector());
    }

    public List<String> findSimilarTexts(String queryText, List<String> candidates, int topK) {
        Embedding queryEmbedding = generateEmbedding(queryText);
        
        return candidates.stream()
            .map(text -> new SimilarityResult(text, calculateSimilarity(queryText, text)))
            .sorted((a, b) -> Double.compare(b.similarity, a.similarity))
            .limit(topK)
            .map(result -> result.text)
            .toList();
    }

    // Helper methods

    private double[] normalizeVector(double[] vector) {
        double norm = 0.0;
        for (double value : vector) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        
        if (norm == 0.0) {
            return vector;
        }
        
        double[] normalized = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = vector[i] / norm;
        }
        
        return normalized;
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
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    // Embedding strategies

    public enum EmbeddingStrategy {
        HASH_BASED(MockEmbeddingProvider::hashBasedEmbedding),
        RANDOM(MockEmbeddingProvider::randomEmbedding),
        WORD_COUNT(MockEmbeddingProvider::wordCountEmbedding),
        CHARACTER_BASED(MockEmbeddingProvider::characterBasedEmbedding),
        SEMANTIC_CLUSTERS(MockEmbeddingProvider::semanticClusterEmbedding);

        private final EmbeddingGenerator generator;

        EmbeddingStrategy(EmbeddingGenerator generator) {
            this.generator = generator;
        }

        public double[] generateVector(String text, int dimensions, Random random, double baseValue) {
            return generator.generate(text, dimensions, random, baseValue);
        }
    }

    @FunctionalInterface
    private interface EmbeddingGenerator {
        double[] generate(String text, int dimensions, Random random, double baseValue);
    }

    private static double[] hashBasedEmbedding(String text, int dimensions, Random random, double baseValue) {
        // Use text hash to seed random generator for deterministic results
        Random textRandom = new Random(text.hashCode());
        double[] vector = new double[dimensions];
        
        for (int i = 0; i < dimensions; i++) {
            vector[i] = baseValue + textRandom.nextGaussian() * 0.1;
        }
        
        return vector;
    }

    private static double[] randomEmbedding(String text, int dimensions, Random random, double baseValue) {
        double[] vector = new double[dimensions];
        
        for (int i = 0; i < dimensions; i++) {
            vector[i] = random.nextGaussian();
        }
        
        return vector;
    }

    private static double[] wordCountEmbedding(String text, int dimensions, Random random, double baseValue) {
        String[] words = text.toLowerCase().split("\\s+");
        Map<String, Integer> wordCounts = new HashMap<>();
        
        for (String word : words) {
            wordCounts.merge(word, 1, Integer::sum);
        }
        
        double[] vector = new double[dimensions];
        int index = 0;
        
        for (Map.Entry<String, Integer> entry : wordCounts.entrySet()) {
            if (index >= dimensions) break;
            vector[index] = entry.getValue() * baseValue;
            index++;
        }
        
        // Fill remaining dimensions with text-based values
        for (int i = index; i < dimensions; i++) {
            vector[i] = (text.hashCode() % 1000) / 1000.0 * baseValue;
        }
        
        return vector;
    }

    private static double[] characterBasedEmbedding(String text, int dimensions, Random random, double baseValue) {
        double[] vector = new double[dimensions];
        
        for (int i = 0; i < dimensions; i++) {
            if (i < text.length()) {
                vector[i] = (text.charAt(i) / 128.0) * baseValue;
            } else {
                vector[i] = baseValue * 0.1;
            }
        }
        
        return vector;
    }

    private static double[] semanticClusterEmbedding(String text, int dimensions, Random random, double baseValue) {
        // Simple semantic clustering based on word patterns
        String lowerText = text.toLowerCase();
        double[] vector = new double[dimensions];
        
        // Technology cluster
        if (lowerText.contains("ai") || lowerText.contains("machine") || lowerText.contains("algorithm")) {
            for (int i = 0; i < dimensions / 4; i++) {
                vector[i] = baseValue + 0.5;
            }
        }
        
        // Science cluster
        if (lowerText.contains("research") || lowerText.contains("study") || lowerText.contains("analysis")) {
            for (int i = dimensions / 4; i < dimensions / 2; i++) {
                vector[i] = baseValue + 0.5;
            }
        }
        
        // Business cluster
        if (lowerText.contains("market") || lowerText.contains("business") || lowerText.contains("strategy")) {
            for (int i = dimensions / 2; i < 3 * dimensions / 4; i++) {
                vector[i] = baseValue + 0.5;
            }
        }
        
        // General cluster
        for (int i = 3 * dimensions / 4; i < dimensions; i++) {
            vector[i] = baseValue + (text.hashCode() % 100) / 100.0;
        }
        
        return vector;
    }

    // Helper classes

    private static class SimilarityResult {
        final String text;
        final double similarity;

        SimilarityResult(String text, double similarity) {
            this.text = text;
            this.similarity = similarity;
        }
    }

    // Factory methods for common configurations

    public static MockEmbeddingProvider forTesting() {
        return new MockEmbeddingProvider(384, EmbeddingStrategy.HASH_BASED)
            .withBaseValue(0.1)
            .withNormalization(true);
    }

    public static MockEmbeddingProvider forPerformanceTesting() {
        return new MockEmbeddingProvider(768, EmbeddingStrategy.RANDOM)
            .withBaseValue(0.05)
            .withNormalization(false);
    }

    public static MockEmbeddingProvider forSemanticTesting() {
        return new MockEmbeddingProvider(512, EmbeddingStrategy.SEMANTIC_CLUSTERS)
            .withBaseValue(0.2)
            .withNormalization(true);
    }

    public static MockEmbeddingProvider withCustomDimensions(int dimensions) {
        return new MockEmbeddingProvider(dimensions, EmbeddingStrategy.HASH_BASED);
    }
}