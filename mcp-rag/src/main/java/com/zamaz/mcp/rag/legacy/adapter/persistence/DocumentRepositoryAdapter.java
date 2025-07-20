package com.zamaz.mcp.rag.adapter.persistence;

import com.zamaz.mcp.rag.adapter.persistence.entity.DocumentEntity;
import com.zamaz.mcp.rag.adapter.persistence.entity.DocumentEntity.DocumentStatusEnum;
import com.zamaz.mcp.rag.domain.model.Document;
import com.zamaz.mcp.rag.domain.model.DocumentId;
import com.zamaz.mcp.rag.domain.model.DocumentStatus;
import com.zamaz.mcp.rag.domain.model.OrganizationId;
import com.zamaz.mcp.rag.domain.port.DocumentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter implementation of DocumentRepository using JPA.
 */
@Repository
@Transactional
public class DocumentRepositoryAdapter implements DocumentRepository {
    
    private final DocumentJpaRepository jpaRepository;
    private final DocumentEntityMapper mapper;
    
    public DocumentRepositoryAdapter(
            DocumentJpaRepository jpaRepository,
            DocumentEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public Document save(Document document) {
        DocumentEntity entity = mapper.toEntity(document);
        DocumentEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }
    
    @Override
    public Optional<Document> findById(DocumentId id) {
        return jpaRepository.findById(UUID.fromString(id.toString()))
            .map(mapper::toDomain);
    }
    
    @Override
    public Optional<Document> findByIdAndOrganization(DocumentId id, OrganizationId organizationId) {
        return jpaRepository.findByIdAndOrganizationId(
                UUID.fromString(id.toString()),
                UUID.fromString(organizationId.toString())
            )
            .map(mapper::toDomain);
    }
    
    @Override
    public List<Document> findByOrganization(OrganizationId organizationId) {
        return jpaRepository.findAllByOrganizationIdOrderByCreatedAtDesc(
                UUID.fromString(organizationId.toString())
            )
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Document> findByFilters(
            OrganizationId organizationId,
            List<DocumentStatus> statuses,
            String titleFilter,
            Integer limit,
            Integer offset) {
        
        // Convert status list
        List<DocumentStatusEnum> statusEnums = null;
        if (statuses != null && !statuses.isEmpty()) {
            statusEnums = statuses.stream()
                .map(this::mapStatus)
                .collect(Collectors.toList());
        }
        
        // Create pageable
        Pageable pageable;
        if (limit != null) {
            int page = offset != null ? offset / limit : 0;
            pageable = PageRequest.of(page, limit);
        } else {
            pageable = PageRequest.of(0, Integer.MAX_VALUE);
        }
        
        return jpaRepository.findByFilters(
                UUID.fromString(organizationId.toString()),
                statusEnums,
                titleFilter,
                pageable
            )
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public void delete(DocumentId id) {
        jpaRepository.deleteById(UUID.fromString(id.toString()));
    }
    
    @Override
    public boolean exists(DocumentId id) {
        return jpaRepository.existsById(UUID.fromString(id.toString()));
    }
    
    /**
     * Map domain status to JPA enum.
     */
    private DocumentStatusEnum mapStatus(DocumentStatus status) {
        return switch (status) {
            case UPLOADED -> DocumentStatusEnum.UPLOADED;
            case PROCESSING -> DocumentStatusEnum.PROCESSING;
            case PROCESSED -> DocumentStatusEnum.PROCESSED;
            case FAILED -> DocumentStatusEnum.FAILED;
            case ARCHIVED -> DocumentStatusEnum.ARCHIVED;
        };
    }
}