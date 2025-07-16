package com.zamaz.mcp.rag.service;

import com.zamaz.mcp.rag.dto.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of VectorStoreService using InMemoryVectorStore.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreServiceImpl implements VectorStoreService {
    
    private final InMemoryVectorStore vectorStore;
    
    @Override
    public String storeVector(String id, List<Float> vector, Map<String, Object> metadata) {
        String organizationId = (String) metadata.get("organizationId");
        String documentId = (String) metadata.get("documentId");
        String chunkId = (String) metadata.get("chunkId");
        String content = (String) metadata.get("content");
        
        float[] embedding = toFloatArray(vector);
        
        InMemoryVectorStore.VectorDocument doc = new InMemoryVectorStore.VectorDocument(
            id, organizationId, documentId, chunkId, content, embedding, metadata
        );
        
        vectorStore.upsert(doc);
        log.debug("Stored vector {} for organization {}", id, organizationId);
        
        return id;
    }
    
    @Override
    public void storeVectors(Map<String, List<Float>> vectors, Map<String, Map<String, Object>> metadata) {
        vectors.forEach((id, vector) -> {
            Map<String, Object> meta = metadata.get(id);
            if (meta != null) {
                storeVector(id, vector, meta);
            }
        });
    }
    
    @Override
    public List<SearchResult> searchSimilar(List<Float> queryVector, String organizationId, int limit, double threshold) {
        float[] embedding = toFloatArray(queryVector);
        
        List<InMemoryVectorStore.SearchResult> results = vectorStore.search(organizationId, embedding, limit);
        
        return results.stream()
            .filter(r -> r.score >= threshold)
            .map(r -> {
                SearchResult result = new SearchResult();
                result.setDocumentId(r.document.getDocumentId());
                result.setChunkId(r.document.getChunkId());
                result.setContent(r.document.getContent());
                result.setScore((double) r.score);
                result.setMetadata(r.document.getMetadata());
                return result;
            })
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteVector(String id) {
        // Not implemented in simple in-memory store
        log.warn("Delete vector by ID not implemented in in-memory store");
    }
    
    @Override
    public void deleteVectorsByFilter(Map<String, Object> filter) {
        String organizationId = (String) filter.get("organizationId");
        String documentId = (String) filter.get("documentId");
        
        if (organizationId != null && documentId != null) {
            vectorStore.deleteByDocumentId(organizationId, documentId);
        } else if (organizationId != null && filter.containsKey("deleteAll")) {
            vectorStore.deleteAll(organizationId);
        }
    }
    
    @Override
    public void ensureCollection(String organizationId) {
        // No-op for in-memory store
        log.debug("Collection ensured for organization: {}", organizationId);
    }
    
    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}