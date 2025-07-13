package com.example.mcpcontextclient.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShareContextRequest {
    private String contextId;
    private String targetOrgId;
    private String accessLevel;
    private Integer expiresInHours;
}
