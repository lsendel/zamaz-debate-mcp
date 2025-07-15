package com.zamaz.mcp.organization.controller;

import com.zamaz.mcp.organization.dto.OrganizationDto;
import com.zamaz.mcp.organization.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations", description = "Organization management endpoints")
public class OrganizationController {
    
    private final OrganizationService organizationService;
    
    @PostMapping
    @Operation(summary = "Create a new organization")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrganizationDto> createOrganization(
            @Valid @RequestBody OrganizationDto.CreateOrganizationRequest request) {
        OrganizationDto organization = organizationService.createOrganization(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(organization);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get organization by ID")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrganizationDto> getOrganization(@PathVariable UUID id) {
        OrganizationDto organization = organizationService.getOrganization(id);
        return ResponseEntity.ok(organization);
    }
    
    @GetMapping
    @Operation(summary = "List all organizations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrganizationDto>> listOrganizations(Pageable pageable) {
        Page<OrganizationDto> organizations = organizationService.listOrganizations(pageable);
        return ResponseEntity.ok(organizations);
    }
    
    @GetMapping("/my")
    @Operation(summary = "List current user's organizations")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<OrganizationDto>> listMyOrganizations(@RequestParam UUID userId) {
        List<OrganizationDto> organizations = organizationService.listUserOrganizations(userId);
        return ResponseEntity.ok(organizations);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update organization")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrganizationDto> updateOrganization(
            @PathVariable UUID id,
            @Valid @RequestBody OrganizationDto.UpdateOrganizationRequest request) {
        OrganizationDto organization = organizationService.updateOrganization(id, request);
        return ResponseEntity.ok(organization);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete organization")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOrganization(@PathVariable UUID id) {
        organizationService.deleteOrganization(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/users")
    @Operation(summary = "Add user to organization")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> addUserToOrganization(
            @PathVariable UUID id,
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "member") String role) {
        organizationService.addUserToOrganization(id, userId, role);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    
    @DeleteMapping("/{id}/users/{userId}")
    @Operation(summary = "Remove user from organization")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeUserFromOrganization(
            @PathVariable UUID id,
            @PathVariable UUID userId) {
        organizationService.removeUserFromOrganization(id, userId);
        return ResponseEntity.noContent().build();
    }
}