package com.zamaz.mcp.modulith.api;

import com.zamaz.mcp.modulith.organization.Organization;
import com.zamaz.mcp.modulith.organization.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for organization management.
 */
@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {
    
    private final OrganizationService organizationService;
    
    @PostMapping
    public ResponseEntity<Organization> createOrganization(@RequestBody CreateOrganizationRequest request) {
        Organization org = organizationService.createOrganization(request.name(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(org);
    }
    
    @GetMapping
    public List<Organization> listOrganizations() {
        return organizationService.listOrganizations();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Organization> getOrganization(@PathVariable UUID id) {
        return organizationService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PatchMapping("/{id}/tier")
    public ResponseEntity<Organization> updateTier(
            @PathVariable UUID id,
            @RequestParam Organization.SubscriptionTier tier) {
        Organization updated = organizationService.updateTier(id, tier);
        return ResponseEntity.ok(updated);
    }
    
    public record CreateOrganizationRequest(String name, String description) {}
}