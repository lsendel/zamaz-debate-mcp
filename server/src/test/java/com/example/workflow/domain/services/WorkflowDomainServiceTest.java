package com.example.workflow.domain.services;

import com.example.workflow.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test class for WorkflowDomainService
 * Verifies implementation of requirements 1.1, 1.3, 3.5, 3.6
 */
@ExtendWith(MockitoExtension.class)
class WorkflowDomainServiceTest {
    
    @Mock
    private ConditionEvaluationService conditionEvaluationService;
    
    private WorkflowDomainService workflowDomainService;
    
    @BeforeEach
    void setUp() {
        workflowDomainService = new WorkflowDomainService(conditionEvaluationService);
    }
    
    @Test
    void shouldCreateWorkflowWithValidInput() {
        // Given
        String name = "Test Workflow";
        String organizationId = "org-123";
        
        WorkflowNode startNode = new WorkflowNode(
            NodeId.generate(), 
            NodeType.START, 
            "Start", 
            new Position(0, 0)
        );
        
        WorkflowNode endNode = new WorkflowNode(
            NodeId.generate(), 
            NodeType.END, 
            "End", 
            new Position(100, 0)
        );
        
        WorkflowEdge edge = new WorkflowEdge(
            EdgeId.generate(),
            startNode.getId(),
            endNode.getId(),
            EdgeType.DEFAULT
        );
        
        List<WorkflowNode> nodes = List.of(startNode, endNode);
        List<WorkflowEdge> edges = List.of(edge);
        
        // When
        Workflow workflow = workflowDomainService.createWorkflow(name, organizationId, nodes, edges);
        
        // Then
        assertNotNull(workflow);
        assertEquals(name, workflow.getName());
        assertEquals(organizationId, workflow.getOrganizationId());
        assertEquals(WorkflowStatus.DRAFT, workflow.getStatus());
        assertEquals(2, workflow.getNodes().size());
        assertEquals(1, workflow.getEdges().size());
    }
    
    @Test
    void shouldValidateConnectionBetweenNodes() {
        // Given
        WorkflowNode startNode = new WorkflowNode(
            NodeId.generate(), 
            NodeType.START, 
            "Start", 
            new Position(0, 0)
        );
        
        WorkflowNode taskNode = new WorkflowNode(
            NodeId.generate(), 
            NodeType.TASK, 
            "Task", 
            new Position(100, 0)
        );
        
        // When
        ConnectionValidationResult result = workflowDomainService.validateConnection(
            startNode, taskNode, EdgeType.DEFAULT
        );
        
        // Then
        assertTrue(result.isValid());
        assertTrue(result.errors().isEmpty());
    }
    
    @Test
    void shouldRejectInvalidConnections() {
        // Given
        WorkflowNode endNode = new WorkflowNode(
            NodeId.generate(), 
            NodeType.END, 
            "End", 
            new Position(0, 0)
        );
        
        WorkflowNode taskNode = new WorkflowNode(
            NodeId.generate(), 
            NodeType.TASK, 
            "Task", 
            new Position(100, 0)
        );
        
        // When - trying to connect FROM an end node (invalid)
        ConnectionValidationResult result = workflowDomainService.validateConnection(
            endNode, taskNode, EdgeType.DEFAULT
        );
        
        // Then
        assertFalse(result.isValid());
        assertFalse(result.errors().isEmpty());
        assertTrue(result.errors().get(0).contains("End nodes cannot have outgoing connections"));
    }
    
