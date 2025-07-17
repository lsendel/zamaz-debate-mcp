package com.zamaz.mcp.controller.repository.specification;

import com.zamaz.mcp.controller.entity.Debate;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

import com.zamaz.mcp.controller.entity.DebateStatus;

public class DebateSpecifications {

    public static Specification<Debate> hasOrganizationId(UUID organizationId) {
        return (root, query, criteriaBuilder) ->
                organizationId == null ? criteriaBuilder.conjunction() :
                criteriaBuilder.equal(root.get("organizationId"), organizationId);
    }

    public static Specification<Debate> hasStatus(DebateStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? criteriaBuilder.conjunction() :
                criteriaBuilder.equal(root.get("status"), status);
    }
}
