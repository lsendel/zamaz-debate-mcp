package com.example.mcpcontextclient.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchContextsRequest {
    private String query;
    private String namespaceId;
    private int limit;
    private boolean includeShared;
}
