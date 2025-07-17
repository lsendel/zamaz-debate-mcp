package com.zamaz.mcp.organization.adapter.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

/**
 * Response DTO for organization creation.
 */
@Data
@AllArgsConstructor
public class CreateOrganizationResponse {
    private UUID id;
    private String name;
}