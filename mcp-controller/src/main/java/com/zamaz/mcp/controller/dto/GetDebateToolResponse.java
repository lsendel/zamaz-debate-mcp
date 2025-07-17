package com.zamaz.mcp.controller.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class GetDebateToolResponse {
    private UUID id;
    private String topic;
    private String format;
    private String status;
    private Integer currentRound;
    private Integer maxRounds;
    private List<ParticipantDto> participants;
}
