package com.zamaz.mcp.controller.dto;

import lombok.Data;

import java.util.Map;

@Data
public class TemplateVariablesRequest {
    private Map<String, Object> variables;
}
