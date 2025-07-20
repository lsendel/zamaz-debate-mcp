package com.zamaz.workflow.api.graphql.type;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActionResult {
    private boolean success;
    private String message;
    private Object data;
}