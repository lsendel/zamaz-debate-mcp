package com.zamaz.workflow.application.service;

import com.zamaz.workflow.application.dto.CreateWorkflowRequest;
import com.zamaz.workflow.application.dto.WorkflowResponse;
import com.zamaz.workflow.application.port.WorkflowNotificationPort;
import com.zamaz.workflow.domain.command.CreateWorkflowCommand;
import com.zamaz.workflow.domain.entity.Workflow;
import com.zamaz.workflow.domain.repository.WorkflowRepository;
import com.zamaz.workflow.domain.service.WorkflowDomainService;
import com.zamaz.workflow.domain.valueobject.OrganizationId;
import com.zamaz.workflow.domain.valueobject.WorkflowId;
import com.zamaz.telemetry.domain.entity.TelemetryData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkflowApplicationService {
    private final WorkflowDomainService workflowDomainService;
    private final WorkflowRepository workflowRepository;
    private final WorkflowNotificationPort notificationPort;
    
    public WorkflowResponse createWorkflow(CreateWorkflowRequest request) {
        CreateWorkflowCommand command = CreateWorkflowCommand.builder()
                .name(request.getName())
                .description(request.getDescription())
                .organizationId(OrganizationId.of(request.getOrganizationId()))
                .createdBy(request.getCreatedBy())
                .build();
        
        Workflow workflow = workflowDomainService.createWorkflow(command);
        workflowRepository.save(workflow);
        
        notificationPort.notifyWorkflowCreated(workflow);
        
        return WorkflowResponse.from(workflow);
    }
    
    public void executeWorkflowWithTelemetry(String workflowId, TelemetryData data) {
        WorkflowId id = WorkflowId.of(workflowId);
        workflowDomainService.executeWorkflow(id, data);
    }
    
    @Transactional(readOnly = true)
    public List<WorkflowResponse> getWorkflowsByOrganization(String organizationId) {
        return workflowRepository.findByOrganization(OrganizationId.of(organizationId))
                .stream()
                .map(WorkflowResponse::from)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflow(String workflowId) {
        return workflowRepository.findById(WorkflowId.of(workflowId))
                .map(WorkflowResponse::from)
                .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
    }
    
    public void updateWorkflow(String workflowId, UpdateWorkflowRequest request) {
        Workflow workflow = workflowRepository.findById(WorkflowId.of(workflowId))
                .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
        
        // Update workflow based on request
        // This would involve more complex logic in a real implementation
        
        workflowRepository.save(workflow);
        notificationPort.notifyWorkflowUpdated(workflow);
    }
    
    public static class WorkflowNotFoundException extends RuntimeException {
        public WorkflowNotFoundException(String workflowId) {
            super("Workflow not found: " + workflowId);
        }
    }
    
    public static class UpdateWorkflowRequest {
        // Request fields would be defined here
    }
}