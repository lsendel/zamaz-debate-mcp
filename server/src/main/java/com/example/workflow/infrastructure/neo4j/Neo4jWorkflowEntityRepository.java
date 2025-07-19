package com.example.workflow.infrastructure.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Neo4j repository interface for WorkflowEntity
 * Provides Cypher query methods for workflow operations
 */
@Repository
public interface Neo4jWorkflowEntityRepository extends Neo4jRepository<WorkflowEntity, String> {
    
    // Basic queries
    List<WorkflowEntity> findByOrganizationId(String organizationId);
    List<WorkflowEntity> findByStatus(String status);
    List<WorkflowEntity> findByOrganizationIdAndStatus(String organizationId, String status);
    List<WorkflowEntity> findByNameContainingIgnoreCase(String namePattern);
    
    // Time-based queries
    @Query("MATCH (w:Workflow) WHERE w.organizationId = $organizationId AND w.createdAt >= $fromTime AND w.createdAt <= $toTime RETURN w")
    List<WorkflowEntity> findCreatedBetween(@Param("organizationId") String organizationId, 
                                           @Param("fromTime") Instant fromTime, 
                                           @Param("toTime") Instant toTime);
    
    @Query("MATCH (w:Workflow) WHERE w.organizationId = $organizationId AND w.updatedAt >= $afterTime RETURN w")
    List<WorkflowEntity> findUpdatedAfter(@Param("organizationId") String organizationId, 
                                         @Param("afterTime") Instant afterTime);
    
    // Node-based queries
    @Query("MATCH (w:Workflow)-[:CONTAINS]->(n:WorkflowNode) WHERE w.organizationId = $organizationId AND n.type = $nodeType RETURN DISTINCT w")
    List<WorkflowEntity> findByNodeType(@Param("organizationId") String organizationId, 
                                       @Param("nodeType") String nodeType);
    
    @Query("MATCH (w:Workflow)-[:CONTAINS]->(n:WorkflowNode) WHERE w.organizationId = $organizationId WITH w, count(n) as nodeCount WHERE nodeCount >= $minNodes AND nodeCount <= $maxNodes RETURN w")
    List<WorkflowEntity> findByNodeCount(@Param("organizationId") String organizationId, 
                                        @Param("minNodes") int minNodes, 
                                        @Param("maxNodes") int maxNodes);
    
    @Query("MATCH (w:Workflow)-[:CONTAINS]->(n:WorkflowNode) WHERE n.id = $nodeId RETURN w")
    Optional<WorkflowEntity> findByNodeId(@Param("nodeId") String nodeId);
    
    // Search queries
    @Query("MATCH (w:Workflow) WHERE w.organizationId = $organizationId AND ($namePattern IS NULL OR w.name CONTAINS $namePattern) AND ($status IS NULL OR w.status = $status) RETURN w ORDER BY w.updatedAt DESC SKIP $offset LIMIT $limit")
    List<WorkflowEntity> searchWorkflows(@Param("organizationId") String organizationId,
                                        @Param("namePattern") String namePattern,
                                        @Param("status") String status,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);
    
    @Query("MATCH (w:Workflow) WHERE w.organizationId = $organizationId AND ($namePattern IS NULL OR w.name CONTAINS $namePattern) AND ($status IS NULL OR w.status = $status) RETURN count(w)")
    long countSearchResults(@Param("organizationId") String organizationId,
                           @Param("namePattern") String namePattern,
                           @Param("status") String status);
    
    // Statistics queries
    long countByOrganizationId(String organizationId);
    long countByOrganizationIdAndStatus(String organizationId, String status);
    
    @Query("MATCH (w:Workflow)-[:CONTAINS]->(n:WorkflowNode) WHERE w.organizationId = $organizationId RETURN avg(count(n))")
    Double getAverageNodeCount(@Param("organizationId") String organizationId);
    
    @Query("MATCH (w:Workflow)-[:HAS_EDGE]->(e) WHERE w.organizationId = $organizationId RETURN avg(count(e))")
    Double getAverageEdgeCount(@Param("organizationId") String organizationId);
    
    @Query("MATCH (w:Workflow) WHERE w.organizationId = $organizationId RETURN max(w.createdAt)")
    Optional<Instant> getLastCreatedTime(@Param("organizationId") String organizationId);
    
    @Query("MATCH (w:Workflow) WHERE w.organizationId = $organizationId RETURN max(w.updatedAt)")
    Optional<Instant> getLastUpdatedTime(@Param("organizationId") String organizationId);
    
    // Complex workflow structure queries
    @Query("MATCH (w:Workflow)-[:CONTAINS]->(start:WorkflowNode) WHERE w.id = $workflowId AND NOT EXISTS((start)<-[:CONNECTS_TO]-()) RETURN start")
    List<WorkflowNodeEntity> findStartNodes(@Param("workflowId") String workflowId);
    
