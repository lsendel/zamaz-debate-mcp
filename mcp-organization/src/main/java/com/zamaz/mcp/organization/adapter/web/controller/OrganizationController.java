package com.zamaz.mcp.organization.adapter.web.controller;

import com.zamaz.mcp.common.architecture.adapter.web.WebAdapter;
import com.zamaz.mcp.organization.adapter.web.dto.*;
import com.zamaz.mcp.organization.adapter.web.mapper.OrganizationWebMapper;
import com.zamaz.mcp.organization.application.command.*;
import com.zamaz.mcp.organization.application.port.inbound.*;
import com.zamaz.mcp.organization.application.query.GetOrganizationQuery;
import com.zamaz.mcp.organization.application.query.OrganizationView;
import com.zamaz.mcp.organization.domain.model.OrganizationId;
import com.zamaz.mcp.organization.domain.model.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

/**
 * Web adapter for organization management.
 * Translates HTTP requests to application use cases.
 */
@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations", description = "Organization management endpoints")
public class OrganizationController implements WebAdapter {
    
    private final CreateOrganizationUseCase createOrganizationUseCase;
    private final GetOrganizationUseCase getOrganizationUseCase;
    private final UpdateOrganizationUseCase updateOrganizationUseCase;
    private final AddUserToOrganizationUseCase addUserToOrganizationUseCase;
    private final RemoveUserFromOrganizationUseCase removeUserFromOrganizationUseCase;
    private final OrganizationWebMapper mapper;
    
    @PostMapping
    @Operation(summary = "Create a new organization")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CreateOrganizationResponse> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        // Map request to command
        var command = new CreateOrganizationCommand(
            mapper.toOrganizationName(request.getName()),
            mapper.toOrganizationDescription(request.getDescription()),
            UserId.from(currentUser.getUsername()),
            request.getSettings()
        );
        
        // Execute use case
        var organizationId = createOrganizationUseCase.execute(command);
        
        // Build response
        var response = new CreateOrganizationResponse(
            organizationId.value(),
            request.getName()
        );
        
        var location = URI.create("/api/v1/organizations/" + organizationId.value());
        return ResponseEntity.created(location).body(response);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get organization by ID")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrganizationResponse> getOrganization(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        // Create query
        var query = GetOrganizationQuery.of(
            id.toString(),
            currentUser.getUsername()
        );
        
        // Execute use case
        var organizationView = getOrganizationUseCase.execute(query);
        
        // Map to response
        var response = mapper.toOrganizationResponse(organizationView);
        
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update organization")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> updateOrganization(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrganizationRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        // Build command
        var commandBuilder = new UpdateOrganizationCommand.Builder(
            id.toString(),
            currentUser.getUsername()
        );
        
        if (request.getName() != null) {
            commandBuilder.withName(request.getName());
        }
        if (request.getDescription() != null) {
            commandBuilder.withDescription(request.getDescription());
        }
        if (request.getSettings() != null) {
            commandBuilder.withSettings(request.getSettings());
        }
        
        // Execute use case
        updateOrganizationUseCase.execute(commandBuilder.build());
        
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/users")
    @Operation(summary = "Add user to organization")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> addUserToOrganization(
            @PathVariable UUID id,
            @Valid @RequestBody AddUserRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        // Create command
        var command = AddUserToOrganizationCommand.of(
            id.toString(),
            request.getUserId(),
            request.getRole(),
            currentUser.getUsername()
        );
        
        // Execute use case
        addUserToOrganizationUseCase.execute(command);
        
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    
    @DeleteMapping("/{id}/users/{userId}")
    @Operation(summary = "Remove user from organization")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> removeUserFromOrganization(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        // Create command
        var command = RemoveUserFromOrganizationCommand.of(
            id.toString(),
            userId.toString(),
            currentUser.getUsername()
        );
        
        // Execute use case
        removeUserFromOrganizationUseCase.execute(command);
        
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/{id}/users/me")
    @Operation(summary = "Leave organization (remove self)")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> leaveOrganization(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        // Create command for self-removal
        var command = RemoveUserFromOrganizationCommand.of(
            id.toString(),
            currentUser.getUsername(),
            currentUser.getUsername()
        );
        
        // Execute use case
        removeUserFromOrganizationUseCase.execute(command);
        
        return ResponseEntity.noContent().build();
    }
}