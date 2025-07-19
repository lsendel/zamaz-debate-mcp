package com.example.workflow.application;

import com.example.workflow.domain.*;
import com.example.workflow.domain.ports.*;
import com.example.workflow.domain.services.*;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class WorkflowApplicationService {
    
    private final WorkflowDomainService workflowDomainService;
    private final WorkflowRepository workflowRepository;
    private final TelemetryRepository telemetryRepository;
    
    public WorkflowApplicationService(WorkflowDomainService workflowDomainService,
                                   WorkflowRepository workflowRepository,
                                   TelemetryRepository telemetryRepository) {
        this.workflowDomainService = workflowDomainService;
        this.workflowRepository = workflowRepository;
        this.telemetryRepository = telemetryRepository;
    }
    
    public WorkflowResponse createWorkflow(CreateWorkflowRequest request) {
        Workflow workflow = workflowDomainService.createWorkflow(
            request.name(),
            request.organizationId(),
            request.nodes(),
            request.edges()
        );
        
        workflowRepository.save(workflow);
        return new WorkflowResponse(workflow);
    }
    
    public Optional<WorkflowResponse> getWorkflow(WorkflowId id) {
        return workflowRepository.findById(id)
            .map(WorkflowResponse::new);
    }
    
    public List<WorkflowResponse> getWorkflowsByOrganization(String organizationId) {
        return workflowRepository.findByOrganization(organizationId)
            .stream()
            .map(WorkflowResponse::new)
            .toList();
    }
    
    public WorkflowExecutionResponse executeWorkflow(WorkflowId workflowId, TelemetryData telemetryData) {
        Optional<Workflow> workflowOpt = workflowRepository.findById(workflowId);
        if (workflowOpt.isEmpty()) {
            throw new IllegalArgumentException("Workflow not found: " + workflowId);
        }
        
        Workflow workflow = workflowOpt.get();
        WorkflowExecution execution = workflowDomainService.executeWorkflow(workflow, telemetryData);
        
        return new WorkflowExecutionResponse(execution);
    }
}

record CreateWorkflowRequest(String name, String organizationId, List<WorkflowNode> nodes, List<WorkflowEdge> edges) {}
record WorkflowResponse(WorkflowId id, String name, String organizationId, WorkflowStatus status) {
    public WorkflowResponse(Workflow workflow) {
        this(workflow.getId(), workflow.getName(), workflow.getOrganizationId(), workflow.getStatus());
    }
}
record WorkflowExecutionResponse(ExecutionId id, WorkflowId workflowId, ExecutionStatus status) {
    public WorkflowExecutionResponse(WorkflowExecution execution) {
        this(execution.getId(), execution.getWorkflowId(), execution.getStatus());
    }
}