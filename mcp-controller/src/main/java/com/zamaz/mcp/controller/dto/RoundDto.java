package com.zamaz.mcp.controller.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class RoundDto {
    private UUID id;
    private Integer roundNumber;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private List<ResponseDto> responses;
}
