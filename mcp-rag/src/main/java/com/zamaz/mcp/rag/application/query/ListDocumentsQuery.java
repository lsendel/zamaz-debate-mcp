package com.zamaz.mcp.rag.application.query;

import com.zamaz.mcp.rag.domain.model.DocumentStatus;
import com.zamaz.mcp.rag.domain.model.OrganizationId;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Query to list documents with optional filtering.
 */
public record ListDocumentsQuery(
    OrganizationId organizationId,
    Set<DocumentStatus> statuses,
    String titleFilter,
    Integer limit,
    Integer offset
) {
    
    public ListDocumentsQuery {
        Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        Objects.requireNonNull(statuses, "Statuses cannot be null");
        
        if (limit != null && limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        
        if (offset != null && offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }
    }
    
    public static ListDocumentsQuery forOrganization(OrganizationId organizationId) {
        return new ListDocumentsQuery(organizationId, Set.of(DocumentStatus.values()), null, null, null);
    }
    
    public static ListDocumentsQuery searchable(OrganizationId organizationId) {
        return new ListDocumentsQuery(
            organizationId, 
            Set.of(DocumentStatus.COMPLETED), 
            null, null, null
        );
    }
    
    public static ListDocumentsQuery pending(OrganizationId organizationId) {
        return new ListDocumentsQuery(
            organizationId,
            Set.of(DocumentStatus.PENDING),
            null, null, null
        );
    }
    
    public static ListDocumentsQuery failed(OrganizationId organizationId) {
        return new ListDocumentsQuery(
            organizationId,
            Set.of(DocumentStatus.FAILED),
            null, null, null
        );
    }
    
    public ListDocumentsQuery withStatuses(Set<DocumentStatus> newStatuses) {
        Objects.requireNonNull(newStatuses, "Statuses cannot be null");
        return new ListDocumentsQuery(organizationId, newStatuses, titleFilter, limit, offset);
    }
    
    public ListDocumentsQuery withTitleFilter(String filter) {
        return new ListDocumentsQuery(organizationId, statuses, filter, limit, offset);
    }
    
    public ListDocumentsQuery withPagination(int limit, int offset) {
        return new ListDocumentsQuery(organizationId, statuses, titleFilter, limit, offset);
    }
    
    public Optional<String> getTitleFilter() {
        return Optional.ofNullable(titleFilter);
    }
    
    public Optional<Integer> getLimit() {
        return Optional.ofNullable(limit);
    }
    
    public Optional<Integer> getOffset() {
        return Optional.ofNullable(offset);
    }
    
    public boolean hasStatusFilter() {
        return !statuses.isEmpty() && statuses.size() < DocumentStatus.values().length;
    }
    
    public boolean hasTitleFilter() {
        return titleFilter != null && !titleFilter.trim().isEmpty();
    }
    
    public boolean hasPagination() {
        return limit != null || offset != null;
    }
}