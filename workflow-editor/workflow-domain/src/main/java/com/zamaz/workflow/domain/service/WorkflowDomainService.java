package com.zamaz.workflow.domain.service;

import com.zamaz.workflow.domain.entity.Workflow;
import com.zamaz.workflow.domain.entity.WorkflowNode;
import com.zamaz.workflow.domain.event.WorkflowExecutionResult;
import com.zamaz.workflow.domain.valueobject.WorkflowId;
import com.zamaz.workflow.domain.command.CreateWorkflowCommand;

public interface WorkflowDomainService {
    Workflow createWorkflow(CreateWorkflowCommand command);
    
    void executeWorkflow(WorkflowId id, Object telemetryData);
    
    WorkflowExecutionResult processNode(WorkflowNode node, Object telemetryData);
    
    void validateWorkflow(Workflow workflow);
    
    boolean canNodeConnect(WorkflowNode source, WorkflowNode target);
}