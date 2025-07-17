package com.zamaz.mcp.controller.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnalysisStatus {
    private String debateId;
    private String status; // READY, IN_PROGRESS, COMPLETED, ERROR
    private String message;
    private Double progress; // 0.0 to 1.0
}
