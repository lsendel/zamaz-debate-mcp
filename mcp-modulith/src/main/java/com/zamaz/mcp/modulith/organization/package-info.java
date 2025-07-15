/**
 * Organization management module.
 * Handles multi-tenant organization management, including creation, updates, and member management.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Organization Management",
    allowedDependencies = {"security", "shared"}
)
package com.zamaz.mcp.modulith.organization;