    @Query("MATCH (w:Workflow)-[:CONTAINS]->(end:WorkflowNode) WHERE w.id = $workflowId AND NOT EXISTS((end)-[:CONNECTS_TO]->()) RETURN end")
    List<WorkflowNodeEntity> findEndNodes(@Param("workflowId") String workflowId);
    
    @Query("MATCH (w:Workflow)-[:CONTAINS]->(n:WorkflowNode)-[:CONNECTS_TO]->(next:WorkflowNode) WHERE w.id = $workflowId AND n.id = $nodeId RETURN next")
    List<WorkflowNodeEntity> findNextNodes(@Param("workflowId") String workflowId, @Param("nodeId") String nodeId);
    
    @Query("MATCH (w:Workflow)-[:CONTAINS]->(prev:WorkflowNode)-[:CONNECTS_TO]->(n:WorkflowNode) WHERE w.id = $workflowId AND n.id = $nodeId RETURN prev")
    List<WorkflowNodeEntity> findPreviousNodes(@Param("workflowId") String workflowId, @Param("nodeId") String nodeId);
    
    // Advanced workflow path queries
    @Query("MATCH (w:Workflow)-[:CONTAINS]->(start:WorkflowNode)-[:CONNECTS_TO*]->(end:WorkflowNode) WHERE w.id = $workflowId AND start.id = $startNodeId AND end.id = $endNodeId RETURN EXISTS((start)-[:CONNECTS_TO*]->(end))")
    boolean hasPathBetweenNodes(@Param("workflowId") String workflowId, @Param("startNodeId") String startNodeId, @Param("endNodeId") String endNodeId);
    
    @Query("MATCH (w:Workflow)-[:CONTAINS]->(n:WorkflowNode) WHERE w.id = $workflowId MATCH path = (n)-[:CONNECTS_TO*]->(end:WorkflowNode) WHERE NOT EXISTS((end)-[:CONNECTS_TO]->()) RETURN length(path) ORDER BY length(path) DESC LIMIT 1")
    Integer findMaxPathLength(@Param("workflowId") String workflowId);
    
    @Query("MATCH (w:Workflow)-[:CONTAINS]->(n:WorkflowNode) WHERE w.id = $workflowId AND n.type = $nodeType RETURN count(n)")
    long countNodesByType(@Param("workflowId") String workflowId, @Param("nodeType") String nodeType);
    
    // Workflow validation queries
    @Query("MATCH (w:Workflow) WHERE w.id = $workflowId MATCH (w)-[:CONTAINS]->(n:WorkflowNode) OPTIONAL MATCH (n)-[r:CONNECTS_TO]->(target) WHERE NOT EXISTS((w)-[:CONTAINS]->(target)) RETURN count(r) > 0")
    boolean hasOrphanedEdges(@Param("workflowId") String workflowId);
    
    @Query("MATCH (w:Workflow) WHERE w.id = $workflowId MATCH (w)-[:CONTAINS]->(n:WorkflowNode) WHERE NOT EXISTS((n)<-[:CONNECTS_TO]-()) AND NOT EXISTS((n)-[:CONNECTS_TO]->()) RETURN count(n) > 0")
    boolean hasIsolatedNodes(@Param("workflowId") String workflowId);
    
    @Query("MATCH (w:Workflow)-[:CONTAINS]->(n:WorkflowNode) WHERE w.id = $workflowId MATCH (n)-[:CONNECTS_TO]->(n) RETURN count(n) > 0")
    boolean hasSelfLoops(@Param("workflowId") String workflowId);
    
    @Query("MATCH (w:Workflow)-[:CONTAINS]->(n:WorkflowNode) WHERE w.id = $workflowId MATCH path = (n)-[:CONNECTS_TO*]->(n) RETURN count(path) > 0")
    boolean hasCycles(@Param("workflowId") String workflowId);
    
    // Advanced search queries with full-text search
    @Query("CALL db.index.fulltext.queryNodes('workflow_search_idx', $searchTerm) YIELD node, score WHERE node.organizationId = $organizationId RETURN node ORDER BY score DESC SKIP $offset LIMIT $limit")
    List<WorkflowEntity> fullTextSearch(@Param("organizationId") String organizationId, @Param("searchTerm") String searchTerm, @Param("offset") int offset, @Param("limit") int limit);
    
    @Query("CALL db.index.fulltext.queryNodes('workflow_search_idx', $searchTerm) YIELD node WHERE node.organizationId = $organizationId RETURN count(node)")
    long countFullTextSearch(@Param("organizationId") String organizationId, @Param("searchTerm") String searchTerm);
    
