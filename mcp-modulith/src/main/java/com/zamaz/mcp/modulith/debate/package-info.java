/**
 * Debate management module.
 * Handles debate creation, orchestration, and participant management.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Debate Management",
    allowedDependencies = {"llm", "shared", "organization::OrganizationService"}
)
package com.zamaz.mcp.modulith.debate;