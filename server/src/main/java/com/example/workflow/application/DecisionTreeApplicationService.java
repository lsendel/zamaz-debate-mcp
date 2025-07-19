package com.example.workflow.application;

import com.example.workflow.domain.*;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class DecisionTreeApplicationService {
    
    public Workflow createSampleDecisionTree() {
        WorkflowNode startNode = new WorkflowNode(
            NodeId.generate(), NodeType.START, "Start", new Position(0, 0)
        );
        
        WorkflowNode decisionNode = new WorkflowNode(
            NodeId.generate(), NodeType.DECISION, "Temperature Check", 
            new Position(100, 0), Map.of("conditions", "temperature > 25")
        );
        
        WorkflowNode hotPath = new WorkflowNode(
            NodeId.generate(), NodeType.TASK, "Hot Weather Action", new Position(200, -50)
        );
        
        WorkflowNode coldPath = new WorkflowNode(
            NodeId.generate(), NodeType.TASK, "Cold Weather Action", new Position(200, 50)
        );
        
        List<WorkflowEdge> edges = List.of(
            new WorkflowEdge(EdgeId.generate(), startNode.getId(), decisionNode.getId(), EdgeType.DEFAULT),
            new WorkflowEdge(EdgeId.generate(), decisionNode.getId(), hotPath.getId(), EdgeType.CONDITIONAL_TRUE),
            new WorkflowEdge(EdgeId.generate(), decisionNode.getId(), coldPath.getId(), EdgeType.CONDITIONAL_FALSE)
        );
        
        return new Workflow(
            WorkflowId.generate(),
            "Sample Decision Tree",
            "sample-org",
            List.of(startNode, decisionNode, hotPath, coldPath),
            edges
        );
    }
}