    // Complex filtering queries
    @Query("MATCH (w:Workflow) WHERE w.organizationId = $organizationId " +
           "AND ($statuses IS NULL OR w.status IN $statuses) " +
           "AND ($nodeTypes IS NULL OR EXISTS((w)-[:CONTAINS]->(n:WorkflowNode) WHERE n.type IN $nodeTypes)) " +
           "AND ($minNodes IS NULL OR size((w)-[:CONTAINS]->(:WorkflowNode)) >= $minNodes) " +
           "AND ($maxNodes IS NULL OR size((w)-[:CONTAINS]->(:WorkflowNode)) <= $maxNodes) " +
           "AND ($createdAfter IS NULL OR w.createdAt >= $createdAfter) " +
           "AND ($updatedAfter IS NULL OR w.updatedAt >= $updatedAfter) " +
           "RETURN w ORDER BY w.updatedAt DESC SKIP $offset LIMIT $limit")
    List<WorkflowEntity> findWithComplexFilter(@Param("organizationId") String organizationId,
                                              @Param("statuses") List<String> statuses,
                                              @Param("nodeTypes") List<String> nodeTypes,
                                              @Param("minNodes") Integer minNodes,
                                              @Param("maxNodes") Integer maxNodes,
                                              @Param("createdAfter") Instant createdAfter,
                                              @Param("updatedAfter") Instant updatedAfter,
                                              @Param("offset") int offset,
                                              @Param("limit") int limit);
    
    @Query("MATCH (w:Workflow) WHERE w.organizationId = $organizationId " +
           "AND ($statuses IS NULL OR w.status IN $statuses) " +
           "AND ($nodeTypes IS NULL OR EXISTS((w)-[:CONTAINS]->(n:WorkflowNode) WHERE n.type IN $nodeTypes)) " +
           "AND ($minNodes IS NULL OR size((w)-[:CONTAINS]->(:WorkflowNode)) >= $minNodes) " +
           "AND ($maxNodes IS NULL OR size((w)-[:CONTAINS]->(:WorkflowNode)) <= $maxNodes) " +
           "AND ($createdAfter IS NULL OR w.createdAt >= $createdAfter) " +
           "AND ($updatedAfter IS NULL OR w.updatedAt >= $updatedAfter) " +
           "RETURN count(w)")
    long countWithComplexFilter(@Param("organizationId") String organizationId,
                               @Param("statuses") List<String> statuses,
                               @Param("nodeTypes") List<String> nodeTypes,
                               @Param("minNodes") Integer minNodes,
                               @Param("maxNodes") Integer maxNodes,
                               @Param("createdAfter") Instant createdAfter,
                               @Param("updatedAfter") Instant updatedAfter);
    
    // Performance optimization queries
    @Query("MATCH (w:Workflow) WHERE w.organizationId = $organizationId WITH w MATCH (w)-[:CONTAINS]->(n:WorkflowNode) RETURN w, collect(n) as nodes")
    List<WorkflowEntity> findByOrganizationWithNodes(@Param("organizationId") String organizationId);
    
    @Query("MATCH (w:Workflow) WHERE w.id = $workflowId WITH w MATCH (w)-[:CONTAINS]->(n:WorkflowNode) OPTIONAL MATCH (n)-[e:CONNECTS_TO]->(target:WorkflowNode) RETURN w, collect(DISTINCT n) as nodes, collect(DISTINCT e) as edges")
    Optional<WorkflowEntity> findByIdWithNodesAndEdges(@Param("workflowId") String workflowId);
    
    // Batch operations
    @Query("MATCH (w:Workflow) WHERE w.id IN $workflowIds DETACH DELETE w")
    void deleteAllByIds(@Param("workflowIds") List<String> workflowIds);
    
    @Query("MATCH (w:Workflow) WHERE w.organizationId = $organizationId AND w.status = $status SET w.status = $newStatus, w.updatedAt = $updatedAt")
    void updateStatusForOrganization(@Param("organizationId") String organizationId,
                                    @Param("status") String status,
                                    @Param("newStatus") String newStatus,
                                    @Param("updatedAt") Instant updatedAt);
    
    @Query("UNWIND $workflows AS workflow " +
           "MERGE (w:Workflow {id: workflow.id}) " +
           "SET w.name = workflow.name, w.organizationId = workflow.organizationId, w.status = workflow.status, w.updatedAt = workflow.updatedAt " +
           "WITH w, workflow " +
           "UNWIND workflow.nodes AS node " +
           "MERGE (n:WorkflowNode {id: node.id}) " +
           "SET n.type = node.type, n.name = node.name, n.workflowId = workflow.id " +
           "MERGE (w)-[:CONTAINS]->(n)")
    void batchUpsertWorkflows(@Param("workflows") List<Map<String, Object>> workflows);
}