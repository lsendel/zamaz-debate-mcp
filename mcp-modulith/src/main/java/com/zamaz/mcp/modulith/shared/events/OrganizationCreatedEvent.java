package com.zamaz.mcp.modulith.shared.events;

import com.zamaz.mcp.modulith.organization.Organization;

import java.util.UUID;

/**
 * Domain event published when a new organization is created.
 * Other modules can listen to this event to perform their initialization.
 */
public record OrganizationCreatedEvent(
    UUID organizationId,
    String organizationName,
    Organization.SubscriptionTier tier
) {}