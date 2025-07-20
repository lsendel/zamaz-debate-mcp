package com.zamaz.mcp.rag.domain.model.embedding;

import java.util.Arrays;
import java.util.Objects;

/**
 * Value Object representing an embedding vector.
 * Immutable representation of a high-dimensional vector used for similarity search.
 */
public class EmbeddingVector {
    
    private final float[] values;
    private final int dimensions;
    
    private EmbeddingVector(float[] values) {
        Objects.requireNonNull(values, "Embedding values cannot be null");
        
        if (values.length == 0) {
            throw new IllegalArgumentException("Embedding vector cannot be empty");
        }
        
        // Common embedding dimensions
        if (values.length != 384 && values.length != 768 && values.length != 1536) {
            throw new IllegalArgumentException(
                "Unsupported embedding dimension: " + values.length + 
                ". Supported dimensions are 384, 768, or 1536"
            );
        }
        
        this.values = Arrays.copyOf(values, values.length); // Defensive copy
        this.dimensions = values.length;
        
        // Validate values are normalized (magnitude should be close to 1)
        double magnitude = calculateMagnitude();
        if (Math.abs(magnitude - 1.0) > 0.01) {
            // Auto-normalize if not normalized
            normalize();
        }
    }
    
    /**
     * Factory method to create an embedding vector
     */
    public static EmbeddingVector of(float[] values) {
        return new EmbeddingVector(values);
    }
    
    /**
     * Factory method to create from double array
     */
    public static EmbeddingVector of(double[] values) {
        float[] floatValues = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            floatValues[i] = (float) values[i];
        }
        return new EmbeddingVector(floatValues);
    }
    
    /**
     * Get the values as a defensive copy
     */
    public float[] getValues() {
        return Arrays.copyOf(values, values.length);
    }
    
    /**
     * Get the number of dimensions
     */
    public int getDimensions() {
        return dimensions;
    }
    
    /**
     * Calculate cosine similarity with another embedding
     */
    public double cosineSimilarity(EmbeddingVector other) {
        Objects.requireNonNull(other, "Other embedding cannot be null");
        
        if (this.dimensions != other.dimensions) {
            throw new IllegalArgumentException(
                "Cannot calculate similarity between embeddings of different dimensions: " +
                this.dimensions + " vs " + other.dimensions
            );
        }
        
        double dotProduct = 0.0;
        for (int i = 0; i < dimensions; i++) {
            dotProduct += this.values[i] * other.values[i];
        }
        
        // Since vectors are normalized, magnitudes are 1, so cosine similarity = dot product
        return dotProduct;
    }
    
    /**
     * Calculate Euclidean distance to another embedding
     */
    public double euclideanDistance(EmbeddingVector other) {
        Objects.requireNonNull(other, "Other embedding cannot be null");
        
        if (this.dimensions != other.dimensions) {
            throw new IllegalArgumentException(
                "Cannot calculate distance between embeddings of different dimensions: " +
                this.dimensions + " vs " + other.dimensions
            );
        }
        
        double sum = 0.0;
        for (int i = 0; i < dimensions; i++) {
            double diff = this.values[i] - other.values[i];
            sum += diff * diff;
        }
        
        return Math.sqrt(sum);
    }
    
    /**
     * Calculate magnitude of the vector
     */
    private double calculateMagnitude() {
        double sum = 0.0;
        for (float value : values) {
            sum += value * value;
        }
        return Math.sqrt(sum);
    }
    
    /**
     * Normalize the vector to unit length
     */
    private void normalize() {
        double magnitude = calculateMagnitude();
        if (magnitude > 0) {
            for (int i = 0; i < values.length; i++) {
                values[i] = (float) (values[i] / magnitude);
            }
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmbeddingVector that = (EmbeddingVector) o;
        return dimensions == that.dimensions && Arrays.equals(values, that.values);
    }
    
    @Override
    public int hashCode() {
        int result = Objects.hash(dimensions);
        result = 31 * result + Arrays.hashCode(values);
        return result;
    }
    
    @Override
    public String toString() {
        return "EmbeddingVector{dimensions=" + dimensions + ", values=[" + 
               values[0] + ", " + values[1] + ", ..., " + values[dimensions-1] + "]}";
    }
}