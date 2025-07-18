package com.zamaz.mcp.rag.testing;

import com.zamaz.mcp.rag.domain.model.Document;
import com.zamaz.mcp.rag.domain.model.Embedding;
import com.zamaz.mcp.rag.domain.model.SearchResult;
import com.zamaz.mcp.rag.service.VectorStore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory vector store implementation for testing.
 * Provides similarity search without external dependencies.
 */
public class InMemoryVectorStore implements VectorStore {

    private final Map<String, VectorDocument> documents = new ConcurrentHashMap<>();
    private final Map<String, Collection> collections = new ConcurrentHashMap<>();
    private final SimilarityCalculator similarityCalculator;
    private int dimensions = 384; // Default embedding dimension
    
    public InMemoryVectorStore() {
        this.similarityCalculator = new CosineSimilarityCalculator();
    }

    public InMemoryVectorStore(SimilarityCalculator calculator) {
        this.similarityCalculator = calculator;
    }

    @Override
    public String addDocument(String collectionName, Document document, Embedding embedding) {
        ensureCollection(collectionName);
        
        String id = document.getId() != null ? document.getId() : UUID.randomUUID().toString();
        VectorDocument vectorDoc = new VectorDocument(id, document, embedding);
        
        documents.put(id, vectorDoc);
        collections.get(collectionName).addDocument(id);
        
        return id;
    }

    @Override
    public List<String> addDocuments(String collectionName, List<Document> documents, List<Embedding> embeddings) {
        if (documents.size() != embeddings.size()) {
            throw new IllegalArgumentException("Documents and embeddings lists must have the same size");
        }
        
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            String id = addDocument(collectionName, documents.get(i), embeddings.get(i));
            ids.add(id);
        }
        
