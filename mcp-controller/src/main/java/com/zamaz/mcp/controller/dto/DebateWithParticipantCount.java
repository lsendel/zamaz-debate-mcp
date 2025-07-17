package com.zamaz.mcp.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public interface DebateWithParticipantCount {
    UUID getId();
    UUID getOrganizationId();
    String getTitle();
    String getDescription();
    String getTopic();
    String getFormat();
    Integer getMaxRounds();
    Integer getCurrentRound();
    String getStatus();
    String getSettings();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
    LocalDateTime getStartedAt();
    LocalDateTime getCompletedAt();
    long getParticipantCount();
}
