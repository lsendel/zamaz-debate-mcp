package com.zamaz.workflow.api.graphql.input;

import lombok.Data;

@Data
public class UpdateWorkflowInput {
    private String name;
    private String description;
    private String status;
}