        return ids;
    }

    @Override
    public List<SearchResult> search(String collectionName, Embedding queryEmbedding, int topK) {
        return search(collectionName, queryEmbedding, topK, 0.0);
    }

    @Override
    public List<SearchResult> search(String collectionName, Embedding queryEmbedding, int topK, double threshold) {
        Collection collection = collections.get(collectionName);
        if (collection == null) {
            return Collections.emptyList();
        }
        
        return collection.getDocumentIds().stream()
            .map(documents::get)
            .filter(Objects::nonNull)
            .map(doc -> new SearchResultImpl(
                doc, 
                similarityCalculator.calculate(queryEmbedding.getVector(), doc.embedding.getVector())
            ))
            .filter(result -> result.getScore() >= threshold)
            .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
            .limit(topK)
            .map(result -> (SearchResult) result)
            .collect(Collectors.toList());
    }

    @Override
    public List<SearchResult> searchWithFilter(String collectionName, Embedding queryEmbedding, 
                                             int topK, Map<String, Object> filters) {
        Collection collection = collections.get(collectionName);
        if (collection == null) {
            return Collections.emptyList();
        }
        
        return collection.getDocumentIds().stream()
            .map(documents::get)
            .filter(Objects::nonNull)
            .filter(doc -> matchesFilters(doc.document, filters))
            .map(doc -> new SearchResultImpl(
                doc, 
                similarityCalculator.calculate(queryEmbedding.getVector(), doc.embedding.getVector())
            ))
            .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
            .limit(topK)
            .map(result -> (SearchResult) result)
            .collect(Collectors.toList());
    }

    @Override
    public boolean deleteDocument(String collectionName, String documentId) {
        Collection collection = collections.get(collectionName);
        if (collection != null) {
            collection.removeDocument(documentId);
        }
        
        return documents.remove(documentId) != null;
    }

    @Override
    public boolean deleteCollection(String collectionName) {
        Collection collection = collections.remove(collectionName);
        if (collection != null) {
            // Remove all documents in the collection
            collection.getDocumentIds().forEach(documents::remove);
            return true;
        }
        return false;
    }

    @Override
    public Document getDocument(String collectionName, String documentId) {
        VectorDocument vectorDoc = documents.get(documentId);
        if (vectorDoc != null) {
            Collection collection = collections.get(collectionName);
            if (collection != null && collection.containsDocument(documentId)) {
                return vectorDoc.document;
            }
        }
        return null;
    }

    @Override
    public List<Document> getDocuments(String collectionName, List<String> documentIds) {
        return documentIds.stream()
            .map(id -> getDocument(collectionName, id))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    public boolean updateDocument(String collectionName, String documentId, Document document, Embedding embedding) {
        if (!documents.containsKey(documentId)) {
            return false;
        }
        
        Collection collection = collections.get(collectionName);
        if (collection == null || !collection.containsDocument(documentId)) {
            return false;
        }
        
        VectorDocument vectorDoc = new VectorDocument(documentId, document, embedding);
        documents.put(documentId, vectorDoc);
        return true;
    }

    @Override
    public void createCollection(String collectionName) {
        collections.putIfAbsent(collectionName, new Collection(collectionName));
    }

    @Override
    public boolean collectionExists(String collectionName) {
        return collections.containsKey(collectionName);
    }

    @Override
    public List<String> listCollections() {
        return new ArrayList<>(collections.keySet());
    }

    @Override
    public long getDocumentCount(String collectionName) {
        Collection collection = collections.get(collectionName);
        return collection != null ? collection.getDocumentCount() : 0;
    }

    @Override
    public void clearCollection(String collectionName) {
        Collection collection = collections.get(collectionName);
        if (collection != null) {
            collection.getDocumentIds().forEach(documents::remove);
            collection.clear();
        }
    }

    // Testing-specific methods

    public void setDimensions(int dimensions) {
        this.dimensions = dimensions;
    }

    public int getDimensions() {
        return dimensions;
    }

    public void clear() {
        documents.clear();
        collections.clear();
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDocuments", documents.size());
        stats.put("totalCollections", collections.size());
        stats.put("dimensions", dimensions);
        
        Map<String, Integer> collectionSizes = new HashMap<>();
        collections.forEach((name, collection) -> 
            collectionSizes.put(name, collection.getDocumentCount()));
        stats.put("collectionSizes", collectionSizes);
        
        return stats;
    }

    public List<VectorDocument> getAllDocuments() {
        return new ArrayList<>(documents.values());
    }

    public List<VectorDocument> getDocumentsInCollection(String collectionName) {
        Collection collection = collections.get(collectionName);
        if (collection == null) {
            return Collections.emptyList();
        }
        
        return collection.getDocumentIds().stream()
            .map(documents::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    // Helper methods

    private void ensureCollection(String collectionName) {
        collections.putIfAbsent(collectionName, new Collection(collectionName));
    }

    private boolean matchesFilters(Document document, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        
        Map<String, Object> metadata = document.getMetadata();
        if (metadata == null) {
            return filters.isEmpty();
        }
        
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
            String key = filter.getKey();
            Object expectedValue = filter.getValue();
            Object actualValue = metadata.get(key);
            
            if (!Objects.equals(expectedValue, actualValue)) {
                return false;
            }
        }
        
        return true;
    }

    // Inner classes

    public static class VectorDocument {
        public final String id;
        public final Document document;
        public final Embedding embedding;

        public VectorDocument(String id, Document document, Embedding embedding) {
            this.id = id;
            this.document = document;
            this.embedding = embedding;
        }
    }

    private static class Collection {
        private final String name;
        private final Set<String> documentIds = ConcurrentHashMap.newKeySet();

        public Collection(String name) {
            this.name = name;
        }

        public void addDocument(String documentId) {
            documentIds.add(documentId);
        }

        public void removeDocument(String documentId) {
            documentIds.remove(documentId);
        }

        public boolean containsDocument(String documentId) {
            return documentIds.contains(documentId);
        }

        public Set<String> getDocumentIds() {
            return new HashSet<>(documentIds);
        }

        public int getDocumentCount() {
            return documentIds.size();
        }

        public void clear() {
            documentIds.clear();
        }
    }

    private static class SearchResultImpl implements SearchResult {
        private final VectorDocument vectorDocument;
        private final double score;

        public SearchResultImpl(VectorDocument vectorDocument, double score) {
            this.vectorDocument = vectorDocument;
            this.score = score;
        }

        @Override
        public String getId() {
            return vectorDocument.id;
        }

        @Override
        public Document getDocument() {
            return vectorDocument.document;
        }

        @Override
        public double getScore() {
            return score;
        }

        @Override
        public Map<String, Object> getMetadata() {
            Map<String, Object> metadata = new HashMap<>();
            if (vectorDocument.document.getMetadata() != null) {
                metadata.putAll(vectorDocument.document.getMetadata());
            }
            metadata.put("similarity_score", score);
            return metadata;
        }
    }

    // Similarity calculation interfaces and implementations

    public interface SimilarityCalculator {
        double calculate(double[] vector1, double[] vector2);
    }

    public static class CosineSimilarityCalculator implements SimilarityCalculator {
        @Override
        public double calculate(double[] vector1, double[] vector2) {
            if (vector1.length != vector2.length) {
                throw new IllegalArgumentException("Vectors must have the same dimensions");
            }
            
            double dotProduct = 0.0;
            double norm1 = 0.0;
            double norm2 = 0.0;
            
            for (int i = 0; i < vector1.length; i++) {
                dotProduct += vector1[i] * vector2[i];
                norm1 += Math.pow(vector1[i], 2);
                norm2 += Math.pow(vector2[i], 2);
            }
            
            if (norm1 == 0.0 || norm2 == 0.0) {
                return 0.0;
            }
            
            return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
        }
    }

    public static class EuclideanDistanceCalculator implements SimilarityCalculator {
        @Override
        public double calculate(double[] vector1, double[] vector2) {
            if (vector1.length != vector2.length) {
                throw new IllegalArgumentException("Vectors must have the same dimensions");
            }
            
            double sum = 0.0;
            for (int i = 0; i < vector1.length; i++) {
                sum += Math.pow(vector1[i] - vector2[i], 2);
            }
            
            // Convert distance to similarity (higher is better)
            double distance = Math.sqrt(sum);
            return 1.0 / (1.0 + distance);
        }
    }

    public static class DotProductCalculator implements SimilarityCalculator {
        @Override
        public double calculate(double[] vector1, double[] vector2) {
            if (vector1.length != vector2.length) {
                throw new IllegalArgumentException("Vectors must have the same dimensions");
            }
            
            double dotProduct = 0.0;
            for (int i = 0; i < vector1.length; i++) {
                dotProduct += vector1[i] * vector2[i];
            }
            
            return dotProduct;
        }
    }
}