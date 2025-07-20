package com.zamaz.mcp.rag.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Value object representing a vector embedding.
 */
public record Embedding(List<Double> vector) implements ValueObject {
    
    private static final int MIN_DIMENSIONS = 128;
    private static final int MAX_DIMENSIONS = 4096;
    
    public Embedding {
        Objects.requireNonNull(vector, "Embedding vector cannot be null");
        
        if (vector.isEmpty()) {
            throw new IllegalArgumentException("Embedding vector cannot be empty");
        }
        
        if (vector.size() < MIN_DIMENSIONS || vector.size() > MAX_DIMENSIONS) {
            throw new IllegalArgumentException(
                "Embedding vector must have between " + MIN_DIMENSIONS + 
                " and " + MAX_DIMENSIONS + " dimensions"
            );
        }
        
        // Check for null values
        for (int i = 0; i < vector.size(); i++) {
            if (vector.get(i) == null) {
                throw new IllegalArgumentException("Embedding vector cannot contain null values at index " + i);
            }
            if (!Double.isFinite(vector.get(i))) {
                throw new IllegalArgumentException("Embedding vector cannot contain infinite or NaN values at index " + i);
            }
        }
        
        // Create immutable copy
        this.vector = List.copyOf(vector);
    }
    
    public static Embedding of(List<Double> vector) {
        return new Embedding(vector);
    }
    
    public static Embedding of(double[] vector) {
        Objects.requireNonNull(vector, "Vector array cannot be null");
        return new Embedding(Arrays.stream(vector).boxed().toList());
    }
    
    public static Embedding of(float[] vector) {
        Objects.requireNonNull(vector, "Vector array cannot be null");
        List<Double> doubleVector = new java.util.ArrayList<>();
        for (float f : vector) {
            doubleVector.add((double) f);
        }
        return new Embedding(doubleVector);
    }
    
    public int dimensions() {
        return vector.size();
    }
    
    public Double get(int index) {
        return vector.get(index);
    }
    
    public double[] toDoubleArray() {
        return vector.stream().mapToDouble(Double::doubleValue).toArray();
    }
    
    public float[] toFloatArray() {
        float[] result = new float[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            result[i] = vector.get(i).floatValue();
        }
        return result;
    }
    
    /**
     * Calculate cosine similarity with another embedding.
     */
    public double cosineSimilarity(Embedding other) {
        Objects.requireNonNull(other, "Other embedding cannot be null");
        
        if (this.dimensions() != other.dimensions()) {
            throw new IllegalArgumentException(
                "Cannot calculate similarity between embeddings of different dimensions: " +
                this.dimensions() + " vs " + other.dimensions()
            );
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < vector.size(); i++) {
            double a = vector.get(i);
            double b = other.vector.get(i);
            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }
        
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    /**
     * Calculate Euclidean distance to another embedding.
     */
    public double euclideanDistance(Embedding other) {
        Objects.requireNonNull(other, "Other embedding cannot be null");
        
        if (this.dimensions() != other.dimensions()) {
            throw new IllegalArgumentException(
                "Cannot calculate distance between embeddings of different dimensions: " +
                this.dimensions() + " vs " + other.dimensions()
            );
        }
        
        double sum = 0.0;
        for (int i = 0; i < vector.size(); i++) {
            double diff = vector.get(i) - other.vector.get(i);
            sum += diff * diff;
        }
        
        return Math.sqrt(sum);
    }
    
    /**
     * Calculate the magnitude (L2 norm) of this embedding.
     */
    public double magnitude() {
        double sum = 0.0;
        for (Double value : vector) {
            sum += value * value;
        }
        return Math.sqrt(sum);
    }
    
    /**
     * Normalize this embedding to unit length.
     */
    public Embedding normalize() {
        double mag = magnitude();
        if (mag == 0.0) {
            return this;
        }
        
        List<Double> normalized = vector.stream()
            .map(v -> v / mag)
            .toList();
        
        return new Embedding(normalized);
    }
    
    /**
     * Check if this embedding is normalized (unit length).
     */
    public boolean isNormalized() {
        double mag = magnitude();
        return Math.abs(mag - 1.0) < 1e-6;
    }
    
    @Override
    public String toString() {
        if (vector.size() <= 5) {
            return "Embedding{dimensions=" + dimensions() + ", vector=" + vector + "}";
        } else {
            return String.format("Embedding{dimensions=%d, vector=[%.4f, %.4f, %.4f, ...]}",
                dimensions(), vector.get(0), vector.get(1), vector.get(2));
        }
    }
}