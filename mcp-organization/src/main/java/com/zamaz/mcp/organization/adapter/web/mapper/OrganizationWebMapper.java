package com.zamaz.mcp.organization.adapter.web.mapper;

import com.zamaz.mcp.common.architecture.mapper.DomainMapper;
import com.zamaz.mcp.organization.adapter.web.dto.OrganizationResponse;
import com.zamaz.mcp.organization.application.query.OrganizationView;
import com.zamaz.mcp.organization.domain.model.OrganizationDescription;
import com.zamaz.mcp.organization.domain.model.OrganizationName;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Mapper for converting between web DTOs and domain objects.
 * This is part of the web adapter layer.
 */
@Component
public class OrganizationWebMapper {
    
    /**
     * Converts a string to OrganizationName value object.
     */
    public OrganizationName toOrganizationName(String name) {
        return name != null ? OrganizationName.from(name) : null;
    }
    
    /**
     * Converts a string to OrganizationDescription value object.
     */
    public OrganizationDescription toOrganizationDescription(String description) {
        return description != null 
            ? OrganizationDescription.from(description) 
            : OrganizationDescription.empty();
    }
    
    /**
     * Converts OrganizationView to OrganizationResponse DTO.
     */
    public OrganizationResponse toOrganizationResponse(OrganizationView view) {
        return OrganizationResponse.builder()
            .id(view.id())
            .name(view.name())
            .description(view.description())
            .settings(view.settings())
            .active(view.active())
            .memberCount(view.memberCount())
            .members(view.members().stream()
                .map(this::toMemberResponse)
                .collect(Collectors.toList()))
            .createdAt(view.createdAt())
            .updatedAt(view.updatedAt())
            .build();
    }
    
    /**
     * Converts MemberView to MemberResponse DTO.
     */
    private OrganizationResponse.MemberResponse toMemberResponse(OrganizationView.MemberView member) {
        return OrganizationResponse.MemberResponse.builder()
            .userId(member.userId())
            .email(member.email())
            .firstName(member.firstName())
            .lastName(member.lastName())
            .fullName(member.getFullName())
            .role(member.role())
            .joinedAt(member.joinedAt())
            .build();
    }
}