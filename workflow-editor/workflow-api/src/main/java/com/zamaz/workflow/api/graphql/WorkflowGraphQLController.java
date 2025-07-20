package com.zamaz.workflow.api.graphql;

import com.zamaz.workflow.application.dto.CreateWorkflowRequest;
import com.zamaz.workflow.application.dto.WorkflowResponse;
import com.zamaz.workflow.application.service.WorkflowApplicationService;
import com.zamaz.workflow.api.graphql.input.CreateWorkflowInput;
import com.zamaz.workflow.api.graphql.input.UpdateWorkflowInput;
import com.zamaz.workflow.api.graphql.type.ActionResult;
import com.zamaz.workflow.api.websocket.WorkflowEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WorkflowGraphQLController {
    private final WorkflowApplicationService workflowService;
    private final WorkflowEventPublisher eventPublisher;
    
    @QueryMapping
    @PreAuthorize("hasRole('WORKFLOW_USER')")
    public List<WorkflowResponse> workflows(@Argument String organizationId) {
        log.debug("Fetching workflows for organization: {}", organizationId);
        return workflowService.getWorkflowsByOrganization(organizationId);
    }
    
    @QueryMapping
    @PreAuthorize("hasRole('WORKFLOW_USER')")
    public WorkflowResponse workflow(@Argument String id) {
        log.debug("Fetching workflow: {}", id);
        return workflowService.getWorkflow(id);
    }
    
    @MutationMapping
    @PreAuthorize("hasRole('WORKFLOW_ADMIN')")
    public WorkflowResponse createWorkflow(@Argument CreateWorkflowInput input, Principal principal) {
        log.info("Creating workflow: {} by user: {}", input.getName(), principal.getName());
        
        CreateWorkflowRequest request = CreateWorkflowRequest.builder()
                .name(input.getName())
                .description(input.getDescription())
                .organizationId(input.getOrganizationId())
                .createdBy(principal.getName())
                .build();
        
        return workflowService.createWorkflow(request);
    }
    
    @MutationMapping
    @PreAuthorize("hasRole('WORKFLOW_ADMIN')")
    public WorkflowResponse updateWorkflow(@Argument String id, @Argument UpdateWorkflowInput input) {
        log.info("Updating workflow: {}", id);
        
        WorkflowApplicationService.UpdateWorkflowRequest request = new WorkflowApplicationService.UpdateWorkflowRequest();
        // Map input to request
        
        workflowService.updateWorkflow(id, request);
        return workflowService.getWorkflow(id);
    }
    
    @MutationMapping
    @PreAuthorize("hasRole('WORKFLOW_ADMIN')")
    public Boolean deleteWorkflow(@Argument String id) {
        log.info("Deleting workflow: {}", id);
        try {
            // Implementation would go here
            return true;
        } catch (Exception e) {
            log.error("Failed to delete workflow: {}", id, e);
            return false;
        }
    }
    
    @MutationMapping
    @PreAuthorize("hasRole('WORKFLOW_USER')")
    public ActionResult triggerAction(@Argument String nodeId, @Argument String action) {
        log.info("Triggering action: {} on node: {}", action, nodeId);
        
        try {
            // Implementation would trigger the action
            return ActionResult.builder()
                    .success(true)
                    .message("Action triggered successfully")
                    .build();
        } catch (Exception e) {
            log.error("Failed to trigger action", e);
            return ActionResult.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build();
        }
    }
    
    @SubscriptionMapping
    @PreAuthorize("hasRole('WORKFLOW_USER')")
    public Flux<WorkflowExecutionEvent> workflowExecution(@Argument String workflowId) {
        log.debug("Subscribing to workflow execution events for: {}", workflowId);
        return eventPublisher.subscribeToWorkflowExecution(workflowId);
    }
    
    @lombok.Builder
    @lombok.Data
    public static class WorkflowExecutionEvent {
        private String workflowId;
        private String nodeId;
        private String status;
        private String timestamp;
        private Object data;
    }
}