    @Test
    void shouldExecuteWorkflowWithTelemetryData() {
        // Given
        WorkflowNode startNode = new WorkflowNode(
            NodeId.generate(), 
            NodeType.START, 
            "Start", 
            new Position(0, 0)
        );
        
        WorkflowNode endNode = new WorkflowNode(
            NodeId.generate(), 
            NodeType.END, 
            "End", 
            new Position(100, 0)
        );
        
        WorkflowEdge edge = new WorkflowEdge(
            EdgeId.generate(),
            startNode.getId(),
            endNode.getId(),
            EdgeType.DEFAULT
        );
        
        Workflow workflow = new Workflow(
            WorkflowId.generate(),
            "Test Workflow",
            "org-123",
            List.of(startNode, endNode),
            List.of(edge)
        );
        workflow.activate();
        
        TelemetryData telemetryData = new TelemetryData(
            TelemetryId.generate(),
            DeviceId.of("device-123"),
            "org-123"
        );
        
        // When
        WorkflowExecution execution = workflowDomainService.executeWorkflow(workflow, telemetryData);
        
        // Then
        assertNotNull(execution);
        assertEquals(ExecutionStatus.COMPLETED, execution.getStatus());
        assertEquals(workflow.getId(), execution.getWorkflowId());
    }
    
    @Test
    void shouldProcessDecisionNodeWithConditions() {
        // Given
        WorkflowNode startNode = new WorkflowNode(
            NodeId.generate(), 
            NodeType.START, 
            "Start", 
            new Position(0, 0)
        );
        
        WorkflowNode decisionNode = new WorkflowNode(
            NodeId.generate(), 
            NodeType.DECISION, 
            "Decision", 
            new Position(50, 0),
            Map.of("conditions", "temperature > 25")
        );
        
        WorkflowNode trueNode = new WorkflowNode(
            NodeId.generate(), 
            NodeType.TASK, 
            "True Path", 
            new Position(100, -50)
        );
        
        WorkflowNode falseNode = new WorkflowNode(
            NodeId.generate(), 
            NodeType.TASK, 
            "False Path", 
            new Position(100, 50)
        );
        
        List<WorkflowEdge> edges = List.of(
            new WorkflowEdge(EdgeId.generate(), startNode.getId(), decisionNode.getId(), EdgeType.DEFAULT),
            new WorkflowEdge(EdgeId.generate(), decisionNode.getId(), trueNode.getId(), EdgeType.CONDITIONAL_TRUE),
            new WorkflowEdge(EdgeId.generate(), decisionNode.getId(), falseNode.getId(), EdgeType.CONDITIONAL_FALSE)
        );
        
        Workflow workflow = new Workflow(
            WorkflowId.generate(),
            "Decision Workflow",
            "org-123",
            List.of(startNode, decisionNode, trueNode, falseNode),
            edges
        );
        workflow.activate();
        
        TelemetryData telemetryData = new TelemetryData(
            TelemetryId.generate(),
            DeviceId.of("device-123"),
            "org-123"
        );
        telemetryData.addMetric("temperature", MetricValue.of(30.0));
        
        // Mock condition evaluation to return true
        when(conditionEvaluationService.evaluateConditions(any(), any())).thenReturn(true);
        
        // When
        WorkflowExecution execution = workflowDomainService.executeWorkflow(workflow, telemetryData);
        
        // Then
        assertNotNull(execution);
        assertEquals(trueNode.getId(), execution.getCurrentNodeId());
        assertTrue(execution.getContextData("condition_result_" + decisionNode.getId()).equals(true));
    }
    
    @Test
    void shouldValidateExecutionReadiness() {
        // Given
        WorkflowNode startNode = new WorkflowNode(
            NodeId.generate(), 
            NodeType.START, 
            "Start", 
            new Position(0, 0)
        );
        
        WorkflowNode decisionNode = new WorkflowNode(
            NodeId.generate(), 
            NodeType.DECISION, 
            "Decision", 
            new Position(50, 0)
            // No conditions configured - should cause readiness issue
        );
        
        Workflow workflow = new Workflow(
            WorkflowId.generate(),
            "Test Workflow",
            "org-123",
            List.of(startNode, decisionNode),
            List.of()
        );
        
        // When
        ExecutionReadinessResult result = workflowDomainService.validateExecutionReadiness(workflow);
        
        // Then
        assertFalse(result.isReady());
        assertFalse(result.issues().isEmpty());
        assertTrue(result.issues().stream().anyMatch(issue -> 
            issue.contains("Decision/Condition node") && issue.contains("has no conditions configured")));
    }
}