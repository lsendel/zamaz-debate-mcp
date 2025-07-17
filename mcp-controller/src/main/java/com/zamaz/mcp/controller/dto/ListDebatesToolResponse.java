package com.zamaz.mcp.controller.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ListDebatesToolResponse {
    private List<DebateInfo> debates;

    @Data
    @Builder
    public static class DebateInfo {
        private UUID id;
        private String topic;
        private String status;
        private LocalDateTime createdAt;
    }
}
