package com.example.mcpcontextclient.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetContextWindowRequest {
    private String contextId;
    private int maxTokens;
    private String strategy;
}
