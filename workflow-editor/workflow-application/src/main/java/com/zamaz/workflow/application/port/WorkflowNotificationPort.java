package com.zamaz.workflow.application.port;

import com.zamaz.workflow.domain.entity.Workflow;

public interface WorkflowNotificationPort {
    void notifyWorkflowCreated(Workflow workflow);
    
    void notifyWorkflowUpdated(Workflow workflow);
    
    void notifyWorkflowExecutionStarted(String workflowId);
    
    void notifyWorkflowExecutionCompleted(String workflowId, boolean success);
}