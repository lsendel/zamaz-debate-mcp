package com.example.workflow.graphql;

import com.example.workflow.application.*;
import com.example.workflow.domain.*;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;
import java.util.List;
import java.util.Optional;

@Controller
public class WorkflowGraphQLController {
    
    private final WorkflowApplicationService workflowApplicationService;
    
    public WorkflowGraphQLController(WorkflowApplicationService workflowApplicationService) {
        this.workflowApplicationService = workflowApplicationService;
    }
    
    @QueryMapping
    public List<WorkflowResponse> workflows(@Argument String organizationId) {
        return workflowApplicationService.getWorkflowsByOrganization(organizationId);
    }
    
    @QueryMapping
    public Optional<WorkflowResponse> workflow(@Argument String id) {
        return workflowApplicationService.getWorkflow(WorkflowId.of(id));
    }
    
    @MutationMapping
    public WorkflowResponse createWorkflow(@Argument CreateWorkflowInput input) {
        CreateWorkflowRequest request = new CreateWorkflowRequest(
            input.name(),
            input.organizationId(),
            List.of(),
            List.of()
        );
        return workflowApplicationService.createWorkflow(request);
    }
    
    @MutationMapping
    public WorkflowExecutionResponse executeWorkflow(@Argument String id) {
        return workflowApplicationService.executeWorkflow(WorkflowId.of(id), null);
    }
}

record CreateWorkflowInput(String name, String organizationId) {}