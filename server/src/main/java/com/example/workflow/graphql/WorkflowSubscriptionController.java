package com.example.workflow.graphql;

import com.example.workflow.application.WorkflowExecutionResponse;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import java.time.Duration;

@Controller
public class WorkflowSubscriptionController {
    
    @SubscriptionMapping
    public Flux<WorkflowExecutionResponse> workflowExecution(@Argument String workflowId) {
        return Flux.interval(Duration.ofSeconds(1))
            .map(i -> new WorkflowExecutionResponse(null, null, null));
    }
}