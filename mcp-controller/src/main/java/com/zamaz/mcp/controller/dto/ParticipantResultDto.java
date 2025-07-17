package com.zamaz.mcp.controller.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParticipantResultDto {
    private String name;
    private String position;
    private Integer responseCount;
}
