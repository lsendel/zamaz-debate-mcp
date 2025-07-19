package com.example.workflow.infrastructure;

import com.example.workflow.domain.*;
import com.example.workflow.domain.ports.WorkflowRepository;
import com.example.workflow.infrastructure.neo4j.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for Neo4j workflow repository
 * Tests the complete Neo4j adapter implementation with TestContainers
 */
@SpringBootTest
@Testcontainers
@Transactional
class Neo4jWorkflowRepositoryTest {
    
    @Container
    static Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:5.13")
            .withAdminPassword("testpassword")
            .withReuse(true);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.neo4j.uri", neo4jContainer::getBoltUrl);
        registry.add("spring.neo4j.authentication.username", () -> "neo4j");
        registry.add("spring.neo4j.authentication.password", () -> "testpassword");
    }
    
    @Autowired
    private WorkflowRepository workflowRepository;
    
    private Workflow testWorkflow;
    private final String testOrganizationId = "test-org-123";
    
    @BeforeEach
    void setUp() {
        // Create test workflow with nodes and edges
        WorkflowId workflowId = WorkflowId.generate();
        NodeId startNodeId = NodeId.generate();
        NodeId taskNodeId = NodeId.generate();
        NodeId endNodeId = NodeId.generate();
        EdgeId edge1Id = EdgeId.generate();
        EdgeId edge2Id = EdgeId.generate();
        
        WorkflowNode startNode = new WorkflowNode(
            startNodeId, 
            NodeType.START, 
            "Start Node", 
            Position.of(100, 100)
        );
        
        WorkflowNode taskNode = new WorkflowNode(
            taskNodeId, 
            NodeType.TASK, 
            "Task Node", 
            Position.of(200, 100)
        );
        
        WorkflowNode endNode = new WorkflowNode(
            endNodeId, 
            NodeType.END, 
            "End Node", 
            Position.of(300, 100)
        );
        
        WorkflowEdge edge1 = new WorkflowEdge(
            edge1Id, 
            startNodeId, 
            taskNodeId, 
            "Start to Task", 
            EdgeType.DEFAULT
        );
        
        WorkflowEdge edge2 = new WorkflowEdge(
            edge2Id, 
            taskNodeId, 
            endNodeId, 
            "Task to End", 
            EdgeType.DEFAULT
        );
        
        testWorkflow = new Workflow(
            workflowId,
            "Test Workflow",
            testOrganizationId,
            List.of(startNode, taskNode, endNode),
            List.of(edge1, edge2)
        );
    }
    
    @Test
    void shouldSaveAndRetrieveWorkflow() {
        // When
        workflowRepository.save(testWorkflow);
        
        // Then
        Optional<Workflow> retrieved = workflowRepository.findById(testWorkflow.getId());
        
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getName()).isEqualTo("Test Workflow");
        assertThat(retrieved.get().getOrganizationId()).isEqualTo(testOrganizationId);
        assertThat(retrieved.get().getNodes()).hasSize(3);
        assertThat(retrieved.get().getEdges()).hasSize(2);
    }
    
    @Test
    void shouldFindWorkflowsByOrganization() {
        // Given
        workflowRepository.save(testWorkflow);
        
        // When
        List<Workflow> workflows = workflowRepository.findByOrganization(testOrganizationId);
        
        // Then
        assertThat(workflows).hasSize(1);
        assertThat(workflows.get(0).getId()).isEqualTo(testWorkflow.getId());
    }
    
    @Test
    void shouldFindWorkflowsByStatus() {
        // Given
        testWorkflow.activate();
        workflowRepository.save(testWorkflow);
        
        // When
        List<Workflow> activeWorkflows = workflowRepository.findByStatus(WorkflowStatus.ACTIVE);
        
        // Then
        assertThat(activeWorkflows).hasSize(1);
        assertThat(activeWorkflows.get(0).getStatus()).isEqualTo(WorkflowStatus.ACTIVE);
    }
    
    @Test
    void shouldFindWorkflowsByNodeType() {
        // Given
        workflowRepository.save(testWorkflow);
        
        // When
        List<Workflow> workflowsWithStartNodes = workflowRepository.findByNodeType(testOrganizationId, NodeType.START);
        
        // Then
        assertThat(workflowsWithStartNodes).hasSize(1);
        assertThat(workflowsWithStartNodes.get(0).getId()).isEqualTo(testWorkflow.getId());
    }
    
    @Test
    void shouldFindWorkflowsByNodeCount() {
        // Given
        workflowRepository.save(testWorkflow);
        
        // When
        List<Workflow> workflows = workflowRepository.findByNodeCount(testOrganizationId, 2, 5);
        
        // Then
        assertThat(workflows).hasSize(1);
        assertThat(workflows.get(0).getNodes()).hasSize(3);
    }
    
    @Test
    void shouldPerformComplexSearch() {
        // Given
        workflowRepository.save(testWorkflow);
        
        // When
        WorkflowRepository.WorkflowSearchQuery query = new WorkflowRepository.WorkflowSearchQuery(
            testOrganizationId,
            "Test",
            List.of(WorkflowStatus.DRAFT),
            List.of(NodeType.START),
            null,
            null,
            2,
            5,
            0,
            10,
            WorkflowRepository.WorkflowSortBy.NAME,
            WorkflowRepository.SortDirection.ASC
        );
        
        WorkflowRepository.WorkflowSearchResult result = workflowRepository.search(query);
        
        // Then
        assertThat(result.workflows()).hasSize(1);
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.workflows().get(0).getName()).contains("Test");
    }
    
    @Test
    void shouldFilterWorkflows() {
        // Given
        workflowRepository.save(testWorkflow);
        
        // When
        WorkflowRepository.WorkflowFilter filter = new WorkflowRepository.WorkflowFilter(
            List.of(WorkflowStatus.DRAFT),
            List.of(NodeType.START),
            2,
            5,
            null,
            null
        );
        
        List<Workflow> filtered = workflowRepository.findAll(testOrganizationId, filter);
        
        // Then
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getStatus()).isEqualTo(WorkflowStatus.DRAFT);
    }
    
    @Test
    void shouldGetWorkflowStatistics() {
        // Given
        workflowRepository.save(testWorkflow);
        
        // When
        WorkflowRepository.WorkflowStatistics stats = workflowRepository.getStatistics(testOrganizationId);
        
        // Then
        assertThat(stats.totalWorkflows()).isEqualTo(1);
        assertThat(stats.draftWorkflows()).isEqualTo(1);
        assertThat(stats.activeWorkflows()).isEqualTo(0);
        assertThat(stats.averageNodes()).isEqualTo(3.0);
        assertThat(stats.averageEdges()).isEqualTo(2.0);
    }
    
    @Test
    void shouldDeleteWorkflow() {
        // Given
        workflowRepository.save(testWorkflow);
        assertThat(workflowRepository.exists(testWorkflow.getId())).isTrue();
        
        // When
        workflowRepository.delete(testWorkflow.getId());
        
        // Then
        assertThat(workflowRepository.exists(testWorkflow.getId())).isFalse();
        assertThat(workflowRepository.findById(testWorkflow.getId())).isEmpty();
    }
    
    @Test
    void shouldBatchSaveWorkflows() {
        // Given
        Workflow workflow2 = new Workflow(
            WorkflowId.generate(),
            "Test Workflow 2",
            testOrganizationId,
            List.of(new WorkflowNode(NodeId.generate(), NodeType.START, "Start", Position.origin())),
            List.of()
        );
        
        // When
        workflowRepository.saveAll(List.of(testWorkflow, workflow2));
        
        // Then
        List<Workflow> workflows = workflowRepository.findByOrganization(testOrganizationId);
        assertThat(workflows).hasSize(2);
    }
    
    @Test
    void shouldFindWorkflowByNodeId() {
        // Given
        workflowRepository.save(testWorkflow);
        NodeId nodeId = testWorkflow.getNodes().get(0).getId();
        
        // When
        Optional<Workflow> found = workflowRepository.findByNodeId(nodeId);
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(testWorkflow.getId());
    }
    
    @Test
    void shouldCountWorkflowsByOrganization() {
        // Given
        workflowRepository.save(testWorkflow);
        
        // When
        long count = workflowRepository.countByOrganization(testOrganizationId);
        
        // Then
        assertThat(count).isEqualTo(1);
    }
    
    @Test
    void shouldFindWorkflowsCreatedBetween() {
        // Given
        workflowRepository.save(testWorkflow);
        Instant before = Instant.now().minusSeconds(60);
        Instant after = Instant.now().plusSeconds(60);
        
        // When
        List<Workflow> workflows = workflowRepository.findCreatedBetween(testOrganizationId, before, after);
        
        // Then
        assertThat(workflows).hasSize(1);
        assertThat(workflows.get(0).getId()).isEqualTo(testWorkflow.getId());
    }
    
    @Test
    void shouldFindWorkflowsUpdatedAfter() {
        // Given
        workflowRepository.save(testWorkflow);
        Instant before = Instant.now().minusSeconds(60);
        
        // When
        List<Workflow> workflows = workflowRepository.findUpdatedAfter(testOrganizationId, before);
        
        // Then
        assertThat(workflows).hasSize(1);
        assertThat(workflows.get(0).getId()).isEqualTo(testWorkflow.getId());
    }
}